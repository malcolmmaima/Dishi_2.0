package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
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
import com.malcolmmaima.dishi.Controller.Services.ForegroundService;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;

import io.fabric.sdk.android.services.common.SafeToast;

public class NotificationSettings extends AppCompatActivity {

    String TAG = "NotificationSettings";
    String myPhone;
    DatabaseReference myRef;
    Switch socialSwitch, orderSwitch, chatSwitch;
    FirebaseAuth mAuth;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);


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
                                    Intent slideactivity = new Intent(NotificationSettings.this, SecurityPin.class)
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

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Notifications");

        socialSwitch = findViewById(R.id.SocialNotifications);
        orderSwitch = findViewById(R.id.OrderNotifications);
        chatSwitch = findViewById(R.id.ChatNotifications);

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });

        //initialize and set values
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    UserModel myUserDetails = dataSnapshot.getValue(UserModel.class);
                    socialSwitch.setChecked(myUserDetails.getSocialNotification());
                    orderSwitch.setChecked(myUserDetails.getOrderNotification());
                    chatSwitch.setChecked(myUserDetails.getChatNotification());
                } catch (Exception e){
                    Log.e(TAG, "onDataChange: ", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        socialSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position

                if(isChecked == true && buttonView.isPressed()){
                    myRef.child("socialNotification").setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            socialSwitch.setChecked(false);
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    });
                }

                else if(isChecked == false && buttonView.isPressed()){
                    myRef.child("socialNotification").setValue(false).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            socialSwitch.setChecked(true);
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    });
                }

            }
        });

        orderSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position

                if(isChecked == true && buttonView.isPressed()){
                    myRef.child("orderNotification").setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            orderSwitch.setChecked(false);
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    });
                }

                else if(isChecked == false && buttonView.isPressed()){
                    myRef.child("orderNotification").setValue(false).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            orderSwitch.setChecked(true);
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    });
                }

            }
        });

        chatSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position

                if(isChecked == true && buttonView.isPressed()){
                    myRef.child("chatNotification").setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            chatSwitch.setChecked(false);
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    });
                }

                else if(isChecked == false && buttonView.isPressed()){
                    myRef.child("chatNotification").setValue(false).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Saved", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            chatSwitch.setChecked(true);
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(R.id.parentlayout), "Something went wrong", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    });
                }

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
                                Intent slideactivity = new Intent(NotificationSettings.this, SecurityPin.class)
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


    public void startNotificationService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        serviceIntent.putExtra("title", "Dishi");
        serviceIntent.putExtra("message", "Welcome to Dishi");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            ContextCompat.startForegroundService(this, serviceIntent);

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        startNotificationService(); //restart notification service for new changes to take effect
    }
}
