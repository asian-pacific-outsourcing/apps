package com.apo.apps.caller.actions;
/********************************************************************
* @(#)AOActionTOL.java 1.00 20110217
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* AOActionTOL: Action class to disposition a contact as TOL
* which is a Take Off List for the AOs.
*
* @author Rick Salamone
* @version 1.00, 20110327 rts created
* @version 1.01, 20110403 rts added help
*******************************************************/
import com.apo.apps.caller.CallerGUI;
import com.apo.contact.Raw;
import com.apo.contact.Dispo;
import com.apo.contact.Source;
import com.apo.contact.SBRadioGroup;
import com.shanebow.dao.*;
import com.shanebow.dao.edit.*;
import com.shanebow.ui.LAF;
import com.shanebow.ui.SBAction;
import com.shanebow.ui.SBDialog;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

public final class AOActionTOL
	extends SBAction
	{
	private static final String HELP = "<HTML><I>This contact will be"
		+ " deassigned from you, and sent to<BR>the manager who will"
		+ " make a final determination of<BR>the contact's status.";

	public AOActionTOL()
		{
		super( "TOL", 'T', "Take off list", "clock" );
		}

	@Override public void actionPerformed(ActionEvent e)
		{
		CallerGUI gui = CallerGUI.getInstance();
		Raw edited = gui.getEdited();
		if ( edited == null )
			return;
 		String[] options = { "OK", "Cancel" };
		EditComment comment = new EditComment();
		JPanel panel = buildPanel(comment);
		while ( true )
			{
			if ( 0 != JOptionPane.showOptionDialog(gui, panel,
				LAF.getDialogTitle(toString()), JOptionPane.DEFAULT_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0] ))
				return;

			try
				{
				Comment msg = comment.get();
				if ( msg.isEmpty())
					throw new DataFieldException("Comment cannot be blank");
				gui.finishUp( edited, null, Dispo.TOL, When.TODAY, "");
				return;
				}
			catch (Exception ex) { SBDialog.error("Data Access Error", ex.getMessage()); }
			}
		}

	private JPanel buildPanel(EditComment aComment)
		{
		JLabel help = new JLabel(HELP);
		LAF.titled(help, "Help");
		LAF.titled(aComment, "Reason?");

		JPanel it = new JPanel(new BorderLayout());
		it.add( help, BorderLayout.NORTH );
		it.add( aComment, BorderLayout.CENTER );
		return it;
		}
	}
