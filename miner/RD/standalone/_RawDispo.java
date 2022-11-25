package com.apo.apps.RawDispo;
/********************************************************************
* @(#)_RawDispo.java	1.00 10/05/27
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* _RawDispo: The entry point and main container frame for the
* manual Dispositoning of Raw Data.
*
* NOTE: Dispositions are presented and edited using radio buttons -
* Since the DispoRenderer and DispoEditor are explicitly tied to column
* zero of the JTable, any SQL SELECT statements must have disposion as
* the first field selected.
*
* @version 1.00 05/27/10
* @author Rick Salamone
* 20100529 RTS Renders & edits dispositons using radio buttons
* 20100604 RTS Ability to select contacts by page or record number
* 20100610 RTS 1.01 change D dispo to PD and navigate to next page
* 20100613 RTS 1.02 fixed sort orders on the pages first by dispo then id
* 20100621 RTS 1.06 ui redesign, removed tabs & moved log to the help menu
* 20100627 RTS 1.07 login checking
* 20100629 RTS 1.08 maintenance & lincense extention
* 20100704 RTS 1.09 fixed login bug - Source & User stepping on toes
* 20100705 RTS 1.10 added dispo UD
* 20100708 RTS 1.11 dialog prompt for page number entry
* 20100820 RTS 1.12 modified to handle new datafield package structure
* 20101003 RTS 1.13 added explicit DB connect/disconnect calls
* 20101104 RTS 1.14 uses DFTable instead of JTable
* @version 1.15 20110113 rts using SBMenuBar & SBProperties
* @version 1.16 20110316 rts gets dispo choices from the TQ role object
*******************************************************/
import com.apo.contact.Raw;
import com.shanebow.dao.table.DFTable;
import com.apo.employee.Role;
import com.apo.net.Access;
import com.apo.admin.DlgLogin;
import com.shanebow.ui.SBDialog;
import com.shanebow.ui.menu.*;
import com.shanebow.ui.LAF;
import com.shanebow.util.SBDate;
import com.shanebow.util.SBProperties;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class _RawDispo
	extends JFrame
	implements ActionListener
	{
	private static long blowUp = SBDate.toTime("20111231  23:59");

	public static final String CMD_GET_DATA="Get Page...";
	public static final String CMD_CHECK_IN="Check In";

	private final String fAppName;

	// Set m_useSelector to true to have the outstanding pages appear
	// in a combo box in the lower left corner. Setting this value to
	// false causes a pop up to prompt for each page to be dispositioned
	// TODO: as of March 2011 it is not possible set this value to true
	// unless and until the code in PageSelect.java is modified to use
	// a DAO!
	boolean m_useSelector = false;
	private SelectMode     modeSelect; // used if m_useSelector is true

	// Page Table Settings
	private PageTableModel m_pageModel;
	private Dimension	    m_spacing = new Dimension ( 4, 2 );
//	private int				    m_rowHeight = 20;
	public  static final Font FONT = new Font("SansSerif", Font.BOLD, 14);

	public _RawDispo()
		{
		super();
		SBProperties props = SBProperties.getInstance();
		fAppName = props.getProperty("app.name");
		setTitle( fAppName + " " + props.getProperty("app.version"));
		setBounds(props.getRectangle("usr.app.bounds",
		                         25, 25, 1300, 900));
		cmdLogin();
		buildContent();
		buildMenus();
		ensureHaveWork();
		setVisible(true);
		cmdGetData();
		}

	private void cmdLogin()
		{
		new DlgLogin(this, Access.RD).getUID();
		com.shanebow.util.SBLog.format( "_uid: %s Access._login: %s", Access.getUID(), Access._login );
		}

	protected void buildContent()
		{
		getContentPane().add( tabContactList(), BorderLayout.CENTER );
		}

	private JPanel tabContactList()
		{
		JPanel p = new JPanel( new BorderLayout());
		p.setBorder(LAF.getStandardBorder());
		p.add( createTable(), BorderLayout.CENTER);
		p.add( btnPanel(), BorderLayout.SOUTH);
		return p;
		}

	public JScrollPane createTable()
		{
		DFTable table = new DFTable(m_pageModel = new PageTableModel());

		// Set up to edit & display dispositions
		table.addDispoEditor( Role.TQ.saveDispos());

		table.setFont(FONT);
//		table.setRowHeight( m_rowHeight );
		table.setIntercellSpacing( m_spacing );
		TableColumnModel colModel = table.getColumnModel();
		int nCols = m_pageModel.getColumnCount();
		for ( int c = 0; c < nCols; c++ )
			{
			TableColumn tc = colModel.getColumn(c);
			tc.setPreferredWidth( PageRow.COLS[c].m_width );
			}
		JScrollPane scrollpane = new JScrollPane(table);
		scrollpane.setBorder(new BevelBorder(BevelBorder.LOWERED));
		return scrollpane;
		}

	private JPanel btnPanel()
		{
		JPanel p = new JPanel( new BorderLayout());
		if ( m_useSelector )
			{
			modeSelect = new SelectMode(this, CMD_GET_DATA);
			p.add( modeSelect, BorderLayout.WEST );
			}

		JButton btnCheckIn = makeButton(CMD_CHECK_IN);
		p.add( makeButton(CMD_CHECK_IN), BorderLayout.EAST);
		getRootPane().setDefaultButton(btnCheckIn);
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
			CMD_GET_DATA, CMD_CHECK_IN, null,
			new SBViewLogAction(f),
			null,
			LAF.setExitAction(new com.shanebow.ui.SBExitAction(f)
				{
				public void doApplicationCleanup() { Raw.DAO.shutdown(); }
				}));
		menuBar.addEditMenu();
		menuBar.add(menuBar.getThemeMenu());
		menuBar.addMenu("Help",
			new SBActHelp(), null,
			new SBAboutAction(f, SBProperties.getInstance()));
		setJMenuBar(menuBar);
		}

	public void ensureHaveWork()
		{
		if ( !m_useSelector )
			return;
		if ( modeSelect.getPageCount() > 0 )
			return;
		SBDialog.showMsg( null, fAppName, "No work found: All pages are checked in.", 0 );
 		LAF.exit(1); // cmdAppExit();
		}

	public void cmdCheckIn()
		{
		if (m_pageModel.checkIn())
			{
			if ( m_useSelector )
				{
				modeSelect.reset();
				ensureHaveWork();
				}
			else cmdGetData();
			}	
		}

	private String promptPage() // only called if m_useSelector is false
		{
		String page = JOptionPane.showInputDialog("Page to disposition:" );
		if ( page == null )
			System.exit(0);
		return "page = " + page;
		}

	public void cmdGetData()
		{
		if ( m_useSelector )
			{
			int perPage = modeSelect.getShow();
			String whereClause = modeSelect.getWhereClause();
			m_pageModel.getPage( whereClause, perPage );
			}
		else
			{
			String page = "";
			while ( !m_pageModel.getPage( page = promptPage(), -1 ))
				SBDialog.inputError( page + " not found");
			}
		}

	public void actionPerformed(ActionEvent e)
		{
		String cmd = e.getActionCommand();
		if ( cmd.equals(CMD_GET_DATA))      cmdGetData();
		else if ( cmd.equals(CMD_CHECK_IN)) cmdCheckIn();
		}

	public static void main( String[] args )
		{
		SBProperties.load(_RawDispo.class, "com/apo/apo.properties");
		LAF.initLAF(blowUp, true);
		Access.parseCmdLine(args);
		new _RawDispo();
		}
	} // 317
