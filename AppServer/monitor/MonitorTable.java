package com.apo.apps.AppServer.monitor;
/********************************************************************
* @(#)MonitorTable.java 1.00 20100926
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* MonitorTable: Extends JTable to display an arbitrary CSV file.
*
* @version 1.00 20100926
* @author Rick Salamone
* 20100926 RTS 1.01 first iteration
* 20110505 rts 1.04 removed criteria stuff - now in mgr app
*******************************************************/
import com.apo.net.ClientOp;
import com.apo.net.Message;
import com.shanebow.util.SBLog;
import com.shanebow.ui.SBDialog;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class MonitorTable extends JTable
	implements ChangeListener, ListSelectionListener
	{
	public static final String DOUBLECLICK_PROPERTY="DoubleClick";
	public static final String SELECTED_PROPERTY="RowSelected";
	private int m_selectedRow = -1;
	protected final MonitorPopup popup = new MonitorPopup();

	public MonitorTable(UsrTableModel model)
		{
		super(model); // super(new UsrTableModel());
		setSelectionMode ( ListSelectionModel.SINGLE_SELECTION );

		TableColumn tcol = getColumnModel().getColumn(model.COL_VER);
		tcol.setCellRenderer(new DefaultTableCellRenderer()
			{
			public void setValue(Object value)
				{
				String ver = (String) value;
				setForeground(java.awt.Color.WHITE);
				setBackground(ver.equals("")? java.awt.Color.RED : java.awt.Color.BLUE);
				setText(ver);
				}
			});


		popup.add(new AbstractAction("Work settings")
			{
			public void actionPerformed(ActionEvent e)
				{
				configureWork(popup.getUserStats());
				}
			});

		popup.add(new AbstractAction("Send Message")
			{
			public void actionPerformed(ActionEvent e)
				{
				UserStats us = popup.getUserStats();
				String prompt = "Message for " + us.login();
				String msg = JOptionPane.showInputDialog(prompt);
				if ( msg == null )
					return;
				us.send( ClientOp.CODE_ECHO, Message.UNSOLICITED, msg );
				}
			});

		popup.addSeparator();
		popup.add(new AbstractAction("Log off")
			{
			public void actionPerformed(ActionEvent e)
				{
				UserStats us = popup.getUserStats();
				if ( SBDialog.confirm("Are you sure you want log off " + us.login() + "?"));
					us.send( ClientOp.CODE_LOGOUT, Message.UNSOLICITED, "" );
				}
			});

		addMouseListener(new MouseAdapter() // to handle double clicks selections
			{
			public void mouseClicked(MouseEvent e)
				{ if ( e.getClickCount() > 1 ) 	onDoubleClick(); } 
			public void mouseReleased (MouseEvent e) { popup.show(e); }
			public void mousePressed (MouseEvent e)  { popup.show(e); }
			});
		}

	@Override public UsrTableModel getModel() { return (UsrTableModel)(super.getModel()); }
	protected final void log( String fmt, Object... args )
		{
		SBLog.write( getClass().getSimpleName(), String.format( fmt, args ));
		}

	// implement ListSelectionListener to get selected row
	public void valueChanged( ListSelectionEvent e )
		{
		super.valueChanged(e);
		ListSelectionModel lsm = (ListSelectionModel)e.getSource();
		if ( lsm.getValueIsAdjusting())
			return;

		UsrTableModel model = (UsrTableModel)getModel();
		// Find out which indexes are selected
		int newSelectedRow = lsm.getLeadSelectionIndex();
		if ( lsm.isSelectionEmpty())
			{
			if ( model.getRowCount() > 0 )
				lsm.setSelectionInterval(0,0);
			else
				firePropertyChange( SELECTED_PROPERTY, m_selectedRow, m_selectedRow=-1 );
			return;
			}
		else // if ( lsm.isSelectedIndex(newSelectedRow))
			{
			if ( m_selectedRow != newSelectedRow )
				firePropertyChange( SELECTED_PROPERTY, m_selectedRow, newSelectedRow );
			m_selectedRow = newSelectedRow;
			}
		}

	public void stateChanged(ChangeEvent e)
		{
		Object src = e.getSource();
		if ( src instanceof JSlider )
			setFontSize(((JSlider)src).getValue());
		}

	public void setFontSize(int size)
		{
		setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
		}

	@Override public void setFont(Font f)
		{
		super.setFont(f);
		if ( getRowHeight() != f.getSize() + 4 )
			setRowHeight( f.getSize() + 4 );
		}

	private void onDoubleClick()
		{
		int row = getSelectionModel().getLeadSelectionIndex();
		// int col = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();
		if ( row >= 0 )	// actually clicked on a contact, row will be -1
			{						// when header is clicked, or if table is empty
			UserStats us = getModel().getRow(row);
	//		dlgAssignWork.show(us);
		configureWork(us);
			log( "Double click %s: %s", us.getClass().getSimpleName(), us.name());
			}
		}

	void configureWork(final UserStats us)
		{
		}
	}

	class MonitorPopup extends JPopupMenu
		{
		UserStats us;
		public void show(MouseEvent e)
			{
			if (!e.isPopupTrigger ()) return;
			java.awt.Component component = e.getComponent();
			if ( ! (component instanceof MonitorTable))
				return;
		MonitorTable table = (MonitorTable)component;
			int row = table.rowAtPoint(e.getPoint());
			us = table.getModel().getRow(row);
	System.out.println("* clicked at point " + e.getPoint() + " user " + us.login());
			super.show (table, e.getX(), e.getY());
			}
		public UserStats getUserStats() { return us; }
		}
