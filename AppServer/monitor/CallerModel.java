package com.apo.apps.AppServer.monitor;
/********************************************************************
* @(#)CallerModel.java	1.00 10/06/29
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* CallerModel: Table data model to manage caller production stats.
*
* @author Rick Salamone
* @version 1.00, 20100629 rts created as TQModel
* @version 2.00, 20101023 generalized to handle any dispoing caller type
* @version 2.01, 20101116 accomodates BackedContactList changes
* @version 2.02, 20110418 eliminated backup to text file
* @version 2.03, 20110423 added changes for LO Role
* @version 2.04, 20110504 rts major changes to persist criteria and give to mgr
*******************************************************/
import com.apo.employee.Role;
import com.apo.net.Access;
import com.apo.contact.Dispo;
import com.shanebow.util.SBDate;
import com.shanebow.util.SBLog;
import com.shanebow.util.SBProperties;

public final class CallerModel
	extends UsrTableModel
	implements Monitor
	{
	private final Role fRole;
	private String fCriteriaString;

	public CallerModel( Role aRole )
		{
		super(aRole.saveDispos());
		fRole = aRole;
		long access = aRole.access();
		SBProperties props = SBProperties.getInstance();
		setCriteria( getMaxHTR(), getCountriesCSV());
		}

	public int getMaxHTR()
		{
		return SBProperties.getInstance().getInt("usr."+fRole.code()+".maxHTR",
		        (fRole.access() < Access.AO)? 5 : 0 );
		}

	public String getCountriesCSV()
		{
		return SBProperties.getInstance().getProperty("usr."+fRole.code()+".countries","");
		}

	// 5/4/2011 NEW NEW CRITERIA
	public final void setCriteria( int aMaxHTR, String aCountriesCSV )
		{
		SBProperties props = SBProperties.getInstance();
		props.set("usr."+fRole.code()+".maxHTR", "" + aMaxHTR );
		props.set("usr."+fRole.code()+".countries", aCountriesCSV );
		fCriteriaString
		      = "WHERE page=0"
		      + " AND disposition IN " + Dispo.dbCriteriaList(fRole.fetchDispos())
		      + ((aMaxHTR > 0)? " AND noAnswer <=" + aMaxHTR : "")
		      + countryClause(aCountriesCSV);
System.out.println("NEW setCriteria " + fRole.code() + ": '" + fCriteriaString + "'");
		}

	public final String getWhere() { return fCriteriaString; }
	private String countryClause(String aCountries)
		{
		if ( aCountries.isEmpty())
			return "";
//		int numCountries = aCountries.split(",").length;
		return " AND countryID "
		          + ((getAccess() == Access.TQ)? "IN (" : "NOT IN (")
		          + aCountries + ")";
		}

	// implement Monitor
	@Override public String  getTitle()  { return fRole.code(); }
	@Override public String  getToolTip(){ return fRole.shortName() + " Production"; }
	@Override public Dispo[] getDispos() { return fRole.saveDispos(); }
	@Override public long    getAccess() { return fRole.access(); }
	@Override public UsrTableModel getModel() { return this; }

	@Override public final CallerStats addUser(com.apo.employee.User usr,
		long loginTime, long access, String version )
		{
		CallerStats cs = new CallerStats( usr, access, loginTime, this, version );
		cs.m_modelRow = super.addUser( cs );
		return cs;
		}
	} // 86
