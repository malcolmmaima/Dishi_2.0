package com.malcolmmaima.dishi.View.Fragments;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
import com.malcolmmaima.dishi.Controller.Utils.GenerateThumbnails;
import com.malcolmmaima.dishi.Controller.Utils.GetCurrentDate;
import com.malcolmmaima.dishi.Model.StatusUpdateModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.FollowersFollowing;
import com.malcolmmaima.dishi.View.Activities.SearchActivity;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.malcolmmaima.dishi.View.Activities.ViewProfile;
import com.malcolmmaima.dishi.View.Activities.ViewReview;
import com.malcolmmaima.dishi.View.Adapter.StatusUpdateAdapter;
import com.squareup.picasso.Picasso;
import com.volokh.danylo.hashtaghelper.HashTagHelper;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import io.fabric.sdk.android.services.common.SafeToast;

import static android.app.Activity.RESULT_OK;
import static com.crashlytics.android.core.CrashlyticsCore.TAG;

public class ProfileFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "ProfileFragment";
    List<StatusUpdateModel> statusUpdates;
    RecyclerView recyclerview;
    String myPhone;

    Query posts;
    LinearLayoutManager layoutmanager;
    DatabaseReference myRef, myPostUpdates, followersCounterRef, followingCounterref;
    ValueEventListener myListener, followersCounterListener, followingCounterListener;
    ChildEventListener postUpdatesListener;
    FirebaseUser user;

    CircleImageView profilePhoto;
    TextView loadMore;
    MyTextView_Roboto_Medium profileName;
    MyTextView_Roboto_Light profileBio;
    MyTextView_Roboto_Regular following, followers;
    LinearLayout followingLayout, followersLayout;
    ImageButton emoji;
    EmojiconEditText myStatusUpdate;
    View rootView;
    EmojIconActions emojIcon;
    Button postBtn;
    ImageButton imageUpload;
    UserModel myUserDetails;
    HashTagHelper mTextHashTagHelper;

    MyTextView_Roboto_Regular emptyTag;
    AppCompatImageView icon;
    ImageView selectedImage;
    SwipeRefreshLayout mSwipeRefreshLayout;

    // instance for firebase storage and StorageReference
    FirebaseStorage storage;
    StorageReference storageReference;

    private ProgressBar progressBar;
    private int progressStatus = 0;

    // Uri indicates, where the image will be picked from
    private Uri filePath;

    // request code
    private final int PICK_IMAGE_REQUEST = 22;

    private int mPosts = 5;
    private int defaultPosts = 5;
    StatusUpdateAdapter recycler;

    public static ProfileFragment newInstance() {
        ProfileFragment fragment = new ProfileFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_user_profile, container, false);
        rootView = v.findViewById(R.id.activity_main);
        profilePhoto = v.findViewById(R.id.user_profile_photo);
        profileName = v.findViewById(R.id.user_profile_name);
        profileBio = v.findViewById(R.id.user_profile_short_bio);
        following = v.findViewById(R.id.following);
        followers = v.findViewById(R.id.followers);
        followingLayout = v.findViewById(R.id.followingLayout);
        followersLayout = v.findViewById(R.id.followersLayout);
        imageUpload = v.findViewById(R.id.camera);
        selectedImage = v.findViewById(R.id.selectedImage);
        progressBar = v.findViewById(R.id.progressBar);
        recyclerview = v.findViewById(R.id.rview);
        recyclerview.setNestedScrollingEnabled(false);
        loadMore = v.findViewById(R.id.loadMore);
        loadMore.setVisibility(View.GONE);

        emoji = v.findViewById(R.id.emoji);
        myStatusUpdate = v.findViewById(R.id.myStatus);
        postBtn = v.findViewById(R.id.postStatus);
        postBtn.setVisibility(View.GONE);
        imageUpload.setVisibility(View.GONE);
        emoji.setVisibility(View.GONE);

        icon = v.findViewById(R.id.noPostsIcon);
        emptyTag = v.findViewById(R.id.empty_tag);

        layoutmanager = new LinearLayoutManager(getContext());
        recyclerview.setLayoutManager(layoutmanager);
        statusUpdates = new ArrayList<>();
        recycler = new StatusUpdateAdapter(getContext(), statusUpdates);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
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
                fetchPosts(defaultPosts);
            }
        });

        loadMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPosts = mPosts + 5;
                fetchPosts(mPosts);
            }
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
        myPostUpdates = FirebaseDatabase.getInstance().getReference("posts/"+myPhone);
        followersCounterRef = FirebaseDatabase.getInstance().getReference("followers/"+myPhone);
        followingCounterref = FirebaseDatabase.getInstance().getReference("following/"+myPhone);

        followingLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainActivity = new Intent(getContext(), FollowersFollowing.class);
                mainActivity.putExtra("phone", myPhone);
                mainActivity.putExtra("target", "following");
                mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainActivity);
            }
        });

        followersLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainActivity = new Intent(getContext(), FollowersFollowing.class);
                mainActivity.putExtra("phone", myPhone);
                mainActivity.putExtra("target", "followers");
                mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainActivity);
            }
        });

        // get the Firebase  storage reference
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        //Keep track of total followers
        followersCounterListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    int totalFollowers = (int) dataSnapshot.getChildrenCount();
                    myRef.child("followers").setValue(totalFollowers);
                } catch (Exception e){}
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        followersCounterRef.addValueEventListener(followersCounterListener);

        //Keep track of total following
        followingCounterListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    int totalFollowing = (int) dataSnapshot.getChildrenCount();
                    myRef.child("following").setValue(totalFollowing);
                } catch (Exception e){}
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        followingCounterref.addValueEventListener(followingCounterListener);

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

        emojIcon = new EmojIconActions(getContext(), rootView, myStatusUpdate, emoji);
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
                    try {
                        Linkify.addLinks(profileBio, Linkify.ALL);
                    } catch(Exception e){
                        Log.e(TAG, "onBindViewHolder: ", e);
                    }

                    //handle hashtags
                    if(myUserDetails.getBio().contains("#")){
                        mTextHashTagHelper = HashTagHelper.Creator.create(getResources().getColor(R.color.colorPrimary),
                                new HashTagHelper.OnHashTagClickListener() {
                                    @Override
                                    public void onHashTagClicked(String hashTag) {
                                        String searchHashTag = "#"+hashTag;
                                        Intent slideactivity = new Intent(getContext(), SearchActivity.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                        slideactivity.putExtra("searchString", searchHashTag);
                                        startActivity(slideactivity);
                                    }
                                });

                        mTextHashTagHelper.handle(profileBio);
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
                        Picasso.with(getContext()).load(myUserDetails.getProfilePicSmall()).fit().centerCrop()
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(profilePhoto);
                    }

                    else {
                        Picasso.with(getContext()).load(myUserDetails.getProfilePic()).fit().centerCrop()
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(profilePhoto);
                    }
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myRef.addValueEventListener(myListener);

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
                    SafeToast.makeText(getContext(), "Cannot be empty!", Toast.LENGTH_SHORT).show();
                    myStatusUpdate.setEnabled(true);
                    imageUpload.setEnabled(true);
                    postBtn.setEnabled(true);
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
                        Intent slideactivity = new Intent(getContext(), ViewImage.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        slideactivity.putExtra("imageURL", myUserDetails.getProfilePicBig());
                        startActivity(slideactivity);
                    }

                    else {
                        Intent slideactivity = new Intent(getContext(), ViewImage.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        slideactivity.putExtra("imageURL", myUserDetails.getProfilePic());
                        startActivity(slideactivity);
                    }
                } catch (Exception e){
                    Log.e(TAG, "onClick: ", e);
                }
            }
        });

        return  v;
    }

    private void uploadContent(String imgLink) {
        //Get current date
        GetCurrentDate currentDate = new GetCurrentDate();
        String postDate = currentDate.getDate();
        GenerateThumbnails thumbnails = new GenerateThumbnails();

        StatusUpdateModel statusUpdate = new StatusUpdateModel();
        statusUpdate.setStatus(myStatusUpdate.getText().toString().trim());
        statusUpdate.setAuthor(myPhone);
        statusUpdate.setPostedTo(myPhone);
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
                mSwipeRefreshLayout.setRefreshing(false);

                myStatusUpdate.setEnabled(true);
                imageUpload.setEnabled(true);
                postBtn.setEnabled(true);

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

    // Select Image method
    private void SelectImage()
    {

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
    public void onActivityResult(int requestCode,
                                 int resultCode,
                                 Intent data)
    {

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
                        .getBitmap(getContext().getContentResolver(),
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
    private void uploadImage()
    {
        if (filePath != null) {

            // Code for showing progressDialog while uploading
            progressBar.setVisibility(View.VISIBLE);

            // Defining the child of storageReference
            StorageReference ref
                    = storageReference
                    .child(
                            "Users/"+myPhone+"/"
                                    + System.currentTimeMillis()+ "." + GetFileExtension(filePath));

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
                                        .makeText(getContext(),
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

    // Creating Method to get the selected image file Extension from File Path URI.
    public String GetFileExtension(Uri uri) {

        ContentResolver contentResolver = getContext().getContentResolver();

        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();

        // Returning the file Extension.
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri)) ;

    }

    private void fetchPosts(int postCount) {
        mSwipeRefreshLayout.setRefreshing(true);
        //Fetch the updates from status_updates node
        statusUpdates.clear();

        myPostUpdates.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()) {
                    if(!dataSnapshot.exists()){
                        mSwipeRefreshLayout.setRefreshing(false);
                        emptyTag.setText("NO POSTS");
                        emptyTag.setVisibility(View.VISIBLE);
                        icon.setVisibility(View.VISIBLE);
                        recyclerview.setVisibility(View.GONE);
                    }
                }

                else {
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
                                    StatusUpdateAdapter recycler = new StatusUpdateAdapter(getContext(), statusUpdates);
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try { myRef.removeEventListener(myListener); } catch (Exception e){}
        try { followersCounterRef.removeEventListener(followersCounterListener); } catch (Exception e){}
        try { followingCounterref.removeEventListener(followingCounterListener); } catch (Exception e){}
        //myPostUpdates.removeEventListener(postUodatesChildListener);
    }

    @Override
    public void onRefresh() {
        fetchPosts(defaultPosts);
    }
}