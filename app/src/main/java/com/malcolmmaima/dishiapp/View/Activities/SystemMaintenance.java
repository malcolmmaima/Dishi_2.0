package com.malcolmmaima.dishiapp.View.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.malcolmmaima.dishiapp.Controller.Services.ForegroundService;
import com.malcolmmaima.dishiapp.Controller.Services.TrackingService;
import com.malcolmmaima.dishiapp.R;

public class SystemMaintenance extends AppCompatActivity {

    TextView logOut;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_maintenance);

        logOut = findViewById(R.id.logout);

        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(SystemMaintenance.this, ForegroundService.class));
                stopService(new Intent(SystemMaintenance.this, TrackingService.class));
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(SystemMaintenance.this,MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            }
        });
    }
}
