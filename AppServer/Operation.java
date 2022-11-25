package com.apo.apps.AppServer;
/********************************************************************
* @(#)Operation.java 1.00 10/06/29
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* Operation: The 'heavy lifting' for the server done in the background.
* There is a method for each client operation which performs the necessary
* database operations and encodes a reply into a ByteBuffer. The main loop:
*
* @author Rick Salamone
*******************************************************/
import com.apo.apps.AppServer.monitor.*;
import com.apo.apps.AppServer.rox.ClientRequest;
import com.apo.apps.AppServer.rox.ClientChannel;
import com.apo.admin.OrderDBDAO;
import com.apo.admin.RawDBDAO;
import com.apo.admin.SysDBDAO;
import com.apo.admin.TouchDB;
import com.apo.admin.TouchDBDAO;
import com.apo.contact.*;
import com.apo.contact.Dispo;
import com.apo.contact.touch.Touch;
import com.apo.contact.touch.TouchCode;
import com.apo.order.Order;
import com.shanebow.dao.*;
import com.apo.employee.Role;
import com.apo.net.*;
import com.shanebow.util.CSV;
import com.shanebow.dao.DBStatement;
import com.apo.employee.User;
import com.shanebow.util.SBDate;
import com.shanebow.util.SBLog;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.nio.ByteBuffer;

public class Operation
	{
	private static final SysDBDAO fSysDAO = new SysDBDAO();
	private static final OrderDBDAO fOrderDBDAO = new OrderDBDAO();
	private static final RawDBDAO RawLead = new RawDBDAO();
	private static final HashMap<Long, String> _roleStrings = new HashMap<Long, String>();
	private static WorkQueue fTQWorkQueue;
	static
		{
		Order.DAO = fOrderDBDAO;
		Raw.DAO = RawLead;
		Touch.DAO = new TouchDBDAO();
		for ( Role role : Role.getAll())
			_roleStrings.put( new Long(role.access()), role.marshall());
		fTQWorkQueue = new WorkQueue((CallerModel)(Monitors.find(Access.TQ)), -1);
		}
	protected final Audience fNewOrderAudience = new Audience( new EmpID(7), new EmpID(13));
	protected final Audience fModOrderAudience = new Audience( new EmpID(7), new EmpID(8), new EmpID(13));

	protected final Message fRequest;
	protected UserStats fUser;
	protected ByteBuffer bb = null;
	public ByteBuffer getReply() { return bb; }

	private UserStats fetchUser()
		{
		return fUser = UserStats.fetch(fRequest.uid());
		}

	private UserStats fetchUser( int uid )
		{
		return fUser = UserStats.fetch( uid );
		}

	private ByteBuffer doLogout(ClientChannel aChannel)
		{
		if ( fRequest.uid() > 0 )
			fetchUser();
log("logout %d %s on channel %s", fRequest.uid(),
((fUser==null)? "?":fUser.toString()), aChannel.toString());
		if ( fUser == null )
			{
			for (UserStats us : UserStats.getAll())
				if ( us.getChannel() == aChannel )
					{
					fUser = us;
log("logout found user %d %s on channel %s", fRequest.uid(),
fUser.toString(), aChannel.toString());
					break;
					}
				}
		if ( fUser != null )
			{
			String lockedIDcsv = fUser.releaseAllLocks();
			if ( !lockedIDcsv.isEmpty())
				releaseLocks(lockedIDcsv);
			fUser.setChannel(null);
			}
		return encode( Snafu.CODE_NONE, "" );
		}

	public void releaseLocks( final String aIDcsv )
		{
		DBStatement db = null;
		ResultSet rs = null;
		try
			{
			db = new DBStatement();
			String stmt = "UPDATE " + Raw.DB_TABLE
		            + " SET page=0 WHERE id IN (" + aIDcsv + ")";
			db.executeUpdate( stmt );
log("releaseLocks user %d released ids: %s", fRequest.uid(), aIDcsv);
			}
		catch (Throwable t)
			{
			log( "Logout Error: %s\n  Request: %s\n  Offending SQL: %s",
			t.getMessage(), fRequest.toString(), ((db == null)? "none" : db.getSQL()));
			}
		finally { if (db != null) db.close(); }
		}

	public Operation(ClientRequest request)
		{
		fRequest = request.message;
// System.out.println(">" + fRequest);
		if ( fRequest.hasSnafu())
			{
			bb = error( fRequest.getSnafu());
			return;
			}
		byte op = fRequest.op();
/**************/
		if ( op != ClientOp.CODE_LOGIN  && op != ClientOp.CODE_LOGOUT )
			{
			if ( fetchUser( fRequest.uid()) == null )
				{
				bb = error( Snafu.ACCESS_DENIED );
				return;
				}
			if ( op != ClientOp.CODE_SYSCMD )
				fUser.operation(op, fRequest.time());
			fUser.setChannel(request.channel);
			}
/**************/
		switch( op )
			{
			case ClientOp.CODE_LOGIN:     bb = doLogin(request.channel);   break;
			case ClientOp.CODE_LOGOUT:    bb = doLogout(request.channel);  break;
			case ClientOp.CODE_ADDLEAD:   bb = addLead();   break;
			case ClientOp.CODE_GETWORK:   bb = getWork();   break;
			case ClientOp.CODE_CANWORK:   bb = canWork();   break;
			case ClientOp.CODE_ADDHIST:   bb = addHist();   break;
			case ClientOp.CODE_GETHIST:   bb = getHist();   break;
			case ClientOp.CODE_MODLEAD:   bb = modLead();   break;
			case ClientOp.CODE_GETLEAD:   bb = getLead();   break;
			case ClientOp.CODE_QUERY:     bb = query();     break;
			case ClientOp.CODE_UPDATE:    bb = update();    break;
			case ClientOp.CODE_CNTWORK:   bb = cntWork();   break;
			case ClientOp.CODE_EMAIL:     bb = email();     break;
			case ClientOp.CODE_ASSIGN:    bb = assign();    break;
			case ClientOp.CODE_BROCHURE:  bb = brochure();  break;
			case ClientOp.CODE_MAILSENT:  bb = mailsent();  break;
			case ClientOp.CODE_COUNT:     bb = count();     break;
			case ClientOp.CODE_ECHO:      bb = echo();      break;
			case ClientOp.CODE_NEWORDER:  bb = newOrder();  break;
			case ClientOp.CODE_ORDMOD:    bb = ordMod();    break;
			case ClientOp.CODE_BROADCAST: bb = broadcast(); break;
			case ClientOp.CODE_SYSCMD:    bb = syscmd();    break;

			default: bb = error( Snafu.BAD_OP ); break;
			}
		}

	protected final void log ( String fmt, Object... args )
		{
		SBLog.write( getClass().getSimpleName(), String.format(fmt, args));
		}

	protected final ByteBuffer error(Snafu snafu)
		{
		return encode(snafu.code(), "");
		}

	private ByteBuffer encode( byte err, String data )
		{
		if ( fRequest.forwarded() // came from a satelite server
		|| ( !fRequest.wantReply() && (err == Snafu.CODE_NONE)))
			return null;
		byte flags = fRequest.wantReply()? 0 : Message.UNSOLICITED;
		return Message.encode(fRequest.op(), err, flags, fRequest.uid(), fRequest.seq(), data);
		}

	/**
	* 0: login name
	* 1: password
	* 2: access
	* 3: app version
	*/
	private ByteBuffer doLogin(ClientChannel aChannel)
		{
		String[] pieces = fRequest.data().split(Message.SEP, 4 );
		String version = (pieces.length > 3)? pieces[3] : "?";
		if ( pieces.length < 3 )
			return error( Snafu.BAD_CSV_COUNT );
		User usr = User.parse( pieces[0] );
		if ( usr == User.XX )
			return error( Snafu.BAD_LOGIN_NAME );
		if ( !usr.password().equals( pieces[1] ))
			return error( Snafu.BAD_PASSWORD );
		long access = 0;
		try
			{
			access = Long.parseLong( pieces[2] );
			if ( !usr.isAuthorizedTo(access))
				return error( Snafu.ACCESS_DENIED );
			}
		catch (Exception e) { return error( Snafu.PARSE_ERROR ); }

		Monitor monitor = Monitors.find(access);
		if ( monitor == null ) return error( Snafu.ACCESS_DENIED );

		if ( !usr.office().isValidIP(aChannel.getAddress()))
			return error( Snafu.ACCESS_DENIED );
		UserStats ur = fetchUser( usr.id());
		if ( ur == null )
			monitor.addUser(usr, fRequest.time(), access, version);
		else if ( ur.isConnected())
			return error( Snafu.ALREADY_IN );
		else
			ur.logBackIn(fRequest.time(), version);
		fetchUser( usr.id()); // for channel assignment
fUser.setChannel(aChannel);
log("login usr: " + usr + " on channel: " + aChannel);
		String reply = "" + usr.id() + Message.SEP + SBDate.timeNow()
		             + Message.SEP + _roleStrings.get(new Long(access));
		return encode( Snafu.CODE_NONE, reply );
		}

	private ByteBuffer addLead()
		{
		UserStats ur = fetchUser();
		if ( ur == null )
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String ladding = "";
		try
			{
			String[] pieces = fRequest.data().split(Message.SEP);
			Raw raw = new Raw(pieces[0]);
			EmpID empID = EmpID.parse(pieces[1]);
			TouchCode tc = TouchCode.parse(pieces[2]);
			String source = (pieces.length<4)? "" : pieces[3];
			ContactID id = Raw.DAO.addLead( raw, empID, tc, source );
			ladding = id.csvRepresentation();
			}
		catch ( DataFieldException dfe )
			{
			snafuCode = Snafu.CODE_SQL_ERROR;
			ladding = dfe.getMessage();
			}
		catch ( DuplicateException e )
			{
			snafuCode = Snafu.CODE_DUPLICATE;
	//		log( "Dup: " + fRequest.data());
			ladding = e.getMessage();
			}
		catch (Throwable t)
			{
			snafuCode = Snafu.CODE_PARSE_ERROR;
			ladding = t.getClass().getSimpleName() + " " + t.getMessage();
			log( "Error: %s\n  Request: %s", t.toString(), fRequest.toString());
			}
		if (ur instanceof DMStats)
			((DMStats)ur).doAdd(snafuCode);
		return encode( snafuCode, ladding );
		}

//		        + " AND callback >= " + fCallAfter.dbRepresentation()

	private ByteBuffer getWork()
		{
		UserStats ur = fetchUser();
		if ( ur == null || !(ur instanceof CallerStats))
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String csv = "";

		if ( ur.access() == Access.TQ )
			{
			csv = fTQWorkQueue.get(ur);
			if (csv == null)
				snafuCode = Snafu.CODE_NOT_FOUND;
			return encode( snafuCode, csv );
			}

		DBStatement db = null;
		ResultSet rs = null;
		try
			{
			db = new DBStatement(true); // updatable
			String stmt = "SELECT * FROM " + Raw.DB_TABLE + " "
			            + ((CallerStats)ur).getWhereClause()
+ " AND callback <= " + SBDate.timeNow()
//			            + fRequest.data()
			            + " ORDER BY disposition DESC, callback ASC;";
/************
// log( "GET WORK: " + stmt );
System.out.println( "GET WORK: "
// +"\n      NEW: " + ((CallerModel)ur.getMonitor()).getWhere()
+"\n ur.where: " + ((CallerStats)ur).getWhereClause()
+"\n      req: " + fRequest.data()
+"\n     stmt: " + stmt );
************/
			rs = db.executeQuery( stmt );
			if ( rs.next())
				{
				long id = rs.getLong(1);
				csv = "" + id;
				for ( int i = 2; i <= Raw.NUM_FIELDS; i++ )
					{
					String fieldAsString = rs.getString(i);
					csv += ",\"" + ((fieldAsString==null)?"":fieldAsString) + "\"";
					}
				rs.updateLong("page", fRequest.uid());
				rs.updateRow();
				ur.acquiredID(id);
				}
			else snafuCode = Snafu.CODE_NOT_FOUND;
			}
		catch (Throwable t)
			{
			snafuCode = Snafu.CODE_SQL_ERROR;
			log( "Error: %s\n  Request: %s\n  Offending SQL: %s",
				t.getMessage(), fRequest.toString(), ((db == null)? "none" : db.getSQL()));
			}
		finally { if (db != null) { db.closeResultSet(rs); db.close(); }}
		return encode( snafuCode, csv );
		}

	private ByteBuffer canWork()
		{
		UserStats ur = fetchUser();
		if ( ur == null )
			return error( Snafu.ACCESS_DENIED );

		String lockedIDcsv = ur.releaseAllLocks();
		if ( !lockedIDcsv.isEmpty())
			releaseLocks(lockedIDcsv);
		return encode( Snafu.CODE_NONE, lockedIDcsv );
		}

	private ByteBuffer modLead()
		{
		UserStats ur = fetchUser();
		if ( ur == null )
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String data = "OK";
		Dispo dispo = null;
		try
			{
			String[] pieces = fRequest.data().split(Message.SEP, 5);
			TouchCode touchCode = TouchCode.parse(pieces[0]);
			boolean releaseLock = (pieces[2].charAt(0) == '1');
			short uid = Short.parseShort(pieces[3]);
			Raw raw = new Raw(pieces[4]);
			dispo = raw.dispo();
			Raw.DAO.update( raw, releaseLock, touchCode, pieces[1], fRequest.time(), uid );
			if ( releaseLock )
				ur.releasedID(raw.id());
			}
		catch ( NumberFormatException e )
			{
			data = e.getMessage();
			snafuCode = Snafu.CODE_PARSE_ERROR;
			log( "Error: %s\n  Request: %s", e.getMessage(), fRequest.toString());
			}
		catch ( Throwable t )
			{
			data = t.getMessage();
			snafuCode = Snafu.CODE_SQL_ERROR;
			log( "Error: %s\n  Request: %s", t.getMessage(), fRequest.toString());
			}
		finally
			{
			if ( ur instanceof CallerStats )
				((CallerStats)ur).doDispo(dispo);
			}
		return encode( snafuCode, data );
		}

	private ByteBuffer addHist()
		{
		UserStats ur = fetchUser();
		if ( ur == null )
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String ladding = "OK";
		try
			{
			String[] pieces = fRequest.data().split(Message.SEP, 3);
			Touch.DAO.add( new Touch(ContactID.parse(pieces[1]),
			                        new When(fRequest.time()), Source.find(fRequest.uid()),
			                        TouchCode.parse(pieces[0]),
			                        Comment.parse(pieces[2]) ));
			}
		catch ( Throwable t )
			{
			ladding = t.getMessage();
			snafuCode = Snafu.CODE_SQL_ERROR;
			}
		return encode( snafuCode, ladding );
		}

	private ByteBuffer count()
		{
		UserStats ur = fetchUser();
		if ( ur == null )
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String ladding = "";
		try
			{
			String[] pieces = fRequest.data().split(Message.SEP, 2);
			long count = fSysDAO.sqlCount( pieces[0], pieces[1] );
			ladding = "" + count;
			}
		catch ( Throwable t )
			{
			ladding = t.getMessage();
			snafuCode = Snafu.CODE_SQL_ERROR;
			}
		return encode( snafuCode, ladding );
		}

	private ByteBuffer getHist()
		{
		UserStats ur = fetchUser();
		if ( ur == null )
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String ladding = "";
		try
			{
			ContactID id = ContactID.parse(fRequest.data());
			for ( Touch touch : Touch.DAO.fetch(id))
				ladding += touch.toCSV() + Message.SEP;
			}
		catch ( Throwable t )
			{
			ladding = t.getMessage();
			snafuCode = Snafu.CODE_SQL_ERROR;
			log( "Error: %s\n  Request: %s", t.getMessage(), fRequest.toString());
			}
		return encode( snafuCode, ladding );
		}

	private ByteBuffer getLead()
		{
		UserStats ur = (UserStats)fetchUser();
		if ( ur == null )
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String ladding = "";
		DBStatement db = null;
		ResultSet rs = null;
		long id = 0;
		try
			{
			ContactID contactID = ContactID.parse(fRequest.data());
			db = new DBStatement(true); // updatable
			String stmt = "SELECT *" + " FROM " + Raw.DB_TABLE
			            + " WHERE id = " + contactID.dbRepresentation();
			rs = db.executeQuery( stmt );
			if ( rs.next())
				{
				ladding = "" + contactID;
				for ( int i = 1; i < Raw.NUM_FIELDS; i++ )
					{
					String fieldAsString = rs.getString(i+1);
					ladding += ",\"" + ((fieldAsString==null)?"":fieldAsString) + "\"";
					if (( i == Raw.PAGE )
					&&  !fieldAsString.equals("0"))
						return error( Snafu.LOCKED_LEAD );
					}
				rs.updateLong("page", fRequest.uid());
				rs.updateRow();
				ur.acquiredID(id);
				}
			else snafuCode = Snafu.CODE_NOT_FOUND;
			}
		catch (Throwable t)
			{
			ladding = t.getMessage();
			snafuCode = Snafu.CODE_SQL_ERROR;
			log( "Error: %s\n  Request: %s\n  Offending SQL: %s",
				t.getMessage(), fRequest.toString(), ((db == null)? "none" : db.getSQL()));
			}
		finally { if (db != null) { db.closeResultSet(rs); db.close(); }}
		return encode( snafuCode, ladding );
		}

	private ByteBuffer query()
		{
		UserStats ur = fetchUser();
		if ( ur == null )
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String ladding = "";
		DBStatement db = null;
		ResultSet rs = null;
		try
			{
			String[] pieces = fRequest.data().split(Message.SEP, 4);
			int maxRows = Integer.parseInt(pieces[0]);
			boolean wantHeaders = pieces[1].equals("1");
			boolean lockRecords = pieces[2].equals("1");
			db = new DBStatement(lockRecords); // updatable
			String stmt = pieces[3];
			rs = db.executeQuery( stmt );
			ladding = rsToCSV(ur, rs, wantHeaders, maxRows);
			}
		catch ( NumberFormatException e )
			{
			ladding = e.getMessage();
			snafuCode = Snafu.CODE_PARSE_ERROR;
			}
		catch (Throwable t)
			{
			ladding = t.getMessage();
			snafuCode = Snafu.CODE_SQL_ERROR;
			log( "Error: %s\n  Request: %s\n  Offending SQL: %s",
				t.getMessage(), fRequest.toString(), ((db == null)? "none" : db.getSQL()));
			}
		finally { if (db != null) { db.closeResultSet(rs); db.close(); }}
		return encode( snafuCode, ladding );
		}

	private ByteBuffer update()
		{
		UserStats ur = fetchUser();
		if ( ur == null )
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String ladding = "";
		try { ladding = "" + fSysDAO.sqlUpdate(fRequest.data()); }
		catch ( Throwable t )
			{
			ladding = t.getMessage();
			snafuCode = Snafu.CODE_SQL_ERROR;
			}
		return encode( snafuCode, ladding );
		}

	private ByteBuffer email()
		{
		UserStats ur = fetchUser();
		if ( ur == null )
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String ladding = "OK";
		DBStatement db = null;
		String[] pieces = fRequest.data().split(Message.SEP, 2);
		try
			{
			ContactID contactID = ContactID.parse(pieces[0]);
			EMailAddress address = EMailAddress.parse(pieces[1]);
			db = new DBStatement();
			RawLead.sendEmail( db, contactID, address, fRequest.time(), fRequest.uid());
			}
		catch ( Throwable t )
			{
			ladding = t.getMessage();
			snafuCode = Snafu.CODE_SQL_ERROR;
			log( "Error: %s\n  Request: %s\n  Offending SQL: %s",
				t.getMessage(), fRequest.toString(), ((db == null)? "none" : db.getSQL()));
			}
		finally
			{
			if (db != null) db.close();
			}
		return encode( snafuCode, ladding );
		}

	private ByteBuffer assign()
		{
		UserStats ur = fetchUser();
		if ( ur == null )
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String ladding = "";
		try
			{
			String[] pieces = fRequest.data().split(Message.SEP, 3);
			Source assignedTo = Source.parse(pieces[0]);
			Source assignedBy = Source.parse(pieces[1]);
			String csvRawIDs = pieces[2];
			Raw.DAO.assign( assignedTo, assignedBy, csvRawIDs );
			}
		catch ( Throwable t )
			{
			ladding = t.getMessage();
			snafuCode = Snafu.CODE_SQL_ERROR;
			log( "Error: %s\n  Request: %s", ladding, fRequest.toString());
			}
		return encode( snafuCode, ladding );
		}

	private ByteBuffer mailsent()
		{
		UserStats ur = fetchUser();
		if ( ur == null )
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String ladding = "";
		try
			{
			String[] pieces = fRequest.data().split(Message.SEP, 5);
			Comment desc = Comment.parse(pieces[0]);
			When sentWhen = When.parse(pieces[1]);
			Source sentBy = Source.parse(pieces[2]);
			boolean scheduleCall = pieces[3].charAt(0) == '1';
			String csvRawIDs = pieces[4];
			Raw.DAO.sentMail( desc, sentWhen, sentBy, scheduleCall, csvRawIDs );
			}
		catch ( Throwable t )
			{
			ladding = t.getMessage();
			snafuCode = Snafu.CODE_SQL_ERROR;
			log( "Error: %s\n  Request: %s", ladding, fRequest.toString());
			}
		return encode( snafuCode, ladding );
		}

	private ByteBuffer cntWork()
		{
		UserStats ur = fetchUser();
		if ( ur == null || !(ur instanceof CallerStats))
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String ladding = "";
		DBStatement db = null;
		ResultSet rs = null;
		try
			{
		long endOfDay = SBDate.toTime( SBDate.yyyymmdd() + "  23:59:59" );
			db = new DBStatement(true); // updatable
			String stmt = "SELECT COUNT(*) AS x "
			            + "FROM " + Raw.DB_TABLE
			            + " " + ((CallerStats)ur).getWhereClause()
+ " AND callback <= " + endOfDay;
//			            + " " + fRequest.data();
			rs = db.executeQuery( stmt );
			if ( rs.next())
				ladding = "" + rs.getLong("x");
			else ladding = "0";
			}
		catch (Throwable t)
			{
			ladding = t.getMessage();
			snafuCode = Snafu.CODE_SQL_ERROR;
			log( "Error: %s\n  Request: %s\n  Offending SQL: %s",
				t.getMessage(), fRequest.toString(), ((db == null)? "none" : db.getSQL()));
			}
		finally { if (db != null) { db.closeResultSet(rs); db.close(); }}
		return encode( snafuCode, ladding );
		}

	private static final int MAX_ROWS_PER_MESSAGE = 100;
	private String rsToCSV(UserStats ur, ResultSet rs, boolean wantHeaders, int maxRows)
		throws SQLException
		{
		StringBuilder csv = new StringBuilder();
		java.sql.ResultSetMetaData metaData = rs.getMetaData();
		int cols = metaData.getColumnCount();

		// Get the column names
		if ( wantHeaders )
			for ( int col = 0; col < cols; col++ )
				{
				csv.append(",\"");
				csv.append(metaData.getColumnLabel(col+1));
				csv.append('"');
				}

		// Get all rows.
		int row = 0;      // number of rows in the current (partial) maessage
		int totalRows = 0;
		while (rs.next())
			{
			if ((maxRows > 0) && (++totalRows > maxRows))
				break;
			for ( int i = 1; i <= cols; i++)
				{
				csv.append(",\"");
				csv.append(rs.getObject(i));
				csv.append('"');
				}
			if ( ++row >= MAX_ROWS_PER_MESSAGE )
				{
				ur.send( ClientOp.CODE_PARTIAL, (byte)0, "" + row + "," + cols + csv.toString());
				csv.delete(0,csv.length());
				row = 0;
				}
			}
		return "" + row + "," + cols + csv.toString();
		}

	private String listToCSV(UserStats ur, List<Raw> aRaws)
		{
		StringBuilder csv = new StringBuilder();
		int cols = Raw.NUM_DB_FIELDS;

		// Get all rows.
		int row = 0;      // number of rows in the current (partial) maessage
		int totalRows = 0;
		for ( Raw raw : aRaws )
			{
			csv.append(",").append(raw.toCSV());
			if ( ++row >= MAX_ROWS_PER_MESSAGE )
				{
				ur.send( ClientOp.CODE_PARTIAL, (byte)0, "" + row + "," + cols + csv.toString());
				csv.delete(0,csv.length());
				row = 0;
				}
			}
		return "" + row + "," + cols + csv.toString();
		}

	private ByteBuffer brochure()
		{
		UserStats ur = fetchUser();
		if ( ur == null )
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String ladding = "OK";
		try
			{
			String[] pieces = fRequest.data().split(Message.SEP, 4);
			Comment desc = Comment.parse(pieces[0]);
			When when = When.parse(pieces[1]);
			EmpID empID = EmpID.parse(pieces[2]);
			ContactID rawID = ContactID.parse(pieces[3]);
			Raw.DAO.mailReq( desc, when, empID, rawID );
			}
		catch ( DataFieldException e )
			{
			ladding = e.getMessage();
			snafuCode = Snafu.CODE_SQL_ERROR;
			}
		catch ( Throwable t )
			{
			ladding = t.getClass().getSimpleName() + " " + t.getMessage();
			snafuCode = Snafu.CODE_PARSE_ERROR;
			log( "Error: %s\n  Request: %s", ladding, fRequest.toString());
			}
		return encode( snafuCode, ladding );
		}

	private ByteBuffer newOrder()
		{
		UserStats ur = fetchUser();
		if ( ur == null )
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String ladding = "";
		try
			{
			Order order = new Order(fRequest.data());
			OrderID id = Order.DAO.add(order);
			ladding = "" + id;
			send( fNewOrderAudience, ClientOp.CODE_NEWORDER, new Order(id, order).toCSV());
			}
		catch ( Throwable t )
			{
			ladding = t.getMessage();
			snafuCode = Snafu.CODE_SQL_ERROR;
			log( "Error: %s\n  Request: %s", t.getMessage(), fRequest.toString());
			}
		return encode( snafuCode, ladding );
		}

	private ByteBuffer ordMod()
		{
		UserStats ur = (UserStats)fetchUser();
		if ( ur == null )
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String ladding = "";
		try
			{
			String[] pieces = fRequest.data().split(Message.SEP, 5);
			Order order = new Order(pieces[0]);
			When when = When.parse(pieces[1]);
			EmpID empID = EmpID.parse(pieces[2]);
			TouchCode touchCode = TouchCode.parse(pieces[3]);
			Comment comment = (pieces.length<5)? Comment.BLANK : Comment.parse(pieces[4]);
			Order.DAO.update(order, when, empID, touchCode, comment);
			String orderCSV = order.toCSV();
			send( fModOrderAudience, ClientOp.CODE_ORDMOD, orderCSV);
/**********
			if ( order.dispo.equals(Dispo.AOP) || order.dispo.equals(Dispo.AOF))
				{
				UserStats ao = UserStats.fetch(fRequest.uid());
				ao.send(ClientOp.CODE_ORDMOD, Message.UNSOLICITED, orderCSV)
				}
**********/
			}
		catch (Throwable t)
			{
			ladding = t.getMessage();
			snafuCode = Snafu.CODE_SQL_ERROR;
			log( "Error: %s\n  Request: %s", t.getMessage(), fRequest.toString());
			}
		return encode( snafuCode, ladding );
		}

	private ByteBuffer echo()
		{
		return encode( fRequest.err(), fRequest.data());
		}

	protected final int send(Audience aAudience, byte aOpCode, String aMsg )
		{
		int recipients = 0;
			for ( UserStats us : UserStats.getAll())
				if ( us != fUser
				&&   aAudience.member(us.user(), us.access())
				&&   us.send(aOpCode, Message.UNSOLICITED, aMsg))
					++recipients;
		return recipients;
		}

	private ByteBuffer broadcast()
		{
		UserStats ur = fetchUser();
		if ( ur == null )
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String ladding = "OK";
		int recipients = 0;
		try
			{
			String[] pieces = fRequest.data().split(Message.SEP, 3);
			byte opCode = Byte.parseByte(pieces[0]);
			Audience audience = Audience.parse(pieces[1]);
	recipients = send( audience, opCode, pieces[2] );
		/************
			for ( UserStats us : UserStats.getAll())
				{
				if ( audience.member(us.user(), us.access())
				&&   us.send(opCode, Message.UNSOLICITED, pieces[2]))
					++recipients;
				}
		************/
			ladding = "" + recipients;
			}
		catch (Throwable t)
			{
			ladding = t.getMessage();
			snafuCode = Snafu.CODE_SQL_ERROR;
			log( "Error: %s\n  Request: %s\n", t.getMessage(), fRequest.toString());
			}
		return encode( snafuCode, ladding );
		}

	private ByteBuffer syscmd()
		{
		UserStats ur = fetchUser();
		if ( ur == null )
			return error( Snafu.ACCESS_DENIED );

		byte snafuCode = Snafu.CODE_NONE;
		String ladding = "";
		try
			{
			String[] pieces = fRequest.data().split(Message.SEP);
			byte opCode = Byte.parseByte(pieces[0]);
			if ( opCode == ClientOp.SC_USR_LIST ) // fetch logged in user list
				for ( UserStats us : UserStats.getAll())
					ladding += (ladding.isEmpty()? "" : Message.SEP) + us.login() + "," + us.name();
			else if ( opCode == ClientOp.SC_ROLE_GET )
				ladding = _roleStrings.get(Long.decode(pieces[1]));
			else if ( opCode == ClientOp.SC_ROLE_SET )
				{
//System.out.println("set access " + pieces[1] + "\n  " + pieces[2]);
				_roleStrings.put(Long.decode(pieces[1]), pieces[2]);
				RoleFileDAO.freeze(_roleStrings);
				}
			else if ( opCode == ClientOp.SC_STATS_GET )
				{
				long lastUpdate = Long.parseLong(pieces[1]);
				long access = Long.parseLong(pieces[2]);
				for ( UserStats us : UserStats.getAll())
					if ((us.access() == access) && lastUpdate < us.lastTime())
						ladding += us.csv() + Message.SEP;
				}
			else if ( opCode == ClientOp.SC_STATS_RESET )
				{
				long access = Long.parseLong(pieces[1]);
				for ( UserStats us : UserStats.getAll())
					if (us.access() == access)
						us.clearStats();
				}
			else if ( opCode == ClientOp.SC_CRITERIA_GET )
				{
				long access = Long.parseLong(pieces[1]);
				Monitor monitor = Monitors.find(access);
				if ( monitor == null
				|| !(monitor instanceof CallerModel))
					return error( Snafu.ACCESS_DENIED );
				ladding = "" + access + Message.SEP
				        + ((CallerModel)monitor).getCountriesCSV() + Message.SEP
				        + ((CallerModel)monitor).getMaxHTR();
				}
			else if ( opCode == ClientOp.SC_CRITERIA_SET )
				{
				long access = Long.parseLong(pieces[1]);
				String countryCSV = pieces[2];
				int maxHTR = Integer.parseInt(pieces[3]);
				Monitor monitor = Monitors.find(access);
				if ( monitor == null
				|| !(monitor instanceof CallerModel))
					return error( Snafu.ACCESS_DENIED );
				((CallerModel)monitor).setCriteria( maxHTR, countryCSV );
				}
			else if ( opCode == ClientOp.SC_CHECKOUT )
				{
				int maxRecords = Integer.parseInt(pieces[1]);
				int perPage = Integer.parseInt(pieces[2]);
				short uid = Short.parseShort(pieces[3]);
				String whereClause = pieces[4];
				List<Raw> raws = new Vector<Raw>(maxRecords);
				Raw.DAO.checkOut(raws, maxRecords, perPage, whereClause, uid);
				ladding = listToCSV(ur, raws);
				}
			else if ( opCode == ClientOp.SC_NEXT_CHECKOUT_PAGE )
				{
				ladding = "" + Raw.DAO.nextCheckOutPage();
				}
			else if ( opCode == ClientOp.SC_PURGE_WORKQ )
				{
				long access = Long.decode(pieces[1]);
				fTQWorkQueue.purge();
				}
			}
		catch (Throwable t)
			{
			ladding = t.getMessage();
			snafuCode = Snafu.CODE_SQL_ERROR;
			log( "Error: %s\n  Request: %s\n", t.getMessage(), fRequest.toString());
			}
		return encode( snafuCode, ladding );
		}
	} // 402
