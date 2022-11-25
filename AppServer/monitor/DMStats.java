package com.apo.apps.AppServer.monitor;
/********************************************************************
* @(#)DMStats.java	1.00 10/07/09
* Copyright (c) 2010-2011 by Richard T. Salamone, Jr. All rights reserved.
*
* DMStats: Compiles user information for the server.
*
* @version 1.00 07/09/10
* @author Rick Salamone
* 20100709 rts 1.00 created
* 20110430 rts 1.01 added ability to get stats as CSV
* 20110503 rts 1.02 added clearStats method
*******************************************************/
import com.apo.net.*;
import com.shanebow.util.CSV;
import com.apo.employee.User;
import java.util.*;
import javax.swing.SwingUtilities;

public final class DMStats extends UserStats
	{
	// Class data & methods
	private static final DMModel _model = new DMModel();
	public  static final DMModel getModel() { return _model; }

	// Instance data & methods
	private int      m_adds = 0;
	private int      m_dups = 0;

	DMStats( User usr, long access, long loginTime, String version )
		{
		super( usr, Access.DM, loginTime, (Monitor)getModel(), version);
		}


	@Override public void clearStats()
		{
		super.clearStats();
		m_adds = 0;
		m_dups = 0;
		}

	@Override public String csvHeader()
		{
		return super.csvHeader() + ",Adds,Dups,Total";
		}

	@Override public final String csv()
		{
		return super.csv()
			+ "," + m_adds + "," + m_dups + "," + (m_adds + m_dups);
		}

	public void doAdd( byte snafuCode )
		{
		if ( snafuCode == Snafu.CODE_NONE ) ++m_adds;
		else if ( snafuCode == Snafu.CODE_DUPLICATE ) ++m_dups;
		else error();
		SwingUtilities.invokeLater( new Runnable()
			{
			public void run() { _model.fireTableDataChanged(); }
			});
		}

	@Override public int getProduction(int index)
		{ return (index==0)? m_adds : m_dups; }
	int adds()  { return m_adds; }
	int dups()  { return m_dups; }
	@Override public int getProductionTotal() { return m_adds + m_dups; }
	}
