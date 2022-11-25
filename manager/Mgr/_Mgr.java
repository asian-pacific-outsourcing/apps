package com.apo.apps.manager.Mgr;
/********************************************************************
* @(#)_Mgr.java 1.00 20100728
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* _Mgr: The entry point and main container frame for the
* AO & LO manager application.
*
* @author Rick Salamone
* @version 1.00 20101020 rts first demo version
* @version 1.01 20101023 rts sends comment to server
* @version 1.02 20101024 rts added history tab
* @version 1.03 20101031 rts title displays version, login displays title
* @version 1.04 20101117 rts decoupled the contact gui from main app
* @version 1.05 20110113 rts using SBMenuBar & SBProperties
* @version 1.06 20110403 rts added support for pending orders
*******************************************************/
import com.apo.apps.caller.CallerGUI;
import com.apo.apps.caller.DlgContact;
import com.apo.apps.manager.Mgr.assign.AssignWork;
import com.apo.contact.Dispo;
import com.apo.employee.Role;
import com.apo.net.Access;
import com.apo.net.client.ClientApp;
import com.apo.net.client.BroadcastAction;
import com.apo.net.client.LogoutRoleAction;
import com.apo.net.client.EditBulletinAction;
import com.apo.net.client.NekoAction;
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

public final class _Mgr
	extends ClientApp
	{
	private static final long blowUp = 0; // SBDate.toTime("20101231  23:59");

	private final DlgContact dlgContact = new DlgContact(this);
	private OrderTableModel fPendingModel;

	public _Mgr()
		{
		super(Access.MGRAD, 950, 700);
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
		JTabbedPane tabbedPane = new com.shanebow.ui.SBTabbedPane();
		ImageIcon icon = new ImageIcon("image/byellow.gif");
		tabbedPane.addTab("Contacts", icon, new Report(dlgContact), "Contact Listing" );
		tabbedPane.addTab("Assignments",  icon, new AssignWork(), "Assign work to callers" );
		if ( Access.allowedTo(Access.AO|Access.LO))
			tabbedPane.addTab("Orders",  icon, new OrderReport(dlgContact), "Orders Listing" );
		int uid = Access.getUID();
		if ( uid == 7 || uid == 8 || uid == 13 )
			{
			Dispo dispo = null;
			if ( uid == 13 ) dispo = Dispo.RMP;
			if ( uid == 8 || uid == 7 ) dispo = Dispo.ADP;

// Debug only
OrderStatus nextStates[] = OrderStatus.nextStates(dispo);
log("Next States:"); for ( OrderStatus os : nextStates ) log("..." + os );
			OrderStatus interestSet[] = OrderStatus.interestSet(dispo);
log("Interest Set:"); for ( OrderStatus os : interestSet ) log("..." + os );
			String interestCSV = "";
			for ( int i = 0; i < interestSet.length; i++ )
				interestCSV += ((i == 0)? "" : ",") + interestSet[i].dbRepresentation();
			OrderTable toDoTable = new OrderTable(dlgContact, "usr.order.report.");
			fPendingModel = (OrderTableModel)toDoTable.getModel();
			fPendingModel.setInterestSet(interestSet);
//			toDoTable.makeConfigurable();
			JPanel toDoPanel = new JPanel(new BorderLayout());
			toDoPanel.add(new JScrollPane(toDoTable), BorderLayout.CENTER);
			tabbedPane.addTab("Pending",  icon, toDoPanel, "Orders requiring action" );
			String sql = "SELECT * FROM " + Order.DB_TABLE
			           + " WHERE " + Order.dbField(Order.STATUS)
			           + " IN (" + interestCSV + ")"
			           + "\n ORDER BY " + Order.dbField(Order.ACTIVITY) + " DESC";
			try { fPendingModel.fetch( -1, sql ); }
			catch (Exception e) { log( "Error retrieving Pending orders: " + e ); }
			}
// long[] managerOf = Access.manages();
		for ( Role role : Role.getAll())
			{
			long roleAccess = role.access();
			if ( Access.allowedTo(roleAccess)
			&& ((roleAccess & Access.MGR) == 0 || (uid == 7))) // 7 get mgr screen
				tabbedPane.addTab(role.code(), icon,
				 new MonitorTable(role), role.shortName() + " Monitor" );
			}
		return tabbedPane;
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
// System.out.println("check order status " + order.status() + " in interest set");
				if ( !fPendingModel.inInterestSet(order))
					return;
				int index = fPendingModel.indexOf(order);
				if ( index >= 0 ) fPendingModel.set(index, order);
				else fPendingModel.add(0, order);
				showBulletin( order.title()
					+ " requires action and has been added to the pending list" );
				}
			catch(Exception e) { log("Error receiving a pending order: " + e); }
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
		if ( Access.allowedTo(Access.TQ))
			menuBar.addMenu("CheckOut",
				new com.apo.contact.checkout.ActTQCheckOut(),
//				new com.apo.contact.checkout.ActPurgeTQWorkQ(),
				new com.apo.contact.checkout.ActNextPage(),
				new com.apo.contact.checkout.ActTQUndoCheckOut());
/**********/
		menuBar.addMenu("Management",
			new BroadcastAction(this), null,
//			new EditBulletinAction(this), null,
			new LogoutRoleAction(this), null,
			new NekoAction(this));
/**********/
		if ( Access.getUID() == 7 ) menuBar.addMenu("DBA",
			new com.apo.net.SQLAction(), null,
			new com.apo.employee.edit.EditRoleAction());

		menuBar.addMenu("Settings",
			LAF.getPreferencesAction(), null,
			menuBar.getThemeMenu());
		menuBar.addMenu("Help",
			new SBActHelp(), null,
			new SBAboutAction(f));
		setJMenuBar(menuBar);
		}

	public static void main( String[] args )
		{
		SBProperties.load(_Mgr.class, "com/apo/apo.properties");
SBProperties.getInstance().dump();
		LAF.initLAF(blowUp, true);
		Access.parseCmdLine(args);
		new _Mgr();
		}
	} // 272
