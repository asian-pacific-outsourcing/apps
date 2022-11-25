package com.apo.apps.caller.actions;
/********************************************************************
* @(#)TQActionFinish.java 1.00 20110217
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* TQActionFinish: Action class to disposition a contact as VBR
* which requires an address, and schedules an VO call back.
*
* @author Rick Salamone
* @version 1.00, 20110217 rts created
*******************************************************/
import com.apo.apps.caller.CallerGUI;
import com.apo.contact.Raw;
import com.apo.contact.Dispo;
import com.apo.contact.Source;
import com.apo.contact.edit.RawPanel;
import com.shanebow.dao.*;
import com.shanebow.dao.edit.*;
import com.apo.net.Access;
import com.shanebow.ui.LAF;
import com.shanebow.ui.SBAction;
import com.shanebow.ui.SBDialog;
import com.shanebow.util.SBDate;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

public final class TQActionFinish
	extends SBAction
	{
	private static final int CALLBACK_BY_VO_DAYS=3;

	public TQActionFinish()
		{
		super( "Lead", 'L', "Qualify this contact as a lead", "next" );
		}

	@Override public void actionPerformed(ActionEvent e)
		{
		CallerGUI gui = CallerGUI.getInstance();
		Dispo dispo = gui.getUnedited().dispo();
		gui.set(Raw.DISPO, Dispo.L);
		Raw edited = gui.getEdited();
		if ( edited == null )
			gui.set(Raw.DISPO, dispo);
		else try
			{
			When when = getWhen();
			gui.finishUp( edited, Source.XX, Dispo.L, when, "");
			}
		catch (Exception ex) { SBDialog.error("Error", ex.getMessage()); }
		return;
		}

	private final When getWhen()
		throws DataFieldException
		{
		long time = SBDate.adjust(Raw.DAO.getServerTime()
		          + CALLBACK_BY_VO_DAYS * 24 * 60 * 60, 0, 0);
		return new When(time);
		}
	}
