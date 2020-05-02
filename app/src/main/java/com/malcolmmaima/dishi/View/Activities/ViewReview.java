package com.malcolmmaima.dishi.View.Activities;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.malcolmmaima.dishi.Controller.Utils.CommentKeyBoardFix;
import com.malcolmmaima.dishi.Controller.Utils.GetCurrentDate;
import com.malcolmmaima.dishi.Controller.Utils.TimeAgo;
import com.malcolmmaima.dishi.Model.StatusUpdateModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ReviewReplyAdapter;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import io.fabric.sdk.android.services.common.SafeToast;

import static android.view.View.INVISIBLE;

public class ViewReview extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ViewReview";
    DatabaseReference reviewsRef, authorUserDetailsRef;
    ValueEventListener likesListener, commentsListener, authorUserDetailsRefListener, reviewsRefListener;
    TextView profileName, userUpdate, likesTotal, commentsTotal, emptyTag, timePosted, reviewOptions;
    ImageView profilePic, imageShare, likePost, comments, sharePost;
    String myPhone;
    Button postStatus;
    EmojiconEditText statusPost;
    ImageView selectedImage;
    RecyclerView recyclerView;
    List<StatusUpdateModel> list;
    UserModel authorUser;
    StatusUpdateModel viewPost;
    ImageButton emoji, imageUpload;
    EmojIconActions emojIcon;
    View rootView;
    SwipeRefreshLayout mSwipeRefreshLayout;
    String key, postedTo;

    // instance for firebase storage and StorageReference
    FirebaseStorage storage;
    StorageReference storageReference;

    private ProgressBar progressBar;
    private int progressStatus = 0;

    // Uri indicates, where the image will be picked from
    private Uri filePath;

    // request code
    private final int PICK_IMAGE_REQUEST = 22;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_review);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            SafeToast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
        } else {
            //keep toolbar pinned at top. push edittext on keyboard load
            new CommentKeyBoardFix(this);
            //Hide keyboard on activity load
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number

            rootView = findViewById(R.id.parentlayout);
            profileName = findViewById(R.id.profileName);
            userUpdate = findViewById(R.id.userUpdate);
            profilePic = findViewById(R.id.profilePic);
            likePost = findViewById(R.id.likePost);
            comments = findViewById(R.id.comments);
            sharePost = findViewById(R.id.sharePost);
            likesTotal = findViewById(R.id.likesTotal);
            commentsTotal = findViewById(R.id.commentsTotal);
            postStatus = findViewById(R.id.postStatus);
            statusPost = findViewById(R.id.inputComment);
            imageUpload = findViewById(R.id.camera);
            imageUpload.setVisibility(View.GONE);
            selectedImage = findViewById(R.id.selectedImage);
            progressBar = findViewById(R.id.progressBar);
            recyclerView = findViewById(R.id.rview);
            recyclerView.setNestedScrollingEnabled(false);
            emptyTag = findViewById(R.id.empty_tag);
            timePosted = findViewById(R.id.timePosted);
            emoji = findViewById(R.id.emoji);
            emoji.setVisibility(View.GONE);
            imageShare = findViewById(R.id.imageShare);
            reviewOptions = findViewById(R.id.reviewOptions);

            Toolbar topToolBar = findViewById(R.id.toolbar);
            setSupportActionBar(topToolBar);

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            setTitle("Replies");

            //Back button on toolbar
            topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish(); //Go back to previous activity
                }
            });
            //topToolBar.setLogo(R.drawable.logo);
            //topToolBar.setLogoDescription(getResources().getString(R.string.logo_desc));

            String author = getIntent().getStringExtra("author");
            postedTo = getIntent().getStringExtra("postedTo");
            key = getIntent().getStringExtra("key");

            reviewsRef = FirebaseDatabase.getInstance().getReference("reviews/"+postedTo);
            authorUserDetailsRef = FirebaseDatabase.getInstance().getReference("users/"+author);

            // get the Firebase  storage reference
            storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();

            // SwipeRefreshLayout
            mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
            mSwipeRefreshLayout.setOnRefreshListener(ViewReview.this);
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
                    fetchReviews();
                }
            });

            //Get author's user details
            authorUserDetailsRefListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        authorUser = dataSnapshot.getValue(UserModel.class);
                        profileName.setText(authorUser.getFirstname()+" "+authorUser.getLastname());

                        Picasso.with(ViewReview.this).load(authorUser.getProfilePic()).fit().centerCrop()
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(profilePic);
                    } catch (Exception e){

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            authorUserDetailsRef.addValueEventListener(authorUserDetailsRefListener);

            //Get post details
            try {
                reviewsRefListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(!dataSnapshot.exists()){
                            finish();
                            SafeToast.makeText(ViewReview.this, "Review no longer exists!", Toast.LENGTH_LONG).show();
                        } else {
                            try {
                                viewPost = dataSnapshot.getValue(StatusUpdateModel.class);

                                //Get today's date
                                GetCurrentDate currentDate = new GetCurrentDate();
                                String currDate = currentDate.getDate();

                                //Get date status update was posted
                                String dtEnd = currDate;
                                String dtStart = viewPost.getTimePosted();
                                /**
                                 * date string conversion to Date:
                                 * https://stackoverflow.com/questions/8573250/android-how-can-i-convert-string-to-date
                                 */
                                //Format both current date and date status update was posted
                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss:Z");
                                try {

                                    //Convert String date values to Date values
                                    Date dateStart;
                                    Date dateEnd;

                                    //Date dateStart = format.parse(dtStart);
                                    String[] timeS = Split(viewPost.getTimePosted());
                                    String[] timeT = Split(currDate);

                                    /**
                                     * timeS[0] = date
                                     * timeS[1] = hr
                                     * timeS[2] = min
                                     * timeS[3] = seconds
                                     * timeS[4] = timezone
                                     */

                                    //post timeStamp
                                    if(timeS[4].equals("EAT")){ //Noticed some devices post timezone like so ... i'm going to optimize for EA first
                                        timeS[4] = "GMT+03:00";

                                        //2020-04-27:20:37:32:GMT+03:00
                                        dtStart = timeS[0]+":"+timeS[1]+":"+timeS[2]+":"+timeS[3]+":"+timeS[4];
                                        dateStart = format.parse(dtStart);
                                    } else {
                                        dateStart = format.parse(dtStart);
                                    }

                                    //my device current date
                                    if(timeT[4].equals("EAT")){ //Noticed some devices post timezone like so ... i'm going to optimize for EA first
                                        timeT[4] = "GMT+03:00";

                                        //2020-04-27:20:37:32:GMT+03:00
                                        dtEnd = timeT[0]+":"+timeT[1]+":"+timeT[2]+":"+timeT[3]+":"+timeT[4];
                                        dateEnd = format.parse(dtEnd);
                                    } else {
                                        dateEnd = format.parse(dtEnd);
                                    }

                                    /**
                                     * refer to: https://memorynotfound.com/calculate-relative-time-time-ago-java/
                                     */
                                    //Now compute timeAgo duration
                                    TimeAgo timeAgo = new TimeAgo();

                                    timePosted.setText(timeAgo.toRelative(dateStart, dateEnd, 1));
                                    //Toast.makeText(context, "ago: " + timeAgo.toRelative(dateEnd, dateStart), Toast.LENGTH_LONG).show();
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    Log.d(TAG, "timeStamp: "+e.getMessage());
                                }

                                if(viewPost.getStatus().equals("")){
                                    userUpdate.setVisibility(View.GONE);
                                } else {
                                    userUpdate.setVisibility(View.VISIBLE);
                                    userUpdate.setText(viewPost.getStatus());
                                }

                                if(viewPost.getImageShare() != null){
                                    imageShare.setVisibility(View.VISIBLE);
                                    try {
                                        Picasso.with(ViewReview.this).load(viewPost.getImageShare()).fit().centerCrop()
                                                .placeholder(R.drawable.gray_gradient_background)
                                                .error(R.drawable.gray_gradient_background)
                                                .into(imageShare);
                                    } catch (Exception e){}
                                } else {
                                    imageShare.setVisibility(View.GONE);
                                }
                            } catch (Exception e) {
                            }
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                };
                reviewsRef.child(key).addValueEventListener(reviewsRefListener);

            } catch (Exception e){}

            //creating a popup menu
            PopupMenu popup = new PopupMenu(ViewReview.this, reviewOptions);
            //inflating menu from xml resource
            popup.inflate(R.menu.review_options_menu);

            Menu myMenu = popup.getMenu();
            MenuItem deleteOption = myMenu.findItem(R.id.delete);
            MenuItem reportOption = myMenu.findItem(R.id.report);

            //Can only delete reviews that i have authored or on my posts
            if(postedTo.equals(myPhone) || author.equals(myPhone)){
                try {
                    deleteOption.setVisible(true);
                } catch (Exception e){}
            } else {
                try {
                    deleteOption.setVisible(false);
                } catch (Exception e){}
            }

            //Can only report comments that i havent authored
            if(!myPhone.equals(author)){
                try {
                    reportOption.setVisible(true);
                } catch (Exception e){}
            } else {
                try {
                    reportOption.setVisible(false);
                } catch (Exception e){}
            }

            reviewOptions.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {

                                case R.id.delete:
                                    final AlertDialog deletePost = new AlertDialog.Builder(ViewReview.this)
                                            //set message, title, and icon
                                            .setMessage("Delete Review?")
                                            //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                                            //set three option buttons
                                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    DatabaseReference postDetails = FirebaseDatabase.getInstance().getReference("reviews/"+postedTo+"/"+key);
                                                    postDetails.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            //do nothing since there's a listener that will check to see if review still exists
                                                        }
                                                    });
                                                }
                                            }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    //do nothing

                                                }
                                            })//setNegativeButton

                                            .create();
                                    deletePost.show();
                                    return (true);
                                case R.id.report:
                                    final AlertDialog reportStatus = new AlertDialog.Builder(ViewReview.this)
                                            //set message, title, and icon
                                            .setMessage("Report this review?")
                                            //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                                            //set three option buttons
                                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    Intent slideactivity = new Intent(ViewReview.this, ReportAbuse.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    slideactivity.putExtra("type", "reviewUpdate");
                                                    slideactivity.putExtra("author", author);
                                                    slideactivity.putExtra("postedTo", postedTo);
                                                    slideactivity.putExtra("reviewKey", key);
                                                    startActivity(slideactivity);
                                                }
                                            }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    //do nothing
                                                }
                                            })//setNegativeButton

                                            .create();
                                    reportStatus.show();
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
                    //displaying the popup
                    popup.show();

                }
            });

            imageShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent slideactivity = new Intent(ViewReview.this, ViewImage.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        slideactivity.putExtra("imageURL", viewPost.getImageShare());
                        startActivity(slideactivity);
                    } catch (Exception e){}
                }
            });

            profileName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if(!myPhone.equals(author)){
                        Intent slideactivity = new Intent(ViewReview.this, ViewProfile.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        slideactivity.putExtra("phone", viewPost.getAuthor());
                        Bundle bndlanimation =
                                ActivityOptions.makeCustomAnimation(ViewReview.this, R.anim.animation,R.anim.animation2).toBundle();
                        startActivity(slideactivity, bndlanimation);
                    } else {
                        finish();
                    }
                }
            });

            emoji.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    emojIcon.ShowEmojIcon();
                }
            });

            profilePic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent slideactivity = new Intent(ViewReview.this, ViewImage.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("imageURL", authorUser.getProfilePic());
                    startActivity(slideactivity);
                }
            });

            statusPost.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if(hasFocus){
                        emoji.setVisibility(View.VISIBLE);
                        postStatus.setVisibility(View.VISIBLE);
                        imageUpload.setVisibility(View.VISIBLE);
                    }
                    else {

                        if(statusPost.getText().toString().length() > 1){
                            postStatus.setVisibility(View.VISIBLE);
                            imageUpload.setVisibility(View.VISIBLE);
                            emoji.setVisibility(View.VISIBLE);
                        } else {
                            postStatus.setVisibility(View.GONE);
                            imageUpload.setVisibility(View.GONE);
                            emoji.setVisibility(View.GONE);
                        }

                    }
                }
            });

            emojIcon = new EmojIconActions(ViewReview.this, rootView, statusPost, emoji);
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

            postStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSwipeRefreshLayout.setRefreshing(true);


                    if(statusPost.getText().toString().equals("") && selectedImage.isShown()){
                        uploadImage(); //This will upload image then on successful upload call uploadContent()
                    }

                    else if(statusPost.getText().toString().equals("") && !selectedImage.isShown()){
                        mSwipeRefreshLayout.setRefreshing(false);
                        SafeToast.makeText(ViewReview.this, "Cannot be empty!", Toast.LENGTH_SHORT).show();
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

            //Fetch status update likes
            likesListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        int total = (int) dataSnapshot.getChildrenCount();
                        Double totalLikes = Double.valueOf(total);

                        //below 1000
                        if(totalLikes < 1000){
                            DecimalFormat value = new DecimalFormat("#");
                            likesTotal.setText(""+value.format(totalLikes));
                        }

                        // 1000 to 999,999
                        else if(totalLikes >= 1000 && totalLikes <= 999999){
                            if(totalLikes % 1000 == 0){ //No remainder
                                DecimalFormat value = new DecimalFormat("#####");
                                likesTotal.setText(""+value.format(total/1000)+"K");
                            }

                            else { //Has remainder 999.9K
                                DecimalFormat value = new DecimalFormat("######.#");
                                Double divided = totalLikes/1000;
                                if(value.format(divided).equals("1000")){
                                    likesTotal.setText("1M"); //if rounded off
                                } else {
                                    likesTotal.setText(""+value.format(divided)+"K");
                                }
                            }
                        }

                        // 1,000,0000 to 999,999,999
                        else if(totalLikes >= 1000000 && totalLikes <= 999999999){
                            if(totalLikes % 1000000 == 0) { //No remainder
                                DecimalFormat value = new DecimalFormat("#");
                                likesTotal.setText(""+value.format(totalLikes/1000000)+"M");
                            }

                            else { //Has remainder 9.9M, 999.9M etc
                                DecimalFormat value = new DecimalFormat("#.#");
                                if(value.format(totalLikes/1000000).equals("1000")){
                                    likesTotal.setText("1B"); //if rounded off
                                } else {
                                    likesTotal.setText(""+value.format(totalLikes/1000000)+"M");
                                }
                            }
                        }
                    } catch (Exception e){}
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            reviewsRef.child(key).child("likes").addValueEventListener(likesListener);

            commentsListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        int total = (int) dataSnapshot.getChildrenCount();
                        Double totalComments = Double.valueOf(total);

                        //below 1000
                        if(totalComments < 1000){
                            DecimalFormat value = new DecimalFormat("#");
                            commentsTotal.setText(""+value.format(totalComments));
                        }

                        // 1000 to 999,999
                        else if(totalComments >= 1000 && totalComments <= 999999){
                            if(totalComments % 1000 == 0){ //No remainder
                                DecimalFormat value = new DecimalFormat("#####");
                                commentsTotal.setText(""+value.format(total/1000)+"K");
                            }

                            else { //Has remainder 999.9K
                                DecimalFormat value = new DecimalFormat("######.#");
                                Double divided = totalComments/1000;
                                if(value.format(divided).equals("1000")){
                                    commentsTotal.setText("1M"); //if rounded off
                                } else {
                                    commentsTotal.setText(""+value.format(divided)+"K");
                                }
                            }
                        }

                        // 1,000,0000 to 999,999,999
                        else if(totalComments >= 1000000 && totalComments <= 999999999){
                            if(totalComments % 1000000 == 0) { //No remainder
                                DecimalFormat value = new DecimalFormat("#");
                                commentsTotal.setText(""+value.format(totalComments/1000000)+"M");
                            }

                            else { //Has remainder 9.9M, 999.9M etc
                                DecimalFormat value = new DecimalFormat("#.#");
                                if(value.format(totalComments/1000000).equals("1000")){
                                    commentsTotal.setText("1B"); //if rounded off
                                } else {
                                    commentsTotal.setText(""+value.format(totalComments/1000000)+"M");
                                }
                            }
                        }
                    } catch (Exception e){}
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            reviewsRef.child(key).child("comments").addValueEventListener(commentsListener);

            //On loading adapter fetch the like status
            reviewsRef.child(key).child("likes").child(myPhone).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.exists()){
                        likePost.setTag(R.drawable.unliked);
                        likePost.setImageResource(R.drawable.unliked);
                    } else {
                        likePost.setTag(R.drawable.liked);
                        likePost.setImageResource(R.drawable.liked);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            likePost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int id = (int)likePost.getTag();
                    if( id == R.drawable.unliked){
                        //Add to my favourites
                        reviewsRef.child(key).child("likes").child(myPhone).setValue("like").addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                likePost.setTag(R.drawable.liked);
                                likePost.setImageResource(R.drawable.liked);
                                //Toast.makeText(context,restaurantDetails.getName()+" added to favourites",Toast.LENGTH_SHORT).show();
                            }
                        });


                    } else{
                        //Remove from my favourites
                        reviewsRef.child(key).child("likes").child(myPhone).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                likePost.setTag(R.drawable.unliked);
                                likePost.setImageResource(R.drawable.unliked);
                            }
                        });

                    }
                }
            });
        }

    }

    private void fetchReviews() {
        mSwipeRefreshLayout.setRefreshing(true);
        //Fetch the updates from status_updates node
        commentsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                list = new ArrayList<>();
                for(DataSnapshot updates : dataSnapshot.getChildren()){
                    StatusUpdateModel statusUpdateModel = updates.getValue(StatusUpdateModel.class);
                    statusUpdateModel.key = updates.getKey();
                    statusUpdateModel.setCommentKey(key);
                    list.add(statusUpdateModel);
                }

                try {
                    mSwipeRefreshLayout.setRefreshing(false);
                    if (!list.isEmpty()) {
                        emptyTag.setVisibility(INVISIBLE);
                        //Collections.reverse(list);
                        recyclerView.setVisibility(View.VISIBLE);
                        ReviewReplyAdapter recycler = new ReviewReplyAdapter(ViewReview.this, list);
                        RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(ViewReview.this);
                        recyclerView.setLayoutManager(layoutmanager);

                        recycler.notifyDataSetChanged();

                        recyclerView.setAdapter(recycler);
                    } else {
                        emptyTag.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(INVISIBLE);
                    }
                }

                catch (Exception e){
                    recyclerView.setVisibility(INVISIBLE);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        reviewsRef.child(key).child("comments").addListenerForSingleValueEvent(commentsListener);
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
                        .getBitmap(ViewReview.this.getContentResolver(),
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
                                    + UUID.randomUUID().toString()); //switched from 'phone' to 'myPhone' ...
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
                                        .makeText(ViewReview.this,
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
        GetCurrentDate getCurrentDate = new GetCurrentDate();
        String time = getCurrentDate.getDate();

        final StatusUpdateModel comment = new StatusUpdateModel();
        comment.setStatus(statusPost.getText().toString().trim());
        comment.setTimePosted(time);
        comment.setAuthor(myPhone);
        comment.setPostedTo(postedTo);
        comment.setImageShare(imgLink);

        String commentKey = reviewsRef.push().getKey();
        reviewsRef.child(key).child("comments").child(commentKey).setValue(comment).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mSwipeRefreshLayout.setRefreshing(false);
                comment.key = commentKey;
                statusPost.setText("");
                list.add(comment);

                recyclerView.setVisibility(View.VISIBLE);
                emptyTag.setVisibility(View.GONE);
                ReviewReplyAdapter recycler = new ReviewReplyAdapter(ViewReview.this, list);
                RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(ViewReview.this);
                recyclerView.setLayoutManager(layoutmanager);
                recycler.notifyDataSetChanged();
                recyclerView.setAdapter(recycler);
            }
        });
    }

    public String[] Split(String timeStamp){

        String[] arrSplit = timeStamp.split(":");

        return arrSplit;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            authorUserDetailsRef.removeEventListener(authorUserDetailsRefListener);
            reviewsRef.removeEventListener(likesListener);
            reviewsRef.removeEventListener(commentsListener);
            reviewsRef.child(key).removeEventListener(reviewsRefListener);
        } catch (Exception e){

        }
    }

    @Override
    public void onRefresh() {
        fetchReviews();
    }
}
