package com.malcolmmaima.dishiapp.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishiapp.R;



public class HelpActivity extends AppCompatActivity {
    String TAG = "HelpActivity";
    String myPhone;
    DatabaseReference myRef, supportRef;
    FirebaseAuth mAuth;
    FirebaseUser user;
    String helpType;
    RelativeLayout resetPin, privacyPolicy, dishiFaq, contactSupport, supportDash;
    View supportDashBorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Help");

        resetPin = findViewById(R.id.resetPin);
        resetPin.setEnabled(false);
        privacyPolicy = findViewById(R.id.privacyPolicy);
        dishiFaq = findViewById(R.id.dishiFaq);
        contactSupport = findViewById(R.id.contactSupport);
        supportDashBorder = findViewById(R.id.supportDashBorder);
        supportDashBorder.setVisibility(View.GONE);
        supportDash = findViewById(R.id.supportDash);
        supportDash.setVisibility(View.GONE);
        supportDash.setEnabled(false);

        try {
            helpType = getIntent().getStringExtra("type");
        } catch (Exception e){
            Log.e(TAG, "onCreate: ", e);
        }

        //Set fb database reference
        myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber();
            myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
            supportRef = FirebaseDatabase.getInstance().getReference("support/"+myPhone);

            if(helpType != null){
                if(helpType.equals("reset")){
                    resetPin.setEnabled(true);
                }

                if(helpType.equals("normal")){
                    myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){
                                resetPin.setEnabled(true);
                                myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        try {
                                            Boolean locked = dataSnapshot.getValue(Boolean.class);

                                            if (locked == true) {
                                                Intent slideactivity = new Intent(HelpActivity.this, SecurityPin.class)
                                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                slideactivity.putExtra("pinType", "resume");
                                                startActivity(slideactivity);
                                            }
                                        } catch (Exception e){
                                            Log.e(TAG, "onDataChange: ", e);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }

                            else {
                                resetPin.setEnabled(false);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            //allow support accounts to deal with user issues (Support dashboard)
            supportRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        try {
                            Boolean approved = dataSnapshot.getValue(Boolean.class);
                            if (approved == true) {
                                supportDashBorder.setVisibility(View.VISIBLE);
                                supportDash.setVisibility(View.VISIBLE);
                                supportDash.setEnabled(true);
                            } else {
                                supportDashBorder.setVisibility(View.GONE);
                                supportDash.setVisibility(View.GONE);
                                supportDash.setEnabled(false);
                            }
                        } catch (Exception e){

                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });

        resetPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent slideactivity = new Intent(HelpActivity.this, ResetPin.class);
                Bundle bndlanimation =
                        ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                startActivity(slideactivity, bndlanimation);
            }
        });

        privacyPolicy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent slideactivity = new Intent(HelpActivity.this, PrivacyPolicy.class);
                Bundle bndlanimation =
                        ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                startActivity(slideactivity, bndlanimation);
            }
        });

        dishiFaq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent slideactivity = new Intent(HelpActivity.this, DishiFaq.class);
                Bundle bndlanimation =
                        ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                startActivity(slideactivity, bndlanimation);
            }
        });

        contactSupport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent slideactivity = new Intent(HelpActivity.this, ContactSupport.class);
                Bundle bndlanimation =
                        ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                startActivity(slideactivity, bndlanimation);
            }
        });

        supportDash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //allow support accounts to deal with user issues (Support dashboard)
                supportRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            try {
                                Boolean approved = dataSnapshot.getValue(Boolean.class);
                                if (approved == true) {
                                    Intent slideactivity = new Intent(HelpActivity.this, SupportPin.class);
                                    Bundle bndlanimation =
                                            ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                    startActivity(slideactivity, bndlanimation);

                                } else {
                                    supportDashBorder.setVisibility(View.GONE);
                                    supportDash.setVisibility(View.GONE);
                                    supportDash.setEnabled(false);
                                    Snackbar snackbar = Snackbar
                                            .make((LinearLayout) findViewById(R.id.parentlayout), "You're not approved to do this", Snackbar.LENGTH_LONG);

                                    snackbar.show();
                                }
                            } catch (Exception e){

                            }
                        } else {
                            Snackbar snackbar = Snackbar
                                    .make((LinearLayout) findViewById(R.id.parentlayout), "You're not approved to do this", Snackbar.LENGTH_LONG);

                            snackbar.show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(helpType != null){
            if(helpType.equals("reset")){
                resetPin.setEnabled(true);
            }

            if(helpType.equals("normal")){
                myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            resetPin.setEnabled(true);
                            myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    try {
                                        Boolean locked = dataSnapshot.getValue(Boolean.class);

                                        if (locked == true) {
                                            Intent slideactivity = new Intent(HelpActivity.this, SecurityPin.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            slideactivity.putExtra("pinType", "resume");
                                            startActivity(slideactivity);
                                        }
                                    } catch (Exception e){
                                        Log.e(TAG, "onDataChange: ", e);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }

                        else {
                            resetPin.setEnabled(false);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        }
    }
}
