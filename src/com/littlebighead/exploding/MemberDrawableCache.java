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
	
	private static final int WIDTH = 72;
	private static final int HEIGHT = 72;

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
		MemberInfo mi = new MemberInfo(member.getLimbData(), PlayerColours.values()[member.isSetColourRef() ? member.getColourRef() % PlayerColours.values().length : 0].color());
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
	static Drawable selectedDrawable = null;
	/**
	 * @param mi
	 * @return
	 */
	private static DrawableInfo createDrawableInfo(MemberInfo mi) {
		DrawableInfo di = new DrawableInfo();
		// TODO Auto-generated method stub
		di.bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(di.bitmap);
		canvas.drawARGB(0, 0, 0, 0);

		Body body = new Body(mi.color);
		body.setLimbInfo(mi.limbInfo);
		canvas.translate(WIDTH/2, HEIGHT/2);
		canvas.scale(1.0f*WIDTH/500, 1.0f*HEIGHT/500);
		body.draw(canvas, false);
		Drawable d = new BitmapDrawable(di.bitmap);
		d.setBounds(0, 0, WIDTH, HEIGHT);
		StateListDrawable sld = new StateListDrawable() {
			@Override
			public boolean setState(int[] stateSet) {
				// debug...
				//Log.d(TAG,"setState: "+Arrays.toString(stateSet));
				return super.setState(stateSet);
			}
			
		};
		di.drawable = d;
		sld.setBounds(-WIDTH/2, -HEIGHT/2, WIDTH/2, HEIGHT/2);
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
		
		Bitmap bitmapCarried = Bitmap.createBitmap(di.bitmap);		
		canvas = new Canvas(bitmapCarried);
		Paint tp = new Paint();
		tp.setColor(0xffffffff);
		tp.setTextSize(WIDTH/4);
		canvas.drawText("UNPLACED", 0, HEIGHT/2, tp);
		di.drawableCarried = new BitmapDrawable(bitmapCarried);
		di.drawableCarried.setBounds(0, 0, WIDTH, HEIGHT);
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
		/**
		 * @param limbInfo
		 * @param color
		 */
		public MemberInfo(String limbInfo, int color) {
			super();
			this.limbInfo = limbInfo;
			this.color = color;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
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
