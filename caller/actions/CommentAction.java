package com.apo.apps.caller.actions;
/********************************************************************
* @(#)CommentAction.java 1.01 20110221
* Copyright (c) 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* CommentAction: Action to request that a brochure be sent to the lead.
*
* @author Rick Salamone
* @version 1.00 20110221 rts created
*******************************************************/
import com.apo.apps.caller.CallerGUI;
import com.apo.contact.Source;
import com.apo.contact.touch.Touch;
import com.apo.contact.touch.TouchCode;
import com.apo.contact.touch.TouchDAO;
import com.apo.net.Access;
import com.shanebow.dao.Comment;
import com.shanebow.dao.When;
import com.shanebow.dao.edit.DlgComment;
import com.shanebow.ui.SBAction;
import com.shanebow.ui.SBDialog;
import java.awt.event.*;
import javax.swing.*;

public final class CommentAction
	extends SBAction
	{
	public CommentAction()
		{
		super( "Comment", 'C', "Add a comment to this contact's history",	"comment" );
		}

	@Override public void actionPerformed(ActionEvent evt)
		{
		Comment comment = DlgComment.get();
		if ( comment == null )
			return;
		try
			{
			CallerGUI gui = CallerGUI.getInstance();
			Source source = Source.find(Access.getUID());
			Touch.DAO.add( new Touch( gui.getUnedited().id(),
				new When(Touch.DAO.getServerTime()),
				source, TouchCode.COMMENTED, comment ));
			gui.refreshHistory();
			}
		catch (Exception e) { SBDialog.error("Comment Error", e.getMessage()); }
		}
	}