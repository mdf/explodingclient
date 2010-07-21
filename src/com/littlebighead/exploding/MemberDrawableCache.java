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
package com.littlebighead.exploding;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import uk.ac.horizon.ug.exploding.client.BackgroundThread;
import uk.ac.horizon.ug.exploding.client.model.Member;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.Log;

/** Cache of Drawables of members.
 * 
 * @author cmg
 *
 */
public class MemberDrawableCache {
	
	private static final int WIDTH = 100;
	private static final int HEIGHT = 100;

	/** get for Member */
	public static synchronized Drawable getDrawable(Member member) {
		return getDrawableInfo(member).drawable;
	}
	/** get for Member */
	public static synchronized Drawable getDrawableCarried(Member member) {
		return getDrawableInfo(member).drawableCarried;
	}
	/** get for Member */
	public static synchronized Drawable getDrawableMap(Member member) {
		return getDrawableInfo(member).drawableMap;
	}
	/** get for Member */
	public static synchronized Bitmap getBitmap(Member member) {
		return getDrawableInfo(member).bitmap;
	}
	private static synchronized DrawableInfo getDrawableInfo(Member member) {
		if (member.getLimbData()==null) {
			Log.e(TAG,"Member with null limbData: "+member.getID());
		}
		MemberInfo mi = new MemberInfo(member.getLimbData(), PlayerColours.values()[member.isSetColourRef() ? member.getColourRef() % PlayerColours.values().length : 0].color(), member.getParentMemberID()==null);
		DrawableInfo di = cache.get(mi);
		if (di==null) {
			di = createDrawableInfo(mi);
			// TODO state
			cache.put(mi, di);
		}
		return di;
	}

	static final int selected[] = new int[] { android.R.attr.state_selected };
	static final int unselected[] = new int[] { };
	protected static final String TAG = "MemberDrawableCache";
	private static final int AVATAR_RADIUS = 8;
	private static final int AVATAR_RADIUS2 = 4;
	static Drawable selectedDrawable = null;
	/**
	 * @param mi
	 * @return
	 */
	private static DrawableInfo createDrawableInfo(MemberInfo mi) {
		DrawableInfo di = new DrawableInfo();

		Body body = new Body(mi.color);
		if (mi.limbInfo!=null)
			body.setLimbInfo(mi.limbInfo);
		float minY = body.getMinY();
		float radius = body.getRadius();

		// community
		Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		//canvas.drawARGB(0, 0, 0, 0);
		canvas.drawARGB(0xff, 0xff, 0xff, 0xff);
		canvas.save();
		canvas.translate(WIDTH/2, 5+HEIGHT/2*(-minY)/radius);
		canvas.scale(1.0f*(WIDTH-10)/(2*radius), 1.0f*(HEIGHT-10)/(2*radius));
		body.draw(canvas, false);
		canvas.restore();
		if (mi.avatar) {
			Paint sp = new Paint();
			sp.setColor(0xff000000);
			sp.setStyle(Paint.Style.FILL);
			//sp.setStrokeWidth(3);
			canvas.drawOval(new RectF(WIDTH/2-AVATAR_RADIUS, HEIGHT/2-AVATAR_RADIUS,WIDTH/2+AVATAR_RADIUS,HEIGHT/2+AVATAR_RADIUS), sp);
			sp.setColor(0xffffffff);
			canvas.drawOval(new RectF(WIDTH/2-AVATAR_RADIUS2, HEIGHT/2-AVATAR_RADIUS2,WIDTH/2+AVATAR_RADIUS2,HEIGHT/2+AVATAR_RADIUS2), sp);
		}
		Drawable d = new BitmapDrawable(bitmap);
		d.setBounds(0, 0, WIDTH, HEIGHT);
		di.drawable = d;

		// community carried
		Bitmap bitmapCarried = Bitmap.createBitmap(bitmap);		
		canvas = new Canvas(bitmapCarried);
		Paint tp = new Paint();
		tp.setColor(0xff000000);
		tp.setTextSize(WIDTH/5);
		tp.setFakeBoldText(true);
		canvas.drawText("PLACE ME", 3, 3*HEIGHT/4, tp);
		di.drawableCarried = new BitmapDrawable(bitmapCarried);
		di.drawableCarried.setBounds(0, 0, WIDTH, HEIGHT);

		// map
		di.bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
		canvas = new Canvas(di.bitmap);
		canvas.drawARGB(0, 0, 0, 0);
		canvas.save();
		//canvas.drawARGB(0xff, 0xff, 0xff, 0xff);
		canvas.translate(WIDTH/2, 5+HEIGHT/2*(-minY)/radius);
//		canvas.scale(1.0f*(WIDTH-10)/(2*radius), 1.0f*(HEIGHT-10)/(2*radius));
		canvas.scale(1.0f*WIDTH/(2*radius), 1.0f*HEIGHT/(2*radius));
		body.drawShadow(canvas, 0);//5*2*radius/(HEIGHT-10));
		canvas.scale(1.0f*(WIDTH-10)/(WIDTH), 1.0f*(HEIGHT-10)/HEIGHT);
		body.draw(canvas, false);
		canvas.restore();
		if (mi.avatar) {
			Paint sp = new Paint();
			sp.setColor(0xff000000);
			sp.setStyle(Paint.Style.FILL);
			//sp.setStrokeWidth(3);
			canvas.drawOval(new RectF(WIDTH/2-AVATAR_RADIUS, HEIGHT/2-AVATAR_RADIUS,WIDTH/2+AVATAR_RADIUS,HEIGHT/2+AVATAR_RADIUS), sp);
			sp.setColor(0xffffffff);
			canvas.drawOval(new RectF(WIDTH/2-AVATAR_RADIUS2, HEIGHT/2-AVATAR_RADIUS2,WIDTH/2+AVATAR_RADIUS2,HEIGHT/2+AVATAR_RADIUS2), sp);
		}

		StateListDrawable sld = new StateListDrawable() {
			@Override
			public boolean setState(int[] stateSet) {
				// debug...
				//Log.d(TAG,"setState: "+Arrays.toString(stateSet));
				return super.setState(stateSet);
			}
			
		};
//		sld.setBounds(-WIDTH/2, -HEIGHT/2, WIDTH/2, HEIGHT/2);
		sld.setBounds(-WIDTH/2, -HEIGHT, WIDTH/2, 0);
		di.drawableMap = sld;
		Bitmap bitmapSelected = Bitmap.createBitmap(di.bitmap);
		canvas = new Canvas(bitmapSelected);
		Paint sp = new Paint();
		sp.setColor(0xff0080ff);
		sp.setStyle(Paint.Style.STROKE);
		sp.setStrokeWidth(3);
		canvas.drawOval(new RectF(1f,1f,WIDTH-2,HEIGHT-2), sp);
		sld.addState(selected, new BitmapDrawable(bitmapSelected));
		sld.addState(unselected, new BitmapDrawable(di.bitmap));
		
		return di;
	}

	/** cache */
	private static Map<MemberInfo, DrawableInfo> cache = new HashMap<MemberInfo, DrawableInfo>();
	
	/** info maintained for member */
	static class DrawableInfo {
		Bitmap bitmap;
		Drawable drawable;
		Drawable drawableCarried;
		Drawable drawableMap;
	}
	
	/** info needed for a Member view - used as hashmap key */
	static class MemberInfo {
		String limbInfo;
		int color;
		boolean avatar;
		/**
		 * @param limbInfo
		 * @param color
		 */
		public MemberInfo(String limbInfo, int color, boolean avatar) {
			super();
			this.limbInfo = limbInfo;
			this.color = color;
			this.avatar = avatar;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (avatar ? 1231 : 1237);
			result = prime * result + color;
			result = prime * result
					+ ((limbInfo == null) ? 0 : limbInfo.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MemberInfo other = (MemberInfo) obj;
			if (avatar != other.avatar)
				return false;
			if (color != other.color)
				return false;
			if (limbInfo == null) {
				if (other.limbInfo != null)
					return false;
			} else if (!limbInfo.equals(other.limbInfo))
				return false;
			return true;
		}
		
	}
}
