package uk.ac.horizon.ug.exploding.client;

import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

/** just a test for now */
class MyMapItem extends OverlayItem {

	public MyMapItem(GeoPoint point, String title, String snippet) {
		super(point, title, snippet);
		// TODO Auto-generated constructor stub
	}

	static final int selected[] = new int[] { android.R.attr.state_selected };
	static final int unselected[] = new int[] {};
	private static final String TAG = "MyMapItem";

	@Override
	public Drawable getMarker(int stateBitset) {
		// TODO Auto-generated method stub
		Log.d(TAG,"getmarker("+stateBitset+"), "+((stateBitset & OverlayItem.ITEM_STATE_FOCUSED_MASK)!=0 ? "selected" : "unselected"));
		this.mMarker.setState((stateBitset & OverlayItem.ITEM_STATE_FOCUSED_MASK)!=0 ? selected : unselected);
		return this.mMarker;
		//return super.getMarker(stateBitset);
	}
	
}