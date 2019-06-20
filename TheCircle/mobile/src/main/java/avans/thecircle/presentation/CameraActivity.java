package avans.thecircle.presentation;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
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

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.rtplibrary.rtmp.RtmpCamera1;

import avans.thecircle.api.ChatApplication;
import avans.thecircle.api.GetSatoshi;
import avans.thecircle.domain.Message;
import avans.thecircle.R;
import avans.thecircle.adapters.MessageAdapter;
import avans.thecircle.interfaces.OnSatoshiAvailable;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class CameraActivity extends AppCompatActivity
        implements ConnectCheckerRtmp, View.OnClickListener, SurfaceHolder.Callback, OnViewersAvailable, OnTaskComplete, OnSatoshiAvailable {

    private Socket socket;
    private RtmpCamera1 rtmpCamera1;
    private ImageButton button;
    private ImageButton pauseBtn;
    private EditText etUrl;
    private String streamUrl = "rtmp://159.65.197.36:1936/live/";
    private EditText editText;
    private ListView messagesView;
    private MessageAdapter messageAdapter;
    private JSONObject user;
    private String userId;
    private String firstname;
    private String lastname;

    private String currentDateAndTime = "";
    private File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/rtmp-rtsp-stream-client-java");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.container_view);
        SurfaceView surfaceView = findViewById(R.id.surfaceView);

        SharedPreferences sharedPreferences = getSharedPreferences("Settings", Context.MODE_PRIVATE);
        userId = sharedPreferences.getString("userId", "0");
        firstname = sharedPreferences.getString("firstName", "0");
        lastname = sharedPreferences.getString("lastName", "0");
        if(userId.equals("0")) {
            Intent activity2Intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(activity2Intent);
        }
        streamUrl = streamUrl + userId;

        //create socket
        ChatApplication app = (ChatApplication) getApplication();
        socket = app.getSocket();
        socket.on(Socket.EVENT_CONNECT,onJoinRoom);
        socket.on("message",onMessage);
        socket.connect();

        button = findViewById(R.id.b_start_stop);
        button.setOnClickListener(this);
        ImageButton switchCamera = findViewById(R.id.switch_camera);
        switchCamera.setOnClickListener(this);

        pauseBtn = findViewById(R.id.b_pause_start);
        pauseBtn.setOnClickListener(this);
        pauseBtn.setVisibility(View.GONE);
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
                checkViewers(userId);
            }
        }, 0, 2000);

        Timer t2 = new Timer();
        t2.schedule(new TimerTask() {
            @Override
            public void run() {
                checkSatoshi(userId);
            }
        }, 0, 900000);

    }

    public void sendMessage(View view) throws JSONException {
        String txt = editText.getText().toString();
        if (txt.length() > 0) {
            JSONObject message = new JSONObject();
            String signature = EncryptMessage(txt);
            message.put("text",txt);
            message.put("roomId",userId);
            message.put("signature",signature);
            message.put("sender",userId) ;
            message.put("firstName",firstname);
            message.put("lastName",lastname);
            socket.emit("message",message);
            editText.getText().clear();

        }
    }
    public void checkViewers(String id) {
        GetViewers getViewers = new GetViewers(this, this, id);
        getViewers.execute();
    }
    public void checkSatoshi(String id) {
        GetSatoshi getSatoshi = new GetSatoshi(this, this, id);
        getSatoshi.execute();
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
                    pauseBtn.setVisibility(View.GONE);
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
                        pauseBtn.setVisibility(View.VISIBLE);
                        rtmpCamera1.startStream(streamUrl);
                    } else {
                        Toast.makeText(this, "Error preparing stream, This device cant do it",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    button.setImageResource(R.drawable.start_btn);
                    pauseBtn.setVisibility(View.GONE);
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
                if(rtmpCamera1.isVideoEnabled()) {
                    rtmpCamera1.disableVideo();
                    rtmpCamera1.disableAudio();
                    pauseBtn.setImageResource(R.drawable.start_btn);
                } else {
                    rtmpCamera1.enableVideo();
                    rtmpCamera1.enableAudio();
                    pauseBtn.setImageResource(R.drawable.pause_btn);
                }

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
            pauseBtn.setVisibility(View.GONE);
        }
        rtmpCamera1.stopPreview();
    }

    public String EncryptMessage(String text){
        try{
            //Hash Message
            MessageDigest digest;
            digest = MessageDigest.getInstance("SHA-256");
            digest.update(text.getBytes());
            byte[] magnitude = digest.digest();
            BigInteger bi = new BigInteger(1, magnitude);
            String hash = String.format("%0" + (magnitude.length << 1) + "x", bi);
            Log.e("ITEMFORENCRYPTION",hash);

            //TODO: set private key from login
            String PK = "-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQChuJMuqiJ6SDAs\\nh+btr1O/P9aMRNiCc0l5J485Efk4hbBL7Q19ujk5pBm9O3EDbqU3j9t07sxaxE9u\\nQdd2a5QaZzUaD9MaPsu08ztdD3E18TDx2afQ/nLaBntj/hepoGhm39rppXADW6NX\\n3fny8IlO1OPaCPTsTnVzS7VLJ9xO095x/JOKr960UGYa6xYzQeo9AMbJCQr57QLl\\n4lRrLdA7LuuhKvDPwHjAOLvbjP57DQGkkBf8CchvbpbwOeUH11GbiyidjGqUXpvs\\nNQpV28azt8hFdaymdtEwqBWf7tzPXyPJPwfsvXBQ/2Vi7cy/ecCHFv43cWfcwN09\\nLs/Aym0RAgMBAAECggEAUsWMl1q+8MVX2sLoIAkXnRBIeFyYUBQ/q8HinTwkyZgr\\nRoEa4ZnZxjXGcsMksbQE3e5ETZIXh/FoEi+i3tpq4CSo2iD1VD6FtqSzYosPz6MW\\nAQL80IwbLpoYt3IKnGgcZ2L1wZZKQX26mbNkRcJ1FKMDx8nDydrSZGOAc+n/6VvY\\nzA0hsJWQdd4Iw/lcu6Bl3ZkA3LdIqgU/eSdWAKeW3p40nfhTjGKja7pNw3FW90Iw\\n26IjOiFpiZhpyesV39UiV5pLxgGP4OJ8ZJ2g/Ylu5+fBeg/XoSbxdgByI15tp6Fm\\nBIn4n0I9dnoGgJtMLW1S+nht0cuhXsr6VAeziB3aAQKBgQDUL7vZqnVt7m0dunGZ\\nLA/gQ8NbE65ba+d4PFlWlzS6yesgadFiQYId94S9HWbyWQq4DJphSBfvkvCgRmVN\\nfaQ/3PQOZh0AywvW4goMi3uVu8/kTh3qDOs0zJY4dPsoFVxD7LHvjiy7XaYdU0WE\\n1V+EUzGLlE9ntKfle+L8DQnlcQKBgQDDHTcejzCYS7nxaQOHz+xucPLiFyZGIbd4\\nX3fTcpeDKI3iKBn+X8s4RuZ2M2ASAUJD/jiQOly2fJqiTe8+lPnOHJyOcR92Gd5d\\nSC9zkuc2kVOmvmPzaAwo18lrg3apxjTtgg0RN2oHrv4ts/u9CYMOuai9nrAD+Cp7\\nd8zD5c6xoQKBgQC1IlKQS+2W/LR1blXPVkfvQKmiNDjhnkmo4Iu7WbUPx5NKxkqS\\nQC3dexD7h//73ntCBrA7X5nfUGbNy4prDliKlApbyFv181+V/rxpXSEQ/5VG5lCv\\nHnwjRIrwgxxsaV+sNIDpaUtSX22RFyb/cE7r3UEsUF1AwH2bb5ijZlYOYQKBgECz\\n3oR2zZGjgx0ISxBtpiUVtaW+MYYORk5Xsl/fX1kySKLX047ka3rVIDXQYap22me7\\n1TwW7onhllH+cDkbpB9yo4QlBV7fwzrB5mJ4M86HsOrZtkGQnn+o13Wc2ewA+6pL\\nd1PfQX0czdOQHQoaLmjiro97ITmfwU7CcpgADGQhAoGBAI0/xWxFArVYJDDCIye5\\nIVQBZJOzSw/bfGJUSEYfu4kcq8cP9MOMrLSx4/StiNevRPXwzit/j89eWQEf/NX3\\ni/JwfiUVdiGD1+LGc7sWAdI1+Jdh8FePa7QlSkFVDKL9yABl/iLW/vA5ZNfIhz7q\\n6zKabWFS4COVsCB77GNE1Bll\\n-----END PRIVATE KEY-----";
            // Remove the "BEGIN" and "END" lines, as well as any whitespace
            String pkcs8Pem = PK;
            pkcs8Pem = pkcs8Pem.replace("-----BEGIN PRIVATE KEY-----", "");
            pkcs8Pem = pkcs8Pem.replace("-----END PRIVATE KEY-----", "");
            String replaced = pkcs8Pem.replaceAll("(\\\\n)","");
            Log.e("ENCRYPTHASH",""+replaced);
            byte [] pkcs8EncodedBytes = android.util.Base64.decode(replaced, Base64.DEFAULT);
            // extract the private key
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privKey = kf.generatePrivate(keySpec);
            System.out.println(privKey);
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privKey);
            signer.update(hash.getBytes());
            byte[] signature = signer.sign();
            byte[] encoded = Base64.encode(signature,Base64.DEFAULT);
            StringBuilder hexBuilder = new StringBuilder();
            for(byte b:signature)
            {
                hexBuilder.append(String.format("%02x", b));
            }
            String hexSignature = hexBuilder.toString();
            String encodedstring = new String(encoded);
            Log.e("ENCRYPTHASH",hexSignature);
            return hexSignature;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            Log.e("ENCRYPTHASH", "" + e);
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return "";
    }
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
    private Emitter.Listener onJoinRoom = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    socket.emit("join",userId);
                    //TODO: USER ID FROM USER LOGIN MUST COME HERE
                }
            });
        }
    };
    private Emitter.Listener onMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        //extract data from fired event

                        String msg = data.getString("text");
                        // make instance of message
                        Message message = new Message(msg,data.getString("firstName")+" "+data.getString("lastName"));
                        messageAdapter.add(message);

                        messagesView.setSelection(messageAdapter.getCount() -1);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
        }
    };


    @Override
    public void OnViewersAvailable(ReponseState state, String viewers) {
       TextView TVviewers = findViewById(R.id.viewers);
       Log.e("VIEWERS:", viewers);
       TVviewers.setText(viewers);
    }

    @Override
    public void onTaskComplete(ReponseState state) {

    }

    @Override
    public void OnSatoshiAvailable(ReponseState state, String coins) {
        TextView TVcoins = findViewById(R.id.coins);
        TVcoins.setText(coins);
    }
}