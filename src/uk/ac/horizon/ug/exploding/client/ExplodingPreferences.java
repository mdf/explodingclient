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

import uk.ac.horizon.ug.exploding.client.logging.ActivityLogger;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * @author cmg
 *
 */
public class ExplodingPreferences extends PreferenceActivity {

	private ActivityLogger logger = new ActivityLogger(this);

	/**
	 * 
	 */
	public ExplodingPreferences() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		logger.logOnCreate(this, savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		// force initialisation of device ID
		getDeviceId(this);
	}
	/** get default device id (imei) */
	public static String getDefaultDeviceId(Context context) {
		TelephonyManager mTelephonyMgr = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		String imei = mTelephonyMgr.getDeviceId(); // Requires READ_PHONE_STATE  
		return imei;
	}
	public static final String CLIENT_ID = "clientId";
	/** get device id */
	public static String getDeviceId(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (!preferences.contains(CLIENT_ID) || preferences.getString(CLIENT_ID, "").length()==0) {
			preferences.edit().putString(CLIENT_ID, getDefaultDeviceId(context)).commit();
		}
		return preferences.getString(CLIENT_ID, null);
	}
	public static final String PLAYER_NAME = "playerName";
	/** get device id */
	public static String getPlayerName(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		return preferences.getString(PLAYER_NAME, "");
	}
	public static final String GAME_TAG = "gameTag";
	/** get game tag */
	public static String getGameTag(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		String tag = preferences.getString(GAME_TAG, "");
		if (tag==null || tag.length()==0)
			return null;
		return tag;
	}
	public static final String HTTP_TIMEOUT = "httpTimeout";
	private static final String TAG = "ExplodingPreferences";
	public static final String SHOW_ZONES_ON_MAP = "showZonesOnMap";
	public static final String POLL_INTERVAL = "pollInterval";
	public static final String POLL_TO_FOLLOW = "pollToFollow";
	/** get device id */
	public static int getHttpTimeout(Context context) {
		try {
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			return Integer.parseInt(preferences.getString(HTTP_TIMEOUT, "0"));
		}
		catch (Exception e) {
			Log.e(TAG,"Getting http timeout", e);
		}			
		return 30000; // default?!
	}

	@Override
	protected void onPause() {
		logger.logOnPause();
		super.onPause();
		int timeout = getHttpTimeout(this);
		BackgroundThread.setHttpTimeout(timeout);
	}

	@Override
	protected void onResume() {
		logger.logOnResume();
		super.onResume();
	}

	/* (non-Javadoc)
	 * @see android.preference.PreferenceActivity#onStop()
	 */
	@Override
	protected void onStop() {
		super.onStop();
		BackgroundThread.setShouldBePaused(true);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();
		BackgroundThread.setShouldBePaused(false);
	}
	
}
