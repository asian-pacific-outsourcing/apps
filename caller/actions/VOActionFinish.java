package com.apo.apps.caller.actions;
/********************************************************************
* @(#)VOActionFinish.java 1.00 20110425
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* VOActionFinish: Action class to finish the verification process
* once enoungh time has transpired to expect that the previously
* requested brouchure has arrived.
*
* @author Rick Salamone
* @version 1.00, 20110425 rts created
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

public final class VOActionFinish
	extends SBAction
	{
	private static final String HELP = "<HTML><I>This contact should"
		+ " have received the brochure by now.<BR>"
		+ "Please call the contact to determine whether he should be:<UL>"
		+ "<LI>Sent to the next stage - <b>Lead</B>,"
		+ "<LI>called back at a better time - <B>CB,</B>, or"
		+ "<LI>taken off the list - <B>TOL</B>"
		+ "<LI><B>Note:</B>to resend the brochure, exit this screen and click <B>Brochure</B>";

	public VOActionFinish()
		{
		super( "Finish", 'F', "Finish Verification", "next" );
		}

	@Override public void actionPerformed(ActionEvent e)
		{
		CallerGUI gui = CallerGUI.getInstance();
		Raw edited = gui.getEdited();
		if ( edited == null )
			return;
 		String[] options = { "TOL", "CB", "Lead" };
		JPanel panel = buildPanel();
		while ( true )
			{
			switch ( JOptionPane.showOptionDialog(gui, panel,
				LAF.getDialogTitle(toString()), JOptionPane.DEFAULT_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0] ))
				{
				case JOptionPane.CLOSED_OPTION: return;
				case 0: gui.fireAction( "TOL" ); return; // 
				case 1: gui.fireAction( "CB" ); return; //
				case 2: break; // handle the lead action( TOL );
				}

			try
				{
				When when = new When(Raw.DAO.getServerTime());
				gui.finishUp( edited, Source.XX, Dispo.VOL, when, fComment.toString());
				return;
				}
			catch (Exception ex) { SBDialog.error("Data Access Error", ex.getMessage()); }
			}
		}

	private final EditComment fComment = new EditComment();
	private JPanel buildPanel()
		{
		JPanel it = new JPanel(new BorderLayout());

		it.add( LAF.titled(new JLabel(HELP), "Help"), BorderLayout.NORTH);
		it.add( LAF.titled(fComment, "Optional Comment" ), BorderLayout.CENTER );
		return it;
		}
	}
