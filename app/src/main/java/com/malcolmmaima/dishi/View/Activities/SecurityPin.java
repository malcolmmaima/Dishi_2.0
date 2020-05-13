package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Controller.Services.ForegroundService;
import com.malcolmmaima.dishi.Controller.Services.TrackingService;
import com.malcolmmaima.dishi.R;

import io.fabric.sdk.android.services.common.SafeToast;

public class SecurityPin extends AppCompatActivity {
    FirebaseUser user;
    String myPhone, pinType;
    DatabaseReference myRef;
    FirebaseAuth mAuth;
    String TAG = "SecurityPin";
    String myPin, oldPin, loginPin, accountType;
    ImageView pin1, pin2, pin3, pin4;
    LinearLayout num1, num2, num3, num4, num5, num6, num7, num8, num9, num0;
    MyTextView_Roboto_Medium logout, resetPin;
    MyTextView_Roboto_Regular title1, title2;
    int [] pinCombo = new int[4];
    int counter = 0;
    Boolean reEnter = false;
    Boolean reset, locked;
    ProgressBar progressBar;

    //receipt notification vals
    String orderOn, deliveredOn, restaurantName, orderID, restaurantPhone, key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadActivity();
    }

    private void loadActivity() {
        setContentView(R.layout.activity_security_pin);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {

            pinType = getIntent().getStringExtra("pinType");
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number

            myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);

            logout = findViewById(R.id.logOut);
            resetPin = findViewById(R.id.resetPin);
            resetPin.setVisibility(View.GONE); //TODO: implement reset pin mechanism later

            pin1 = findViewById(R.id.pin1);
            pin2 = findViewById(R.id.pin2);
            pin3 = findViewById(R.id.pin3);
            pin4 = findViewById(R.id.pin4);

            num0 = findViewById(R.id.num0);
            num1 = findViewById(R.id.num1);
            num2 = findViewById(R.id.num2);
            num3 = findViewById(R.id.num3);
            num4 = findViewById(R.id.num4);
            num5 = findViewById(R.id.num5);
            num6 = findViewById(R.id.num6);
            num7 = findViewById(R.id.num7);
            num8 = findViewById(R.id.num8);
            num9 = findViewById(R.id.num9);

            title1 = findViewById(R.id.title1);
            title2 = findViewById(R.id.title2);

            progressBar = findViewById(R.id.progressBar);
            progressBar.setVisibility(View.GONE);

            if(pinType.equals("setPin")){

                logout.setVisibility(View.GONE);
                //Check to see if i have an existing pin
                myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            reset = true;
                            oldPin = dataSnapshot.getValue(String.class);
                            title1.setText("Enter your OLD four");
                            title2.setText("digit security PIN");

                        } else {
                            reset = false;
                            title1.setText("Enter your preferred");
                            title2.setText("four digit security PIN");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            if(pinType.equals("login")){
                accountType = getIntent().getStringExtra("accType");
                logout.setVisibility(View.VISIBLE);
                title1.setText("Your pin is required to");
                title2.setText("login to your account");
                myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    try {
                                        locked = dataSnapshot.getValue(Boolean.class);
                                        if (locked == false) {
                                            //proceed to account
                                            if (accountType.equals("1")) {
                                                Intent slideactivity = new Intent(SecurityPin.this, CustomerActivity.class)
                                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                Bundle bndlanimation =
                                                        ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                                startActivity(slideactivity, bndlanimation);
                                            }
                                            if (accountType.equals("2")) {
                                                Intent slideactivity = new Intent(SecurityPin.this, RestaurantActivity.class)
                                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                Bundle bndlanimation =
                                                        ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                                startActivity(slideactivity, bndlanimation);
                                            }
                                            if (accountType.equals("3")) {
                                                Intent slideactivity = new Intent(SecurityPin.this, RiderActivity.class)
                                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                Bundle bndlanimation =
                                                        ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                                startActivity(slideactivity, bndlanimation);
                                            }
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
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            if(pinType.equals("resume")){
                logout.setVisibility(View.VISIBLE);
                title1.setText("Your pin is required to");
                title2.setText("login to your account");
            }

            num0.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pin(0);
                }
            });

            num1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pin(1);
                }
            });

            num2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pin(2);
                }
            });

            num3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pin(3);
                }
            });

            num4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pin(4);
                }
            });

            num5.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pin(5);
                }
            });

            num6.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pin(6);
                }
            });

            num7.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pin(7);
                }
            });

            num8.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pin(8);
                }
            });

            num9.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pin(9);
                }
            });

            resetPin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //implement reset machanism later
                }
            });

            logout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    myRef.child("appLocked").setValue(true);
                    stopService(new Intent(SecurityPin.this, ForegroundService.class));
                    stopService(new Intent(SecurityPin.this, TrackingService.class));
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(SecurityPin.this,SplashActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                }
            });
        }
    }

    private void pin(int i) {
        if(counter < 4){
            pinCombo[counter] = i;
            counter++;

            if(counter == 1){
                pin1.setColorFilter(ContextCompat.getColor(SecurityPin.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
            }

            if(counter == 2){
                pin2.setColorFilter(ContextCompat.getColor(SecurityPin.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
            }

            if(counter == 3){
                pin3.setColorFilter(ContextCompat.getColor(SecurityPin.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
            }

            if(counter == 4){
                pin4.setColorFilter(ContextCompat.getColor(SecurityPin.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
                myPin = ""+pinCombo[0]+""+pinCombo[1]+""+pinCombo[2]+""+pinCombo[3]+"";

                if(pinType.equals("setPin")){
                    if(reset == true){
                        if(oldPin.equals(myPin)){
                            oldPin = myPin; //update old pin to new pin
                            if(reEnter == false){
                                disableEnableButtons(false);
                                progressBar.setVisibility(View.VISIBLE);
                                myRef.child("pendingPin").setValue(myPin).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        progressBar.setVisibility(View.GONE);
                                        title1.setText("Enter your new preferred");
                                        title2.setText("four digit security PIN");
                                        SafeToast.makeText(SecurityPin.this, "ENTER NEW PIN!", Toast.LENGTH_LONG).show();
                                        disableEnableButtons(true);
                                        resetPinEnter(false);
                                        reset = false;
                                    }
                                });
                            }

                        } else {
                            finish();
                            Toast.makeText(this, "WRONG OLD PIN", Toast.LENGTH_LONG).show();
                        }
                    }

                    else {
                        if(reEnter == false){
                            disableEnableButtons(false);
                            progressBar.setVisibility(View.VISIBLE);
                            myRef.child("pendingPin").setValue(myPin).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    progressBar.setVisibility(View.GONE);
                                    title1.setText("Enter your preferred");
                                    title2.setText("four digit security PIN");
                                    SafeToast.makeText(SecurityPin.this, "RE-ENTER PIN!", Toast.LENGTH_LONG).show();
                                    disableEnableButtons(true);
                                    resetPinEnter(true);
                                }
                            });
                        }
                        else {
                            updatePIN();
                        }
                    }
                }

                if(pinType.equals("login")){
                    myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            loginPin = dataSnapshot.getValue(String.class);

                            if(myPin.equals(loginPin)){
                                locked = false;
                                myRef.child("appLocked").setValue(false);
                                //proceed to account
                                if(accountType.equals("1")){
                                    Intent slideactivity = new Intent(SecurityPin.this, CustomerActivity.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    Bundle bndlanimation =
                                            ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                    startActivity(slideactivity, bndlanimation);
                                }
                                if(accountType.equals("2")){
                                    Intent slideactivity = new Intent(SecurityPin.this, RestaurantActivity.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    Bundle bndlanimation =
                                            ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                    startActivity(slideactivity, bndlanimation);
                                }
                                if(accountType.equals("3")){
                                    Intent slideactivity = new Intent(SecurityPin.this, RiderActivity.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    Bundle bndlanimation =
                                            ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                    startActivity(slideactivity, bndlanimation);
                                }
                            } else {
                                locked = true;
                                myRef.child("appLocked").setValue(true);
                                pinCombo = new int[4];
                                resetPinEnter(false);
                                SafeToast.makeText(SecurityPin.this, "WRONG PIN!", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }

                if(pinType.equals("resume")){
                    myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            loginPin = dataSnapshot.getValue(String.class);

                            if(myPin.equals(loginPin)){
                                myRef.child("appLocked").setValue(false);
                                //proceed to account
                                locked = false;
                                finish();
                            } else {
                                myRef.child("appLocked").setValue(true);
                                locked = true;
                                pinCombo = new int[4];
                                resetPinEnter(false);
                                SafeToast.makeText(SecurityPin.this, "WRONG PIN!", Toast.LENGTH_LONG).show();
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

    private void updatePIN() {
        disableEnableButtons(false);
        progressBar.setVisibility(View.VISIBLE);
        myRef.child("pendingPin").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    String setPin = dataSnapshot.getValue(String.class);

                    if(setPin.equals(myPin)){
                        progressBar.setVisibility(View.GONE);
                        myRef.child("pin").setValue(myPin).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                myRef.child("pendingPin").removeValue();
                                finish();
                                SafeToast.makeText(SecurityPin.this, "PIN SET", Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        disableEnableButtons(true);
                        SafeToast.makeText(SecurityPin.this, "PIN MISMATCH", Toast.LENGTH_LONG).show();
                        resetPinEnter(false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void disableEnableButtons(boolean b) {
        num0.setEnabled(b);
        num1.setEnabled(b);
        num2.setEnabled(b);
        num3.setEnabled(b);
        num4.setEnabled(b);
        num5.setEnabled(b);
        num6.setEnabled(b);
        num7.setEnabled(b);
        num8.setEnabled(b);
        num9.setEnabled(b);
    }

    private void resetPinEnter(Boolean reenter) {
        reEnter = reenter;
        counter = 0;

        if(pinType.equals("setPin")){
            title1.setText("Enter your preferred");
            title2.setText("four digit security PIN");
        }

        if(pinType.equals("login")){
            title1.setText("Your pin is required to");
            title2.setText("login to your account");
        }

        pin1.setColorFilter(ContextCompat.getColor(SecurityPin.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
        pin2.setColorFilter(ContextCompat.getColor(SecurityPin.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
        pin3.setColorFilter(ContextCompat.getColor(SecurityPin.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
        pin4.setColorFilter(ContextCompat.getColor(SecurityPin.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            if (locked == true) {

                this.finishAffinity();
            }
        } catch (Exception e){
            Log.e(TAG, "onDestroy: ", e);
        }
    }
}
