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

import android.os.SystemClock;
import android.util.Log;

public class ConnectionHandler
{
	public static final int SOCKET_TIMEOUT = 5000;

	private static Socket socket = null;
	private static PrintWriter out = null;
	private static BufferedReader in = null;

	private static MessageSender messageSender;

	private static Queue<String> messageQueue = new LinkedList<String>();

	public static synchronized boolean connect(String ip, int port)
	{
		if(socket != null && socket.isConnected())
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

			messageSender = new MessageSender();
			new Thread(messageSender, "Message Sender").start();

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
		if(messageSender != null)
		{
			messageSender.kill();
		}

		if(out != null)
		{
			out.close();
		}

		if(in != null)
		{
			try
			{
				in.close();
			}
			catch(IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if(socket != null)
		{
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

		out = null;
		in = null;
		socket = null;
	}

	public static synchronized boolean isConnected()
	{
		return socket != null && out != null && in != null && socket.isConnected();
	}

	public static void sendMessage(String message)
	{
		synchronized(messageQueue)
		{
			messageQueue.offer(message);

			messageQueue.notifyAll();
		}
	}

	private static class MessageSender implements Runnable
	{
		private volatile boolean alive;

		public MessageSender()
		{
			alive = true;
		}

		public void kill()
		{
			synchronized(messageQueue)
			{
				alive = false;
				messageQueue.notifyAll();
			}
		}

		int counter = 0; // TODO

		@Override
		public void run()
		{
			while(alive)
			{
				synchronized(messageQueue)
				{
					while(messageQueue.isEmpty())
					{
						try
						{
							messageQueue.wait();
						}
						catch(InterruptedException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						if(!alive)
						{
							break;
						}
					}
				}

				if(!alive)
				{
					return;
				}

				synchronized(messageQueue)
				{
					if(isConnected())
					{
						long time = SystemClock.elapsedRealtime();
						out.println(messageQueue.poll() + ": " + counter++);
						try
						{
							Log.e("debug", in.readLine());
						}
						catch(IOException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						Log.e("debug", "Time: " + (SystemClock.elapsedRealtime() - time));
					}
				}
			}
		}
	}
}
