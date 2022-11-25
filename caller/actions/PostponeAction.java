package com.apo.apps.caller.actions;
/********************************************************************
* @(#)PostponeAction.java 1.00 20110217
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* PostponeAction: Action class to postpone a scheduled call.
*
* @author Rick Salamone
* @version 1.00, 20110217 rts created
*******************************************************/
import com.apo.apps.caller.CallerGUI;
import com.apo.contact.Raw;
import com.apo.contact.Dispo;
import com.apo.contact.Source;
import com.apo.net.Access;
import com.shanebow.dao.*;
import com.shanebow.ui.LAF;
import com.shanebow.ui.SBAction;
import java.awt.event.ActionEvent;
import java.net.*;
import java.util.*;
import javax.swing.*;

public final class PostponeAction
	extends SBAction
	{
	public PostponeAction()
		{
		super( "Postpone", 'P', "Try calling again later today", "clock" );
		}

	@Override public void actionPerformed(ActionEvent e)
		{
		CallerGUI gui = CallerGUI.getInstance();
		Raw edited = gui.getEdited();
		if ( edited == null )
			return;
		Integer[] choices = { 1, 2, 3, 4, 5, 6 };
		String prompt = "Try again in how many hours?";
		Integer choice = (Integer)JOptionPane.showInputDialog(gui, prompt,
		               LAF.getDialogTitle(toString()),
 		               JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);
		if ( choice == null ) // canceled
			return;
		int seconds = choice.intValue() * 60 * 60;
		long timeNow = Raw.DAO.getServerTime();
		short uID = Access.getUID();
		When callback = new When(timeNow + seconds);
		Dispo dispo = edited.dispo();
		if ( dispo == Dispo.XX ) dispo = Dispo.CB;
		else if ( dispo == Dispo.L ) dispo = Dispo.VCB;
		else if ( dispo == Dispo.VOL ) dispo = Dispo.ACB;
		Source assignTo = Source.find(uID);
		gui.finishUp( edited, assignTo, dispo, callback, "" );
		}
	}
