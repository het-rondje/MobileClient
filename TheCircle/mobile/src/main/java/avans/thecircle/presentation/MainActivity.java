package avans.thecircle.presentation;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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
                EditText key = findViewById(R.id.key);
                EditText userId = findViewById(R.id.userId);
                logIn(key.getText().toString(), userId.getText().toString());
            }
        });
    }


    public void logIn(String key, String userId) {
        Date date = new Date();
        Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
        String txt = timeStamp.toString();
        if (txt.length() > 0) {
            JSONObject message = new JSONObject();

            try {

                //Hash text
                MessageDigest digest;
                digest = MessageDigest.getInstance("SHA-256");
                digest.update(txt.getBytes());
                byte[] magnitude = digest.digest();
                BigInteger bi = new BigInteger(1, magnitude);
                String hash = String.format("%0" + (magnitude.length << 1) + "x", bi);
                StringBuilder pkcs8Lines = new StringBuilder();
                BufferedReader rdr = new BufferedReader(new StringReader(key));
                String line;
                while ((line = rdr.readLine()) != null) {
                    pkcs8Lines.append(line);
                }
                // Remove the "BEGIN" and "END" lines, as well as any whitespace
                String pkcs8Pem = pkcs8Lines.toString();
                pkcs8Pem = pkcs8Pem.replace("-----BEGIN PRIVATE KEY-----", "");
                pkcs8Pem = pkcs8Pem.replace("-----END PRIVATE KEY-----", "");
                String replaced = pkcs8Pem.replaceAll("(\\\\n)","");
                Log.e("TAGGGGG",""+replaced);
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
                String encryptHash = new String(signature);
                Log.e("HASH", encryptHash);

                AuthenticationTask authenticationTask = new AuthenticationTask(this.getApplicationContext(), this, userId, encryptHash, timeStamp);
                authenticationTask.execute();


                message.put("text",txt);
                message.put("sender","");
                message.put("roomId","_rFGS_JEC");
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (SignatureException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            }


        }
        else
        {
            //TODO: TOAST TOEVOEGEN
        }

//        Intent activity2Intent = new Intent(getApplicationContext(), CameraActivity.class);
//        startActivity(activity2Intent);
    }
    @Override
    public void onAuthResponse(ReponseState state, String token, String userId) {
        if (state == ReponseState.SUCCESS) {
            SharedPreferences sharedPref = getSharedPreferences("PREFERENCES", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("token", token);
            editor.putString("userId", userId);
            editor.apply();
            Intent activity2Intent = new Intent(getApplicationContext(), CameraActivity.class);
            startActivity(activity2Intent);
        }
    }
}
