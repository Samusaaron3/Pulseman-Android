package edu.calpoly.csc.pulseman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class ConnectionHandler
{
	public static final int SOCKET_TIMEOUT = 5000;

	private static Socket socket = null;
	private static PrintWriter out = null;
	private static BufferedReader in = null;

	private static MessageSender messageSender;
	private static Receiver receiver;

	private static Queue<String> messageQueue = new LinkedList<String>();

	private static ArrayList<ConnectionStatusListener> listeners = new ArrayList<ConnectionStatusListener>();
	private static ArrayList<MessageReceiver> messageReceivers = new ArrayList<MessageReceiver>();

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
			//socket.setSoTimeout(SOCKET_TIMEOUT);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			messageSender = new MessageSender();
			new Thread(messageSender, "Message Sender").start();
			
			receiver = new Receiver();
			new Thread(receiver, "Message Receiver").start();

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
		
		if(receiver != null)
		{
			receiver.kill();
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

		synchronized(listeners)
		{
			for(int i = 0; i < listeners.size(); ++i)
			{
				listeners.get(i).onConnectionLost();
			}
		}
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
			try
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
						disconnect();
						return;
					}

					synchronized(messageQueue)
					{
						if(isConnected())
						{
							// long time = SystemClock.elapsedRealtime();
							out.println(messageQueue.poll() + ": " + counter++);
							// in.readLine();
						}
					}
				}
			}
			catch(Exception e)
			{
				//
			}
			disconnect();
		}
	}
	
	private static class Receiver implements Runnable
	{
		private boolean alive;

		public Receiver()
		{
			alive = true;
		}

		public void kill()
		{
			alive = false;
		}

		@Override
		public void run()
		{
			String inputLine;

			try
			{
				while(alive && (inputLine = in.readLine()) != null)
				{
					synchronized(messageReceivers)
					{
						for(int i = 0; i < messageReceivers.size(); ++i)
						{
							messageReceivers.get(i).onMessageReceived(inputLine);
						}
					}
				}
			}
			catch(IOException e)
			{
				e.printStackTrace(); // TODO
			}
		}
	}

	public static void addConnectionStatusListener(ConnectionStatusListener listener)
	{
		synchronized(listeners)
		{
			listeners.add(listener);
		}
	}

	public static void removeConnectionStatusListener(ConnectionStatusListener listener)
	{
		synchronized(listeners)
		{
			listeners.remove(listener);
		}
	}

	interface ConnectionStatusListener
	{
		public void onConnectionLost();
	}
	
	/**
	 * Adds a MessageReceiver to the list of receivers
	 * 
	 * @param receiver The MessageReceiver to add
	 */
	public static void addmessageReceiver(MessageReceiver receiver)
	{
		synchronized(messageReceivers)
		{
			messageReceivers.add(receiver);
		}
	}

	/**
	 * Removes a MessageReceiver from the list of receivers
	 * 
	 * @param receiver The MessageReceiver to remove
	 */
	public static void removemessageReceiver(MessageReceiver receiver)
	{
		synchronized(messageReceivers)
		{
			messageReceivers.remove(receiver);
		}
	}

	/**
	 * Interface used to notify listeners when messages are received from an Android client
	 * 
	 * @author Aaron Jacobs
	 */
	public interface MessageReceiver
	{
		/**
		 * Called when a message is received from an Android client
		 * 
		 * @param message The message received by the client
		 */
		public void onMessageReceived(String message);

		/**
		 * Called when the connection to the client is established
		 */
		public void onConnectionEstablished(InetAddress client);

		/**
		 * Called when the connection to the client is lost
		 */
		public void onConnectionLost(InetAddress client);
	}
}
