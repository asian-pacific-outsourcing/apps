package com.apo.apps.manager.MailManager;
/********************************************************************
* @(#)Report.java 1.00 10/05/24
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* Report: A panel that accepts SQL input and displays the results
* of the SQL in a table.
*
* @author Rick Salamone
* @version 1.00, 20100524 rts for the SQL Editor program
* @version 1.50, 20101030 rts now makes requests to application server
* @version 1.51, 20110203 rts decoupled table from report
* @version 1.51, 20110601 rts handles email change requests
*******************************************************/
import com.apo.contact.Raw;
import com.apo.contact.DlgDetails;
import com.apo.contact.report.RawTable;
import com.apo.contact.report.RawTableModel;
import com.apo.contact.touch.TouchCode;
import com.shanebow.ui.LAF;
import com.shanebow.ui.SBDialog;
import com.shanebow.util.SBProperties;
import com.shanebow.util.SBLog;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

public class Report
	extends JPanel
	{
	private static final String CMD_SAVE = "Save";

	private final ReportControls  fControls;
	private final RawTableModel   fModel = new RawTableModel();
	private final RawTable        fTable = new RawTable(fModel);
	private final JLabel          fStatusBar = new JLabel("");
	private final JButton btnFetch = new JButton("Fetch");
	private final MergeFileAction fMergeFileAction;
	private final SentMailAction  fSentMailAction;
	private final EMailExportFileAction fEMailExportFileAction;

	public Report(DlgDetails aDlgContact)
		{
		super ( new BorderLayout());
		RawTable.dlgDetails = aDlgContact;

		fMergeFileAction = new MergeFileAction(fModel);
		fSentMailAction = new SentMailAction(fModel);
		fEMailExportFileAction = new EMailExportFileAction(fModel);

		fControls = new ReportControls( aDlgContact, fTable );
		fTable.makeConfigurable();

		JSplitPane report = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT );
		report.setLeftComponent( fControls );
		report.setRightComponent( resultsPanel());
		report.setDividerLocation( 260 );
		report.setOneTouchExpandable(true);
		report.setContinuousLayout(true);

		add(report, BorderLayout.CENTER);
		add(statusPanel(), BorderLayout.SOUTH);
		}

	private JComponent statusPanel()
		{
		Dimension edgeSpacer = new Dimension(5, 0);
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
		p.add(Box.createRigidArea(edgeSpacer));
		p.add( btnFetch );
		btnFetch.addActionListener( new ActionListener()
			{
			public void actionPerformed(ActionEvent e) { onFetch(); }
			});
		p.add(Box.createRigidArea(edgeSpacer));
		p.add( fStatusBar );
		p.add(Box.createHorizontalGlue());

		p.add( 	new JButton(fMergeFileAction));
		p.add(Box.createRigidArea(edgeSpacer));
		p.add( new JButton(fSentMailAction));
		p.add(Box.createRigidArea(edgeSpacer));
		p.add( new JButton(fEMailExportFileAction));
		p.add(Box.createRigidArea(edgeSpacer));
		JButton btnSave = new JButton(CMD_SAVE);
		btnSave.addActionListener( new ActionListener()
			{
			public void actionPerformed(ActionEvent e) { fModel.saveAs(fTable); }
			});
		p.add( btnSave );
		p.add(Box.createRigidArea(edgeSpacer));
		return p;
		}

	private JComponent resultsPanel()
		{
		JScrollPane scroller = new JScrollPane(fTable);
		scroller.setBorder( BorderFactory.createLoweredBevelBorder());
		return scroller;
		}

	public void onFetch()
		{
		TouchCode action = (TouchCode)fControls.getAction();
		boolean isBrochureRequest = action.equals(TouchCode.MAILREQ);
		fMergeFileAction.setEnabled(isBrochureRequest);
		fSentMailAction.setEnabled(isBrochureRequest);
		fEMailExportFileAction.setEnabled(action.equals(TouchCode.QUALIFIED)
		                      || action.equals(TouchCode.EMAILCHG));
		String sql = fControls.getSQL();
		int show = fControls.getMaxShowCount();
		SBLog.write( "SQL: '" + sql + "'" );
		if ( fModel.fetch( show, sql ))
				{
				fControls.updateHistory();
				fStatusBar.setText( "Retrieved " + fModel.getRowCount() + " items" );
				}
		else fStatusBar.setText( "ERROR: " + fModel.getLastError());
		}
	}
