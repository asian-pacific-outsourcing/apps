package com.apo.apps.caller.actions;
/********************************************************************
* @(#)ChangeEmailAction.java 1.01 20110224
* Copyright (c) 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* ChangeEmailAction: Displays a modal dialog, that allows the user
* to change a contact's email address.
*
* @author Rick Salamone
* @version 1.00 20110601 rts created
*******************************************************/
import com.apo.apps.caller.CallerGUI;
import com.apo.contact.Raw;
import com.apo.contact.Dispo;
import com.apo.contact.RawDAO;
import com.apo.contact.touch.TouchCode;
import com.apo.net.Access;
import com.shanebow.dao.*;
import com.shanebow.dao.edit.EditEMailAddress;
import com.shanebow.ui.LAF;
import com.shanebow.ui.SBAction;
import com.shanebow.ui.SBDialog;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import com.shanebow.util.SBLog;

public final class ChangeEmailAction
	extends SBAction
	{
	/**
	* Constructor. 
	*/
	public ChangeEmailAction()
		{
		super("Change eMail", 'C', "Change the contact's email address", "email" );
		}

	@Override public boolean menuOnly() { return true; }
	@Override public void actionPerformed(ActionEvent evt)
		{
		CallerGUI gui = CallerGUI.getInstance();
		Raw raw = gui.getUnedited();
		EMailAddress old = gui.getEMailAddress();
		if ( old == null ) old = EMailAddress.BLANK;
		EditEMailAddress editor = new EditEMailAddress();
		editor.set(old);
 		String[] options = { "OK", "Cancel" };
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(LAF.titled(editor, "New eMail Address"), BorderLayout.CENTER);
		while ( true )
			{
			if ( 0 != JOptionPane.showOptionDialog(gui, panel,
				LAF.getDialogTitle(toString()), JOptionPane.DEFAULT_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0] ))
				return;
			try
				{
				EMailAddress now = (EMailAddress)editor.get();
				if ( now.isEmpty())
					throw new DataFieldException("eMail cannot be blank");
				if ( now.equals(old))
					throw new DataFieldException("eMail was not changed!");
				DataField[] fields = raw.getFieldsDefensive();
				fields[Raw.EMAIL] = now;
				raw = new Raw(fields);
				long timeNow = Raw.DAO.getServerTime();
				boolean releaseLock = false;
				Raw.DAO.update( raw, releaseLock, TouchCode.EMAILCHG,
				                old.toString(), timeNow, Access.getUID());
				gui.set(Raw.EMAIL, now);
//				gui.refreshHistory();
				return;
				}
			catch (Exception e) { SBDialog.error("Change eMail Error", e.getMessage()); }
			}
		}
	}
