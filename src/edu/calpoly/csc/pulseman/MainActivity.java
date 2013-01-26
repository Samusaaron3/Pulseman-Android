package edu.calpoly.csc.pulseman;

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
				if(ConnectionHandler.isConnected())
				{
					ConnectionHandler.sendMessage("touch");
				}

				return false;
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
				ConnectionHandler.connect("129.65.102.176", PORT);
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
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
