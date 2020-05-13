package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

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
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;

import io.fabric.sdk.android.services.common.SafeToast;

public class PrivacySecurity extends AppCompatActivity {

    String TAG = "PrivacySecurity";
    String myPhone;
    DatabaseReference myRef;
    ValueEventListener myRefListener;
    Switch shareOrders, syncContacts;
    RelativeLayout setViewPhone, blockedAccounts, myLocationSettings, accountPrivacy, accountPin, loginActivity;
    LinearLayout shareOrdersOption;
    MyTextView_Roboto_Regular phoneVisibilityTxt, accountPrivacyTxt, pinStatus;
    View setViewPhoneBorder, shareOrdersOptionBorder;
    int chckdItem = 0;
    int chckItem2 = 0;
    AlertDialog alertPhone, alertPrivacy;
    UserModel myUserDetails;
    FirebaseAuth mAuth;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_security);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Privacy & Security");

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber();
            myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
            myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                Boolean locked = dataSnapshot.getValue(Boolean.class);

                                if(locked == true){
                                    Intent slideactivity = new Intent(PrivacySecurity.this, SecurityPin.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    slideactivity.putExtra("pinType", "resume");
                                    startActivity(slideactivity);
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

        shareOrders = findViewById(R.id.shareOrdersSwitch);
        syncContacts = findViewById(R.id.syncContacts);
        setViewPhone = findViewById(R.id.setViewPhone);
        setViewPhone.setVisibility(View.GONE);

        blockedAccounts = findViewById(R.id.blockedAccounts);
        myLocationSettings = findViewById(R.id.myLocationSettings);
        accountPrivacy = findViewById(R.id.accountPrivacy);
        accountPin = findViewById(R.id.accountPin);
        pinStatus = findViewById(R.id.pinStatus);
        loginActivity = findViewById(R.id.loginActivity);
        accountPrivacyTxt = findViewById(R.id.accountPrivacyTxt);
        phoneVisibilityTxt = findViewById(R.id.phoneVisibilityTxt);
        setViewPhoneBorder = findViewById(R.id.setViewPhoneBorder);
        setViewPhoneBorder.setVisibility(View.GONE);

        shareOrdersOptionBorder = findViewById(R.id.shareOrdersOptionBorder);
        shareOrdersOptionBorder.setVisibility(View.GONE);

        shareOrdersOption = findViewById(R.id.shareOrdersOption);
        shareOrdersOption.setVerticalGravity(View.GONE);

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });

        myRefListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                myUserDetails = dataSnapshot.getValue(UserModel.class);
                try {
                    shareOrders.setChecked(myUserDetails.getShareOrders());
                    syncContacts.setChecked(myUserDetails.getSyncContacts());
                } catch (Exception er){
                    Log.e(TAG, "onDataChange: ", er);
                }

                try {
                    if (myUserDetails.getPhoneVisibility().equals("everyone")) {
                        chckdItem = 0;
                        phoneVisibilityTxt.setText("Everyone");
                    }

                    if (myUserDetails.getPhoneVisibility().equals("mutual")) {
                        chckdItem = 1;
                        phoneVisibilityTxt.setText("Mutual");
                    }

                    if (myUserDetails.getPhoneVisibility().equals("none")) {
                        chckdItem = 2;
                        phoneVisibilityTxt.setText("None");
                    }

                    if (myUserDetails.getPhoneVisibility() == null || myUserDetails.getPhoneVisibility().isEmpty()) {
                        chckdItem = 2;
                        phoneVisibilityTxt.setText("None");
                    }
                } catch (Exception e){
                    Log.e(TAG, "onDataChange: ", e);
                }

                try {
                    if(!myUserDetails.getAccount_type().equals("1")){
                        setViewPhone.setVisibility(View.GONE); //Only show this settings option to customer accounts
                        setViewPhoneBorder.setVisibility(View.GONE);

                        shareOrdersOptionBorder.setVisibility(View.GONE);
                        shareOrdersOption.setVisibility(View.GONE);
                    } else {
                        setViewPhone.setVisibility(View.VISIBLE);
                        setViewPhoneBorder.setVisibility(View.VISIBLE);

                        shareOrdersOptionBorder.setVisibility(View.VISIBLE);
                        shareOrdersOption.setVisibility(View.VISIBLE);
                    }
                } catch (Exception err){
                    Log.e(TAG, "onDataChange: ", err);
                }

                try {
                    if(myUserDetails.getAccountPrivacy().equals("public")){
                        chckItem2 = 0;
                        accountPrivacyTxt.setText("Public");
                    }

                    if(myUserDetails.getAccountPrivacy().equals("private")){
                        chckItem2 = 1;
                        accountPrivacyTxt.setText("Private");
                    }
                } catch (Exception errr){
                    Log.e(TAG, "onDataChange: ", errr);
                }

                try {
                    if (dataSnapshot.child("pin").exists()) {
                        pinStatus.setText("****");
                    } else {
                        pinStatus.setText("Not set");
                    }
                } catch (Exception e){
                    Log.e(TAG, "onDataChange: ", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myRef.addValueEventListener(myRefListener);

        //switches
        shareOrders.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position

                if(isChecked == true && buttonView.isPressed()){
                    myRef.child("shareOrders").setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            shareOrders.setChecked(false);
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    });
                }

                else if(isChecked == false && buttonView.isPressed()){
                    myRef.child("shareOrders").setValue(false).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            shareOrders.setChecked(true);
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    });
                }

            }
        });

        syncContacts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position

                if(isChecked == true && buttonView.isPressed()){
                    myRef.child("syncContacts").setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            syncContacts.setChecked(false);
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    });
                }

                else if(isChecked == false && buttonView.isPressed()){
                    myRef.child("syncContacts").setValue(false).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            syncContacts.setChecked(false);
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    });
                }
            }
        });

        //others
        setViewPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(PrivacySecurity.this);
                String[] items = {"Everyone","Mutual Follows","None"};

                int checkedItem = chckdItem; //set to value from db
                alertDialog.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                myRef.child("phoneVisibility").setValue("everyone").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Snackbar snackbar = Snackbar
                                                .make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG);
                                        snackbar.show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Snackbar snackbar = Snackbar
                                                .make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                                        snackbar.show();
                                    }
                                });
                                break;
                            case 1:
                                myRef.child("phoneVisibility").setValue("mutual").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Snackbar snackbar = Snackbar
                                                .make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG);
                                        snackbar.show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Snackbar snackbar = Snackbar
                                                .make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                                        snackbar.show();
                                    }
                                });
                                break;
                            case 2:
                                myRef.child("phoneVisibility").setValue("none").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Snackbar snackbar = Snackbar
                                                .make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG);
                                        snackbar.show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Snackbar snackbar = Snackbar
                                                .make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                                        snackbar.show();
                                    }
                                });

                                AlertDialog alertUser = new AlertDialog.Builder(PrivacySecurity.this)
                                        .setMessage("Please note that vendors will have access to your phone number during active orders :-)")
                                        //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                                        .setCancelable(false)
                                        //set three option buttons
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {

                                            }
                                        }).create();
                                alertUser.setCancelable(false);
                                alertUser.show();
                                break;
                        }
                        alertPhone.dismiss();
                    }
                });
                alertPhone = alertDialog.create();
                alertPhone.setCancelable(true);
                alertPhone.show();
            }
        });

        blockedAccounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent blockedActivity = new Intent(PrivacySecurity.this, MyBlockedAccounts.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(blockedActivity);
            }
        });

        myLocationSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (myUserDetails.getAccount_type().equals("2")) {
                        Intent locationActivity = new Intent(PrivacySecurity.this, LocationSettings.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(locationActivity);
                    } else {
                        Intent blockedActivity = new Intent(PrivacySecurity.this, MyLocation.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(blockedActivity);
                    }
                } catch (Exception e){
                    Log.e(TAG, "onClick: ", e);
                    Snackbar snackbar = Snackbar
                            .make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            }
        });

        accountPrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(PrivacySecurity.this);
                String[] items = {"Public","Private"};

                int checkedItem = chckItem2; //set to value from db
                alertDialog.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                myRef.child("accountPrivacy").setValue("public").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Snackbar snackbar = Snackbar
                                                .make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG);
                                        snackbar.show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Snackbar snackbar = Snackbar
                                                .make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                                        snackbar.show();
                                    }
                                });
                                break;
                            case 1:
                                myRef.child("accountPrivacy").setValue("private").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Snackbar snackbar = Snackbar
                                                .make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG);
                                        snackbar.show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Snackbar snackbar = Snackbar
                                                .make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                                        snackbar.show();
                                    }
                                });
                                break;

                        }
                        alertPrivacy.dismiss();
                    }
                });
                alertPrivacy = alertDialog.create();
                alertPrivacy.setCancelable(true);
                alertPrivacy.show();
            }
        });

        accountPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent securityPin = new Intent(PrivacySecurity.this, SecurityPin.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                securityPin.putExtra("pinType", "setPin");
                startActivity(securityPin);
            }
        });

        loginActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent blockedActivity = new Intent(PrivacySecurity.this, DeviceLoginActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(blockedActivity);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Boolean locked = dataSnapshot.getValue(Boolean.class);

                            if(locked == true){
                                Intent slideactivity = new Intent(PrivacySecurity.this, SecurityPin.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                slideactivity.putExtra("pinType", "resume");
                                startActivity(slideactivity);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            myRef.removeEventListener(myRefListener);
        } catch (Exception e){
            Log.e(TAG, "onDestroy: ", e);
        }
    }
}
