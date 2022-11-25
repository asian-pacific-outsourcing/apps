package com.apo.apps.AppServer.rox;
/********************************************************************
* @(#)ClientChannel.java 1.00 20110124
* Copyright (c) 2010 by Richard T. Salamone, Jr. All rights reserved.
*
* ClientChannel: Details for maintaining a persistent connection to a
* single client and methods for receiving Messages from that client.
*
* Since NIO client input is non-blocking, the arrival of data does
*	does not mean that a complete message has been received. 
*
* The ongoing message is maintained in fBuffer until it is complete,
* then the complete message  is returned.
*
* @author Andrew Davison, April 2005, ad@fivedots.coe.psu.ac.th
* @author Rick Salamone
* @version 1.00, 20110124 rts created from Davison's ideas
* @version 1.01, 20110130 rts modified for Message version 3
*******************************************************/
import com.apo.net.Message;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;

public class ClientChannel
	{
	private static final int BUFSIZ = 1024;	// size of the input buffer message

	private Rox fServer;           // the server
	private SocketChannel fChannel; // this client's io fChannel 
	private ByteBuffer fBuffer;    // input buffer for receiving requests

	public ClientChannel(SocketChannel aChannel, Rox aServer)
		{
		fChannel = aChannel;
		fServer = aServer;
		fBuffer = ByteBuffer.allocateDirect(BUFSIZ);
		fBuffer.clear();

		System.out.println(toString());
		}

	@Override public String toString()
		{
		InetAddress iaddr = getAddress();
		if ( iaddr == null ) return "Client not connected";
		return "Client @ " + iaddr.getHostAddress() + ":"
		                   + fChannel.socket().getPort();
		}

	public final InetAddress getAddress()
		{
		if ( fChannel == null )
			return null;
		Socket sock = fChannel.socket();
		if ( sock == null )
			return null;
		return sock.getInetAddress();
		}

	public void closeDown()
		{
		try { fChannel.close(); }
		catch(IOException e) { System.out.println(e); }
		}

	/** read() is called when this client's fChannel is readable.
	* Just buffer the data, server will call getMessage() when
	* it's time to actually parse the data
	*/
	public int read()
		throws IOException
		{
		return fChannel.read(fBuffer);
		}

	/** getMessage() is called by Rox after data has been read
	* into this fChannel. If there's a complete message it is
	* parsed and returned to Rox. Then the input is drained of
	* this message.
	* @return a request message or null if the request is incomplete
	*/
	public Message getMessage()
		{
		if ( !Message.isComplete(fBuffer))
			return null;
		int position = fBuffer.position();
		fBuffer.position(0);
		Message msg = new Message(fBuffer);
// System.out.format("Buffer had %d bytes, message consumed %d\n", position, fBuffer.position());
System.out.println("Message: " + msg);

		fBuffer.clear();
		return msg;
		}

	public void send( ByteBuffer data )
		{
		fServer.send(fChannel, data);
		}

	public void send(byte code, byte aFlags, short uid, String data )
		{
		ByteBuffer bb = Message.encode( code, (byte)0, aFlags, uid, (short)0, data);
		bb.flip();
		fServer.send(fChannel, bb);
		}
	}
