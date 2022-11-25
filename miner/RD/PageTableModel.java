package com.apo.apps.miner.RD;
/********************************************************************
* @(#)PageTableModel.java	1.00 10/06/04
* Copyright (c) 2010-2011 by Richard T. Salamone, Jr. All rights reserved.
*
* PageTableModel: Manages the table data displayed for one checked out
* page that is being dispositioned after the TQ process.  Reads the
* contact records on a given page from the data base. User can edit
* selected fields (disposition, callback date, phone #s, email). The
* commit method causes edits to be validated and written back to the
* database.
*
* @author Rick Salamone
* @version 1.00 20100604 rts created
* @version 2.00 20110316 rts modified fetch & checkin to use Raw DAO
*******************************************************/
import com.apo.contact.*;
import com.apo.contact.touch.TouchCode;
import com.apo.net.Access;
import com.shanebow.util.SBLog;
import com.shanebow.ui.SBDialog;
import java.util.Vector;
import javax.swing.table.AbstractTableModel;

public class PageTableModel
	extends AbstractTableModel
	{
	private static final String MODULE="PTM";
	private static final String SEPARATOR="==================================================";
	Vector<PageRow>    m_rows = new Vector<PageRow>(11);

	private void logSeparate( String msg )
		{
		SBLog.write( SEPARATOR );
		SBLog.write( MODULE, msg );
		}

	private boolean logError( String msg )
		{
		java.awt.Toolkit.getDefaultToolkit().beep();
		SBLog.error(MODULE + " ERROR", msg );
		return false;
		}

	private boolean logSuccess()
		{
		SBLog.write(MODULE, "Success" );
		return true;
		}

	public boolean getPage(String whereClause, int maxRows )
		{
		String stmt = "SELECT * FROM " + Raw.DB_TABLE
		           + " WHERE " + whereClause
		           + " ORDER BY disposition DESC, id ASC;";
		logSeparate( "Execute SQL: " + stmt );
		Vector<Raw> list = new Vector<Raw>();
		try
			{
			Raw.DAO.fetch(list, maxRows, stmt);
			m_rows.clear(); // Get all rows
			for ( Raw raw : list )
				m_rows.add(new PageRow(raw));
			fireTableDataChanged(); // Tell the listeners a new page has arrived.
			}
		catch (Exception ex) { logError( ex.toString()); return false; }
		logSuccess();
		return m_rows.size() > 0;
		}

	public boolean isDirty()
		{
		int nRows = m_rows.size();
		for ( int i = 0; i < nRows; i++ )
			if ( m_rows.get(i).isDirty())
				return true;
		return false;
		}

	public boolean checkIn()
		{
		int nRows = m_rows.size();
		for ( int i = 0; i < nRows; i++ )
			if ( !m_rows.get(i).isConsistent())
				return false;
		long when = Raw.DAO.getServerTime();
		short uid = Access.getUID();
		try
			{
			for ( int i = nRows-1; i >= 0; --i )
				{
				Raw raw = m_rows.get(i).getEdits();
				Raw.DAO.update( raw, true, TouchCode.CHECKIN, "", when, uid);
				m_rows.remove(i);
				fireTableRowsDeleted(i,i);
				}
			return true;
			}
		catch (Exception e)
			{
			SBDialog.error("Check In Contacts", e.getMessage());
			return logError( e.toString());
			}
		}

	public String getColumnName(int col)
		{
		return PageRow.getColumnName(col);
		}

	public Class getColumnClass(int col)
		{
		return PageRow.getColumnClass(col);
		}

	public boolean isCellEditable(int row, int col)
		{
		return PageRow.isEditable(col);
		}

	public int getColumnCount()
		{
		return PageRow.getColumnCount();
		}

	public int getRowCount()
		{
		return m_rows.size();
		}

	public Object getValueAt(int aRow, int aColumn)
		{
		return m_rows.get(aRow).getCol(aColumn);
		}

	public void setValueAt(Object value, int row, int col)
		{
		m_rows.get(row).setValueAt( value, col);
		}
	}
