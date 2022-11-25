package com.apo.apps.AppServer.monitor;
/********************************************************************
* @(#)UsrTableModel.java	1.00 10/06/29
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* UsrTableModel: Base class for the various table data models that
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
* @version 1.02 20110505 Criteria now handled by manager app
*******************************************************/
import com.shanebow.util.SBDate;
import com.shanebow.util.SBLog;
import java.util.Vector;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.AbstractTableModel;
import javax.swing.SwingUtilities;

public class UsrTableModel
	extends AbstractTableModel
	{
	static final int COL_VER     = 0;
	static final int COL_LOGIN   = 1;
	static final int COL_NAME    = 2;
	static final int COL_BEGIN   = 3;
	static final int COL_LAST    = 4;
	static final int COL_REQS    = 5;
	static final int COL_ERRS    = 6;
	static final int COL_WORK    = 7;
	static final int COL_DISPO1  = 8; // first "bucket" dispo count column

	private final Vector<UserStats> m_data = new Vector<UserStats>();
	private final Object[] m_prodLabels;
	private final int COL_TOTAL;
	private final int COL_RATE;

	UsrTableModel( Object[] bucketNames )
		{
		m_prodLabels = bucketNames;
		COL_TOTAL = COL_DISPO1 + bucketNames.length;
		COL_RATE  = COL_TOTAL + 1;
		}

	protected final void log( String fmt, Object... args )
		{
		SBLog.write( getClass().getSimpleName(), String.format( fmt, args ));
		}

	public void initColumns( JTable table )
		{
		}

	public int getColumnCount()
		{
		// user info columns + production counts (one for each dispo)
		// + 1 col for "Total" + 1 col for "Rate"
		return COL_DISPO1 + m_prodLabels.length + 2; 
		}

	public String getColumnName(int c)
		{
		return (c == COL_VER)  ? "Version"
		     : (c == COL_LOGIN)? "Code"
		     : (c == COL_NAME) ? "Name"
		     : (c == COL_BEGIN)? "Start"
		     : (c == COL_LAST) ? "Last"
		     : (c == COL_REQS) ? "Requests"
		     : (c == COL_ERRS) ? "Errors"
		     : (c == COL_WORK) ? "Contacts"
		     : (c == COL_TOTAL)? "Total"
		     : (c == COL_RATE) ? "Rate"
		     : m_prodLabels[c - COL_DISPO1].toString();
		}

	public UserStats getRow( int r ) { return m_data.get(r); }
	public int       getRowCount()   { return m_data.size(); }

	public Object getValueAt( int r, int c )
		{
		UserStats usr = getRow(r);
		return (c == COL_VER) ?  (usr.isConnected()? usr.version():"")
		     : (c == COL_LOGIN)? usr.login()
		     : (c == COL_NAME) ? usr.name()
		     : (c == COL_BEGIN)? SBDate.friendly( usr.loginTime())
		     : (c == COL_LAST) ? SBDate.friendly( usr.lastTime())
		     : (c == COL_WORK) ? usr.lockString(' ')
		     : (c == COL_REQS) ? usr.requests()
		     : (c == COL_ERRS) ? usr.errors()
		     : (c == COL_TOTAL)? usr.getProductionTotal()
		     : (c == COL_RATE) ? usr.rate()
		     : usr.getProduction(c - COL_DISPO1);
		}

	public boolean    isCellEditable(int r, int c) { return false; }

	public final int addUser(UserStats us )
		{
		final int index;
		synchronized(this)
			{
			m_data.add( us );
			index = m_data.size() - 1;
			}
		SwingUtilities.invokeLater( new Runnable()
			{
			public void run() { fireTableRowsInserted( index, index ); }
			});
//		fireTableRowsInserted( index, index );
		return index;
		}

	public void reset()
		{
		m_data.clear();
		fireTableDataChanged();
		}
	}
