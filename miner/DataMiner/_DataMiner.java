package com.apo.apps.miner.DataMiner;
/********************************************************************
* @(#)_DataMiner.java	1.00 20100524
* Copyright (c) 2010-2011 by Richard T. Salamone, Jr. All rights reserved.
*
* _DataMiner: The entry point and main container frame for the
* manual Data Mining application.
*
* @author Rick Salamone
* @version 1.00 20100524 rts
* 20100526 RTS 1.02 added Edit menu
* 20100615 RTS 1.04 using DMContactPanel instead of ContactPanel
* 20100621 RTS 1.06 moved log to the help menu
* 20100623 RTS 1.06 improved error check & report
* 20100627 RTS 1.07 country ids, and login checking
* 20100629 RTS adding clint/server operation
* 20100629 RTS 1.08 maintenance & lincense extention
* 20100629 RTS 1.09 fixed login bug - Source & User stepping on toes
* 20100718 RTS 1.11 maintenance & lincense extention
* 20100728 RTS 1.12 non-network maintenance release
* 20100728 RTS 1.15 added several country codes
* 20100901 RTS 1.16 now writes to a backup file as well as main csv
* 20101003 RTS 1.17 modified for history, new import file structure, no blow up
* 20101005 RTS 1.19 bug fix to login
* 20101101 RTS 1.20 sends date to CSV create
* 20101116 RTS 1.22 improved BackedContactList
* @version 1.23 20110113 rts using SBMenuBar & SBProperties
*******************************************************/
import com.apo.contact.Raw;
import com.apo.contact.BackedContactList;
import com.apo.contact.edit.RawPanel;
import com.apo.net.Access;
import com.shanebow.ui.SBDialog;
import com.shanebow.ui.menu.*;
import com.shanebow.ui.SBTextPanel;
import com.shanebow.ui.LAF;
import com.shanebow.util.SBDate;
import com.shanebow.util.SBLog;
import com.shanebow.util.SBProperties;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class _DataMiner extends JFrame
	implements ActionListener
	{
	protected static long blowUp = 0; // SBDate.toTime("20111231  23:59");
	static final int    APP_WIDTH = 290;
	static final int    APP_HEIGHT = 400;

	public static final String CMD_CONTACT_ADD="Add >";
	public static final String CMD_WEB_SITE="Change Site...";
	public static final String CMD_CLEAR="Clear";

	private BackedContactList m_contacts = new BackedContactList(
		                           new JLabel("  0", JLabel.RIGHT),
		                           Raw.DMCSV_HEADER);
	private final RawPanel    m_fields;

	public _DataMiner()
		{
		super();
		SBProperties props = SBProperties.getInstance();
		setTitle(props.getProperty("app.name")
		        + " " + props.getProperty("app.version"));
		setBounds(props.getRectangle("usr.app.bounds",
		                         25, 25, APP_WIDTH, APP_HEIGHT));
		m_fields = new RawPanel((byte)1, new int[] {Raw.NAME, Raw.POSITION,
			Raw.COMPANY, Raw.PHONE, Raw.MOBILE, Raw.ALTPHONE, Raw.EMAIL,
			Raw.ADDRESS, Raw.COUNTRYID, Raw.TYPE }, null);
		buildContent();
		buildMenus();
		cmdLogin();
		setVisible(true);
		getContentPane().invalidate();
		}

	protected void cmdLogin()
		{
		short m_uid = new DlgLogin(this, Access.DM ).getUID();
		m_contacts.setFilespec("", SBDate.yyyymmdd(), Access._login );
		cmdWebSite();
		}

	protected void buildContent()
		{
		getContentPane().add( mainPanel(), BorderLayout.CENTER );
//		getContentPane().add( btnPanel(), BorderLayout.SOUTH );
		}

	private JComponent mainPanel()
		{
		ImageIcon icon = new ImageIcon("image/byellow.gif");
		final JTabbedPane tabbedPane = new JTabbedPane();
		
		tabbedPane.addTab("Contact", icon, tabContact(), "Data entry screen" );
		tabbedPane.addTab("List",    icon, m_contacts,   "Contacts Mined" );
		return tabbedPane;
		}

	private JPanel tabContact()
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
		JButton btnAdd = makeButton(CMD_CONTACT_ADD);
		Dimension edgeSpacer = new Dimension(5, 0);
		p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
		p.add(Box.createRigidArea(edgeSpacer));
		p.add( new JLabel( "Contacts Mined:" ));
		p.add(Box.createRigidArea(edgeSpacer));
		p.add( m_contacts.getCounterLabel());
		p.add(Box.createHorizontalGlue());
		p.add( btnAdd );
		p.add(Box.createRigidArea(edgeSpacer));
		getRootPane().setDefaultButton(btnAdd);
		return p;
		}

	private JButton makeButton(String caption)
		{
		JButton b = new JButton(caption);
		b.setMnemonic(caption.charAt(0));
		b.addActionListener(this);
		return b;
		}

	protected void buildMenus()
		{
		SBMenuBar menuBar = new SBMenuBar();
		JFrame f = this;

		menuBar.addMenu(this, "File",
			CMD_WEB_SITE, null,
			new SBViewLogAction(f),
			null,
			LAF.setExitAction(new com.shanebow.ui.SBExitAction(f)
				{
				public void doApplicationCleanup() {}
				}));
		menuBar.addEditMenu();
		menuBar.add(menuBar.getThemeMenu());
		menuBar.addMenu("Help",
			new SBActHelp(), null,
			new SBAboutAction(f, SBProperties.getInstance()));
		setJMenuBar(menuBar);
		}

	public void actionPerformed(ActionEvent e)
		{
		String cmd = e.getActionCommand();
		if ( cmd.equals(CMD_CONTACT_ADD))    cmdContactAdd();
		else if ( cmd.equals(CMD_WEB_SITE))  cmdWebSite();
		}

	public void cmdContactAdd()
		{
		Raw edited = m_fields.getEdited(); // displays problems to user
		if ( edited != null )
			{
			String csv = "" + SBDate.timeNow() + "," + Access.getUID()
			           + "," + m_host + "," + edited.toCSV();
			SBLog.write ( "Add Contact" );
			m_contacts.add(csv);
			m_fields.setContact(null);
			}
		}

	private void cmdWebSite()
		{
		String inputValue = null;
		do
			{
			inputValue = JOptionPane.showInputDialog( this,
				"Please paste in the\n web site that you are mining",
				"Change Web Site", JOptionPane.INFORMATION_MESSAGE ); 
			}
		while (( inputValue == null ) || !setSite(inputValue));
		setTitle( LAF.getDialogTitle(m_host));
		}

	private String m_host = "";
	public boolean setSite(String site)
		{
		try
			{
			java.net.URL u = new java.net.URL(site);
			m_host = u.getHost();
			SBLog.write( "Mining host: " + m_host );
			return true;
			}
		catch ( Exception e ) { return false; }
		}

	public static void main( String[] args )
		{
		SBProperties.load(_DataMiner.class, "com/apo/apo.properties");
		LAF.initLAF(blowUp, true);
		new _DataMiner();
		}
	}
