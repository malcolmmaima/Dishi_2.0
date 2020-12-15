package com.malcolmmaima.dishiapp.View.Activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishiapp.Controller.Interface.OnRiderSelected;
import com.malcolmmaima.dishiapp.Model.UserModel;
import com.malcolmmaima.dishiapp.R;
import com.malcolmmaima.dishiapp.View.Adapter.AddRiderAdapter;

import java.util.ArrayList;
import java.util.List;



public class AddRider extends AppCompatActivity implements OnRiderSelected {

    List<UserModel> riders = new ArrayList<>();
    DatabaseReference riderUserAccounts, myRidersRef, ridersRef, myRef;
    ValueEventListener ridersRefListener;
    ProgressBar progressBar;
    EditText searchPhone;
    RecyclerView recyclerview;
    MyTextView_Roboto_Medium emptyTag;
    String myPhone;
    FirebaseUser user;
    ChildEventListener riderAddedListener;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_rider);

        //get auth state
        mAuth = FirebaseAuth.getInstance();
        //User is logged in
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number
            riderUserAccounts = FirebaseDatabase.getInstance().getReference("users");
            myRidersRef = FirebaseDatabase.getInstance().getReference("my_riders/"+myPhone);
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
                                    Intent slideactivity = new Intent(AddRider.this, SecurityPin.class)
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

            progressBar = findViewById(R.id.progressBar);
            searchPhone = findViewById(R.id.riderPhone);
            recyclerview = findViewById(R.id.rview);
            emptyTag = findViewById(R.id.empty_tag);

            Toolbar topToolBar = findViewById(R.id.toolbar);
            setSupportActionBar(topToolBar);

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            setTitle("Add Rider");

            //Back button on toolbar
            topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish(); //Go back to previous activity
                }
            });

            searchPhone.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    progressBar.setVisibility(View.INVISIBLE);
                    riders.clear();
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    riders.clear();
                }

                @Override
                public void afterTextChanged(final Editable s) {
                    progressBar.setVisibility(View.VISIBLE);
                    riders.clear();

                    /**
                     * Pass search value to our search function
                     */
                    searchRider(s.toString().trim());
                }
            });

            riderAddedListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                    /**
                     * Check if rider has been added
                     */
                    try {
                        if (dataSnapshot.getKey().equals(searchPhone.getText().toString().trim())) {
                            finish();
                            Toast.makeText(AddRider.this, "Rider accepted request, refresh!", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e){

                    }

                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            myRidersRef.addChildEventListener(riderAddedListener);
        }
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
                            try {
                                Boolean locked = dataSnapshot.getValue(Boolean.class);

                                if(locked == true){
                                    Intent slideactivity = new Intent(AddRider.this, SecurityPin.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    slideactivity.putExtra("pinType", "resume");
                                    startActivity(slideactivity);
                                }
                            } catch (Exception e){

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

    private void searchRider(final String phone) {
        riderUserAccounts.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot user : dataSnapshot.getChildren()){
                    try {
                        String accountType = user.child("account_type").getValue(String.class);
                        if (accountType.equals("3")) {
                            UserModel riderUser = user.getValue(UserModel.class);

                            /**
                             * Compare search parameter with value returned from db
                             */
                            if (phone.equals(user.getKey())) {
                                riderUser.setPhone(user.getKey());
                                riders.add(riderUser);
                            }

                            if (!riders.isEmpty()) {
                                progressBar.setVisibility(View.INVISIBLE);
                                //Collections.reverse(orders);
                                AddRiderAdapter recycler = new AddRiderAdapter(AddRider.this, riders, AddRider.this);
                                RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(AddRider.this);
                                recyclerview.setLayoutManager(layoutmanager);
                                recyclerview.setItemAnimator(new DefaultItemAnimator());
                                recycler.notifyDataSetChanged();
                                recyclerview.setAdapter(recycler);
                                emptyTag.setVisibility(View.INVISIBLE);
                            } else {
                                progressBar.setVisibility(View.INVISIBLE);
                                AddRiderAdapter recycler = new AddRiderAdapter(AddRider.this, riders, AddRider.this);
                                RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(AddRider.this);
                                recyclerview.setLayoutManager(layoutmanager);
                                recyclerview.setItemAnimator(new DefaultItemAnimator());
                                recyclerview.setAdapter(recycler);
                                emptyTag.setVisibility(View.VISIBLE);

                            }
                        }
                    } catch (Exception e){

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onRiderSelected(String riderPhone, String restaurantPhone) {
        /**
         * listen to the my_restaurants node to check if request has been sent
         */
        ridersRef = FirebaseDatabase.getInstance().getReference("my_restaurants/"+riderPhone+"/"+restaurantPhone);
        ridersRefListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Toast.makeText(AddRider.this, "Request sent to rider", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        ridersRef.addValueEventListener(ridersRefListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            myRidersRef.removeEventListener(riderAddedListener);
            ridersRef.removeEventListener(ridersRefListener);
        } catch (Exception e){

        }
    }
}
