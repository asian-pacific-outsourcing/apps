package com.apo.apps.caller.actions;
/********************************************************************
* @(#)AOActionUNC.java 1.00 20110217
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* AOActionUNC: Action class to disposition a contact as UNC
* which is a Uncontactable for the AOs.
*
* @author Rick Salamone
* @version 1.00, 20110401 rts created
*******************************************************/
import com.apo.apps.caller.CallerGUI;
import com.apo.contact.Raw;
import com.apo.contact.Dispo;
import com.apo.contact.Source;
import com.shanebow.dao.*;
import com.shanebow.dao.edit.*;
import com.shanebow.ui.LAF;
import com.shanebow.ui.SBAction;
import com.shanebow.ui.SBDialog;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

public final class AOActionUNC
	extends SBAction
	{
	private static final String HELP = "<HTML><I>This contact will be"
		+ " deassigned from you,<BR> and <B>permanently</B> removed from the"
		+ " system.<BR><BR>Only click </I>OK<I> if it is impossible to reach<BR>"
		+ "the contact.";

	public AOActionUNC()
		{
		super( "UNC", 'U', "Uncontactable", "clock" );
		}

	@Override public void actionPerformed(ActionEvent e)
		{
		CallerGUI gui = CallerGUI.getInstance();
		Raw edited = gui.getEdited();
		if ( edited == null )
			return;
 		String[] options = { "OK", "Cancel" };
		JLabel help = new JLabel(HELP);
		LAF.titled(help, "Help");
		if ( 0 != JOptionPane.showOptionDialog(gui, help,
			LAF.getDialogTitle(toString()), JOptionPane.DEFAULT_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, options, options[0] ))
			return;

		try
			{
			Source assignTo = Source.XX;
			gui.finishUp( edited, assignTo, Dispo.UNC, When.TODAY, "");
			return;
			}
		catch (Exception ex) { SBDialog.error("Data Access Error", ex.getMessage()); }
		}
	}
