package com.malcolmmaima.dishi.View;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.R;

import com.malcolmmaima.dishi.Controller.PreferenceManager;

public class SplashActivity extends AppCompatActivity {

    private PreferenceManager prefManager;
    String myPhone;
    private FirebaseAuth mAuth;
    ProgressBar progressBar;

    private static final int PERMISSIONS_REQUEST = 100;

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

        new CountDownTimer(60000, 10000) { //Check connection status every 10 seconds and prompt user if not connected
            public void onTick(long millisUntilFinished) {
                //Toast.makeText(getContext(), "seconds remaining: " + millisUntilFinished / 1000, Toast.LENGTH_SHORT).show();
                checkConnection();
            }

            public void onFinish() {
                //Toast.makeText(getContext(), "done!", Toast.LENGTH_SHORT).show();
            }
        }.start();

        mAuth = FirebaseAuth.getInstance();

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
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                myPhone = user.getPhoneNumber(); //Current logged in user phone number

                FirebaseDatabase db = FirebaseDatabase.getInstance();
                final DatabaseReference dbRef = db.getReference("users/" + myPhone);

                //Check whether user is verified, if true send them directly to MyAccountRestaurant
                dbRef.child("verified").addValueEventListener(new ValueEventListener() {
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

                        else if (verified.toString().equals("true")) { //Will need to check account type as well, then redirect to account type

                            //User is verified, so we need to check their account type and redirect accordingly
                            dbRef.child("account_type").addValueEventListener(new ValueEventListener() {
                                @Override public void onDataChange(DataSnapshot dataSnapshot) {
                                    String account_type = dataSnapshot.getValue(String.class);
                                    //String account_type = Integer.toString(acc_type);

                                    if(account_type.equals("1")){ //Customer account
                                        try {
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(SplashActivity.this, "Customer Account", Toast.LENGTH_LONG).show();
//                                            Intent slideactivity = new Intent(SplashActivity.this, MyAccountCustomer.class)
//                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                                            Bundle bndlanimation =
//                                                    ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
//                                            startActivity(slideactivity, bndlanimation);
                                        } catch (Exception e){

                                        }
                                    }

                                    else if (account_type.equals("2")){ //Provider Restaurant account
                                        try {
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(SplashActivity.this, "Provider Account", Toast.LENGTH_LONG).show();
//                                            Intent slideactivity = new Intent(SplashActivity.this, MyAccountRestaurant.class)
//                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                                            Bundle bndlanimation =
//                                                    ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation,R.anim.animation2).toBundle();
//                                            startActivity(slideactivity, bndlanimation);
                                        } catch (Exception e){

                                        }
                                    }

                                    else if (account_type.equals("3")){ //Nduthi account
                                        try {
                                            progressBar.setVisibility(View.GONE);
                                            //Slide to new activity
                                            Toast.makeText(SplashActivity.this, "Nduthi Account", Toast.LENGTH_LONG).show();
//                                            Intent slideactivity = new Intent(SplashActivity.this, MyAccountNduthi.class)
//                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                                            Bundle bndlanimation =
//                                                    ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation,R.anim.animation2).toBundle();
//                                            startActivity(slideactivity, bndlanimation);
                                        } catch (Exception e){

                                        }

                                    }

                                    else if (account_type.equals("X")){
                                        Toast.makeText(SplashActivity.this, "Your account has been disabled", Toast.LENGTH_LONG).show();

                                    }

                                    else { // Others
                                        Toast.makeText(SplashActivity.this, "'Others' account still in development", Toast.LENGTH_LONG).show();
                                    }

                                    //Debugging purposes
                                    //Toast.makeText(SplashActivity.this, "Account type: " + account_type, Toast.LENGTH_LONG).show();
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

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }
    public void checkConnection(){
        if(isOnline()){
            //Toast.makeText(SplashActivity.this, "You are connected to Internet", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.VISIBLE);
        }else{
            progressBar.setVisibility(View.GONE);
            Toast.makeText(SplashActivity.this, "You are not connected to the Internet", Toast.LENGTH_SHORT).show();
        }
    }

}
