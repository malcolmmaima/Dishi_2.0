package com.malcolmmaima.dishi.View.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.tabs.TabLayout;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ViewPagerAdapter;
import com.malcolmmaima.dishi.View.Fragments.CustomerOrderFragment;
import com.malcolmmaima.dishi.View.Fragments.FragmentFollowers;
import com.malcolmmaima.dishi.View.Fragments.FragmentFollowing;
import com.malcolmmaima.dishi.View.Fragments.FragmentFood;
import com.malcolmmaima.dishi.View.Fragments.FragmentRestaurants;
import com.malcolmmaima.dishi.View.Fragments.ReviewsFragment;
import com.malcolmmaima.dishi.View.Fragments.ViewRestaurantMenuFragment;

import java.util.ArrayList;
import java.util.List;

public class FollowersFollowing extends AppCompatActivity {

    String phone;

    //This is our tablayout
    private TabLayout tabLayout;

    //This is our viewPager
    private ViewPager viewPager;

    ViewPagerAdapter adapter;

    FragmentFollowing fragmentFollowing;
    FragmentFollowers fragmentFollowers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_followers_following);

        phone = getIntent().getStringExtra("phone");
        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle(phone);

        // Setting ViewPager for each Tabs
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        // Set Tabs inside Toolbar
        TabLayout tabs = (TabLayout) findViewById(R.id.result_tabs);
        tabs.setupWithViewPager(viewPager);

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });

    }

    // Add Fragments to Tabs
    private void setupViewPager(ViewPager viewPager) {
        fragmentFollowing = new FragmentFollowing();
        fragmentFollowers = new FragmentFollowers();

        Bundle data = new Bundle();//bundle instance
        data.putString("phone", phone);//string to pass with a key value

        fragmentFollowing.setArguments(data);//Set bundle data to fragment
        fragmentFollowers.setArguments(data);

        Adapter adapter = new Adapter(getSupportFragmentManager());
        adapter.addFragment(fragmentFollowing, "Following");
        adapter.addFragment(fragmentFollowers, "Followers");
        viewPager.setAdapter(adapter);

    }

    static class Adapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public Adapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

    }
}
