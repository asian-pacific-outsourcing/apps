package com.apo.apps.caller;
/********************************************************************
* @(#)DlgContact.java	1.13 10/04/07
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* DlgContact: allows the user to add, edit or delete a contact.
*
* @author Rick Salamone
* @version 1.01 20100627 RTS position at lower right corner of screen
* @version 1.02 20100627 RTS responds to change in theme menu
* @version 1.03 20101005 RTS added history tab
* @version 1.04 20101006 RTS added touches
* @version 1.05 20101010 RTS added comments, cleaned up button bar
* @version 1.06 20101012 RTS added contact search for id feature
* @version 1.07 20101021 RTS using common DlgComment
*******************************************************/
import com.shanebow.dao.ContactID;
import com.apo.contact.Raw;
import com.apo.contact.DlgDetails;
import com.apo.order.*;
import com.shanebow.ui.LAF;
import com.shanebow.util.SBLog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public final class DlgContact
	extends DlgDetails
	implements Titled, OrderTablePopup
	{
	private final CallerGUI fGUI = new CallerGUI(this);
	private Raw fRaw;

	public DlgContact(JFrame f)
		{
		super(f);

		JPanel top = new JPanel(new BorderLayout());
		top.setBorder(LAF.getStandardBorder());
		top.setPreferredSize(new Dimension(600,375));
		top.add(fGUI, BorderLayout.CENTER);
		top.add(fGUI.getControlPanel(), BorderLayout.SOUTH);
		setContentPane(top);
		}

	public final void setOrder(Order aOrder)
		{
		if ( fGUI.cmdFetchID(aOrder.rawID()))
//		if (( fRaw == null || !fRaw.id().equals(aOrder.rawID()))
//		&& fGUI.cmdFetchID(aOrder.rawID()))
			fRaw = fGUI.getUnedited();
		super.setVisible(true);
		}

	@Override public void setContact(Raw aRaw)
		{
		if ( fRaw == null && aRaw == null ) return;
		if ( fRaw != null && fRaw.equals(aRaw)) return;
		fRaw = aRaw;
		if ( isVisible())
			fGUI.display(fRaw);
		}

	public CallerGUI getGUI() { return fGUI; }

	@Override public void setVisible(boolean visible)
		{
		if ( isVisible() == visible )
			return;
		if ( !visible )
			fGUI.release();
		else if ( fRaw != null )
			fGUI.display(fRaw);
		super.setVisible(visible);
		}
	}
