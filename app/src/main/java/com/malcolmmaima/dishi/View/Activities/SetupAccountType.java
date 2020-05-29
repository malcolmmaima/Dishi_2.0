package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.malcolmmaima.dishi.Controller.Services.ForegroundService;
import com.malcolmmaima.dishi.Controller.Services.TrackingService;
import com.malcolmmaima.dishi.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.fabric.sdk.android.services.common.SafeToast;

public class SetupAccountType extends AppCompatActivity {

    private String TAG;
    CardView customer, restaurant, rider;
    String accountType, myPhone;
    Button logout;

    /**
     * Firebase
     */
    private DatabaseReference myRef;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_account_type);

        TAG = "SetupAccountType";
        accountType = "0";
        progressDialog = new ProgressDialog(SetupAccountType.this);

        initWidgets();

        //Set fb database reference
        myRef = FirebaseDatabase.getInstance().getReference("users");

        //get auth state
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        //User is logged in
        if(mAuth.getInstance().getCurrentUser() != null) {
            /**
             * Customer account
             */
            customer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final AlertDialog proceedAcc = new AlertDialog.Builder(SetupAccountType.this)
                            .setMessage("Proceed as customer?")
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //SafeToast.makeText(SetupAccountType.this, "Customer account", Toast.LENGTH_SHORT).show();
                                    accountType = "1";

                                    try {
                                        progressDialog.setTitle("Saving...");
                                        progressDialog.setCancelable(false);
                                        progressDialog.show();
                                    } catch (Exception e){

                                    }
                                    //Set account type in database under logged in user's node
                                    myRef.child(myPhone).child("account_type").setValue(accountType).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            try {
                                                progressDialog.dismiss();
                                            } catch (Exception e){

                                            }

                                            //Set signup date and make sure it is posted to the database before loading customer account
                                            String date = getDate();
                                            myRef.child(myPhone).child("signupDate").setValue(date).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    //Load customer account
                                                    Intent slideactivity = new Intent(SetupAccountType.this, CustomerActivity.class)
                                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                    Bundle bndlanimation =
                                                            null;
                                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                                                        bndlanimation = ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                                    }
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                                        try {
                                                            startActivity(slideactivity, bndlanimation);
                                                        } catch (Exception e){

                                                        }
                                                    }
                                                }
                                            });
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            //error
                                            SafeToast.makeText(SetupAccountType.this, "Something wrong occurred", Toast.LENGTH_SHORT).show();
                                            progressDialog.dismiss();
                                        }
                                    });

                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Do nothing
                                }
                            })
                            .create();
                    proceedAcc.show();

                }

            });


            /**
             * Restaurant account
             */
            restaurant.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    final AlertDialog proceedAcc = new AlertDialog.Builder(SetupAccountType.this)
                            //set message, title, and icon
                            .setMessage("Proceed as vendor?")
                            //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //SafeToast.makeText(SetupAccountType.this, "Restaurant account", Toast.LENGTH_SHORT).show();
                                    accountType = "2";

                                    try {
                                        progressDialog.setTitle("Saving...");
                                        progressDialog.setCancelable(false);
                                        progressDialog.show();
                                    } catch (Exception e){

                                    }
                                    //Set account type in database under logged in user's node
                                    myRef.child(myPhone).child("account_type").setValue(accountType).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            try {
                                                progressDialog.dismiss();
                                            } catch (Exception e){

                                            }

                                            //Set signup date and make sure it is posted to the database before loading customer account
                                            String date = getDate();
                                            myRef.child(myPhone).child("signupDate").setValue(date);
                                            //Load account
                                            Intent slideactivity = new Intent(SetupAccountType.this, VendorActivity.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            Bundle bndlanimation =
                                                    null;
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                                                bndlanimation = ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                            }
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                                try {
                                                    startActivity(slideactivity, bndlanimation);
                                                } catch (Exception e){

                                                }
                                            }
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            //error
                                            SafeToast.makeText(SetupAccountType.this, "Something wrong occurred", Toast.LENGTH_SHORT).show();
                                            progressDialog.dismiss();
                                        }
                                    });
                                }
                            }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //do nothing

                                }
                            })//setNegativeButton

                            .create();
                    proceedAcc.show();
                }
            });

            /**
             * Rider Account
             */
            rider.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    final AlertDialog proceedAcc = new AlertDialog.Builder(SetupAccountType.this)
                            //set message, title, and icon
                            .setMessage("Proceed as rider?")
                            //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                            //set three option buttons
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //SafeToast.makeText(SetupAccountType.this, "Rider account", Toast.LENGTH_SHORT).show();
                                    accountType = "3";

                                    try {
                                        progressDialog.setTitle("Saving...");
                                        progressDialog.setCancelable(false);
                                        progressDialog.show();
                                    } catch (Exception e){

                                    }
                                    //Set account type in database under logged in user's node
                                    myRef.child(myPhone).child("account_type").setValue(accountType).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            try {
                                                progressDialog.dismiss();
                                            } catch (Exception e){

                                            }

                                            //Set signup date and make sure it is posted to the database before loading customer account
                                            String date = getDate();
                                            myRef.child(myPhone).child("signupDate").setValue(date).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    //Load customer account
                                                    Intent slideactivity = new Intent(SetupAccountType.this, RiderActivity.class)
                                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                    Bundle bndlanimation =
                                                            null;
                                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                                                        bndlanimation = ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                                    }
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                                        try {
                                                            startActivity(slideactivity, bndlanimation);
                                                        } catch (Exception e){

                                                        }
                                                    }
                                                }
                                            });
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            //error
                                            SafeToast.makeText(SetupAccountType.this, "Something wrong occurred", Toast.LENGTH_SHORT).show();
                                            progressDialog.dismiss();
                                        }
                                    });
                                }
                            }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //do nothing

                                }
                            })//setNegativeButton

                            .create();
                    proceedAcc.show();

                }
            });

            logout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final AlertDialog logout = new AlertDialog.Builder(SetupAccountType.this)
                            .setMessage("Logout?")
                            //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                            .setCancelable(false)
                            //set three option buttons
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //Log out
                                    stopService(new Intent(SetupAccountType.this, ForegroundService.class));
                                    stopService(new Intent(SetupAccountType.this, TrackingService.class));
                                    FirebaseAuth.getInstance().signOut();
                                    startActivity(new Intent(SetupAccountType.this,SplashActivity.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                    finish();
                                }
                            })//setPositiveButton

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Do nothing
                                }
                            })

                            .create();
                    logout.show();
                }
            });
        }

        //You're not logged in
        else {
            SafeToast.makeText(this, "You're not logged in", Toast.LENGTH_LONG).show();
            //Slide to new activity
            Intent slideactivity = new Intent(SetupAccountType.this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Bundle bndlanimation =
                    null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                bndlanimation = ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                try {
                    startActivity(slideactivity, bndlanimation);
                } catch (Exception e){

                }
            }
        }


    }

    /**
     * Initialize the activity widgets
     */
    private void initWidgets(){
        Log.d(TAG, "initWidgets: Initializing Widgets.");
        customer = findViewById(R.id.customerAccountCard);
        restaurant = findViewById(R.id.restaurantAccountCard);
        rider = findViewById(R.id.riderCard);
        logout = findViewById(R.id.btn_logout);

    }

    private String getDate() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        TimeZone timeZone = TimeZone.getDefault();
        Calendar calendar = Calendar.getInstance(timeZone);
        String time = date+ ":" +
                String.format("%02d" , calendar.get(Calendar.HOUR_OF_DAY))+":"+
                String.format("%02d" , calendar.get(Calendar.MINUTE))+":"+
                String.format("%02d" , calendar.get(Calendar.SECOND)) +":"+
                timeZone.getDisplayName(false, TimeZone.SHORT);

        return time;
    }
}
