package com.malcolmmaima.dishi.View.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.malcolmmaima.dishi.Controller.ForegroundService;
import com.malcolmmaima.dishi.Controller.TrackingService;
import com.malcolmmaima.dishi.R;

public class BlockedAccount extends AppCompatActivity {
    TextView logOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_account);

        logOut = findViewById(R.id.logout);

        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(BlockedAccount.this, ForegroundService.class));
                stopService(new Intent(BlockedAccount.this, TrackingService.class));
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(BlockedAccount.this,MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            }
        });
    }
}
