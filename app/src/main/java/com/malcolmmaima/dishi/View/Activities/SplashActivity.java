package com.malcolmmaima.dishi.View.Activities;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnFailureListener;
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

import com.malcolmmaima.dishi.Controller.PreferenceManager;

import io.fabric.sdk.android.Fabric;

public class SplashActivity extends AppCompatActivity {

    private PreferenceManager prefManager;
    String myPhone;
    private FirebaseAuth mAuth;
    ProgressBar progressBar;

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);


//        Temporarily disable disk persistence. uncomment to enable
//        try {
//            //Enable disk persistence
//            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
//        } catch (Exception e){
//
//        }

        mAuth = FirebaseAuth.getInstance();

        Fabric.with(this, new Crashlytics());

        // Checking for first time launch
        prefManager = new PreferenceManager(this);
        if (!prefManager.isFirstTimeLaunch()) {
            if (mAuth.getInstance().getCurrentUser() == null || mAuth.getInstance().getCurrentUser().getPhoneNumber() == null) {
                progressBar.setVisibility(View.GONE);
                //User is not signed in, send them back to verification page
                //Toast.makeText(this, "Not logged in!", Toast.LENGTH_LONG).show();
                startActivity(new Intent(SplashActivity.this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));//Load Main Activity and clear activity stack
                finish();

            } else {
                //get device id
                final String android_id = Settings.Secure.getString(this.getContentResolver(),
                        Settings.Secure.ANDROID_ID);


                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                myPhone = user.getPhoneNumber(); //Current logged in user phone number

                FirebaseDatabase db = FirebaseDatabase.getInstance();
                final DatabaseReference dbRef = db.getReference("users/" + myPhone);

                //Compare device id of current device id and previous logged device id,
                //if not same prompt logout device. On fresh login will set new device id. limit account logins to one device
                dbRef.child("device_id").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            String fetchedId = dataSnapshot.getValue(String.class);
                            //device id's do not match, prompt to logout atleast one device
                            if(!android_id.equals(fetchedId)){
                                //Log out
                                Toast.makeText(SplashActivity.this, "You're logged in a different device!", Toast.LENGTH_LONG).show();
                                stopService(new Intent(SplashActivity.this, TrackingService.class));
                                FirebaseAuth.getInstance().signOut();
                                startActivity(new Intent(SplashActivity.this,MainActivity.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                            }
                        }

                        else {
                            dbRef.child("device_id").setValue(android_id);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
                //Check whether user is verified, if true send them directly to MyAccountRestaurant
                dbRef.child("verified").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String verified = dataSnapshot.getValue(String.class);

                        //Toast.makeText(SplashActivity.this, "Verified: " + verified, Toast.LENGTH_LONG).show();
                        if(verified == null){
                            verified = "false";

                            dbRef.child("verified").setValue(verified).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    progressBar.setVisibility(View.GONE);
                                    Intent mainActivity = new Intent(SplashActivity.this, SetupProfile.class);
                                    mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);//Load Main Activity and clear activity stack
                                    startActivity(mainActivity);
                                }
                            })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Write failed
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(SplashActivity.this, "Error!", Toast.LENGTH_LONG).show();
                                        }
                                    });

                        }

                        else if (verified.equals("true")) { //Will need to check account type as well, then redirect to account type

                            //User is verified, so we need to check their account type and redirect accordingly
                            dbRef.child("account_type").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override public void onDataChange(DataSnapshot dataSnapshot) {
                                    final String account_type = dataSnapshot.getValue(String.class);
                                    //String account_type = Integer.toString(acc_type);

                                    if(account_type == null){
                                        //Toast.makeText(SplashActivity.this, "account type null", Toast.LENGTH_SHORT).show();
                                        //Set account type to 0 if setting up no complete
                                        dbRef.child("account_type").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {

                                                    //Toast.makeText(SplashActivity.this, "You have not finished setting up your account!", Toast.LENGTH_LONG).show();

                                                    Intent slideactivity = new Intent(SplashActivity.this, SetupAccountType.class)
                                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                    Bundle bndlanimation =
                                                            ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation,R.anim.animation2).toBundle();
                                                    getApplicationContext().startActivity(slideactivity, bndlanimation);
                                            }
                                        });
                                    }

                                    else {
                                        if(account_type.equals("1")){ //Customer account
                                            try {
                                                //Toast.makeText(SplashActivity.this, "Customer Account", Toast.LENGTH_LONG).show();
                                                Intent slideactivity = new Intent(SplashActivity.this, CustomerActivity.class)
                                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                Bundle bndlanimation =
                                                        ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                                startActivity(slideactivity, bndlanimation);
                                            } catch (Exception e){

                                            }
                                        }

                                        else if (account_type.equals("2")){ //Provider Restaurant account
                                            try {
                                                progressBar.setVisibility(View.GONE);
                                                //Toast.makeText(SplashActivity.this, "Provider Account", Toast.LENGTH_LONG).show();
                                            Intent slideactivity = new Intent(SplashActivity.this, RestaurantActivity.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            Bundle bndlanimation =
                                                    ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation,R.anim.animation2).toBundle();
                                            startActivity(slideactivity, bndlanimation);
                                            } catch (Exception e){

                                            }
                                        }

                                        else if (account_type.equals("3")){ //Nduthi account
                                            try {
                                                progressBar.setVisibility(View.GONE);
                                                //Slide to new activity
                                                //Toast.makeText(SplashActivity.this, "Rider Account", Toast.LENGTH_LONG).show();
                                            Intent slideactivity = new Intent(SplashActivity.this, RiderActivity.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            Bundle bndlanimation =
                                                    ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation,R.anim.animation2).toBundle();
                                            startActivity(slideactivity, bndlanimation);
                                            } catch (Exception e){

                                            }

                                        }

                                        else if (account_type.equals("0")){
                                            //Toast.makeText(SplashActivity.this, "You have not finished setting up your account!", Toast.LENGTH_LONG).show();

                                            Intent slideactivity = new Intent(SplashActivity.this, SetupAccountType.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            Bundle bndlanimation =
                                                    ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation,R.anim.animation2).toBundle();
                                            getApplicationContext().startActivity(slideactivity, bndlanimation);
                                        }

                                        else if (account_type.equals("X")){
                                            Snackbar snackbar = Snackbar
                                                    .make((LinearLayout) findViewById(R.id.parentlayout), "Your account has been disabled", Snackbar.LENGTH_LONG);

                                            snackbar.show();
                                        }

                                        else { // Others
                                            Toast.makeText(SplashActivity.this, "'Others' account still in development", Toast.LENGTH_LONG).show();
                                        }
                                    }

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    //DB error, try again...if fails login again
                                    //

                                }
                            });

                        } else {
                            progressBar.setVisibility(View.GONE);
                            //User is not verified so have them verify their profile details first
                            startActivity(new Intent(SplashActivity.this, SetupProfile.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));//Load Main Activity and clear activity stack
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

            }
        }

        else {
            progressBar.setVisibility(View.GONE);
            startActivity(new Intent(SplashActivity.this, WelcomeActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }

    }
}
