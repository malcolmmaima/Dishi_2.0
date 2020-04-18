package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.alexzh.circleimageview.CircleImageView;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.CommentKeyBoardFix;
import com.malcolmmaima.dishi.Controller.GetCurrentDate;
import com.malcolmmaima.dishi.Model.StatusUpdateModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.StatusUpdateAdapter;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import io.fabric.sdk.android.services.common.SafeToast;

public class ViewProfile extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    String phone, myPhone;

    private static final String TAG = "ProfileActivity";
    List<StatusUpdateModel> statusUpdates;
    RecyclerView recyclerview;

    DatabaseReference profileRef, myPostUpdates;
    ValueEventListener myListener, myPostListener;
    FirebaseUser user;

    CircleImageView profilePhoto;
    TextView profileName, profileBio, following, followers;
    ImageButton emoji;
    EmojiconEditText myStatusUpdate;
    View rootView;
    EmojIconActions emojIcon;
    Button postBtn;
    UserModel myUserDetails;

    TextView emptyTag;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle("Profile");

        //keep toolbar pinned at top. push edittext on keyboard load
        new CommentKeyBoardFix(this);

        //Hide keyboard on activity load
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to previous activity
            }
        });

        phone = getIntent().getStringExtra("phone");

        rootView = findViewById(R.id.activity_main);
        profilePhoto = findViewById(R.id.user_profile_photo);
        profileName = findViewById(R.id.user_profile_name);
        profileBio = findViewById(R.id.user_profile_short_bio);
        following = findViewById(R.id.following);
        followers = findViewById(R.id.followers);
        recyclerview = findViewById(R.id.rview);
        recyclerview.setNestedScrollingEnabled(false);

        emoji = findViewById(R.id.emoji);
        myStatusUpdate = findViewById(R.id.myStatus);
        postBtn = findViewById(R.id.postStatus);
        postBtn.setVisibility(View.GONE);
        emoji.setVisibility(View.GONE);

        icon = findViewById(R.id.noPostsIcon);
        emptyTag = findViewById(R.id.empty_tag);

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

                mSwipeRefreshLayout.setRefreshing(true);
                fetchPosts();
            }
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        profileRef = FirebaseDatabase.getInstance().getReference("users/"+phone);
        myPostUpdates = FirebaseDatabase.getInstance().getReference("posts/"+phone);

        myStatusUpdate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    emoji.setVisibility(View.VISIBLE);
                    postBtn.setVisibility(View.VISIBLE);
                }
                else {

                    if(myStatusUpdate.getText().toString().length() > 1){
                        postBtn.setVisibility(View.VISIBLE);
                        emoji.setVisibility(View.VISIBLE);
                    } else {
                        postBtn.setVisibility(View.GONE);
                        emoji.setVisibility(View.GONE);
                    }

                }
            }
        });

        emojIcon = new EmojIconActions(ViewProfile.this, rootView, myStatusUpdate, emoji);
        emojIcon.setUseSystemEmoji(false); //if we set this to true then the default emojis for chat wil be the system emojis
        emojIcon.setIconsIds(R.drawable.ic_action_keyboard, R.drawable.smiley);
        emojIcon.setKeyboardListener(new EmojIconActions.KeyboardListener() {
            @Override
            public void onKeyboardOpen() {
                Log.e(TAG, "Keyboard opened!");
            }

            @Override
            public void onKeyboardClose() {
                emojIcon.closeEmojIcon();
                Log.e(TAG, "Keyboard closed");
            }
        });

        myListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mSwipeRefreshLayout.setRefreshing(false);
                try {
                    myUserDetails = dataSnapshot.getValue(UserModel.class);
                    profileName.setText(myUserDetails.getFirstname() + " " + myUserDetails.getLastname());
                    profileBio.setText(myUserDetails.getBio());

                    Double totalFollowers = Double.valueOf(myUserDetails.getFollowers());
                    Double totalFollowing = Double.valueOf(myUserDetails.getFollowing());
                    /**
                     * Followers counter
                     */

                    //below 1000
                    if(totalFollowers < 1000){
                        DecimalFormat value = new DecimalFormat("#");
                        followers.setText(""+value.format(totalFollowers));
                    }

                    // 1000 to 999,999
                    else if(totalFollowers >= 1000 && totalFollowers <= 999999){
                        if(totalFollowers % 1000 == 0){ //No remainder
                            DecimalFormat value = new DecimalFormat("#####");
                            followers.setText(""+value.format(totalFollowers/1000)+"K");
                        }

                        else { //Has remainder 999.9K
                            DecimalFormat value = new DecimalFormat("######.#");
                            Double divided = totalFollowers/1000;
                            if(value.format(divided).equals("1000")){
                                followers.setText("1M"); //if rounded off
                            } else {
                                followers.setText(""+value.format(divided)+"K");
                            }
                        }
                    }

                    // 1,000,0000 to 999,999,999
                    else if(totalFollowers >= 1000000 && totalFollowers <= 999999999){
                        if(totalFollowers % 1000000 == 0) { //No remainder
                            DecimalFormat value = new DecimalFormat("#");
                            followers.setText(""+value.format(totalFollowers/1000000)+"M");
                        }

                        else { //Has remainder 9.9M, 999.9M etc
                            DecimalFormat value = new DecimalFormat("#.#");
                            if(value.format(totalFollowers/1000000).equals("1000")){
                                followers.setText("1B"); //if rounded off
                            } else {
                                followers.setText(""+value.format(totalFollowers/1000000)+"M");
                            }
                        }
                    }

                    /**
                     * Following counter
                     */
                    //below 1000
                    if(totalFollowing < 1000){
                        DecimalFormat value = new DecimalFormat("#");
                        following.setText(""+value.format(totalFollowing));
                    }

                    // 1000 to 999,999
                    else if(totalFollowing >= 1000 && totalFollowing <= 999999){
                        if(totalFollowing % 1000 == 0){ //No remainder
                            DecimalFormat value = new DecimalFormat("#####");
                            following.setText(""+value.format(totalFollowing/1000)+"K");
                        }

                        else { //Has remainder 999.9K
                            DecimalFormat value = new DecimalFormat("######.#");
                            Double divided = totalFollowing/1000;
                            if(value.format(divided).equals("1000")){
                                following.setText("1M"); //if rounded off
                            } else {
                                following.setText(""+value.format(divided)+"K");
                            }
                        }
                    }

                    // 1,000,0000 to 999,999,999
                    else if(totalFollowing >= 1000000 && totalFollowing <= 999999999){
                        if(totalFollowing % 1000000 == 0) { //No remainder
                            DecimalFormat value = new DecimalFormat("#");
                            following.setText(""+value.format(totalFollowing/1000000)+"M");
                        }

                        else { //Has remainder 9.9M, 999.9M etc
                            DecimalFormat value = new DecimalFormat("#.#");
                            if(value.format(totalFollowing/1000000).equals("1000")){
                                following.setText("1B"); //if rounded off
                            } else {
                                following.setText(""+value.format(totalFollowing/1000000)+"M");
                            }
                        }
                    }



                } catch (Exception e){}

                try {
                    //Load food image
                    Picasso.with(ViewProfile.this).load(myUserDetails.getProfilePic()).fit().centerCrop()
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(profilePhoto);
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        profileRef.addValueEventListener(myListener);

        postBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwipeRefreshLayout.setRefreshing(true);
                //Get current date
                GetCurrentDate currentDate = new GetCurrentDate();
                String postDate = currentDate.getDate();

                if(!myStatusUpdate.getText().toString().equals("")){
                    StatusUpdateModel statusUpdate = new StatusUpdateModel();
                    statusUpdate.setStatus(myStatusUpdate.getText().toString());
                    statusUpdate.setAuthor(myPhone);
                    statusUpdate.setPostedTo(phone);
                    statusUpdate.setTimePosted(postDate);
                    String key = myPostUpdates.push().getKey();
                    myPostUpdates.child(key).setValue(statusUpdate).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mSwipeRefreshLayout.setRefreshing(false);
                            myStatusUpdate.setText("");
                            myStatusUpdate.clearFocus();
                            statusUpdate.key = key;
                            statusUpdates.add(statusUpdate);

                            emptyTag.setVisibility(View.GONE);
                            icon.setVisibility(View.GONE);
                            recyclerview.setVisibility(View.VISIBLE);
                            Collections.reverse(statusUpdates);
                            StatusUpdateAdapter recycler = new StatusUpdateAdapter(ViewProfile.this, statusUpdates);
                            RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(ViewProfile.this);
                            recyclerview.setLayoutManager(layoutmanager);
                            recycler.notifyDataSetChanged();
                            recyclerview.setAdapter(recycler);
                        }
                    });

                } else {
                    mSwipeRefreshLayout.setRefreshing(false);
                    SafeToast.makeText(ViewProfile.this, "Cannot be empty!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        emoji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emojIcon.ShowEmojIcon();
            }
        });

        profilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent slideactivity = new Intent(ViewProfile.this, ViewImage.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("imageURL", myUserDetails.getProfilePic());
                    startActivity(slideactivity);
                } catch (Exception e){}
            }
        });
    }

    private void fetchPosts() {
        //Fetch the updates from status_updates node
        myPostUpdates.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mSwipeRefreshLayout.setRefreshing(true);
                statusUpdates = new ArrayList<>();
                for(DataSnapshot updates : dataSnapshot.getChildren()){
                    StatusUpdateModel statusUpdateModel = updates.getValue(StatusUpdateModel.class);
                    statusUpdateModel.key = updates.getKey();
                    statusUpdates.add(statusUpdateModel);
                }

                try {
                    mSwipeRefreshLayout.setRefreshing(false);
                    if (!statusUpdates.isEmpty()) {
                        emptyTag.setVisibility(View.GONE);
                        icon.setVisibility(View.GONE);
                        recyclerview.setVisibility(View.VISIBLE);
                        Collections.reverse(statusUpdates);
                        recyclerview.setVisibility(View.VISIBLE);
                        StatusUpdateAdapter recycler = new StatusUpdateAdapter(ViewProfile.this, statusUpdates);
                        RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(ViewProfile.this);
                        recyclerview.setLayoutManager(layoutmanager);

                        recycler.notifyDataSetChanged();
                        recyclerview.setAdapter(recycler);
                    } else {
                        emptyTag.setText("NO POSTS");
                        emptyTag.setVisibility(View.VISIBLE);
                        icon.setVisibility(View.VISIBLE);
                        recyclerview.setVisibility(View.GONE);
                    }
                }

                catch (Exception e){
                    emptyTag.setVisibility(View.VISIBLE);
                    emptyTag.setText("ERROR");
                    recyclerview.setVisibility(View.VISIBLE);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        profileRef.removeEventListener(myListener);
    }

    @Override
    public void onRefresh() {
        fetchPosts();
    }
}
