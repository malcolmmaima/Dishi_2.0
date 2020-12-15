package com.malcolmmaima.dishiapp.View.Activities;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.NotificationChannel;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishiapp.Controller.Services.ForegroundService;
import com.malcolmmaima.dishiapp.Controller.Services.TrackingService;
import com.malcolmmaima.dishiapp.Model.MyDeviceModel;
import com.malcolmmaima.dishiapp.R;

import com.malcolmmaima.dishiapp.Controller.Utils.PreferenceManager;

public class SplashActivity extends AppCompatActivity {

    String TAG = "SplashActivity";
    String myPhone;
    ProgressBar progressBar;

    NotificationChannel channel;
    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    @Override
    protected void onStart() {
        super.onStart();
        /**
         * onStart check to see if ForegroundService is already running, if not then start
         */
        try {
            Boolean serviceRunning = isMyServiceRunning(ForegroundService.class);

            if (serviceRunning != true) {
                startNotificationService();
            }
        } catch (Exception e){

        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //FirebaseCrashlytics.getInstance();

        try {
            Boolean serviceRunning = isMyServiceRunning(ForegroundService.class);

            if (serviceRunning != true) {
                startNotificationService();
            }
        } catch (Exception e){

        }

        loadSplash();
    }

    private void loadSplash() {
        setContentView(R.layout.activity_splash);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        // Checking for first time launch
        PreferenceManager prefManager = new PreferenceManager(this);
        if (!prefManager.isFirstTimeLaunch()) {

            if (mAuth.getInstance().getCurrentUser() == null || mAuth.getInstance().getCurrentUser().getPhoneNumber() == null) {
                progressBar.setVisibility(View.GONE);
                //User is not signed in, send them back to verification page
                //Toast.makeText(this, "Not logged in!", Toast.LENGTH_LONG).show();
                startActivity(new Intent(SplashActivity.this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));//Load Main Activity and clear activity stack
                //stopNotificationService(); //commented out... bugfix.. crashing in api 26+

            } else { //Are logged in
                //get device id
                final String android_id = Settings.Secure.getString(this.getContentResolver(),
                        Settings.Secure.ANDROID_ID);

                DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference("admin");
                adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        try {
                            Boolean maintenance = dataSnapshot.child("maintenance").getValue(Boolean.class);

                            if (maintenance == true) {
                                progressBar.setVisibility(View.GONE);
                                Intent mainActivity = new Intent(SplashActivity.this, SystemMaintenance.class);
                                mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);//Load Main Activity and clear activity stack
                                startActivity(mainActivity);
                            }

                            if (maintenance == false) {


                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                myPhone = user.getPhoneNumber(); //Current logged in user phone number

                                FirebaseDatabase db = FirebaseDatabase.getInstance();
                                final DatabaseReference dbRef = db.getReference("users/" + myPhone);
                                DatabaseReference myDevicesRef = db.getReference("mydevices/"+myPhone);

                                //Compare device id of current device id and previous logged device id,
                                //if not same prompt logout device. On fresh login will set new device id. limit account logins to one device
                                //Using live listener to keep active track of any new logged device
                                dbRef.child("device_id").addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.exists()){

                                            try {
                                                String fetchedId = dataSnapshot.getValue(String.class);
                                                //device id's do not match, prompt to logout atleast one device
                                                if (!android_id.equals(fetchedId)) {
                                                    //Log out
                                                    Toast.makeText(SplashActivity.this, "You're logged in a different device!", Toast.LENGTH_LONG).show();
                                                    stopService(new Intent(SplashActivity.this, ForegroundService.class));
                                                    stopService(new Intent(SplashActivity.this, TrackingService.class));
                                                    FirebaseAuth.getInstance().signOut();
                                                    startActivity(new Intent(SplashActivity.this, MainActivity.class)
                                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                                }


                                            } catch (Exception e){

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
                                        try {
                                            String verified = dataSnapshot.getValue(String.class);

                                            //Toast.makeText(SplashActivity.this, "Verified: " + verified, Toast.LENGTH_LONG).show();
                                            if (verified == null) {
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

                                            } else if (verified.equals("true")) { //Will need to check account type as well, then redirect to account type

                                                //A security feature, we want to confirm this device has not been blocked from accessing the account
                                                myDevicesRef.child(android_id).addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        if (dataSnapshot.exists()) {
                                                            try {
                                                                MyDeviceModel myDevice = dataSnapshot.getValue(MyDeviceModel.class);
                                                                if (myDevice.getBlocked() == true) {
                                                                    //Load DeviceBlocked activity
                                                                    Intent slideactivity = new Intent(SplashActivity.this, DeviceBlocked.class)
                                                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                    Bundle bndlanimation =
                                                                            ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                                                    getApplicationContext().startActivity(slideactivity, bndlanimation);
                                                                } else {
                                                                    //proceed
                                                                    loadAccount();
                                                                }
                                                            } catch (Exception e) {

                                                            }
                                                        } else {
                                                            loadAccount();
                                                        }

                                                    }

                                                    private void loadAccount() {
                                                        //User is verified, so we need to check their account type and redirect accordingly
                                                        dbRef.child("account_type").addValueEventListener(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                                try {
                                                                    final String account_type = dataSnapshot.getValue(String.class);
                                                                    //String account_type = Integer.toString(acc_type);

                                                                    if (account_type == null) {
                                                                        //Toast.makeText(SplashActivity.this, "account type null", Toast.LENGTH_SHORT).show();
                                                                        //Set account type to 0 if setting up no complete
                                                                        dbRef.child("account_type").setValue("0").addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                            @Override
                                                                            public void onSuccess(Void aVoid) {

                                                                                //Toast.makeText(SplashActivity.this, "You have not finished setting up your account!", Toast.LENGTH_LONG).show();

                                                                                Intent slideactivity = new Intent(SplashActivity.this, SetupAccountType.class)
                                                                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                                Bundle bndlanimation =
                                                                                        ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                                                                getApplicationContext().startActivity(slideactivity, bndlanimation);
                                                                            }
                                                                        });
                                                                    } else {
                                                                        if (account_type.equals("1")) { //Customer account
                                                                            try {
                                                                                dbRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                                                                                    @Override
                                                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                                        if (dataSnapshot.exists()) {
                                                                                            try {
                                                                                                dbRef.child("appLocked").setValue(true);
                                                                                                //Toast.makeText(SplashActivity.this, "Customer Account", Toast.LENGTH_LONG).show();
                                                                                                Intent slideactivity = new Intent(SplashActivity.this, SecurityPin.class)
                                                                                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                                                slideactivity.putExtra("pinType", "login");
                                                                                                slideactivity.putExtra("accType", "1");
                                                                                                startActivity(slideactivity);
                                                                                            } catch (Exception e) {
                                                                                                Log.e(TAG, "onDataChange: ", e);
                                                                                            }
                                                                                        } else {
                                                                                            try {
                                                                                                //Toast.makeText(SplashActivity.this, "Customer Account", Toast.LENGTH_LONG).show();
                                                                                                Intent slideactivity = new Intent(SplashActivity.this, CustomerActivity.class)
                                                                                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                                                Bundle bndlanimation =
                                                                                                        ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                                                                                startActivity(slideactivity, bndlanimation);
                                                                                            } catch (Exception e) {
                                                                                                Log.e(TAG, "onDataChange: ", e);
                                                                                            }
                                                                                        }
                                                                                    }

                                                                                    @Override
                                                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                                    }
                                                                                });

                                                                            } catch (Exception e) {
                                                                                Log.e(TAG, "onDataChange: ", e);
                                                                            }
                                                                        } else if (account_type.equals("2")) { //Provider Restaurant account
                                                                            try {
                                                                                dbRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                                                                                    @Override
                                                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                                        if (dataSnapshot.exists()) {
                                                                                            try {
                                                                                                dbRef.child("appLocked").setValue(true);
                                                                                                //Toast.makeText(SplashActivity.this, "Customer Account", Toast.LENGTH_LONG).show();
                                                                                                Intent slideactivity = new Intent(SplashActivity.this, SecurityPin.class)
                                                                                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                                                slideactivity.putExtra("pinType", "login");
                                                                                                slideactivity.putExtra("accType", "2");
                                                                                                startActivity(slideactivity);
                                                                                            } catch (Exception e) {
                                                                                                Log.e(TAG, "onDataChange: ", e);
                                                                                            }
                                                                                        } else {
                                                                                            try {
                                                                                                //Toast.makeText(SplashActivity.this, "Customer Account", Toast.LENGTH_LONG).show();
                                                                                                Intent slideactivity = new Intent(SplashActivity.this, VendorActivity.class)
                                                                                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                                                Bundle bndlanimation =
                                                                                                        ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                                                                                startActivity(slideactivity, bndlanimation);
                                                                                            } catch (Exception e) {
                                                                                                Log.e(TAG, "onDataChange: ", e);
                                                                                            }
                                                                                        }
                                                                                    }

                                                                                    @Override
                                                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                                    }
                                                                                });
                                                                            } catch (Exception e) {
                                                                                Log.e(TAG, "onDataChange: ", e);
                                                                            }
                                                                        } else if (account_type.equals("3")) { //Nduthi account
                                                                            try {
                                                                                dbRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                                                                                    @Override
                                                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                                        if (dataSnapshot.exists()) {
                                                                                            try {
                                                                                                dbRef.child("appLocked").setValue(true);
                                                                                                //Toast.makeText(SplashActivity.this, "Customer Account", Toast.LENGTH_LONG).show();
                                                                                                Intent slideactivity = new Intent(SplashActivity.this, SecurityPin.class)
                                                                                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                                                slideactivity.putExtra("pinType", "login");
                                                                                                slideactivity.putExtra("accType", "3");
                                                                                                startActivity(slideactivity);
                                                                                            } catch (Exception e) {
                                                                                                Log.e(TAG, "onDataChange: ", e);
                                                                                            }
                                                                                        } else {
                                                                                            try {
                                                                                                //Toast.makeText(SplashActivity.this, "Customer Account", Toast.LENGTH_LONG).show();
                                                                                                Intent slideactivity = new Intent(SplashActivity.this, RiderActivity.class)
                                                                                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                                                Bundle bndlanimation =
                                                                                                        ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                                                                                startActivity(slideactivity, bndlanimation);
                                                                                            } catch (Exception e) {
                                                                                                Log.e(TAG, "onDataChange: ", e);
                                                                                            }
                                                                                        }
                                                                                    }

                                                                                    @Override
                                                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                                    }
                                                                                });
                                                                            } catch (Exception e) {
                                                                                Log.e(TAG, "onDataChange: ", e);
                                                                            }

                                                                        } else if (account_type.equals("0")) {
                                                                            try {
                                                                                //Toast.makeText(SplashActivity.this, "You have not finished setting up your account!", Toast.LENGTH_LONG).show();

                                                                                Intent slideactivity = new Intent(SplashActivity.this, SetupAccountType.class)
                                                                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                                Bundle bndlanimation =
                                                                                        ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                                                                getApplicationContext().startActivity(slideactivity, bndlanimation);
                                                                            } catch (Exception e) {
                                                                                Log.e(TAG, "onDataChange: ", e);
                                                                            }
                                                                        } else if (account_type.equals("x") || account_type.equals("X")) {
                                                                            try {
                                                                                Intent slideactivity = new Intent(SplashActivity.this, BlockedAccount.class)
                                                                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                                Bundle bndlanimation =
                                                                                        ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation, R.anim.animation2).toBundle();
                                                                                getApplicationContext().startActivity(slideactivity, bndlanimation);
                                                                            } catch (Exception e) {
                                                                                Log.e(TAG, "onDataChange: ", e);
                                                                            }
                                                                        } else { // Others
                                                                            finish();
                                                                            Toast.makeText(SplashActivity.this, "Account type does not exist", Toast.LENGTH_LONG).show();
                                                                        }
                                                                    }
                                                                } catch (Exception e) {

                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                                //DB error, try again...if fails login again
                                                                //

                                                            }
                                                        });
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                    }
                                                });


                                            } else {
                                                progressBar.setVisibility(View.GONE);
                                                //User is not verified so have them verify their profile details first
                                                startActivity(new Intent(SplashActivity.this, SetupProfile.class)
                                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));//Load Main Activity and clear activity stack
                                            }
                                        } catch (Exception e){

                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                    }
                                });
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

        else {
            progressBar.setVisibility(View.GONE);
            startActivity(new Intent(SplashActivity.this, WelcomeActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        }
    }

    public void startNotificationService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        serviceIntent.putExtra("title", "Dishi");
        serviceIntent.putExtra("message", "Welcome to Dishi");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
            //ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            ContextCompat.startForegroundService(this, serviceIntent);

        }

    }

    public void stopNotificationService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Boolean serviceRunning = isMyServiceRunning(ForegroundService.class);

        if(serviceRunning != true){
            startNotificationService();
        }

        loadSplash();
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        Boolean serviceRunning = isMyServiceRunning(ForegroundService.class);

        if(serviceRunning != true){
            startNotificationService();
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
