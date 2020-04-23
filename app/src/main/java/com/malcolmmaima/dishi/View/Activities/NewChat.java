package com.malcolmmaima.dishi.View.Activities;

import android.os.Bundle;

import androidx.annotation.NonNull;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.AddRiderAdapter;
import com.malcolmmaima.dishi.View.Adapter.NewChatAdapter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class NewChat extends AppCompatActivity {


    DatabaseReference followingRef;
    ProgressBar progressBar;
    EditText searchPhone;
    RecyclerView recyclerview;
    TextView emptyTag;
    String myPhone;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_chat);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        followingRef = FirebaseDatabase.getInstance().getReference("following/"+myPhone);

        progressBar = findViewById(R.id.progressBar);
        searchPhone = findViewById(R.id.riderPhone);
        recyclerview = findViewById(R.id.rview);
        emptyTag = findViewById(R.id.empty_tag);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("New Chat");

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
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(final Editable s) {
                progressBar.setVisibility(View.VISIBLE);

                /**
                 * Pass search value to our search function
                 */
                searchUser(s.toString().trim());
            }
        });
    }

    private void searchUser(final String names) {
        List<UserModel> usersFollowing = new ArrayList<>();
        followingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot user : dataSnapshot.getChildren()){
                    DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/"+user.getKey());
                    userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            UserModel userFound = dataSnapshot.getValue(UserModel.class);
                            userFound.setPhone(user.getKey());

                            /**
                             * Compare search parameter with value returned from db
                             */
                            String userName = userFound.getFirstname()+" "+userFound.getLastname();
                            if(userName.toLowerCase().contains(names.toLowerCase()) && !usersFollowing.contains(userFound.getPhone())){
                                usersFollowing.add(userFound);
                            }

                            if (!usersFollowing.isEmpty()) {
                                progressBar.setVisibility(View.GONE);
                                //Collections.reverse(orders);

                                NewChatAdapter recycler = new NewChatAdapter(NewChat.this, usersFollowing);
                                RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(NewChat.this);
                                recyclerview.setLayoutManager(layoutmanager);
                                recyclerview.setItemAnimator(new DefaultItemAnimator());
                                recycler.notifyDataSetChanged();
                                recyclerview.setAdapter(recycler);
                                emptyTag.setVisibility(View.GONE);
                            } else {
                                progressBar.setVisibility(View.GONE);
                                NewChatAdapter recycler = new NewChatAdapter(NewChat.this, usersFollowing);
                                RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(NewChat.this);
                                recyclerview.setLayoutManager(layoutmanager);
                                recyclerview.setItemAnimator(new DefaultItemAnimator());
                                recyclerview.setAdapter(recycler);
                                emptyTag.setVisibility(View.VISIBLE);
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


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
