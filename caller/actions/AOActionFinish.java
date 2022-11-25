package com.apo.apps.caller.actions;
/********************************************************************
* @(#)AOActionFinish.java 1.00 20110425
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* AOActionFinish: Action class to finish the verification process
* once enoungh time has transpired to expect that the previously
* requested brouchure has arrived.
*
* @author Rick Salamone
* @version 1.00, 20110506 rts created
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

public final class AOActionFinish
	extends SBAction
	{
	private static final String HELP = "<HTML><I>"
		+ "This contact's order is paid.<BR>"
		+ "Do the <B>loader introduction<B>, and click:<UL>"
		+ "<LI><B>Done -</B> if complete, or,"
		+ "<LI><B>CB -</B> to call back at a better time.";

	public AOActionFinish()
		{
		super( "Finish", 'F', "Loader Introduction", "next" );
		}

	@Override public void actionPerformed(ActionEvent e)
		{
		CallerGUI gui = CallerGUI.getInstance();
		Raw edited = gui.getEdited();
		if ( edited == null )
			return;
 		String[] options = { "CB", "Done" };
		JPanel panel = buildPanel();
		while ( true )
			{
			switch ( JOptionPane.showOptionDialog(gui, panel,
				LAF.getDialogTitle(toString()), JOptionPane.DEFAULT_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0] ))
				{
				case JOptionPane.CLOSED_OPTION: return;
				case 0: gui.fireAction( "CB" ); return; // 
				case 1: break; // handle the into action( LI );
				}

			try
				{
				When when = new When(Raw.DAO.getServerTime());
				gui.finishUp( edited, Source.XX, Dispo.LI, when, "");
				return;
				}
			catch (Exception ex) { SBDialog.error("Data Access Error", ex.getMessage()); }
			}
		}

	private JPanel buildPanel()
		{
		JPanel it = new JPanel(new BorderLayout());

		it.add( LAF.titled(new JLabel(HELP), "Help"), BorderLayout.CENTER);
		return it;
		}
	}
