/**
 * Copyright 2010 The University of Nottingham
 * 
 * This file is part of explodingclient.
 *
 *  explodingclient is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  explodingclient is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with explodingclient.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package uk.ac.horizon.ug.exploding.client;

import java.util.LinkedList;
import java.util.List;

import uk.ac.horizon.ug.exploding.client.model.Position;
import uk.ac.horizon.ug.exploding.client.model.Zone;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Paint.Style;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/**
 * @author cmg
 *
 */
public class ZoneOverlay extends Overlay {

	private static final String TAG = "ZoneOverlay";
	private static final double ONE_MILLION = 1000000;
	private static final int[] ZONE_COLOR = new int [] { 0xff0000, 0xffff00, 0x00ff00, 0x00ffff, 0x0000ff, 0xff00ff };
	private static final int ZONE_ALPHA = 40;
	private static final int BORDER_ALPHA = 150;
	private List<Zone> zones = new LinkedList<Zone>();
	
	private boolean visible;
	private boolean handleTouch;
	private boolean fakeLocation;
	
	public ZoneOverlay(List<Object> zones)
	{
		for (Object z : zones) {
			if (z instanceof Zone) {
				this.zones.add((Zone)z);
			}
			else
				Log.e(TAG,"Ignore non-zone "+z);
		}
	}
	
	public synchronized void setVisible(boolean visible, boolean handleTouch, boolean fakeLocation) {
		this.visible = visible;
		this.handleTouch = handleTouch;
		this.fakeLocation = fakeLocation;
		Log.d(TAG,"setVisible("+visible+","+handleTouch+")");
	}
	
	/* (non-Javadoc)
	 * @see com.google.android.maps.Overlay#draw(android.graphics.Canvas, com.google.android.maps.MapView, boolean)
	 */
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		if (shadow || !visible)
			return;
		Projection proj = mapView.getProjection();
		Point point = new Point();
		int zi = 0;
		for (Zone z : zones) {
			Position ps [] = z.getCoordinates();
			GeoPoint gp1 = null;
			Path path = new Path();
			for (int i=0; i<ps.length; i++) {
				if (ps[i]!=null && ps[i].getLatitude()!=null && ps[i].getLongitude()!=null) {
					GeoPoint gp = new GeoPoint(degreesToMicros(ps[i].getLatitude()), degreesToMicros(ps[i].getLongitude()));
					proj.toPixels(gp, point);
					if (gp1==null) {
						gp1 = gp;
						path.moveTo(point.x, point.y);
					}
					else
						path.lineTo(point.x, point.y);
				}
			}
			if (gp1!=null) {
				proj.toPixels(gp1, point);
				path.lineTo(point.x, point.y);
				path.close();

				Paint paint = new Paint();
				if (ZoneService.isGameZone(z))
					// black for game area
					paint.setColor(0x000000);
				else
					paint.setColor(ZONE_COLOR[zi % ZONE_COLOR.length]);
				paint.setStyle(Style.FILL);
				paint.setAlpha(ZONE_ALPHA);
				canvas.drawPath(path, paint);
				
				paint.setStyle(Style.STROKE);
				paint.setAlpha(BORDER_ALPHA);
				canvas.drawPath(path, paint);
				
//				Log.d(TAG,"Drew zone "+z+" (end point "+gp1+" -> "+point+")");
			}
//			else
//				Log.d(TAG,"No point on zone "+z);
			zi++;
		}
	}

	public static int degreesToMicros(double degrees) {
		return (int)(ONE_MILLION*degrees);
	}

	/* (non-Javadoc)
	 * @see com.google.android.maps.Overlay#onTap(com.google.android.maps.GeoPoint, com.google.android.maps.MapView)
	 */
	@Override
	public boolean onTap(GeoPoint p, MapView mapView) {
		if (visible && (handleTouch || fakeLocation)) {
			double latitude = p.getLatitudeE6()*1.0/ONE_MILLION;
			double longitude = p.getLongitudeE6()*1.0/ONE_MILLION;
			if (fakeLocation) {
				Toast.makeText(mapView.getContext(), "Fake location "+longitude+","+latitude, Toast.LENGTH_SHORT).show();
				Log.d(TAG, "onTap: Fake location "+longitude+","+latitude);
				LocationUtils.fakeLocation(mapView.getContext(), latitude, longitude);
			} else {
				Zone zone = ZoneService.getZone(mapView.getContext(), latitude, longitude);
				boolean outsideGameArea = ZoneService.outsideGameArea(mapView.getContext(), latitude, longitude);
				Toast.makeText(mapView.getContext(), "Zone: "+(zone==null ? "none" : zone.getName())+(outsideGameArea ? " (outside game area)" : " (inside game area)"), Toast.LENGTH_SHORT).show();
				Log.d(TAG,"onTap: Zone: "+(zone==null ? "none" : zone.getName())+(outsideGameArea ? " (outside game area)" : " (inside game area)"));		
			}
		}
		return super.onTap(p, mapView);
	}
	
	
}
