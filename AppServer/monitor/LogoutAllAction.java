package com.apo.apps.AppServer.monitor;
/********************************************************************
* @(#)LogoutAllAction.java 1.00 20110125
* Copyright (c) 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* LogoutAllAction: Action to logout all clients from the server.
*
* @author Rick Salamone
* @version 1.00 20110125
*******************************************************/
import com.apo.apps.AppServer.monitor.UserStats;
import com.apo.apps.AppServer.rox.ClientChannel;
import com.apo.net.ClientOp;
import com.apo.net.Message;
import java.awt.event.*;
import javax.swing.AbstractAction;

public final class LogoutAllAction
	extends AbstractAction
	{
	public static final String CMD_NAME="Logout All";
  /**
  * Constructor. 
  * 
  */
  public LogoutAllAction()
		{
		super(CMD_NAME);
		putValue(SHORT_DESCRIPTION, "Logout and shutdown all client apps");
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_L));
		}

	@Override public void actionPerformed(ActionEvent e)
		{
		for ( UserStats user : UserStats.getAll())
			user.send(ClientOp.CODE_LOGOUT, Message.UNSOLICITED, "" );
	  }
	}