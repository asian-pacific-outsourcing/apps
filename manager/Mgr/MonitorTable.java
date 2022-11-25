package com.apo.apps.manager.Mgr;
/********************************************************************
* @(#)MonitorTable.java 1.00 20100926
* Copyright (c) 2010-2011 by Richard T. Salamone, Jr. All rights reserved.
*
* MonitorTable: Extends JTable to display an arbitrary CSV file.
*
* @version 1.00 20100926
* @author Rick Salamone
* 20100926 RTS first iteration
* 20100505 RTS added Criteria & Reset button
* 20100526 RTS added Tally button
*******************************************************/
import com.apo.employee.Role;
import com.apo.net.Access;
import com.apo.net.ClientOp;
import com.apo.net.Message;
import com.apo.net.SysDAO;
import com.shanebow.util.SBDate;
import com.shanebow.util.SBLog;
import com.shanebow.ui.SBDialog;
import com.shanebow.ui.SBSelectable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class MonitorTable
	extends JPanel
	implements SBSelectable //	, ChangeListener, ListSelectionListener
	{
	Timer timer;

//	public static final String DOUBLECLICK_PROPERTY="DoubleClick";
//	public static final String SELECTED_PROPERTY="RowSelected";
	private int m_selectedRow = -1;
	protected final MonitorPopup popup = new MonitorPopup();
	private final JTable fTable;
	private final JLabel fStatus;
	private final JLabel fClock = new JLabel("88:88");
	private final Role   fRole;
	private int tick;

	private void onTick()
		{
		if ( tick++ % 30 == 0 )
			{
// System.out.println("access: " + fAccess + " showing: " + isVisible());
			fStatus.setText("Updtaing...");
			try
				{
				long time = ((MonitorModel)fTable.getModel()).update();
				String hhmm = SBDate.hhmm(time); //serverTime);
				fClock.setText(hhmm);
				fStatus.setText("");
				}
			catch (Exception e) { fStatus.setText("Update ERROR: " + e); }
			}
		}

	private void onReset()
		{
		if ( !SBDialog.confirm(
			"<HTML>Click OK to clear all production counts on <B>this screen</B>"))
			return;
		fStatus.setText("Reseting...");
		try
			{
			((MonitorModel)fTable.getModel()).reset();
			tick = 0;
			}
		catch (Exception e) { fStatus.setText("Update ERROR: " + e); }
		}

	public void onSelect()
		{
		tick = 0;
		timer.start();
		}

	public void onDeselect()
		{
		System.out.println("deselect: " + fRole);
		timer.stop();
		}

	public MonitorTable(Role aRole)
		{
		super(new BorderLayout());
		fRole = aRole;
		long access = fRole.access();
		fStatus = new JLabel("Constructor " + getClass().getSimpleName());
		fTable = new JTable(new MonitorModel(access));
		fTable.setSelectionMode ( ListSelectionModel.SINGLE_SELECTION );
		TableColumn tcol = fTable.getColumnModel().getColumn(0);
		tcol.setCellRenderer(new DefaultTableCellRenderer()
			{
			public void setValue(Object value)
				{
				String ver = (String)value;
				setForeground(Color.WHITE);
				setBackground(ver.equals("0")? Color.RED: Color.BLUE);
				setText(ver.equals("0")? "" : ver);
				}
			});
		add(new JScrollPane(fTable), BorderLayout.CENTER);
		add(getControlPanel(access), BorderLayout.SOUTH);
		int delay = 1000;
		timer = new Timer(delay, new ActionListener()
			{
			public void actionPerformed(ActionEvent e) { onTick(); }
			});

		popup.add(new AbstractAction("Send Message")
			{
			public void actionPerformed(ActionEvent e)
				{
				String[] us = popup.getUserStats();
				String prompt = "Message for " + us[1];
				String msg = JOptionPane.showInputDialog(prompt);
				if ( msg == null )
					return;
//				us.send( ClientOp.CODE_ECHO, Message.UNSOLICITED, msg );
				}
			});

/************
		popup.add(new AbstractAction("Work settings")
			{
			public void actionPerformed(ActionEvent e)
				{
				configureWork(popup.getUserStats());
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
************/

		fTable.addMouseListener(new MouseAdapter() // to handle double clicks selections
			{
			public void mouseClicked(MouseEvent e)
				{ if ( e.getClickCount() > 1 ) 	onDoubleClick(); } 
			public void mouseReleased (MouseEvent e) { popup.show(e); }
			public void mousePressed (MouseEvent e)  { popup.show(e); }
			});
		}

	public JPanel getControlPanel(long access)
		{
		Dimension edgeSpacer = new Dimension(5, 0);
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
		p.add(Box.createRigidArea(edgeSpacer));
		p.add(fClock);
		p.add(Box.createRigidArea(edgeSpacer));
		p.add(fStatus);
		p.add(Box.createHorizontalGlue());
		JButton btnReset = new JButton( fRole.code() + " Reset");
		btnReset.addActionListener(new ActionListener()
			{
			public void actionPerformed(ActionEvent e) { onReset(); }
			});
		if ( access > Access.DM && access < Access.LO )
			p.add(new JButton(new CriteriaAction(fRole)));
		p.add(Box.createRigidArea(edgeSpacer));
		p.add(btnReset);
		p.add(Box.createRigidArea(edgeSpacer));
		p.add(new JButton(new MonitorTallyAction(fRole.code(), (MonitorModel)fTable.getModel())));
		p.add(Box.createRigidArea(edgeSpacer));
//		p.add(LAF.getCommandRow(buttons));
		return p;
		}

	private void onDoubleClick()
		{
		int row = fTable.getSelectionModel().getLeadSelectionIndex();
System.out.println("Double click: row " + row );
		// int col = fTable.getColumnModel().getSelectionModel().getLeadSelectionIndex();
		if ( row >= 0 )	// actually clicked on a contact, row will be -1
			{						// when header is clicked, or if table is empty
			String loginName = (String)fTable.getModel().getValueAt(row,1);
		SBDialog.error("Double Click", loginName);
	//		dlgAssignWork.show(us);
	//	configureWork(us);
	//		log( "Double click %s: %s", us.getClass().getSimpleName(), us.name());
			}
		}

	protected final void log( String fmt, Object... args )
		{
		SBLog.write( getClass().getSimpleName(), String.format( fmt, args ));
		}

/************
	public MonitorModel getModel() { return (MonitorModel)(fTable.getModel()); }
	// implement ListSelectionListener to get selected row
	public void valueChanged( ListSelectionEvent e )
		{
		super.valueChanged(e);
		ListSelectionModel lsm = (ListSelectionModel)e.getSource();
		if ( lsm.getValueIsAdjusting())
			return;

		MonitorModel model = (MonitorModel)getModel();
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

	void configureWork(final UserStats us)
		{
		}
************/
	}

class MonitorPopup extends JPopupMenu
	{
	String[] rowData;

	public void show(MouseEvent e)
		{
		if (!e.isPopupTrigger ()) return;
		java.awt.Component component = e.getComponent();
		if ( ! (component instanceof JTable))
			return;
	JTable table = (JTable)component;
		int row = table.rowAtPoint(e.getPoint());
		MonitorModel model = (MonitorModel)table.getModel();
		rowData = model.getRow(row);
	System.out.println("* clicked at point " + e.getPoint() + " user " + rowData[0]);
		super.show (table, e.getX(), e.getY());
		}

	public String[] getUserStats() { return rowData; }
	}

class ServerClock
	extends JLabel
	{
	public ServerClock()
		{
		super("88:88");
		}
	}
