package com.malcolmmaima.dishi.View.Adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Light;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Controller.Utils.GetCurrentDate;
import com.malcolmmaima.dishi.Controller.Utils.TimeAgo;
import com.malcolmmaima.dishi.Model.NotificationModel;
import com.malcolmmaima.dishi.Model.StatusUpdateModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.ReportAbuse;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.malcolmmaima.dishi.View.Activities.ViewProfile;
import com.malcolmmaima.dishi.View.Activities.ViewStatus;
import com.squareup.picasso.Picasso;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


import io.fabric.sdk.android.services.common.SafeToast;


public class StatusUpdateAdapter extends RecyclerView.Adapter<StatusUpdateAdapter.MyHolder> {

    Context context;
    List<StatusUpdateModel> listdata;
    long DURATION = 200;
    String TAG = "StatusAdapter";

    public StatusUpdateAdapter(Context context, List<StatusUpdateModel> listdata) {
        this.listdata = listdata;
        this.context = context;
    }

    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_status_update,parent,false);

        MyHolder myHolder = new MyHolder(view);
        return myHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final StatusUpdateAdapter.MyHolder holder, final int position) {

        StatusUpdateModel statusUpdateModel = listdata.get(position);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String myPhone = user.getPhoneNumber(); //Current logged in user phone number
        UserModel [] postUser = new UserModel[listdata.size()];

        /**
         * Adapter animation
         */
        setAnimation(holder.itemView, position);

        //Setup db references
        DatabaseReference postUserDetails = FirebaseDatabase.getInstance().getReference("users/"+statusUpdateModel.getAuthor());
        DatabaseReference postDetails = FirebaseDatabase.getInstance().getReference("posts/"+statusUpdateModel.getPostedTo()+"/"+statusUpdateModel.key);

        postDetails.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    try {
                        listdata.remove(position);
                        notifyItemRemoved(position);
                    } catch (Exception e){
                        Log.d(TAG, "statusUpdate: error " + e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //fetch post User Details
        postUserDetails.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    holder.likePost.setEnabled(false);
                    holder.profilePic.setEnabled(false);
                } else {
                    try {
                        postUser[position] = dataSnapshot.getValue(UserModel.class);

                        //Set profile pic
                        Picasso.with(context).load(postUser[position].getProfilePic()).fit().centerCrop()
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(holder.profilePic);

                        holder.profileName.setText(postUser[position].getFirstname() + " " + postUser[position].getLastname());
                    } catch (Exception e){
                        Log.d(TAG, "onDataChange: "+e.getMessage());
                    }
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
        String dtStart = statusUpdateModel.getTimePosted();

        //https://stackoverflow.com/questions/8573250/android-how-can-i-convert-string-to-date
        //Format both current date and date status update was posted
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss:Z");
        try {

            //Convert String date values to Date values
            Date dateStart;
            Date dateEnd;

            //Date dateStart = format.parse(dtStart);
            String[] timeS = Split(statusUpdateModel.getTimePosted());
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

            holder.timePosted.setText(timeAgo.toRelative(dateStart, dateEnd, 1));

        } catch (ParseException e) {
            e.printStackTrace();
            Log.d(TAG, "timeStamp: "+ e.getMessage());
        }

        //set post details
        if(statusUpdateModel.getStatus().equals("")){
            holder.userUpdate.setVisibility(View.GONE);
        } else {
            holder.userUpdate.setVisibility(View.VISIBLE);
            holder.userUpdate.setText(statusUpdateModel.getStatus());
        }

        //imageShare
        if(statusUpdateModel.getImageShare() != null){
            holder.imageShare.setVisibility(View.VISIBLE);
            try {
                Picasso.with(context).load(statusUpdateModel.getImageShare()).fit().centerCrop()
                        .placeholder(R.drawable.gray_gradient_background)
                        .error(R.drawable.gray_gradient_background)
                        .into(holder.imageShare);
            } catch (Exception e){}
        } else {
            holder.imageShare.setVisibility(View.GONE);
        }

        holder.imageShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent slideactivity = new Intent(context, ViewImage.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("imageURL", statusUpdateModel.getImageShare());
                    context.startActivity(slideactivity);
                } catch (Exception e){}
            }
        });

        //Like status (initialize)
        postDetails.child("likes").child(myPhone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    holder.likePost.setTag(R.drawable.unliked);
                    holder.likePost.setImageResource(R.drawable.unliked);
                } else {
                    holder.likePost.setTag(R.drawable.liked);
                    holder.likePost.setImageResource(R.drawable.liked);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //likes count
        postDetails.child("likes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    int total = (int) dataSnapshot.getChildrenCount();
                    Double totalLikes = Double.valueOf(total);

                    //below 1000
                    if(totalLikes < 1000){
                        DecimalFormat value = new DecimalFormat("#");
                        holder.likesTotal.setText(""+value.format(totalLikes));
                    }

                    // 1000 to 999,999
                    else if(totalLikes >= 1000 && totalLikes <= 999999){
                        if(totalLikes % 1000 == 0){ //No remainder
                            DecimalFormat value = new DecimalFormat("#####");
                            holder.likesTotal.setText(""+value.format(total/1000)+"K");
                        }

                        else { //Has remainder 999.9K
                            DecimalFormat value = new DecimalFormat("######.#");
                            Double divided = totalLikes/1000;
                            if(value.format(divided).equals("1000")){
                                holder.likesTotal.setText("1M"); //if rounded off
                            } else {
                                holder.likesTotal.setText(""+value.format(divided)+"K");
                            }
                        }
                    }

                    // 1,000,0000 to 999,999,999
                    else if(totalLikes >= 1000000 && totalLikes <= 999999999){
                        if(totalLikes % 1000000 == 0) { //No remainder
                            DecimalFormat value = new DecimalFormat("#");
                            holder.likesTotal.setText(""+value.format(totalLikes/1000000)+"M");
                        }

                        else { //Has remainder 9.9M, 999.9M etc
                            DecimalFormat value = new DecimalFormat("#.#");
                            if(value.format(totalLikes/1000000).equals("1000")){
                                holder.likesTotal.setText("1B"); //if rounded off
                            } else {
                                holder.likesTotal.setText(""+value.format(totalLikes/1000000)+"M");
                            }
                        }
                    }

                    postDetails.child("likes").child(myPhone).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(!dataSnapshot.exists()){
                                holder.likePost.setTag(R.drawable.unliked);
                                holder.likePost.setImageResource(R.drawable.unliked);
                            } else {
                                holder.likePost.setTag(R.drawable.liked);
                                holder.likePost.setImageResource(R.drawable.liked);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                } catch (Exception e){}
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //comments count
        postDetails.child("comments").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    int total = (int) dataSnapshot.getChildrenCount();
                    Double totalComments = Double.valueOf(total);

                    //below 1000
                    if(totalComments < 1000){
                        DecimalFormat value = new DecimalFormat("#");
                        holder.commentsTotal.setText(""+value.format(totalComments));
                    }

                    // 1000 to 999,999
                    else if(totalComments >= 1000 && totalComments <= 999999){
                        if(totalComments % 1000 == 0){ //No remainder
                            DecimalFormat value = new DecimalFormat("#####");
                            holder.commentsTotal.setText(""+value.format(total/1000)+"K");
                        }

                        else { //Has remainder 999.9K
                            DecimalFormat value = new DecimalFormat("######.#");
                            Double divided = totalComments/1000;
                            if(value.format(divided).equals("1000")){
                                holder.commentsTotal.setText("1M"); //if rounded off
                            } else {
                                holder.commentsTotal.setText(""+value.format(divided)+"K");
                            }
                        }
                    }

                    // 1,000,0000 to 999,999,999
                    else if(totalComments >= 1000000 && totalComments <= 999999999){
                        if(totalComments % 1000000 == 0) { //No remainder
                            DecimalFormat value = new DecimalFormat("#");
                            holder.commentsTotal.setText(""+value.format(totalComments/1000000)+"M");
                        }

                        else { //Has remainder 9.9M, 999.9M etc
                            DecimalFormat value = new DecimalFormat("#.#");
                            if(value.format(totalComments/1000000).equals("1000")){
                                holder.commentsTotal.setText("1B"); //if rounded off
                            } else {
                                holder.commentsTotal.setText(""+value.format(totalComments/1000000)+"M");
                            }
                        }
                    }
                } catch (Exception e){}
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //creating a popup menu
        PopupMenu popup = new PopupMenu(context, holder.statusOptions);
        //inflating menu from xml resource
        popup.inflate(R.menu.status_options_menu);

        Menu myMenu = popup.getMenu();
        MenuItem deleteOption = myMenu.findItem(R.id.delete);
        MenuItem reportOption = myMenu.findItem(R.id.report);

        //Can only delete posts on my wall or i've authored
        if(statusUpdateModel.getPostedTo().equals(myPhone) || statusUpdateModel.getAuthor().equals(myPhone)){
            try {
                deleteOption.setVisible(true);
            } catch (Exception e){}
        } else {
            try {
                deleteOption.setVisible(false);
            } catch (Exception e){}
        }

        //Can only report posts that i havent authored
        if(!myPhone.equals(statusUpdateModel.getAuthor())){
            try {
                reportOption.setVisible(true);
            } catch (Exception e){}
        } else {
            try {
                reportOption.setVisible(false);
            } catch (Exception e){}
        }


        //status options
        holder.statusOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //adding click listener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.delete:
                                final AlertDialog deletePost = new AlertDialog.Builder(context)
                                        //set message, title, and icon
                                        .setMessage("Delete post?")
                                        //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                                        //set three option buttons
                                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                DatabaseReference postDetails = FirebaseDatabase.getInstance().getReference("posts/"+statusUpdateModel.getPostedTo()+"/"+statusUpdateModel.key);
                                                postDetails.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        //remember if by any chance we decide to change the status check listener(to live listener)
                                                        // that does this same operation, then make sure to comment below out
                                                        try {
                                                            listdata.remove(position);
                                                            notifyItemRemoved(position);
                                                        } catch (Exception e){
                                                            Log.d(TAG, "statusUpdate: error " + e.getMessage());
                                                        }
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
                                final AlertDialog reportStatus = new AlertDialog.Builder(context)
                                        //set message, title, and icon
                                        .setMessage("Report this status?")
                                        //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                                        //set three option buttons
                                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                Intent slideactivity = new Intent(context, ReportAbuse.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                slideactivity.putExtra("type", "statusUpdate");
                                                slideactivity.putExtra("author", statusUpdateModel.getAuthor());
                                                slideactivity.putExtra("postedTo", statusUpdateModel.getPostedTo());
                                                slideactivity.putExtra("statusKey", statusUpdateModel.key);
                                                context.startActivity(slideactivity);
                                            }
                                        })
                                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
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

        holder.profileName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!myPhone.equals(statusUpdateModel.getAuthor()) && !statusUpdateModel.getPostedTo().equals(statusUpdateModel.getAuthor())){
                    Intent slideactivity = new Intent(context, ViewProfile.class)
                            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                    slideactivity.putExtra("phone", statusUpdateModel.getAuthor());
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                    context.startActivity(slideactivity, bndlanimation);
                }
            }
        });

        holder.profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent slideactivity = new Intent(context, ViewImage.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("imageURL", postUser[position].getProfilePic());
                    context.startActivity(slideactivity);
                } catch (Exception e){

                }
            }
        });

        holder.likePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = (int)holder.likePost.getTag();
                if( id == R.drawable.unliked){
                    //Add to my favourites
                    postDetails.child("likes").child(myPhone).setValue("like").addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //update likes count
                            postDetails.child("likes").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    try {
                                        int total = (int) dataSnapshot.getChildrenCount();
                                        Double totalLikes = Double.valueOf(total);

                                        //below 1000
                                        if(totalLikes < 1000){
                                            DecimalFormat value = new DecimalFormat("#");
                                            holder.likesTotal.setText(""+value.format(totalLikes));
                                        }

                                        // 1000 to 999,999
                                        else if(totalLikes >= 1000 && totalLikes <= 999999){
                                            if(totalLikes % 1000 == 0){ //No remainder
                                                DecimalFormat value = new DecimalFormat("#####");
                                                holder.likesTotal.setText(""+value.format(total/1000)+"K");
                                            }

                                            else { //Has remainder 999.9K
                                                DecimalFormat value = new DecimalFormat("######.#");
                                                Double divided = totalLikes/1000;
                                                if(value.format(divided).equals("1000")){
                                                    holder.likesTotal.setText("1M"); //if rounded off
                                                } else {
                                                    holder.likesTotal.setText(""+value.format(divided)+"K");
                                                }
                                            }
                                        }

                                        // 1,000,0000 to 999,999,999
                                        else if(totalLikes >= 1000000 && totalLikes <= 999999999){
                                            if(totalLikes % 1000000 == 0) { //No remainder
                                                DecimalFormat value = new DecimalFormat("#");
                                                holder.likesTotal.setText(""+value.format(totalLikes/1000000)+"M");
                                            }

                                            else { //Has remainder 9.9M, 999.9M etc
                                                DecimalFormat value = new DecimalFormat("#.#");
                                                if(value.format(totalLikes/1000000).equals("1000")){
                                                    holder.likesTotal.setText("1B"); //if rounded off
                                                } else {
                                                    holder.likesTotal.setText(""+value.format(totalLikes/1000000)+"M");
                                                }
                                            }
                                        }

                                        postDetails.child("likes").child(myPhone).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if(!dataSnapshot.exists()){
                                                    holder.likePost.setTag(R.drawable.unliked);
                                                    holder.likePost.setImageResource(R.drawable.unliked);
                                                } else {
                                                    holder.likePost.setTag(R.drawable.liked);
                                                    holder.likePost.setImageResource(R.drawable.liked);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });
                                    } catch (Exception e){}
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                            //send notification
                            if(!statusUpdateModel.getAuthor().equals(myPhone)){
                                DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notifications/"+statusUpdateModel.getAuthor());

                                String notifKey = notificationRef.push().getKey();
                                GetCurrentDate currentDate = new GetCurrentDate();

                                //send notification
                                NotificationModel liked = new NotificationModel();
                                liked.setFrom(myPhone);
                                liked.setType("likedstatus");
                                liked.setImage("");
                                liked.setSeen(false);
                                liked.setTimeStamp(currentDate.getDate());
                                liked.setMessage(statusUpdateModel.key);
                                liked.setAuthor(statusUpdateModel.getAuthor());
                                liked.setPostedTo(statusUpdateModel.getPostedTo());

                                notificationRef.child(notifKey).setValue(liked); //send to db

                                //Also send the notification to the person's wall where this status update appears
                                if(!statusUpdateModel.getPostedTo().equals(myPhone) && !statusUpdateModel.postedTo.equals(statusUpdateModel.getAuthor())){
                                    DatabaseReference notificationRef2 = FirebaseDatabase.getInstance().getReference("notifications/"+statusUpdateModel.getPostedTo());
                                    String notif2key = notificationRef2.push().getKey();

                                    notificationRef2.child(notif2key).setValue(liked); //send to db
                                }
                            }
//                            holder.likePost.setTag(R.drawable.liked);
//                            holder.likePost.setImageResource(R.drawable.liked);
                            //Toast.makeText(context,restaurantDetails.getName()+" added to favourites",Toast.LENGTH_SHORT).show();
                        }
                    });


                } else{
                    //Remove from my favourites
                    postDetails.child("likes").child(myPhone).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            //update likes count
                            postDetails.child("likes").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    try {
                                        int total = (int) dataSnapshot.getChildrenCount();
                                        Double totalLikes = Double.valueOf(total);

                                        //below 1000
                                        if(totalLikes < 1000){
                                            DecimalFormat value = new DecimalFormat("#");
                                            holder.likesTotal.setText(""+value.format(totalLikes));
                                        }

                                        // 1000 to 999,999
                                        else if(totalLikes >= 1000 && totalLikes <= 999999){
                                            if(totalLikes % 1000 == 0){ //No remainder
                                                DecimalFormat value = new DecimalFormat("#####");
                                                holder.likesTotal.setText(""+value.format(total/1000)+"K");
                                            }

                                            else { //Has remainder 999.9K
                                                DecimalFormat value = new DecimalFormat("######.#");
                                                Double divided = totalLikes/1000;
                                                if(value.format(divided).equals("1000")){
                                                    holder.likesTotal.setText("1M"); //if rounded off
                                                } else {
                                                    holder.likesTotal.setText(""+value.format(divided)+"K");
                                                }
                                            }
                                        }

                                        // 1,000,0000 to 999,999,999
                                        else if(totalLikes >= 1000000 && totalLikes <= 999999999){
                                            if(totalLikes % 1000000 == 0) { //No remainder
                                                DecimalFormat value = new DecimalFormat("#");
                                                holder.likesTotal.setText(""+value.format(totalLikes/1000000)+"M");
                                            }

                                            else { //Has remainder 9.9M, 999.9M etc
                                                DecimalFormat value = new DecimalFormat("#.#");
                                                if(value.format(totalLikes/1000000).equals("1000")){
                                                    holder.likesTotal.setText("1B"); //if rounded off
                                                } else {
                                                    holder.likesTotal.setText(""+value.format(totalLikes/1000000)+"M");
                                                }
                                            }
                                        }

                                        postDetails.child("likes").child(myPhone).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                if(!dataSnapshot.exists()){
                                                    holder.likePost.setTag(R.drawable.unliked);
                                                    holder.likePost.setImageResource(R.drawable.unliked);
                                                } else {
                                                    holder.likePost.setTag(R.drawable.liked);
                                                    holder.likePost.setImageResource(R.drawable.liked);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });
                                    } catch (Exception e){}
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

//                            holder.likePost.setTag(R.drawable.unliked);
//                            holder.likePost.setImageResource(R.drawable.unliked);
                        }
                    });

                }
            }
        });

        holder.sharePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "share!", Toast.LENGTH_SHORT).show();
            }
        });

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent slideactivity = new Intent(context, ViewStatus.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                slideactivity.putExtra("author", statusUpdateModel.getAuthor());
                slideactivity.putExtra("postedTo", statusUpdateModel.getPostedTo());
                slideactivity.putExtra("key", statusUpdateModel.key);
                Bundle bndlanimation = ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                context.startActivity(slideactivity, bndlanimation);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listdata.size();
    }

    class MyHolder extends RecyclerView.ViewHolder{
        MyTextView_Roboto_Regular profileName, userUpdate, likesTotal, commentsTotal;
        MyTextView_Roboto_Light timePosted;
        TextView statusOptions;
        ImageView profilePic, imageShare, likePost, comments, sharePost;
        CardView cardView;

        public MyHolder(View itemView) {
            super(itemView);

            profileName = itemView.findViewById(R.id.profileName);
            userUpdate = itemView.findViewById(R.id.userUpdate);
            profilePic = itemView.findViewById(R.id.profilePic);
            imageShare = itemView.findViewById(R.id.imageShare);
            likePost = itemView.findViewById(R.id.likePost);
            comments = itemView.findViewById(R.id.comments);
            sharePost = itemView.findViewById(R.id.sharePost);
            likesTotal = itemView.findViewById(R.id.likesTotal);
            commentsTotal = itemView.findViewById(R.id.commentsTotal);
            cardView = itemView.findViewById(R.id.card_view);
            timePosted = itemView.findViewById(R.id.timePosted);
            statusOptions = itemView.findViewById(R.id.statusOptions);

            //Long Press
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    setClipboard(context, listdata.get(getAdapterPosition()).getStatus());
                    return false;
                }
            });
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

    private void setClipboard(Context context, String text) {
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
            SafeToast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show();

        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
            SafeToast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show();
        }
    }

    public String[] Split(String timeStamp){

        String[] arrSplit = timeStamp.split(":");

        return arrSplit;
    }
}