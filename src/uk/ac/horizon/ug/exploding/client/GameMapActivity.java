/**
 * Copyright 2010 The University of Nottingham

 * 
 * This file is part of GenericAndroidClient.
 *
 *  GenericAndroidClient is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  GenericAndroidClient is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with GenericAndroidClient.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package uk.ac.horizon.ug.exploding.client;

import com.littlebighead.exploding.Body;
import com.littlebighead.exploding.CommunityView;
import com.littlebighead.exploding.Limb;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONStringer;

import uk.ac.horizon.ug.exploding.client.Client.QueuedMessage;
import uk.ac.horizon.ug.exploding.client.model.Game;

import uk.ac.horizon.ug.exploding.client.logging.ActivityLogger;
import uk.ac.horizon.ug.exploding.client.logging.LoggingActivity;
import uk.ac.horizon.ug.exploding.client.logging.LoggingUtils;

import uk.ac.horizon.ug.exploding.client.model.Member;
import uk.ac.horizon.ug.exploding.client.model.Message;
import uk.ac.horizon.ug.exploding.client.model.Player;
import uk.ac.horizon.ug.exploding.client.model.Position;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.ItemizedOverlay.OnFocusChangeListener;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author cmg
 * @author Robin
 *
 */
public class GameMapActivity extends MapActivity implements ClientStateListener, ClientMessageListener, OnFocusChangeListener {

	private static final String TAG = "Map";
	private static final int MILLION = 1000000;
	private static final int MIN_ZOOM_LEVEL = 19;// Robin Default
	private MyLocationOverlay myLocationOverlay;
	private MyMapOverlay itemOverlay;
	private static Member currentMember;
	private MapView mapView;

	static enum DialogId { PLACE, PLACE_TO_SERVER, CARRY, CARRY_TO_SERVER /*, NEW_CONTENT*/ };
	private ActivityLogger logger = new ActivityLogger(this);

	public static String LOGTYPE_GAME_ACTION = "GameAction";
	public static String LOGTYPE_GAME_STATE = "GameState";
	public static void logAction(String action) {
		log(LOGTYPE_GAME_ACTION, action, null, null);
	}
	public static void logState(String action) {
		log(LOGTYPE_GAME_STATE, action, null, null);
	}
	public static void logAction(String action, String extraKey, Object extraValue) {
		log(LOGTYPE_GAME_ACTION, action, extraKey, extraValue);
	}
	public static void logState(String action, String extraKey, Object extraValue) {
		log(LOGTYPE_GAME_STATE, action, extraKey, extraValue);
	}
	public static void log(String type, String action, String extraKey, Object extraValue) {
		try {
			JSONStringer js = new JSONStringer();
			js.object();
			js.key("action");
			js.value(action);
			if (extraKey!=null) {
				js.key(extraKey);
				js.value(extraValue);
			}
			js.endObject();
			LoggingUtils.log(type, js.toString());
		}
		catch (Exception e) {
			Log.e(TAG,"log("+action+","+extraKey+","+extraValue+")", e);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		logger.logOnCreate(this, savedInstanceState);
    	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		try {
			Log.d(TAG, "Try to load map view");
			setContentView(R.layout.map);
			// BEGIN Robin's code - from com.littlebighead.exploding.GameMapView
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			mapView = (MapView)findViewById(R.id.map_view);
			mapView.setReticleDrawMode(MapView.ReticleDrawMode.DRAW_RETICLE_OVER);
			Button button = (Button)findViewById(R.id.map_story_button);

			//button.setOnClickListener(this);
			button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					if (canAuthor(true)) {
						logAction("CreateStoryButton");
						setCurrentMember(null);
						Intent myIntent = new Intent();
						myIntent.setClassName("uk.ac.horizon.ug.exploding.client", "com.littlebighead.exploding.AddStoryView");
						startActivity(myIntent);
					}
				}
			});

			button = (Button)findViewById(R.id.map_community_button);
			//			button.setOnClickListener(this);
			button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					logAction("CommunitiesButton");
					setCurrentMember(null);
					Intent myIntent = new Intent();
					myIntent.setClassName("uk.ac.horizon.ug.exploding.client", "com.littlebighead.exploding.CommunityView");
					startActivity(myIntent);
				}
			});

			button = (Button)findViewById(R.id.map_create_button);
			//			button.setOnClickListener(this);
			button.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					if (canCreateMember(true)) {
						logAction("CreateMemberButton");
						setCurrentMember(null);
						Intent myIntent = new Intent();
						myIntent.setClassName("uk.ac.horizon.ug.exploding.client", "com.littlebighead.exploding.CreateMemberView");
						startActivityForResult(myIntent,1);
					}
				}
			});


			// END Robin's code
			mapView.setBuiltInZoomControls(true);
			myLocationOverlay = new MyLocationOverlay(this, mapView);
			myLocationOverlay.runOnFirstFix(new Runnable() {
				public void run() {
					centreOnMyLocation();

					if (currentMember!=null) {
						if (currentMember.isSetCarried() && currentMember.getCarried()) {					
							askToPlace();
						} 
					}
				}
			});
			Resources res = getResources();
			Drawable drawable = res.getDrawable(R.drawable.icon/*android.R.drawable.btn_star*/);
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			Log.d(TAG,"defaultDrawable="+drawable);
			ClientState clientState = BackgroundThread.getClientState(this);
			itemOverlay = new MyMapOverlay(drawable, clientState);
			BackgroundThread.addClientStateListener(itemOverlay, this, Member.class.getName());
			itemOverlay.setOnFocusChangeListener(this);

			mapView.getOverlays().add(myLocationOverlay);
			mapView.getOverlays().add(itemOverlay);

		}
		catch (Exception e) {
			Log.e(TAG, "Error loading map view: "+e);
		}
		Set<String> types = new HashSet<String>();
		types.add(Message.class.getName());
		types.add(Game.class.getName());
		types.add(Member.class.getName());
		types.add(Player.class.getName());
		BackgroundThread.addClientStateListener(this, this, ClientState.Part.ZONE.flag(), types);
		ClientState clientState = BackgroundThread.getClientState(this);
		clientStateChanged(clientState, true);
		centreOnMyLocation();
	}
	private static final long ZONE_VIBRATE_MS = 500;

	private String currentYear = null;

	@Override
	public void clientStateChanged(final ClientState clientState) {
		clientStateChanged(clientState, false);
	}

	public void clientStateChanged(final ClientState clientState, boolean isInitial) {
		if (clientState==null)
			return;
		if (clientState.isZoneChanged() || !isInitial)
			zoneChanged(clientState.getZoneID());
		if (isInitial || clientState.getChangedTypes().contains(Message.class.getName()))
			handleMessages(clientState);
		if (isInitial || clientState.getChangedTypes().contains(Game.class.getName()))
			checkYear(clientState);
		if (isInitial || clientState.getChangedTypes().contains(Member.class.getName()))
			updateMembers(clientState);
		if (isInitial || clientState.getChangedTypes().contains(Player.class.getName()))
			updateButtons(clientState);
	}

	/**
	 * @param clientState
	 */
	private void updateButtons(ClientState clientState) {
		Client cache = clientState.getCache();
		if (cache==null)
			return;
		Player player = (Player)cache.getFirstFact(Player.class.getName());
		Button button;
		button = (Button)findViewById(R.id.map_story_button);
		button.setEnabled(canAuthor(false));
		button = (Button)findViewById(R.id.map_create_button);
		button.setEnabled(canCreateMember(false));
		button = (Button)findViewById(R.id.map_community_button);
		// dis/enable communities
		button.setEnabled(cache.getFirstFact(Member.class.getName())!=null);
	}
	/**
	 * @param clientState
	 */
	private void updateMembers(ClientState clientState) {
		Client cache = clientState.getCache();
		if (cache==null)
			return;
		List<Member> members = CommunityView.getMyMembers(clientState);//cache.getFacts(Member.class.getName());
		Member average = new Member();
		average.setHealth(0);
		average.setWealth(0);
		average.setAction(0);
		average.setBrains(0);
		for (Member member : members) {
			if (member.isSetHealth())
				average.setHealth(average.getHealth()+member.getHealth());
			if (member.isSetWealth())
				average.setWealth(average.getWealth()+member.getWealth());
			if (member.isSetAction())
				average.setAction(average.getAction()+member.getAction());
			if (member.isSetBrains())
				average.setBrains(average.getBrains()+member.getBrains());
		}
		if (members.size()>0) {
			average.setHealth(average.getHealth()/members.size());
			average.setWealth(average.getWealth()/members.size());
			average.setAction(average.getAction()/members.size());
			average.setBrains(average.getBrains()/members.size());
			updateAttributes2(average);
		}
		else
			updateAttributes2(null);
		TextView membersTextView = (TextView)findViewById(R.id.MemberCntTextView);
		membersTextView.setText(""+members.size());		
		logState("updateMembers", "members", members.size());
		mapView.invalidate();
	}

	/**
	 * @param clientState
	 */
	private void checkYear(ClientState clientState) {
		// TODO Auto-generated method stub
		Client cache = clientState.getCache();
		if (cache==null)
			return;
		List<Object> games = cache.getFacts(Game.class.getName());
		if (games.size()>0) {
			Game game = (Game)games.get(0);
			if (game.getYear()!=null && !game.getYear().equals(currentYear)) {
				// BEGIN ROBIN
				TextView yearTextView = (TextView)findViewById(R.id.YearTextView);
				// END ROBIN
				yearTextView.setText(game.getYear());
				logState("updateYear", "year", game.getYear());
			}
		}
	}

	/**
	 * @param zoneID
	 */
	protected void zoneChanged(String zoneID) {
		//		logger.log("Zone", "zoneID", zoneID);
		Log.d(TAG, "Zone change to "+zoneID);
		if (zoneID!=null) {
			Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
			if (vibrator!=null)
				vibrator.vibrate(ZONE_VIBRATE_MS);
			Toast.makeText(GameMapActivity.this, "Entered zone "+zoneID, Toast.LENGTH_SHORT).show();
			// BEGIN ROBIN
			TextView zoneTextView = (TextView)findViewById(R.id.ZoneTextView);
			zoneTextView.setText("You are in: "+zoneID);
			logState("updateZone", "zoneID", zoneID);
			// END ROBIN
		}
	}		
	/**
	 * @param clientState
	 */
	private void handleMessages(ClientState clientState) {
		if (clientState==null || clientState.getCache()==null) 
			return;
		List<Object> messages = clientState.getCache().getFacts(Message.class.getName());
		if (messages.size()==0)
			return;
		Log.d(TAG,"Messages: "+messages.size());

		playAudio();

		Message bestContentMessage = null;
		for (Object m : messages) {
			Message message = (Message)m;
			Log.d(TAG, "Message: "+m);
			//NotificationUtils.postMessage(this, message);
			clientState.getCache().removeFactSilent(message);
			// TODO add global content type
			if (uk.ac.horizon.ug.exploding.client.model.Message.MSG_TIMELINE_CONTENT.equals(message.getType()) ||
					uk.ac.horizon.ug.exploding.client.model.Message.MSG_TIMELINE_CONTENT_GLOBAL.equals(message.getType())) {
				// Pick one...
				if (bestContentMessage==null)
					bestContentMessage = message;
				else if (uk.ac.horizon.ug.exploding.client.model.Message.MSG_TIMELINE_CONTENT_GLOBAL.equals(message.getType()))
					// global event > non-global
					bestContentMessage = message;

			}
			else {
				logState("newMessage.context", "message", message.toString());

				if (message.getTitle()!=null) {
					// context message -> toast
					Toast.makeText(this, message.getTitle(), Toast.LENGTH_LONG).show();
				}
				else
					Log.e(TAG,"Ignoring context message with no title: "+message);
			}
		}
		if (bestContentMessage!=null) {
			currentMessage = bestContentMessage;
			logState("newMessage", "message", currentMessage.toString());
			Log.d(TAG,"show NEW_CONTENT dialog for "+currentMessage);
			//showDialog(DialogId.NEW_CONTENT.ordinal());

			TextView textview = (TextView)findViewById(R.id.ContentTextView);
			if (currentMessage != null){

				textview.setText(currentMessage.getTitle() + " [more..]");
				textview.setVisibility(TextView.VISIBLE);
				textview.setOnClickListener(new OnClickListener(){
					public void onClick(View v){
						
						Intent myIntent = new Intent();
						myIntent.setClassName("uk.ac.horizon.ug.exploding.client", "com.littlebighead.exploding.TimeEventDialog");
						
						if (currentMessage.getYear()!=null)
							myIntent.putExtra("year", currentMessage.getYear());
						if (currentMessage.getTitle()!=null)
							myIntent.putExtra("name", currentMessage.getTitle());
						if (currentMessage.getDescription()!=null)
							myIntent.putExtra("desc", currentMessage.getDescription());

						startActivity(myIntent);

					}
				});

			}
			//Intent myIntent = new Intent();
			//myIntent.setClassName("uk.ac.horizon.ug.exploding.client", "com.littlebighead.exploding.TimeEventSmallDialog");
			//if (currentMessage.getYear()!=null)
			//	myIntent.putExtra("year", currentMessage.getYear());
			//if (currentMessage.getTitle()!=null)
			//	myIntent.putExtra("name", currentMessage.getTitle());
			//if (currentMessage.getDescription()!=null)
			//	myIntent.putExtra("desc", currentMessage.getDescription());

			//startActivity(myIntent);

			//			Dialog dialog = getNewContentDialog();
			//			if (currentMessage!=null && currentMessage.getTitle()!=null) {
			//				Log.d(TAG,"Prepare NEW_CONTENT dialog with title "+currentMessage.getTitle());
			//				dialog.setTitle(currentMessage.getTitle());
			//			}
			//			else {
			//				Log.e(TAG,"prepare new content dialog with no current message title");
			//				dialog.setTitle("Something's happening...");
			//			}
			//			dialog.show();
			//			
			//			if (newContentTimer!=null)
			//				newContentTimer.cancel();
			//			newContentTimer = new CountDownTimer(NEW_CONTENT_TIME_MS, NEW_CONTENT_TIME_MS) {
			//				@Override
			//				public void onTick(long millisUntilFinished) {
			//				}
			//				@Override
			//				public void onFinish() {
			//					if (newContentDialog!=null && newContentDialog.isShowing()) {
			//						Log.d(TAG,"Timeout dismiss new content dialog");
			//						//dismissDialog(DialogId.NEW_CONTENT.ordinal());
			//						if (newContentDialog!=null && newContentDialog.isShowing())
			//							newContentDialog.hide();
			//					}
			//				}
			//			};
			//			newContentTimer.start();
		}
	}
	// BEGIN ROBIN
	private MediaPlayer mMediaPlayer = null;
	private void playAudio () {
		try {
			if (mMediaPlayer == null)
				mMediaPlayer = MediaPlayer.create(this, R.raw.beep);

			if (mMediaPlayer != null) {
				// http://www.soundjay.com/beep-sounds-1.html lots of free beeps here
				if (mMediaPlayer.isPlaying() == false) {
					mMediaPlayer.setLooping(false);
					mMediaPlayer.start();
				}
			}
		} catch (Exception e) {
			Log.e("beep", "error: " + e.getMessage(), e);
		}
	}
	// END ROBIN
	//	@Override
	//	public boolean onCreateOptionsMenu(Menu menu) {
	//    	MenuInflater inflater = getMenuInflater();    
	//    	inflater.inflate(R.menu.map_menu, menu);    
	//    	return true;
	//	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		logger.logOnOptionsItemSelected(item);
		switch (item.getItemId()) {
		case R.id.map_my_location:
			centreOnMyLocation();
			return true;
		case R.id.map_menu_gps:
		{
			Intent intent = new Intent();
			intent.setClass(this, GpsStatusActivity.class);
			startActivity(intent);
			return true;
		}			
		case R.id.map_menu_create_member:
		{
			if (!canCreateMember(true))
				return true;
			Intent intent = new Intent();
			intent.setClass(this, CreateMemberActivity.class);
			startActivity(intent);
			return true;
		}						
		}
		return super.onOptionsItemSelected(item);
	}
	private boolean canCreateMember(boolean notify) {
		// check if we can...
		Player player = getPlayer();
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		boolean standaloneMode = preferences.getBoolean("standaloneMode", false);
		if (!standaloneMode && (player==null || !player.isSetNewMemberQuota() || player.getNewMemberQuota()<1)) {
			if (notify)
				Toast.makeText(this, "You cannot create a member yet - keep playing", Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
	private boolean canAuthor(boolean notify) {
		// check if we can...
		Player player = getPlayer();
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		boolean standaloneMode = preferences.getBoolean("standaloneMode", false);
		if (!standaloneMode && (player==null || !player.isSetCanAuthor() || player.getCanAuthor()==false)) {
			if (notify)
				Toast.makeText(this, "You cannot create a story yet - keep playing", Toast.LENGTH_LONG).show();
			return false;
		}
		return true;
	}
	/**
	 * @return
	 */
	private Player getPlayer() {
		Client cache = BackgroundThread.getClientState(this).getCache();
		if (cache==null)
			return null;
		List<Object> players = cache.getFacts(Player.class.getName());
		if (players.size()==0)
			return null;
		return (Player)players.get(0);
	}

	private void centreOnMyLocation() {
		try {
			Location loc = LocationUtils.getCurrentLocation(this);
			if (loc!=null) {
				centreOn(loc.getLatitude(), loc.getLongitude());
			}
			else
			{
				Toast.makeText(this, "Current location unknown", Toast.LENGTH_SHORT).show();
			}
		}catch (Exception e) {
			Log.e(TAG, "doing centreOnMyLocation", e);
		}
	}

	/**
	 * @param latitude
	 * @param longitude
	 */
	private void centreOn(double latitude, double longitude) {
		// TODO Auto-generated method stub
		MapView mapView = (MapView)findViewById(R.id.map_view);
		MapController controller = mapView.getController();
		int zoomLevel = mapView.getZoomLevel();
		// zoom Level 15 is about 1000m on a side
		if (zoomLevel < MIN_ZOOM_LEVEL)
			controller.setZoom(MIN_ZOOM_LEVEL);
		GeoPoint point = new GeoPoint((int)(latitude*MILLION), (int)(longitude*MILLION));
		controller.animateTo(point);		
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void onPause() {
		logger.logOnPause();
		// TODO Auto-generated method stub
		//LocationUtils.unregisterOnThread(this, this, null);
		myLocationOverlay.disableCompass();
		myLocationOverlay.disableMyLocation();
		setCurrentMember(null);
		super.onPause();
	}

	private Location placeLocation;
	private int placeZone;
	@Override
	protected void onResume() {
		logger.logOnResume();
		// TODO Auto-generated method stub
		super.onResume();
		myLocationOverlay.enableCompass();
		myLocationOverlay.enableMyLocation();
		//		LocationUtils.registerOnThread(this, this, null);
		itemOverlay.setFocus(null);
		//updateAttributes(currentMember);
		if (currentMember!=null) {
			if (currentMember.isSetCarried() && currentMember.getCarried()) {

				centreOnMyLocation();
				askToPlace();
			} 
			else {
				if (currentMember.isSetPosition()) {
					Position p = currentMember.getPosition();
					if (p.isSetLatitude() && p.isSetLongitude())
						centreOn(p.getLatitude(), p.getLongitude());
				}				
				int pos = itemOverlay.indexOf(currentMember);
				if (pos>=0) {
					itemOverlay.setFocus(itemOverlay.getItem(pos));
					Log.d(TAG,"Set focus to item "+pos);
				}
				else
					Log.d(TAG,"Could not find member in overlay: "+currentMember);
				checkCarry();
			}
		}
	}		
	private void askToPlace() {
		if (currentMember==null || !currentMember.isSetCarried() || !currentMember.getCarried())
		{
			Log.e(TAG,"askToPlace called with null/uncarried member: "+currentMember);
			return;
		}
		try {
			ClientState cs = BackgroundThread.getClientState(this);
			placeLocation = LocationUtils.getCurrentLocation(this);
			placeZone = cs.getZoneOrgID();
			// drop...
			if (placeLocation!=null) {
				showDialog(DialogId.PLACE.ordinal());
			}
		}
		catch (Exception e) {
			Log.e(TAG,"askToPlace()", e);
		}
	}
	/**
	 * 
	 */
	private void checkCarry() {
		if (canCarry()) {
			showDialog(DialogId.CARRY.ordinal());
		}
	}
	private boolean canCarry() {
		// TODO Auto-generated method stub
		if (currentMember==null)
			return false;
		if (currentMember.isSetCarried() && currentMember.getCarried())
			return false;
		// "avatar" - can't pick up first member - it should follow you
		if (currentMember.getParentMemberID()==null)
			return false;
		ClientState cs = BackgroundThread.getClientState(this);
		if (cs==null)
			return false;
		if (LocationUtils.getCurrentLocation(this)==null)
			return false;
		Location loc = cs.getLastLocation();
		if (loc==null)
			return false;
		Position pos = currentMember.getPosition();
		if (pos==null)
			return false;
		Location l2 = new Location("gps");
		l2.setLatitude(pos.getLatitude());
		l2.setLongitude(pos.getLongitude());
		l2.setAltitude(pos.getElevation());
		double dist = loc.distanceTo(l2);
		if (dist>CARRY_DISTANCE_M)
			return false;
		return true;
	}
	private static double CARRY_DISTANCE_M = 20;
	//	@Override
	//	public void onLocationChanged(Location location) {
	//		// TODO Auto-generated method stub
	//		
	//	}
	//
	//	@Override
	//	public void onProviderDisabled(String provider) {
	//		// TODO Auto-generated method stub
	//		
	//	}
	//
	//	@Override
	//	public void onProviderEnabled(String provider) {
	//		// TODO Auto-generated method stub
	//		
	//	}
	//
	//	@Override
	//	public void onStatusChanged(String provider, int status, Bundle extras) {
	//		// TODO Auto-generated method stub
	//		
	//	}


	// BEGIN ROBIN  com.littlebighead.exploding.GameMapView
	/** create member activity result */
	@Override 
	public void onActivityResult(int requestCode, int resultCode, Intent data) {     
		super.onActivityResult(requestCode, resultCode, data); 
		switch(requestCode) { 
		case (1) : { 
			if (resultCode == Activity.RESULT_OK) { 

				//        	  ArrayList<Limb> limbs = (ArrayList<Limb>)data.getExtras().get("limbs");
				//        	  for (Limb limb: Body.limbs) {
				//        		  Log.i("limb position", Double.toString(limb.x));
				//       	  }
				// can't place immediately for now so push to community with a message
				Toast.makeText(this, "Your new community member will appear in a moment", Toast.LENGTH_LONG).show();

				setCurrentMember(null);
				Intent myIntent = new Intent();
				myIntent.setClassName("uk.ac.horizon.ug.exploding.client", "com.littlebighead.exploding.CommunityView");
				startActivity(myIntent);

			} 
			break; 
		} 
		} 
	}
	// END ROBIN

	public static Member getCurrentMember() {
		return currentMember;
	}

	public static void setCurrentMember(Member currentMember) {
		GameMapActivity.currentMember = currentMember;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id==DialogId.PLACE.ordinal()) {
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.place_member_dialog);
			dialog.setCancelable(true);
			dialog.setTitle("Place Here?");
			dialog.setOnCancelListener(new OnCancelListener()  {				
				@Override
				public void onCancel(DialogInterface dialog) {
					dismissDialog(DialogId.PLACE.ordinal());
				}
			});
			Button ok = (Button)dialog.findViewById(R.id.place_member_dialog_ok_button);
			ok.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dismissDialog(DialogId.PLACE.ordinal());
					//playerDialogActive = false;
					// TODO
					placeCurrentMember();
				}
			});
			Button cancel = (Button)dialog.findViewById(R.id.place_member_dialog_cancel_button);
			cancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					dialog.cancel();
				}

			});
			return dialog;

		}
		if (id==DialogId.PLACE_TO_SERVER.ordinal()) {
			ProgressDialog creatingPd = new ProgressDialog(this);
			creatingPd.setCancelable(true);
			creatingPd.setMessage("Placing member...");
			creatingPd.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					if (cache!=null && placeMemberMessage!=null) 
						cache.cancelMessage(placeMemberMessage, true);
					cache = null;
					placeMemberMessage = null;
					placedMember = null;
				}
			});
			return creatingPd;
		}
		if (id==DialogId.CARRY.ordinal()) {
			final Dialog dialog = new Dialog(this);
			dialog.setContentView(R.layout.carry_member_dialog);
			dialog.setCancelable(true);
			dialog.setTitle("Closest Member");
			dialog.setOnCancelListener(new OnCancelListener()  {				
				@Override
				public void onCancel(DialogInterface dialog) {
					dismissDialog(DialogId.CARRY.ordinal());
				}
			});
			Button ok = (Button)dialog.findViewById(R.id.carry_member_dialog_ok_button);
			ok.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dismissDialog(DialogId.CARRY.ordinal());
					//playerDialogActive = false;
					// TODO
					carryCurrentMember();
				}
			});
			Button cancel = (Button)dialog.findViewById(R.id.carry_member_dialog_cancel_button);
			cancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					dialog.cancel();
				}

			});
			return dialog;

		}
		if (id==DialogId.CARRY_TO_SERVER.ordinal()) {
			ProgressDialog creatingPd = new ProgressDialog(this);
			creatingPd.setCancelable(true);
			creatingPd.setMessage("Moving member...");
			creatingPd.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					if (cache!=null && placeMemberMessage!=null) 
						cache.cancelMessage(placeMemberMessage, true);
					cache = null;
					placeMemberMessage = null;
					placedMember = null;
				}
			});
			return creatingPd;
		}
		//		if (id==DialogId.NEW_CONTENT.ordinal()) {
		//			return getNewContentDialog();
		//			
		//		}
		return super.onCreateDialog(id);
	}

	// See TimeEventSmallDialog
	//
	//	private Dialog getNewContentDialog() {
	//		final Dialog dialog = new Dialog(this);
	//		dialog.setContentView(R.layout.new_content_dialog);
	//		dialog.setCancelable(true);
	//		dialog.setTitle("{Message...}");
	//		dialog.setOnCancelListener(new OnCancelListener()  {				
	//			@Override
	//			public void onCancel(DialogInterface dialog) {
	//				//dismissDialog(DialogId.NEW_CONTENT.ordinal());
	//				dialog.dismiss();
	//			}
	//		});
	//		Button ok = (Button)dialog.findViewById(R.id.new_content_dialog_ok_button);
	//		ok.setOnClickListener(new OnClickListener() {
	//			@Override
	//			public void onClick(View v) {					
	//				//dismissDialog(DialogId.NEW_CONTENT.ordinal());
	//				dialog.dismiss();
	//				showNewEvent();
	//			}
	//		});
	//		Button cancel = (Button)dialog.findViewById(R.id.new_content_dialog_cancel_button);
	//		cancel.setOnClickListener(new OnClickListener() {
	//			@Override
	//			public void onClick(View arg0) {
	//				dialog.cancel();
	//			}
	//			
	//		});
	//		newContentDialog = dialog;
	//		return dialog;
	//	}
	/* (non-Javadoc)
	 * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
	 */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		//		if (id==DialogId.NEW_CONTENT.ordinal()) {
		//			// fix title
		//			if (currentMessage!=null && currentMessage.getTitle()!=null) {
		//				Log.d(TAG,"Prepare NEW_CONTENT dialog with title "+currentMessage.getTitle());
		//				dialog.setTitle(currentMessage.getTitle());
		//			}
		//			else {
		//				Log.e(TAG,"prepare new content dialog with no current message title");
		//				dialog.setTitle("Something's happening...");
		//			}
		//		}
		super.onPrepareDialog(id, dialog);
	}
	/** message to show if the user asks for details... */
	private Message currentMessage;
	//	private Dialog newContentDialog;
	//	private CountDownTimer newContentTimer;
	protected void showNewEvent() {
		if (currentMessage==null) {
			Log.e(TAG,"showNewEvent(null)");
			return;
		}
		// BEGIN ROBIN
		//if (message.getType())...??
		//		logState("newMessage", "message", currentMessage.toString());
		Log.d(TAG,"showNewEvent("+currentMessage+")");
		logAction("showMessage", "message", currentMessage.toString());
		Intent myIntent = new Intent();
		myIntent.setClassName("uk.ac.horizon.ug.exploding.client", "com.littlebighead.exploding.TimeEventDialog");
		if (currentMessage.getYear()!=null)
			myIntent.putExtra("year", currentMessage.getYear());
		if (currentMessage.getTitle()!=null)
			myIntent.putExtra("name", currentMessage.getTitle());
		if (currentMessage.getDescription()!=null)
			myIntent.putExtra("desc", currentMessage.getDescription());

		startActivity(myIntent);
		// END ROBIN

	}
	/**
	 * 
	 */
	protected void carryCurrentMember() {
		if (currentMember!=null) {
			logAction("carryMember.start", "memberID", currentMember.getID());
			Member m = new Member();
			m.setID(currentMember.getID());
			m.setCarried(true);
			m.setPosition(currentMember.getPosition());
			m.setZone(currentMember.getZone());
			placedMember = m;

			ClientState cs = BackgroundThread.getClientState(this);
			if (cs==null) {
				Log.e(TAG,"carryCurrentMember: ClientState null");
				return;
			}
			cache = cs.getCache();
			if (cache==null) {
				Log.e(TAG,"carryCurrentMember: ClientState null");
				return;
			}				
			try {
				// Note: this is (now) an async action
				placeMemberMessage = cache.queueMessage(cache.updateFactMessage(currentMember, m), this);
				Log.i(TAG,"Carry member: "+m);

				showDialog(DialogId.CARRY_TO_SERVER.ordinal());
			}
			catch (Exception e) {
				Toast.makeText(this, "Sorry: "+e, Toast.LENGTH_LONG).show();
				Log.e(TAG, "Carrying member", e);
				logAction("carryMember.error", "exception", e.toString());
			}

		}
	}
	private Client cache;
	private QueuedMessage placeMemberMessage;
	private Member placedMember;
	/**
	 * 
	 */
	protected void placeCurrentMember() {
		// TODO Auto-generated method stub
		if (currentMember!=null) {
			logAction("placeMember.start", "memberID", currentMember.getID());
			Member m = new Member();
			m.setID(currentMember.getID());
			m.setCarried(false);
			Position pos = new Position();
			pos.setLatitude(placeLocation.getLatitude());
			pos.setLongitude(placeLocation.getLongitude());
			pos.setElevation(placeLocation.getAltitude());
			m.setPosition(pos);
			m.setZone(placeZone);
			placedMember = m;

			ClientState cs = BackgroundThread.getClientState(this);
			if (cs==null) {
				Log.e(TAG,"placeCurrentMember: ClientState null");
				return;
			}
			cache = cs.getCache();
			if (cache==null) {
				Log.e(TAG,"placeCurrentMember: ClientState null");
				return;
			}				
			try {
				// Note: this is (now) an async action
				placeMemberMessage = cache.queueMessage(cache.updateFactMessage(currentMember, m), this);
				Log.i(TAG,"Place member: "+m);

				showDialog(DialogId.PLACE_TO_SERVER.ordinal());
			}
			catch (Exception e) {
				Toast.makeText(this, "Sorry: "+e, Toast.LENGTH_LONG).show();
				Log.e(TAG, "Placing member", e);
				logAction("placeMember.error", "exception", e.toString());
			}

		}
	}
	@Override
	public void onMessageResponse(MessageStatusType status,
			String errorMessage, Object value) {
		Log.d(TAG,"onMessageResponse: status="+status+", error="+errorMessage+", value="+value);
		if (currentMember!=null && currentMember.getCarried())
			dismissDialog(DialogId.PLACE_TO_SERVER.ordinal());
		else
			dismissDialog(DialogId.CARRY_TO_SERVER.ordinal());

		if (status==MessageStatusType.OK) {
			logAction("carry/placeMember.ok");
			// fiddle with cache?
			ClientState cs = BackgroundThread.getClientState(this);
			if (currentMember!=null && placedMember!=null && cs!=null) {
				currentMember.setCarried(placedMember.getCarried());
				currentMember.setPosition(placedMember.getPosition());
				currentMember.setZone(placedMember.getZone());
				itemOverlay.clientStateChanged(cs);
			}
			//Toast.makeText(this, "Done: the map will update in a moment", Toast.LENGTH_LONG).show();
		}
		else {
			logAction("carry/placeMember.error", "message", errorMessage);
			Toast.makeText(this, "Sorry: "+errorMessage, Toast.LENGTH_LONG).show();
		}
		// tidy up
		placeMemberMessage = null;
		placedMember = null;
		cache = null;
	}

	/* (non-Javadoc)
	 * @see com.google.android.maps.ItemizedOverlay.OnFocusChangeListener#onFocusChanged(com.google.android.maps.ItemizedOverlay, com.google.android.maps.OverlayItem)
	 */
	@Override
	public void onFocusChanged(ItemizedOverlay overlay, OverlayItem newFocus) {
		// TODO Auto-generated method stub
		if (newFocus instanceof MyMapItem) {
			MyMapItem mmi = (MyMapItem)newFocus;
			Log.d(TAG,"Focus changed to "+mmi.getMember().getID());
			logState("focusChanged", "memberID", mmi.getMember().getID());

			currentMember = mmi.getMember();
			//updateAttributes(mmi.getMember());
			checkCarry();
		}
		else {
			//updateAttributes(null);
			logState("focusChanged", "newFocus", newFocus==null ? "null" : newFocus.toString());
		}
	}

	/**
	 * @param member
	 */
	private void updateAttributes2(Member member) {
		TextView tv;
		tv = (TextView)findViewById(R.id.ActionTextView);
		tv.setText(member==null ? "-" : ""+member.getAction());
		tv = (TextView)findViewById(R.id.BrainsTextView);
		tv.setText(member==null ? "-" : ""+member.getBrains());
		tv = (TextView)findViewById(R.id.HealthTextView);
		tv.setText(member==null ? "-" : ""+member.getHealth());
		tv = (TextView)findViewById(R.id.WealthTextView);
		tv.setText(member==null ? "-" : ""+member.getWealth());
	}
}
