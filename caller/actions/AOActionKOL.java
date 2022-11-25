package com.apo.apps.caller.actions;
/********************************************************************
* @(#)AOActionKOL.java 1.00 20110217
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* AOActionKOL: Action class to disposition a contact as KOL
* which is a Keep On List for the AOs.
*
* @author Rick Salamone
* @version 1.00, 20110401 rts created
* @version 1.01, 20110401 rts added date/time
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

public final class AOActionKOL
	extends SBAction
	{
	private static final String HELP = "<HTML><I>This contact will be"
		+ " deassigned from you, and reassigned<BR> in the future.<BR><BR>"
		+ "Please provide a short comment explaining the contact's<BR>"
		+ "situation and how far you got in the pitch.";

	public AOActionKOL()
		{
		super( "KOL", 'K', "Keep On List", "clock" );
		}

	@Override public void actionPerformed(ActionEvent e)
		{
		CallerGUI gui = CallerGUI.getInstance();
		Raw edited = gui.getEdited();
		if ( edited == null )
			return;
 		String[] options = { "OK", "Cancel" };
		EditComment comment = new EditComment();
		DateTimePanel dateTime = new DateTimePanel();
		JPanel panel = buildPanel( comment, dateTime );
		while ( true )
			{
			if ( 0 != JOptionPane.showOptionDialog(gui, panel,
				LAF.getDialogTitle(toString()), JOptionPane.DEFAULT_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0] ))
				return;

			try
				{
				long time = dateTime.getTime();
				if ( time < Raw.DAO.getServerTime())
					throw new DataFieldException("Call back must be in the future");
				Comment msg = comment.get();
				if ( msg.isEmpty())
					throw new DataFieldException("Comment cannot be blank");
				gui.finishUp( edited, null, Dispo.KOL, new When(time), msg.toString());
				return;
				}
			catch (Exception ex) { SBDialog.error("Data Access Error", ex.getMessage()); }
			}
		}

	private JPanel buildPanel(EditComment aComment, DateTimePanel aDateTimePanel)
		{
		JLabel help = new JLabel(HELP);
		LAF.titled(help, "Help");
		LAF.titled(aComment, "Reason?");
		LAF.titled(aDateTimePanel, "Call back");
		aDateTimePanel.setPreferredSize(new Dimension(160,200));
		aDateTimePanel.setTime();

		JPanel left = new JPanel(new BorderLayout());
		left.add( help, BorderLayout.NORTH );
		left.add( aComment, BorderLayout.CENTER );
		JPanel it = new JPanel(new BorderLayout());
		it.add( left, BorderLayout.CENTER );
		it.add( aDateTimePanel, BorderLayout.EAST);
		return it;
		}
	}
