package com.apo.apps.manager.MailManager;
/********************************************************************
* @(#)_MailManager.java 1.00 20110228
* Copyright (c) 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* _MailManager: The entry point and main container frame for the
* MailManager application.
*
* @author Rick Salamone
* @version 1.00 20110228 rts first demo version
* @version 1.01 20110303 rts creates a mail merge file
*******************************************************/
import com.apo.apps.caller.CallerGUI;
import com.apo.apps.caller.DlgContact;
import com.apo.net.Access;
import com.apo.net.client.ClientApp;
import com.shanebow.ui.menu.*;
import com.shanebow.ui.LAF;
import com.shanebow.util.SBDate;
import com.shanebow.util.SBProperties;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class _MailManager
	extends ClientApp
	{
	private static long blowUp = 0; // SBDate.toTime("20101231  23:59");
	static final int    APP_WIDTH = 950;
	static final int    APP_HEIGHT = 700;

	private CallerGUI      contactGUI;
	private final DlgContact dlgContact = new DlgContact(this);

	public _MailManager()
		{
		super(Access.MM, APP_WIDTH, APP_HEIGHT);
		buildContent();
		buildMenus();
		contactGUI.onConnect();
		setVisible(true);
		}

	final JTabbedPane tabbedPane = new JTabbedPane();
	protected void buildContent()
		{
		contactGUI = dlgContact.getGUI();
		Report report = new Report(dlgContact);
		report.setBorder(LAF.getStandardBorder());
		getContentPane().add( report );
		}

	protected void buildMenus()
		{
		SBMenuBar menuBar = new SBMenuBar();
		JFrame f = this;

		menuBar.addMenu( "File",
			new SBViewLogAction(f),
			null,
			LAF.setExitAction(new com.shanebow.ui.SBExitAction(f)
				{
				public void doApplicationCleanup()
					{
					disconnect();
					}
				}));
		menuBar.addEditMenu();
		menuBar.addMenu("Settings",
			LAF.getPreferencesAction(), null,
			menuBar.getThemeMenu());
		menuBar.addMenu("Help",
			new SBActHelp(), null,
			new SBAboutAction(f, SBProperties.getInstance()));
		setJMenuBar(menuBar);
		}

	public static void main( String[] args )
		{
		SBProperties.load(_MailManager.class, "com/apo/apo.properties");
		LAF.initLAF(blowUp, true);
Access._serverIP = Access.IP_APO_REMOTE;
		Access.parseCmdLine(args);
		JFrame frame = new _MailManager();
		}
	} // 272
