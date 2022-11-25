package com.apo.apps.caller.actions;
/********************************************************************
* @(#)VOActionTOL.java 1.00 20110425
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* VOActionTOL: Action class to disposition a contact as VNI, VUD,
* or VBD which effectively marks the contact as "don not call".
*
* @author Rick Salamone
* @version 1.00, 20110425 rts created
*******************************************************/
import com.apo.apps.caller.CallerGUI;
import com.apo.contact.Raw;
import com.apo.contact.Dispo;
import com.shanebow.dao.*;
import com.shanebow.dao.edit.*;
import com.apo.net.Access;
import com.shanebow.ui.LAF;
import com.shanebow.ui.SBAction;
import com.shanebow.ui.SBDialog;
import com.apo.contact.SBRadioGroup;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

public final class VOActionTOL
	extends SBAction
	{
	private static final String HELP = "<HTML><I>This contact will be"
		+ " permantently removed from the call list.<BR>"
		+ "Please select the reason why the contact is to be removed.";

	public VOActionTOL()
		{
		super( "TOL", 'T', "Take Off List", "clock" );
		}

	@Override public void actionPerformed(ActionEvent e)
		{
		CallerGUI gui = CallerGUI.getInstance();
		Raw edited = gui.getEdited();
		if ( edited == null )
			return;
 		String[] options = { "OK", "Cancel" };
		JPanel panel = buildPanel();
		while ( true )
			{
			if ( 0 != JOptionPane.showOptionDialog(gui, panel,
				LAF.getDialogTitle(toString()), JOptionPane.DEFAULT_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0] ))
				return;

			try
				{
				Dispo dispo = getDispo();
				gui.finishUp( edited, null, dispo, When.BLANK, "");
				return;
				}
			catch (Exception ex) { SBDialog.error("Data Access Error", ex.getMessage()); }
			}
		}

	private SBRadioGroup<String>  radio;

	private final Dispo getDispo()
		throws DataFieldException
		{
		String selectedName = (String)radio.getSelected();
		for ( Dispo dispo : Access.getRole().saveDispos())
			if (dispo.name().equals(selectedName))
				return dispo;
		throw new DataFieldException("You must specify why this contact is being removed");
		}

	private JPanel buildPanel()
		{
		JPanel it = new JPanel(new BorderLayout());

		JPanel reason = new JPanel(new GridLayout(0,1));
		int count = 0;
		Dispo[] saveDispos = Access.getRole().saveDispos();
		for ( Dispo d : saveDispos)
			if (d.id() < 0) ++count;
		String dispoStrings[] = new String[count];
		int i = 0;
		for ( Dispo dispo : saveDispos)
			if (dispo.id() < 0) dispoStrings[i++] = dispo.name();
		radio = new SBRadioGroup<String>( reason, dispoStrings );

		JPanel details = new JPanel(new BorderLayout());
		it.add( LAF.titled(new JLabel(HELP), "Help"), BorderLayout.CENTER );
		it.add( LAF.titled(reason, "TOL Reason"), BorderLayout.SOUTH );

		return it;
		}
	}
