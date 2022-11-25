package com.apo.apps.AppServer.rox;

import com.apo.net.Message;

public final class ClientRequest
	{
	public final ClientChannel channel;
	public final Message message;

	public ClientRequest(ClientChannel aChannel, Message aMessage)
		{
		this.channel = aChannel;
		this.message = aMessage;
		}
	}