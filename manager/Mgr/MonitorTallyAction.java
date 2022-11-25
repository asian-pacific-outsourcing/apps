package com.apo.apps.manager.Mgr;
/********************************************************************
* @(#)MonitorTallyAction.java 1.01 20110525
* Copyright (c) 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* MonitorTallyAction: Action to request that a brochure be sent to the lead.
*
* @author Rick Salamone
* @version 1.00 20110525 rts created
*******************************************************/
// import com.shanebow.dao.When;
import com.shanebow.ui.LAF;
import com.shanebow.ui.SBAction;
import com.shanebow.ui.SBDialog;
import com.shanebow.ui.layout.LabeledPairPanel;
import java.awt.event.*;
import javax.swing.*;

final class MonitorTallyAction
	extends SBAction
	{
	private static final int NONPRODUCTION_COLS=8;
	private final JLabel[] fLblCounts;
	private final MonitorModel fModel;

	public MonitorTallyAction(String aRoleCode, MonitorModel aMonitorModel)
		{
		super( aRoleCode + " Tally", 'T', "Snapshot production summary", null );
		fModel = aMonitorModel;
		int numItems = fModel.getColumnCount() - fModel.f1stProdCol + 2;
		fLblCounts = new JLabel[numItems];
		for ( int i = 0; i < fLblCounts.length; i++ )
			fLblCounts[i] = new JLabel("0", JLabel.LEADING);
		}

	@Override public void actionPerformed(ActionEvent evt)
		{
 		String[] options = { "Update" };
		JComponent panel = buildPanel();
		do
			{
			updateCounts();
			}
		while ( 0 == JOptionPane.showOptionDialog(null, panel,
			LAF.getDialogTitle(toString()), JOptionPane.DEFAULT_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, options, options[0] ));
		}

	private JComponent buildPanel()
		{
		LabeledPairPanel it = new LabeledPairPanel();
		it.addRow( "Users", fLblCounts[0] );
		it.addRow( "Logged In", fLblCounts[1] );
		for ( int i = 2; i < fLblCounts.length; i++ )
			it.addRow( fModel.getColumnName(i + fModel.f1stProdCol - 2), fLblCounts[i] );
		return it;
		}

	private void updateCounts()
		{
		final String prefix = "<HTML><B>&nbsp;";
		int numUsers = fModel.getRowCount();
		fLblCounts[0].setText(prefix + numUsers );
		int loggedIn = 0;
		for ( int row = 0; row < numUsers; row++ )
			if ( !fModel.getValueAt(row, 0).equals("0"))
				++loggedIn;
		fLblCounts[1].setText(prefix +  loggedIn );
		for ( int i = 2; i < fLblCounts.length; i++ )
			{
			int col = i + fModel.f1stProdCol - 2;
			int value = 0;
			for ( int row = 0; row < numUsers; row++ )
				try { value += Integer.parseInt((String)fModel.getValueAt(row, col)); }
				catch (Exception e) { fLblCounts[i].setText("?"); continue; }
			fLblCounts[i].setText(prefix + value);
			}
		}
	}