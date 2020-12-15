package com.malcolmmaima.dishiapp.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishiapp.R;



public class SupportPin extends AppCompatActivity {
    FirebaseUser user;
    String myPhone;
    DatabaseReference supportRef;
    FirebaseAuth mAuth;
    String TAG = "SupportPin";
    String myPin, loginPin;
    ImageView pin1, pin2, pin3, pin4;
    LinearLayout num1, num2, num3, num4, num5, num6, num7, num8, num9, num0;
    int [] pinCombo = new int[4];
    int counter = 0;
    Boolean reEnter = false;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadActivity();
    }

    private void loadActivity() {
        setContentView(R.layout.activity_support_pin);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {

            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number

            supportRef = FirebaseDatabase.getInstance().getReference("support");


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

            progressBar = findViewById(R.id.progressBar);
            progressBar.setVisibility(View.GONE);

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

        }

    private void pin(int i) {
        if(counter < 4){
            pinCombo[counter] = i;
            counter++;

            if(counter == 1){
                pin1.setColorFilter(ContextCompat.getColor(SupportPin.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
            }

            if(counter == 2){
                pin2.setColorFilter(ContextCompat.getColor(SupportPin.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
            }

            if(counter == 3){
                pin3.setColorFilter(ContextCompat.getColor(SupportPin.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
            }

            if(counter == 4){
                progressBar.setVisibility(View.VISIBLE);
                pin4.setColorFilter(ContextCompat.getColor(SupportPin.this, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
                myPin = ""+pinCombo[0]+""+pinCombo[1]+""+pinCombo[2]+""+pinCombo[3]+"";

                supportRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        loginPin = dataSnapshot.getValue(String.class);
                        progressBar.setVisibility(View.GONE);

                        if(myPin.equals(loginPin)){
                            //Proceed to support dashboard
                            finish();
                            Intent slideactivity = new Intent(SupportPin.this, SupportDashboard.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            Bundle bndlanimation =
                                    ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                            startActivity(slideactivity, bndlanimation);

                        } else {
                            progressBar.setVisibility(View.GONE);
                            pinCombo = new int[4];
                            resetPinEnter(false);
                            Toast.makeText(SupportPin.this, "WRONG PIN!", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        }

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

        pin1.setColorFilter(ContextCompat.getColor(SupportPin.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
        pin2.setColorFilter(ContextCompat.getColor(SupportPin.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
        pin3.setColorFilter(ContextCompat.getColor(SupportPin.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
        pin4.setColorFilter(ContextCompat.getColor(SupportPin.this, R.color.grey), android.graphics.PorterDuff.Mode.SRC_IN);
    }

}
