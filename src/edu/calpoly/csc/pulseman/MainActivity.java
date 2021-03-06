package edu.calpoly.csc.pulseman;

import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

import edu.calpoly.csc.pulseman.ConnectionHandler.ConnectionStatusListener;
import edu.calpoly.csc.pulseman.ConnectionHandler.MessageReceiver;

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
			private boolean isDiastole = true;
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if(event.getAction() == MotionEvent.ACTION_DOWN)
				{
					if(isDiastole)
					{
						heartImageView.setImageResource(R.drawable.heart_diastole_depressed);
					}
					else
					{
						heartImageView.setImageResource(R.drawable.heart_systole_depressed);
					}

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
					isDiastole = !isDiastole;
					if(isDiastole)
					{
						heartImageView.setImageResource(R.drawable.heart_diastole);
					}
					else
					{
						heartImageView.setImageResource(R.drawable.heart_systole);
					}
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

	private static final int MAX_COLOR = 255;
	private static class Renderer extends TimerTask implements MessageReceiver
	{
		private SurfaceHolder holder;
		private Paint paint;
		//float value = 0.0f;
		//private double f = 0.0;
		private double percent = 0.0;
		private int red, green;

		public Renderer(SurfaceHolder holder)
		{
			this.holder = holder;
			paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setStyle(Paint.Style.FILL);
			
			ConnectionHandler.addmessageReceiver(this);
		}

		@Override
		public void run()
		{
			if(holder.getSurface().isValid())
			{
				Canvas canvas = holder.lockCanvas();
				canvas.drawColor(Color.BLACK);
				
				if(percent > 1.0)
				{
					percent = 1.0;
				}
				else if(percent < 0.0)
				{
					percent = 0.0;
				}
				
				//value = ((float)Math.sin(f) + 1.0f) / 2.0f;
				red = (int)((-percent * percent + 1.0f) * MAX_COLOR);
				green = (int)((-(percent - 1.0f) * (percent - 1.0f) + 1.0f) * MAX_COLOR);
				
				if(red > MAX_COLOR)
				{
					red = MAX_COLOR;
				}
				if(green > MAX_COLOR)
				{
					green = MAX_COLOR;
				}

				paint.setColor(Color.rgb(red, green, 0));

				canvas.drawRect(0.0f, 25.0f, (float)percent * canvas.getWidth(), canvas.getHeight() - 25.0f, paint);

				//f += 0.01;

				holder.unlockCanvasAndPost(canvas);
			}
		}

		@Override
		public void onMessageReceived(String message)
		{
			try
			{
				percent = Double.parseDouble(message);
			}
			catch(Exception e)
			{
				//
			}
		}

		@Override
		public void onConnectionEstablished(InetAddress client)
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onConnectionLost(InetAddress client)
		{
			// TODO Auto-generated method stub
			
		}
	}
}
