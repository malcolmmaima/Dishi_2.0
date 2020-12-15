package com.malcolmmaima.dishiapp.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishiapp.Model.UserModel;
import com.malcolmmaima.dishiapp.R;
import com.malcolmmaima.dishiapp.View.Adapter.FollowerFollowingAdapter;
import com.malcolmmaima.dishiapp.View.Adapter.UserManageAdapter;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.VISIBLE;
import static com.malcolmmaima.dishiapp.View.Activities.SearchActivity.hideKeyboard;

public class UserManagement extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    RecyclerView recyclerview;
    MyTextView_Roboto_Regular emptyTag;
    SwipeRefreshLayout mSwipeRefreshLayout;
    String searchValue, myPhone, word;
    FirebaseUser user;
    int searchCap;
    LinearLayoutManager layoutmanager;
    EditText searchWord;
    Button btnSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        loadActivity();
    }

    private void loadActivity() {
        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle("");

        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber();
        searchCap = 100;

        btnSearch = findViewById(R.id.btnSearch);
        searchWord = findViewById(R.id.edtSearch);
        recyclerview = findViewById(R.id.rview);
        emptyTag = findViewById(R.id.empty_tag);
        layoutmanager = new LinearLayoutManager(UserManagement.this);
        recyclerview.setLayoutManager(layoutmanager);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        /**
         * Showing Swipe Refresh animation on activity create
         * As animation won't start on onCreate, post runnable is used
         */
        mSwipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {
                searchUsers("");
            }
        });

        searchWord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchWord.requestFocus();
            }
        });

        searchWord.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    searchWord.clearFocus();
                    hideKeyboard(UserManagement.this);
                    word = searchWord.getText().toString().trim();
                    searchUsers(word);
                    return true;
                }
                return false;
            }
        });

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchWord.clearFocus();
                hideKeyboard(UserManagement.this);
                word = searchWord.getText().toString().trim();
                searchUsers(word);
            }
        });
    }

    private void searchUsers(String s) {
        if(isStringNullOrWhiteSpace(s)){
            emptyTag.setText("Type something");
            emptyTag.setVisibility(VISIBLE);
        } else {
            emptyTag.setText("Nothing found");
            mSwipeRefreshLayout.setRefreshing(true);
            DatabaseReference usersRef;
            usersRef = FirebaseDatabase.getInstance().getReference("users");
            List<UserModel> usersFound = new ArrayList<>();
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.exists()){
                        mSwipeRefreshLayout.setRefreshing(false);
                        FollowerFollowingAdapter recycler = new FollowerFollowingAdapter(UserManagement.this, usersFound);
                        recyclerview.setLayoutManager(layoutmanager);
                        recyclerview.setItemAnimator(new DefaultItemAnimator());
                        recyclerview.setAdapter(recycler);
                        emptyTag.setVisibility(View.VISIBLE);
                    } else {
                        for(DataSnapshot user_ : dataSnapshot.getChildren()){
                            UserModel userFound = user_.getValue(UserModel.class);
                            userFound.setPhone(user_.getKey());

                            String name = userFound.getFirstname()+" "+userFound.getLastname()+" "+userFound.getPhone();
                            if(name.toLowerCase().contains(s.toLowerCase()) && !myPhone.equals(userFound.getPhone()) && userFound.getFirstname() != null){
                                if(usersFound.size() < searchCap){
                                    usersFound.add(userFound);
                                }
                            }

                            if (!usersFound.isEmpty()) {
                                mSwipeRefreshLayout.setRefreshing(false);
                                //Collections.reverse(orders);
                                UserManageAdapter recycler = new UserManageAdapter(UserManagement.this, usersFound);
                                recyclerview.setLayoutManager(layoutmanager);
                                recyclerview.setItemAnimator(new DefaultItemAnimator());
                                recycler.notifyDataSetChanged();
                                recyclerview.setAdapter(recycler);
                                emptyTag.setVisibility(View.GONE);
                            } else {
                                mSwipeRefreshLayout.setRefreshing(false);
                                UserManageAdapter recycler = new UserManageAdapter(UserManagement.this, usersFound);
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
    }

    public static boolean isStringNullOrWhiteSpace(String value) {
        if (value == null) {
            return true;
        }

        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onRefresh() {
        loadActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recyclerview.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                // no-op
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                recyclerview.setAdapter(null);
                layoutmanager = null;
            }
        });
    }
}
