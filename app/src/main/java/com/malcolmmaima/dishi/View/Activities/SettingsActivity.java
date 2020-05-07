package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.alexzh.circleimageview.CircleImageView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.squareup.picasso.Picasso;

import io.fabric.sdk.android.services.common.SafeToast;

public class SettingsActivity extends AppCompatActivity {

    String myPhone;
    private DatabaseReference myRef;
    private String TAG;
    MyTextView_Roboto_Medium userName;
    MyTextView_Roboto_Regular phoneNumber;
    CircleImageView profilePic;
    CardView personalDetails;
    RelativeLayout accountSettings, notificationSettings, help, about;
    ValueEventListener myRefListener;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            TAG = "SettingsActivity";

            //Initialize widgets
            initWidgets();

            Toolbar topToolBar = findViewById(R.id.toolbar);
            setSupportActionBar(topToolBar);

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            setTitle("Settings");

            //get auth state
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number

            //Set fb database reference
            myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);

            /**
             * Get logged in user details
             */
            myRefListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        UserModel user = dataSnapshot.getValue(UserModel.class);

                        //Set username on drawer header
                        userName.setText(user.getFirstname() + " " + user.getLastname());
                        phoneNumber.setText(myPhone);

                        Picasso.with(SettingsActivity.this).load(user.getProfilePic()).fit().centerCrop()
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(profilePic);

                    } catch (Exception e){
                        Log.e(TAG, "onDataChange: " + e);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };

            myRef.addValueEventListener(myRefListener);

            personalDetails.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent slideactivity = new Intent(SettingsActivity.this, PersonalDetails.class);
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                    startActivity(slideactivity, bndlanimation);
                }
            });

            //Back button on toolbar
            topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish(); //Go back to previous activity
                }
            });

            accountSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent slideactivity = new Intent(SettingsActivity.this, AccountSettings.class);
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                    startActivity(slideactivity, bndlanimation);
                }
            });

            notificationSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent slideactivity = new Intent(SettingsActivity.this, NotificationSettings.class);
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                    startActivity(slideactivity, bndlanimation);
                }
            });

            help.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent slideactivity = new Intent(SettingsActivity.this, HelpActivity.class);
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                    startActivity(slideactivity, bndlanimation);
                }
            });

            about.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent slideactivity = new Intent(SettingsActivity.this, AboutActivity.class);
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                    startActivity(slideactivity, bndlanimation);
                }
            });
        }
    }

    private void initWidgets() {
        personalDetails = findViewById(R.id.personalDetails);
        userName = findViewById(R.id.userName);
        phoneNumber = findViewById(R.id.phoneNumber);
        profilePic = findViewById(R.id.profilePic);

        userName.setText("Loading...");
        phoneNumber.setText("Loading...");

        accountSettings = findViewById(R.id.account);
        notificationSettings = findViewById(R.id.notifications);
        help = findViewById(R.id.help);
        about = findViewById(R.id.about);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            myRef.removeEventListener(myRefListener);
        } catch (Exception e){

        }
    }
}
