package com.apo.apps.AppServer;
/********************************************************************
* @(#)Monitors.java 1.00 10/06/29
* Copyright (c) 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* Monitors: Creates a static Monitor for each client Role and
* maintains these in a static array.
*
* @author Rick Salamone
* 20110423 rts added LO role
* 20110505 rts clean up and move criter to mgr app
*******************************************************/
import com.apo.employee.Role;
import com.apo.net.Access;
import com.apo.apps.AppServer.monitor.*;

public class Monitors
	{
	public static final Monitor DM = (Monitor)(DMStats.getModel());
	public static final Monitor TQ = new CallerModel( Role.TQ );
	public static final Monitor VO = new CallerModel( Role.VO );
	public static final Monitor AO = new CallerModel( Role.AO );
	public static final Monitor LO = new CallerModel( Role.LO );
	public static final Monitor MGR = (Monitor)(MgrStats.getModel());

	public static final Monitor[] all = { DM, TQ, VO, AO, LO, MGR };
	public static final Monitor find( long access )
		{
		if ((access&Access.MGR) == Access.MGR)
			return MGR;
		for ( Monitor m : all )
			if ( m.getAccess() == access )
				return m;
		return null;
		}
	}
