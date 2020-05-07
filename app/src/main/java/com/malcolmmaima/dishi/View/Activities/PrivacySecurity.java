package com.malcolmmaima.dishi.View.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.R;

public class PrivacySecurity extends AppCompatActivity {

    String myPhone;
    DatabaseReference myRef;
    Switch shareOrders, syncContacts;
    RelativeLayout setViewPhone, blockedAccounts, myLocationSettings, accountPrivacy, accountPin, loginActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_security);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Privacy & Security");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        //Set fb database reference
        myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);

        shareOrders = findViewById(R.id.shareOrdersSwitch);
        syncContacts = findViewById(R.id.syncContacts);
        setViewPhone = findViewById(R.id.setViewPhone);
        blockedAccounts = findViewById(R.id.blockedAccounts);
        myLocationSettings = findViewById(R.id.myLocationSettings);
        accountPrivacy = findViewById(R.id.accountPrivacy);
        accountPin = findViewById(R.id.accountPin);
        loginActivity = findViewById(R.id.loginActivity);

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });

        //switches
        shareOrders.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position

                if(isChecked == true && buttonView.isPressed()){
                    Toast.makeText(PrivacySecurity.this, "Share orders ON", Toast.LENGTH_SHORT).show();
                }

                else {
                    Toast.makeText(PrivacySecurity.this, "Share orders OFF", Toast.LENGTH_SHORT).show();
                }

            }
        });

        syncContacts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position

                if(isChecked == true && buttonView.isPressed()){
                    Toast.makeText(PrivacySecurity.this, "Sync contacts ON", Toast.LENGTH_SHORT).show();
                }

                else {
                    Toast.makeText(PrivacySecurity.this, "Sync contacts OFF", Toast.LENGTH_SHORT).show();
                }

            }
        });

        //others
        setViewPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(PrivacySecurity.this, "clicked", Toast.LENGTH_SHORT).show();
            }
        });

        blockedAccounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(PrivacySecurity.this, "clicked", Toast.LENGTH_SHORT).show();
            }
        });

        myLocationSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(PrivacySecurity.this, "clicked", Toast.LENGTH_SHORT).show();
            }
        });

        accountPrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(PrivacySecurity.this, "clicked", Toast.LENGTH_SHORT).show();
            }
        });

        accountPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(PrivacySecurity.this, "clicked", Toast.LENGTH_SHORT).show();
            }
        });

        loginActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(PrivacySecurity.this, "clicked", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
