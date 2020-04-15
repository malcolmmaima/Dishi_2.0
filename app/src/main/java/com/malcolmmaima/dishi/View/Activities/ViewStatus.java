package com.malcolmmaima.dishi.View.Activities;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.GetCurrentDate;
import com.malcolmmaima.dishi.Model.StatusUpdateModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.CommentAdapter;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


import io.fabric.sdk.android.services.common.SafeToast;

import static android.view.View.INVISIBLE;

public class ViewStatus extends AppCompatActivity {

    DatabaseReference postRef, authorUserDetailsRef;
    ValueEventListener likesListener, commentsListener, authorUserDetailsRefListener;
    TextView profileName, userUpdate, likesTotal, commentsTotal, emptyTag, timePosted;
    ImageView profilePic, deleteBtn, likePost, comments, sharePost;
    String myPhone;
    Button postStatus;
    EditText statusPost;
    RecyclerView recyclerView;
    List<StatusUpdateModel> list;
    String phone;
    UserModel authorUser;
    StatusUpdateModel viewPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_status);

        //Hide keyboard on activity load
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        profileName = findViewById(R.id.profileName);
        userUpdate = findViewById(R.id.userUpdate);
        profilePic = findViewById(R.id.profilePic);
        deleteBtn = findViewById(R.id.deleteBtn);
        likePost = findViewById(R.id.likePost);
        comments = findViewById(R.id.comments);
        sharePost = findViewById(R.id.sharePost);
        likesTotal = findViewById(R.id.likesTotal);
        commentsTotal = findViewById(R.id.commentsTotal);
        postStatus = findViewById(R.id.postStatus);
        statusPost = findViewById(R.id.inputComment);
        recyclerView = findViewById(R.id.rview);
        emptyTag = findViewById(R.id.empty_tag);
        timePosted = findViewById(R.id.timePosted);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Comments");

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });
        //topToolBar.setLogo(R.drawable.logo);
        //topToolBar.setLogoDescription(getResources().getString(R.string.logo_desc));

        final String author = getIntent().getStringExtra("author");
        final String postedTo = getIntent().getStringExtra("postedTo");
        final String key = getIntent().getStringExtra("key");

        postRef = FirebaseDatabase.getInstance().getReference("posts/"+postedTo);
        authorUserDetailsRef = FirebaseDatabase.getInstance().getReference("users/"+author);

        //Get author's user details
        authorUserDetailsRefListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    authorUser = dataSnapshot.getValue(UserModel.class);
                    profileName.setText(authorUser.getFirstname()+" "+authorUser.getLastname());

                    Picasso.with(ViewStatus.this).load(authorUser.getProfilePic()).fit().centerCrop()
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
        postRef.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    viewPost = dataSnapshot.getValue(StatusUpdateModel.class);
                    userUpdate.setText(viewPost.getStatus());
                } catch (Exception e){}
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //Post timestamp
        timePosted.setText("loading...");

        profileName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!myPhone.equals(author)){
                    Intent slideactivity = new Intent(ViewStatus.this, ViewProfile.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    slideactivity.putExtra("phone", myPhone);
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(ViewStatus.this, R.anim.animation,R.anim.animation2).toBundle();
                    startActivity(slideactivity, bndlanimation);
                } else {
                    finish();
                }
            }
        });

        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent slideactivity = new Intent(ViewStatus.this, ViewImage.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                slideactivity.putExtra("imageURL", authorUser.getProfilePic());
                startActivity(slideactivity);
            }
        });

        postStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                GetCurrentDate getCurrentDate = new GetCurrentDate();
                String time = getCurrentDate.getDate();

                final StatusUpdateModel comment = new StatusUpdateModel();
                comment.setStatus(statusPost.getText().toString());
                comment.setTimePosted(time);
                comment.setAuthor(myPhone);
                comment.setPostedTo(postedTo);

                String commentKey = postRef.push().getKey();

                if(statusPost.getText().toString().equals("")){
                    SafeToast.makeText(ViewStatus.this, "You must enter something!", Toast.LENGTH_SHORT).show();
                }

                else {
                    postRef.child(key).child("comments").child(commentKey).setValue(comment).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(ViewStatus.this, "Comment posted!", Toast.LENGTH_SHORT).show();
                            comment.key = commentKey;
                            statusPost.setText("");
                        }
                    });

                }

            }

        });

        //Fetch status update likes
        likesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    int totalLikes = (int) dataSnapshot.getChildrenCount();
                    likesTotal.setText("" + totalLikes);
                } catch (Exception e){}
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        postRef.child(key).child("likes").addValueEventListener(likesListener);

        //On loading adapter fetch the like status
        postRef.child(key).child("likes").child(myPhone).addListenerForSingleValueEvent(new ValueEventListener() {
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
                    postRef.child(key).child("likes").child(myPhone).setValue("like").addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            likePost.setTag(R.drawable.liked);
                            likePost.setImageResource(R.drawable.liked);
                            //Toast.makeText(context,restaurantDetails.getName()+" added to favourites",Toast.LENGTH_SHORT).show();
                        }
                    });


                } else{
                    //Remove from my favourites
                    postRef.child(key).child("likes").child(myPhone).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            likePost.setTag(R.drawable.unliked);
                            likePost.setImageResource(R.drawable.unliked);
                        }
                    });

                }
            }
        });

        //Fetch the updates from status_updates node
        commentsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                int totalComments = (int) dataSnapshot.getChildrenCount();
                commentsTotal.setText("" + totalComments);

                list = new ArrayList<>();
                for(DataSnapshot updates : dataSnapshot.getChildren()){
                    StatusUpdateModel statusUpdateModel = updates.getValue(StatusUpdateModel.class);
                    statusUpdateModel.key = updates.getKey();
                    statusUpdateModel.setCommentKey(key);
                    list.add(statusUpdateModel);
                }

                try {
                    if (!list.isEmpty()) {
                        emptyTag.setVisibility(INVISIBLE);
                        //Collections.reverse(list);
                        recyclerView.setVisibility(View.VISIBLE);
                        CommentAdapter recycler = new CommentAdapter(ViewStatus.this, list);
                        RecyclerView.LayoutManager layoutmanager = new LinearLayoutManager(ViewStatus.this);
                        recyclerView.setLayoutManager(layoutmanager);

                        recycler.notifyDataSetChanged();

                        recyclerView.getItemAnimator().setAddDuration(1000);
                        recyclerView.getItemAnimator().setRemoveDuration(1000);
                        recyclerView.getItemAnimator().setMoveDuration(1000);
                        recyclerView.getItemAnimator().setChangeDuration(1000);

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
        postRef.child(key).child("comments").addValueEventListener(commentsListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        authorUserDetailsRef.removeEventListener(authorUserDetailsRefListener);
        postRef.removeEventListener(likesListener);
        postRef.removeEventListener(commentsListener);
    }
}
