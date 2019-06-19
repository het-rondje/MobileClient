package avans.thecircle.api;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
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



public class AuthenticationTask extends AsyncTask<String, Void, String> {
    private static final String API_URL = GlobalValues.HOST_URL + "/api/users/";
    private AuthenticationTaskListener listener;
    private Context context;
    private String userId;


    public AuthenticationTask(Context context, AuthenticationTaskListener listener, String userId) {
        this.context = context;
        this.listener = listener;
        this.userId = userId;
    }

    @Override
    protected String doInBackground(String... strings) {
        InputStream inputStream = null;
        int responsCode = -1;
        String serverUrl = GlobalValues.HOST_URL + "/api/users/" + this.userId;
        String response = "";
        if(this.userId.length() == 0) {
            return "";
        }

        try {
            URL url = new URL(serverUrl);
            URLConnection urlConnection = url.openConnection();
            if (!(urlConnection instanceof HttpURLConnection)) {
                return "";
            }
            HttpURLConnection httpConnection = (HttpURLConnection) urlConnection;
            httpConnection.setConnectTimeout(16000);
            httpConnection.setReadTimeout(16000);
            httpConnection.setAllowUserInteraction(false);
            httpConnection.setInstanceFollowRedirects(true);
            httpConnection.setRequestMethod("GET");

            httpConnection.connect();
            inputStream = httpConnection.getInputStream();

            response = getStringFromInputStream(inputStream);



        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;

    }
    @Override
    protected void onPostExecute(String response) {
        Log.e("res",response);
        if(response.length() == 0){
            listener.onAuthResponse(ReponseState.INVALID_CREDENTIALS, "");
        } else {
            try {
                JSONObject jsonObject = new JSONObject(response);
                String id = jsonObject.get("_id").toString();
                listener.onAuthResponse(ReponseState.SUCCESS, id);
            } catch (JSONException e) {
                Log.d("Error", e.toString());
            }
        }



    }

    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

}
