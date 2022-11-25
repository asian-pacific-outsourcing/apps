package com.apo.apps.AppServer.monitor;
/********************************************************************
* @(#)UserStats.java 1.00 10/07/09
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* UserStats: Compiles user information for the server.
*
* @author Rick Salamone
* @version 1.00 20100709 RTS created
* @version 1.01 20101016 RTS master user list and static fetch method
* 20110430 rts 1.02 added ability to get stats as CSV
* 20110503 rts 1.03 added clearStats method
* 20110505 rts 1.04 removed criteria stuff - now in mgr app
*******************************************************/
import com.apo.apps.AppServer.rox.ClientChannel;
import com.shanebow.dao.ContactID;
import com.apo.net.*;
import com.apo.employee.User;
import com.shanebow.util.SBArray;
import com.shanebow.util.SBLog;
import java.util.*;

public class UserStats
	{
	// Class data & methods
//	private static final List<UserStats>  _allUsers = new Vector<UserStats>();
	private static final SBArray<UserStats>  _allUsers = new SBArray<UserStats>(50);
	public static final Iterable<UserStats> getAll()
		{
		return _allUsers;
		}

	public static UserStats fetch( int uid )
		{
		for ( UserStats ur : _allUsers )
			if ( ur.uid() == uid )
				return ur;
		return null;
		}

	// Instance data & methods
	protected final Monitor  m_monitor;
	private final User       m_user;
	private final long       m_access;
	private String           m_version;
	private ClientChannel    m_channel;  // for bidirectional communication
	protected int            m_modelRow; // what row are we in the table model
	private long             m_loginTime = 0;
	private long             m_lastTime = 0;
	private byte             m_lastOp;
	private int              m_seq = 0;
	private int              m_requests = 0;   // total messages processed
	private int              m_errors = 0;     // number of error messages

	public UserStats( User usr, long access, long loginTime, Monitor mon, String version )
		{
		m_user = usr;
		m_access = access;
		m_version = version;
		m_loginTime = loginTime;
		m_monitor = mon;
		synchronized (UserStats.class)
			{
// 			_allUsers.add(this);
 			_allUsers.insert(this);
			}
		}

	@Override public int hashCode() { return m_user.hashCode(); }
	public void logBackIn( long loginTime, String version )
		{
		m_version = version;
		m_loginTime = loginTime;
		}

	public void clearStats()
		{
		m_requests = 0;
		m_errors = 0;
		}

	public final void setChannel(ClientChannel c) { m_channel = c; }
	public final ClientChannel getChannel() { return m_channel; }
	public final boolean send( byte opCode, byte aFlags, String msg )
		{
		if ( m_channel == null )
			{
			log("Cannot send to " + login() + ": No channel");
			return false;
			}
		m_channel.send(opCode, aFlags, (short)0, msg);
		return true;
		}

	// LOCKING - these methods are used to track the locks
	// set on contacts by this user
	private final SBArray<ContactID> fLocks = new SBArray<ContactID>(5);
	public final String lockString(char aSeparator)
		{
		synchronized(fLocks)
			{
			if (fLocks.isEmpty()) return "";
			StringBuilder csv = new StringBuilder();
			for ( ContactID id : fLocks )
				csv.append( id.toString()).append(aSeparator);
			csv.deleteCharAt(csv.length()-1);
			return csv.toString();
			}
		}

	public void acquiredID(long id)
		{
		synchronized(fLocks) { fLocks.add(new ContactID(id)); }
		}
	public void releasedID(ContactID fID)
		{
		synchronized(fLocks) { fLocks.remove(fID); }
		}
	public String releaseAllLocks()
		{
		synchronized(fLocks)
			{
			String csv = lockString(',');
			fLocks.clear();
			return csv;
			}
		}

	protected final void log ( String fmt, Object... args )
		{
		SBLog.write( getClass().getSimpleName() + " " + m_user.login(),
		             String.format(fmt, args));
		}

	public void operation( byte op, long time )
		{
		m_lastTime = time;
		m_lastOp = op;
		++m_requests;
		}

	public void error()
		{
		++m_errors;
		}

	public void error(String msg)
		{
		++m_errors;
		log( msg );
		}

	public final short    uid()       { return m_user.id(); }
	public final User     user()      { return m_user; }
	public final String   login()     { return m_user.login(); }
	public final String   name()      { return m_user.name(); }
	public final long     access()    { return m_access; }
	public final String   version()   { return m_version; }
	public final long     loginTime() { return m_loginTime; }
	public final long     lastTime()  { return m_lastTime; }
	public final int      requests()  { return m_requests; }
	public final int      errors()    { return m_errors; }
	public final byte     lastOp()    { return m_lastOp; }
	public final Monitor  getMonitor(){ return m_monitor; }
	@Override public String toString(){ return m_user.login(); }

	public String csvHeader()
		{
		return ",Code,Name,Start,Last,Contact,Requests,Errors";
		}

	public final boolean isConnected() { return m_channel != null; }
		
	public String csv()
		{
		return (isConnected()? version() : "0") + "," + login() + ","
		     + name() + "," + loginTime() + "," + lastTime() + ","
//		     + currentID() + "," + requests() + "," + errors();
		     + lockString(' ') + "," + requests() + "," + errors();
		}

	public int getProduction(int index) { return -1; }
	public int getProductionTotal() { return 0; }
	public final int rate()
		{
		int den = (int)(lastTime() - loginTime());
		if ( den <= 0 )
			return 0;
		return (360 * getProductionTotal()) / den;
		}

	public boolean equals( UserStats other ){ return this.user() == other.user(); }
	}
