package avans.thecircle.presentation;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import avans.thecircle.R;
import avans.thecircle.api.GetViewers;
import avans.thecircle.interfaces.OnTaskComplete;
import avans.thecircle.interfaces.OnViewersAvailable;
import avans.thecircle.utilities.ReponseState;

public class MainActivity extends WearableActivity implements OnViewersAvailable, OnTaskComplete {

    private TextView mTextView;
    private String userId = "eLbwB5tVW";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.text);

        // Enables Always-on
        setAmbientEnabled();

        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                checkViewers(userId);
            }
        }, 0, 2000);
    }
    public void checkViewers(String id) {
        GetViewers getViewers = new GetViewers(this, this, id);
        getViewers.execute();
    }

    @Override
    public void onTaskComplete(ReponseState state) {

    }

    @Override
    public void OnViewersAvailable(ReponseState state, String viewers) {
        TextView TVviewers = findViewById(R.id.text);
        TVviewers.setText(viewers);
    }
}
