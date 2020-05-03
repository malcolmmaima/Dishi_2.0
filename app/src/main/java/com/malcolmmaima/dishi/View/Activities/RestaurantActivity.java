package com.malcolmmaima.dishi.View.Activities;

import android.Manifest;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import com.alexzh.circleimageview.CircleImageView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.ForegroundService;
import com.malcolmmaima.dishi.Controller.TrackingService;
import com.malcolmmaima.dishi.Model.NotificationModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Fragments.FragmentStats;
import com.malcolmmaima.dishi.View.Fragments.HomeFragment;
import com.malcolmmaima.dishi.View.Fragments.MenuFragment;
import com.malcolmmaima.dishi.View.Fragments.MyRidersFragment;
import com.malcolmmaima.dishi.View.Fragments.RestaurantOrdersFragment;
import com.malcolmmaima.dishi.View.Fragments.ProfileFragment;
import com.squareup.picasso.Picasso;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import io.fabric.sdk.android.services.common.SafeToast;

public class RestaurantActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    FloatingActionButton addMenu;
    Menu myMenu;
    String myPhone, imageURL;
    private DatabaseReference myRef, myNotificationsRef;
    private ValueEventListener myRefListener, myNotificationsListener;
    private FirebaseAuth mAuth;
    private String TAG;
    private static final int PERMISSIONS_REQUEST = 100;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment selectedFragment = null;
            switch (item.getItemId()) {
                case R.id.navigation_orders:
                    setTitle("Orders");
                    selectedFragment = RestaurantOrdersFragment.newInstance();
                    break;
                case R.id.navigation_home:
                    setTitle("Home");
                    selectedFragment = HomeFragment.newInstance();
                    break;
                case R.id.navigation_profile:
                    setTitle("Profile");
                    selectedFragment = ProfileFragment.newInstance();
                    break;
            }

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.flContent, selectedFragment);
            transaction.commit();
            return true;


        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant);


        //User is logged in
        if(mAuth.getInstance().getCurrentUser() != null) {

            TAG = "RestaurantActivity";

            imageURL = "";

            addMenu = findViewById(R.id.button_add_menu);

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            setTitle("");

            BottomNavigationView navView = findViewById(R.id.nav_view);
            navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

            try {
                //get auth state
                mAuth = FirebaseAuth.getInstance();
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                myPhone = user.getPhoneNumber(); //Current logged in user phone number

                //Set fb database reference
                myRef = FirebaseDatabase.getInstance().getReference("users/" + myPhone);
                myNotificationsRef = FirebaseDatabase.getInstance().getReference("notifications/"+myPhone);
            } catch (Exception e){

            }

            /**
             * Load add menu activity
             */

            addMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent slideactivity = new Intent(RestaurantActivity.this, AddMenu.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
            /**
             * Manually displaying the first fragment - one time only
             */
            setTitle("Orders");
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.flContent, RestaurantOrdersFragment.newInstance());
            transaction.commit();

            //Used to select an item programmatically
            navView.getMenu().getItem(2).setChecked(true);

            /**
             * Navigation drawer
             */

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();

            NavigationView navigationView = (NavigationView) findViewById(R.id.drawer_nav_view);
            navigationView.setNavigationItemSelectedListener(this);

            //Drawer header
            View headerView = navigationView.getHeaderView(0);
            final CircleImageView profilePic = headerView.findViewById(R.id.profilePic);
            final TextView navUsername = headerView.findViewById(R.id.userName);
            final ImageButton notificationIcon = headerView.findViewById(R.id.notifications);

            //Set header data
            navUsername.setText("");

            /**
             * Get logged in user details
             */
            myRefListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        UserModel user = dataSnapshot.getValue(UserModel.class);

                        if(!user.getAccount_type().equals("2")){
                            SafeToast.makeText(RestaurantActivity.this, "Not allowed!", Toast.LENGTH_LONG).show();
                            finish();
                        }

                        imageURL = user.getProfilePic();

                        //Set username on drawer header
                        navUsername.setText(user.getFirstname() + " " + user.getLastname());

                        Picasso.with(RestaurantActivity.this).load(user.getProfilePic()).fit().centerCrop()
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(profilePic);

                    } catch (Exception e){
                        Log.e(TAG, "onDataChange: " + e);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            myRef.addValueEventListener(myRefListener);

            /**
             * Check notifications
             */

            myNotificationsListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    notificationIcon.setBackgroundResource(R.drawable.ic_notifications_white_48dp);
                    for(DataSnapshot notifs : dataSnapshot.getChildren()){
                        NotificationModel allnotifications = notifs.getValue(NotificationModel.class);
                        if(allnotifications.getSeen() == false){
                            notificationIcon.setBackgroundResource(R.drawable.active_notification_64dp);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            myNotificationsRef.addValueEventListener(myNotificationsListener);

            profilePic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Make sure image url is not empty
                    if(!imageURL.equals("")){
                        Intent slideactivity = new Intent(RestaurantActivity.this, ViewImage.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        slideactivity.putExtra("imageURL", imageURL);
                        startActivity(slideactivity);
                    }

                    else {
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.drawer_layout), "Something went wrong", Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                }
            });

            notificationIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent slideactivity = new Intent(RestaurantActivity.this, MyNotifications.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(slideactivity);

                }
            });

            ////////////////////////////////////
            //Check whether GPS tracking is enabled//

            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                //SafeToast.makeText(this, "Please turn on GPS", Toast.LENGTH_LONG).show();
                GoogleApiClient googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                        .addApi(LocationServices.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this).build();
                googleApiClient.connect();

                LocationRequest locationRequest = LocationRequest.create();
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                locationRequest.setInterval(5 * 1000);
                locationRequest.setFastestInterval(2 * 1000);
                LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest);

                //**************************
                builder.setAlwaysShow(true); //this is the key ingredient
                //**************************

                PendingResult<LocationSettingsResult> result =
                        LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
                result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                    @Override
                    public void onResult(@NonNull LocationSettingsResult result) {
                        final Status status = result.getStatus();
//                final LocationSettingsStates state = result.getLocationSettingsStates();

                        switch (status.getStatusCode()) {
                            case LocationSettingsStatusCodes.SUCCESS:


                                break;
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                // Location settings are not satisfied. But could be fixed by showing the user
                                // a dialog.
                                try {
                                    // Show the dialog by calling startResolutionForResult(),
                                    // and check the result in onActivityResult().
                                    status.startResolutionForResult(RestaurantActivity.this, 1000);
                                } catch (IntentSender.SendIntentException e) {
                                    // Ignore the error.
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                break;
                        }
                    }
                });
            }

            //Check whether this app has access to the location permission//

            int permission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);

            //If the location permission has been granted, then start the TrackerService//

            if (permission == PackageManager.PERMISSION_GRANTED) {
                startTrackerService();
            } else {

                //If the dishi doesn’t currently have access to the user’s location, then request access//

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_REQUEST);
            }
        } else {
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {

        //If the permission has been granted...//

        if (requestCode == PERMISSIONS_REQUEST && grantResults.length == 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            //...then start the GPS tracking service//

            startTrackerService();
        } else {

            //If the user denies the permission request, then display a toast with some more information//
            Snackbar snackbar = Snackbar.make(findViewById(R.id.drawer_layout), "Please enable location services to allow GPS tracking", Snackbar.LENGTH_LONG);
            snackbar.show();

        }
    }

    //Start the TrackerService//

    private void startTrackerService() {
        startService(new Intent(this, TrackingService.class));
        //Notify the user that tracking has been enabled//

        //SafeToast.makeText(this, "GPS tracking enabled", Toast.LENGTH_SHORT).show();

        //////////////////////////////////
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment fragment = null;
        final Class[] fragmentClass = {null};

        if (id == R.id.menu1) {
            setTitle("My Menu");
            fragmentClass[0] = MenuFragment.class;
            //SafeToast.makeText(this, "Clicked!", Toast.LENGTH_SHORT).show();
        }

        else if (id == R.id.menu2) {
            setTitle("My Stats");
            fragmentClass[0] = FragmentStats.class;
            //SafeToast.makeText(this, "Clicked!", Toast.LENGTH_SHORT).show();
        }

        else if (id == R.id.menu4) {
            setTitle("My Riders");
            fragmentClass[0] = MyRidersFragment.class;
        }

        else if (id == R.id.nav_settings) {

            Intent slideactivity = new Intent(RestaurantActivity.this, SettingsActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Bundle bndlanimation =
                    ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation,R.anim.animation2).toBundle();
            startActivity(slideactivity, bndlanimation);

        }

        else if (id == R.id.logOut) {
            final AlertDialog logout = new AlertDialog.Builder(RestaurantActivity.this)
                    .setMessage("Logout?")
                    //.setIcon(R.drawable.ic_done_black_48dp) //will replace icon with name of existing icon from project
                    .setCancelable(false)
                    //set three option buttons
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //Log out
                            //SafeToast.makeText(MyAccountRestaurant.this, "Logout", Toast.LENGTH_LONG).show();
                            stopService(new Intent(RestaurantActivity.this, ForegroundService.class));
                            stopService(new Intent(RestaurantActivity.this, TrackingService.class));
                            FirebaseAuth.getInstance().signOut();
                            startActivity(new Intent(RestaurantActivity.this,SplashActivity.class)
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

        try {
            fragment = (Fragment) fragmentClass[0].newInstance();

            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.flContent, fragment).commit();

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_restaurant_account, menu);
        myMenu = menu;

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Bundle bndlanimation =
                ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.animation,R.anim.animation2).toBundle();

        switch(item.getItemId()) {
            case R.id.search:
                Intent searchActivity = new Intent(RestaurantActivity.this, SearchActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(searchActivity, bndlanimation);
                return(true);

            case R.id.sendDM:
                Intent slideactivity = new Intent(RestaurantActivity.this, Inbox.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(slideactivity, bndlanimation);
                return(true);
        }
        return(super.onOptionsItemSelected(item));
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            myRef.removeEventListener(myRefListener);
            myNotificationsRef.removeEventListener(myNotificationsListener);
        } catch (Exception e){

        }
    }
}
