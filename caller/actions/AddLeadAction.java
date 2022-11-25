package com.apo.apps.caller.actions;
/********************************************************************
* @(#)AddLeadAction.java 1.01 20110224
* Copyright (c) 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* AddLeadAction: Displays a modal dialog, with a tab for each
* available conversion.
*
* @author Rick Salamone
* @version 1.00 20110224 rts created
* @version 1.50 20110307 rts allows for multiple contacts at same phone #
*******************************************************/
import com.apo.apps.caller.CallerGUI;
import com.apo.contact.Raw;
import com.apo.contact.Dispo;
import com.apo.contact.HTRCount;
import com.apo.contact.RawDAO;
import com.apo.contact.Source;
import com.apo.contact.touch.Touch;
import com.apo.contact.touch.TouchCode;
import com.apo.contact.edit.RawPanel;
import com.shanebow.dao.*;
import com.shanebow.dao.edit.EditComment;
import com.apo.net.Access;
import com.shanebow.ui.LAF;
import com.shanebow.ui.SBAction;
import com.shanebow.ui.SBDialog;
import com.shanebow.ui.calendar.MonthCalendar;
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import javax.swing.*;

public final class AddLeadAction
	extends SBAction
	{
	/**
	* Constructor. 
	*/
	public AddLeadAction()
		{
		super("Add", 'A', "Add a new lead to the system",	"contact" );
		}

	@Override public void actionPerformed(ActionEvent evt)
		{
		CallerGUI gui = CallerGUI.getInstance();
		Source caller = Source.find(Access.getUID());
		// The record we're building: Fill in the default values
		DataField[] rawFields = new DataField[Raw.NUM_FIELDS];
		rawFields[Raw.ID] = ContactID.NEW_CONTACT;
		rawFields[Raw.TYPE] = ContactType.BIZ;
		rawFields[Raw.DISPO] = Dispo.ACB;
		rawFields[Raw.ADDRESS] = Address.BLANK;
		rawFields[Raw.CALLER] = caller;
		rawFields[Raw.HTRCOUNT] = HTRCount.ZERO_HTR;
		rawFields[Raw.CALLBACK] = new When(Raw.DAO.getServerTime());
		rawFields[Raw.PAGE] = CheckOutID.CHECKED_IN;
		rawFields[Raw.WEBSITE] = WebAddress.BLANK;

		// Screeen #1 - Get the phone numbers
		String prompt = "<HTML>Please enter <B>all phone numbers</B> to,"
		              + " check whether this<BR> guy is already in the system,"
		              + " thereby saving<BR> you a considerable amount of typing."
		              + "<BR><HR><BR>";
		int[] fieldNumbers = { Raw.PHONE, Raw.MOBILE, Raw.ALTPHONE };
		String[] options = { "Search", "Cancel" };
		if ( !getFields( prompt, rawFields, fieldNumbers, options, true, null ))
			return;

// dump( "After screen #1", rawFields );

		// Lookup the phone #'s: if found display the contact for editing
		Raw raw = findByPhone(rawFields);
		if ( raw != null ) // contact already exists bring him up in editor
			{
			gui.display(raw);
			return;
			}

		// Screeen #2 - Phones numbers not found, get other crucial data
		JPanel south = southPanel();

		prompt = "<HTML>The contact was not found via a phone number search.<BR>"
		       + "Please enter the following information to create a new<BR>"
		       + "record for this lead."
		       + "<BR><HR><BR>";
		fieldNumbers = new int[]{ Raw.NAME, Raw.COMPANY, Raw.POSITION,
		                 Raw.EMAIL, Raw.COUNTRYID, Raw.HOMELAND, Raw.ADDRESS, Raw.TYPE };
		options[0] = "Add";
		if ( !getFields( prompt, rawFields, fieldNumbers, options, false, south ))
			return;
		rawFields[Raw.CALLBACK] = new When(fCalendar.getDate());
		raw = new Raw(rawFields);

		// dump( "After screen #2", rawFields );
		long now = Touch.DAO.getServerTime();
		try
			{
			When nowWhen = new When(now);
			EmpID me = Access.empID();
			ContactID rawID = Raw.DAO.addLead(raw, me, TouchCode.IDBROAD, "" );
			rawFields[Raw.ID] = rawID;
			if ( fSendBrochure.isSelected())
				Raw.DAO.mailReq(Comment.parse("Lee Byers|Welcome"), nowWhen, me, rawID);
			Comment comment = getComment();
			if ( comment != null )
				Touch.DAO.add( new Touch(rawID, nowWhen, caller, TouchCode.COMMENTED, comment));
			}
		catch (Exception e)
			{
			SBDialog.error("Data Access Error", e.getMessage());
			return;
			}
//		gui().display(new Raw(rawFields));
		}

	private void dump(String aTitle, DataField[] aValues )
		{
		new Raw(aValues).dump(aTitle);
		}

	private Raw findByPhone(DataField[] rawFields)
		{
		String phones = Raw.DAO.makeList(rawFields[Raw.PHONE],
		                         rawFields[Raw.MOBILE],
		                         rawFields[Raw.ALTPHONE]);

		String query = "SELECT * FROM " + Raw.DB_TABLE
		      + " WHERE phone IN (" + phones + ")"
		      + " OR mobile IN (" + phones + ")"
		      + " OR altPhone IN (" + phones + ")";
		Vector<Raw> hits = new Vector<Raw>();
		try
			{
			Raw.DAO.fetch(hits, -1, query);
			if ( hits.size() == 0 ) return null;

			String[] options = { "Use Selected", "Add New" };
			String prompt = "<HTML>This list contains contacts with the<BR>"
			       + "phone number(s) you entered. Select your guy<BR>"
			       + "from the list and click <I>" + options[0] + "</I>.<BR>"
			       + "Otherwise click on <I>" + options[1] + "</I> if<BR>"
			       + "your contact is not in the list";
			JPanel panel = new JPanel(new BorderLayout());
			JList hitList = new JList(hits);
			hitList.setVisibleRowCount(5);
			hitList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			hitList.setSelectedIndex(0);
			panel.add(new JLabel(prompt), BorderLayout.NORTH);
			panel.add(new JScrollPane(hitList), BorderLayout.CENTER);
			int selected = JOptionPane.showOptionDialog(CallerGUI.getInstance(),
				panel,	LAF.getDialogTitle("Phone Matches"), JOptionPane.DEFAULT_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[0] );
			if ( selected != 0 ) return null;
			return (Raw)hitList.getSelectedValue();
			}			
		catch (Exception e)
			{
			SBDialog.error("Get Phone: Data Access Error", e.getMessage());
			return null;
			}
		}

	private boolean getFields(String aPrompt, DataField[] aFieldValues,
		int[] aFieldNumbers, String[] options, boolean allowBlanks, JPanel south )
		{
		JPanel panel = new JPanel(new BorderLayout());
		JLabel prompt = new JLabel(aPrompt);
		RawPanel editor = new RawPanel((byte)1, aFieldNumbers, null);
		panel.add(prompt, BorderLayout.NORTH);
		panel.add(editor, BorderLayout.CENTER);
		if ( south != null )
			panel.add(south, BorderLayout.SOUTH);

		while (true) // until user quits or enters valid data
			{
			int selected = JOptionPane.showOptionDialog(CallerGUI.getInstance(),
				panel,	LAF.getDialogTitle("Add Contact"), JOptionPane.DEFAULT_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[0] );
			if ( selected != 0 ) return false;
			if (!editor.validInputs()
			|| (!allowBlanks && editor.hasBlankFields())
			||  !isValid(south) )
				continue; // try again
			try
				{
				for ( int f : aFieldNumbers )
					aFieldValues[f] = editor.get(f);
				return true;
				}
			catch (Exception x) { SBDialog.error("Add: Unexpected Error", x.getMessage()); }
			}
		}

	private EditComment fComment = new EditComment();
	private JCheckBox fSendBrochure = new JCheckBox("Send Brochure", true);
	private MonthCalendar fCalendar = new MonthCalendar();
	private JPanel southPanel()
		{
		JPanel left = new JPanel(new BorderLayout());
		left.add(fComment, BorderLayout.CENTER);
		left.add(fSendBrochure, BorderLayout.SOUTH);
		LAF.titled(fComment, "Optional Comment");

		JPanel right = new JPanel(new BorderLayout());
		right.add(fCalendar, BorderLayout.CENTER);
		LAF.titled(right, "Call back date");
		fCalendar.setPreferredSize(new Dimension(140,140));

		JPanel it = new JPanel(new BorderLayout());
		it.add(left, BorderLayout.CENTER);
		it.add(right, BorderLayout.EAST);
		return it;
		}

	private boolean isValid(JPanel southPanel)
		{
		return (southPanel == null)? true
				: (getComment() == null)? false
				: (fCalendar.getDate() <= Raw.DAO.getServerTime())?
		           SBDialog.inputError("Call back date must be in the future")
		     : true;
		}

	private Comment getComment()
		{
		try
			{
			return fComment.get();
			}
		catch (DataFieldException e)
			{
			SBDialog.inputError( e.getMessage());
			return null;
			}
		}
	}
