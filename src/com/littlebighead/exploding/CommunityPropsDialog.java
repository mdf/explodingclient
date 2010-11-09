package com.littlebighead.exploding;

import uk.ac.horizon.ug.exploding.client.BackgroundThread;
import uk.ac.horizon.ug.exploding.client.GameMapActivity;
import uk.ac.horizon.ug.exploding.client.R;
import uk.ac.horizon.ug.exploding.client.logging.ActivityLogger;
import uk.ac.horizon.ug.exploding.client.model.Member;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
//import android.widget.EditText;
import android.widget.TextView;

//public class CommunityPropsDialog {
public class CommunityPropsDialog extends Dialog {

	// TODO Logging
	
    public interface ReadyListener {
        public void ready(String name);
    }

    // CMG
    private Member member;
    private TextView etStatus;
    private TextView etHealth[] = new TextView[2];
    private TextView etWealth[] = new TextView[2];
    private TextView etAction[] = new TextView[2];
    private TextView etBrains[] = new TextView[2];
    private static final int MAX_WIDTH = 250;
    // end CMG
    
    private String name;
    private ReadyListener readyListener;
    TextView etName;
    private boolean justFinishOnEnd;
    
    private ActivityLogger logger = new ActivityLogger(this);
    
    public CommunityPropsDialog(Context context, Member member,
            ReadyListener readyListener) {
        super(context);
        this.name = member.getName();
        this.member = member;
        this.readyListener = readyListener;
    }
    public CommunityPropsDialog(Context context, Member member,
            ReadyListener readyListener, boolean justFinishOnEnd) {
        super(context);
        this.name = member.getName();
        this.member = member;
        this.readyListener = readyListener;
        this.justFinishOnEnd = justFinishOnEnd;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.logOnCreate(this.getContext(), savedInstanceState);
        setContentView(R.layout.com_attrib_dialogue);
        //setTitle("Enter your Name ");
        Button buttonOK = (Button) findViewById(R.id.dismiss_member_props);
        buttonOK.setOnClickListener(new OKListener());
        etName = (TextView) findViewById(R.id.com_attrib_name_text_view);
        etStatus = (TextView) findViewById(R.id.com_attrib_status_text_view);
        etHealth[0] = (TextView) findViewById(R.id.com_attrib_health1_text_view);
        etHealth[1] = (TextView) findViewById(R.id.com_attrib_health2_text_view);
        etWealth[0] = (TextView) findViewById(R.id.com_attrib_wealth1_text_view);
        etWealth[1] = (TextView) findViewById(R.id.com_attrib_wealth2_text_view);
        etBrains[0] = (TextView) findViewById(R.id.com_attrib_brains1_text_view);
        etBrains[1] = (TextView) findViewById(R.id.com_attrib_brains2_text_view);
        etAction[0] = (TextView) findViewById(R.id.com_attrib_action1_text_view);
        etAction[1] = (TextView) findViewById(R.id.com_attrib_action2_text_view);
        
        etName.setText(member.getName());
        etStatus.setText((member.getParentMemberID()==null ? "First, " : "")+(member.getCarried() ? "Carried" : "Placed"));
        setWidths(etHealth, member.getHealth());
        setWidths(etWealth, member.getWealth());
        setWidths(etAction, member.getAction());
        setWidths(etBrains, member.getBrains());
        
        
    }

    /**
	 * @param etBrains2
	 * @param brains
	 */
	private void setWidths(TextView[] ets, Integer pval) {
		int val = pval!=null ? pval.intValue() : 0;
        ViewGroup.LayoutParams params = /*new ViewGroup.LayoutParams*/(ets[0].getLayoutParams());
        params.width = MAX_WIDTH*val/10;
        ets[0].setLayoutParams(params);
        params = /*new ViewGroup.LayoutParams*/(ets[1].getLayoutParams());
        params.width = MAX_WIDTH*(10-val)/10;
        ets[1].setLayoutParams(params);
	}

	private class OKListener implements android.view.View.OnClickListener {
        @Override
        public void onClick(View v) {
            GameMapActivity.setCurrentMember(member);
            CommunityPropsDialog.this.dismiss();
            logger.log("memberGoToMap", "member", member.toString());
            if (!justFinishOnEnd) {
            	Intent intent = new Intent();
            	intent.setClass(CommunityPropsDialog.this.getContext(), GameMapActivity.class);
            	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            	CommunityPropsDialog.this.getContext().startActivity(intent);
            }
            readyListener.ready(String.valueOf(etName.getText()));
        }
    }

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		logger.logOnBackPressed();
	}
	@Override
	protected void onStart() {
		super.onStart();
		logger.logOnStart();
		BackgroundThread.setShouldBePaused(false);
	}
	@Override
	protected void onStop() {
		super.onStop();
		logger.logOnStop();
		BackgroundThread.setShouldBePaused(true);
	}

}
