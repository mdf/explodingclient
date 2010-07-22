package com.littlebighead.exploding;


import java.util.Collections;
import java.util.LinkedList;

import java.util.List;

import uk.ac.horizon.ug.exploding.client.BackgroundThread;
import uk.ac.horizon.ug.exploding.client.Client;
import uk.ac.horizon.ug.exploding.client.ClientState;
import uk.ac.horizon.ug.exploding.client.ClientStateListener;
import uk.ac.horizon.ug.exploding.client.GameMapActivity;
import uk.ac.horizon.ug.exploding.client.R;
import uk.ac.horizon.ug.exploding.client.logging.LoggingActivity;
import uk.ac.horizon.ug.exploding.client.model.Member;
import uk.ac.horizon.ug.exploding.client.model.Player;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import android.os.Bundle;
//import android.os.CountDownTimer;

//import android.content.Intent;

import android.view.View.OnClickListener;



public class CommunityView extends LoggingActivity implements ClientStateListener {	//implements OnClickListener {
	private static final String TAG = "CommunityView";

	public Dialog dialog;
	
	// BEGIN cmg
	private List<Member> members = new LinkedList<Member>();
	private GridView gridview;
	private ImageAdapter imageAdapter;
	// END cmg
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
//    	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    	
    	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	
    	setContentView(R.layout.community);  
    	
        gridview = (GridView) findViewById(R.id.gridview);
        imageAdapter = new ImageAdapter(this);
        gridview.setAdapter(imageAdapter);

        // CMG
        BackgroundThread.addClientStateListener(this, this, Member.class.getName());
        ClientState clientState = BackgroundThread.getClientState(this);
        clientStateChanged(clientState);
        GameMapActivity.setCurrentMember(null);
        // END CMG

        /*
        Context mContext = getApplicationContext();
        dialog = new Dialog(mContext);

        dialog.setContentView(R.layout.com_attrib_dialogue);
        dialog.setTitle("Custom Dialog"); 
        */ 
        
        /*
		Button button = (Button)dialog.findViewById(R.id.dismiss_member_props);
		//button.setOnClickListener(this);
		button.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	Log.i("click","clicked");
		    	finish();
//		    	finish();
		    }
		});       
    	*/


		/*
		button = (Button)findViewById(R.id.cancel_button);
		//button.setOnClickListener(this);
		button.setOnClickListener(new OnClickListener() {
		    public void onClick(View v) {
		    	finish();
		    }
		});
		*/

    }
    
    /* (non-Javadoc)
	 * @see uk.ac.horizon.ug.exploding.client.logging.LoggingActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		BackgroundThread.removeClientStateListener(this);
	}

	private class OnReadyListener implements CommunityPropsDialog.ReadyListener {
        @Override
        public void ready(String name) {
            //Toast.makeText(CommunityView.this, name, Toast.LENGTH_LONG).show();
        }
    }
    
    /*
    protected Dialog onPrepareDialog(int id) {
        Dialog dialog = null;
        switch(id) {
        case R.layout.com_attrib_dialogue:
            // do the work to define the pause Dialog
            break;
//        case DIALOG_GAMEOVER_ID:
            // do the work to define the game over Dialog
//            break;
        default:
            dialog = null;
        }
        return dialog;
    }
    
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch(id) {
        case R.layout.com_attrib_dialogue:
            // do the work to define the pause Dialog
        	//dialog = 
            break;
//        case DIALOG_GAMEOVER_ID:
            // do the work to define the game over Dialog
//            break;
        default:
            dialog = null;
        }
        return dialog;
    }
    */
    


    public class ImageAdapter extends BaseAdapter {	//implements onClickListener {
        private Context mContext;
        
        //private Dialog dialog;

        public ImageAdapter(Context c) {
            mContext = c;
        }

        public int getCount() {
            return members.size();
        }

        public Object getItem(int position) {
            return members.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        // create a new ImageView for each item referenced by the Adapter
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {  // if it's not recycled, initialize some attributes
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(120, 120));
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setPadding(2,2,2,2);//(8, 8, 8, 8);
 //               imageView.setOnClickListener(this);
                /*
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
//                      Log.d("onClick","position ["+position+"]");
                    	Log.d("onClick","position ["+position+"]");
                    	//showDialog(R.layout.com_attrib_dialogue);
                    	 CommunityView cv = (CommunityView) mContext;
                    	 cv.dialog.show();
                    }

                 });
                 */
                
        		imageView.setOnClickListener(new OnClickListener() {
        		    public void onClick(View v) {
        		        
        		        CommunityPropsDialog myDialog = new CommunityPropsDialog(mContext, members.get(position), new OnReadyListener());
        		        myDialog.show();
        		        
        		        /*
        				Intent myIntent = new Intent();
        				myIntent.setClassName("com.littlebighead.exploding", "com.littlebighead.exploding.CommunityMemberPropsView");
        				startActivity(myIntent);
        				*/
        				
        			}
        		});                
                
            } else {
            	imageView = (ImageView) convertView;
            }

            Member member = members.get(position);
            imageView.setImageDrawable(member.isSetCarried() && member.getCarried() ? MemberDrawableCache.getDrawableCarried(member) : MemberDrawableCache.getDrawable(member));
            //imageView.setImageResource(mThumbIds[0]);
            return imageView;
        }
        
        @Override
		public boolean hasStableIds() {
			return false;
		}

		public void onClick(View view) {
        	((Button) view).setText("*");
        	
        }

    }

    // CMG
    public static List<Member> getMyMembers(ClientState clientState) {
		List<Member> members = new LinkedList<Member>();
		if (clientState==null)
			return members;
		Client cache = clientState.getCache();
		if (cache!=null) {
			List<Object> ps = cache.getFacts(Player.class.getName());
			Player player = null;
			if (ps.size()>0) {
				player = (Player)ps.get(0);
				List<Object> ms = cache.getFacts(Member.class.getName());
				for (Object m : ms) {
					Member member  = (Member)m;
					if (player.getID().equals(member.getPlayerID()))
						members.add(member);
				}
			}
		}
		Collections.sort(members, new MemberComparator());
		return members;
    }
    static class MemberComparator implements java.util.Comparator<Member> {

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(Member m1, Member m2) {
			if (m1.getID()==null)
				return -1;
			if (m2.getID()==null)
				return 1;
			return m1.getID().compareTo(m2.getID());
		}
    	
    }
    
	/* (non-Javadoc)
	 * @see uk.ac.horizon.ug.exploding.client.ClientStateListener#clientStateChanged(uk.ac.horizon.ug.exploding.client.ClientState)
	 */
	@Override
	public void clientStateChanged(ClientState clientState) {
		members = getMyMembers(clientState);
		Log.d(TAG,"Found "+members.size()+" members");
		
		imageAdapter.notifyDataSetChanged();
		gridview.requestLayout();
	}
	// END CMG
}

