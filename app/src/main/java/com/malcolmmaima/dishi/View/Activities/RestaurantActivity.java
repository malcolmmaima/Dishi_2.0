package com.malcolmmaima.dishi.View.Activities;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.alexzh.circleimageview.CircleImageView;
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
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Fragments.CustomerOrderFragment;
import com.malcolmmaima.dishi.View.Fragments.HomeFragment;
import com.malcolmmaima.dishi.View.Fragments.OrdersFragment;
import com.malcolmmaima.dishi.View.Fragments.ProfileFragment;
import com.squareup.picasso.Picasso;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class RestaurantActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    FloatingActionButton addMenu;

    String myPhone;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    private String TAG;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment selectedFragment = null;
            switch (item.getItemId()) {
                case R.id.navigation_orders:
                    selectedFragment = OrdersFragment.newInstance();
                    break;
                case R.id.navigation_home:
                    selectedFragment = HomeFragment.newInstance();
                    break;
                case R.id.navigation_profile:
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

        TAG = "RestaurantActivity";

        addMenu = findViewById(R.id.button_add_menu);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("");

        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //get auth state
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        //Set fb database reference
        myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);

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
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.flContent, HomeFragment.newInstance());
        transaction.commit();

        //Used to select an item programmatically
        navView.getMenu().getItem(0).setChecked(true);

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
        final TextView emailAddress = headerView.findViewById(R.id.emailaddress);

        //Set header data
        navUsername.setText("");
        emailAddress.setText("");

        //User is logged in
        if(mAuth.getInstance().getCurrentUser() != null) {

            /**
             * Get logged in user details
             */
            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        UserModel user = dataSnapshot.getValue(UserModel.class);

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
            });
        }

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
            fragmentClass[0] = OrdersFragment.class;
            //Toast.makeText(this, "Clicked!", Toast.LENGTH_SHORT).show();
        }

        else if (id == R.id.menu2) {
            Toast.makeText(this, "Clicked!", Toast.LENGTH_SHORT).show();
        }

        else if (id == R.id.menu3) {
            Toast.makeText(this, "Clicked!", Toast.LENGTH_SHORT).show();
        } //else if (id == R.id.menu4) {
        //fragmentClass[0] = FragmentFour.class;
        //}

        else if (id == R.id.nav_share) {
            Toast.makeText(RestaurantActivity.this, "Clicked!", Toast.LENGTH_LONG).show();

        } else if (id == R.id.nav_send) {
            Toast.makeText(this, "Clicked!", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
