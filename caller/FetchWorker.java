package com.apo.apps.caller;
/********************************************************************
* @(#)FetchWorker.java 1.00 20110602
* Copyright 2011 by Richard T. Salamone, Jr. All rights reserved.
*
* FetchWorker: Maintains a queue of scheduled work, that is
* populated in a background task, and retrieved as needed by
* the foreground task.
*
* @author Rick Salamone
* @version 1.00, 20110602
* 20110602 rts decoupled the actions from this gui
*******************************************************/
import com.shanebow.util.SBLog;
import java.util.*;
import java.io.*;
import javax.swing.SwingUtilities;

public final class FetchWorker
	{
	/** Throughout the code, this is the object we synchronize on so this
	* is also the object we wait() and notifyAll() on.
	*/
	protected LinkedList<Work> fList = new LinkedList<Work>();
	protected int MAX = 3;
	protected boolean done = false; // Also protected by lock on fList
	private volatile boolean fAvailable = false;

	FetchWorker()
		{
		log( "Launching prefetch" );
		new Producer().start();
		}

	private void log(String fmt, Object... args)
		{
		final String msg = String.format(fmt, args);
		if (SwingUtilities.isEventDispatchThread())
			SBLog.write(msg);
		else SwingUtilities.invokeLater(new Runnable()
			{
			public void run() { SBLog.write(msg); }
			});
		}

	/** Inner class representing the Producer side */
	class Producer extends Thread
		{
		public void run()
			{
			while (true)
				{
				Work work = getRequestFromNetwork();
				// Get request from the network - outside the synch section.
				// We're actually reading from the server: it might be a while
				synchronized(fList)
					{
					while (fList.size() == MAX) // queue "full"
						try
							{
							log("Producer WAITING");
							fList.wait(); // Limit the size
							}
						catch (InterruptedException ex)
							{
							log("Producer INTERRUPTED");
							}
					fList.addFirst(work);
					fAvailable = true;
					fList.notifyAll(); // must own the lock
					log("Fetched %s; List size now %d", work.toString(), fList.size());
					if (done)
						break;
					// yield(); // Useful for green threads & demo programs.
					}
				}
			}

		private Work getRequestFromNetwork()
			{
			Work work = new Work();
			work.fetch();
			return work;
			}
		} // end of producer

	public boolean hasWork() { return fAvailable; }

	public Work dequeue()
		{
		Work obj = null;
		int len = 0;
		synchronized(fList)
			{
			while (fList.size() == 0)
				{
				try { fList.wait(); }
				catch (InterruptedException ex) { return null; }
				}
			obj = fList.removeLast();
			len = fList.size();
			fAvailable = len > 0;
			fList.notifyAll();
			}
		log("dequeue work " + obj + "\nList size now " + len);
		return obj;
		}
	}
