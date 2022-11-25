package com.apo.apps.RawAdmin.rawimport;
/********************************************************************
* @(#)ActViewImportFailures.java 1.0 20110609
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* ActViewImportFailures: Action to view the raw contact import
* failure log.
*
* @version 1.00 20110609
* @author Rick Salamone
* 20110609 rts created
*******************************************************/
import com.shanebow.ui.SBAction;
import java.awt.event.*;

public class ActViewImportFailures
	extends SBAction
	{
	public ActViewImportFailures()
		{
		super ( "Failed Imports", 'F', "View the Import contacts failure log", null );
		}

	@Override public boolean menuOnly() { return true; }
	@Override public void actionPerformed(ActionEvent evt)
		{
		ImportFails.launch();
		}
	}
