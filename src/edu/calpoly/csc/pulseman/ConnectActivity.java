package edu.calpoly.csc.pulseman;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ConnectActivity extends Activity
{
	private EditText editText;
	private Button button;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connect);

		editText = (EditText)findViewById(R.id.connectEditText);
		button = (Button)findViewById(R.id.connectButton);

		button.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				final String ipAddress = editText.getText().toString();

				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ConnectActivity.this);
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(MainActivity.IP_ADDRESS, ipAddress);
				editor.commit();

				Intent intent = new Intent(ConnectActivity.this, MainActivity.class);
				intent.putExtra(MainActivity.IP_ADDRESS, ipAddress);

				startActivity(intent);
				finish();
			}
		});
	}

	public void onResume()
	{
		super.onResume();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String defaultIP = preferences.getString(MainActivity.IP_ADDRESS, "");

		editText.setText(defaultIP);
	}
}