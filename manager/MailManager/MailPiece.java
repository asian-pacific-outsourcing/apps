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
import com.shanebow.dao.*;

public final class MailPiece
	{
	public static final MailPiece[] AVAILABLE =
		{
		new MailPiece("Lee Byers|Welcome Brochure", true),
		};

	private MailPiece( String aDesc, boolean aCallOnArrival )
		{
		fDesc = aDesc;
		fCallOnArrival = aCallOnArrival;
		}

	public boolean callOnArrival() { return fCallOnArrival; }
	@Override public String toString() { return fDesc.toString(); }
	public Comment desc()
		throws DataFieldException
		{
		return Comment.parse(fDesc);
		}

	// PRIVATE
	private final String fDesc;
	private final boolean fCallOnArrival;
	}
