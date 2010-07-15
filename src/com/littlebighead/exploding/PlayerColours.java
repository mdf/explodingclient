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

/**
 * @author cmg
 *
 */
public enum PlayerColours {

	Yellow(0xffFCC00), 
	Pink(0xffE11588), //?
	Orange(0xffF96432), 	
	OliveGreen(0xff949431),  
	LightBlue(0xff00A3E3), 
	LightGreen(0xffC5C531), 
	Purple(0xff993399), 
	DarkBlue(0xff333399), 
	Red(0xffFF0032), 
	Green(0xff329900); 
	
	private PlayerColours(int color) { this.color = color; }
	private int color;
	public int color() { return color; }
}
