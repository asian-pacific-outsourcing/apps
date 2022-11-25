package com.apo.apps.caller.actions;
/********************************************************************
* @(#)AOActionCB.java 1.00 20110217
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* AOActionCB: Action class to disposition a contact as ACB
* which schedules an AO call back.
*
* @author Rick Salamone
* @version 1.00, 20110217 rts created
*******************************************************/
import com.apo.apps.caller.CallerGUI;
import com.apo.contact.Raw;
import com.apo.contact.Dispo;
import com.apo.contact.Source;
import com.apo.contact.SBRadioGroup;
import com.shanebow.dao.*;
import com.shanebow.dao.edit.*;
import com.apo.net.Access;
import com.shanebow.ui.LAF;
import com.shanebow.ui.SBAction;
import com.shanebow.ui.SBDialog;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

public final class AOActionCB
	extends SBAction
	{
	public AOActionCB()
		{
		super( "CB", 'C', "Call back", "clock" );
		}

	@Override public void actionPerformed(ActionEvent e)
		{
		CallerGUI gui = CallerGUI.getInstance();
		Raw edited = gui.getEdited();
		if ( edited == null )
			return;
 		String[] options = { "OK", "Cancel" };
		JPanel panel = buildPanel();
		fDateTimePanel.setTime();
		while ( true )
			{
			if ( 0 != JOptionPane.showOptionDialog(gui, panel,
				LAF.getDialogTitle(toString()), JOptionPane.DEFAULT_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0] ))
				return;

			try
				{
				String details = getDetails();
				When when = getWhen();
				Dispo dispo = edited.dispo();
				if ( dispo.equals(Dispo.L)) dispo = Dispo.VCB;
				else if ( dispo.equals(Dispo.VOL)) dispo = Dispo.ACB;
				Source assignTo = null;
				if ( edited.assignedTo().equals(Source.XX)
				&& details.startsWith(SPOKETOCHOICES[0]))
					assignTo = Access.usrID();
				gui.finishUp( edited, assignTo, dispo, when, details);
				return;
				}
			catch (Exception ex) { SBDialog.error("Data Access Error", ex.getMessage()); }
			}
		}

	private final DateTimePanel fDateTimePanel = new DateTimePanel();
	private final EditComment fComment = new EditComment();
	private SBRadioGroup<String>  radio;
	private static final String[] SPOKETOCHOICES =
		{ "Client", "Secretary", "Wife", "Other", "No Answer" };

	private final String getDetails()
		throws DataFieldException
		{
		String who = (String)radio.getSelected();
		if ( who == null )
			throw new DataFieldException("You must specify who you spoke with");
		if ( who.equals("No Answer")) return who;
		Comment saidWhat = fComment.get();
		if ( saidWhat.isEmpty())
			throw new DataFieldException("You must specify what was said");
		return who + " said " + saidWhat;
		}

	private final When getWhen()
		throws DataFieldException
		{
		long time = fDateTimePanel.getTime();
		if ( time < Raw.DAO.getServerTime())
			throw new DataFieldException("Call back must be in the future");
		return new When(time);
		}

	private JPanel buildPanel()
		{
		JPanel it = new JPanel(new BorderLayout());

		JPanel spokeWith = new JPanel(new GridLayout(1,0));
		LAF.titled(spokeWith, "Spoke With");
		radio = new SBRadioGroup<String>( spokeWith, SPOKETOCHOICES );
		LAF.titled(fComment, "Who said..." );

		JPanel details = new JPanel(new BorderLayout());
		details.add( spokeWith, BorderLayout.NORTH );
		details.add( fComment, BorderLayout.CENTER );

		fDateTimePanel.setPreferredSize(new Dimension(160,200));
		LAF.titled(fDateTimePanel, "Call back");

		it.add(details, BorderLayout.WEST);
		it.add(fDateTimePanel, BorderLayout.EAST);
		return it;
		}
	}
