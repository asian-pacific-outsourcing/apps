package com.apo.apps.AppServer.rox;

import com.apo.apps.AppServer.Operation;
import com.apo.net.Message;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class APOWorker
	implements Runnable
	{
	private final List<ClientRequest> queue = new LinkedList<ClientRequest>();

	public void processData(ClientChannel ci, Message msg)
		{
		synchronized(queue)
			{
			queue.add(new ClientRequest(ci, msg));
			queue.notify();
			}
		}

	public void run()
		{
		ClientRequest request;

		while(true)
			{
			synchronized(queue) // Wait for requests
				{
				while(queue.isEmpty())
					{
					try { queue.wait(); }
					catch (InterruptedException e) {}
					}
				request = queue.remove(0);
				}

			ByteBuffer reply = new Operation(request).getReply();
			if ( reply == null ) { System.out.println("No reply"); continue; }
			reply.flip();

			request.channel.send(reply);
			}
		}
	}
