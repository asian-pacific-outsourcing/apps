package com.apo.apps.caller.LO;
/********************************************************************
* @(#)_LO.java 1.00 20100423
* Copyright (c) 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* _LO: The entry point and main container frame for the Loader application.
*
* @author Rick Salamone
* @version 1.00 20100423 rts first demo version
*******************************************************/
import com.apo.apps.caller.CallerGUI;
import com.apo.apps.caller.DlgContact;
import com.apo.net.Access;
import com.apo.net.client.ClientApp;
import com.apo.order.*;
import com.apo.contact.report.Report;
import com.shanebow.ui.menu.*;
import com.shanebow.ui.LAF;
import com.shanebow.util.SBDate;
import com.shanebow.util.SBProperties;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// for order processing...
import com.apo.net.ClientOp;
import com.apo.net.Message;

public final class _LO
	extends ClientApp
	{
	private static final long blowUp = 0; // SBDate.toTime("20101231  23:59");

	private final DlgContact dlgContact = new DlgContact(this);

	public _LO()
		{
		super(Access.LO, 950, 700);
		buildContent();
		buildMenus();
		CallerGUI.getInstance().onConnect();
		setVisible(true);
		}

	protected void buildContent()
		{
		getContentPane().add( mainPanel());
		}

	private JComponent mainPanel()
		{
		JTabbedPane tabbedPane = new JTabbedPane();
		ImageIcon icon = new ImageIcon("image/byellow.gif");
		tabbedPane.addTab("Contacts", icon, new Report(dlgContact), "Contact Listing" );
		return tabbedPane;
		}

	@Override protected void unsolicited(Message msg)
		{
		byte op = msg.op();
		if ( op == ClientOp.CODE_NEWORDER
		||   op == ClientOp.CODE_ORDMOD )
			{
			}
		else super.unsolicited(msg);
		}

	protected void buildMenus()
		{
		SBMenuBar menuBar = new SBMenuBar();
		JFrame f = this;
JMenuItem viewDlg = new JMenuItem("Contact Dialog");
viewDlg.addActionListener( new ActionListener()
	{
	public void actionPerformed(ActionEvent e)
		{
		dlgContact.setVisible(true);
		}
	});
		menuBar.addMenu( "File",
viewDlg,
			new SBViewLogAction(f), null,
			LAF.setExitAction(new com.shanebow.ui.SBExitAction(f)
				{
				public void doApplicationCleanup() { disconnect(); }
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
		SBProperties.load(_LO.class, "com/apo/apo.properties");
SBProperties.getInstance().dump();
		LAF.initLAF(blowUp, true);
		Access.parseCmdLine(args);
		new _LO();
		}
	} // 272
