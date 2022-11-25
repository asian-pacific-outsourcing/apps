package com.apo.apps.miner.DataMiner;
/********************************************************************
* @(#)DlgLogin.java 1.0 20100428
* Copyright 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* DLgLogin: Prompts for user name and password to log in to an application.
*
* @author Rick Salamone
* @version 1.00 20100428 RTS
* @version 1.01 20101010 RTS uses LabeledPairPanel
*******************************************************/
import com.apo.net.Access;
import com.apo.employee.User;
import com.shanebow.ui.SBDialog;
import com.shanebow.ui.layout.LabeledPairPanel;
import java.awt.*;
import javax.swing.*;

public class DlgLogin
	{
	private static final String CMD_LOGIN="Login";
	private static int NUM_CHARS = 15;
	private static final String DEFAULT_USER = "";
	private static final String DEFAULT_PASSWORD = "";
	private static final String CONNECT_TITLE = "Login";
	private static final String[] ConnectOptionNames = { CMD_LOGIN };

	Component   m_owner;
	long        m_accessRequest;
	LabeledPairPanel  connectionPanel;
	private final JTextField  tfUserName = new JTextField(NUM_CHARS);
	private final JTextField  tfPassword = new JPasswordField(NUM_CHARS);

	public DlgLogin(Component owner, long accessRequest)
		{
		m_owner = owner;
		m_accessRequest = accessRequest;

		connectionPanel = new LabeledPairPanel();
		connectionPanel.addRow("User name: ", tfUserName);
		connectionPanel.addRow( "Password: ", tfPassword);
		tfUserName.requestFocusInWindow();
		}

	public short getUID()
		{
		while ( true )
			if( JOptionPane.showOptionDialog(m_owner, connectionPanel, CONNECT_TITLE,
					JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
						null, ConnectOptionNames, ConnectOptionNames[0]) == 0)
				{
				if ( isValidLogin( m_accessRequest ))
					return Access.getUID();
				}
			else System.exit(1); // return null;
		}

	public String getUserName()
		{
		return Access._login;
		}

	private boolean isValidLogin( long accessRequest )
		{
		User uid = null;
		String usr = tfUserName.getText().trim();
		if ( usr.isEmpty())
			return SBDialog.inputError( "User name cannot be blank" );
		try
			{
			uid = User.parse(usr);
			if ( uid == User.XX )
				return SBDialog.inputError( "Invalid user name" );
			}
		catch ( Exception e ) { return SBDialog.inputError( "Invalid user name" ); }
		if ( !tfPassword.getText().trim().equals(uid.password()))
			return SBDialog.inputError( "Incorrect Password - check caps lock" );
		if ( !uid.isAuthorizedTo(accessRequest))
			return SBDialog.inputError( "Access Denied" );
		Access._login = usr;
		Access.setUID(uid.id());
		com.shanebow.util.SBLog.format( "Logged in as: '%s'", usr );
		return true;
		}
	}
