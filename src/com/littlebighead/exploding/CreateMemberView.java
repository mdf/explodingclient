package com.littlebighead.exploding;


import java.util.ArrayList;

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
import uk.ac.horizon.ug.exploding.client.model.Position;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
//import android.os.CountDownTimer;

//import android.content.Intent;

import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;



public class CreateMemberView extends LoggingActivity implements ClientMessageListener {	//implements OnClickListener {
    DrawView drawView;	
	
    // BEGIN CMG
    static final String TAG = "CreateMember";
	private static enum DialogId {
		CREATING_MEMBER
	}
	private Client cache;
	private QueuedMessage createMemberMessage;
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id==DialogId.CREATING_MEMBER.ordinal()) {
			ProgressDialog creatingPd = new ProgressDialog(this);
			creatingPd.setCancelable(true);
			creatingPd.setMessage("Creating member...");
			creatingPd.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					if (cache!=null && createMemberMessage!=null) {
						GameMapActivity.logAction("createMember.cancel");
						cache.cancelMessage(createMemberMessage, true);
					}
				}
			});
			return creatingPd;
		}
		return super.onCreateDialog(id);
	}
	private void createMember() {
		try {
			Member member = new Member();

			//SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			//member.setName(preferences.getString(ExplodingPreferences.PLAYER_NAME, ""));
			
			member.setName(nameEditText.getText().toString());
			if (member.getName()==null || member.getName().length()==0) {
				Toast.makeText(this, "Please give your community member a name", Toast.LENGTH_SHORT).show();
				return;
			}

//			EditText et = (EditText)findViewById(R.id.create_member_action_edit_text);
//			member.setAction(Integer.parseInt(et.getText().toString()));
//			et = (EditText)findViewById(R.id.create_member_health_edit_text);
//			member.setHealth(Integer.parseInt(et.getText().toString()));
//			et = (EditText)findViewById(R.id.create_member_wealth_edit_text);
//			member.setWealth(Integer.parseInt(et.getText().toString()));
//			et = (EditText)findViewById(R.id.create_member_brains_edit_text);
//			member.setBrains(Integer.parseInt(et.getText().toString()));
//			et = (EditText)findViewById(R.id.create_member_name_edit_text);
//			member.setName(et.getText().toString());
			int attributes[] = drawView.body.getAttributes();
			member.setHealth(attributes[0]);
			member.setWealth(attributes[1]);
			member.setBrains(attributes[2]);
			member.setAction(attributes[3]);			
			member.setLimbData(drawView.body.getLimbInfo());
			
			ClientState clientState = BackgroundThread.getClientState(this);
			Client cache = clientState.getCache();
			
//			Location loc = clientState.getLastLocation();
//			if (loc!=null) {
//				// TODO don't place immediately
//				Position pos = new Position();
//				pos.setLatitude(loc.getLatitude());
//				pos.setLongitude(loc.getLongitude());
//				pos.setElevation(loc.getAltitude());
//				member.setPosition(pos);
//				member.setCarried(false);
//				member.setZone(clientState.getZoneOrgID());
//			}
//			else 
			member.setCarried(true);
			
			cache = clientState.getCache();

			GameMapActivity.logAction("createMember.start", "member", member.toString());

			// Note: this is (now) an async action
			createMemberMessage = cache.queueMessage(cache.addFactMessage(member), this);
			Log.i(TAG,"Creating member: "+member);

			// Can't do this because we have no ID (ready to place...)
			//GameMapActivity.setCurrentMember(member);
			
			showDialog(DialogId.CREATING_MEMBER.ordinal());
		} 
		catch (Exception e) {
			Toast.makeText(this, "Sorry: "+e, Toast.LENGTH_LONG).show();
			Log.e(TAG, "Creating member", e);
			GameMapActivity.logAction("createMember.error", "exception", e.toString());
		}
	}

	@Override
	public void onMessageResponse(MessageStatusType status,
			String errorMessage, Object value) {
		Log.d(TAG,"onMessageResponse: status="+status+", error="+errorMessage+", value="+value);
		dismissDialog(DialogId.CREATING_MEMBER.ordinal());

		if (status==MessageStatusType.OK) {
			GameMapActivity.logAction("createMember.ok");
	    	Intent resultIntent = new Intent();
	    	setResult(Activity.RESULT_OK, resultIntent);            		    	
			this.finish();
		}
		else {
			Toast.makeText(this, "Sorry: "+errorMessage, Toast.LENGTH_LONG).show();
			GameMapActivity.setCurrentMember(null);
			GameMapActivity.logAction("createMember.error", "error", errorMessage);
		}
		// tidy up
		createMemberMessage = null;
		cache = null;
	}
	static final int DEFAULT_COLOR = 0xff0000ff;
	/**
	 * @return
	 */
	private int getPlayerColor() {
		// TODO Auto-generated method stub
		ClientState cs = BackgroundThread.getClientState(this);
		if (cs==null)
			return DEFAULT_COLOR;
		Client cache = cs.getCache();
		if (cache==null)
			return DEFAULT_COLOR;
		Player player = (Player)cache.getFirstFact(Player.class.getName());
		if (player==null)
			return DEFAULT_COLOR;
		if (!player.isSetColourRef()) {
			Log.d(TAG,"Player colourRef unset");
			return DEFAULT_COLOR;
		}
		return PlayerColours.values()[player.getColourRef() % PlayerColours.values().length].color();
	}
	private EditText nameEditText;
	// END CMG
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    	
    	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	
//    	setContentView(R.layout.create_member);  

		//View view = (View)findViewById(R.id.View01);
		
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        
        TextView title = new TextView(this);
        title.setText("Community Member's Name:");
        mainLayout.addView(title);

        nameEditText = new EditText(this);
        nameEditText.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
        nameEditText.setPadding(5, 5, 5, 5);
        mainLayout.addView(nameEditText);
        
        
        
        // begin cmg
        Button buttons[] = new Button[4];
        // end cmg

        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
        
        for (int f=0; f<5; f++) {
        	Button button = new Button(this);
        	if (f<buttons.length)
        		buttons[f] = button;
        	button.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT,1));
        	button.setTextSize(20);
        	switch(f) {
        		case 0:
        			button.setBackgroundColor(0xff00aeef);
                	button.setText("H");
        			break;
        		case 1:
        			button.setBackgroundColor(0xffFFCC00);
                	button.setText("W");
        			break;
        		case 2:
        			button.setBackgroundColor(0xffFF6633);
                	button.setText("Kn");
        			break;
        		case 3:
        			button.setBackgroundColor(0xff999933);
                	button.setText("P");
        			break;
        		default:
        			button.setTextSize(15);
            		button.setText("Done");
//                	button.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT,2));
            		button.setOnClickListener(new OnClickListener() {
            		    public void onClick(View v) {

            		    	// cmg
            		    	createMember();
            		    	// end cmg
            		    	
//            		    	Intent resultIntent = new Intent();
//            		    	Bundle extras = new Bundle();
//            		    	extras.putSerializable("limbInfo", drawView.body.getLimbInfo());
//            		    	extras.putSerializable("attributes", drawView.body.getLimbInfo());
//            		    	resultIntent.putExtras(extras);
            		    	          		    	
//            		    	resultIntent.putExtra("limbs", drawView.body.limbs);
//            		    	setResult(Activity.RESULT_OK, resultIntent);            		    	
//            		    	finish();
            		    }
            		});
        	}
        	
        	buttonLayout.addView(button);
        	
        }
        
        
        RelativeLayout drawLayout = new RelativeLayout(this);
        mainLayout.addView(drawLayout);
        drawLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,600));

        drawView = new DrawView(this, getPlayerColor(), buttons);
        drawView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 600));
        //mainLayout.addView(drawView);
        drawLayout.addView(drawView);
        drawView.requestFocus();
        mainLayout.addView(buttonLayout);

        
        LinearLayout sizeButtons = new LinearLayout(this);
        sizeButtons.setOrientation(LinearLayout.HORIZONTAL);
        sizeButtons.setGravity(Gravity.LEFT);
        sizeButtons.setLayoutParams(new RelativeLayout.LayoutParams(200,100));
        drawLayout.addView(sizeButtons);
        
        Button sizeUp = new Button(this);
        sizeUp.setText("+");
        sizeUp.setTextSize(20);
        sizeUp.setLayoutParams(new LinearLayout.LayoutParams(60,60));
        sizeUp.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	if (Limb.prev != null) {
		    		Limb.prev.xradius += 5;
		    		Limb.prev.yradius += 5;
		    		if (Limb.prev.xradius > 100) Limb.prev.xradius = 100;
		    		if (Limb.prev.yradius > 100) Limb.prev.yradius = 100;
		    		drawView.invalidate();
		    	}
			}

		});
        sizeButtons.addView(sizeUp);
        
        Button sizeDown = new Button(this);
        sizeDown.setText("-");
        sizeDown.setTextSize(20);
        sizeDown.setLayoutParams(new LinearLayout.LayoutParams(60,60));
        sizeDown.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	if (Limb.prev != null) {
		    		Limb.prev.xradius -= 5;
		    		Limb.prev.yradius -= 5;
		    		if (Limb.prev.xradius < 10) Limb.prev.xradius = 10;
		    		if (Limb.prev.yradius < 10) Limb.prev.yradius = 10;
		    		drawView.invalidate();
		    	}
			}
		});
        sizeButtons.addView(sizeDown);

        
                
        setContentView(mainLayout);
/*        
        
        Button button = new Button(this);
        button.setText("Done");
        button.setWidth(100);
        button.setHeight(100);
        button.x = 100;
        */
        
        /*
		Button button = (Button)findViewById(R.id.done_button);
		//button.setOnClickListener(this);
		button.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	finish();
		    }
		});
		*/


    }
}

