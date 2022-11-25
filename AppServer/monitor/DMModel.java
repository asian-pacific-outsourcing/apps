package com.apo.apps.AppServer.monitor;
/********************************************************************
* @(#)DMModel.java	1.00 10/06/29
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* DMModel: Table data model to manage data miner production stats.
*
* @version 1.00 06/29/10
* @author Rick Salamone
*******************************************************/
import com.apo.employee.User;

public final class DMModel
	extends UsrTableModel
	implements Monitor
	{
	static final String[] bucketNames =
		{ "Adds", "Dups" };

	public DMModel() { super(bucketNames); }

	// implement Monitor
	@Override public final DMStats addUser(User usr, long loginTime,
		                                long access, String version )
		{
		DMStats dms = new DMStats( usr, access, loginTime, version );
		dms.m_modelRow = super.addUser( dms );
		return dms;
		}

	@Override public final String  getTitle()  { return "DM"; }
	@Override public final String  getToolTip() { return "Data Miners production"; }
	@Override public com.apo.contact.Dispo[] getDispos() { return null; }
	@Override public final long    getAccess() { return com.apo.net.Access.DM; }
	@Override public final UsrTableModel getModel() { return this; }
	}
