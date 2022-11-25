package com.apo.apps.RawAdmin.rawimport;
/********************************************************************
* @(#)ImportWorker.java 1.00 20100523
* Copyright 2010-2011 by Richard T. Salamone, Jr. All rights reserved.
*
* ImportWorker: Parses a csv file, attempting to create a new
* contact record in the database for each line.
*
* @author Rick Salamone
* @version 1.00 05/23/10
* @version 1.01 06/01/10 now in background - massive debug at APO
* @version 1.02 06/03/10 now using the defaults supplied by user
* @version 2.00 08/24/10 Handles multiple files, merges and sorts first
* @version 2.01 08/31/10 duplicate check compares ContactNames: was String
* @version 2.02 20101003 changes for new altPhone field
* @version 2.03 20101004 delegated db add logic to admin to share with server
* @version 2.04 20101004 makes touch entry
* @version 2.05 20101020 uses the correct user for the touch entry
* @version 2.06 20110209 using RawDBDAO instead of RawLead
*******************************************************/
import com.apo.admin.TouchDB;
import com.apo.contact.*;
import com.apo.contact.touch.TouchCode;
import com.shanebow.dao.ContactID;
import com.shanebow.dao.EmpID;
import com.shanebow.dao.DBStatement;
import com.shanebow.dao.DataFieldException;
import com.shanebow.dao.DuplicateException;
import com.shanebow.util.CSV;
import com.shanebow.ui.SBDialog;
import com.shanebow.util.MessageLogger;
import com.shanebow.util.SBLog;
import com.shanebow.util.SBDate;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import javax.swing.SwingWorker;

public final class ImportWorker extends SwingWorker<Void, ImportStatus>
	{
	private static final String CSV_HEADER=Raw.DMCSV_HEADER;

	public static final String BAD_HEADER = "File header does not match expected values";
	public static final String BAD_FILE = "File open error";
	public static final String MORE_HEADERS = "There are more headers than database fields";

	static ImportFailsLog logFails = new ImportFailsLog();
	MessageLogger logTrace = null;

	private File[]         m_files;        // the import files
	private long           m_startTime;    // when this import began
	private int            m_errCount = 0; // running count of errors/exceptions
	private int            m_dupCount = 0; // running count of rejected duplicates
	private int            m_line = 0;     // current line # being processed
	private int            m_lineCount;    // total # of lines to import (for progress)

	ImportWorker( File[] files )
		{
		m_files = files;
		m_startTime = SBDate.timeNow();
		}

	private void appLog ( String format, Object... args )
		{
		String msg = String.format( format, args );
		SBLog.write( "Import Contacts", msg );
		}

	private void trace ( String format, Object... args )
		{
		logTrace.write( String.format( format, args ));
		}

	private String readln(BufferedReader stream)
		throws IOException
		{
		int ch;
		int prev = 0;
		String s = "";
		while (( ch = stream.read()) != -1 )
			{
			switch ( ch )
				{
				case -1: return null;
				case 10: if ( prev == 13 ) return s;
				// case 12: s= "<ff>"; break;
				case 13: prev = 13; break;
				default: if ( ch >= 32 ) s += "" + (char)ch; break;
				}
			}
		return null;
		}

	@Override
	protected void process(List<ImportStatus> chunks)
		{
		int line = 0;
		for ( ImportStatus found : chunks )
			line = found.m_line;
		setProgress( line * 100 / m_lineCount );
		}

	@Override
	public Void doInBackground()
		{
		appLog ( "begin import" );
		Vector<String> lines = null; // all of file lines, sans headers, sorted
		String text = ""; // current csv line we are processing
		DBStatement db = null;
		long when = com.shanebow.util.SBDate.timeNow();
		short uid = com.apo.net.Access.getUID();
		try
			{
			lines = mergeAndSort( m_files );
			m_lineCount = lines.size();

			trace( "*** sort complete, importing to database...");
			db = new DBStatement();
			for ( m_line = 0; m_line < m_lineCount; ++m_line )
				{
				try
					{
					String[] pieces = text.split(",", 4);
					long whenMined = Long.parseLong(pieces[0]);
					EmpID minerID = EmpID.parse(pieces[1]);
					String host = pieces[2];
					Raw raw = new Raw( "0," + pieces[3]);
					ContactID id = ((com.apo.admin.RawDBDAO)Raw.DAO).addLead( db, raw,
					                 minerID, TouchCode.MINED, host, whenMined );
					TouchDB.add( db, TouchCode.IMPORTED, id, when, uid, "" );
					}
				catch (DuplicateException e)
					{
					trace( "Line #%d, Duplicate #%d: %s", m_line, ++m_dupCount, e.getMessage());
					}
				catch (DataFieldException e)
					{
					if ( ++m_errCount == 1 )
						logFails.write(CSV_HEADER);
					logFails.write(text);
					trace( "Line #%d, Error #%d: %s", m_line, m_errCount, e.toString());
					}
				catch (Throwable e)
					{
					if ( ++m_errCount == 1 )
						logFails.write(CSV_HEADER);
					logFails.write(text);
					trace( "Line #%d, Error #%d (FATAL): %s", m_line, m_errCount, e.toString());
					trace( "csv: " + text + "\nlast sql statement: " + db.getSQL()
					         + "\nTry cleaning file and rerun import" );
					break;
					}
				finally { publish( new ImportStatus( m_line, null )); }
				}
			}
		catch (Exception e)
			{
			trace ( "ERROR: " + e.toString());
			appLog ( "ERROR: " + e.toString());
e.printStackTrace();
	//		return false;
			}
		finally
			{
			if (db != null) db.close();
			if (lines != null) lines.clear();
			if (lines != null) lines.trimToSize();
			}
		summary();
		firePropertyChange("state", "", "FINISHED" );
		return (Void)null;
		}

	private Vector<String> mergeAndSort( File[] files )
		throws IOException
		{
		Vector<String> m_lines = new Vector<String>(200,100);
		BufferedReader stream = null;
		for ( File file : files )
			{
			trace ( "merging '" + file.getName() + "'");
			try
				{
				stream = new BufferedReader(new FileReader(file));
				}
			catch ( IOException e )
				{
				appLog ( BAD_FILE );
				SBDialog.error( BAD_FILE, e.getLocalizedMessage());
				throw e;
				}
			try
				{
				String line;
				if ( !checkHeaders(file.getName(), readln(stream)))
//				if ( !checkHeaders(stream.readLine()))
					throw new IOException("Bad header");
				while (( line =  readln(stream)) != null )
//				while (( line = stream.readLine()) != null )
					m_lines.add(line.trim());
				}
			finally { if ( stream != null ) stream.close(); }
			}
		m_lines.trimToSize();
		trace( "*** Merged %d files for %d total lines", m_files.length, m_lines.size());
		trace( "*** sorting...");
		Collections.sort(m_lines);
		return m_lines;
		}

	private void summary()
		{
		String msg = "*** Read " + m_line + " lines: " + m_errCount + " errors, "
		      + m_dupCount + " definite duplicates";
		long elapsedTime = SBDate.timeNow() - m_startTime;
		msg += String.format ( "\n*** Elapsed time: %d:%02d", elapsedTime/60, elapsedTime%60 );
		trace ( msg );
		appLog ( msg );
		}

	public boolean checkHeaders( String filename, String text )
		{
		if ( !CSV_HEADER.toLowerCase().equals(text.toLowerCase()))
			{
			appLog ( filename + " Warning: " + BAD_HEADER );
			if ( !SBDialog.confirm( filename + "\n" + BAD_HEADER
			    + "\n" + text.toLowerCase() + "(found)\n"
			    + CSV_HEADER.toLowerCase() + "(expected)\n\nProceed anyway?" ))
				return false;
			}
		return true;
		}
	}

class ImportStatus
	{
	String m_msg;
	int m_line;
	public ImportStatus( int line, String msg )
		{
		m_line = line;
		String m_msg = msg;
		}
	}
