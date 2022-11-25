package com.apo.apps.AppServer.monitor;
/********************************************************************
* @(#)MgrStats.java	1.00 10/07/09
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* MgrStats: Compiles user information for the server.
*
* @version 1.00 07/09/10
* @author Rick Salamone
* 20100709 RTS 1.00 created
*******************************************************/
import com.apo.net.*;
import com.shanebow.util.CSV;
import com.apo.employee.User;
import java.util.*;
import javax.swing.SwingUtilities;

public final class MgrStats
	extends UserStats
	{
	// Class data & methods
	private static final MgrModel _model = new MgrModel();
	public  static final MgrModel getModel() { return _model; }

	MgrStats( User usr, long access, long loginTime,String version )
		{
		super( usr, access, loginTime, (Monitor)getModel(), version );
		}

	@Override public int getProduction(int index) { return 0; }
	@Override public int getProductionTotal() { return 0; }
	}
