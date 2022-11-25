package com.apo.apps.manager.Mgr;
/********************************************************************
* @(#)CriteriaAction.java.java 1.01 20110304
* Copyright © 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* CriteriaAction.java: Action to allow a manager to specify the default
* work criteria for a particular job Role.
*
* @author Rick Salamone
* @version 1.00 20110505 rts demo version
*******************************************************/
import com.apo.contact.HTRCount;
import com.apo.contact.edit.EditHTRCount;
import com.shanebow.dao.*;
import com.shanebow.dao.edit.*;
import com.apo.employee.Role;
import com.apo.net.Access;
import com.apo.net.SysDAO;
import com.apo.net.ClientOp;
import com.apo.net.Message;
import com.shanebow.ui.LAF;
import com.shanebow.ui.SBAction;
import com.shanebow.ui.SBDialog;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import javax.swing.*;

public final class CriteriaAction
	extends SBAction
	{
	public CriteriaAction( Role aRole )
		{
		super( aRole.code() + " Criteria", 'C', "Specify countries and HTR counts for callers", null );
		fRole = aRole;
		}

	@Override public void actionPerformed(ActionEvent evt)
		{
 		String[] options = { "OK", "Cancel" };
		long access = fRole.access();
		HTRCount htr = null;
		String countryCSV = null;
		try
			{
			Message reply = SysDAO.DAO().syscmd( ClientOp.SC_CRITERIA_GET, "" + access);
			String[] pieces = reply.data().split(Message.SEP); // access, countries, htr
			countryCSV = pieces[1];
			htr = HTRCount.parse(pieces[2]);
			}
		catch (Exception e)
			{
			SBDialog.error("Fetch Criteria Error", e.toString());
			return;
			}
		JPanel panel = buildPanel(access, htr, countryCSV );
		while ( true )
			{
			if ( 0 != JOptionPane.showOptionDialog(null, panel,
				LAF.getDialogTitle(toString()), JOptionPane.DEFAULT_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0] ))
				return;

			try
				{
				SysDAO.DAO().syscmd( ClientOp.SC_CRITERIA_SET,
				                     "" + access, getCountries(), getHTR());
				SBDialog.inform(null, toString(), "Successful" );
				return;
				}
			catch (Exception e) { SBDialog.error("Data Access Error", e.getMessage()); }
			}
		}

	// PRIVATE
	private final Role fRole;
	private EditHTRCount edHTR;
	private static final CountryChooser lstCountry = new CountryChooser();

	private JPanel buildPanel(long aAccess, HTRCount aHTR, String aCountriesCSV)
		{
		String countryTitle = (aAccess == Access.TQ)? "Call in" : "DO NOT CALL";
		lstCountry.setCSV(aCountriesCSV);

		JPanel it = new JPanel(new BorderLayout());
		it.add(LAF.titled(lstCountry, countryTitle), BorderLayout.CENTER);
		if ( aHTR != null && !aHTR.isEmpty() )
			it.add(LAF.titled(edHTR = new EditHTRCount(aHTR), "Max HTR"), BorderLayout.SOUTH);
		return it;
		}

	private String getHTR()
		throws DataFieldException
		{
		if ( edHTR == null ) return "0";
		HTRCount htr = edHTR.get();
		if ( htr.isEmpty())
			throw new DataFieldException("Max HTR must be greater than zero");
		return htr.toString();
		}

	private String getCountries()
		{
		return lstCountry.getCSV();
		}
	}
