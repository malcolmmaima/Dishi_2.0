package com.malcolmmaima.dishiapp.View.Adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

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
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Light;
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishiapp.Controller.Utils.GetCurrentDate;
import com.malcolmmaima.dishiapp.Controller.Utils.TimeAgo;
import com.malcolmmaima.dishiapp.Model.NotificationModel;
import com.malcolmmaima.dishiapp.Model.StatusUpdateModel;
import com.malcolmmaima.dishiapp.Model.UserModel;
import com.malcolmmaima.dishiapp.R;
import com.malcolmmaima.dishiapp.View.Activities.ViewImage;
import com.malcolmmaima.dishiapp.View.Activities.ViewProfile;
import com.malcolmmaima.dishiapp.View.Activities.ViewReview;
import com.malcolmmaima.dishiapp.View.Activities.ViewStatus;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.MyHolder>{
    String TAG = "NotificationAdapter";
    Context context;
    List<NotificationModel> listdata;
    long DURATION = 250;
    UserModel userData;
    String myPhone;
    FirebaseUser user;

    public NotificationAdapter(Context context, List<NotificationModel> listdata) {
        this.listdata = listdata;
        this.context = context;
    }


    @Override
    public NotificationAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_notification,parent,false);

        NotificationAdapter.MyHolder myHolder = new NotificationAdapter.MyHolder(view);
        return myHolder;
    }

    public void onBindViewHolder(final NotificationAdapter.MyHolder holder, final int position) {
        final NotificationModel my_notification = listdata.get(position);

        /**
         * Adapter animation
         */
        setAnimation(holder.itemView, position);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        //Hide notification icon types on load
        holder.liked.setVisibility(View.GONE);
        holder.statusImage.setVisibility(View.GONE);
        holder.reviewIcon.setVisibility(View.GONE);
        holder.commented.setVisibility(View.GONE);
        holder.postedWall.setVisibility(View.GONE);
        holder.followUnfollow.setVisibility(View.GONE);

        holder.contact_name.setText("loading...");
        DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/"+my_notification.getFrom());
        DatabaseReference followRequests = FirebaseDatabase.getInstance().getReference("followRequests/"+my_notification.getFrom());
        DatabaseReference profileFollowers = FirebaseDatabase.getInstance().getReference("followers/"+my_notification.getFrom());
        DatabaseReference followingRef = FirebaseDatabase.getInstance().getReference("following/"+myPhone+"/"+my_notification.getFrom());
        DatabaseReference followingFromRef = FirebaseDatabase.getInstance().getReference("following/"+my_notification.getFrom()+"/"+myPhone);
        userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userData = dataSnapshot.getValue(UserModel.class);
                holder.contact_name.setText(userData.getFirstname()+" "+userData.getLastname());

                /**
                 * Load image url onto imageview
                 */
                try {

                    if(userData.getProfilePicSmall() != null){
                        Picasso.with(context).load(userData.getProfilePicSmall()).fit().centerCrop()
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(holder.profilePic);
                    }

                    else {
                        Picasso.with(context).load(userData.getProfilePic()).fit().centerCrop()
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(holder.profilePic);
                    }
                } catch (Exception e){
                    Log.e(TAG, "onDataChange: ", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //Get today's date
        GetCurrentDate currentDate = new GetCurrentDate();
        String currDate = currentDate.getDate();

        //Get date status update was posted
        String dtEnd = currDate;
        String dtStart = my_notification.getTimeStamp();

        //https://stackoverflow.com/questions/8573250/android-how-can-i-convert-string-to-date
        //Format both current date and date status update was posted
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss:Z");
        try {

            //Convert String date values to Date values
            Date dateStart;
            Date dateEnd;

            //Date dateStart = format.parse(dtStart);
            String[] timeS = Split(my_notification.getTimeStamp());
            String[] timeT = Split(currDate);

            /**
             * timeS[0] = date
             * timeS[1] = hr
             * timeS[2] = min
             * timeS[3] = seconds
             * timeS[4] = timezone
             */

            //post timeStamp
            if(!timeS[4].equals("GMT+03:00")){ //Noticed some devices post timezone like so ... i'm going to optimize for EA first
                timeS[4] = "GMT+03:00";

                //2020-04-27:20:37:32:GMT+03:00
                dtStart = timeS[0]+":"+timeS[1]+":"+timeS[2]+":"+timeS[3]+":"+timeS[4];
                dateStart = format.parse(dtStart);
            } else {
                dateStart = format.parse(dtStart);
            }

            //my device current date
            if(!timeT[4].equals("GMT+03:00")){ //Noticed some devices post timezone like so ... i'm going to optimize for EA first
                timeT[4] = "GMT+03:00";

                //2020-04-27:20:37:32:GMT+03:00
                dtEnd = timeT[0]+":"+timeT[1]+":"+timeT[2]+":"+timeT[3]+":"+timeT[4];
                dateEnd = format.parse(dtEnd);
            } else {
                dateEnd = format.parse(dtEnd);
            }

            //https://memorynotfound.com/calculate-relative-time-time-ago-java/
            //Now compute timeAgo duration
            TimeAgo timeAgo = new TimeAgo();

            holder.notificationTime.setText(timeAgo.toRelative(dateStart, dateEnd, 1));

        } catch (ParseException e) {
            e.printStackTrace();
            //Log.d(TAG, "timeStamp: "+ e.getMessage());
        }

        //Show follow button if type of notification is 'followedwall'
        if(my_notification.getType().equals("followedwall")){
            holder.liked.setVisibility(View.GONE);
            holder.statusImage.setVisibility(View.GONE);
            holder.reviewIcon.setVisibility(View.GONE);
            holder.commented.setVisibility(View.GONE);
            holder.postedWall.setVisibility(View.GONE);
            holder.followUnfollow.setVisibility(View.VISIBLE);
            holder.notificationMessage.setText(my_notification.getMessage());

            /**
             * Below code must always be same as for 'followrequest' below
             * any changes to code below should be made in 'followrequest' and FollowerFollowingAdapter
             * */
            holder.notificationMessage.setText(my_notification.getMessage());
            DatabaseReference followersRef = FirebaseDatabase.getInstance().getReference("followers/"+myPhone+"/"+my_notification.getFrom());
            followersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.exists()){
                        holder.followUnfollow.getBackground().setColorFilter(context.getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
                        holder.followUnfollow.setText("ACCEPT");
                    }
                    else {

                        //Check to see if i had earlier sent a follow request
                        DatabaseReference followRequest = FirebaseDatabase.getInstance().getReference("followRequests/"+my_notification.getFrom()+"/"+myPhone);
                        followRequest.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                //looks like i did
                                if(dataSnapshot.exists()){
                                    holder.followUnfollow.getBackground().setColorFilter(context.getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
                                    holder.followUnfollow.setText("REQUESTED");
                                } else { //I didn't, check follow status
                                    //check to see if i follow this new request
                                    followingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(!dataSnapshot.exists()){
                                                holder.followUnfollow.getBackground().setColorFilter(context.getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
                                                holder.followUnfollow.setText("FOLLOW");
                                            }
                                            else {
                                                holder.followUnfollow.getBackground().setColorFilter(context.getResources().getColor(R.color.colorPrimaryDark), PorterDuff.Mode.MULTIPLY);
                                                holder.followUnfollow.setText("UNFOLLOW");
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
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            /**
             * Ends here
             * */
        }

        //Show accept button if type of notification is 'followrequest'
        if(my_notification.getType().equals("followrequest")){
            holder.liked.setVisibility(View.GONE);
            holder.statusImage.setVisibility(View.GONE);
            holder.reviewIcon.setVisibility(View.GONE);
            holder.commented.setVisibility(View.GONE);
            holder.postedWall.setVisibility(View.GONE);
            holder.followUnfollow.setVisibility(View.VISIBLE);

            /**
             * Below code must always be same as for 'followedwall' above
             * any changes to code below should be made in 'followedwall' and FollowerFollowingAdapter
             * */
            holder.notificationMessage.setText(my_notification.getMessage());
            DatabaseReference followersRef = FirebaseDatabase.getInstance().getReference("followers/"+myPhone+"/"+my_notification.getFrom());
            followersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.exists()){
                        holder.followUnfollow.getBackground().setColorFilter(context.getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
                        holder.followUnfollow.setText("ACCEPT");
                    }
                    else {

                        //Check to see if i had earlier sent a follow request
                        DatabaseReference followRequest = FirebaseDatabase.getInstance().getReference("followRequests/"+my_notification.getFrom()+"/"+myPhone);
                        followRequest.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                //looks like i did
                                if(dataSnapshot.exists()){
                                    holder.followUnfollow.getBackground().setColorFilter(context.getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
                                    holder.followUnfollow.setText("REQUESTED");
                                } else { //I didn't, check follow status
                                    //check to see if i follow this new request
                                    followingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(!dataSnapshot.exists()){
                                                holder.followUnfollow.getBackground().setColorFilter(context.getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
                                                holder.followUnfollow.setText("FOLLOW");
                                            }
                                            else {
                                                holder.followUnfollow.getBackground().setColorFilter(context.getResources().getColor(R.color.colorPrimaryDark), PorterDuff.Mode.MULTIPLY);
                                                holder.followUnfollow.setText("UNFOLLOW");
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
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            /**
             * Ends here
             * */
        }

        if(my_notification.getType().equals("postedwall")){
            holder.followUnfollow.setVisibility(View.GONE);
            holder.liked.setVisibility(View.GONE);
            holder.statusImage.setVisibility(View.GONE);
            holder.reviewIcon.setVisibility(View.GONE);
            holder.commented.setVisibility(View.GONE);

            holder.postedWall.setVisibility(View.VISIBLE);
            holder.notificationMessage.setText("posted on your wall");
        }

        if(my_notification.getType().equals("likedstatus")){
            holder.followUnfollow.setVisibility(View.GONE);
            holder.commented.setVisibility(View.GONE);
            holder.postedWall.setVisibility(View.GONE);
            holder.statusImage.setVisibility(View.GONE);

            holder.liked.setVisibility(View.VISIBLE);
            DatabaseReference postDetailsRef = FirebaseDatabase.getInstance().getReference("posts/"+my_notification.getPostedTo()+"/"+my_notification.getMessage());
            postDetailsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.exists()){
                        holder.notificationMessage.setText("[Deleted post]");
                    } else {
                        try {
                            StatusUpdateModel statusUpdate = dataSnapshot.getValue(StatusUpdateModel.class);
                            if(statusUpdate.getStatus().length() > 50) {
                                holder.notificationMessage.setText("liked: " +statusUpdate.getStatus().substring(0, 50) + "...");
                            } else {
                                holder.notificationMessage.setText("liked: " + statusUpdate.getStatus());
                            }

                            if(statusUpdate.getImageShare() != null && !statusUpdate.getImageShare().equals("")){
                                holder.commented.setVisibility(View.GONE);
                                holder.statusImage.setVisibility(View.VISIBLE);

                                Picasso.with(context).load(statusUpdate.getImageShare()).fit().centerCrop()
                                        .placeholder(R.drawable.gray_gradient_background)
                                        .error(R.drawable.gray_gradient_background)
                                        .into(holder.statusImage);
                            }

                        } catch (Exception e){
                            Log.e(TAG, "onDataChange: ", e);
                            holder.liked.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        if(my_notification.getType().equals("commentedstatus")){
            holder.followUnfollow.setVisibility(View.GONE);
            holder.liked.setVisibility(View.GONE);
            holder.postedWall.setVisibility(View.GONE);
            holder.statusImage.setVisibility(View.GONE);
            holder.reviewIcon.setVisibility(View.GONE);

            holder.commented.setVisibility(View.VISIBLE);

            DatabaseReference postDetailsRef = FirebaseDatabase.getInstance().getReference("posts/"+my_notification.getPostedTo()+"/"+my_notification.getStatusKey());
            postDetailsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        StatusUpdateModel statusUpdate = dataSnapshot.getValue(StatusUpdateModel.class); //the actual status
                        DatabaseReference userDataRef = FirebaseDatabase.getInstance().getReference("users/"+my_notification.getFrom());
                        userDataRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                UserModel userModel = dataSnapshot.getValue(UserModel.class);

                                postDetailsRef.child("comments").child(my_notification.getMessage()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        if(!dataSnapshot.exists()){
                                            holder.notificationMessage.setText("[Deleted post]");
                                        } else {
                                            try {
                                                StatusUpdateModel comment = dataSnapshot.getValue(StatusUpdateModel.class); //the comment reply to above mentioned status

                                                if(comment.getStatus().length() > 50) {
                                                    holder.notificationMessage.setText("commented: " +comment.getStatus().substring(0, 50) + "...");
                                                } else {
                                                    holder.notificationMessage.setText("commented: " + comment.getStatus());
                                                }

                                                if(statusUpdate.getImageShare() != null && !statusUpdate.getImageShare().equals("")){
                                                    holder.commented.setVisibility(View.GONE);
                                                    holder.statusImage.setVisibility(View.VISIBLE);

                                                    Picasso.with(context).load(statusUpdate.getImageShare()).fit().centerCrop()
                                                            .placeholder(R.drawable.gray_gradient_background)
                                                            .error(R.drawable.gray_gradient_background)
                                                            .into(holder.statusImage);
                                                }
                                            } catch (Exception e){
                                                Log.e(TAG, "onDataChange: ", e);
                                            }
                                        }

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
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
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        //This notification is specific to vendor accounts
        if(my_notification.getType().equals("postedreview")){
            holder.followUnfollow.setVisibility(View.GONE);
            holder.liked.setVisibility(View.GONE);
            holder.postedWall.setVisibility(View.GONE);
            holder.statusImage.setVisibility(View.GONE);
            holder.commented.setVisibility(View.GONE);

            holder.reviewIcon.setVisibility(View.VISIBLE);

            DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews/"+myPhone+"/"+my_notification.getMessage());
            reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.exists()){
                        holder.notificationMessage.setText("[Deleted review]");
                    } else {
                        try {
                            StatusUpdateModel review = dataSnapshot.getValue(StatusUpdateModel.class); //the comment reply to above mentioned status

                            if(review.getStatus().length() > 50) {
                                holder.notificationMessage.setText("posted review: " +review.getStatus().substring(0, 50) + "...");
                            } else {
                                holder.notificationMessage.setText("posted review: " + review.getStatus());
                            }

                            if(review.getImageShare() != null && !review.getImageShare().equals("")){
                                holder.reviewIcon.setVisibility(View.GONE);
                                holder.statusImage.setVisibility(View.VISIBLE);

                                Picasso.with(context).load(review.getImageShare()).fit().centerCrop()
                                        .placeholder(R.drawable.gray_gradient_background)
                                        .error(R.drawable.gray_gradient_background)
                                        .into(holder.statusImage);
                            }
                        } catch (Exception e){
                            Log.e(TAG, "onDataChange: ", e);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }


        if(my_notification.getType().equals("commentedreview")){
            holder.followUnfollow.setVisibility(View.GONE);
            holder.liked.setVisibility(View.GONE);
            holder.postedWall.setVisibility(View.GONE);
            holder.statusImage.setVisibility(View.GONE);
            holder.commented.setVisibility(View.GONE);

            holder.reviewIcon.setVisibility(View.VISIBLE);

            DatabaseReference reviewsRef = FirebaseDatabase.getInstance().getReference("reviews/"+myPhone+"/"+my_notification.getMessage());
            reviewsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.exists()){
                        holder.notificationMessage.setText("[Deleted review]");
                    } else {
                        try {
                            StatusUpdateModel review = dataSnapshot.getValue(StatusUpdateModel.class); //the comment reply to above mentioned status

                            if(review.getStatus().length() > 50) {
                                holder.notificationMessage.setText("replied review: " +review.getStatus().substring(0, 50) + "...");
                            } else {
                                holder.notificationMessage.setText("replied review: " + review.getStatus());
                            }

                            if(review.getImageShare() != null && !review.getImageShare().equals("")){
                                holder.reviewIcon.setVisibility(View.GONE);
                                holder.statusImage.setVisibility(View.VISIBLE);

                                Picasso.with(context).load(review.getImageShare()).fit().centerCrop()
                                        .placeholder(R.drawable.gray_gradient_background)
                                        .error(R.drawable.gray_gradient_background)
                                        .into(holder.statusImage);
                            }
                        } catch (Exception e){
                            Log.e(TAG, "onDataChange: ", e);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }


        /**
         * Click listener on our card
         */
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if(my_notification.getType().equals("followedwall") && !myPhone.equals(my_notification.getFrom())){
                    Intent slideactivity = new Intent(context, ViewProfile.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    slideactivity.putExtra("phone", my_notification.getFrom());
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                    context.startActivity(slideactivity, bndlanimation);
                }

                if(my_notification.getType().equals("followrequest") && !myPhone.equals(my_notification.getFrom())){
                    Intent slideactivity = new Intent(context, ViewProfile.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    slideactivity.putExtra("phone", my_notification.getFrom());
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                    context.startActivity(slideactivity, bndlanimation);
                }

                if(my_notification.getType().equals("postedwall")){
                    Intent slideactivity = new Intent(context, ViewStatus.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("author", my_notification.getFrom());
                    slideactivity.putExtra("postedTo", myPhone);
                    slideactivity.putExtra("key", my_notification.getMessage());
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                    context.startActivity(slideactivity, bndlanimation);
                }

                if(my_notification.getType().equals("likedstatus")){
                    try {
                        Intent slideactivity = new Intent(context, ViewStatus.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        slideactivity.putExtra("author", my_notification.getAuthor());
                        slideactivity.putExtra("postedTo", my_notification.getPostedTo());
                        slideactivity.putExtra("key", my_notification.getMessage());
                        Bundle bndlanimation = ActivityOptions.makeCustomAnimation(context, R.anim.animation, R.anim.animation2).toBundle();
                        context.startActivity(slideactivity, bndlanimation);
                    } catch (Exception e){
                        Log.e(TAG, "onClick: ", e);
                    }
                }

                if(my_notification.getType().equals("commentedstatus")){
                    try {
                        Intent slideactivity = new Intent(context, ViewStatus.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        slideactivity.putExtra("author", my_notification.getAuthor());
                        slideactivity.putExtra("postedTo", my_notification.getPostedTo());
                        slideactivity.putExtra("key", my_notification.getStatusKey());
                        Bundle bndlanimation = ActivityOptions.makeCustomAnimation(context, R.anim.animation, R.anim.animation2).toBundle();
                        context.startActivity(slideactivity, bndlanimation);
                    } catch (Exception e){
                        Log.e(TAG, "onClick: ", e);
                    }
                }

                if(my_notification.getType().equals("postedreview")){
                    Intent slideactivity = new Intent(context, ViewReview.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("author", my_notification.getFrom());
                    slideactivity.putExtra("postedTo", myPhone);
                    slideactivity.putExtra("key", my_notification.getMessage());
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                    context.startActivity(slideactivity, bndlanimation);
                }

                if(my_notification.getType().equals("commentedreview")){
                    Intent slideactivity = new Intent(context, ViewReview.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("author", my_notification.getFrom());
                    slideactivity.putExtra("postedTo", myPhone);
                    slideactivity.putExtra("key", my_notification.getMessage());
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                    context.startActivity(slideactivity, bndlanimation);
                }

            }
        });

        holder.followUnfollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(holder.followUnfollow.getText().equals("ACCEPT")){
                    DatabaseReference myFollowersRef = FirebaseDatabase.getInstance().getReference("followers/"+myPhone);
                    myFollowersRef.child(my_notification.getFrom()).setValue("follow").addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            followingFromRef.setValue("follow"); //update recipients following node as well since i just accepted their request
                            DatabaseReference followRequests = FirebaseDatabase.getInstance().getReference("followRequests/"+myPhone);
                            followRequests.child(my_notification.getFrom()).removeValue();
                            //check to see if i follow this new request
                            followingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if(!dataSnapshot.exists()){
                                        holder.followUnfollow.getBackground().setColorFilter(context.getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
                                        holder.followUnfollow.setText("FOLLOW");
                                    }
                                    else {
                                        holder.followUnfollow.getBackground().setColorFilter(context.getResources().getColor(R.color.colorPrimaryDark), PorterDuff.Mode.MULTIPLY);
                                        holder.followUnfollow.setText("UNFOLLOW");
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                            Toast.makeText(context, "Accepted", Toast.LENGTH_LONG).show();

                            sendNotification("accepted follow request", "followedwall");
                        }

                        private void sendNotification(String message, String type) {
                            DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notifications/"+my_notification.getFrom());

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
                    });

                }

                else if(holder.followUnfollow.getText().equals("REQUESTED")){
                    //Do nothing
                }
                else {

                    DatabaseReference followerRef = FirebaseDatabase.getInstance().getReference("followers/"+my_notification.getFrom()+"/"+myPhone);
                    followingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(!dataSnapshot.exists()){
                                //Check to see if profile is private
                                userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        userData = dataSnapshot.getValue(UserModel.class);

                                        if(userData.getAccountPrivacy().equals("private")){
                                            //send follow request

                                            followRequests.child(myPhone).setValue("followrequest").addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    try {
                                                        holder.followUnfollow.setText("REQUESTED");
                                                        Snackbar.make(v.getRootView(), "Request sent", Snackbar.LENGTH_LONG).show();
                                                    } catch (Exception er){
                                                        Log.e(TAG, "onFailure: ", er);
                                                    }

                                                    sendNotification("wants to follow you", "followrequest");
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    try {
                                                        holder.followUnfollow.setText("REQUESTED");
                                                        Snackbar.make(v.getRootView(), "Something went wrong", Snackbar.LENGTH_LONG).show();
                                                    } catch (Exception er){
                                                        Log.e(TAG, "onFailure: ", er);
                                                    }
                                                }
                                            });

                                        }

                                        if(userData.getAccountPrivacy().equals("public")){
                                            //automatically follow
                                            profileFollowers.child(myPhone).setValue("follow").addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    followingRef.setValue("follow");
                                                    holder.followUnfollow.getBackground().setColorFilter(context.getResources().getColor(R.color.colorPrimaryDark), PorterDuff.Mode.MULTIPLY);
                                                    holder.followUnfollow.setText("UNFOLLOW");
                                                    sendNotification("followed you", "followedwall");
                                                }
                                            });
                                        }
                                    }

                                    private void sendNotification(String message, String type) {
                                        DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notifications/"+my_notification.getFrom());

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
                            else {
                                followingRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        followerRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                holder.followUnfollow.getBackground().setColorFilter(context.getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
                                                holder.followUnfollow.setText("FOLLOW");
                                            }
                                        });

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        holder.followUnfollow.getBackground().setColorFilter(context.getResources().getColor(R.color.colorPrimaryDark), PorterDuff.Mode.MULTIPLY);
                                        holder.followUnfollow.setText("UNFOLLOW");
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }

            }
        });


        /**
         * View image click listener
         */
        holder.profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference profileP = FirebaseDatabase.getInstance().getReference("users/"+my_notification.getFrom());
                profileP.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        try {
                            UserModel thisUser = dataSnapshot.getValue(UserModel.class);

                            if(thisUser.getProfilePicBig() != null){
                                Intent slideactivity = new Intent(context, ViewImage.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                slideactivity.putExtra("imageURL", thisUser.getProfilePicBig());
                                context.startActivity(slideactivity);
                            }

                            else {
                                Intent slideactivity = new Intent(context, ViewImage.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                slideactivity.putExtra("imageURL", thisUser.getProfilePic());
                                context.startActivity(slideactivity);
                            }
                        } catch (Exception e){
                            Log.e(TAG, "onClick: ", e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });

        holder.contact_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!myPhone.equals(my_notification.getFrom())){
                    Intent slideactivity = new Intent(context, ViewProfile.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    slideactivity.putExtra("phone", my_notification.getFrom());
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                    context.startActivity(slideactivity, bndlanimation);
                }
            }
        });

    }

    /**
     * @https://medium.com/better-programming/android-recyclerview-with-beautiful-animations-5e9b34dbb0fa
     */
    private void setAnimation(View itemView, int i) {
        boolean on_attach = true;
        if(!on_attach){
            i = -1;
        }
        boolean isNotFirstItem = i == -1;
        i++;
        itemView.setAlpha(0.f);
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator animator = ObjectAnimator.ofFloat(itemView, "alpha", 0.f, 0.5f, 1.0f);
        ObjectAnimator.ofFloat(itemView, "alpha", 0.f).start();
        animator.setStartDelay(isNotFirstItem ? DURATION / 2 : (i * DURATION / 3));
        animator.setDuration(500);
        animatorSet.play(animator);
        animator.start();
    }

    public int getItemCount() {
        return listdata.size();
    }

    class MyHolder extends RecyclerView.ViewHolder{
        MyTextView_Roboto_Medium contact_name;
        MyTextView_Roboto_Regular notificationMessage;
        MyTextView_Roboto_Light notificationTime;
        ImageView profilePic, liked, commented, postedWall, statusImage, reviewIcon;
        LinearLayout cardView;
        AppCompatButton followUnfollow;

        public MyHolder(View itemView) {
            super(itemView);
            contact_name = itemView.findViewById(R.id.contact_name);
            profilePic = itemView.findViewById(R.id.user_dp);
            cardView = itemView.findViewById(R.id.card_view);
            notificationMessage = itemView.findViewById(R.id.message);
            followUnfollow = itemView.findViewById(R.id.followUnfollow);
            notificationTime = itemView.findViewById(R.id.notificationTime);
            liked = itemView.findViewById(R.id.liked);
            commented = itemView.findViewById(R.id.commented);
            postedWall = itemView.findViewById(R.id.postedWall);
            statusImage = itemView.findViewById(R.id.statusImage);
            reviewIcon = itemView.findViewById(R.id.reviewIcon);

            //Long Press
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    //Do something
                    return false;
                }
            });

        }
    }

    public String[] Split(String timeStamp){

        String[] arrSplit = timeStamp.split(":");

        return arrSplit;
    }

}
