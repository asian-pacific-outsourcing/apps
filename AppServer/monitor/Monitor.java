package com.apo.apps.AppServer.monitor;
/********************************************************************
* @(#)Monitor.java	1.00 10/07/09
* Copyright (c) 2010-2011 by Richard T. Salamone, Jr. All rights reserved.
*
* Monitor: Compiles user information for the server.
*
* @author Rick Salamone
* @version 1.00, 20101023 rts created
* @version 1.01, 20101025 rts added getRoleWorkCriteria() for specifying workload
* @version 1.02, 20110418 rts removed csv backup of leads
* 20110505 rts 1.03 removed criteria stuff - now in mgr app
* 20110615 rts 1.04 addUser() returns UserStat, was void
*******************************************************/
import com.apo.contact.Dispo;
import com.apo.employee.User;

public interface Monitor
	{
	public UserStats addUser(User usr, long loginTime, long access, String version );
	public String    getTitle();
	public String    getToolTip();
	public Dispo[]   getDispos();
	public long      getAccess();

	public UsrTableModel  getModel();
	}
