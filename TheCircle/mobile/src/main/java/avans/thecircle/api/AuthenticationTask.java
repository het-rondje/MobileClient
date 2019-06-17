package avans.thecircle.api;

import android.content.Context;
import android.os.AsyncTask;

import avans.thecircle.domain.GlobalValues;
import avans.thecircle.interfaces.AuthenticationTaskListener;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthenticationTask extends AsyncTask<String, Void, Response> {
    private static final String API_URL = GlobalValues.HOST_URL + "/api/login/";
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
        return null;
    }
}
