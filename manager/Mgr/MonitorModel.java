package com.apo.apps.manager.Mgr;
/********************************************************************
* @(#)MonitorModel.java	1.00 10/06/29
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* MonitorModel: Base class for the various table data models that
* comprise the server monitor. Displays columns of user information
* to the left, followed by production counts, and finally a total
* production and production rate columns on the right.
*
* The production columns provide one column for counting each possible
* disposition the workers can produce. These columns are referred to as
* "buckets" in the code.
*
* The constructor takes an array of "bucketNames" which specifies the
* number and names of these columns. So for eample, the TQ table model
* is created by pass.
*
* @version 1.00 06/29/10
* @author Rick Salamone
* @version 1.01 20110122 modified to new WorkCriteria, * before id
*******************************************************/
import com.shanebow.dao.DataFieldException;
import com.apo.contact.Dispo;
import com.apo.employee.Role;
import com.apo.net.*;
import com.shanebow.util.SBDate;
import com.shanebow.util.SBLog;
import java.util.Vector;
import java.util.StringTokenizer;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.AbstractTableModel;
import javax.swing.SwingUtilities;

public final class MonitorModel
	extends AbstractTableModel
	{
	final int f1stProdCol;
	private String[] fColumnNames;
	private final long fRoleAccess;
	private final Vector<String[]> fColData = new Vector<String[]>();

	MonitorModel(long aRoleAccess)
		{
		String csvHeaders = "Version,Code,Name,Start,Last,Contact,Requests,Errors";
		f1stProdCol = 8;
		if ( aRoleAccess == Access.DM )
			csvHeaders += ",Adds,Dups";
		else if ((aRoleAccess & Access.MGR) == 0 )
			{
			Role role = Role.find(aRoleAccess);
			for (Dispo dispo : role.saveDispos())
				csvHeaders += "," + dispo;
			}
		csvHeaders += ",Total";
		fColumnNames = csvHeaders.split(",");
		fRoleAccess = aRoleAccess;
		}

	protected final void log( String fmt, Object... args )
		{
		SBLog.write( getClass().getSimpleName(), String.format( fmt, args ));
		}

	public void initColumns( JTable table )
		{
		}

	private long lastUpdate = 0;
	long update()
		throws DataFieldException
		{
		long serverTime = SysDAO.DAO().getServerTime();
		Message reply = SysDAO.DAO().syscmd(ClientOp.SC_STATS_GET,
			"" + lastUpdate, "" + fRoleAccess);
		StringTokenizer st = new StringTokenizer(reply.data(), Message.SEP);
		while (st.hasMoreTokens())
			{
			String csv = st.nextToken();
			try { updateRow(csv.split(",")); }
			catch (Exception e) { log("update Error: " + e.getMessage());}
			}
		return lastUpdate = serverTime;
		}

	public void reset()
		throws DataFieldException
		{
		fColData.clear();
		fireTableDataChanged();
		lastUpdate = 0;
		Message reply = SysDAO.DAO().syscmd(ClientOp.SC_STATS_RESET,
			"" + fRoleAccess);
		}

	private void updateRow(String[] aColData)
		{
		int size = fColData.size();
		for ( int r = 0; r < size; r++ )
			{
			String[] cols = fColData.get(r);
			if ( cols[1].equals(aColData[1]))
				{
				fColData.set(r,aColData);
				fireTableRowsUpdated(r,r);
				return;
				}
			}
		fColData.add(aColData);
		fireTableRowsInserted(size,size);
		}

	public int getColumnCount() { return fColumnNames.length; }

	public String getColumnName(int c) { return fColumnNames[c]; }

	public String[] getRow( int r ) { return fColData.get(r); }
	public int      getRowCount()   { return fColData.size(); }

	public Object getValueAt( int r, int c )
		{
		try
			{
			String[] row = getRow(r);
			if ( c== 3 || c == 4) // login time or last op time
				return SBDate.friendly(Long.parseLong(row[c]));
			return row[c];
			}
		catch (Exception e) { return String.format("%d,%d: %s", r, c, e.toString());}
		}

	public boolean    isCellEditable(int r, int c) { return false; }
	}
