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

import uk.ac.horizon.ug.exploding.client.logging.LoggingActivity;

/** An Activity that when visible allows our background activities to run.
 * Typically all activities in our application!
 * 
 * @author cmg
 *
 */
public class ApplicationActivity extends LoggingActivity {

	/**
	 * 
	 */
	public ApplicationActivity() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see uk.ac.horizon.ug.exploding.client.logging.LoggingActivity#onPause()
	 */
	@Override
	protected void onStart() {
		super.onPause();
		BackgroundThread.setShouldBePaused(false);
	}

	/* (non-Javadoc)
	 * @see uk.ac.horizon.ug.exploding.client.logging.LoggingActivity#onResume()
	 */
	@Override
	protected void onStop() {
		super.onResume();
		BackgroundThread.setShouldBePaused(true);
	}

}
