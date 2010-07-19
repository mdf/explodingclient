package uk.ac.horizon.ug.exploding.client;

import uk.ac.horizon.ug.exploding.client.model.Member;
import uk.ac.horizon.ug.exploding.client.model.Player;
import uk.ac.horizon.ug.exploding.client.model.Position;

import java.util.LinkedList;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
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
		return members.indexOf(member);
	}
	/* (non-Javadoc)
	 * @see uk.ac.horizon.ug.exploding.client.ClientStateListener#clientStateChanged(uk.ac.horizon.ug.exploding.client.ClientState)
	 */
	@Override
	public synchronized void clientStateChanged(final ClientState clientState) {
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
			
 				List<Object> allMembers = clientState.getCache().getFacts(Member.class.getName());
 				members = new LinkedList<Object>();
 				for (Object m : allMembers) {
 					Member member = (Member)m;
 					if (player.getID().equals(member.getPlayerID()) && !member.getCarried())
 						members.add(member);
 				}
			}
		}
		Log.d(TAG,"Members changed: "+size()+" found");
		setLastFocusedIndex(-1);
		setFocus(null);
		populate();		
	}
	@Override
	protected synchronized boolean onTap(int index) {
		Log.d(TAG,"onTap("+index+") -> focus");
		setFocus(null);
		if (index<size())
			setFocus(getItem(index));
		return true;
		//return super.onTap(index);
	}
	
}