package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.UserModel;
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

    String phone, target;
    DatabaseReference userDetails;

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

        setTitle("");
        phone = getIntent().getStringExtra("phone");
        target = getIntent().getStringExtra("target");

        userDetails = FirebaseDatabase.getInstance().getReference("users/"+phone);
        userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    UserModel userModel = dataSnapshot.getValue(UserModel.class);
                    setTitle(userModel.getFirstname() + " " + userModel.getLastname());
                } catch (Exception e){}
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        // Setting ViewPager for each Tabs
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        //Navigate to specific fragment based on what target user clicked on in profile
        if(target.equals("following")){
            viewPager.setCurrentItem(0);
        } else {
            viewPager.setCurrentItem(1);
        }


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

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}
