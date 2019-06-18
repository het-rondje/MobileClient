package avans.thecircle.api;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

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

import avans.thecircle.domain.GlobalValues;
import avans.thecircle.interfaces.AuthenticationTaskListener;
import avans.thecircle.utilities.ReponseState;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthenticationTask extends AsyncTask<String, Void, Response> {
    private static final String API_URL = GlobalValues.HOST_URL + "/api/users/";
    private AuthenticationTaskListener listener;
    private Context context;
    private String userId;
    private String signature;
    private String key;
    private Timestamp timeStamp;

    public AuthenticationTask(Context context, AuthenticationTaskListener listener, String userId, String key) {
        this.context = context;
        this.listener = listener;
        this.userId = userId;
        this.key = key;
        this.timeStamp = new Timestamp(System.currentTimeMillis());
    }

    @Override
    protected Response doInBackground(String... strings) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = new FormBody.Builder()
                .build();

        String txt = this.timeStamp.toString();
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
                BufferedReader rdr = new BufferedReader(new StringReader(this.key));
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
                this.signature = encryptHash;


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


        Request request = new Request.Builder()
                .url(API_URL + userId)
                .post(body)
                .addHeader("userId", userId)
                .addHeader("timeStamp", timeStamp.toString())
                .addHeader("timeSignature", signature)
                .build();

        try {
            Response response = client.newCall(request).execute();
            return response;
        } catch (IOException e) {
            Log.e(this.getClass().getSimpleName(), "Error doing post!", e);
            return null;
        }
    }
    @Override
    protected void onPostExecute(Response response) {


        if(response == null){
            Log.e(this.getClass().getSimpleName(), "Api request had an error and the response returned null!");
            return;
        }

        try {
            int code = response.code();
            String json = response.body().string();

            if(code == 412){
                listener.onAuthResponse(ReponseState.INVALID_CREDENTIALS, "", "");
                return;
            }

            if(code == 200){
                JSONObject jsonObject = new JSONObject(json);

                String token = jsonObject.getString("token");
                String roomName = jsonObject.getString("userId");

                listener.onAuthResponse(ReponseState.SUCCESS, token, roomName);

            }

        } catch (IOException e) {
            Log.e(this.getClass().getSimpleName(), "Error while processing response body", e);
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "Error while parsing json", e);
        }


    }

}
