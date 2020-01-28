package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.TrackingService;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Maps.SearchLocation;

public class LocationSettings extends AppCompatActivity {

    private String TAG;
    String myPhone;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    Switch defaultLocSwitch, liveLocSwitch;
    Button setLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_settings);

        TAG = "LocationSettings";

        //Initialize widgets
        initWidgets();

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Location");
        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });

        //get auth state
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        //Set fb database reference
        myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
        myRef.child("locationType").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                try {
                    String locationType = dataSnapshot.getValue(String.class);
                    if(locationType.equals("live")){
                        liveLocSwitch.setChecked(true);
                        defaultLocSwitch.setChecked(false);
                        startService(new Intent(LocationSettings.this, TrackingService.class));
                    }

                    if(locationType.equals("default")){
                        liveLocSwitch.setChecked(false);
                        defaultLocSwitch.setChecked(true);
                        stopService(new Intent(LocationSettings.this, TrackingService.class));
                    }

                } catch (Exception e){
                    Snackbar snackbar = Snackbar.make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }

        });

        defaultLocSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
                if(isChecked){
                    liveLocSwitch.setChecked(false);
                    myRef.child("locationType").setValue("default");
                    stopService(new Intent(LocationSettings.this, TrackingService.class));
                    checkStaticLocation();
                }

                else {
                    liveLocSwitch.setChecked(true);
                    myRef.child("locationType").setValue("live");
                    setLocation.setVisibility(View.GONE);
                    startService(new Intent(LocationSettings.this, TrackingService.class));
                }


            }
        });

        liveLocSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position

                if(isChecked){
                    defaultLocSwitch.setChecked(false);
                    myRef.child("locationType").setValue("live");
                    setLocation.setVisibility(View.GONE);
                    startService(new Intent(LocationSettings.this, TrackingService.class));
                }

                else {
                    defaultLocSwitch.setChecked(true);
                    myRef.child("locationType").setValue("default");
                    checkStaticLocation();
                    stopService(new Intent(LocationSettings.this, TrackingService.class));
                }

            }
        });

        setLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent slideactivity = new Intent(LocationSettings.this, SearchLocation.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(slideactivity);
            }
        });
    }

    private void checkStaticLocation() {
        //Check if user has set default location
        myRef.child("my_location").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChildren()){
                    //my location data exists
                    setLocation.setVisibility(View.VISIBLE);
                    setLocation.setText("CHANGE LOCATION");
                }

                else {
                    //User has not set static business location
                    setLocation.setVisibility(View.VISIBLE);
                    setLocation.setText("SET LOCATION");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void initWidgets() {
        defaultLocSwitch = findViewById(R.id.defaultLocSwitch);
        liveLocSwitch = findViewById(R.id.liveLocSwitch);
        setLocation = findViewById(R.id.setLocation);
    }
}