package com.apo.apps.caller;
/********************************************************************
* @(#)ActionList.java 1.00 20110509
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* ActionList: A list of SBActions for the CallerGUI. Methods are
* provided for significant events such as when editor is dirty or
* when a new contact is displayed, so that the actions can be enabled
* or disabled as appropriate.
*
* @author Rick Salamone
* @version 1.00, 20110509 split out from the caller gui
*******************************************************/
import com.apo.apps.caller.actions.*;
import com.apo.contact.Raw;
import com.apo.contact.Dispo;
import com.apo.contact.edit.RawPanel;
import com.apo.order.*;
import com.apo.net.Access;
import com.shanebow.dao.*;
import com.shanebow.dao.edit.DlgAddress;
import com.apo.employee.Role;
import com.shanebow.ui.SBAction;
import com.shanebow.ui.SBDialog;
import com.shanebow.util.SBLog;
import com.shanebow.util.SBProperties;
import com.shanebow.ui.LAF;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

public class ActionList
	extends ArrayList<SBAction>
	{
	private final ActionList fActions = this;
	private final boolean    isManager;
	private final boolean    m_autoNext;

	private final SBAction fAddLeadAction = new AddLeadAction();
	private final SBAction fAddressAction = new AddressAction();
	private final SBAction fBrochureAction = new BrochureAction();
	private final SBAction fChangeEmailAction = new ChangeEmailAction();
	private final SBAction fCommentAction = new CommentAction();
	private final SBAction fEmailAction = new EmailAction();
	private final SBAction fCommitAction = new SaveAction();
//	private final SBAction fSelfAssignAction = new SelfAssignAction();
	private final SBAction fPostponeAction = new PostponeAction();
//	private final SBAction fReleaseAction = new ReleaseAction();

	private final SBAction fQCBAction = new TQActionCB();
	private final SBAction fTQActionFinish = new TQActionFinish();

	private final SBAction fVOActionTOL = new VOActionTOL();
	private final SBAction fVOMailAction = new VOActionMailReq();
	private final SBAction fVOLAction = new VOActionFinish();

	private final SBAction fUNCAction = new AOActionUNC();
	private final SBAction fTOLAction = new AOActionTOL();
	private final SBAction fKOLAction = new AOActionKOL();
	private final SBAction fACBAction = new AOActionCB();
	private final SBAction fLIAction = new AOActionFinish();
	private final SBAction fNewOrderAction = new AOActionNewOrder();

	private class AddressAction extends SBAction
		{
		public AddressAction() { super("Address", 'A', "Edit the contact's address",	"contact" ); }

		@Override public void actionPerformed(ActionEvent evt)
			{
			CallerGUI gui = CallerGUI.getInstance();
			Address address = DlgAddress.get(gui.getAddress());
			if ( address == null )
				return;
			gui.set(Raw.ADDRESS, address);
			}
		}

	private class BrochureAction extends SBAction
		{
		static final String CMD_NAME = "Brochure";
		public BrochureAction()
			{
			super( CMD_NAME, 'B', "Request a brochure for this contact", "brochure" );
			}

		@Override public boolean menuOnly() { return true; }
		@Override public void actionPerformed(ActionEvent evt)
			{
			CallerGUI gui = CallerGUI.getInstance();
			Address address = gui.getAddress();
			if ( address == null || address.isEmpty())
				{
				SBDialog.error(CMD_NAME + " Request Error", "Missing or invalid address");
				return;
				}
			try
				{
				gui.set(Raw.ADDRESS, address);
				Raw.DAO.mailReq( Comment.parse("Lee Byers|Welcome"),
				     new When(Raw.DAO.getServerTime()), Access.empID(), gui.getRawID());
				SBDialog.inform(LAF.getDialogTitle(CMD_NAME),
					"Successfully sent a brochure request to the mailman");
				gui.refreshHistory();
				}
			catch (Exception e) { SBDialog.error("Request Mail Error", e.getMessage()); }
			}
		}

	private class EmailAction extends SBAction
		{
		EmailAction() { super("Welcome Email", 'W', "Re-send the welcome eMail", "envelope" ); }

		@Override public boolean menuOnly() { return true; }
		@Override public void actionPerformed(ActionEvent evt)
			{
			CallerGUI gui = CallerGUI.getInstance();
			EMailAddress eAddress = gui.getEMailAddress();
			if ( eAddress == null || eAddress.isEmpty())
				{
				SBDialog.inputError("Blank or invalid email address");
				return;
				}
			try
				{
				Raw.DAO.reqEmail( gui.getRawID(), eAddress );
				gui.refreshHistory();
				}
			catch (Exception e) { SBDialog.error("Send eMail Error", e.getMessage()); }
			}
		}

/**************************
	private class ReleaseAction extends SBAction
		{
		ReleaseAction()
			{
			super("Release", 'R', "Release this contact for editing by others", "" );
			}
		@Override public void actionPerformed(ActionEvent evt)
			{
			CallerGUI gui = CallerGUI.getInstance();
			gui.release();
			}
		}
**************************/

	private class SaveAction extends SBAction
		{
		SaveAction() { super("Save", 'S', "Commit changes to the system database", "save" ); }

		@Override public void actionPerformed(ActionEvent e)
			{
			CallerGUI gui = CallerGUI.getInstance();
			gui.cmdCommit();
			}
		}

	private final SBAction fWorkloadAction = new SBAction("Workload", 'W',
		"Number of remaining contact calls scheduled for today",	"" )
		{
		@Override public boolean menuOnly() { return true; }
		@Override public void actionPerformed(ActionEvent evt)
			{
			try
				{
				int count = Raw.DAO.countWork("Count");
				SBDialog.inform( "Scheduled Work",
					"Given your current settings, there are\n"
					+ count + " contacts scheduled to be called today." );
				}
			catch (Exception e) { SBDialog.error("Workload Error", e.getMessage()); }
			}
		};

	private final SBAction fNewFetchAction = new SBAction("Fetch", 'F',
		"Test new background fetch",	"contact" )
		{
		@Override public void actionPerformed(ActionEvent evt)
			{
			CallerGUI gui = CallerGUI.getInstance();
			gui.newFetchWork();
			}
		};

	public void setDirty(boolean isDirty, boolean haveDispo)
		{
		fCommitAction.setEnabled( isDirty && haveDispo);
		fAddLeadAction.setEnabled( !isDirty);
		}

	public ActionList(Role role)
		{
		super();
		isManager = role.isManager();
		m_autoNext = SBProperties.getInstance().getBoolean("autoNext");
//if (m_autoNext) add( fNewFetchAction );
		addIfRole( Access.AO,    fAddLeadAction, null );
		addIfAllowed("call.email",       fEmailAction );
		addIfAllowed("call.email.edit",  fChangeEmailAction );
		addIfAllowed("app.address.edit", fAddressAction);
		addIfAllowed("call.brochure",    fBrochureAction);
		addIfAllowed("call.comment",     fCommentAction);
		addIfRole( Access.TQ,            null, fVOActionTOL, fQCBAction,
		                                 fPostponeAction, fTQActionFinish, null);
		addIfRole( Access.VO,            null, fVOActionTOL, fACBAction,
		                                 fVOMailAction, fVOLAction, null);
		addIfRole( Access.AO,    null, fUNCAction, fTOLAction, fKOLAction,
		                                 fACBAction, fNewOrderAction, fLIAction, null );
		long roleAccess = Access.getRole().access();
		if ( isManager )
			add( fCommitAction );
		if ( m_autoNext )
			add(fWorkloadAction);
// add(new com.shanebow.tools.Expose.threads.ActThread(null));
		}

	private void addIfRole( long aRoleAccess, SBAction... aActions )
		{
		if ( Access.getRole().access() == aRoleAccess )
			for ( SBAction action : aActions )
				add( action );
		}

	private void addIfAllowed( String aPropString, SBAction... aActions )
		{
		if ( SBProperties.getInstance().getBoolean(aPropString, false))
			for ( SBAction action : aActions )
				add( action );
		}

	public JComponent getButtonPanel()
		{
		int numButtons = 0;
		for ( SBAction act : this )
			if ( act != null && !act.menuOnly()) ++numButtons;
		JButton[] buttons = new JButton[numButtons];
		int i = 0;
		for ( SBAction action : fActions )
			if (action != null && !action.menuOnly()) buttons[i++] = action.makeButton();
		return LAF.getCommandRow(buttons);
		}

	public void enableContactActions(Raw aRaw)
		{
		boolean on = (aRaw != null);
		Dispo dispo = (aRaw != null)? aRaw.dispo() : Dispo.XX;
		long roleAccess = Access.getRole().access();
		fEmailAction.setEnabled(on);
		fAddressAction.setEnabled(on);
		fBrochureAction.setEnabled(on);
		fCommentAction.setEnabled(on);
		fChangeEmailAction.setEnabled(on);
//		fSelfAssignAction.setEnabled(on);
		fPostponeAction.setEnabled(on);
		fTQActionFinish.setEnabled(on);

		fVOLAction.setEnabled(dispo.equals(Dispo.BR));
		fVOActionTOL.setEnabled(on && !dispo.equals(Dispo.BR));
		fVOMailAction.setEnabled(on);
		fACBAction.setEnabled(on && !dispo.equals(Dispo.BR));

		if ( roleAccess == Access.AO )
			{
			fAddLeadAction.setEnabled(!dispo.equals(Dispo.AOF));
			on = on && !dispo.equals(Dispo.AOF);
			fUNCAction.setEnabled(on);
			fTOLAction.setEnabled(on);
			fKOLAction.setEnabled(on);
			fNewOrderAction.setEnabled(on);
			fLIAction.setEnabled(dispo.equals(Dispo.AOF));
			fACBAction.setEnabled(on);
			if ( dispo.equals(Dispo.AOF))
				fireAction("LI");
			else if ( dispo.equals(Dispo.AOP))
				{
				CallerGUI gui = CallerGUI.getInstance();
				gui.editOrder(gui.getMostRecentOrder());
				}
			}
		if ( dispo.equals(Dispo.BR)
		&&  (Access.getRole().access() == Access.VO))
			fireAction("VOL");
		}

	public final void fireAction(String aKey)
		{
		long roleAccess = Access.getRole().access();
		if ( aKey.equals("CB"))
			fACBAction.actionPerformed(null);
		else if ( aKey.equals("TOL") && (roleAccess == Access.VO))
			fVOActionTOL.actionPerformed(null);
		else if ( aKey.equals("VOL"))
			fVOLAction.actionPerformed(null);
		else if ( aKey.equals("LI"))
			fLIAction.actionPerformed(null);
		}

	protected final void log ( String msg, Object... args )
		{
		SBLog.write( getClass().getSimpleName(), String.format(msg,args));
		}
	}
