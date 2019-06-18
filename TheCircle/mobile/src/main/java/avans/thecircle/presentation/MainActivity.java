package avans.thecircle.presentation;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import avans.thecircle.R;
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
                Intent activity2Intent = new Intent(getApplicationContext(), CameraActivity.class);
                startActivity(activity2Intent);
            }
        });
    }


    @Override
    public void onAuthResponse(ReponseState state, String token, String userId) {
        if (state == ReponseState.SUCCESS) {
            SharedPreferences sharedPref = getSharedPreferences("PREFERENCES", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("token", token);
            editor.putString("userId", userId);
            editor.apply();
        }

    }
}
