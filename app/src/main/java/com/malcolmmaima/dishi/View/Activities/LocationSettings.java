package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
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

import io.fabric.sdk.android.services.common.SafeToast;

public class LocationSettings extends AppCompatActivity {

    String myPhone;
    private DatabaseReference myRef;
    Switch defaultLocSwitch, liveLocSwitch;
    Button setLocation;
    Double lat, lng;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_settings);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number
            //Set fb database reference
            myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
            myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                Boolean locked = dataSnapshot.getValue(Boolean.class);

                                if(locked == true){
                                    Intent slideactivity = new Intent(LocationSettings.this, SecurityPin.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    slideactivity.putExtra("pinType", "resume");
                                    startActivity(slideactivity);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            String TAG = "LocationSettings";

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
                    requestLocation();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Boolean locked = dataSnapshot.getValue(Boolean.class);

                            if(locked == true){
                                Intent slideactivity = new Intent(LocationSettings.this, SecurityPin.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                slideactivity.putExtra("pinType", "resume");
                                startActivity(slideactivity);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Listen to the SearchLocation activity for LatLng values sent back
     */
    private static final int REQUEST_GET_MAP_LOCATION = 0;
    void requestLocation() {
        startActivityForResult(new Intent(LocationSettings.this, SearchLocation.class), REQUEST_GET_MAP_LOCATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_GET_MAP_LOCATION && resultCode == Activity.RESULT_OK) {
            Double latitude = data.getDoubleExtra("latitude", 0.0);
            Double longitude = data.getDoubleExtra("longitude", 0.0);

            lat = latitude;
            lng = longitude;
            String placeName = data.getStringExtra("place");
            // do something with B's return values

            Snackbar.make(findViewById(R.id.parentlayout), "Saving...", Snackbar.LENGTH_LONG).show();
            myRef.child("my_location").child("latitude").setValue(latitude);
            myRef.child("my_location").child("longitude").setValue(longitude);
            myRef.child("my_location").child("place").setValue(placeName).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Snackbar.make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG).show();
                }
            });
        }
    }

    /**
     * Check Location Status (if set)
     */
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