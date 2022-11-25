package com.apo.apps.manager.MailManager;
/********************************************************************
* @(#)SentMailAction.java 1.01 20110304
* Copyright (c) 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* SentMailAction: Prompts the user to select the mail piece and date
* sent then marks all the contacts displayed in the contact table as
* having been set the piece.
*
* @author Rick Salamone
* @version 1.00 20110304 rts demo version
* @version 1.00 20110309 rts first released version
*******************************************************/
import com.apo.contact.Raw;
import com.apo.contact.RawDAO;
import com.apo.contact.report.RawTableModel;
import com.apo.contact.touch.Touch;
import com.shanebow.dao.*;
import com.apo.net.Access;
import com.shanebow.ui.LAF;
import com.shanebow.ui.SBAction;
import com.shanebow.ui.SBDialog;
import com.shanebow.ui.calendar.MonthCalendar;
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import javax.swing.*;

final class SentMailAction
	extends SBAction
	{
	private final RawTableModel fModel;

	private JComboBox fSentWhat = new JComboBox(MailPiece.AVAILABLE);
	private MonthCalendar fSentWhen = new MonthCalendar();

	/**
	* Constructor. 
	*/
	public SentMailAction( RawTableModel aModel )
		{
		super( "Sent", 'S', "Marks the contacts as having been sent mail", null );
		fModel = aModel;
		}

	@Override public void actionPerformed(ActionEvent evt)
		{
 		String[] options = { "OK", "Cancel" };
		if ( 0 != JOptionPane.showOptionDialog(null, buildPanel(),
			LAF.getDialogTitle("Mark As Sent"), JOptionPane.DEFAULT_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, options, options[0] ))
			return;

		try
			{
			MailPiece mail = (MailPiece)fSentWhat.getSelectedItem();
			When sentWhen = new When(fSentWhen.getDate());
			Raw.DAO.sentMail( mail.desc(), sentWhen, Access.usrID(),
			                  mail.callOnArrival(), fModel.csvRawIDs());
			SBDialog.inform(null, "Sent Mail", "Successfully marked "
			   + fModel.getRowCount() + " contacts\nas having received"
			   + " the mail piece\n" + mail.toString());
			}
		catch (Exception e) { SBDialog.error("Data Access Error", e.getMessage()); }
		}

	private JPanel buildPanel()
		{
		JPanel it = new JPanel(new BorderLayout());
		it.add(fSentWhat, BorderLayout.NORTH);
		LAF.titled(fSentWhat, "What was sent?");

		JPanel sentDate = new JPanel(new BorderLayout());
		sentDate.add(fSentWhen, BorderLayout.CENTER);
		LAF.titled(sentDate, "Date Sent");
		fSentWhen.setPreferredSize(new Dimension(160,160));

		it.add(sentDate, BorderLayout.CENTER);
//		it.add(sentTime, BorderLayout.SOUTH);
		return it;
		}
	}
