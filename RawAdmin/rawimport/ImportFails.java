package com.apo.apps.RawAdmin.rawimport;
/********************************************************************
* @(#)ImportFails.java	1.0 10/06/20
* Copyright 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* ImportFails: Dialog to view the application log.
*
* @version 1.00 06/20/10
* @author Rick Salamone
* 20100627 RTS 1.01 position at lower right corner of screen
* 20100627 RTS 1.02 responds to change in theme menu
*******************************************************/
import com.shanebow.ui.LAF;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ImportFails extends JDialog
	implements ActionListener
	{
	static ImportFails _me = new ImportFails();
	static void launch() { _me.setVisible(true); }

	private static final String DLG_TITLE="Log";
	private static final String CMD_CLOSE="Close";

	private ImportFails( )
		{
		super((Frame)null, LAF.getDialogTitle(DLG_TITLE), false);
		JPanel top = new JPanel();
		top.setBorder(LAF.getStandardBorder());
		ImportWorker.logFails.setPreferredSize( new Dimension(375,500));
		top.setLayout(new BorderLayout());
		top.add(ImportWorker.logFails, BorderLayout.CENTER);
		top.add(btnPanel(), BorderLayout.SOUTH);
		setContentPane(top);
		pack();
		positionDialog();
		LAF.addUISwitchListener(this);
		}

	protected void positionDialog()
		{
		Dimension screenSize = this.getToolkit().getScreenSize();
		Dimension size = this.getSize();
		int y = screenSize.height - size.height;
		int x = screenSize.width - size.width;
		this.setLocation(x,y);
		}

	private JPanel btnPanel()
		{
		JPanel p = new JPanel();
		p.add( makeButton(CMD_CLOSE));
		return p;
		}

	private JButton makeButton(String caption)
		{
		JButton b = new JButton(caption);
		b.addActionListener(this);
		return b;
		}

	public void actionPerformed(ActionEvent e)
		{
		String cmd = e.getActionCommand();
		if ( cmd.equals(CMD_CLOSE))
			setVisible(false);
		}
	}
