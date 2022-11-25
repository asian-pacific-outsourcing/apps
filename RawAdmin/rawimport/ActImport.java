package com.apo.apps.RawAdmin.rawimport;
/********************************************************************
* @(#)ActImport.java 1.0 20110609
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* ActImport: Action to launch the raw contact import process.
*
* @version 1.00 20110609
* @author Rick Salamone
* 20110609 rts created
*******************************************************/
import com.shanebow.ui.SBAction;
import java.awt.event.*;

public class ActImport
	extends SBAction
	{
	public ActImport()
		{
		super ( "Import", 'I', "Import contacts from CSV file", null );
		}

	@Override public boolean menuOnly() { return true; }
	@Override public void actionPerformed(ActionEvent evt)
		{
		new ImportWiz().onBrowseFiles();
		}
	}
