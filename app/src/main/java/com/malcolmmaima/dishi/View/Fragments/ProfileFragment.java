package com.malcolmmaima.dishi.View.Fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.alexzh.circleimageview.CircleImageView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.collect.Range;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.malcolmmaima.dishi.Controller.GetCurrentDate;
import com.malcolmmaima.dishi.Model.StatusUpdateModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.malcolmmaima.dishi.View.Activities.ViewStatus;
import com.malcolmmaima.dishi.View.Adapter.StatusUpdateAdapter;
import com.squareup.picasso.Picasso;

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

public class ProfileFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "ProfileFragment";
    List<StatusUpdateModel> statusUpdates;
    RecyclerView recyclerview;
    String myPhone;

    DatabaseReference myRef, myPostUpdates, followersCounterRef, followingCounterref;
    ValueEventListener myListener, followersCounterListener, followingCounterListener;
    FirebaseUser user;

    CircleImageView profilePhoto;
    TextView profileName, profileBio, following, followers;
    ImageButton emoji;
    EmojiconEditText myStatusUpdate;
    View rootView;
    EmojIconActions emojIcon;
    Button postBtn;
    ImageButton imageUpload;
    UserModel myUserDetails;

    TextView emptyTag;
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
        imageUpload = v.findViewById(R.id.camera);
        selectedImage = v.findViewById(R.id.selectedImage);
        progressBar = v.findViewById(R.id.progressBar);
        recyclerview = v.findViewById(R.id.rview);
        recyclerview.setNestedScrollingEnabled(false);

        emoji = v.findViewById(R.id.emoji);
        myStatusUpdate = v.findViewById(R.id.myStatus);
        postBtn = v.findViewById(R.id.postStatus);
        postBtn.setVisibility(View.GONE);
        imageUpload.setVisibility(View.GONE);
        emoji.setVisibility(View.GONE);

        icon = v.findViewById(R.id.noPostsIcon);
        emptyTag = v.findViewById(R.id.empty_tag);

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
                fetchPosts();
            }
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
        myPostUpdates = FirebaseDatabase.getInstance().getReference("posts/"+myPhone);
        followersCounterRef = FirebaseDatabase.getInstance().getReference("followers/"+myPhone);
        followingCounterref = FirebaseDatabase.getInstance().getReference("following/"+myPhone);

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
                    Picasso.with(getContext()).load(myUserDetails.getProfilePic()).fit().centerCrop()
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

                if(myStatusUpdate.getText().toString().equals("") && selectedImage.isShown()){
                    uploadImage(); //This will upload image then on successful upload call uploadContent()
                }

                else if(myStatusUpdate.getText().toString().equals("") && !selectedImage.isShown()){
                    mSwipeRefreshLayout.setRefreshing(false);
                    SafeToast.makeText(getContext(), "Cannot be empty!", Toast.LENGTH_SHORT).show();
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
                    Intent slideactivity = new Intent(getContext(), ViewImage.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("imageURL", myUserDetails.getProfilePic());
                    startActivity(slideactivity);
                } catch (Exception e){}
            }
        });
        return  v;
    }

    private void uploadContent(String imgLink) {
        //Get current date
        GetCurrentDate currentDate = new GetCurrentDate();
        String postDate = currentDate.getDate();

        StatusUpdateModel statusUpdate = new StatusUpdateModel();
        statusUpdate.setStatus(myStatusUpdate.getText().toString().trim());
        statusUpdate.setAuthor(myPhone);
        statusUpdate.setPostedTo(myPhone);
        statusUpdate.setTimePosted(postDate);
        statusUpdate.setImageShare(imgLink);
        String key = myPostUpdates.push().getKey();
        myPostUpdates.child(key).setValue(statusUpdate).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mSwipeRefreshLayout.setRefreshing(false);
                myStatusUpdate.setText("");
                myStatusUpdate.clearFocus();
                statusUpdate.key = key;
                statusUpdates.add(0,statusUpdate);

                emptyTag.setVisibility(View.GONE);
                icon.setVisibility(View.GONE);
                recyclerview.setVisibility(View.VISIBLE);
                StatusUpdateAdapter recycler = new StatusUpdateAdapter(getContext(), statusUpdates);
                RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                recyclerview.setLayoutManager(layoutmanager);
                recycler.notifyItemInserted(0);
                recyclerview.setAdapter(recycler);
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
                                    + UUID.randomUUID().toString());

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
                        StatusUpdateAdapter recycler = new StatusUpdateAdapter(getContext(), statusUpdates);
                        RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
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
        myRef.removeEventListener(myListener);
        followersCounterRef.removeEventListener(followersCounterListener);
        followingCounterref.removeEventListener(followingCounterListener);
    }

    @Override
    public void onRefresh() {
        fetchPosts();
    }
}