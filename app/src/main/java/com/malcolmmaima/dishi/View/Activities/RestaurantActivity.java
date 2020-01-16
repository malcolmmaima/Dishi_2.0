package com.malcolmmaima.dishi.View.Activities;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Fragments.CustomerOrderFragment;
import com.malcolmmaima.dishi.View.Fragments.HomeFragment;
import com.malcolmmaima.dishi.View.Fragments.OrdersFragment;
import com.malcolmmaima.dishi.View.Fragments.ProfileFragment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class RestaurantActivity extends AppCompatActivity {

    FloatingActionButton addMenu;

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
            transaction.replace(R.id.frame_layout, selectedFragment);
            transaction.commit();
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant);
        addMenu = findViewById(R.id.button_add_menu);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

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
        transaction.replace(R.id.frame_layout, CustomerOrderFragment.newInstance());
        transaction.commit();

        //Used to select an item programmatically
        navView.getMenu().getItem(2).setChecked(true);
    }

}
