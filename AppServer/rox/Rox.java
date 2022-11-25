package com.apo.apps.AppServer.rox;

import com.shanebow.util.SBLog;
import com.apo.net.Access;
import com.apo.net.ClientOp;
import com.apo.net.Message;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

public class Rox
	implements Runnable
	{
	// The channel on which we'll accept connections
	private ServerSocketChannel serverChannel;

	// The selector we'll be monitoring
	private final Selector fSelector;

	// map of persistent channels to their ClientChannel objects
	private final Map<SocketChannel, ClientChannel> fClients
	         = new HashMap<SocketChannel, ClientChannel>();

	private final APOWorker worker;

	// A list of PendingChange instances
	private final List<ChangeRequest> fPendingChanges
	         = new LinkedList<ChangeRequest>();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private final Map<SocketChannel, List<ByteBuffer>> fPendingData
	         = new HashMap<SocketChannel, List<ByteBuffer>>();

	// Mesage sent to worker task upon broken connection
	private final Message PSUEDO_LOGOUT_MSG
		= new Message(Message.encode(ClientOp.CODE_LOGOUT, (byte)0,
		              Message.UNSOLICITED, (short)0, (short)0, ""));


	public Rox(int backlog)
		throws IOException
		{
		worker = new APOWorker();
		new Thread(worker).start();
		fSelector = this.initSelector(backlog);
		}

	@Override public String toString()
		{
		return getClass().getSimpleName()
		      + ( Access._x509? " with" : " no" ) + " x509 "
		      + "at " + serverChannel.socket().getLocalSocketAddress();
		}

	private Selector initSelector(int backlog) throws IOException
		{
		// Create a new selector
		Selector socketSelector = SelectorProvider.provider().openSelector();

		// Create a new non-blocking server socket channel
		this.serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);

		// Bind the server socket to the specified address and port
log("Attempt bind to: " + Access._serverIP + ":" + Access._port);
		InetSocketAddress isa = new InetSocketAddress(
			InetAddress.getByName(Access._serverIP), Access._port);
		serverChannel.socket().bind(isa, backlog);
log("Bound to: " + Access._serverIP + ":" + Access._port);

		// Register the server socket channel, indicating an interest in 
		// accepting new connections
		serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

		return socketSelector;
		}

	public void send(SocketChannel socket, ByteBuffer data)
		{
		synchronized (fPendingChanges)
			{
			// Indicate we want the interest ops set changed
			fPendingChanges.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS,
			                                          SelectionKey.OP_WRITE));

			// And queue the data we want written
			synchronized (fPendingData)
				{
				List<ByteBuffer> queue = (List<ByteBuffer>) fPendingData.get(socket);
				if (queue == null)
					{
					queue = new ArrayList<ByteBuffer>();
					fPendingData.put(socket, queue);
					}
				queue.add(data);
				}
			}

		// Finally, wake up our selecting thread so it can make the required changes
		fSelector.wakeup();
		}

	public void run()
		{
		while (true)
			{
//	log("No. of clients: " + fClients.size());
			try
				{
				synchronized (fPendingChanges) // Process any pending changes
					{
					Iterator changes = fPendingChanges.iterator();
					while (changes.hasNext())
						{
						ChangeRequest change = (ChangeRequest)changes.next();
						switch (change.type)
							{
							case ChangeRequest.CHANGEOPS:
								SelectionKey key = change.socket.keyFor(fSelector);
								if ( key != null )
									key.interestOps(change.ops); // null pointer exception here??
								break;
							case ChangeRequest.REGISTER:
								change.socket.register(fSelector, change.ops);
								break;
							}
						}
					fPendingChanges.clear();
					}

				// Wait for an event one of the registered channels
				fSelector.select();

				// Iterate over the set of keys for which events are available
				Iterator selectedKeys = fSelector.selectedKeys().iterator();
				while (selectedKeys.hasNext())
					{
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();

					if (!key.isValid())
						continue;

					// Check what event is available and deal with it
					if (key.isAcceptable())
						this.accept(key);
					else if (key.isReadable())
						this.read(key);
					else if (key.isWritable())
						this.write(key);
					else if (key.isConnectable())
						this.finishConnection(key);
					}
				}
			catch (Exception e) { e.printStackTrace(); }
			}
		}

	private void accept(SelectionKey key)
		throws IOException
		{
		// For an accept to be pending the channel must be a server socket channel.
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

		// Accept the connection and make it non-blocking
		SocketChannel channel = serverSocketChannel.accept();
//		Socket socket = channel.socket();
		channel.configureBlocking(false);

		// Register the new SocketChannel with our Selector, indicating
		// we'd like to be notified when there's data waiting to be read
		channel.register(fSelector, SelectionKey.OP_READ);

		// add this channel as a new user
		fClients.put(channel, new ClientChannel(channel, this) );
		}

	private void finishConnection(SelectionKey key)
//		throws IOException
		{
	log("Finish connection to Houston");
		SocketChannel channel = (SocketChannel) key.channel();

		// Finish the connection. If the connection operation failed
		// this will raise an IOException.
		try
			{
			channel.finishConnect();
			channel.configureBlocking(false);

			// Register the new SocketChannel with our Selector, indicating
			// we'd like to be notified when there's data waiting to be read
			channel.register(fSelector, SelectionKey.OP_READ);

			// add this channel as a new user
			fClients.put(channel, new ClientChannel(channel, this) );
		log("Connected to Houston");
			}
		catch (IOException e)
			{
			// Cancel the channel's registration with our selector
			log("Houston connect error: " + e);
			key.cancel();
			}
		}

	private void read(SelectionKey key) throws IOException
		{
		SocketChannel channel = (SocketChannel) key.channel();
		ClientChannel client = (ClientChannel) fClients.get(channel);

		// Attempt to read off the channel
		int numRead;
		try
			{
			numRead = client.read();
			}
		catch (IOException e) // The remote forcibly closed the connection, cancel
			{                   // the selection key and close the channel.
	log("forced close: " );
			key.cancel();
			channel.close();
			fClients.remove(channel);  // delete ci from hash map
			this.worker.processData(client, PSUEDO_LOGOUT_MSG);
			return;
			}

		if (numRead == -1) // Remote entity shut the socket down cleanly. Do the
			{                // same from our end and cancel the channel.
			key.channel().close();
			key.cancel();
	log("clean shutdown: " );
			fClients.remove(channel);  // delete ci from hash map
			this.worker.processData(client, PSUEDO_LOGOUT_MSG);
			return;
			}

		Message in = client.getMessage();
		if ( in == null )
			return; // don't have a complete message yet

		// Hand the data off to our worker thread
		this.worker.processData(client, in);
		}

	private void write(SelectionKey key) throws IOException
		{
		SocketChannel socketChannel = (SocketChannel) key.channel();

		synchronized (fPendingData)
			{
			List queue = (List) fPendingData.get(socketChannel);

			// Write until there's not more data ...
			while (!queue.isEmpty())
				{
try {
				ByteBuffer buf = (ByteBuffer) queue.get(0);
				socketChannel.write(buf);
				if (buf.remaining() > 0) // ... or the socket's buffer fills up
					break;
}
catch (Throwable e) { log("BINGO: " + e); }
				queue.remove(0);
				}

			if (queue.isEmpty()) // We wrote away all data, so no longer interested in
				{                 // writing on this socket, Switch back to waiting
				key.interestOps(SelectionKey.OP_READ);
				}
			}
		}

	public SocketChannel initiateConnection(String aIP, int aPort)
		throws IOException
		{
		// Create a non-blocking socket channel
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);

		// Kick off connection establishment
log("start Initiate connection to Houston"); // + aIP + ":" + aPort);
		InetAddress hostAddress = InetAddress.getByName(aIP);
		socketChannel.connect(new InetSocketAddress(hostAddress, aPort));
log("socketChannel.connect Houston"); // + socketChannel);

		// Queue a channel registration since the caller is not the 
		// selecting thread. As part of the registration we'll register
		// an interest in connection events. These are raised when a channel
		// is ready to complete connection establishment.
		synchronized(fPendingChanges)
			{
			fPendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
			}
log("back from register socketChannel"); // + socketChannel);
		return socketChannel;
		}

	private void log(String fmt, Object... args)
		{
		String module = Access.isFlipper()? "Flipper" : "Houston";
		SBLog.write( module, String.format(fmt, args));
		}

	public static void main(String[] args)
		{
		Access.parseCmdLine(args);
		try
			{
			int backlog = 1024;
			new Thread(new Rox(backlog)).start();
			}
		catch (IOException e) { e.printStackTrace(); }
		}
	} //215
