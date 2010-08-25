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

import uk.ac.horizon.ug.exploding.client.R;
import uk.ac.horizon.ug.exploding.client.logging.LoggingActivity;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
//import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * @author cmg
 *
 */
public class TimeEventSmallDialog extends LoggingActivity {

	/* (non-Javadoc)
	 * @see android.app.Dialog#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.new_content_dialog);
		//setCancelable(true);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			setTitle(extras.getString("name"));			
		}		
		else
			setTitle("{Message...}");
//		setOnCancelListener(new OnCancelListener()  {				
//			@Override
//			public void onCancel(DialogInterface dialog) {
//				dialog.dismiss();
//			}
//		});
		Button ok = (Button)findViewById(R.id.new_content_dialog_ok_button);
		ok.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TimeEventSmallDialog.this.finish();//dismiss();
				Intent myIntent = new Intent();
				myIntent.setClassName("uk.ac.horizon.ug.exploding.client", "com.littlebighead.exploding.TimeEventDialog");
				Bundle extras = getIntent().getExtras();
				if (extras != null) {
					myIntent.putExtra("year", extras.getString("year"));
					myIntent.putExtra("name", extras.getString("name"));
					myIntent.putExtra("desc", extras.getString("desc"));
				}
				startActivity(myIntent);

			}
		});
		Button cancel = (Button)findViewById(R.id.new_content_dialog_cancel_button);
		cancel.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				TimeEventSmallDialog.this.finish();//cancel();
			}
		});

		super.onCreate(savedInstanceState);
	}

	private static final long NEW_CONTENT_TIME_MS = 6000;

	protected static final String TAG = "TimeEventSmallDialog";

	private CountDownTimer newContentTimer;
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		if (newContentTimer!=null)
			newContentTimer.cancel();
		newContentTimer = new CountDownTimer(NEW_CONTENT_TIME_MS, NEW_CONTENT_TIME_MS) {
			@Override
			public void onTick(long millisUntilFinished) {
			}
			@Override
			public void onFinish() {
				try {
					TimeEventSmallDialog.this.finish();
				}
				catch (Exception e) {
					Log.e(TAG,"Finish on timer");
				}
			}
		};
		newContentTimer.start();
		super.onResume();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		if (newContentTimer!=null)
			newContentTimer.cancel();
		super.onPause();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
		finish();
		super.onBackPressed();
	}

}
