package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.view.View;
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
import com.malcolmmaima.dishi.R;

public class LocationSettings extends AppCompatActivity {

    private String TAG;
    String myPhone;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    Switch defaultLocSwitch, liveLocSwitch;

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
                    }

                    if(locationType.equals("default")){
                        liveLocSwitch.setChecked(false);
                        defaultLocSwitch.setChecked(true);
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
                }

                else {
                    liveLocSwitch.setChecked(true);
                    myRef.child("locationType").setValue("live");
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
                }

                else {
                    defaultLocSwitch.setChecked(true);
                    myRef.child("locationType").setValue("default");
                }

            }
        });
    }

    private void initWidgets() {
        defaultLocSwitch = findViewById(R.id.defaultLocSwitch);
        liveLocSwitch = findViewById(R.id.liveLocSwitch);
    }
}
