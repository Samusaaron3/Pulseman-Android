package edu.calpoly.csc.pulseman;

import java.util.Timer;
import java.util.TimerTask;

import edu.calpoly.csc.pulseman.ConnectionHandler.ConnectionStatusListener;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.app.Activity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity
{
	public static final int SOCKET_TIMEOUT = 10000, PORT = 42000;
	public static final String IP_ADDRESS = "ipAddress";

	private SurfaceView barSurfaceView;
	private RelativeLayout layout;
	private RelativeLayout statusLayout;
	private ImageView heartImageView;
	private TextView statusText;
	private String ipAddress;
	
	private Timer renderTimer;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		barSurfaceView = (SurfaceView)findViewById(R.id.barSurfaceView);

		Bundle extras = getIntent().getExtras();
		ipAddress = extras.getString(IP_ADDRESS);

		layout = (RelativeLayout)findViewById(R.id.main_layout);
		layout.setBackgroundColor(Color.rgb(252, 207, 207));

		statusLayout = (RelativeLayout)findViewById(R.id.statusLayout);
		statusLayout.setBackgroundColor(Color.YELLOW);
		statusText = (TextView)findViewById(R.id.statusTextView);

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

		heartImageView = (ImageView)findViewById(R.id.heartImageView);
		heartImageView.setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if(event.getAction() == MotionEvent.ACTION_DOWN)
				{
					heartImageView.setImageResource(R.drawable.heartp);

					new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							if(ConnectionHandler.isConnected())
							{
								ConnectionHandler.sendMessage("touch");
							}
						}
					}).start();
				}
				else if(event.getAction() == MotionEvent.ACTION_UP)
				{
					heartImageView.setImageResource(R.drawable.heart);
				}

				return true;
			}
		});
	}

	@Override
	public void onResume()
	{
		super.onResume();
		
		renderTimer = new Timer("Renderer");
		renderTimer.schedule(new Renderer(barSurfaceView.getHolder()), 0, 15);

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				boolean success = ConnectionHandler.connect(ipAddress, PORT);

				if(success)
				{
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							statusLayout.setBackgroundColor(Color.GREEN);
							statusText.setText("Connected!");
						}
					});
				}
				else
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
			}
		}, "Connect thread").start();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		
		renderTimer.cancel();

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

	private static class Renderer extends TimerTask
	{
		private SurfaceHolder holder;
		private Paint paint;
		float value = 0.0f;
		private double f = 0.0;

		public Renderer(SurfaceHolder holder)
		{
			this.holder = holder;
			this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		}

		@Override
		public void run()
		{
			if(holder.getSurface().isValid())
			{
				Canvas canvas = holder.lockCanvas();
				canvas.drawColor(Color.BLACK);
				
				value = ((float)Math.sin(f) + 1.0f) / 2.0f;

				paint.setStyle(Paint.Style.FILL);
				paint.setColor(Color.rgb(255 - (int)(value * 255), (int)(value * 255), 0));

				canvas.drawRect(0.0f, 25.0f, value * canvas.getWidth(), canvas.getHeight() - 25.0f, paint);
				
				f += 0.05;

				holder.unlockCanvasAndPost(canvas);
			}
		}
	}
}
