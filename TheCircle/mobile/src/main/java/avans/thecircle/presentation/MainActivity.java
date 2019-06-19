package avans.thecircle.presentation;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.Timestamp;
import java.util.Date;

import avans.thecircle.R;
import avans.thecircle.api.AuthenticationTask;
import avans.thecircle.interfaces.AuthenticationTaskListener;
import avans.thecircle.utilities.ReponseState;

public class MainActivity extends AppCompatActivity implements AuthenticationTaskListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonOne = findViewById(R.id.loginBtn);

        buttonOne.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText userId = findViewById(R.id.userId);
                logIn(userId.getText().toString());
            }
        });
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 1);
        }
    }


    public void logIn(String userId) {
//        Date date = new Date();
//        Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
//        String txt = timeStamp.toString();
//        if (txt.length() > 0) {
//
//
//            try {
//
//                //Hash text
//                MessageDigest digest;
//                digest = MessageDigest.getInstance("SHA-256");
//                digest.update(txt.getBytes());
//                byte[] magnitude = digest.digest();
//                BigInteger bi = new BigInteger(1, magnitude);
//                String hash = String.format("%0" + (magnitude.length << 1) + "x", bi);
//                StringBuilder pkcs8Lines = new StringBuilder();
//                BufferedReader rdr = new BufferedReader(new StringReader(key));
//                String line;
//                while ((line = rdr.readLine()) != null) {
//                    pkcs8Lines.append(line);
//                }
//                // Remove the "BEGIN" and "END" lines, as well as any whitespace
//                String pkcs8Pem = pkcs8Lines.toString();
//                pkcs8Pem = pkcs8Pem.replace("-----BEGIN PRIVATE KEY-----", "");
//                pkcs8Pem = pkcs8Pem.replace("-----END PRIVATE KEY-----", "");
//                String replaced = pkcs8Pem.replaceAll("(\\\\n)","");
//                Log.e("TAGGGGG",""+replaced);
//                byte [] pkcs8EncodedBytes = android.util.Base64.decode(replaced, Base64.DEFAULT);
//                // extract the private key
//                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
//                KeyFactory kf = KeyFactory.getInstance("RSA");
//                PrivateKey privKey = kf.generatePrivate(keySpec);
//                System.out.println(privKey);
//                Signature signer = Signature.getInstance("SHA256withRSA");
//                signer.initSign(privKey);
//                signer.update(hash.getBytes());
//                byte[] signature = signer.sign();
//                StringBuilder hexBuilder = new StringBuilder();
//                for(byte b: signature){
//                    hexBuilder.append(String.format("%02x", b));
//                }
//                String encryptHash = hexBuilder.toString();
//                Log.e("ENCRYPTHASH", encryptHash);
//
//                AuthenticationTask authenticationTask = new AuthenticationTask(this.getApplicationContext(), this, userId, encryptHash, timeStamp);
//                authenticationTask.execute();
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (NoSuchAlgorithmException e) {
//                e.printStackTrace();
//            } catch (InvalidKeyException e) {
//                e.printStackTrace();
//            } catch (SignatureException e) {
//                e.printStackTrace();
//            } catch (InvalidKeySpecException e) {
//                e.printStackTrace();
//            }
//
//
//        }
//        else
//        {
//            //TODO: TOAST TOEVOEGEN
//        }
        Date date = new Date();
        Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
        String encryptHash = "hash";
        AuthenticationTask authenticationTask = new AuthenticationTask(this.getApplicationContext(), this, userId);
        authenticationTask.execute();
        Intent activity2Intent = new Intent(getApplicationContext(), CameraActivity.class);
        startActivity(activity2Intent);
    }
    @Override
    public void onAuthResponse(ReponseState state, String userId) {
        if (state == ReponseState.SUCCESS) {
//            SharedPreferences sharedPref = getSharedPreferences("PREFERENCES", Context.MODE_PRIVATE);
//            SharedPreferences.Editor editor = sharedPref.edit();
//            editor.putString("token", token);
//            editor.putString("userId", userId);
//            editor.apply();
            // TODO: ACTIONS ON LOGIN SUCCESS
            Log.e("RESPONSE", userId);
            Intent activity2Intent = new Intent(getApplicationContext(), CameraActivity.class);
            startActivity(activity2Intent);
        } else {
            Toast.makeText(MainActivity.this, "Invalid User Id", Toast.LENGTH_SHORT).show();
        }
    }
    public static String bytesToHex(byte[] bytes, char[] hexArray) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
