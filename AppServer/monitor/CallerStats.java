package com.apo.apps.AppServer.monitor;
/********************************************************************
* @(#)CallerStats.java	1.00 10/07/09
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* CallerStats: Compiles user information for the server.
*
* @author Rick Salamone
* 20100709 rts 1.00 created
* 20110430 rts 1.01 added ability to get stats as CSV
* 20110503 rts 1.02 added clearStats method
* 20110505 rts 1.03 redid criteria stuff to move to mgr app
*******************************************************/
import com.apo.contact.Raw;
import com.apo.contact.Dispo;
import com.apo.net.*;
import com.shanebow.util.CSV;
import com.apo.employee.User;
import com.shanebow.util.SBDate;
import java.util.*;
import javax.swing.SwingUtilities;

public final class CallerStats
	extends UserStats
	{
	private final Dispo[]  m_dispos;    // lists the valid dispos
	private final int[]    m_dispoedAs; // production count for each dispo
	private int            m_dispoedTotal = 0;
	private final String   m_uidClause;

	CallerStats( User usr, long access, long loginTime, Monitor mon, String version )
		{
		super( usr, access, loginTime, mon, version );
		m_dispos = mon.getDispos();
		m_dispoedAs = new int[m_dispos.length];
		m_uidClause = " AND " + Raw.dbField(Raw.CALLER)
		            + ((access >= Access.AO) ? ("=" + usr.id())
		                                     : (" IN (0," + usr.id() + ")"));
		}

	@Override public void clearStats()
		{
		super.clearStats();
		for ( int i = 0; i < m_dispos.length; i++ )
			m_dispoedAs[i] = 0;
		m_dispoedTotal = 0;
		}

	public void doDispo(Dispo d)
		{
		boolean found = false;
		if ( d != null )
			for ( int i = 0; !found && (i < m_dispos.length); i++ )
				if ( d == m_dispos[i] )
					{
					++m_dispoedTotal;
					++m_dispoedAs[i];
					found = true;
					}
		if ( !found )
			error("Unknown dispo: " + d);
		SwingUtilities.invokeLater( new Runnable()
			{
			public void run()
				{ m_monitor.getModel().fireTableRowsUpdated( m_modelRow, m_modelRow ); }
			});
		}

	@Override public String csvHeader()
		{
		String csv = super.csvHeader();
		for (Dispo dispo : m_dispos )
			csv += "," + dispo;
		csv += ",Total";
		return csv;
		}

	@Override public final String csv()
		{
		String csv = super.csv();
		for (int x : m_dispoedAs)
			csv += "," + x;
		csv += "," + m_dispoedTotal;
		return csv;
		}

	public final String getWhereClause()
		{
		return ((CallerModel)getMonitor()).getWhere() + m_uidClause;
		}

	@Override
	public int getProduction(int index)  { return m_dispoedAs[index]; }
	@Override
	public int getProductionTotal() { return m_dispoedTotal; }
	}
