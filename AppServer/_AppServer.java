package com.apo.apps.AppServer;
/********************************************************************
* @(#)_AppServer.java 1.00 07/22/10
* Copyright © 2010-2013 by Richard T. Salamone, Jr. All rights reserved.
*
* _AppServer: Entry point to launch the GUI server. First displays the
* configuration dialog then runs the specified server type in the
* background with the desired options. This class also creates the
* monitor frame and menus and specifies high level command actions.
*
* @author Rick Salamone
* 20130923 rts removed old SBMenu reference in order to compile
*******************************************************/
import com.apo.admin.DlgCfgEmail;
import com.apo.employee.DlgUser;
import com.apo.apps.AppServer.monitor.*;
import com.apo.apps.AppServer.rox.*;
import com.apo.net.Access;
import com.shanebow.dao.DBStatement;
import com.shanebow.ui.SBDialog;
import com.shanebow.ui.menu.*;
import com.shanebow.ui.SBTextPanel;
import com.shanebow.ui.LAF;
import com.shanebow.util.SBDate;
import com.shanebow.util.SBLog;
import com.shanebow.util.SBProperties;
import java.awt.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;

public final class _AppServer
	extends JFrame
	implements ActionListener
	{
	private static final boolean AUTOEMAIL=true;
	private static long blowUp = 0; // SBDate.toTime("20101231  23:59");

	// commands specific to this app
	public static final String CMD_CFG_EMAIL="eMail Settings...";
	public static final String CMD_CFG_USERS="Users...";
	private final SBTableFontAction fTableRowSizeAction = new SBTableFontAction(this);
	private final LogoutAllAction fLogoutAllAction = new LogoutAllAction();
	private final NekoAction fNekoAction = new NekoAction();

	// Instance data
//	private Server m_server;

	_AppServer()
		{
		super();
		SBProperties props = SBProperties.getInstance();
		props.setProperty("app.name", (Access.isFlipper()? "Flipper":"Roxie"));
		setTitle(props.getProperty("app.name")
		        + " " + props.getProperty("app.version"));
		setBounds(props.getRectangle("usr.app.bounds", 25, 25, 820, 640));

		DBStatement.connect( "", "" );

		buildContent(getContentPane());
		buildMenus();
		setVisible(true);
		com.apo.admin.Emailer.setEnabled(AUTOEMAIL);
		SBLog.write( "Auto Email: " + (AUTOEMAIL?"enabled":"Disabled"));

//		m_server = launchServer( 1024 );
		launchServer( 1024 );
		}

	protected void buildContent(Container container)
		{
		container.setLayout(new BorderLayout());
		container.add( mainPanel(), BorderLayout.CENTER);
//		container.add( btnPanel(), BorderLayout.SOUTH );
		}

	private JComponent mainPanel()
		{
		SBProperties props = SBProperties.getInstance();
		int size = props.getInt(fTableRowSizeAction.SIZE_PROP_NAME, 12);

		ImageIcon icon = new ImageIcon("image/byellow.gif");
		final JTabbedPane tabbedPane = new JTabbedPane();

		for ( Monitor m : Monitors.all )
			{
			JComponent tab = monitorPane(m, size);
			tabbedPane.addTab(m.getTitle(), icon, tab, m.getToolTip());
			}
		return tabbedPane;
		}

	private final JComponent monitorPane(Monitor m, int aFontSize)
		{
		return createTable(m.getModel(), aFontSize);
		}

	private final JScrollPane createTable(UsrTableModel model, int aFontSize)
		{
		MonitorTable table = new MonitorTable(model);
		fTableRowSizeAction.addChangeListener(table);
		table.setFontSize( aFontSize );
		JScrollPane scrollpane = new JScrollPane(table);
		scrollpane.setBorder(new BevelBorder(BevelBorder.LOWERED));
		return scrollpane;
		}

	protected void buildMenus()
		{
		SBMenuBar menuBar = new SBMenuBar();
		JFrame f = this;

		menuBar.addMenu( "File",
			new SBViewLogAction(f),
			fNekoAction,
			fLogoutAllAction,
			null,
			LAF.setExitAction(new com.shanebow.ui.SBExitAction(f)
				{
				public void doApplicationCleanup()
					{
					fLogoutAllAction.actionPerformed(null);
					try { Thread.sleep(1000); }
					catch (Exception e) {}
					// m_server.cancel();
					DBStatement.disconnect();
					}
				}));
		menuBar.addMenu( this, "Configure",
			CMD_CFG_EMAIL,
			null,
			CMD_CFG_USERS,
			null,
			fTableRowSizeAction,
			menuBar.getThemeMenu());
		menuBar.addMenu( "Help",
			new SBAboutAction(f, SBProperties.getInstance()));
		setJMenuBar(menuBar);
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
		if ( cmd.equals(CMD_CFG_EMAIL))      cmdCfgEmail();
		else if ( cmd.equals(CMD_CFG_USERS)) cmdCfgUsers();
		}

	private Runnable launchServer( int backlog )
		{
		Runnable server = null;
		try
			{
			if (Access._mode.startsWith("rox"))     server = new Rox( backlog );
/************
			else if (Access._mode.startsWith("N1")) server = new N1( backlog );
			else if (Access._mode.startsWith("N2")) server = new N2( backlog );
************/
			new Thread( server ).start();
			SBLog.write( "started server " + server );
// if (Access.isFlipper()) ((Rox)server).initiateConnection("127.0.0.1", 7474);
// if (Access.isFlipper()) ((Rox)server).initiateConnection(Access.IP_APO_REMOTE, 8080);
			}
		catch ( Throwable t )
			{
			SBDialog.fatalError( "Could not launch\n" + Access._mode + ":\n" + t.getMessage());
			}
		return server;
		}

	/**
	* @param args the command line arguments
	*/
	public static void main( String[] args )
		{
		SBProperties.load(_AppServer.class, "com/apo/apo.properties");
		LAF.initLAF(blowUp, true);
		Access._mode = "rox"; // the default mode
		Access.parseCmdLine(args);
		JFrame frame = new _AppServer();
		frame.setVisible(true);
		}
	}
