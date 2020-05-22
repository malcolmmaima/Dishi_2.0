package com.malcolmmaima.dishi.View.Fragments;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
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
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Controller.Utils.GenerateThumbnails;
import com.malcolmmaima.dishi.Controller.Utils.GetCurrentDate;
import com.malcolmmaima.dishi.Model.StatusUpdateModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.NewsFeedAdapter;
import com.malcolmmaima.dishi.View.Adapter.StatusUpdateAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import io.fabric.sdk.android.services.common.SafeToast;

import static android.app.Activity.RESULT_OK;

public class HomeFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    String TAG = "HomeFragment";
    List<StatusUpdateModel> statusUpdates;
    ProgressDialog progressDialog ;
    RecyclerView recyclerview;
    String myPhone;

    DatabaseReference followingRef, myPostUpdates, myBlockedUsersRef;
    FirebaseUser user;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;

    ImageButton emoji;
    EmojiconEditText myStatusUpdate;
    Button postBtn;
    ImageButton imageUpload;
    ImageView selectedImage;
    MyTextView_Roboto_Regular emptyTag;
    private ProgressBar progressBar;
    View rootView;
    EmojIconActions emojIcon;

    // Uri indicates, where the image will be picked from
    private Uri filePath;

    // request code
    private final int PICK_IMAGE_REQUEST = 22;

    // instance for firebase storage and StorageReference
    FirebaseStorage storage;
    StorageReference storageReference;


    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_home, container, false);
        rootView = v.findViewById(R.id.activity_main);
        progressDialog = new ProgressDialog(getContext());

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        followingRef = FirebaseDatabase.getInstance().getReference("following/"+myPhone);
        myPostUpdates = FirebaseDatabase.getInstance().getReference("posts/"+myPhone);
        myBlockedUsersRef = FirebaseDatabase.getInstance().getReference("blocked/"+myPhone);

        icon = v.findViewById(R.id.newsFeedIcon);
        emptyTag = v.findViewById(R.id.empty_tag);
        recyclerview = v.findViewById(R.id.rview);
        imageUpload = v.findViewById(R.id.camera);
        selectedImage = v.findViewById(R.id.selectedImage);
        progressBar = v.findViewById(R.id.progressBar);
        emoji = v.findViewById(R.id.emoji);
        myStatusUpdate = v.findViewById(R.id.myStatus);
        postBtn = v.findViewById(R.id.postStatus);
        postBtn.setVisibility(View.GONE);
        imageUpload.setVisibility(View.GONE);
        emoji.setVisibility(View.GONE);

        // get the Firebase  storage reference
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadNewsFeed();
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
                    myStatusUpdate.setEnabled(true);
                    imageUpload.setEnabled(true);
                    postBtn.setEnabled(true);
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

        /**
         * Showing Swipe Refresh animation on activity create
         * As animation won't start on onCreate, post runnable is used
         */
        mSwipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {

                mSwipeRefreshLayout.setRefreshing(true);

                // Fetching data from server
                loadNewsFeed();

            }
        });


        return  v;
    }

    private void loadNewsFeed() {
        statusUpdates = new ArrayList<>();
        mSwipeRefreshLayout.setRefreshing(true);
        //Add more posts to newsfeed as well
        myPostUpdates.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot updates : dataSnapshot.getChildren()){
                    try {
                        StatusUpdateModel statusUpdateModel = updates.getValue(StatusUpdateModel.class);
                        statusUpdateModel.key = updates.getKey();
                        statusUpdates.add(statusUpdateModel);
                    } catch (Exception e){
                        Log.e(TAG, "onDataChange: ", e);
                    }
                }

                try {
                    mSwipeRefreshLayout.setRefreshing(false);
                    if (!statusUpdates.isEmpty()) {

                        //Sort by most recent (based on timeStamp)
                        Collections.reverse(statusUpdates);
                        try {
                            Collections.sort(statusUpdates, (update1, update2) -> (update2.getTimePosted().compareTo(update1.getTimePosted())));
                        } catch (Exception e){
                            Log.e(TAG, "onDataChange: ", e);
                        }

                        icon.setVisibility(View.GONE);
                        emptyTag.setVisibility(View.GONE);
                        recyclerview.setVisibility(View.VISIBLE);
                        recyclerview.setVisibility(View.VISIBLE);
                        NewsFeedAdapter recycler = new NewsFeedAdapter(getContext(), statusUpdates);
                        RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                        recyclerview.setLayoutManager(layoutmanager);

                        recycler.notifyDataSetChanged();
                        recyclerview.setAdapter(recycler);
                    }
                }

                catch (Exception e){
                    Log.e(TAG, "onDataChange: ", e);
                    recyclerview.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //loop through my 'following' node and get users, vendors that i follow
        followingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mSwipeRefreshLayout.setRefreshing(true);
                for(DataSnapshot following : dataSnapshot.getChildren()){

                    //for each 'following' user ... go to their posts node and fetch status updates
                    DatabaseReference newsFeedPosts = FirebaseDatabase.getInstance().getReference("posts/"+following.getKey());
                    newsFeedPosts.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            mSwipeRefreshLayout.setRefreshing(true);
                            for(DataSnapshot updates : dataSnapshot.getChildren()){
                                try {
                                    StatusUpdateModel statusUpdateModel = updates.getValue(StatusUpdateModel.class);
                                    statusUpdateModel.key = updates.getKey();
                                    //Check if (following) user is someone i have blocked
                                    myBlockedUsersRef.child(following.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(!dataSnapshot.exists()){
                                                //Log.d(TAG, "not blocked: "+following.getKey());
                                                statusUpdates.add(statusUpdateModel); //only add posts to newsfeed if i have not blocked the author of the post
                                            } else {
                                                //Log.d(TAG, "blocked: "+following.getKey());
                                            }

                                            try {
                                                mSwipeRefreshLayout.setRefreshing(false);
                                                if (!statusUpdates.isEmpty()) {

                                                    //Sort by most recent (based on timeStamp)
                                                    Collections.reverse(statusUpdates);
                                                    try {
                                                        Collections.sort(statusUpdates, (update1, update2) -> (update2.getTimePosted().compareTo(update1.getTimePosted())));
                                                    } catch (Exception e){
                                                        Log.e(TAG, "onDataChange: ", e);
                                                    }

                                                    icon.setVisibility(View.GONE);
                                                    emptyTag.setVisibility(View.GONE);
                                                    recyclerview.setVisibility(View.VISIBLE);
                                                    recyclerview.setVisibility(View.VISIBLE);
                                                    NewsFeedAdapter recycler = new NewsFeedAdapter(getContext(), statusUpdates);
                                                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                                                    recyclerview.setLayoutManager(layoutmanager);

                                                    recycler.notifyDataSetChanged();
                                                    recyclerview.setAdapter(recycler);
                                                } else {
                                                    emptyTag.setVisibility(View.VISIBLE);
                                                    icon.setVisibility(View.VISIBLE);
                                                    recyclerview.setVisibility(View.GONE);
                                                }
                                            }

                                            catch (Exception e){
                                                Log.e(TAG, "onDataChange: ", e);
                                                recyclerview.setVisibility(View.VISIBLE);
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

//                            try {
//                                mSwipeRefreshLayout.setRefreshing(false);
//                                if (!statusUpdates.isEmpty()) {
//
//                                    //Sort by most recent (based on timeStamp)
//                                    Collections.reverse(statusUpdates);
//                                    try {
//                                        Collections.sort(statusUpdates, (update1, update2) -> (update2.getTimePosted().compareTo(update1.getTimePosted())));
//                                    } catch (Exception e){
//                                        Log.e(TAG, "onDataChange: ", e);
//                                    }
//
//                                    icon.setVisibility(View.GONE);
//                                    emptyTag.setVisibility(View.GONE);
//                                    recyclerview.setVisibility(View.VISIBLE);
//                                    recyclerview.setVisibility(View.VISIBLE);
//                                    NewsFeedAdapter recycler = new NewsFeedAdapter(getContext(), statusUpdates);
//                                    RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
//                                    recyclerview.setLayoutManager(layoutmanager);
//
//                                    recycler.notifyDataSetChanged();
//                                    recyclerview.setAdapter(recycler);
//                                } else {
//                                    emptyTag.setVisibility(View.VISIBLE);
//                                    icon.setVisibility(View.VISIBLE);
//                                    recyclerview.setVisibility(View.GONE);
//                                }
//                            }
//
//                            catch (Exception e){
//                                Log.e(TAG, "onDataChange: ", e);
//                                recyclerview.setVisibility(View.VISIBLE);
//                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "onCancelled: ", databaseError.toException());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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
                myStatusUpdate.setEnabled(true);
                imageUpload.setEnabled(true);
                postBtn.setEnabled(true);

                mSwipeRefreshLayout.setRefreshing(false);
                myStatusUpdate.setText("");
                myStatusUpdate.clearFocus();
                loadNewsFeed();

                /**
                 * A bit buggy ... messes adapter UI a bit ... will revisit
                 *
                 *                 myStatusUpdate.setText("");
                 *                 myStatusUpdate.clearFocus();
                 *                 statusUpdate.key = key;
                 *                 statusUpdates.add(0,statusUpdate);
                 *
                 *                 icon.setVisibility(View.GONE);
                 *                 emptyTag.setVisibility(View.GONE);
                 *                 recyclerview.setVisibility(View.VISIBLE);
                 *                 StatusUpdateAdapter recycler = new StatusUpdateAdapter(getContext(), statusUpdates);
                 *                 RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(getContext());
                 *                 recyclerview.setLayoutManager(layoutmanager);
                 *                 recycler.notifyItemInserted(0);
                 *                 recyclerview.setAdapter(recycler);
                 */
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

    @Override
    public void onRefresh() {
        loadNewsFeed();
    }
}