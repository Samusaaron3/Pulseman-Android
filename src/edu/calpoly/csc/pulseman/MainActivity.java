package edu.calpoly.csc.pulseman;

import java.sql.Connection;

import edu.calpoly.csc.pulseman.ConnectionHandler.ConnectionStatusListener;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity
{
	public static final int SOCKET_TIMEOUT = 10000, PORT = 42000;
	public static final String IP_ADDRESS = "ipAddress";

	private RelativeLayout layout;
	private String ipAddress;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Bundle extras = getIntent().getExtras();
		ipAddress = extras.getString(IP_ADDRESS);

		final int size = 20;
		final MediaPlayer[] players = new MediaPlayer[20];
		for(int i = 0; i < size; ++i)
		{
			players[i] = MediaPlayer.create(MainActivity.this, R.raw.single_pulse);
		}

		layout = (RelativeLayout)findViewById(R.id.main_layout);
		layout.setOnTouchListener(new OnTouchListener()
		{
			int playerID = 0;

			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				players[playerID++].start(); // no need to call prepare(); create() does that for you
				playerID %= players.length;

				if(ConnectionHandler.isConnected())
				{
					ConnectionHandler.sendMessage("touch");
				}

				return false;
			}
		});

		final RelativeLayout statusLayout = (RelativeLayout)findViewById(R.id.statusLayout);
		statusLayout.setBackgroundColor(Color.GREEN);
		final TextView statusText = (TextView)findViewById(R.id.statusTextView);

		ConnectionHandler.addConnectionStatusListener(new ConnectionStatusListener()
		{
			@Override
			public void onConnectionLost()
			{
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						statusLayout.setBackgroundColor(Color.RED);
						statusText.setText("Disconnected :(");
					}
				});
			}
		});
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
				ConnectionHandler.connect(ipAddress, PORT);
			}
		}, "Connect thread").start();
	}

	@Override
	public void onPause()
	{
		super.onPause();

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				ConnectionHandler.disconnect();
			}
		}, "Disconnect thread").start();

		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
