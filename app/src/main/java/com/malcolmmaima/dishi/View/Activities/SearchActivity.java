package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ViewPagerAdapter;
import com.malcolmmaima.dishi.View.Fragments.FragmentSearchFood;
import com.malcolmmaima.dishi.View.Fragments.FragmentSearchPosts;
import com.malcolmmaima.dishi.View.Fragments.FragmentSearchUsers;
import com.malcolmmaima.dishi.View.Fragments.FragmentSearchVendors;

import io.fabric.sdk.android.services.common.SafeToast;

public class SearchActivity extends AppCompatActivity {

    String TAG = "SearchActivity";
    private ViewPager viewPager;
    ViewPagerAdapter adapter;
    FragmentSearchUsers fragmentSearchUsers;
    FragmentSearchFood fragmentSearchFood;
    FragmentSearchVendors fragmentSearchVendors;
    FragmentSearchPosts fragmentSearchPosts;

    EditText searchWord;
    TabLayout tabLayout;
    DatabaseReference myLocationRef;
    ValueEventListener locationListener;
    UserModel myDetails;
    String myPhone, word;
    private FirebaseAuth mAuth;
    DatabaseReference databaseReference, myUserDetails, myRef;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {

            //Hide keyboard on activity load
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

            //Initializing viewPager
            viewPager = (ViewPager) findViewById(R.id.viewpager);
            viewPager.setOffscreenPageLimit(4);
            setupViewPager(viewPager, "");

            //Initializing the tablayout
            tabLayout = findViewById(R.id.tablayout);

            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    viewPager.setCurrentItem(tab.getPosition(),false);
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });

            viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    tabLayout.getTabAt(position).select();

                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });


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
                                        Intent slideactivity = new Intent(SearchActivity.this, SecurityPin.class)
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

            searchWord = findViewById(R.id.edtSearch);

            Toolbar topToolBar = findViewById(R.id.toolbar);
            setSupportActionBar(topToolBar);

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            setTitle("");

            databaseReference = FirebaseDatabase.getInstance().getReference();
            myLocationRef = FirebaseDatabase.getInstance().getReference("location/"+myPhone);
            myUserDetails = FirebaseDatabase.getInstance().getReference("users/"+myPhone);

            myUserDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    myDetails = dataSnapshot.getValue(UserModel.class);
                    myDetails.setPhone(dataSnapshot.getKey());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


            //Back button on toolbar
            topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish(); //Go back to previous activity
                }
            });


            searchWord.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchWord.requestFocus();
                }
            });

            searchWord.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    //Toast.makeText(SearchActivity.this, "done typing", Toast.LENGTH_SHORT).show();

                    word = editable.toString().trim();
                    searchDB(word);

                }
            });
        }
    }

    private void searchDB(String searchString) {
        setupViewPager(viewPager, searchString);
        viewPager.setCurrentItem(0,false);
        tabLayout.getTabAt(0).select();
    }

    public String getSearchValue() {
        return searchWord.getText().toString().trim();
    }

    private void setupViewPager(ViewPager viewPager, String searchString)
    {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        fragmentSearchUsers = new FragmentSearchUsers();
        fragmentSearchFood = new FragmentSearchFood();
        fragmentSearchVendors = new FragmentSearchVendors();
        fragmentSearchPosts = new FragmentSearchPosts();


        //Pass the search value
        Bundle data = new Bundle();//bundle instance
        data.putString("search", searchString);
        fragmentSearchUsers.setArguments(data);//Set bundle data to fragment
        fragmentSearchFood.setArguments(data);
        fragmentSearchVendors.setArguments(data);
        fragmentSearchPosts.setArguments(data);

        adapter.addFragment(fragmentSearchUsers,"Users");
        adapter.addFragment(fragmentSearchFood,"Food");
        adapter.addFragment(fragmentSearchVendors, "Vendors");
        adapter.addFragment(fragmentSearchPosts, "Posts");
        viewPager.setAdapter(adapter);
    }
}
