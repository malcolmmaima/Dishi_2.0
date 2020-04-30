package com.malcolmmaima.dishi.View.Fragments;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.MyCart;

import java.util.ArrayList;
import java.util.List;

public class CustomerOrderFragment extends Fragment {
    ProgressDialog progressDialog ;
    RecyclerView recyclerview;
    String myPhone;

    DatabaseReference dbRef, myCartRef;
    FirebaseDatabase db;
    FirebaseUser user;
    ValueEventListener cartListener;

    public static CustomerOrderFragment newInstance() {
        CustomerOrderFragment fragment = new CustomerOrderFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_customer_order, container, false);
        progressDialog = new ProgressDialog(getContext());
        final FloatingActionButton fab = v.findViewById(R.id.fab);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        db = FirebaseDatabase.getInstance();

        dbRef = db.getReference("users/"+myPhone);
        myCartRef = db.getReference("cart/"+myPhone);

        // Setting ViewPager for each Tabs
        ViewPager viewPager = (ViewPager) v.findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        // Set Tabs inside Toolbar
        TabLayout tabs = (TabLayout) v.findViewById(R.id.result_tabs);
        tabs.setupWithViewPager(viewPager);

        myCartRef = FirebaseDatabase.getInstance().getReference("cart/"+myPhone);
        cartListener = new ValueEventListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    try {
                        fab.setVisibility(View.VISIBLE);
                    } catch (Exception e){

                    }
                }

                else {
                    try {
                        fab.setVisibility(View.GONE);
                    } catch (Exception e){

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        myCartRef.addValueEventListener(cartListener);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent slideactivity = new Intent(getContext(), MyCart.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(slideactivity);
            }
        });

        return  v;
    }

    // Add Fragments to Tabs
    private void setupViewPager(ViewPager viewPager) {

        Adapter adapter = new Adapter(getChildFragmentManager());
        adapter.addFragment(new FragmentFood(), "Food");
        adapter.addFragment(new FragmentRestaurants(), "Vendors");
        viewPager.setAdapter(adapter);

    }

    public static class Adapter extends FragmentPagerAdapter {
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
    public void onDestroy() {
        super.onDestroy();
        //Dealing with memory leaks.
        myCartRef.removeEventListener(cartListener);
    }
}