package com.apo.apps.RawAdmin.rawimport;
/********************************************************************
* @(#)ImportFailsLog.java 1.00 20101003
* Copyright 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* ImportFailsLog: The actual log for import failures.
*
* @author Rick Salamone
* @version 1.00 20101003
*******************************************************/
import com.shanebow.ui.SBTextPanel;
import java.awt.Color;
import java.awt.Dimension;

class ImportFailsLog extends SBTextPanel
	{
	public ImportFailsLog()
		{
		super("Failed Import Lines", false, Color.YELLOW );
		setTimeStamp(false);
		setPreferredSize( new Dimension(500,300));
		}
	}