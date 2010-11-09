package com.littlebighead.exploding;


import uk.ac.horizon.ug.exploding.client.ApplicationActivity;
import uk.ac.horizon.ug.exploding.client.BackgroundThread;
import uk.ac.horizon.ug.exploding.client.Client;
import uk.ac.horizon.ug.exploding.client.ClientMessageListener;
import uk.ac.horizon.ug.exploding.client.ClientState;
import uk.ac.horizon.ug.exploding.client.ExplodingPreferences;
import uk.ac.horizon.ug.exploding.client.GameMapActivity;
import uk.ac.horizon.ug.exploding.client.MessageStatusType;
import uk.ac.horizon.ug.exploding.client.R;
import uk.ac.horizon.ug.exploding.client.Client.QueuedMessage;
import uk.ac.horizon.ug.exploding.client.logging.LoggingActivity;
import uk.ac.horizon.ug.exploding.client.model.Member;
import uk.ac.horizon.ug.exploding.client.model.Player;
import uk.ac.horizon.ug.exploding.client.model.TimelineEvent;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;

import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import android.os.Bundle;
import android.preference.PreferenceManager;
//import android.os.CountDownTimer;

//import android.content.Intent;

import android.view.View.OnClickListener;



public class AddStoryView extends ApplicationActivity implements ClientMessageListener {	//implements OnClickListener {
	
    // BEGIN CMG
    static final String TAG = "AddStory";
	private static enum DialogId {
		CREATING_STORY
	}
	private Client cache;
	private QueuedMessage message;
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id==DialogId.CREATING_STORY.ordinal()) {
			ProgressDialog creatingPd = new ProgressDialog(this);
			creatingPd.setCancelable(true);
			creatingPd.setMessage("Adding Story...");
			creatingPd.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					if (cache!=null && message!=null) {
						GameMapActivity.logAction("createMember.cancel");
						cache.cancelMessage(message, true);
					}
				}
			});
			return creatingPd;
		}
		return super.onCreateDialog(id);
	}
	private void addStory() {
		try {
			TimelineEvent event = new TimelineEvent();
			EditText et;
			et = (EditText)findViewById(R.id.submit_story_title_edit_text);
			event.setName(et.getText().toString());
			if (event.getName()==null || event.getName().length()==0) {
				Toast.makeText(this, "Please give your story a title", Toast.LENGTH_SHORT).show();
				return;
			}
			et = (EditText)findViewById(R.id.submit_story_description_edit_text);
			event.setDescription(et.getText().toString());
			if (event.getDescription()==null || event.getDescription().length()==0) {
				Toast.makeText(this, "Please give a description of your story", Toast.LENGTH_SHORT).show();
				return;				
			}
			SeekBar sb;
			sb = (SeekBar)findViewById(R.id.HealthSeekBar);
			event.setHealth((int)(10.99*sb.getProgress()/sb.getMax())-5);
			sb = (SeekBar)findViewById(R.id.WealthSeekBar);
			event.setWealth((int)(10.99*sb.getProgress()/sb.getMax())-5);
			sb = (SeekBar)findViewById(R.id.ActionSeekBar);
			event.setAction((int)(10.99*sb.getProgress()/sb.getMax())-5);
			sb = (SeekBar)findViewById(R.id.BrainsSeekBar);
			event.setBrains((int)(10.99*sb.getProgress()/sb.getMax())-5);

			ClientState clientState = BackgroundThread.getClientState(this);
			Client cache = clientState.getCache();

			// ?!
			event.setZoneId(clientState.getZoneOrgID());

			Player player = (Player)cache.getFirstFact(Player.class.getName());
			event.setPlayerID(player.getID());

			GameMapActivity.logAction("addStory.start", "event", event.toString());

			// Note: this is (now) an async action
			message = cache.queueMessage(cache.addFactMessage(event), this);
			Log.i(TAG,"Creating member: "+event);

			showDialog(DialogId.CREATING_STORY.ordinal());
		} 
		catch (Exception e) {
			Toast.makeText(this, "Sorry: "+e, Toast.LENGTH_LONG).show();
			Log.e(TAG, "Creating event", e);
			GameMapActivity.logAction("addStory.error", "exception", e.toString());
		}
	}

	@Override
	public void onMessageResponse(MessageStatusType status,
			String errorMessage, Object value) {
		Log.d(TAG,"onMessageResponse: status="+status+", error="+errorMessage+", value="+value);
		dismissDialog(DialogId.CREATING_STORY.ordinal());

		if (status==MessageStatusType.OK) {
			GameMapActivity.logAction("createMember.ok");
			this.finish();
		}
		else {
			Toast.makeText(this, "Sorry: "+errorMessage, Toast.LENGTH_LONG).show();
			GameMapActivity.logAction("createMember.error", "error", errorMessage);
		}
		// tidy up
		message = null;
		cache = null;
	}
	// END CMG

	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    	
    	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	
    	setContentView(R.layout.submit_story);  
    	
		Button button = (Button)findViewById(R.id.submit_story_submit_button);
		//button.setOnClickListener(this);
		button.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	addStory();
		    }
		});

		button = (Button)findViewById(R.id.submit_story_cancel_button);
		//button.setOnClickListener(this);
		button.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	finish();
		    }
		});

    }
}
