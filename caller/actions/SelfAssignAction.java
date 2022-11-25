package com.apo.apps.caller.actions;
/********************************************************************
* @(#)SelfAssignAction.java 1.00 20110509
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* SelfAssignAction: Allows a user to assign an unassigned contact
* exclusively to himself.
*
* @author Rick Salamone
* @version 1.00, 20110509 split out from the caller gui
* @version 1.01, 20110601 split out from the action list
*******************************************************/
import com.apo.apps.caller.CallerGUI;
import com.apo.contact.Raw;
import com.apo.contact.Source;
import com.apo.net.Access;
import com.shanebow.ui.SBAction;
import com.shanebow.ui.SBDialog;
import java.awt.event.*;

public final class SelfAssignAction extends SBAction
	{
	public SelfAssignAction()
		{
		super("Lock", 'L', 	"Assigns this contact exclusively to you",	"lock" );
		}

	@Override public void actionPerformed(ActionEvent evt)
		{
		CallerGUI gui = CallerGUI.getInstance();
		try
			{
			Source me = Source.find(Access.getUID());
			if ( gui.getUnedited().assignedTo().equals(me))
				return;
			Raw.DAO.assign( me, me, gui.getUnedited());
			gui.set(Raw.CALLER, me);
			}
		catch (Exception e) { SBDialog.error("Lock Error", e.getMessage()); }
		}
	}
