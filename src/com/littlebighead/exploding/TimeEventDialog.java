package com.littlebighead.exploding;


import java.util.List;

import uk.ac.horizon.ug.exploding.client.ApplicationActivity;
import uk.ac.horizon.ug.exploding.client.R;
import uk.ac.horizon.ug.exploding.client.logging.LoggingActivity;
import uk.ac.horizon.ug.exploding.client.model.Message;
import android.app.Activity;
import android.content.Intent;

import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import android.os.Bundle;
//import android.os.CountDownTimer;

//import android.content.Intent;

import android.view.View.OnClickListener;





public class TimeEventDialog extends ApplicationActivity {	
	private static final String TAG = "TimeEventDialog";
//implements OnClickListener {
	
	static List<Message> messages = null;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    	
    	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	
    	setContentView(R.layout.event_dialogue);  
    	
		Button button = (Button)findViewById(R.id.dismiss_button);
		//button.setOnClickListener(this);
		button.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	finish();
		    }
		});
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			Log.i("EXTRAS", "Found extras");
			TextView tv = (TextView)findViewById(R.id.year);
			tv.setText(extras.getString("year"));
//			String value = extras.getString("keyName");
			tv = (TextView)findViewById(R.id.name);
			tv.setText(extras.getString("name"));
			tv = (TextView)findViewById(R.id.description);
			tv.setText(extras.getString("desc"));
			int mi = extras.getInt("messageIndex");
			// TODO
			List<Message> ms = messages;
			Log.d(TAG,"mi="+mi+", "+(ms.size())+" messages");
			Button b = (Button)findViewById(R.id.previous_message);
			if (mi>0 && ms.size()>mi-1) 
				initButton(b, ms, mi-1);
			else
				b.setEnabled(false);
			b = (Button)findViewById(R.id.next_message);
			if (mi>=0 && mi+1<ms.size()) 
				initButton(b, ms, mi+1);
			else
				b.setEnabled(false);
		}		

    }

	/**
	 * @param b
	 * @param ms
	 * @param i
	 */
	private void initButton(Button b, List<Message> ms, final int nmi) {
		// TODO Auto-generated method stub
		b.setEnabled(true);
		final Message currentMessage = ms.get(nmi);
		b.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent myIntent = new Intent();
				myIntent.setClassName("uk.ac.horizon.ug.exploding.client", "com.littlebighead.exploding.TimeEventDialog");

				if (currentMessage.getYear()!=null)
					myIntent.putExtra("year", currentMessage.getYear());
				if (currentMessage.getTitle()!=null)
					myIntent.putExtra("name", currentMessage.getTitle());
				if (currentMessage.getDescription()!=null)
					myIntent.putExtra("desc", currentMessage.getDescription());

				myIntent.putExtra("messageIndex", nmi);
				
				finish();
				startActivity(myIntent);
			}
		});

	}

	/**
	 * @param messages2
	 */
	public static void setMessages(List<Message> messages2) {
		messages = messages2;		
	}
    

}

