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
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Light;
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishiapp.Controller.Utils.GetCurrentDate;
import com.malcolmmaima.dishiapp.Controller.Utils.TimeAgo;
import com.malcolmmaima.dishiapp.Model.StatusUpdateModel;
import com.malcolmmaima.dishiapp.Model.UserModel;
import com.malcolmmaima.dishiapp.R;
import com.malcolmmaima.dishiapp.View.Activities.ReportAbuse;
import com.malcolmmaima.dishiapp.View.Activities.SearchActivity;
import com.malcolmmaima.dishiapp.View.Activities.ViewImage;
import com.malcolmmaima.dishiapp.View.Activities.ViewProfile;
import com.squareup.picasso.Picasso;
import com.volokh.danylo.hashtaghelper.HashTagHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.MyHolder> {

    Context context;
    List<StatusUpdateModel> listdata;
    DatabaseReference postRef, commentAuthorUserDetailsRef;
    long DURATION = 200;
    HashTagHelper mTextHashTagHelper;

    public CommentAdapter(Context context, List<StatusUpdateModel> listdata) {
        this.listdata = listdata;
        this.context = context;
    }

    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_comment,parent,false);

        MyHolder myHolder = new MyHolder(view);
        return myHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final CommentAdapter.MyHolder holder, final int position) {

        final StatusUpdateModel statusUpdateModel = listdata.get(position);

        /**
         * Adapter animation
         */
        setAnimation(holder.itemView, position);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final String myPhone = user.getPhoneNumber(); //Current logged in user phone number
        postRef = FirebaseDatabase.getInstance().getReference("posts/"+statusUpdateModel.getPostedTo());
        commentAuthorUserDetailsRef = FirebaseDatabase.getInstance().getReference("users/"+statusUpdateModel.getAuthor());
        UserModel[] commentUser = new UserModel[listdata.size()];

        //fetch post User Details
        commentAuthorUserDetailsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    commentUser[position] = dataSnapshot.getValue(UserModel.class);

                    //Set profile pic

                    if(commentUser[position].getProfilePicSmall() != null){
                        Picasso.with(context).load(commentUser[position].getProfilePicSmall()).fit().centerCrop()
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(holder.profilePic);
                    }

                    else {
                        Picasso.with(context).load(commentUser[position].getProfilePic()).fit().centerCrop()
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(holder.profilePic);
                    }

                    holder.profileName.setText(commentUser[position].getFirstname() + " " + commentUser[position].getLastname());
                } catch (Exception e){
                    //Log.d(TAG, "onDataChange: "+e.getMessage());
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //set comment
        if(statusUpdateModel.getStatus().equals("")){
            holder.userUpdate.setVisibility(View.GONE);
        } else {
            holder.userUpdate.setVisibility(View.VISIBLE);
            holder.userUpdate.setText(statusUpdateModel.getStatus());
            try {
                Linkify.addLinks(holder.userUpdate, Linkify.ALL);
            } catch(Exception e){
                Log.e(TAG, "onBindViewHolder: ", e);
            }
        }

        //handle hashtags
        if(statusUpdateModel.getStatus().contains("#")){
            mTextHashTagHelper = HashTagHelper.Creator.create(context.getResources().getColor(R.color.colorPrimary),
                    new HashTagHelper.OnHashTagClickListener() {
                        @Override
                        public void onHashTagClicked(String hashTag) {
                            String searchHashTag = "#"+hashTag;
                            Intent slideactivity = new Intent(context, SearchActivity.class)
                                    .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            slideactivity.putExtra("searchString", searchHashTag);
                            slideactivity.putExtra("goToFragment", 3);
                            context.startActivity(slideactivity);
                        }
                    });

            mTextHashTagHelper.handle(holder.userUpdate);
        }

        //imageShare
        if(statusUpdateModel.getImageShare() != null){
            holder.imageShare.setVisibility(View.VISIBLE);
            try {

                if(statusUpdateModel.getImageShareMedium() != null){
                    Picasso.with(context).load(statusUpdateModel.getImageShareMedium()).fit().centerCrop()
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
            holder.imageShare.setVisibility(View.GONE);
        }

        //creating a popup menu
        PopupMenu popup = new PopupMenu(context, holder.commentOptions);
        //inflating menu from xml resource
        popup.inflate(R.menu.comment_options_menu);

        Menu myMenu = popup.getMenu();
        MenuItem deleteOption = myMenu.findItem(R.id.delete);
        MenuItem reportOption = myMenu.findItem(R.id.report);

        //Can only delete comments that i have authored or on my posts
        if(statusUpdateModel.getPostedTo().equals(myPhone) || statusUpdateModel.getAuthor().equals(myPhone)){
            try {
                deleteOption.setVisible(true);
            } catch (Exception e){}
        } else {
            try {
                deleteOption.setVisible(false);
            } catch (Exception e){}
        }

        //Can only report comments that i havent authored
        if(!myPhone.equals(statusUpdateModel.getAuthor())){
            try {
                reportOption.setVisible(true);
            } catch (Exception e){}
        } else {
            try {
                reportOption.setVisible(false);
            } catch (Exception e){}
        }

        holder.commentOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {

                            case R.id.delete:
                                final AlertDialog deleteComment = new AlertDialog.Builder(context)
                                        //set message, title, and icon
                                        .setMessage("Delete comment?")
                                        //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                                        //set three option buttons
                                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                try {
                                                    postRef.child(statusUpdateModel.getCommentKey())
                                                            .child("comments").child(statusUpdateModel.key)
                                                            .removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            try {
                                                                listdata.remove(position);
                                                                notifyItemRemoved(position);
                                                            } catch (Exception e) {
                                                            }
                                                        }
                                                    });
                                                } catch (Exception e){}
                                            }
                                        }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                //do nothing

                                            }
                                        })//setNegativeButton

                                        .create();
                                deleteComment.show();
                                return (true);
                            case R.id.report:
                                    final AlertDialog reportStatus = new AlertDialog.Builder(context)
                                            //set message, title, and icon
                                            .setMessage("Report this comment?")
                                            //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                                            //set three option buttons
                                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    Intent slideactivity = new Intent(context, ReportAbuse.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    slideactivity.putExtra("type", "commentUpdate");
                                                    slideactivity.putExtra("author", statusUpdateModel.getAuthor());
                                                    slideactivity.putExtra("postedTo", statusUpdateModel.getPostedTo());
                                                    slideactivity.putExtra("commentKey", statusUpdateModel.key);
                                                    context.startActivity(slideactivity);
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

        holder.profileName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!myPhone.equals(statusUpdateModel.getAuthor())){
                    Intent slideactivity = new Intent(context, ViewProfile.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

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
                if(commentUser[position].getProfilePicSmall() != null){
                    Intent slideactivity = new Intent(context, ViewImage.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("imageURL", commentUser[position].getProfilePicSmall());
                    context.startActivity(slideactivity);
                }

                else {
                    Intent slideactivity = new Intent(context, ViewImage.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("imageURL", commentUser[position].getProfilePic());
                    context.startActivity(slideactivity);
                }
            }
        });


        //Get today's date
        GetCurrentDate currentDate = new GetCurrentDate();
        String currDate = currentDate.getDate();

        //Get date status update was posted
        String dtEnd = currDate;
        String dtStart = statusUpdateModel.getTimePosted();
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

            /**
             * refer to: https://memorynotfound.com/calculate-relative-time-time-ago-java/
             */
            //Now compute timeAgo duration
            TimeAgo timeAgo = new TimeAgo();

            holder.timePosted.setText(timeAgo.toRelative(dateStart, dateEnd, 1));
            //Toast.makeText(context, "ago: " + timeAgo.toRelative(dateEnd, dateStart), Toast.LENGTH_LONG).show();
        } catch (ParseException e) {
            e.printStackTrace();
            //Log.d(TAG, "timeStamp: "+e.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return listdata.size();
    }

    class MyHolder extends RecyclerView.ViewHolder{
        MyTextView_Roboto_Medium profileName;
        MyTextView_Roboto_Regular userUpdate;
        MyTextView_Roboto_Light timePosted;
        TextView commentOptions;
        ImageView profilePic, imageShare;
        CardView cardView;

        public MyHolder(View itemView) {
            super(itemView);

            profileName = itemView.findViewById(R.id.profileName);
            userUpdate = itemView.findViewById(R.id.userUpdate);
            profilePic = itemView.findViewById(R.id.profilePic);
            imageShare = itemView.findViewById(R.id.imageShare);
            cardView = itemView.findViewById(R.id.card_view);
            timePosted = itemView.findViewById(R.id.timePosted);
            commentOptions = itemView.findViewById(R.id.commentOptions);

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
