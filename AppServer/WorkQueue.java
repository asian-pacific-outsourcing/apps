package com.apo.apps.AppServer;
/********************************************************************
* @(#)WorkQueue.java 1.00 20110602
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* WorkQueue: Maintains a queue of scheduled work, that is
* populated in a background task, and retrieved as needed by
* the foreground task.
*
* @author Rick Salamone
* @version 1.00, 20110602
* 20110602 rts created
*******************************************************/
import com.apo.apps.AppServer.monitor.CallerModel;
import com.apo.apps.AppServer.monitor.UserStats;
import com.apo.contact.Raw;
import com.apo.net.Access;
import com.shanebow.dao.DBStatement;
import com.shanebow.util.SBArray;
import com.shanebow.util.SBDate;
import com.shanebow.util.SBLog;
import java.util.*;
import java.sql.ResultSet;
import javax.swing.SwingUtilities;

public final class WorkQueue
	{
	/** MAX is the maximum number of work records to cache
	*/
	private static final int MAX = 500;

	/** Throughout the code, this is the object we synchronize on so this
	* is also the object we wait() and notifyAll() on.
	*/
	protected SBArray<String> fList = new SBArray<String>(MAX);

	/** The page number to use for locking our records
	*/
	private final int fPage;

	/** The array index to the next record to return
	*/
	private volatile int fIndex;

	private volatile boolean fAvailable = false;
	private final CallerModel fCallerModel;

	WorkQueue(CallerModel aCallerModel, int aPage)
		{
		fCallerModel = aCallerModel;
		fPage = aPage;
		log( "startup: " + fCallerModel.getWhere());
		releaseAllLocks();
		load();
//		purge();
		}

	private void log(String fmt, Object... args)
		{
		final String msg = "WorkQueue." + String.format(fmt, args);
//		System.out.println( msg );
		if (SwingUtilities.isEventDispatchThread())
			SBLog.write(msg);
		else SwingUtilities.invokeLater(new Runnable()
			{
			public void run() { SBLog.write(msg); }
			});
		}

	private void releaseAllLocks()
		{
		DBStatement db = null;
		try
			{
			db = new DBStatement();
			String stmt = "UPDATE " + Raw.DB_TABLE
			            + " SET " + "page=0"
			            + " WHERE page <= " + Access.MAX_UID;
			int count = db.executeUpdate( stmt );
			log( "unlocked %d records: %s", count, stmt );
			}
		catch (Throwable t)
			{
			log( "Error: %s\n  Offending SQL: %s",
				t.getMessage(), ((db == null)? "none" : db.getSQL()));
			}
		finally { if (db != null) db.close(); }
		}

	/** Removes all remaining entries from fList and
	* unlocks the corresponding records in the database
	*/
	public void purge()
		{
		StringBuilder sql = new StringBuilder(
			"UPDATE " + Raw.DB_TABLE + " SET page=0 WHERE id IN (");
		synchronized(fList)
			{
			int size = fList.size();
			for ( int i = 0; i < size; i++ )
				{
				String rawCSV = fList.remove(fIndex++);
				if ( rawCSV != null )
					sql.append( rawCSV.substring(0, rawCSV.indexOf(','))).append(',');
				}
			fIndex = -1;
			}
		sql.setCharAt(sql.length()-1, ')');
		log("clear sql: " + sql.toString());
		DBStatement db = null;
		try
			{
			db = new DBStatement();
			int count = db.executeUpdate( sql.toString());
			log( "cleared %d records", count );
			}
		catch (Throwable t)
			{
			log( "Error: %s\n  Offending SQL: %s",
				t.getMessage(), ((db == null)? "none" : db.getSQL()));
			}
		finally { if (db != null) db.close(); }
		}

	private void load()
		{
		DBStatement db = null;
		ResultSet rs = null;
		int count = 0;
		try
			{
			String stmt = "SELECT * FROM " + Raw.DB_TABLE + " "
			            + fCallerModel.getWhere()
			            + " AND callback <= " + SBDate.timeNow()
			            + " ORDER BY disposition DESC, callback ASC;";
			log( "load: " + stmt );
			synchronized(fList)
				{
				db = new DBStatement(true); // updatable
				rs = db.executeQuery( stmt );
				while ( rs.next())
					{
					long id = rs.getLong(1);
					String csv = "" + id;
					for ( int i = 2; i <= Raw.NUM_FIELDS; i++ )
						{
						String fieldAsString = rs.getString(i);
						csv += ",\"" + ((fieldAsString==null)?"":fieldAsString) + "\"";
						}
					fList.add(csv); // fList.set(index, csv);
					++count;
					rs.updateLong("page", fPage ); // fRequest.uid());
					rs.updateRow();
					if ( fList.size() >= MAX )
						break;
					}
				fAvailable = !fList.isEmpty();
				fIndex = fAvailable? 0 : -1;
				}
			}
		catch (Throwable t)
			{
			log( "Error: %s\n  Offending SQL: %s",
				t.getMessage(), ((db == null)? "none" : db.getSQL()));
			}
		finally { if (db != null) { db.closeResultSet(rs); db.close(); }}
		log( "added %d items", count );
		}

	public boolean hasWork() { return fAvailable; }

	public String get(UserStats ur)
		{
		String rawCSV = null;
		int len = 0;
		synchronized(fList)
			{
			if ( fList.isEmpty()) load();
			if ( !fList.isEmpty())
				{
				rawCSV = fList.remove(fIndex++);
				long id = Long.parseLong(rawCSV.substring(0, rawCSV.indexOf(',')));
				ur.acquiredID(id);
				}
			len = fList.size();
			fAvailable = len > 0;
//			fList.notifyAll();
			}
//		log("dequeue work " + rawCSV + "\nList size now " + len);
		return rawCSV;
		}
	}
