package com.apo.apps.miner.DM;
/********************************************************************
* @(#)_DM.java	1.00 20100524
* Copyright (c) 2010-2011 by Richard T. Salamone, Jr. All rights reserved.
*
* _DM: The entry point and main container frame for the
* manual Data Mining application.
*
* @author Rick Salamone
* @version 1.00 20100524 rts created
* @version 1.01 20100526 rts added Edit menu draggable text fields
* @version 1.03 20100615 rts using DMContactPanel instead of ContactPanel
* @version 1.04 20100621 rts moved log to the help menu
* @version 1.05 20100623 rts improved error check & report
* @version 1.06 20100627 rts country ids, and login checking
* @version 1.07 20100629 RTS adding clint/server operation
* @version 1.08 20100629 rts maintenance & lincense extention
* @version 1.09 20100629 rts fixed login bug - Source & User stepping on toes
* @version 1.11 20100718 rts maintenance & lincense extention
* @version 1.12 20100728 rts first network version
* @version 1.15 20100728 rts added several country codes
* @version 1.16 20100901 rts now writes to a backup file as well as main csv
* @version 1.17 20101003 rts modified for history, new import file structure
* @version 1.18 20101005 rts insists on web site
* @version 1.19 20101005 rts bug fix to login
* @version 1.20 20101005 rts client disconnect upon exit
* @version 1.21 20101031 rts title displays version, login displays title
* @version 1.22 20101116 rts improved BackedContactList
* @version 1.23 20101125 rts email field width increased from 30 to 100
* @version 1.24 20110112 rts using SBMenuBar & SBProperties
* @version 2.01 20110112 rts send without wait for reply
*******************************************************/
import com.apo.contact.Raw;
import com.apo.contact.edit.*;
import com.apo.contact.touch.TouchCode;
import com.apo.employee.Role;
import com.apo.net.Access;
import com.apo.net.*;
import com.apo.net.client.ClientApp;
import com.shanebow.ui.SBDialog;
import com.shanebow.ui.menu.*;
import com.shanebow.ui.SBAction;
import com.shanebow.ui.LAF;
import com.shanebow.util.SBDate;
import com.shanebow.util.SBLog;
import com.shanebow.util.SBProperties;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.shanebow.dao.*; // only for add action

public class _DM
	extends ClientApp
	{
	private static long blowUp = 0; // SBDate.toTime("20101231  23:59");
	static final int    APP_WIDTH = 290;
	static final int    APP_HEIGHT = 400;

	private final LabeledCounter fAdds = new LabeledCounter("Adds");
	private final LabeledCounter fDups = new LabeledCounter("Dups");
	private RawPanel    m_fields;

	private final SBAction fAddAction = new SBAction("Add", 'A',
		"Add a new contact to the system", "next" )
		{
		@Override public void actionPerformed(ActionEvent e) { cmdContactAdd(); }
		};

	private final SBAction fDataSource = new SBAction("Data Source", 'D',
		"Specify where the current contact comes from", null )
		{
		@Override public void actionPerformed(ActionEvent e) { cmdWebSite(); }
		};

	public _DM()
		{
		super(Access.DM, APP_WIDTH, APP_HEIGHT);
		Role role = Access.getRole();
		m_fields = new RawPanel(role.layoutColumns(), role.rawFields(), null);
		buildContent();
		buildMenus();
		setVisible(true);
		cmdWebSite();
		getContentPane().invalidate();
		}

	protected void buildContent()
		{
		getContentPane().add( mainPanel(), BorderLayout.CENTER );
//		getContentPane().add( btnPanel(), BorderLayout.SOUTH );
		}

	private JComponent mainPanel()
		{
		JPanel p = new JPanel( new BorderLayout());
		p.setBorder(LAF.getStandardBorder());
		p.add( m_fields, BorderLayout.CENTER);
		p.add( btnPanel(), BorderLayout.SOUTH);
		return p;
		}

	private JPanel btnPanel()
		{
		JPanel p = new JPanel();
		JButton btnAdd = fAddAction.makeButton();
		Dimension edgeSpacer = new Dimension(5, 0);
		p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
		p.add(Box.createRigidArea(edgeSpacer));
		p.add( fAdds );
		p.add(Box.createRigidArea(edgeSpacer));
		p.add( fDups );
		p.add(Box.createHorizontalGlue());
		p.add( btnAdd );
		p.add(Box.createRigidArea(edgeSpacer));
		getRootPane().setDefaultButton(btnAdd);
		return p;
		}

	protected void buildMenus()
		{
		SBMenuBar menuBar = new SBMenuBar();
		JFrame f = this;

		menuBar.addMenu("File",
			fDataSource, fAddAction, null,
			new SBViewLogAction(f), null,
			LAF.setExitAction(new com.shanebow.ui.SBExitAction(f)
				{
				public void doApplicationCleanup() { disconnect(); }
				}));
		menuBar.addEditMenu();
		menuBar.add(menuBar.getThemeMenu());
		menuBar.addMenu("Help",
			new SBActHelp(), null,
			new SBAboutAction(f, SBProperties.getInstance()));
		setJMenuBar(menuBar);
		}

	public void cmdContactAdd()
		{
		Raw raw = m_fields.getEdited();
		if ( raw != null )
			{
			// raw.dump("New Record");
			try
				{
				// Use addRaw for no waiting
				// Use addLead to wait for a reply with duplicate exception
				Raw.DAO.addRaw(raw, Access.empID(), TouchCode.MINED, m_host);
				fAdds.increment();
				m_fields.setContact(Raw.BLANK);
				m_fields.set( Raw.COUNTRYID, raw.country());
				}
			catch (DataFieldException dfe) { SBDialog.error("Data Access Error", dfe.getMessage()); }
			}
		}

	/**
	* If the add was a duplicate, it will come back as an unsolicited error
	*/
	@Override protected void error(Message msg)
		{
		byte op = msg.op();
		if ( op == ClientOp.CODE_ADDLEAD )
			{
			fAdds.decrement();
			fDups.increment();
			}
		else super.unsolicited(msg);
		}

	private void cmdWebSite()
		{
		String inputValue = Access._mode;

		while (( inputValue == null ) || !setSite(inputValue))
			{
			inputValue = JOptionPane.showInputDialog( this,
				"Please paste in the\n web site that you are mining",
				"Change Web Site", JOptionPane.INFORMATION_MESSAGE ); 
			}
		setTitle( LAF.getDialogTitle(m_host));
		Access._mode = null;
		}

	private String m_host = "";
	public boolean setSite(String site)
		{
		try
			{
			java.net.URL u = new java.net.URL(site);
			m_host = u.getHost();
			com.shanebow.util.SBLog.write( "Mining host: " + m_host );
			return true;
			}
		catch ( Exception e ) { return false; }
		}

	public static void main( String[] args )
		{
		SBProperties.load(_DM.class, "com/apo/apo.properties");
		LAF.initLAF(blowUp, true);
		Access.parseCmdLine(args);
		JFrame frame = new _DM();
		}
	}
