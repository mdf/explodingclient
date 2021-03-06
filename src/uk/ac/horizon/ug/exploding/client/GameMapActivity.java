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
import com.littlebighead.exploding.CommunityPropsDialog;
import com.littlebighead.exploding.CommunityView;
import com.littlebighead.exploding.Limb;
import com.littlebighead.exploding.MemberDrawableCache;
import com.littlebighead.exploding.TimeEventDialog;
import com.littlebighead.exploding.CommunityPropsDialog.ReadyListener;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.json.JSONStringer;

import uk.ac.horizon.ug.exploding.client.Client.QueuedMessage;
import uk.ac.horizon.ug.exploding.client.model.Game;

import uk.ac.horizon.ug.exploding.client.logging.ActivityLogger;
import uk.ac.horizon.ug.exploding.client.logging.LoggingActivity;
import uk.ac.horizon.ug.exploding.client.logging.LoggingUtils;

import uk.ac.horizon.ug.exploding.client.model.GameConfig;
import uk.ac.horizon.ug.exploding.client.model.Member;
import uk.ac.horizon.ug.exploding.client.model.Message;
import uk.ac.horizon.ug.exploding.client.model.Player;
import uk.ac.horizon.ug.exploding.client.model.Position;
import uk.ac.horizon.ug.exploding.client.model.Zone;

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
import android.content.Context;
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
import android.os.Handler;
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
	private TextView contentTextView;
	private ZoneOverlay zoneOverlay;
	
	static enum DialogId { PLACE, PLACE_TO_SERVER, CARRY, CARRY_TO_SERVER /*, NEW_CONTENT*/ };
	private ProgressDialog placeToServerPd;
	private ProgressDialog carryToServerPd;
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
	
	// sort messages by game time
	static class MessageComparator implements Comparator<Message> {

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(Message m1, Message m2) {
			if (m1.getGameTime()==null)
				return m2.getGameTime()==null ? 0 : 1;
			return m1.getGameTime().compareTo(m2.getGameTime());
		}
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		logger.logOnCreate(this, savedInstanceState);
    	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		try {
			MemberDrawableCache.init(this);
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
			contentTextView = (TextView)findViewById(R.id.map_message_text_view);
			Log.d(TAG,"initialise contentTextView="+contentTextView+" in "+this);
			contentTextView.setOnClickListener(new OnClickListener(){
					public void onClick(View v){
						if (currentMessage!=null) {
							Intent myIntent = new Intent();
							myIntent.setClassName("uk.ac.horizon.ug.exploding.client", "com.littlebighead.exploding.TimeEventDialog");

							if (currentMessage.getYear()!=null)
								myIntent.putExtra("year", currentMessage.getYear());
							if (currentMessage.getTitle()!=null)
								myIntent.putExtra("name", currentMessage.getTitle());
							if (currentMessage.getDescription()!=null)
								myIntent.putExtra("desc", currentMessage.getDescription());

							List<Message> messages = new LinkedList<Message>();
							messages.addAll(currentMessages);
							Collections.sort(messages, new MessageComparator());
							TimeEventDialog.setMessages( messages );
							int mi = messages.indexOf(currentMessage);
							myIntent.putExtra("messageIndex", mi);
							
							startActivity(myIntent);
						}
					}
				});
			if (currentMessage!=null && currentMessage.getTitle() != null){
				// show last message
				contentTextView.setText(currentMessage.getTitle() + " [more..]");
				contentTextView.setVisibility(TextView.VISIBLE);
				contentTextView.bringToFront();
				contentTextView.invalidate();
			}
			
			mapView.setBuiltInZoomControls(true);
			myLocationOverlay = new MyLocationOverlay(this, mapView);
			myLocationOverlay.runOnFirstFix(new Runnable() {
				public void run() {
					mHandler.post(new Runnable() {
						public void run() {
							centreOnMyLocation();
							Log.d(TAG,"onFirstFix(), currentLocation="+LocationUtils.getCurrentLocation(GameMapActivity.this));
							if (currentMember!=null) {
								if (currentMember.isSetCarried() && currentMember.getCarried()) {					
									askToPlace();
								} 
							}
						}
					});
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

			Client cache = clientState.getCache();
			if (cache!=null) {
				List<Object> zones = cache.getFacts(Zone.class.getName()); 	
				zoneOverlay = new ZoneOverlay(zones);
				mapView.getOverlays().add(zoneOverlay);
				Log.d(TAG,"Added ZoneOverlay with "+zones.size()+" zones");
			}
			else
				Log.e(TAG,"Could not create zone overlay - no client cache");
			
			mapView.getOverlays().add(itemOverlay);
			
		}
		catch (Exception e) {
			Log.e(TAG, "Error loading map view", e);
		}
		Set<String> types = new HashSet<String>();
		types.add(Message.class.getName());
		types.add(Game.class.getName());
		types.add(Member.class.getName());
		types.add(Player.class.getName());
		BackgroundThread.addClientStateListener(this, this, ClientState.Part.ZONE.flag(), types);
		ClientState clientState = BackgroundThread.getClientState(this);
		clientStateChanged(clientState, true);
		//centreOnMyLocation();
	}
	
	
	/* (non-Javadoc)
	 * @see com.google.android.maps.MapActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		logger.logOnDestroy();
		BackgroundThread.removeClientStateListener(this);
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
			zoneChanged(clientState.getZoneID(), clientState.isZoneChanged());
		if (isInitial || clientState.getChangedTypes().contains(Message.class.getName()))
			handleMessages(clientState);
		if (isInitial || clientState.getChangedTypes().contains(Game.class.getName()))
			checkYear(clientState);
		if (isInitial || clientState.getChangedTypes().contains(Member.class.getName()))
			updateMembers(clientState);
		if (isInitial || clientState.getChangedTypes().contains(Player.class.getName()) || clientState.getChangedTypes().contains(Game.class.getName()))
			updateButtons(clientState);
		if (gameEnded()) {
			logState("gameEnded");
			Toast.makeText(this, "The game is over!", Toast.LENGTH_LONG).show();
			finish();
		}
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
		boolean active = gameActive();
		button = (Button)findViewById(R.id.map_story_button);
		button.setEnabled(active && canAuthor(false));
		button = (Button)findViewById(R.id.map_create_button);
		button.setEnabled(active && canCreateMember(false));
		button = (Button)findViewById(R.id.map_community_button);
		// dis/enable communities
		button.setEnabled(active && cache.getFirstFact(Member.class.getName())!=null);
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
		String currentMemberId = currentMember!=null ? currentMember.getID() : null;
		Member oldCurrentMember = currentMember;
		setCurrentMember(null);
		for (Member member : members) {
			// TODO update currentMember?!
			if (currentMemberId!=null && currentMemberId.equals(member.getID()))
			{
				currentMember = member;
			}
			if (member.isSetHealth())
				average.setHealth(average.getHealth()+member.getHealth());
			if (member.isSetWealth())
				average.setWealth(average.getWealth()+member.getWealth());
			if (member.isSetAction())
				average.setAction(average.getAction()+member.getAction());
			if (member.isSetBrains())
				average.setBrains(average.getBrains()+member.getBrains());
		}
		if (currentMember!=oldCurrentMember)
		{
			logState("currentMemberChanged", "currentMember", currentMember);
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
	protected void zoneChanged(String zoneID, boolean showToast) {
		//		logger.log("Zone", "zoneID", zoneID);
		Log.d(TAG, "Zone change to "+zoneID);
		// "game" zones
		if ("main".equals(zoneID) || zoneID.startsWith("~"))
			zoneID = null;
		if (zoneID!=null) {
			if (showToast) {
				Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
				if (vibrator!=null)
					vibrator.vibrate(ZONE_VIBRATE_MS);
				Toast.makeText(GameMapActivity.this, "You have now entered "+zoneID, Toast.LENGTH_SHORT).show();
			}
		}
		else 
			zoneID = "-";
		// BEGIN ROBIN
		TextView zoneTextView = (TextView)findViewById(R.id.ZoneTextView);
		zoneTextView.setText("You are in: "+zoneID);
		logState("updateZone", "zoneID", zoneID);
		// END ROBIN
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
					uk.ac.horizon.ug.exploding.client.model.Message.MSG_TIMELINE_CONTENT_GLOBAL.equals(message.getType()) ||
					uk.ac.horizon.ug.exploding.client.model.Message.MSG_PRIORITY_TIMELINE_CONTENT.equals(message.getType())
					) {
				// stash
				currentMessages.add(message);
				// Pick one...
				if (bestContentMessage==null)
					bestContentMessage = message;
				else if (uk.ac.horizon.ug.exploding.client.model.Message.MSG_PRIORITY_TIMELINE_CONTENT.equals(message.getType()))
					// priority > global event > non-global
					bestContentMessage = message;
				else if (uk.ac.horizon.ug.exploding.client.model.Message.MSG_TIMELINE_CONTENT_GLOBAL.equals(message.getType()) &&
						!uk.ac.horizon.ug.exploding.client.model.Message.MSG_PRIORITY_TIMELINE_CONTENT.equals(bestContentMessage.getType()))
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

			//TextView textview = (TextView)findViewById(R.id.ContentTextView);
			//if (currentMessage != null){
				
			if (currentMessage.getTitle() != null && contentTextView!=null){
				contentTextView.setText(currentMessage.getTitle() + " [more..]");
				contentTextView.setVisibility(TextView.VISIBLE);
				contentTextView.bringToFront();
				contentTextView.invalidate();
				Log.d(TAG,"Show message "+currentMessage.getTitle()+" in "+contentTextView+" in "+this+" (shown="+contentTextView.isShown()+")");
			}
			else 
				Log.e(TAG,"Problem showing content: "+currentMessage.getTitle()+" in "+contentTextView+" in "+this);
		
			//}
			
			/*Intent myIntent = new Intent();
			myIntent.setClassName("uk.ac.horizon.ug.exploding.client", "com.littlebighead.exploding.TimeEventSmallDialog");
			if (currentMessage.getYear()!=null)
				myIntent.putExtra("year", currentMessage.getYear());
				if (currentMessage.getTitle()!=null)
					myIntent.putExtra("name", currentMessage.getTitle());
				if (currentMessage.getDescription()!=null)
					myIntent.putExtra("desc", currentMessage.getDescription());

			startActivity(myIntent);*/
			
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
		centreOnMyLocation(true);
	}
	private void centreOnMyLocation(boolean checkZoom) {
		try {
			// ignore fake locations as they come from the map anyway!
			Location loc = LocationUtils.getCurrentLocation(this);
			if (loc!=null) {
				centreOn(loc.getLatitude(), loc.getLongitude(), checkZoom);
			}
			else
			{
				loc = ZoneService.getDefaultLocation(this);
				if (loc!=null)
					centreOn(loc.getLatitude(), loc.getLongitude(), checkZoom);
				if (checkZoom)
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
		centreOn(latitude, longitude, true);
	}
	private void centreOn(double latitude, double longitude, boolean checkZoom) {
		// TODO Auto-generated method stub
		MapView mapView = (MapView)findViewById(R.id.map_view);
		MapController controller = mapView.getController();
		int zoomLevel = mapView.getZoomLevel();
		// zoom Level 15 is about 1000m on a side
		if (checkZoom && zoomLevel < MIN_ZOOM_LEVEL)
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
		stopNagging();
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
		startNagging();
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		boolean showZonesOnMap = preferences.getBoolean("showZonesOnMap", false);
		boolean debugZonesOnMap = preferences.getBoolean("debugZonesOnMap", false);
		boolean fakeLocationOnMap = preferences.getBoolean("fakeLocationOnMap", false);
		zoneOverlay.setVisible(showZonesOnMap, debugZonesOnMap, fakeLocationOnMap);
		
//		myLocationOverlay.enableCompass();
		myLocationOverlay.enableMyLocation();
		//		LocationUtils.registerOnThread(this, this, null);
		itemOverlay.setFocus(null);
		//updateAttributes(currentMember);
		boolean centre = true;
		if (currentMember!=null) {
			if (currentMember.isSetCarried() && currentMember.getCarried()) {

				centreOnMyLocation();
				centre = false;
				askToPlace();
			} 
			else {
				if (currentMember.isSetPosition()) {
					Position p = currentMember.getPosition();
					if (p.isSetLatitude() && p.isSetLongitude()) {
						centreOn(p.getLatitude(), p.getLongitude());
						centre = false;
					}
				}				
				int pos = itemOverlay.indexOf(currentMember);
				if (pos>=0) {
					//itemOverlay.setFocus(itemOverlay.getItem(pos));
					Log.d(TAG,"Set focus to item "+pos);
				}
				else
					Log.d(TAG,"Could not find member in overlay: "+currentMember);
				checkCarry();
			}
		}
		if (centre)
			centreOnMyLocation();

	}		
	private void askToPlace() {
		if (currentMember==null || !currentMember.isSetCarried() || !currentMember.getCarried())
		{
			Log.e(TAG,"askToPlace called with null/uncarried member: "+currentMember);
			return;
		}
		if (!gameActive())
		{
			Log.e(TAG,"askToPlace ignored for inactive game");
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
		if (currentMember.getParentMemberID()==null) {
			Toast.makeText(this, "Your first community member will always follow you", Toast.LENGTH_SHORT).show();
			return false;
		}
		ClientState cs = BackgroundThread.getClientState(this);
		if (cs==null)
			return false;
		if (!gameActive())
			return false;
		if (LocationUtils.getCurrentLocation(this)==null) {
			Toast.makeText(this, "You cannot pick up this community member as your current location is not known", Toast.LENGTH_SHORT).show();
			return false;
		}
		Location loc = cs.getLastLocation();
		if (loc==null) {
			Toast.makeText(this, "You cannot pick up this community member as your current location is not known", Toast.LENGTH_SHORT).show();
			return false;
		}
		Position pos = currentMember.getPosition();
		if (pos==null)
			return false;
		Location l2 = new Location("gps");
		l2.setLatitude(pos.getLatitude());
		l2.setLongitude(pos.getLongitude());
		l2.setAltitude(pos.getElevation());
		double dist = loc.distanceTo(l2);
		if (dist>CARRY_DISTANCE_M) {
			Toast.makeText(this, "You are too far away to move this community member", Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}
	private static double CARRY_DISTANCE_M = 40;
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
				Toast.makeText(this, "Please wait until your new community member arrives", Toast.LENGTH_SHORT).show();

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
	private Handler mHandler = new Handler();
	
	/** game ending */
	private boolean gameActive() {
		ClientState cs = BackgroundThread.getClientState(this);
		if (cs==null) {
			Log.e(TAG,"gameActive() null ClientState");
			return true;
		}
		return cs.getGameStatus()==GameStatus.ACTIVE;
	}
	/** game ending */
	private boolean gameEnded() {
		ClientState cs = BackgroundThread.getClientState(this);
		if (cs==null) {
			Log.e(TAG,"gameEnded() null ClientState");
			return true;
		}
		return cs.getGameStatus()==GameStatus.ENDED;
	}

	private static int NAG_INTERVAL_MS = 15000;
	private static int NAG_VIBRATE_MS = 500;
	private static String END_GAME_MESSAGE = "The game is now over please return to the starting location";
	private static String OUTSIDE_PLAYAREA_MESSAGE = "You are no longer in the game area, please walk back towards the starting location";
	private Runnable nagTimerTask = new Runnable() {
		@Override
		public void run() {
			boolean vibrate = false;
			GameConfig config = getGameConfig(GameMapActivity.this);
			if (!gameActive()) {
				logState("nag gameEnding");
				String message = END_GAME_MESSAGE;
				if (config!=null && config.isSetClientMessageEndGame())
					message = config.getClientMessageEndGame();
				Toast.makeText(GameMapActivity.this, message, Toast.LENGTH_LONG).show();
				vibrate = true;
				centreOnMyLocation(false);
			}
			else {
				Location loc = LocationUtils.getCurrentLocation(GameMapActivity.this);
				if (loc!=null && ZoneService.outsideGameArea(GameMapActivity.this, loc.getLatitude(), loc.getLongitude())) {
					logState("nag outOfGameZone");
					String message = OUTSIDE_PLAYAREA_MESSAGE;
					if (config!=null && config.isSetClientMessageOutOfBounds())
						message = config.getClientMessageOutOfBounds();
					Toast.makeText(GameMapActivity.this, message, Toast.LENGTH_LONG).show();
					vibrate = true;
				}
			}
			if (vibrate) {
				Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
				if (vibrator!=null)
					vibrator.vibrate(NAG_VIBRATE_MS);
			}
			mHandler.postDelayed(this, NAG_INTERVAL_MS);
		}
	};
	public static GameConfig getGameConfig(Context context) {
		ClientState cs = BackgroundThread.getClientState(context);
		if (cs==null) {
			Log.e(TAG,"getGameConfig: ClientState null");
			return null;
		}
		Client cache = cs.getCache();
		if (cache==null) {
			Log.e(TAG,"getGameConfig: cache null");
			return null;
		}				
		GameConfig config = (GameConfig)cache.getFirstFact(GameConfig.class.getName());
		return config;
	}
	private static int NAG_DELAY_MS = 2000;
	private void startNagging() {
		mHandler.removeCallbacks(nagTimerTask);
		mHandler.postDelayed(nagTimerTask, NAG_DELAY_MS);
	}
	private void stopNagging() {
		mHandler.removeCallbacks(nagTimerTask);		
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
				}
			});
			placeToServerPd = creatingPd;
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
				}
			});
			carryToServerPd = creatingPd;
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
	private static Message currentMessage;
	private static List<Message> currentMessages = new LinkedList<Message>();
	/** reset - from HomeActivity? */
	public static void reset() {
		currentMessage = null;
		currentMessages = new LinkedList<Message>();
	}
	/** reset - from HomeActivity? */
	public static void reset(Player p) {
		Message cm = currentMessage;
		if (cm!=null && p.getID()!=null && !p.getID().equals(cm.getPlayerID()))
			reset();
	}
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
		else
			Log.d(TAG,"carryCurrentMember: currentMember null");
	}
	private Client cache;
	private QueuedMessage placeMemberMessage;
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
		if (carryToServerPd!=null && carryToServerPd.isShowing())
			dismissDialog(DialogId.CARRY_TO_SERVER.ordinal());
		if (placeToServerPd!=null && placeToServerPd.isShowing())
			dismissDialog(DialogId.PLACE_TO_SERVER.ordinal());

		if (status==MessageStatusType.OK) {
			logAction("carry/placeMember.ok");
			// fiddle with cache? NO
			//Toast.makeText(this, "Done: the map will update in a moment", Toast.LENGTH_LONG).show();
		}
		else {
			logAction("carry/placeMember.error", "message", errorMessage);
			Toast.makeText(this, "Sorry: "+errorMessage, Toast.LENGTH_LONG).show();
		}
		// tidy up
		placeMemberMessage = null;
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
			Player player = getPlayer();
			if (player==null) {
				Log.e(TAG,"onFocusChanged player=null");
				return;
			}
			if (!mmi.getMember().getPlayerID().equals(player.getID()))
			{
				Log.d(TAG,"Ignore other's member "+mmi.getMember().getID());
				return;
			}
			logState("focusChanged", "memberID", mmi.getMember().getID());

			currentMember = mmi.getMember();

			CommunityPropsDialog myDialog = new CommunityPropsDialog(this, currentMember, new ReadyListener() {
				@Override
				public void ready(String name) {
					// force check carry
					checkCarry();
				}				
			}, true);
	        myDialog.show();
			
			//updateAttributes(mmi.getMember());
			//checkCarry();
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
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		logger.logOnBackPressed();
	}
	@Override
	public void onOptionsMenuClosed(Menu menu) {
		super.onOptionsMenuClosed(menu);
		logger.logOnOptionsMenuClosed(menu);
	}
	@Override
	protected void onStart() {
		super.onStart();
		logger.logOnStart();
		BackgroundThread.setShouldBePaused(false);
	}
	@Override
	protected void onStop() {
		super.onStop();
		logger.logOnStop();
		BackgroundThread.setShouldBePaused(true);
	}	
}
