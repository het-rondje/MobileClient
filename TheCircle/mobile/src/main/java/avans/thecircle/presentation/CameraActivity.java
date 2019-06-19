package avans.thecircle.presentation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.rtplibrary.rtmp.RtmpCamera1;

import avans.thecircle.api.GetViewers;
import avans.thecircle.domain.Message;
import avans.thecircle.R;
import avans.thecircle.adapters.MessageAdapter;
import avans.thecircle.interfaces.OnTaskComplete;
import avans.thecircle.interfaces.OnViewersAvailable;
import avans.thecircle.utilities.ReponseState;;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import net.ossrs.rtmp.ConnectCheckerRtmp;

public class CameraActivity extends AppCompatActivity
        implements ConnectCheckerRtmp, View.OnClickListener, SurfaceHolder.Callback, OnViewersAvailable, OnTaskComplete {

    private RtmpCamera1 rtmpCamera1;
    private ImageButton button;
    private EditText etUrl;
    private String streamUrl = "rtmp://159.65.197.36:1936/live?key=mykey";
    private EditText editText;
    private ListView messagesView;
    private MessageAdapter messageAdapter;

    private String currentDateAndTime = "";
    private File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/rtmp-rtsp-stream-client-java");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.container_view);
        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        button = findViewById(R.id.b_start_stop);
        button.setOnClickListener(this);
        ImageButton switchCamera = findViewById(R.id.switch_camera);
        switchCamera.setOnClickListener(this);
        rtmpCamera1 = new RtmpCamera1(surfaceView, this);
        rtmpCamera1.setReTries(10);
        surfaceView.getHolder().addCallback(this);

        editText = (EditText) findViewById(R.id.editText);
        messageAdapter = new MessageAdapter(this);
        messagesView = (ListView) findViewById(R.id.messages_view);
        messagesView.setAdapter(messageAdapter);
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                checkViewers("eLbwB5tVW");
            }
        }, 0, 2000);

    }

    public void sendMessage(View view) {
        String txt = editText.getText().toString();
        if (txt.length() > 0) {
            Message message = new Message(txt, "Jorrit076");
            messageAdapter.add(message);
            editText.getText().clear();
            messagesView.setSelection(messageAdapter.getCount() -1);
        }
    }
    public void checkViewers(String id) {
        GetViewers getViewers = new GetViewers(this, this, id);
        getViewers.execute();
    }
    public void disableCamera() {
        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        surfaceView.setVisibility(View.INVISIBLE);
        Log.e("ACTION", "DISABLE");
    }

    @Override
    public void onConnectionSuccessRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CameraActivity.this, "Connection success", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConnectionFailedRtmp(final String reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (rtmpCamera1.shouldRetry(reason)) {
                    Toast.makeText(CameraActivity.this, "Retry", Toast.LENGTH_SHORT)
                            .show();
                    rtmpCamera1.reTry(5000);  //Wait 5s and retry connect stream
                } else {
                    Toast.makeText(CameraActivity.this, "Connection failed. " + reason, Toast.LENGTH_SHORT)
                            .show();
                    rtmpCamera1.stopStream();
                    button.setImageResource(R.drawable.start_btn);
                }
            }
        });
    }

    @Override
    public void onDisconnectRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CameraActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAuthErrorRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CameraActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAuthSuccessRtmp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CameraActivity.this, "Auth success", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.b_start_stop:
                if (!rtmpCamera1.isStreaming()) {
                    if (rtmpCamera1.isRecording()
                            || rtmpCamera1.prepareAudio() && rtmpCamera1.prepareVideo()) {
                        button.setImageResource(R.drawable.stop_btn);
                        rtmpCamera1.startStream(streamUrl);
                    } else {
                        Toast.makeText(this, "Error preparing stream, This device cant do it",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    button.setImageResource(R.drawable.start_btn);
                    rtmpCamera1.stopStream();
                }
                break;
            case R.id.switch_camera:
                try {
                    rtmpCamera1.switchCamera();
                } catch (CameraOpenException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.b_pause_start:
                disableCamera();
                break;
            default:
                break;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        rtmpCamera1.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && rtmpCamera1.isRecording()) {
            rtmpCamera1.stopRecord();
            Toast.makeText(this,
                    "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(),
                    Toast.LENGTH_SHORT).show();
            currentDateAndTime = "";
        }
        if (rtmpCamera1.isStreaming()) {
            rtmpCamera1.stopStream();
            button.setImageResource(R.drawable.start_btn);
        }
        rtmpCamera1.stopPreview();
    }

    public void onMessage() {

    }

    @Override
    public void OnViewersAvailable(ReponseState state, String viewers) {
       TextView TVviewers = findViewById(R.id.viewers);
       TVviewers.setText(viewers);
    }

    @Override
    public void onTaskComplete(ReponseState state) {

    }
}