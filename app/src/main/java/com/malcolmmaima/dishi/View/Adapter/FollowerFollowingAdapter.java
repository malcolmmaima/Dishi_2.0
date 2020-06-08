package com.malcolmmaima.dishi.View.Adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
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
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishi.Controller.Utils.GetCurrentDate;
import com.malcolmmaima.dishi.Model.NotificationModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.Chat;
import com.malcolmmaima.dishi.View.Activities.ViewCustomerOrder;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.malcolmmaima.dishi.View.Activities.ViewProfile;
import com.squareup.picasso.Picasso;

import java.util.List;

public class FollowerFollowingAdapter extends RecyclerView.Adapter<FollowerFollowingAdapter.MyHolder>{
    String TAG = "FollowerFollowingAdapter";
    Context context;
    List<UserModel> listdata;
    long DURATION = 200;
    String myPhone;
    FirebaseUser user;

    public FollowerFollowingAdapter(Context context, List<UserModel> listdata) {
        this.listdata = listdata;
        this.context = context;
    }


    @Override
    public FollowerFollowingAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_follower_following,parent,false);

        FollowerFollowingAdapter.MyHolder myHolder = new FollowerFollowingAdapter.MyHolder(view);
        return myHolder;
    }

    public void onBindViewHolder(final FollowerFollowingAdapter.MyHolder holder, final int position) {
        final UserModel userModel = listdata.get(position);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        DatabaseReference followingRef = FirebaseDatabase.getInstance().getReference("following/"+myPhone+"/"+userModel.getPhone());
        DatabaseReference followingFromRef = FirebaseDatabase.getInstance().getReference("following/"+userModel.getPhone()+"/"+myPhone);
        DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/"+userModel.getPhone());
        DatabaseReference followRequests = FirebaseDatabase.getInstance().getReference("followRequests/"+userModel.getPhone());
        DatabaseReference profileFollowers = FirebaseDatabase.getInstance().getReference("followers/"+userModel.getPhone());

        holder.followUnfollow.setEnabled(false);
        /**
         * Adapter animation
         */
        //setAnimation(holder.itemView, position);

        /**
         * Set widget values
         **/

        holder.customerName.setText(userModel.getFirstname() + " " + userModel.getLastname());

        if(myPhone.equals(userModel.getPhone())){
            holder.followUnfollow.setVisibility(View.GONE);
        }

        DatabaseReference followersRef = FirebaseDatabase.getInstance().getReference("followers/"+myPhone+"/"+userModel.getPhone());
        followersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                holder.followUnfollow.setEnabled(true);
                if(!dataSnapshot.exists()){
                    //Check if this individual had already sent a follwo request
                    DatabaseReference incomingfollowRequest = FirebaseDatabase.getInstance().getReference("followRequests/"+myPhone+"/"+userModel.getPhone());
                    incomingfollowRequest.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){
                                holder.followUnfollow.getBackground().setColorFilter(context.getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
                                holder.followUnfollow.setText("ACCEPT");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                }
                else {

                    //Check to see if i had earlier sent a follow request
                    DatabaseReference followRequest = FirebaseDatabase.getInstance().getReference("followRequests/"+userModel.getPhone()+"/"+myPhone);
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

        holder.followUnfollow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(holder.followUnfollow.getText().equals("ACCEPT")){
                    DatabaseReference myFollowersRef = FirebaseDatabase.getInstance().getReference("followers/"+myPhone);
                    myFollowersRef.child(userModel.getPhone()).setValue("follow").addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            followingFromRef.setValue("follow"); //update recipients following node as well since i just accepted their request
                            DatabaseReference followRequests = FirebaseDatabase.getInstance().getReference("followRequests/"+myPhone);
                            followRequests.child(userModel.getPhone()).removeValue();
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
                            DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notifications/"+userModel.getPhone());

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

                    DatabaseReference followerRef = FirebaseDatabase.getInstance().getReference("followers/"+userModel.getPhone()+"/"+myPhone);
                    followingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(!dataSnapshot.exists()){
                                //Check to see if profile is private
                                userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        try {
                                            if (userModel.getAccountPrivacy().equals("private")) {
                                                //send follow request

                                                followRequests.child(myPhone).setValue("followrequest").addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        try {
                                                            holder.followUnfollow.setText("REQUESTED");
                                                            Snackbar.make(v.getRootView(), "Request sent", Snackbar.LENGTH_LONG).show();
                                                        } catch (Exception er) {
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
                                                        } catch (Exception er) {
                                                            Log.e(TAG, "onFailure: ", er);
                                                        }
                                                    }
                                                });

                                            }

                                            if (userModel.getAccountPrivacy().equals("public")) {
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
                                        } catch (Exception e){
                                            Log.e(TAG, "onDataChange: ", e);
                                        }
                                    }

                                    private void sendNotification(String message, String type) {
                                        DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notifications/"+userModel.getPhone());

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
         * Click listener on our card
         */
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if(!myPhone.equals(userModel.getPhone())){
                    Intent slideactivity = new Intent(context, ViewProfile.class)
                            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                    slideactivity.putExtra("phone", userModel.getPhone());
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                    context.startActivity(slideactivity, bndlanimation);
                }
            }
        });


        /**
         * View image click listener
         */
        holder.profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(userModel.getProfilePicBig() != null){
                    Intent slideactivity = new Intent(context, ViewImage.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("imageURL", userModel.getProfilePicBig());
                    context.startActivity(slideactivity);
                }

                else {
                    Intent slideactivity = new Intent(context, ViewImage.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("imageURL", userModel.getProfilePic());
                    context.startActivity(slideactivity);
                }
            }
        });


        /**
         * Load image url onto imageview
         */
        try {
            //Load image
            if(userModel.getProfilePicSmall() != null){
                Picasso.with(context).load(userModel.getProfilePicSmall()).fit().centerCrop()
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(holder.profilePic);
            }

            else {
                Picasso.with(context).load(userModel.getProfilePic()).fit().centerCrop()
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(holder.profilePic);
            }
        } catch (Exception e){

        }

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
        MyTextView_Roboto_Medium customerName;
        ImageView profilePic;
        LinearLayout cardView;
        AppCompatButton followUnfollow;

        public MyHolder(View itemView) {
            super(itemView);
            customerName = itemView.findViewById(R.id.username);
            profilePic = itemView.findViewById(R.id.profilePic);
            cardView = itemView.findViewById(R.id.card_view);
            followUnfollow = itemView.findViewById(R.id.followUnfollow);

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

}
