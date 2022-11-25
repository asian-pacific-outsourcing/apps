package com.apo.apps.miner.RD;
/********************************************************************
* @(#)SelectMode.java	1.00 10/06/04
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* SelectMode: Component that allows the user to choose the contacts
* to be dispositioned either by page number or by record id.
*
* @version 1.00 06/04/10
* @author Rick Salamone
*******************************************************/
import com.shanebow.ui.SBDialog;
import com.shanebow.ui.SBTextPanel;
import com.shanebow.util.SBLog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

final public class SelectMode extends JPanel
	implements ActionListener
	{
	private static final String CMD_CHANGE_MODE="Change mode";
	private static final String[] MODES= { "Page #", "Top ID #" };
	private static final int PAGE_MODE = 0;
	private static final int TOP_ID_MODE = 1;
	private JComboBox cbMode = new JComboBox(MODES);
	private SelectTopID idSelect = new SelectTopID(this);
	private PageSelect   pageSelect = new PageSelect();
	int m_mode = PAGE_MODE;
	ActionListener m_al;
	String m_actionCmd;

	public SelectMode(ActionListener al, String actionCmd)
		{
		super();
		add( cbMode );
		cbMode.setActionCommand(CMD_CHANGE_MODE);
		cbMode.addActionListener(this);
		add( pageSelect );
//		pageSelect.setActionCommand(CMD_GET_PAGE);
		pageSelect.addActionListener(this);
		add( idSelect );
		cmdChangeMode();
		m_al = al;
		m_actionCmd = actionCmd;
		}

	public int getPageCount()
		{
		return pageSelect.getItemCount();
		}

	public void reset()
		{
		pageSelect.reset();
		}

	public int getShow()
		{
		return ( m_mode == PAGE_MODE ) ? -1 : idSelect.getShow();
		}

	public String getWhereClause()
		{
		return ( m_mode == PAGE_MODE ) ?
			 "page = " + pageSelect.getSelectedItem()
			: "id >= " + idSelect.getTop();
		}

	private void cmdChangeMode()
		{
		m_mode = cbMode.getSelectedIndex();
		pageSelect.setVisible( m_mode == PAGE_MODE );
		idSelect.setVisible( m_mode == TOP_ID_MODE );
		}

	public void actionPerformed(ActionEvent e)
		{
		Object src = e.getSource();
		if ( src.equals(cbMode))
			cmdChangeMode();
		m_al.actionPerformed( new ActionEvent(this, 0, m_actionCmd ));
		}
	}

final class SelectTopID extends JPanel
	{
	public static final int DEFAULT_PER_PAGE = 11;
	private final JTextField tfTop = new JTextField("0");
	private final JTextField tfShow = new JTextField("" + DEFAULT_PER_PAGE);

	public SelectTopID(ActionListener al)
		{
		super(new GridLayout(1, 0, 2, 0));
		add( tfTop );
		tfTop.addActionListener(al);
		add( new JLabel("Show", JLabel.RIGHT ));
		add( tfShow );
		tfShow.addActionListener(al);
		JButton go = new JButton("Go");
		add(go);
		go.addActionListener(al);
		}

	public long getTop()
		{
		try { return Long.parseLong( tfTop.getText()); }
		catch (Exception e) { return 0; }
		}

	public int getShow()
		{
		try { return Integer.parseInt( tfShow.getText()); }
		catch (Exception e) { return DEFAULT_PER_PAGE; }
		}
	}
