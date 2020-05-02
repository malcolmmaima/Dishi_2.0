package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.R;

import io.fabric.sdk.android.services.common.SafeToast;

public class AccountSettings extends AppCompatActivity {

    RelativeLayout deliveryCharge;
    DatabaseReference myRef;
    String myPhone, accountType;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            deliveryCharge = findViewById(R.id.deliveryCharge);

            Toolbar topToolBar = findViewById(R.id.toolbar);
            setSupportActionBar(topToolBar);

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            setTitle("Account");

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number

            //Set fb database reference
            myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);

            //Back button on toolbar
            topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish(); //Go back to previous activity
                }
            });

            /**
             * Show this setting to restaurant account types only
             */
            myRef.child("account_type").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        accountType = dataSnapshot.getValue(String.class);

                        if (!accountType.equals("2")){
                            deliveryCharge.setVisibility(View.GONE);
                        }

                        else {
                            deliveryCharge.setVisibility(View.VISIBLE);
                        }

                    } catch (Exception e){

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            deliveryCharge.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent slideactivity = new Intent(AccountSettings.this, DeliverCharges.class);
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                    startActivity(slideactivity, bndlanimation);
                }
            });
        }
    }
}
