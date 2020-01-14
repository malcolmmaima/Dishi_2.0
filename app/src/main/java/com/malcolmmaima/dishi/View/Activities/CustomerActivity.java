package com.malcolmmaima.dishi.View.Activities;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Fragments.CustomerOrderFragment;
import com.malcolmmaima.dishi.View.Fragments.HomeFragment;
import com.malcolmmaima.dishi.View.Fragments.UserProfileFragment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.MenuItem;

public class CustomerActivity extends AppCompatActivity {

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment selectedFragment = null;
            switch (item.getItemId()) {
                case R.id.navigation_order:
                    selectedFragment = CustomerOrderFragment.newInstance();
                    break;
                case R.id.navigation_home:
                    selectedFragment = HomeFragment.newInstance();
                    break;
                case R.id.navigation_profile:
                    selectedFragment = UserProfileFragment.newInstance();
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
        setContentView(R.layout.activity_customer);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        //Manually displaying the first fragment - one time only
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_layout, CustomerOrderFragment.newInstance());
        transaction.commit();

        //Used to select an item programmatically
        navView.getMenu().getItem(2).setChecked(true);
    }

}
