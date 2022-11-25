package com.apo.apps.AppServer.monitor;
/********************************************************************
* @(#)NekoAction.java 1.00 20110125
* Copyright (c) 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* NekoAction: Action to logout all clients from the server.
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

public final class NekoAction
	extends AbstractAction
	{
	public static final String CMD_NAME="Neko";
  /**
  * Constructor. 
  * 
  */
  public NekoAction()
		{
		super(CMD_NAME);
		putValue(SHORT_DESCRIPTION, "All users play Neko");
		putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_N));
		}

	@Override public void actionPerformed(ActionEvent e)
		{
		for ( UserStats user : UserStats.getAll())
			user.send(ClientOp.CODE_NEKO, Message.UNSOLICITED, "" );
	  }
	}