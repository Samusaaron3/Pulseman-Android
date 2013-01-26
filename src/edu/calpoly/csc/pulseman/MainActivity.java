package edu.calpoly.csc.pulseman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.RelativeLayout;

public class MainActivity extends Activity
{
	public static final int SOCKET_TIMEOUT = 10000, PORT = 42000;

	private RelativeLayout layout;
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		layout = (RelativeLayout)findViewById(R.id.main_layout);
		layout.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				Log.e("debug", "touch");
				if(out != null)
				{
					out.println("touch");
					Log.e("debug", "message sent");
				}

				return false;
			}
		});

		socket = null;
	}

	@Override
	public void onResume()
	{
		super.onResume();

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				if(socket == null)
				{
					try
					{
						socket = new Socket();
						socket.connect(new InetSocketAddress("129.65.102.176", PORT), SOCKET_TIMEOUT);
						socket.setSoTimeout(SOCKET_TIMEOUT);

						out = new PrintWriter(socket.getOutputStream(), true);
						in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					}
					catch(UnknownHostException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch(IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	@Override
	public void onPause()
	{
		super.onPause();

		// TODO
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

		socket = null;
		out = null;
		in = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
