package com.apo.apps.caller.actions;
/********************************************************************
* @(#)VOActionMailReq.java 1.00 20110217
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* VOActionMailReq: Action class to disposition a contact as VBR
* which requires an address, and schedules an VO call back.
*
* @author Rick Salamone
* @version 1.00, 20110217 rts created
*******************************************************/
import com.apo.apps.caller.CallerGUI;
import com.apo.contact.Raw;
import com.apo.contact.Dispo;
import com.apo.contact.edit.RawPanel;
import com.shanebow.dao.*;
import com.shanebow.dao.edit.*;
import com.apo.net.Access;
import com.shanebow.ui.LAF;
import com.shanebow.ui.SBAction;
import com.shanebow.ui.SBDialog;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

public final class VOActionMailReq
	extends SBAction
	{
	private static final boolean USE_CALENDAR=false;

/***************
	private static final String HELP = "<HTML><I><UL>"
		+ "<LI>Ensure that the address is complete and accurate.<BR>"
		+ "<LI><B>Do not</B> specify the company name here, but rather on the main form."
		+ "<LI>Select a callback date, allowing time for preparation and delivery."
***************/
	private static final String HELP = "<HTML><I><UL>"
		+ "<LI>Ensure that the address is complete and accurate.<BR>"
		+ "<LI>Enter an optional comment to follow this request<BR>"
		+ "<LI>Click <B>OK</B> to send the brochure request and finish this contact";

	public VOActionMailReq()
		{
		super( "Brochure", 'B', "Request a brochure for this contact", "brochure" );
		}

	@Override public void actionPerformed(ActionEvent e)
		{
		CallerGUI gui = CallerGUI.getInstance();
		Raw edited = gui.getEdited();
		if ( edited == null )
			return;
 		String[] options = { "OK", "Cancel" };
		JPanel panel = buildPanel();
		editor.setContact(edited);
		fDateTimePanel.setTime();
		while ( true )
			{
			if ( 0 != JOptionPane.showOptionDialog(gui, panel,
				LAF.getDialogTitle(toString()), JOptionPane.DEFAULT_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0] ))
				return;

			try
				{
				edited = editor.getEdited();
				if ( edited != null )
					{
					When callback = getWhen();
					Raw.DAO.mailReq( Comment.parse("Lee Byers|Welcome"),
					     new When(Raw.DAO.getServerTime()), Access.empID(), edited.id());
					gui.finishUp( edited, Access.usrID(), Dispo.BR, callback, fComment.toString());
					return;
					}
				}
			catch (Exception ex) { SBDialog.error("Error", ex.getMessage()); }
			}
		}

	private static final int[] FIELDS = {Raw.ADDRESS, Raw.COUNTRYID, Raw.TYPE};
	private final RawPanel editor = new RawPanel((byte)1, FIELDS, null);
	private final EditComment fComment = new EditComment();
	private final DateTimePanel fDateTimePanel = new DateTimePanel();

	private final When getWhen()
		throws DataFieldException
		{
		if ( USE_CALENDAR )
			{
			long time = fDateTimePanel.getTime();
			if ( time < Raw.DAO.getServerTime() + 4 * 24 * 60 * 60)
				throw new DataFieldException("Call back must be at least 4 days from now");
			return new When(time);
			}
		else return new When(Raw.DAO.getServerTime() + 10 * 24 * 60 * 60);
		}

	private JPanel buildPanel()
		{
		JPanel it = new JPanel(new BorderLayout());

		JPanel details = new JPanel(new BorderLayout());
		it.add( LAF.titled(new JLabel(HELP), "Help"), BorderLayout.NORTH );
		if ( USE_CALENDAR )
			{
			details.add( LAF.titled(editor, "Mail to"), BorderLayout.CENTER );
			it.add( LAF.titled(fComment, "Optional Comment" ), BorderLayout.SOUTH );

			fDateTimePanel.setPreferredSize(new Dimension(160,200));
			LAF.titled(fDateTimePanel, "Call back");

			it.add(details, BorderLayout.CENTER);
			details.add(fDateTimePanel, BorderLayout.EAST);
			}
		else
			it.add( LAF.titled(fComment, "Optional Comment" ), BorderLayout.CENTER );
		return it;
		}
	}
