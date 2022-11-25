package com.apo.apps.caller;
/********************************************************************
* @(#)CallerApp.java 1.00 20100728
* Copyright (c) 2010-2011 by Richard T. Salamone, Jr. All rights reserved.
*
* CallerApp: The main container frame for the various caller roles.
* Currently these roles include TQ, VO, Tingler, and AO. The client
* app for these roles are essentially the same: After logging in to
* the server, the user is repeatedly given contacts to call, edit,
* disposition, and schedule for call back. The default operations,
* visible/editable fields, allowed disposition, and fetch criteria are
* configured via a properties file for the given role. User preferences
* and server side configration may override or suppliment these settings.
*
* For the developer, coding a new caller role is very straight forward.
* First any new dispositions that may be assigned are created in Dispo
* class, and any new user privledges are created in the Access class.
*
* Then on the client side, a properties file and a simple class are
* coded: This class file has only a main method to load the properties
* and instantiate an object of this class. That's it!
*
* On the server side, a monitor must be created for the new user role
* that tracks and manages the users logged in as this role, and displays
* production counts.
*
* @author Rick Salamone
* @version 1.00 20101020 rts first Verifier demo version
* @version 1.01 20101023 rts sends comment to server
* @version 1.02 20101024 rts added history tab
* @version 1.03 20101031 rts title displays version, login displays title
* @version 1.04 20101117 rts decoupled the contact gui from main app
* @version 1.05 20110113 rts using SBMenuBar & SBProperties
* @version 2.00 20110118 rts Now a configurable frame shared by TQ, VO, AO
* @version 2.01 20110131 rts Updated for Message version 3
* @version 2.02 20110406 rts setVisible before gui.onConnect()
* @version 2.03 20110424 rts override unsolicited(Message) for order processing
*******************************************************/
import com.apo.apps.caller.CallerGUI;
import com.apo.apps.caller.Titled;
import com.apo.net.Access;
import com.apo.net.client.*;
import com.shanebow.ui.SBDialog;
import com.shanebow.ui.menu.*;
import com.shanebow.ui.SBTextPanel;
import com.shanebow.ui.LAF;
import com.shanebow.util.SBDate;
import com.shanebow.util.SBLog;
import com.shanebow.util.SBProperties;
import java.awt.*;
import javax.swing.*;

// for order processing...
import com.apo.net.ClientOp;
import com.apo.net.Message;
import com.apo.order.Order;

public class CallerApp
	extends ClientApp
	implements Titled
	{
	public static void createAndShowGUI(final long access)
		{
		Access._role = access;
		EventQueue.invokeLater(new Runnable()
			{
			public void run() { new CallerApp(access); }
			});
		}

	@Override protected void unsolicited(Message msg)
		{
		byte op = msg.op();
		if ( op == ClientOp.CODE_NEWORDER
		||   op == ClientOp.CODE_ORDMOD )
			{
			try
				{
				Order order = new Order(msg.data());
log("Received order: " + order.title());
				}
			catch(Exception e) { log("Error receiving a pending order: " + e); }
			}
		else super.unsolicited(msg);
		}

	// PRIVATE //
	private static boolean scriptEnabled=false;
	private final CallerGUI contactGUI;

	private CallerApp(long access)
		{
		super(access, 600, 450);
		contactGUI = new CallerGUI(this);
		buildContent();
		buildMenus();
		setVisible(true);
		contactGUI.onConnect();
		}

	protected void buildContent()
		{
		JPanel content = new JPanel(new BorderLayout());
		content.add( contactGUI, BorderLayout.CENTER );
		content.add( contactGUI.getControlPanel(), BorderLayout.SOUTH );
		content.setBorder(LAF.getStandardBorder());
		setContentPane(content);
		}

	protected void buildMenus()
		{
		SBMenuBar menuBar = new SBMenuBar();
		JFrame f = this;

		menuBar.addMenu( "File",
			new SBViewLogAction(f), null,
			LAF.setExitAction(new com.shanebow.ui.SBExitAction(f)
				{
				public void doApplicationCleanup()
					{
					disconnect();
					}
				}));
		menuBar.addEditMenu();
		menuBar.add(menuBar.getThemeMenu());
		menuBar.addMenu("Contact", contactGUI.getMenuItems());
		menuBar.addMenu("Help",
			new SBActHelp(), null,
			new SBAboutAction(f, SBProperties.getInstance()));
		setJMenuBar(menuBar);
		}
	}
