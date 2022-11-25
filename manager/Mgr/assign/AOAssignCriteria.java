package com.apo.apps.manager.Mgr.assign;
/********************************************************************
* @(#)AOAssignCriteria.java.java 1.01 20110304
* Copyright (c) 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* AOAssignCriteria.java: Action to allow AO manager to determine what
* work to assign.
*
* @author Rick Salamone
* @version 1.00 20110505 rts demo version
*******************************************************/
import com.shanebow.dao.*;
import com.shanebow.dao.edit.*;
import com.apo.contact.SBRadioGroup;
import com.apo.employee.Role;
import com.apo.net.Access;
import com.apo.net.SysDAO;
import com.apo.net.ClientOp;
import com.apo.net.Message;
import com.shanebow.ui.LAF;
import com.shanebow.ui.SBAction;
import com.shanebow.ui.SBDialog;
import com.shanebow.util.SBProperties;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import javax.swing.*;

public final class AOAssignCriteria
	extends SBAction
	{
	public AOAssignCriteria()
		{
		super( "Assign Whom", 'A', "Change to dispos you are assigning", null );
		}

	@Override public void actionPerformed(ActionEvent evt)
		{
 		String[] options = { "OK", "Cancel" };
		JPanel panel = buildPanel();
		while ( true )
			{
			if ( 0 != JOptionPane.showOptionDialog(null, panel,
				LAF.getDialogTitle(toString()), JOptionPane.DEFAULT_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0] ))
				return;

			try
				{
				SysDAO.DAO().syscmd( ClientOp.SC_CRITERIA_SET, buildWhere());
				SBDialog.inform(null, toString(), "Successful" );
				return;
				}
			catch (Exception e) { SBDialog.error("Data Access Error", e.getMessage()); }
			}
		}

	// PRIVATE
	private SBRadioGroup<String>  radio;
	private static final String[] CHOICES =
		{ "VL", "TOL", "KOL", "CO" };
	private static final CountryChooser lstCountry = new CountryChooser();

	private final String buildWhere()
		throws DataFieldException
		{
		String who = (String)radio.getSelected();
		if ( who == null )
			throw new DataFieldException("You must specify who you spoke with");
		if ( who.equals("No Answer")) return who;
		return who;
		}

	private JPanel buildPanel()
		{
		SBProperties props = SBProperties.getInstance();
		String[] pieces = props.get("usr.assign.ao").split(Message.SEP); // access, countries, htr
		String dispo = pieces[0];
		String countriesCSV = pieces[1];

		String countryTitle = "DO NOT CALL";
		lstCountry.setCSV(countriesCSV);

		JPanel spokeWith = new JPanel(new GridLayout(1,0));
		LAF.titled(spokeWith, "Spoke With");
		radio = new SBRadioGroup<String>( spokeWith, CHOICES );

		JPanel it = new JPanel(new BorderLayout());
		it.add( spokeWith, BorderLayout.NORTH );
		it.add(LAF.titled(lstCountry, countryTitle), BorderLayout.CENTER);
		return it;
		}

	private String getCountries()
		{
		return lstCountry.getCSV();
		}
	}
