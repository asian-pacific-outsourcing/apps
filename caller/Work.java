package com.apo.apps.caller;
/********************************************************************
* @(#)Work.java 1.00 20110602
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* Work: Represents all information about a contact (raw, history,
* and orders) and provides methods for initializing via calls
* to the server.
*
* @author Rick Salamone
* @version 1.00, 20110602
* 20110602 rts created
*******************************************************/
import com.apo.contact.Raw;
import com.apo.contact.touch.Touch;
import com.apo.order.Order;
import com.apo.employee.Role;
import com.apo.net.Access;
import com.shanebow.ui.SBDialog;
import com.shanebow.util.SBLog;
import com.shanebow.util.SBProperties;
import java.util.List;
import java.util.Vector;

public final class Work
	{
	private static final int POLL_WORK_DELAY = 60;

	private Raw fRaw;
	private List<Order> fOrderList;
	private List<Touch> fTouchList;

	public Work()
		{
		}

	public Raw getRaw() { return fRaw; }
	public List<Order> getOrders() { return fOrderList; }
	public List<Touch> getHistory() { return fTouchList; }
	@Override public String toString() { return fRaw.toString(); }

	public void fetch()
		{
		while ( fRaw == null )
			{
			try
				{
				if ((fRaw = Raw.DAO.getWork("x")) != null )
					{
					if ( Access.allowedTo(Access.VO|Access.AO|Access.LO))
						fTouchList = Touch.DAO.fetch(fRaw.id());
					if ( Access.allowedTo(Access.AO|Access.LO))
						Order.DAO.fetch(fOrderList = new Vector<Order>(), fRaw.id());
					}
				else // No work is scheduled right now
					{
					try { Thread.sleep(POLL_WORK_DELAY*1000); }
					catch (InterruptedException ex) { log("Producer Read INTERRUPTED"); }
					}
				}
			catch (Exception e) { SBDialog.fatalError(e.getMessage()); }
			}
		}

	protected final void log ( String msg, Object... args )
		{
		SBLog.write( getClass().getSimpleName(), String.format(msg,args));
		}
	}
