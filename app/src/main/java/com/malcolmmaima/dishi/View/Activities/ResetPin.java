package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;

import android.app.ActivityOptions;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
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
import com.malcolmmaima.dishi.Controller.Services.ForegroundService;
import com.malcolmmaima.dishi.Controller.Services.TrackingService;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;



public class ResetPin extends AppCompatActivity {
    String TAG = "ResetPin";
    EditText phoneNumber, emailAddress;
    AppCompatButton resetMyPin;
    String myPhone;
    DatabaseReference myRef;
    FirebaseAuth mAuth;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_pin);

        phoneNumber = findViewById(R.id.myPhoneNumber);
        emailAddress = findViewById(R.id.myEmailAddress);
        resetMyPin = findViewById(R.id.resetMyPin);
        resetMyPin.setVisibility(View.GONE);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            resetMyPin.setVisibility(View.VISIBLE);
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number

            //Set fb database reference
            myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
        }

        resetMyPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(CheckFieldValidation()){
                    myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            try {
                                UserModel myDetails = dataSnapshot.getValue(UserModel.class);

                                String phone = phoneNumber.getText().toString().trim();
                                String email = emailAddress.getText().toString().trim();

                                if (myPhone.equals(phone) && myDetails.getEmail().toLowerCase().equals(email.toLowerCase())) {
                                    //remove pin
                                    myRef.child("pin").removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            myRef.child("appLocked").removeValue();
                                            stopService(new Intent(ResetPin.this, ForegroundService.class));
                                            stopService(new Intent(ResetPin.this, TrackingService.class));
                                            FirebaseAuth.getInstance().signOut();
                                            startActivity(new Intent(ResetPin.this, SplashActivity.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                            Toast.makeText(ResetPin.this, "Your PIN has been removed!", Toast.LENGTH_LONG).show();
                                        }
                                    });
                                } else {
                                    Toast.makeText(ResetPin.this, "Make sure to include country code!", Toast.LENGTH_SHORT).show();
                                    Snackbar snackbar = Snackbar
                                            .make(findViewById(R.id.parentlayout), "Invalid details", Snackbar.LENGTH_LONG);
                                    snackbar.show();
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
        });

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Reset PIN");

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private boolean CheckFieldValidation(){
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

        boolean valid = true;

        if(phoneNumber.getText().toString().trim().equals("")){
            phoneNumber.setError("Can't be Empty");
            valid =false;
        }

        if(emailAddress.getText().toString().equals("")){
            emailAddress.setError("Cant't be empty");
            valid =false;
        }

        if(emailAddress.equals(emailPattern)){
            emailAddress.setError("Invalid Email");
            valid =false;
        }

        return valid;
    }
}
