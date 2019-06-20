package avans.thecircle.api;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;

import avans.thecircle.domain.GlobalValues;
import avans.thecircle.interfaces.OnTaskComplete;
import avans.thecircle.interfaces.OnViewersAvailable;
import avans.thecircle.utilities.ReponseState;

public class GetViewers extends AsyncTask<String, Void, String> {

    private OnViewersAvailable onViewersAvailable = null;
    private OnTaskComplete onTaskComplete = null;
    private String id;

    public GetViewers(OnViewersAvailable onViewersAvailable, OnTaskComplete onTaskComplete, String id) {
        this.onViewersAvailable = onViewersAvailable;
        this.onTaskComplete = onTaskComplete;
        this.id = id;
    }

    @Override
    protected String doInBackground(String... strings) {
        InputStream inputStream = null;
        int responsCode = -1;
        String serverUrl = GlobalValues.HOST_URL + "/api/users/" + this.id + "/viewers";
        String response = "";

        try {
            URL url = new URL(serverUrl);
            URLConnection urlConnection = url.openConnection();
            if (!(urlConnection instanceof HttpURLConnection)) {
                return null;
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

    protected void onPostExecute(String response) {

        if(response.length() == 0){
            onViewersAvailable.OnViewersAvailable(ReponseState.ERROR, "");
        } else {
            try {
                JSONObject jsonObject = new JSONObject(response);
                String count = jsonObject.get("count").toString();
                onViewersAvailable.OnViewersAvailable(ReponseState.SUCCESS, count);
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
