package edu.calpoly.csc.pulseman;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class UDPConnectionHandler
{
	public static final int MULTICAST_PORT = 42000, COMMUNICATION_PORT = 42042;
	public static final int SOCKET_TIMEOUT = 5000, MAX_MESSAGE_SIZE = 2048;
	public static final String ALL_HOSTS_GROUP = "224.0.0.1";

	private static DatagramSocket socket = null;

	public static String getStatus()
	{
		if(socket == null)
		{
			return "Connection Not Established";
		}
		else
		{
			return "Pointed at " + socket.getRemoteSocketAddress();
		}
	}

	public static boolean findHost()
	{
		final String outMessage = "pulse";
		byte[] message = encodeMessage(outMessage);

		InetAddress multicastAddress;
		try
		{
			multicastAddress = InetAddress.getByName(ALL_HOSTS_GROUP);

			try
			{
				MulticastSocket s = new MulticastSocket(MainActivity.PORT);
				s.joinGroup(multicastAddress);

				DatagramPacket outPacket = new DatagramPacket(message, message.length, multicastAddress, MainActivity.PORT);
				s.send(outPacket);

				byte[] buffer = new byte[MAX_MESSAGE_SIZE];
				DatagramPacket inPacket;
				do
				{
					inPacket = new DatagramPacket(buffer, buffer.length);
					s.receive(inPacket);
				} while(outMessage.equals(decodeMessage(inPacket.getData())));

				int port = COMMUNICATION_PORT;
				String receivedMessage = decodeMessage(inPacket.getData());
				if(receivedMessage != null)
				{
					try
					{
						port = Integer.parseInt(receivedMessage);
					}
					catch(Exception e)
					{
						//
					}
				}

				if(socket == null)
				{
					socket = new DatagramSocket();
				}
				socket.disconnect();
				socket.connect(new InetSocketAddress(inPacket.getAddress(), port));

				return true;
			}
			catch(IOException e)
			{
				//
			}
		}
		catch(UnknownHostException e)
		{
			//
		}

		return false;
	}

	public static boolean isConnected()
	{
		return socket != null && socket.getRemoteSocketAddress() != null && socket.isConnected();
	}

	public static boolean sendMessage(String message)
	{
		if(isConnected())
		{
			byte[] encodedMessage = encodeMessage(message);

			try
			{
				socket.send(new DatagramPacket(encodedMessage, encodedMessage.length, socket.getRemoteSocketAddress()));

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

			socket.disconnect();
			socket.close();
			socket = null;
		}

		return false;
	}

	private static String decodeMessage(byte[] data)
	{
		ByteBuffer buf = ByteBuffer.wrap(data);
		byte[] payload = new byte[buf.getInt()];
		buf.get(payload);

		try
		{
			return new String(payload, "UTF8");
		}
		catch(UnsupportedEncodingException e)
		{
			//
		}

		return null;
	}

	private static byte[] encodeMessage(String message)
	{
		try
		{
			byte[] messageArray = message.getBytes("UTF8");
			byte[] lengthArray = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE).putInt(messageArray.length).array();
			byte[] buffer = new byte[messageArray.length + lengthArray.length];

			System.arraycopy(lengthArray, 0, buffer, 0, lengthArray.length);
			System.arraycopy(messageArray, 0, buffer, lengthArray.length, messageArray.length);

			return buffer;
		}
		catch(UnsupportedEncodingException e)
		{
			//
		}

		return null;
	}
}
