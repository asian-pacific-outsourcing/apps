package com.apo.apps.caller.TQ;
/********************************************************************
* @(#)_TQ.java 1.00 20100728
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* _TQ: The entry point for the Qualifier application.
*
* @author Rick Salamone
* @version 1.00 20101020 rts first demo version
* @version 1.01 20101023 rts sends comment to server
* @version 1.02 20101024 rts added history tab
* @version 1.03 20101031 rts title displays version, login displays title
* @version 1.04 20101117 rts decoupled the contact gui from main app
* @version 1.05 20110113 rts using SBMenuBar & SBProperties
*******************************************************/
import com.apo.apps.caller.CallerApp;
import com.apo.net.Access;
import com.shanebow.ui.LAF;
import com.shanebow.util.SBDate;
import com.shanebow.util.SBProperties;

public class _TQ
	{
	private static long blowUp = 0; // SBDate.toTime("20101231  23:59");

	public static void main( String[] args )
		{
		SBProperties.load(_TQ.class, "com/apo/apo.properties");
		LAF.initLAF(blowUp, true);
		Access.parseCmdLine(args);
		CallerApp.createAndShowGUI(Access.TQ);
		}
	}
