package edu.calpoly.csc.pulseman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Queue;

public class ConnectionHandler
{
	public static final int SOCKET_TIMEOUT = 10000;

	private static Socket socket = null;
	private static PrintWriter out = null;
	private static BufferedReader in = null;
	
	private static MessageSender messageSender = new MessageSender();

	private static Queue<String> messageQueue = new LinkedList<String>();

	public static synchronized boolean connect(String ip, int port)
	{
		if(socket != null || socket.isConnected())
		{
			throw new IllegalStateException("Already connected to host");
		}

		socket = new Socket();
		try
		{
			socket.connect(new InetSocketAddress(ip, port), SOCKET_TIMEOUT);
			socket.setSoTimeout(SOCKET_TIMEOUT);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			return true;
		}
		catch(SocketException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	public static synchronized void disconnect()
	{
		if(socket == null || !socket.isConnected())
		{
			throw new IllegalStateException("Not connected to host");
		}

		out.close();
		try
		{
			in.close();
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try
		{
			socket.close();
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void sendMessage(String message)
	{
		synchronized(messageQueue)
		{
			messageQueue.offer(message);
		}
	}

	private static class MessageSender implements Runnable
	{
		private boolean alive;

		public MessageSender()
		{
			alive = true;
		}

		@Override
		public void run()
		{
			while(alive)
			{
				synchronized(this)
				{
					while(messageQueue.isEmpty() && alive)
					{
						try
						{
							wait();
						}
						catch(InterruptedException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					if(!alive)
					{
						return;
					}

					if(socket != null && out != null && socket.isConnected())
					{
						out.println(messageQueue.poll());
					}
				}
			}
		}
	}
}
