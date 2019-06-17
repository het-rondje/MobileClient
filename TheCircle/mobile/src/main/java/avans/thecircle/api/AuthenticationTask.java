package avans.thecircle.api;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

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

    public AuthenticationTask(Context context, AuthenticationTaskListener listener, String userId, String signature) {
        this.context = context;
        this.listener = listener;
        this.userId = userId;
        this.signature = signature;
    }

    @Override
    protected Response doInBackground(String... strings) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = new FormBody.Builder()
                .add("userId", userId)
                .add("signature", signature)
                .build();

        Request request = new Request.Builder()
                .url(API_URL + userId)
                .post(body)
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