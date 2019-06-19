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


    public AuthenticationTask(Context context, AuthenticationTaskListener listener, String userId) {
        this.context = context;
        this.listener = listener;
        this.userId = userId;
    }

    @Override
    protected Response doInBackground(String... strings) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(API_URL + userId)
                .get()
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


            if(code == 200){
                JSONObject jsonObject = new JSONObject(json);

                String userId = jsonObject.getString("_id");

                listener.onAuthResponse(ReponseState.SUCCESS, userId);

            } else {
                listener.onAuthResponse(ReponseState.INVALID_CREDENTIALS, "");
                return;
            }

        } catch (IOException e) {
            Log.e(this.getClass().getSimpleName(), "Error while processing response body", e);
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), "Error while parsing json", e);
        }


    }

}
