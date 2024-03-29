package com.malcolmmaima.dishiapp.View.Adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Light;
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishiapp.Controller.Utils.GetCurrentDate;
import com.malcolmmaima.dishiapp.Controller.Utils.TimeAgo;
import com.malcolmmaima.dishiapp.Model.NotificationModel;
import com.malcolmmaima.dishiapp.Model.StatusUpdateModel;
import com.malcolmmaima.dishiapp.Model.UserModel;
import com.malcolmmaima.dishiapp.R;
import com.malcolmmaima.dishiapp.View.Activities.ReportAbuse;
import com.malcolmmaima.dishiapp.View.Activities.SearchActivity;
import com.malcolmmaima.dishiapp.View.Activities.ViewImage;
import com.malcolmmaima.dishiapp.View.Activities.ViewProfile;
import com.malcolmmaima.dishiapp.View.Activities.ViewShareFoodItems;
import com.malcolmmaima.dishiapp.View.Activities.ViewStatus;
import com.squareup.picasso.Picasso;
import com.volokh.danylo.hashtaghelper.HashTagHelper;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class NewsFeedAdapter extends RecyclerView.Adapter<NewsFeedAdapter.MyHolder> {

    Context context;
    List<StatusUpdateModel> listdata;
    long DURATION = 250;
    String TAG = "NewsFeedAdapter";
    int index;
    String stringToBeInserted;
    HashTagHelper mTextHashTagHelper;

    public NewsFeedAdapter(Context context, List<StatusUpdateModel> listdata) {
        this.listdata = listdata;
        this.context = context;
    }

    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_newsfeed,parent,false);

        MyHolder myHolder = new MyHolder(view);
        return myHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final NewsFeedAdapter.MyHolder holder, final int position) {

        final StatusUpdateModel statusUpdateModel = listdata.get(position);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String myPhone = user.getPhoneNumber(); //Current logged in user phone number
        UserModel [] postUser = new UserModel[listdata.size()];
        UserModel [] postedTo = new UserModel[listdata.size()];

        holder.foodShare.setVisibility(View.GONE); //by default dont show unless it's an order share
        holder.postedToPic.setVisibility(View.GONE);
        holder.postedTo.setVisibility(View.GONE);
        holder.imageShare.setVisibility(View.GONE);
        holder.mediaItems.setVisibility(View.GONE);
        /**
         * Adapter animation
         */
        setAnimation(holder.itemView, position);

        //Setup db references
        DatabaseReference postedToRef = FirebaseDatabase.getInstance().getReference("users/"+statusUpdateModel.getPostedTo());
        DatabaseReference postUserDetails = FirebaseDatabase.getInstance().getReference("users/"+statusUpdateModel.getAuthor());
        DatabaseReference postDetails = FirebaseDatabase.getInstance().getReference("posts/"+statusUpdateModel.getPostedTo()+"/"+statusUpdateModel.key);

        if(statusUpdateModel.getAuthor().equals(statusUpdateModel.getPostedTo())){
            holder.postedToPic.setVisibility(View.GONE);
            holder.postedTo.setVisibility(View.GONE);
        } else {
            //fetch post User Details
            postedToRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.exists()){
                        holder.postedToPic.setVisibility(View.VISIBLE);
                        holder.postedTo.setVisibility(View.VISIBLE);
                        holder.postedTo.setText("[Deleted user]");
                        holder.postedToPic.setImageResource(R.drawable.default_profile);
                    } else {
                        try {
                            holder.postedToPic.setVisibility(View.VISIBLE);
                            holder.postedTo.setVisibility(View.VISIBLE);
                            postedTo[position] = dataSnapshot.getValue(UserModel.class);
                            holder.postedTo.setText(postedTo[position].getFirstname() + " " + postedTo[position].getLastname());


                            //Set profile pic
                            if(postedTo[position].getProfilePicSmall() != null){
                                Picasso.with(context).load(postedTo[position].getProfilePicSmall()).fit().centerCrop()
                                        .placeholder(R.drawable.default_profile)
                                        .error(R.drawable.default_profile)
                                        .into(holder.postedToPic);
                            }

                            else {
                                Picasso.with(context).load(postedTo[position].getProfilePic()).fit().centerCrop()
                                        .placeholder(R.drawable.default_profile)
                                        .error(R.drawable.default_profile)
                                        .into(holder.postedToPic);
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

        holder.postedToPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(postedTo[position].getProfilePicBig() != null){
                        Intent slideactivity = new Intent(context, ViewImage.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        slideactivity.putExtra("imageURL", postedTo[position].getProfilePicBig());
                        context.startActivity(slideactivity);
                    }

                    else {
                        Intent slideactivity = new Intent(context, ViewImage.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        slideactivity.putExtra("imageURL", postedTo[position].getProfilePic());
                        context.startActivity(slideactivity);
                    }
                } catch (Exception e){
                    Log.e(TAG, "onClick: ", e);
                }
            }
        });

        holder.postedTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!myPhone.equals(statusUpdateModel.getPostedTo())){
                    Intent slideactivity = new Intent(context, ViewProfile.class)
                            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                    slideactivity.putExtra("phone", statusUpdateModel.getPostedTo());
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                    context.startActivity(slideactivity, bndlanimation);
                }
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
                        if(postUser[position].getProfilePicSmall() != null){
                            Picasso.with(context).load(postUser[position].getProfilePicSmall()).fit().centerCrop()
                                    .placeholder(R.drawable.default_profile)
                                    .error(R.drawable.default_profile)
                                    .into(holder.profilePic);
                        }

                        else {
                            Picasso.with(context).load(postUser[position].getProfilePic()).fit().centerCrop()
                                    .placeholder(R.drawable.default_profile)
                                    .error(R.drawable.default_profile)
                                    .into(holder.profilePic);
                        }

                        holder.profileName.setText(postUser[position].getFirstname() + " " + postUser[position].getLastname());
                    } catch (Exception e){
                        //Log.d(TAG, "onDataChange: "+e.getMessage());
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
            //Log.d(TAG, "timeStamp: "+ e.getMessage());
        }

        //set post details
        if(statusUpdateModel.getStatus().equals("")){
            holder.userUpdate.setVisibility(View.GONE);
        } else {
            holder.userUpdate.setVisibility(View.VISIBLE);
            holder.userUpdate.setText(statusUpdateModel.getStatus());

            //Handle hyperlinks
            try {
                Linkify.addLinks(holder.userUpdate, Linkify.ALL);
            } catch(Exception e){
                Log.e(TAG, "onBindViewHolder: ", e);
            }

            //handle hashtags
            if(statusUpdateModel.getStatus().contains("#")){
                mTextHashTagHelper = HashTagHelper.Creator.create(context.getResources().getColor(R.color.colorPrimary),
                        new HashTagHelper.OnHashTagClickListener() {
                            @Override
                            public void onHashTagClicked(String hashTag) {
                                if(statusUpdateModel.type != null){ //Meaning 'type' value has been passed from FragmentSearchPosts
                                    if(statusUpdateModel.type.equals("searchPost")){
                                        //do nothing
                                    }
                                } else {
                                    //redirect to search (default hashtag search will be posts fragment)
                                    String searchHashTag = "#"+hashTag;
                                    Intent slideactivity = new Intent(context, SearchActivity.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    slideactivity.putExtra("searchString", searchHashTag);
                                    slideactivity.putExtra("goToFragment", 3);
                                    context.startActivity(slideactivity);
                                }
                            }
                        });

                mTextHashTagHelper.handle(holder.userUpdate);
            }
        }

        //imageShare
        if(statusUpdateModel.getImageShare() != null){
            holder.mediaItems.setVisibility(View.VISIBLE);
            holder.imageShare.setVisibility(View.VISIBLE);
            try {
                if(statusUpdateModel.getImageShareBig() != null){
                    Picasso.with(context).load(statusUpdateModel.getImageShareBig()).fit().centerCrop()
                            .placeholder(R.drawable.gray_gradient_background)
                            .error(R.drawable.gray_gradient_background)
                            .into(holder.imageShare);
                }

                else {
                    Picasso.with(context).load(statusUpdateModel.getImageShare()).fit().centerCrop()
                            .placeholder(R.drawable.gray_gradient_background)
                            .error(R.drawable.gray_gradient_background)
                            .into(holder.imageShare);
                }
            } catch (Exception e){
                Log.e(TAG, "onBindViewHolder: ", e);
            }
        } else {
            holder.mediaItems.setVisibility(View.GONE);
            holder.imageShare.setVisibility(View.GONE);
        }

        holder.imageShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {

                    if(statusUpdateModel.getImageShareBig() != null){
                        Intent slideactivity = new Intent(context, ViewImage.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        slideactivity.putExtra("imageURL", statusUpdateModel.getImageShareBig());
                        context.startActivity(slideactivity);
                    }

                    else {
                        Intent slideactivity = new Intent(context, ViewImage.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        slideactivity.putExtra("imageURL", statusUpdateModel.getImageShare());
                        context.startActivity(slideactivity);
                    }
                } catch (Exception e){
                    Log.e(TAG, "onClick: ", e);
                }
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

                                                //delete image from storage if exists in post
                                                if(statusUpdateModel.getImageShare() != null){
                                                    try {
                                                        FirebaseStorage storage = FirebaseStorage.getInstance();
                                                        StorageReference storageRefOriginal = storage.getReferenceFromUrl(statusUpdateModel.getImageShare());
                                                        StorageReference storageImgBig = storage.getReferenceFromUrl(statusUpdateModel.getImageShareBig());
                                                        StorageReference storageImgMedium = storage.getReferenceFromUrl(statusUpdateModel.getImageShareMedium());
                                                        StorageReference storageImgSmall = storage.getReferenceFromUrl(statusUpdateModel.getImageShareSmall());

                                                        //Delete images from storage
                                                        storageRefOriginal.delete();
                                                        storageImgBig.delete();
                                                        storageImgMedium.delete();
                                                        storageImgSmall.delete();
                                                    } catch (Exception e){

                                                    }
                                                }

                                                DatabaseReference postDetails = FirebaseDatabase.getInstance().getReference("posts/"+statusUpdateModel.getPostedTo()+"/"+statusUpdateModel.key);
                                                postDetails.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        try {
                                                            listdata.remove(holder.getAdapterPosition());
                                                            notifyItemRemoved(holder.getAdapterPosition());
                                                        } catch (Exception e){
                                                            Log.e(TAG, "NewsFeedAdapter: error ", e);
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
                if(!myPhone.equals(statusUpdateModel.getAuthor())){
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
                    if(postUser[position].getProfilePicBig() != null){
                        Intent slideactivity = new Intent(context, ViewImage.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        slideactivity.putExtra("imageURL", postUser[position].getProfilePicBig());
                        context.startActivity(slideactivity);
                    }

                    else {
                        Intent slideactivity = new Intent(context, ViewImage.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        slideactivity.putExtra("imageURL", postUser[position].getProfilePic());
                        context.startActivity(slideactivity);
                    }
                } catch (Exception e){
                    Log.e(TAG, "onClick: ", e);
                }
            }
        });

        holder.likePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int id = (int) holder.likePost.getTag();
                    if (id == R.drawable.unliked) {
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
                                            if (totalLikes < 1000) {
                                                DecimalFormat value = new DecimalFormat("#");
                                                holder.likesTotal.setText("" + value.format(totalLikes));
                                            }

                                            // 1000 to 999,999
                                            else if (totalLikes >= 1000 && totalLikes <= 999999) {
                                                if (totalLikes % 1000 == 0) { //No remainder
                                                    DecimalFormat value = new DecimalFormat("#####");
                                                    holder.likesTotal.setText("" + value.format(total / 1000) + "K");
                                                } else { //Has remainder 999.9K
                                                    DecimalFormat value = new DecimalFormat("######.#");
                                                    Double divided = totalLikes / 1000;
                                                    if (value.format(divided).equals("1000")) {
                                                        holder.likesTotal.setText("1M"); //if rounded off
                                                    } else {
                                                        holder.likesTotal.setText("" + value.format(divided) + "K");
                                                    }
                                                }
                                            }

                                            // 1,000,0000 to 999,999,999
                                            else if (totalLikes >= 1000000 && totalLikes <= 999999999) {
                                                if (totalLikes % 1000000 == 0) { //No remainder
                                                    DecimalFormat value = new DecimalFormat("#");
                                                    holder.likesTotal.setText("" + value.format(totalLikes / 1000000) + "M");
                                                } else { //Has remainder 9.9M, 999.9M etc
                                                    DecimalFormat value = new DecimalFormat("#.#");
                                                    if (value.format(totalLikes / 1000000).equals("1000")) {
                                                        holder.likesTotal.setText("1B"); //if rounded off
                                                    } else {
                                                        holder.likesTotal.setText("" + value.format(totalLikes / 1000000) + "M");
                                                    }
                                                }
                                            }

                                            postDetails.child("likes").child(myPhone).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    if (!dataSnapshot.exists()) {
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
                                        } catch (Exception e) {
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                                //send notification
                                if (!statusUpdateModel.getAuthor().equals(myPhone)) {
                                    DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notifications/" + statusUpdateModel.getAuthor());

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
                                    if (!statusUpdateModel.getPostedTo().equals(myPhone) && !statusUpdateModel.postedTo.equals(statusUpdateModel.getAuthor())) {
                                        DatabaseReference notificationRef2 = FirebaseDatabase.getInstance().getReference("notifications/" + statusUpdateModel.getPostedTo());
                                        String notif2key = notificationRef2.push().getKey();

                                        notificationRef2.child(notif2key).setValue(liked); //send to db
                                    }
                                }
    //                            holder.likePost.setTag(R.drawable.liked);
    //                            holder.likePost.setImageResource(R.drawable.liked);
                                //Toast.makeText(context,restaurantDetails.getName()+" added to favourites",Toast.LENGTH_SHORT).show();
                            }
                        });


                    } else {
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
                                            if (totalLikes < 1000) {
                                                DecimalFormat value = new DecimalFormat("#");
                                                holder.likesTotal.setText("" + value.format(totalLikes));
                                            }

                                            // 1000 to 999,999
                                            else if (totalLikes >= 1000 && totalLikes <= 999999) {
                                                if (totalLikes % 1000 == 0) { //No remainder
                                                    DecimalFormat value = new DecimalFormat("#####");
                                                    holder.likesTotal.setText("" + value.format(total / 1000) + "K");
                                                } else { //Has remainder 999.9K
                                                    DecimalFormat value = new DecimalFormat("######.#");
                                                    Double divided = totalLikes / 1000;
                                                    if (value.format(divided).equals("1000")) {
                                                        holder.likesTotal.setText("1M"); //if rounded off
                                                    } else {
                                                        holder.likesTotal.setText("" + value.format(divided) + "K");
                                                    }
                                                }
                                            }

                                            // 1,000,0000 to 999,999,999
                                            else if (totalLikes >= 1000000 && totalLikes <= 999999999) {
                                                if (totalLikes % 1000000 == 0) { //No remainder
                                                    DecimalFormat value = new DecimalFormat("#");
                                                    holder.likesTotal.setText("" + value.format(totalLikes / 1000000) + "M");
                                                } else { //Has remainder 9.9M, 999.9M etc
                                                    DecimalFormat value = new DecimalFormat("#.#");
                                                    if (value.format(totalLikes / 1000000).equals("1000")) {
                                                        holder.likesTotal.setText("1B"); //if rounded off
                                                    } else {
                                                        holder.likesTotal.setText("" + value.format(totalLikes / 1000000) + "M");
                                                    }
                                                }
                                            }

                                            postDetails.child("likes").child(myPhone).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    if (!dataSnapshot.exists()) {
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
                                        } catch (Exception e) {
                                        }
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
                } catch (Exception e){
                    Log.e(TAG, "onClick: ", e);
                }
            }
        });

        if(statusUpdateModel.getReceiptKey() != null){
            holder.mediaItems.setVisibility(View.VISIBLE);
            holder.foodShare.setVisibility(View.VISIBLE);

            //replace default rider image with vendor's profile image
            if(statusUpdateModel.getVendorPhone() != null){
                DatabaseReference vendorDetailsRef = FirebaseDatabase.getInstance().getReference("users/"+statusUpdateModel.getVendorPhone());
                vendorDetailsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            try {
                                UserModel vendorDetails = dataSnapshot.getValue(UserModel.class);
                                if (vendorDetails.getProfilePicSmall() != null) {
                                    Picasso.with(context).load(vendorDetails.getProfilePicSmall()).fit().centerCrop()
                                            .placeholder(R.drawable.delivery_bike)
                                            .error(R.drawable.delivery_bike)
                                            .into(holder.vendorPic);
                                } else {
                                    Picasso.with(context).load(vendorDetails.getProfilePic()).fit().centerCrop()
                                            .placeholder(R.drawable.delivery_bike)
                                            .error(R.drawable.delivery_bike)
                                            .into(holder.vendorPic);
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

        }

        holder.foodShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent slideactivity = new Intent(context, ViewShareFoodItems.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                slideactivity.putExtra("receiptKey", statusUpdateModel.getReceiptKey());
                slideactivity.putExtra("author", statusUpdateModel.getAuthor());
                slideactivity.putExtra("vendorPhone", statusUpdateModel.getVendorPhone());
                context.startActivity(slideactivity);
            }
        });


        holder.sharePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "share!", Toast.LENGTH_SHORT).show();
            }
        });

        holder.userUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent slideactivity = new Intent(context, ViewStatus.class)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                slideactivity.putExtra("author", statusUpdateModel.getAuthor());
                slideactivity.putExtra("postedTo", statusUpdateModel.getPostedTo());
                slideactivity.putExtra("key", statusUpdateModel.key);
                Bundle bndlanimation = ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                context.startActivity(slideactivity, bndlanimation);
            }
        });

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent slideactivity = new Intent(context, ViewStatus.class)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                slideactivity.putExtra("author", statusUpdateModel.getAuthor());
                slideactivity.putExtra("postedTo", statusUpdateModel.getPostedTo());
                slideactivity.putExtra("key", statusUpdateModel.key);
                Bundle bndlanimation = ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                context.startActivity(slideactivity, bndlanimation);
            }
        });
    }

    @Override
    public void onViewAttachedToWindow(@NonNull MyHolder holder) {
        super.onViewAttachedToWindow(holder);

        StatusUpdateModel statusUpdateModel = listdata.get(holder.getAdapterPosition());
        //fetch post User Details
        DatabaseReference postUserDetails = FirebaseDatabase.getInstance().getReference("users/"+statusUpdateModel.getAuthor());
        postUserDetails.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    holder.likePost.setEnabled(false);
                    holder.profilePic.setEnabled(false);
                } else {
                    try {
                        UserModel getUser = dataSnapshot.getValue(UserModel.class);

                        //Set profile pic

                        if(getUser.getProfilePicSmall() != null){
                            Picasso.with(context).load(getUser.getProfilePicSmall()).fit().centerCrop()
                                    .placeholder(R.drawable.default_profile)
                                    .error(R.drawable.default_profile)
                                    .into(holder.profilePic);
                        }

                        else {
                            Picasso.with(context).load(getUser.getProfilePic()).fit().centerCrop()
                                    .placeholder(R.drawable.default_profile)
                                    .error(R.drawable.default_profile)
                                    .into(holder.profilePic);
                        }

                        holder.profileName.setText(getUser.getFirstname() + " " + getUser.getLastname());
                    } catch (Exception e){
                        //Log.d(TAG, "onDataChange: "+e.getMessage());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return listdata.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder{
        MyTextView_Roboto_Medium profileName;
        MyTextView_Roboto_Regular postedTo, userUpdate, likesTotal, commentsTotal;
        MyTextView_Roboto_Light timePosted;
        TextView statusOptions;
        ImageView profilePic, postedToPic,imageShare, likePost, comments, sharePost, vendorPic;
        RelativeLayout foodShare, mediaItems;
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
            postedToPic = itemView.findViewById(R.id.postedToPic);
            postedTo = itemView.findViewById(R.id.postedTo);
            foodShare = itemView.findViewById(R.id.foodShare);
            mediaItems = itemView.findViewById(R.id.mediaItems);
            vendorPic = itemView.findViewById(R.id.foodPic);

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
        animator.setDuration(300);
        animatorSet.play(animator);
        animator.start();
    }

    private void setClipboard(Context context, String text) {
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show();

        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show();
        }
    }

    public String[] Split(String timeStamp){

        String[] arrSplit = timeStamp.split(":");

        return arrSplit;
    }
}