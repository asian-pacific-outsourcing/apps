package com.apo.apps.RawAdmin;
/********************************************************************
* @(#)_RawAdmin.java 1.00 20100523
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* _RawAdmin: The entry point and main container frame for the RawAdmin app
*
* @author Rick Salamone
* @version 1.00 20100523 rts created
* @version 1.00 20100529 rts added bulk import
* @version 1.00 20100531 rts added export
* @version 1.01 20100601 rts implemented auto increment contact.id during import
* @version 1.02 20100603 rts added confirmation prior to database create command
* @version 1.03 20100603 rts added query menu with four items
* @version 1.04 20100610 rts sorts checkouts on dispo DESC then id ASC
* @version 1.05 20100620 rts redesign ui, no more split panes
* @version 1.06 20100621 rts moved the log to the help menu
* @version 1.07 20100627 rts country checkout & major interface change to table
* @version 1.08 20100702 rts country in check out page header
* @version 1.09 20100702 rts moved checkout available to check out wizard
* @version 1.10 20100705 rts added dispo UD
* @version 1.11 20100718 rts maintenance & lincense extention
* @version 1.12 20100821 rts bulk update uses FileWorker/FileLinerParser model
* @version 1.13 20100824 rts add command to view import fails
* @version 1.14 20100824 rts add first cut at Lead checkout
* @version 1.15 20100824 rts second cut of lead checkout
* @version 1.16 20100829 rts bulk check in (formerly bulk update) skips blanks
* @version 1.17 20100829 rts added email, position, company to contacts table
* @version 1.18 20100830 rts fixed CSV split to trim fields which fixed dups import bug
* @version 1.19 20100901 rts new code for lead check out
* @version 1.20 20100924 rts removed licensee, fixed missing users & bulk update
* @version 1.21 20101003 rts added explicit DB connect/disconnect calls
* @version 1.22 20101004 rts maint menu create table modified to create new touch table
* @version 1.23 20101008 rts UI to query touch codes - demo, not implemented
* @version 1.24 20101010 rts UI to query touch codes works
* @version 1.25 20101020 rts Using Access.parseCmdLine - need uid for touches
* @version 1.26 20101031 rts debug contact query
* @version 1.27 20101031 rts fixed Master Bobby lead checkout
* @version 1.28 20101107 rts auto email
* @version 1.29 20101108 rts save main table as csv
* @version 1.30 20101108 rts bug fix for blank source id not allowed
* @version 1.31 20101122 rts email config removes control chars
* @version 1.32 20110113 rts using SBMenuBar & SBProperties
* @version 1.32 20110606 rts packaged checkout actions to facillitate net operation
*******************************************************/
import com.apo.contact.Raw;
import com.apo.contact.report.Report;
import com.apo.order.Order;
import com.apo.contact.touch.Touch;
import com.apo.admin.OrderDBDAO;
import com.apo.admin.RawDBDAO;
import com.apo.admin.TouchDBDAO;
import com.apo.admin.SysDBDAO;
import com.apo.employee.DlgUser;
import com.apo.admin.DlgCfgEmail;
import com.apo.net.Access;
import com.shanebow.ui.SBDialog;
import com.shanebow.ui.menu.*;
import com.shanebow.ui.LAF;
import com.shanebow.util.SBDate;
import com.shanebow.util.SBProperties;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class _RawAdmin extends JFrame
	implements ActionListener
	{
	private static long blowUp = 0; // SBDate.toTime("20101231  23:59");
	static final String APP_NAME = "Contact Administrator";

	public static final String CMD_EXPORT="Export Contacts To...";
	public static final String CMD_BULK_UPDATE="Bulk Update Contacts...";
	public static final String CMD_CHECKIN_BULK="Bulk CheckIn";
	public static final String CMD_DBTABLE_CREATE="Create Database...";
	public static final String CMD_CFG_EMAIL="eMail Settings...";
	public static final String CMD_CFG_USERS="Users...";

	public _RawAdmin()
		{
		super();
		SBProperties props = SBProperties.getInstance();
		setTitle(props.getProperty("app.name")
		        + " " + props.getProperty("app.version"));
		setBounds(props.getRectangle("usr.app.bounds", 25, 25, 1300, 900));

		new SysDBDAO();
		Raw.DAO = new RawDBDAO();
		Touch.DAO = new TouchDBDAO();
		Order.DAO = new OrderDBDAO();
		buildContent();
		buildMenus();
		com.apo.admin.Emailer.setEnabled(true);
		setTransferHandler(new RawDragDrop());
		}

	protected void buildContent()
		{
		getContentPane().add(new Report(new com.apo.apps.caller.DlgContact(this)));
//		getContentPane().add(new Report(new DlgContact(this)));
		}

	protected void buildMenus()
		{
		SBMenuBar menuBar = new SBMenuBar();
		JFrame f = this;

		menuBar.addMenu(this, "File",
			new com.apo.apps.RawAdmin.rawimport.ActImport(),
			new com.apo.apps.RawAdmin.rawimport.ActViewImportFailures(),
			null,
			CMD_EXPORT, null,
//			CMD_BULK_UPDATE, null,
			new com.apo.net.SQLAction(), null,
			new SBViewLogAction(f), null,
CMD_DBTABLE_CREATE,null,
			LAF.setExitAction(new com.shanebow.ui.SBExitAction(f)
				{
				public void doApplicationCleanup()
					{
					Raw.DAO.shutdown();
					}
				}));
		menuBar.addMenu(this, "CheckOut",
			new com.apo.contact.checkout.ActTQCheckOut(),
			new com.apo.contact.checkout.ActNextPage(),
			null,
			CMD_CHECKIN_BULK, null,
			new com.apo.contact.checkout.ActTQUndoCheckOut());
		menuBar.addMenu(this, "Settings",
			CMD_CFG_USERS, CMD_CFG_EMAIL, null,
//			ContactTableDelMenuItem.getInstance(), null,
			LAF.getPreferencesAction(), null,
			menuBar.getThemeMenu());
		menuBar.addMenu("Help",
			new SBActHelp(), null,
			new SBAboutAction(f, SBProperties.getInstance()));
		setJMenuBar(menuBar);
		}

	public void cmdBulkUpdate()
		{
		new BulkUpdateWiz().onBrowseFile();
		}

	public void cmdCheckInBulk()
		{
		new CheckInBulkWiz().onBrowseFile();
		}

	public void cmdExport()
		{
		ExportContacts.prompt();
		}

	private void cmdCfgEmail()
		{
		DlgCfgEmail.launch();
		}

	private void cmdCfgUsers()
		{
		new DlgUser(this).setVisible(true);
		}

	public void actionPerformed(ActionEvent e)
		{
		String cmd = e.getActionCommand();
		if ( cmd.equals(CMD_BULK_UPDATE))           cmdBulkUpdate();
		else if ( cmd.equals(CMD_CHECKIN_BULK ))    cmdCheckInBulk();
		else if ( cmd.equals(CMD_EXPORT))           cmdExport();
		else if ( cmd.equals(CMD_CFG_USERS))        cmdCfgUsers();
		else if ( cmd.equals(CMD_CFG_EMAIL))        cmdCfgEmail();
		else if ( cmd.equals(CMD_DBTABLE_CREATE))
			{
		/*********
			if ( !SBDialog.confirm( "This will permanently empty the contact database!!\nProceed?" ))
				return;
			try
				{
				DBSchema.dropTable(Raw.DB_TABLE);
				DBSchema.createTable(Raw.DB_TABLE, DBSchema.TBL_CONTACT_SCHEMA);
				}
			catch ( Exception ex ) { SBDialog.error(APP_NAME, "Error creating tables:\n" + ex); }
			if ( !SBDialog.confirm( "This will permanently empty the history database!!\nProceed?" ))
				return;
			try
				{
				DBSchema.dropTable(Touch.DB_TABLE);
				DBSchema.createTable(Touch.DB_TABLE, DBSchema.TBL_TOUCH_SCHEMA);
				}
			catch ( Exception ex ) { SBDialog.error(APP_NAME, "Error creating tables:\n" + ex); }
		*********/
			if ( !SBDialog.confirm( "This will permanently empty the order database!!\nProceed?" ))
				return;
			try
				{
				DBSchema.dropTable(Order.DB_TABLE);
				DBSchema.createTable(Order.DB_TABLE, DBSchema.TBL_ORDER_SCHEMA);
				}
			catch ( Exception ex ) { SBDialog.error(APP_NAME, "Error creating tables:\n" + ex); }
			}
		}

	public static void main( String[] args )
		{
		SBProperties.load(_RawAdmin.class, "com/apo/apo.properties");
		LAF.initLAF(blowUp, true);
		Access._login = "Shane";
		Access.setUID(com.apo.employee.User.parse(Access._login).id());
		Access.setRole(com.apo.employee.Role.MGRAD);
		Access.parseCmdLine(args);
		JFrame frame = new _RawAdmin();
		frame.setVisible(true);
		}
	}
