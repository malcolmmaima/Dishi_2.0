package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alexzh.circleimageview.CircleImageView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Light;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Controller.Utils.CommentKeyBoardFix;
import com.malcolmmaima.dishi.Controller.Utils.GenerateThumbnails;
import com.malcolmmaima.dishi.Controller.Utils.GetCurrentDate;
import com.malcolmmaima.dishi.Model.MessageModel;
import com.malcolmmaima.dishi.Model.NotificationModel;
import com.malcolmmaima.dishi.Model.StatusUpdateModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.NewsFeedAdapter;
import com.malcolmmaima.dishi.View.Adapter.StatusUpdateAdapter;
import com.squareup.picasso.Picasso;
import com.volokh.danylo.hashtaghelper.HashTagHelper;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import io.fabric.sdk.android.services.common.SafeToast;

public class ViewProfile extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    String phone, myPhone;
    Menu myMenu;
    private static final String TAG = "ProfileActivity";
    List<StatusUpdateModel> statusUpdates;
    RecyclerView recyclerview;
    TextView loadMore;

    DatabaseReference profileRef, myPostUpdates, profileFollowers,
            followersCounterRef, followingCounterref, followRequests, myRef, myBlockedUsers;
    ValueEventListener myListener, profileFollowersListener, followersCounterListener,
            followingCounterListener, blockedUsersListener;
    ChildEventListener postUpdatesListener;
    Query posts;
    FirebaseUser user;

    CircleImageView profilePhoto;
    MyTextView_Roboto_Medium profileName;
    MyTextView_Roboto_Light profileBio;
    MyTextView_Roboto_Regular following, followers;
    LinearLayout followingLayout, followersLayout;
    RelativeLayout statusActions;
    FrameLayout frame;
    ImageButton emoji;
    EmojiconEditText myStatusUpdate;
    View rootView;
    EmojIconActions emojIcon;
    Button postBtn;
    ImageButton imageUpload;
    AppCompatButton followBtn;
    UserModel myUserDetails;

    HashTagHelper mTextHashTagHelper;
    MyTextView_Roboto_Regular emptyTag;
    AppCompatImageView icon;
    ImageView selectedImage,viewRestaurant;
    SwipeRefreshLayout mSwipeRefreshLayout;

    LinearLayoutManager layoutmanager;
    StatusUpdateAdapter recycler;

    // instance for firebase storage and StorageReference
    FirebaseStorage storage;
    StorageReference storageReference;

    private ProgressBar progressBar;
    private int mPosts = 5;
    private int defaultPosts = 5;

    // Uri indicates, where the image will be picked from
    private Uri filePath;

    // request code
    private final int PICK_IMAGE_REQUEST = 22;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                                try {
                                    Boolean locked = dataSnapshot.getValue(Boolean.class);

                                    if (locked == true) {
                                        Intent slideactivity = new Intent(ViewProfile.this, SecurityPin.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        slideactivity.putExtra("pinType", "resume");
                                        startActivity(slideactivity);
                                    }
                                } catch (Exception e){
                                    Log.e(TAG, "onDataChange: ", e);
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

            loadProfile();
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
                            Boolean locked = dataSnapshot.getValue(Boolean.class);

                            if(locked == true){
                                Intent slideactivity = new Intent(ViewProfile.this, SecurityPin.class)
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

    private void loadProfile() {
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
        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        String intentType = getIntent().getStringExtra("type");

        if(intentType != null){
            //update the notification in db to seen = true
            if(intentType.equals("notification")){
                String notifKey = getIntent().getStringExtra("notifKey");
                DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notifications/"+myPhone);

                //update all notifications status to seen = true
                notificationRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        notificationRef.child(notifKey).child("seen").setValue(true);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        }

        if(myPhone.equals(phone)){
            finish();
            Toast.makeText(this, "Not allowed!", Toast.LENGTH_SHORT).show();
        }

        rootView = findViewById(R.id.activity_main);
        profilePhoto = findViewById(R.id.user_profile_photo);
        profileName = findViewById(R.id.user_profile_name);
        profileBio = findViewById(R.id.user_profile_short_bio);
        following = findViewById(R.id.following);
        followers = findViewById(R.id.followers);
        followingLayout = findViewById(R.id.followingLayout);
        followersLayout = findViewById(R.id.followersLayout);
        recyclerview = findViewById(R.id.rview);
        recyclerview.setNestedScrollingEnabled(false);
        recyclerview.setHasFixedSize(true);
        loadMore = findViewById(R.id.loadMore);
        loadMore.setVisibility(View.GONE);
        followBtn = findViewById(R.id.follow);
        followBtn.setEnabled(false);
        emoji = findViewById(R.id.emoji);
        myStatusUpdate = findViewById(R.id.myStatus);
        imageUpload = findViewById(R.id.camera);
        imageUpload.setVisibility(View.GONE);
        selectedImage = findViewById(R.id.selectedImage);
        progressBar = findViewById(R.id.progressBar);
        postBtn = findViewById(R.id.postStatus);
        postBtn.setVisibility(View.GONE);
        emoji.setVisibility(View.GONE);
        viewRestaurant = findViewById(R.id.viewRestaurant);
        viewRestaurant.setVisibility(View.GONE);
        frame = findViewById(R.id.frame);
        frame.setVisibility(View.GONE);
        statusActions = findViewById(R.id.statusActions);
        statusActions.setVisibility(View.GONE);

        layoutmanager = new LinearLayoutManager(ViewProfile.this);
        recyclerview.setLayoutManager(layoutmanager);
        statusUpdates = new ArrayList<>();
        recycler = new StatusUpdateAdapter(ViewProfile.this, statusUpdates);

        followingLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainActivity = new Intent(ViewProfile.this, FollowersFollowing.class);
                mainActivity.putExtra("phone", phone);
                mainActivity.putExtra("target", "following");
                mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainActivity);
            }
        });

        followersLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainActivity = new Intent(ViewProfile.this, FollowersFollowing.class);
                mainActivity.putExtra("phone", phone);
                mainActivity.putExtra("target", "followers");
                mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainActivity);
            }
        });

        icon = findViewById(R.id.noPostsIcon);
        emptyTag = findViewById(R.id.empty_tag);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);



        profileRef = FirebaseDatabase.getInstance().getReference("users/"+phone);
        myPostUpdates = FirebaseDatabase.getInstance().getReference("posts/"+phone);
        profileFollowers = FirebaseDatabase.getInstance().getReference("followers/"+phone);
        DatabaseReference myFollowing = FirebaseDatabase.getInstance().getReference("following/"+myPhone);
        followersCounterRef = FirebaseDatabase.getInstance().getReference("followers/"+phone);
        followingCounterref = FirebaseDatabase.getInstance().getReference("following/"+phone);
        followRequests = FirebaseDatabase.getInstance().getReference("followRequests/"+phone);
        myBlockedUsers = FirebaseDatabase.getInstance().getReference("blocked/"+myPhone);
        // get the Firebase  storage reference
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        //check to see if i am already following this profile
        profileFollowersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                followBtn.setEnabled(true);
                if(dataSnapshot.exists()){
                    followBtn.getBackground().setColorFilter(getResources().getColor(R.color.colorPrimaryDark), PorterDuff.Mode.MULTIPLY);
                    followBtn.setText("UNFOLLOW");
                }  else {
                    //check to see if private
                    profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            myUserDetails = dataSnapshot.getValue(UserModel.class);

                            try {
                                if (myUserDetails.getAccountPrivacy().equals("private")) {

                                    //check to see if i have a pending follow request sent to this profile
                                    followRequests.child(myPhone).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.exists()) {
                                                followBtn.setText("REQUESTED");
                                            } else {
                                                followBtn.getBackground().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
                                                followBtn.setText("FOLLOW");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                }
                            } catch (Exception e){
                                Log.e(TAG, "onDataChange: ", e);
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
        };

        //Keep track of total followers
        followersCounterListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    int totalFollowers = (int) dataSnapshot.getChildrenCount();
                    profileRef.child("followers").setValue(totalFollowers);
                } catch (Exception e){}
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        //Keep track of total following
        followingCounterListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    int totalFollowing = (int) dataSnapshot.getChildrenCount();
                    profileRef.child("following").setValue(totalFollowing);
                } catch (Exception e){}
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        followBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(followBtn.getText().toString().equals("FOLLOW")){

                    //Check to see if profile is private
                    profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            myUserDetails = dataSnapshot.getValue(UserModel.class);

                            try {
                                if (myUserDetails.getAccountPrivacy().equals("private")) {
                                    //send follow request

                                    followRequests.child(myPhone).setValue("followrequest").addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            try {
                                                followBtn.setText("REQUESTED");
                                                Snackbar.make(rootView, "Request sent", Snackbar.LENGTH_LONG).show();
                                            } catch (Exception er) {
                                                Log.e(TAG, "onFailure: ", er);
                                            }

                                            sendNotification("wants to follow you", "followrequest");
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            try {
                                                followBtn.setText("REQUESTED");
                                                Snackbar.make(rootView, "Something went wrong", Snackbar.LENGTH_LONG).show();
                                            } catch (Exception er) {
                                                Log.e(TAG, "onFailure: ", er);
                                            }
                                        }
                                    });

                                }

                                if (myUserDetails.getAccountPrivacy().equals("public")) {
                                    //automatically follow
                                    profileFollowers.child(myPhone).setValue("follow").addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            myFollowing.child(phone).setValue("follow");
                                            sendNotification("followed you", "followedwall");
                                        }
                                    });
                                }
                            } catch (Exception e){
                                Log.e(TAG, "onDataChange: ", e);
                            }
                        }

                        private void sendNotification(String message, String type) {
                            DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notifications/"+phone);

                            String notifKey = notificationRef.push().getKey();
                            GetCurrentDate currentDate = new GetCurrentDate();

                            //send notification
                            NotificationModel followed = new NotificationModel();
                            followed.setFrom(myPhone);
                            followed.setType(type);
                            followed.setImage("");
                            followed.setSeen(false);
                            followed.setTimeStamp(currentDate.getDate());
                            followed.setMessage(message);

                            notificationRef.child(notifKey).setValue(followed); //send to db
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
                else if(followBtn.getText().toString().equals("REQUESTED")){
                    //Do nothing
                }
                else {
                    profileFollowers.child(myPhone).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            myFollowing.child(phone).removeValue();
                            followBtn.setText("FOLLOW");
                        }
                    });
                }
            }
        });

        myListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                //Does this user exist or nah
                if(!dataSnapshot.exists()){
                    finish();
                    SafeToast.makeText(ViewProfile.this, "User does not exist!", Toast.LENGTH_LONG).show();
                } else {
                    profileFollowers.child(myPhone).addValueEventListener(profileFollowersListener);
                    followersCounterRef.addValueEventListener(followersCounterListener);
                    followingCounterref.addValueEventListener(followingCounterListener);

                    try {
                        myUserDetails = dataSnapshot.getValue(UserModel.class);
                        profileName.setText(myUserDetails.getFirstname() + " " + myUserDetails.getLastname());
                        profileBio.setText(myUserDetails.getBio());

                        try {
                            Linkify.addLinks(profileBio, Linkify.ALL);
                        } catch (Exception e){
                            Log.e(TAG, "onDataChange: ", e);
                        }

                        //handle hashtags
                        if(myUserDetails.getBio().contains("#")){
                            mTextHashTagHelper = HashTagHelper.Creator.create(getResources().getColor(R.color.colorPrimary),
                                    new HashTagHelper.OnHashTagClickListener() {
                                        @Override
                                        public void onHashTagClicked(String hashTag) {
                                            String searchHashTag = "#"+hashTag;
                                            Intent slideactivity = new Intent(ViewProfile.this, SearchActivity.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                            slideactivity.putExtra("searchString", searchHashTag);
                                            startActivity(slideactivity);
                                        }
                                    });

                            mTextHashTagHelper.handle(profileBio);
                        }

                        if(myUserDetails.getAccount_type().equals("2")){
                            viewRestaurant.setVisibility(View.VISIBLE);
                        } else {
                            viewRestaurant.setVisibility(View.GONE);
                        }

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
                        //Load image
                        if(myUserDetails.getProfilePicSmall() != null){
                            Picasso.with(ViewProfile.this).load(myUserDetails.getProfilePicSmall()).fit().centerCrop()
                                    .placeholder(R.drawable.default_profile)
                                    .error(R.drawable.default_profile)
                                    .into(profilePhoto);
                        }

                        else {
                            Picasso.with(ViewProfile.this).load(myUserDetails.getProfilePic()).fit().centerCrop()
                                    .placeholder(R.drawable.default_profile)
                                    .error(R.drawable.default_profile)
                                    .into(profilePhoto);
                        }

                    } catch (Exception e){
                        Log.e(TAG, "onDataChange: ", e);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        profileRef.addValueEventListener(myListener);

        /**
         * Showing Swipe Refresh animation on activity create
         * As animation won't start on onCreate, post runnable is used. also placed after above listener which gets user details
         */
        mSwipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {

                profileCheck();
            }
        });

        loadMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPosts = mPosts + 5;
                fetchPosts(mPosts);
            }
        });

        recyclerview.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

            }
        });

        myStatusUpdate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    emoji.setVisibility(View.VISIBLE);
                    postBtn.setVisibility(View.VISIBLE);
                    imageUpload.setVisibility(View.VISIBLE);
                }
                else {

                    if(myStatusUpdate.getText().toString().length() > 1){
                        postBtn.setVisibility(View.VISIBLE);
                        imageUpload.setVisibility(View.VISIBLE);
                        emoji.setVisibility(View.VISIBLE);
                    } else {
                        postBtn.setVisibility(View.GONE);
                        imageUpload.setVisibility(View.GONE);
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

        viewRestaurant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                Intent slideactivity = new Intent(ViewProfile.this, ViewRestaurant.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                slideactivity.putExtra("restaurant_phone", phone);
                slideactivity.putExtra("distance", 0.0);
                slideactivity.putExtra("profilePic", myUserDetails.getProfilePic());
                Bundle bndlanimation =
                        null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    bndlanimation = ActivityOptions.makeCustomAnimation(ViewProfile.this, R.anim.animation, R.anim.animation2).toBundle();
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    startActivity(slideactivity, bndlanimation);
                }
            }
        });
        imageUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //https://www.geeksforgeeks.org/android-how-to-upload-an-image-on-firebase-storage/
                SelectImage();
            }
        });

        postBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwipeRefreshLayout.setRefreshing(true);
                myStatusUpdate.setEnabled(false);
                imageUpload.setEnabled(false);
                postBtn.setEnabled(false);
                if(myStatusUpdate.getText().toString().equals("") && selectedImage.isShown()){
                    uploadImage(); //This will upload image then on successful upload call uploadContent()
                }

                else if(myStatusUpdate.getText().toString().equals("") && !selectedImage.isShown()){
                    mSwipeRefreshLayout.setRefreshing(false);
                    myStatusUpdate.setEnabled(true);
                    imageUpload.setEnabled(true);
                    postBtn.setEnabled(true);
                    SafeToast.makeText(ViewProfile.this, "Cannot be empty!", Toast.LENGTH_SHORT).show();
                }

                else {
                    if(selectedImage.isShown()){
                        uploadImage(); //This will upload image then on successful upload call uploadContent()
                    } else {
                        String imgLink = null;
                        uploadContent(imgLink);
                    }

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

                    if(myUserDetails.getProfilePicBig() != null){
                        Intent slideactivity = new Intent(ViewProfile.this, ViewImage.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        slideactivity.putExtra("imageURL", myUserDetails.getProfilePicBig());
                        startActivity(slideactivity);
                    }

                    else {
                        Intent slideactivity = new Intent(ViewProfile.this, ViewImage.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        slideactivity.putExtra("imageURL", myUserDetails.getProfilePic());
                        startActivity(slideactivity);
                    }
                } catch (Exception e){}
            }
        });
    }

    private void profileCheck() {
        mSwipeRefreshLayout.setRefreshing(true);
        try {
            profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    myUserDetails = dataSnapshot.getValue(UserModel.class);

                    try {
                        //hide profile posts
                        if (myUserDetails.getAccountPrivacy().equals("private")) {

                            //now check to see if i'm following this user
                            profileFollowers.child(myPhone).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.exists()){
                                        followingLayout.setClickable(true);
                                        followersLayout.setClickable(true);
                                        viewRestaurant.setClickable(true);
                                        statusActions.setVisibility(View.VISIBLE);
                                        frame.setVisibility(View.VISIBLE);
                                        fetchPosts(defaultPosts);
                                    } else {
                                        mSwipeRefreshLayout.setRefreshing(false);
                                        followingLayout.setClickable(false);
                                        followersLayout.setClickable(false);
                                        viewRestaurant.setClickable(false);
                                        recyclerview.setVisibility(View.GONE);
                                        icon.setVisibility(View.VISIBLE);
                                        icon.setImageResource(R.drawable.ic_locked);
                                        emptyTag.setVisibility(View.VISIBLE);
                                        statusActions.setVisibility(View.GONE);
                                        frame.setVisibility(View.GONE);
                                        emptyTag.setText("PRIVATE");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }

                        if (myUserDetails.getAccountPrivacy().equals("public")) {
                            statusActions.setVisibility(View.VISIBLE);
                            frame.setVisibility(View.VISIBLE);
                            fetchPosts(defaultPosts);
                        }
                    } catch (Exception e){
                        Log.e(TAG, "onDataChange: ", e);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } catch (Exception e){
            Log.e(TAG, "run: ", e);
        }

    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            loadProfile();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_profile_view, menu);
        myMenu = menu;

        MenuItem blockOption = myMenu.findItem(R.id.userBlock);
        MenuItem unblockOption = myMenu.findItem(R.id.userUnBlock);
        blockOption.setVisible(false);
        unblockOption.setVisible(false);

        blockedUsersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    blockOption.setVisible(true);
                    unblockOption.setVisible(false);
                } else {
                    for(DataSnapshot blocked : dataSnapshot.getChildren()){
                        if(blocked.getKey().equals(phone)){
                            unblockOption.setVisible(true);
                            blockOption.setVisible(false);
                        } else {
                            blockOption.setVisible(true);
                            unblockOption.setVisible(false);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myBlockedUsers.addValueEventListener(blockedUsersListener);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) { switch(item.getItemId()) {

        case R.id.sendMessage:
            Intent slideactivity = new Intent(ViewProfile.this, Chat.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            slideactivity.putExtra("fromPhone", myPhone);
            slideactivity.putExtra("toPhone", phone);
            Bundle bndlanimation =
                    null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                bndlanimation = ActivityOptions.makeCustomAnimation(ViewProfile.this, R.anim.animation,R.anim.animation2).toBundle();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                startActivity(slideactivity, bndlanimation);
            }
            return true;

        case R.id.userBlock:
            AlertDialog blockuser = new AlertDialog.Builder(ViewProfile.this)
                    //set message, title, and icon
                    .setMessage("Block "+myUserDetails.getFirstname() + " " + myUserDetails.getLastname()+"?")
                    //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                    //set three option buttons
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            myBlockedUsers.child(phone).setValue("blocked").addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    try {
                                        Snackbar.make(rootView, "Blocked", Snackbar.LENGTH_LONG).show();
                                    } catch (Exception er){
                                        Log.e(TAG, "onSuccess: ", er);
                                    }
                                }
                            });
                        }
                    })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //do nothing
                        }
                    })//setNegativeButton

                    .create();
            blockuser.show();
            return true;

        case R.id.userReport:
            AlertDialog reportUser = new AlertDialog.Builder(ViewProfile.this)
                    //set message, title, and icon
                    .setMessage("Report "+myUserDetails.getFirstname() + " " + myUserDetails.getLastname()+"?")
                    //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                    //set three option buttons
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Intent slideactivity = new Intent(ViewProfile.this, ReportAbuse.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            slideactivity.putExtra("type", "userReport");
                            slideactivity.putExtra("user", phone);
                            startActivity(slideactivity);
                        }
                    })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //do nothing
                        }
                    })//setNegativeButton

                    .create();
            reportUser.show();
            return true;

        case R.id.userUnBlock:
            AlertDialog unBlockUser = new AlertDialog.Builder(ViewProfile.this)
                    //set message, title, and icon
                    .setMessage("Unblock "+myUserDetails.getFirstname() + " " + myUserDetails.getLastname()+"?")
                    //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                    //set three option buttons
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            myBlockedUsers.child(phone).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    try {
                                        Snackbar.make(rootView, "Unblocked", Snackbar.LENGTH_LONG).show();
                                    } catch (Exception er){
                                        Log.e(TAG, "onSuccess: ", er);
                                    }
                                }
                            });
                        }
                    })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            //do nothing
                        }
                    })//setNegativeButton

                    .create();
            unBlockUser.show();
            return true;

    }
        return(super.onOptionsItemSelected(item));
    }

    private void fetchPosts(int postCount) {
        mSwipeRefreshLayout.setRefreshing(true);
        //Fetch the updates from status_updates node
        statusUpdates.clear();
        posts = myPostUpdates.limitToLast(postCount);
        postUpdatesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                try {
                    StatusUpdateModel statusUpdateModel = dataSnapshot.getValue(StatusUpdateModel.class);
                    statusUpdateModel.key = dataSnapshot.getKey();
                    if(!statusUpdates.contains(statusUpdateModel)){
                        statusUpdates.add(statusUpdateModel);
                        recyclerview.setHasFixedSize(true);
                    }

                    mSwipeRefreshLayout.setRefreshing(false);
                    if (!statusUpdates.isEmpty()) {
                        //Sort by most recent (based on timeStamp)
                        //Collections.reverse(statusUpdates);
                        try {
                            Collections.sort(statusUpdates, (update1, update2) -> (update2.getTimePosted().compareTo(update1.getTimePosted())));
                        } catch (Exception e){
                            Log.e(TAG, "onDataChange: ", e);
                        }
                        emptyTag.setVisibility(View.GONE);
                        icon.setVisibility(View.GONE);
                        recyclerview.setVisibility(View.VISIBLE);
                        recyclerview.setVisibility(View.VISIBLE);
                        StatusUpdateAdapter recycler = new StatusUpdateAdapter(ViewProfile.this, statusUpdates);
                        recyclerview.setLayoutManager(layoutmanager);

                        recycler.notifyDataSetChanged();
                        recyclerview.setAdapter(recycler);
                    } else {
                        emptyTag.setText("NO POSTS");
                        emptyTag.setVisibility(View.VISIBLE);
                        icon.setVisibility(View.VISIBLE);
                        recyclerview.setVisibility(View.GONE);
                    }

                    //show/hide loadmore...
                    myPostUpdates.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            loadMore.setVisibility(View.GONE);
                            if(dataSnapshot.getChildrenCount() > statusUpdates.size()){
                                loadMore.setVisibility(View.VISIBLE);
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                } catch (Exception e){
                    Log.e(TAG, "onDataChange: ", e);
                }

                try {

                }

                catch (Exception e){
                    emptyTag.setVisibility(View.VISIBLE);
                    emptyTag.setText("ERROR");
                    recyclerview.setVisibility(View.VISIBLE);

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
        posts.addChildEventListener(postUpdatesListener);

    }

    // Select Image method
    private void SelectImage() {

        // Defining Implicit Intent to mobile gallery
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(
                Intent.createChooser(
                        intent,
                        "Select Image from here..."),
                PICK_IMAGE_REQUEST);
    }

    // Override onActivityResult method
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode,
                resultCode,
                data);

        // checking request code and result code
        // if request code is PICK_IMAGE_REQUEST and
        // resultCode is RESULT_OK
        // then set image in the image view
        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            // Get the Uri of data
            filePath = data.getData();
            try {
                // Setting image on image view using Bitmap
                Bitmap bitmap = MediaStore
                        .Images
                        .Media
                        .getBitmap(ViewProfile.this.getContentResolver(),
                                filePath);
                selectedImage.setVisibility(View.VISIBLE);
                selectedImage.setImageBitmap(bitmap);
            }

            catch (IOException e) {
                // Log the exception
                selectedImage.setVisibility(View.GONE);
                e.printStackTrace();
            }
        }
    }

    // UploadImage method
    private void uploadImage() {
        if (filePath != null) {

            // Code for showing progressDialog while uploading
            progressBar.setVisibility(View.VISIBLE);

            // Defining the child of storageReference
            StorageReference ref
                    = storageReference
                    .child(
                            "Users/"+myPhone+"/"
                                    + System.currentTimeMillis()+ "." + GetFileExtension(filePath)); //switched from 'phone' to 'myPhone' ...
            // since we'll be limiting account quotas we dont want to charge someone's account for something they didn't upload

            // adding listeners on upload
            // or failure of image
            ref.putFile(filePath)
                    .addOnSuccessListener(
                            new OnSuccessListener<UploadTask.TaskSnapshot>() {

                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
                                {
                                    ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener() {
                                        @Override
                                        public void onSuccess(Object o) {
                                            // Image uploaded successfully
                                            // Dismiss dialog
                                            selectedImage.setVisibility(View.GONE);
                                            progressBar.setVisibility(View.GONE);

                                            String imgLink = o.toString();
                                            uploadContent(imgLink); //Now upload the status text content
                                        }
                                    });


                                }
                            })

                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e)
                        {

                            // Error, Image not uploaded
                            progressBar.setVisibility(View.GONE);
                            try {
                                Toast
                                        .makeText(ViewProfile.this,
                                                "Failed " + e.getMessage(),
                                                Toast.LENGTH_SHORT)
                                        .show();
                            } catch (Exception er){}
                        }
                    })
                    .addOnProgressListener(
                            new OnProgressListener<UploadTask.TaskSnapshot>() {

                                // Progress Listener for loading
                                // percentage on the dialog box
                                @Override
                                public void onProgress(
                                        UploadTask.TaskSnapshot taskSnapshot)
                                {
                                    double progress
                                            = (100.0
                                            * taskSnapshot.getBytesTransferred()
                                            / taskSnapshot.getTotalByteCount());

                                    progressBar.setProgress((int)progress);
                                }
                            });
        }
    }

    private void uploadContent(String imgLink) {
        //Get current date
        GetCurrentDate currentDate = new GetCurrentDate();
        String postDate = currentDate.getDate();
        GenerateThumbnails thumbnails = new GenerateThumbnails();

        StatusUpdateModel statusUpdate = new StatusUpdateModel();
        statusUpdate.setStatus(myStatusUpdate.getText().toString().trim());
        statusUpdate.setAuthor(myPhone);
        statusUpdate.setPostedTo(phone);
        statusUpdate.setTimePosted(postDate);

        if(imgLink != null){
            statusUpdate.setImageShare(imgLink);
            statusUpdate.setImageShareSmall(thumbnails.GenerateSmall(imgLink));
            statusUpdate.setImageShareMedium(thumbnails.GenerateMedium(imgLink));
            statusUpdate.setImageShareBig(thumbnails.GenerateBig(imgLink));
        }
        String key = myPostUpdates.push().getKey();
        myPostUpdates.child(key).setValue(statusUpdate).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                myStatusUpdate.setEnabled(true);
                imageUpload.setEnabled(true);
                postBtn.setEnabled(true);

                DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notifications/"+phone);

                String notifKey = notificationRef.push().getKey();
                GenerateThumbnails thumbnails1 = new GenerateThumbnails();

                //send notification
                NotificationModel postedOnWall = new NotificationModel();
                postedOnWall.setFrom(myPhone);
                postedOnWall.setType("postedwall");
                if(imgLink != null){
                    postedOnWall.setImage(thumbnails1.GenerateSmall(imgLink));
                }
                postedOnWall.setSeen(false);
                postedOnWall.setTimeStamp(postDate);
                postedOnWall.setMessage(key);

                notificationRef.child(notifKey).setValue(postedOnWall); //send to db

                myStatusUpdate.setText("");
                myStatusUpdate.clearFocus();
                fetchPosts(defaultPosts);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                myStatusUpdate.setEnabled(true);
                imageUpload.setEnabled(true);
                postBtn.setEnabled(true);
                try {
                    Snackbar.make(rootView, "Something went wrong", Snackbar.LENGTH_LONG).show();
                } catch (Exception er){
                    Log.e(TAG, "onFailure: ", e);
                }
            }
        });
    }

    // Creating Method to get the selected image file Extension from File Path URI.
    public String GetFileExtension(Uri uri) {

        ContentResolver contentResolver = getContentResolver();

        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();

        // Returning the file Extension.
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri)) ;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            profileRef.removeEventListener(myListener);
        } catch (Exception e){ }

        try {
            profileFollowers.removeEventListener(profileFollowersListener);
        } catch (Exception e){ }

        try {
            followersCounterRef.removeEventListener(followersCounterListener);
        } catch (Exception e){ }

        try {
            followingCounterref.removeEventListener(followingCounterListener);
        } catch (Exception e){ }

        try {
            myBlockedUsers.removeEventListener(blockedUsersListener);
        } catch (Exception e){ }

        try {
            posts.removeEventListener(postUpdatesListener);
        } catch (Exception e){

        }
    }

    @Override
    public void onRefresh() {
        profileCheck();
    }
}
