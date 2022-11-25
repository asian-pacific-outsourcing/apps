package com.apo.apps.AppServer.monitor;
/********************************************************************
* @(#)MgrModel.java	1.00 10/06/29
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* MgrModel: Table data model to manage data miner production stats.
*
* @version 1.00 06/29/10
* @author Rick Salamone
*******************************************************/
import com.apo.employee.User;

public final class MgrModel
	extends UsrTableModel
	implements Monitor
	{
	static final String[] bucketNames =
		{ "Xs", "Os" };

	public MgrModel() { super(bucketNames); }

	// implement Monitor
	@Override public final MgrStats addUser(User usr, long loginTime,
		long access, String version )
		{
		MgrStats mgrs = new MgrStats( usr, access, loginTime, version );
		mgrs.m_modelRow = super.addUser( mgrs );
		return mgrs;
		}

	@Override public final String  getTitle()  { return "Mgr"; }
	@Override public final String  getToolTip() { return "Managers"; }
	@Override public com.apo.contact.Dispo[] getDispos() { return null; }
	@Override public final long    getAccess() { return com.apo.net.Access.MGR; }
	@Override public final UsrTableModel getModel() { return this; }
	}
