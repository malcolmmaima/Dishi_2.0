package com.malcolmmaima.dishi.View.Activities;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

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
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.AddRiderAdapter;
import com.malcolmmaima.dishi.View.Adapter.OrdersAdapter;

import java.util.ArrayList;
import java.util.List;

public class AddRider extends AppCompatActivity {

    List<UserModel> riders = new ArrayList<>();
    DatabaseReference riderUserAccounts, myRidersRef;
    ProgressBar progressBar;
    EditText searchPhone;
    RecyclerView recyclerview;
    TextView emptyTag;
    String myPhone;
    FirebaseUser user;
    ChildEventListener riderAddedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_rider);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        riderUserAccounts = FirebaseDatabase.getInstance().getReference("users");
        myRidersRef = FirebaseDatabase.getInstance().getReference("my_riders/"+myPhone);

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
                        Toast.makeText(AddRider.this, "Added successfully, refresh!", Toast.LENGTH_LONG).show();
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

    private void searchRider(final String phone) {
        riderUserAccounts.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot user : dataSnapshot.getChildren()){
                    String accountType = user.child("account_type").getValue(String.class);
                    if(accountType.equals("3")){
                        UserModel riderUser = user.getValue(UserModel.class);

                        /**
                         * Compare search parameter with value returned from db
                         */
                        if(phone.equals(user.getKey())){
                            riderUser.setPhone(user.getKey());
                            riders.add(riderUser);
                        }

                        if (!riders.isEmpty()) {
                            progressBar.setVisibility(View.INVISIBLE);
                            //Collections.reverse(orders);
                            AddRiderAdapter recycler = new AddRiderAdapter(AddRider.this, riders);
                            RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(AddRider.this);
                            recyclerview.setLayoutManager(layoutmanager);
                            recyclerview.setItemAnimator(new DefaultItemAnimator());

                            recycler.notifyDataSetChanged();

                            recyclerview.getItemAnimator().setAddDuration(200);
                            recyclerview.getItemAnimator().setRemoveDuration(200);
                            recyclerview.getItemAnimator().setMoveDuration(200);
                            recyclerview.getItemAnimator().setChangeDuration(200);

                            recyclerview.setAdapter(recycler);
                            emptyTag.setVisibility(View.INVISIBLE);
                        } else {
                            progressBar.setVisibility(View.INVISIBLE);
                            AddRiderAdapter recycler = new AddRiderAdapter(AddRider.this, riders);
                            RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(AddRider.this);
                            recyclerview.setLayoutManager(layoutmanager);
                            recyclerview.setItemAnimator(new DefaultItemAnimator());
                            recyclerview.setAdapter(recycler);
                            emptyTag.setVisibility(View.VISIBLE);

                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            myRidersRef.removeEventListener(riderAddedListener);
        } catch (Exception e){

        }
    }
}