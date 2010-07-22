package uk.ac.horizon.ug.exploding.client;

import uk.ac.horizon.ug.exploding.client.model.Member;
import uk.ac.horizon.ug.exploding.client.model.Player;
import uk.ac.horizon.ug.exploding.client.model.Position;

import java.util.LinkedList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.littlebighead.exploding.MemberDrawableCache;

/** just a test for now */
public class MyMapOverlay extends ItemizedOverlay<MyMapItem> implements ClientStateListener {
	private static final String TAG = "MyMapOverlay";
	private static final int MILLION = 1000000;
	//private Drawable defaultMarker;
	private List<Object> members;
	
	public MyMapOverlay(Drawable defaultMarker, ClientState clientState) {
		super(defaultMarker);
		//boundCenter(defaultMarker);
		clientStateChanged(clientState);
	}
	@Override
	protected synchronized MyMapItem createItem(int i) {
		//Log.d(TAG,"CreateItem("+i+"), drawable="+defaultMarker);
		Member member = (Member)members.get(i);
		Position pos = member.getPosition();
		if (pos==null) {
			Log.e(TAG,"Member "+member.getID()+" has null position");
			pos = new Position();
		}
		MyMapItem item = new MyMapItem(new GeoPoint((int)(pos.getLatitude()*MILLION),(int)(pos.getLongitude()*MILLION)), member.getName(), null, member);
		Drawable drawable = MemberDrawableCache.getDrawableMap(member);
		//boundCenter(drawable);
		//boundCenterBottom(drawable);
		item.setMarker(drawable);
		return item;
	}

	@Override
	public synchronized int size() {
		if (members==null)
			return 0;
		return members.size();
	}
	public synchronized int indexOf(Member member) {
		if (members==null)
			return -1;
		return members.indexOf(member);
	}
	/* (non-Javadoc)
	 * @see uk.ac.horizon.ug.exploding.client.ClientStateListener#clientStateChanged(uk.ac.horizon.ug.exploding.client.ClientState)
	 */
	@Override
	public synchronized void clientStateChanged(final ClientState clientState) {
		List<Object> members = null;
		
		if (clientState==null  || clientState.getCache()==null) 
			members = null;
		else {	
			Client cache = clientState.getCache();
			List<Object> ps = cache.getFacts(Player.class.getName());
			Player player = null;
			if (ps.size()==0) 
				members = null;
			else {
 				player = (Player)ps.get(0);

 				// clone list
 				List<Object> allMembers = clientState.getCache().getFacts(Member.class.getName());
 				members = new LinkedList<Object>();
 				for (Object m : allMembers) {
 					Member member = (Member)m;
 					if (!member.getCarried())	
 						//player.getID().equals(member.getPlayerID()) && 
 						members.add(member);
 				}
			}
		}
		Log.d(TAG,"Members changed: "+size()+" found");
		setLastFocusedIndex(-1);
		setFocus(null);
		this.members = members;
		populate();		
	}
	private MyMapItem onTapItem = null;
	@Override
	protected synchronized boolean onTap(int index) {
		Log.d(TAG,"onTap("+index+") -> focus");
		setFocus(null);
		if (index<size()) {
			onTapItem = getItem(index);
			setFocus(onTapItem);
		}
		return true;
		//return super.onTap(index);
	}
	/* (non-Javadoc)
	 * @see com.google.android.maps.ItemizedOverlay#draw(android.graphics.Canvas, com.google.android.maps.MapView, boolean)
	 */
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		try {
			super.draw(canvas, mapView, shadow);
		}
		catch (Exception e) {
			Log.e(TAG,"in ItemizedOverlay.draw()", e);
		}
	}
	/* (non-Javadoc)
	 * @see com.google.android.maps.Overlay#draw(android.graphics.Canvas, com.google.android.maps.MapView, boolean, long)
	 */
	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
			long when) {
		try {
			return super.draw(canvas, mapView, shadow, when);
		}
		catch (Exception e) {
			Log.e(TAG,"in Overlay.draw()", e);
		}
		// no more animation...
		return false;
	}
	private MyMapItem hitMyAvatar = null;
	/* (non-Javadoc)
	 * @see com.google.android.maps.ItemizedOverlay#onTap(com.google.android.maps.GeoPoint, com.google.android.maps.MapView)
	 */
	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		hitMyAvatar = null;
		onTapItem = null;
		boolean hit = super.onTap(p, mapView);
		Log.d(TAG,"onTap: hitMyAvatar="+hitMyAvatar+", onTapItem="+onTapItem);
		if (onTapItem==null && hitMyAvatar!=null) {
			Log.d(TAG,"onTap: late hit my avatar");
			onTap(indexOf(hitMyAvatar.getMember()));
		}
		return hit;
	}
	/* (non-Javadoc)
	 * @see com.google.android.maps.ItemizedOverlay#hitTest(com.google.android.maps.OverlayItem, android.graphics.drawable.Drawable, int, int)
	 */
	@Override
	protected boolean hitTest(MyMapItem item, Drawable marker, int hitX,
			int hitY) {
		boolean hit = super.hitTest(item, marker, hitX, hitY);
		Member member = item.getMember();
		if (hit && member!=null && member.getParentMemberID()==null) {
			hitMyAvatar = item;
			Log.d(TAG,"Suppress hitTest of myAvatar");
			return false;
		}
		return hit;
	}
	
}