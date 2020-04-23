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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.malcolmmaima.dishi.Controller.GetCurrentDate;
import com.malcolmmaima.dishi.Controller.TimeAgo;
import com.malcolmmaima.dishi.Model.StatusUpdateModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.malcolmmaima.dishi.View.Activities.ViewProfile;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.crashlytics.android.core.CrashlyticsCore.TAG;

public class ReviewReplyAdapter extends RecyclerView.Adapter<ReviewReplyAdapter.MyHolder> {

    Context context;
    List<StatusUpdateModel> listdata;
    DatabaseReference postRef, commentAuthorUserDetailsRef;
    long DURATION = 200;

    public ReviewReplyAdapter(Context context, List<StatusUpdateModel> listdata) {
        this.listdata = listdata;
        this.context = context;
    }

    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_review_reply,parent,false);

        MyHolder myHolder = new MyHolder(view);
        return myHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ReviewReplyAdapter.MyHolder holder, final int position) {

        final StatusUpdateModel statusUpdateModel = listdata.get(position);

        /**
         * Adapter animation
         */
        setAnimation(holder.itemView, position);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final String myPhone = user.getPhoneNumber(); //Current logged in user phone number
        postRef = FirebaseDatabase.getInstance().getReference("reviews/"+statusUpdateModel.getPostedTo());
        commentAuthorUserDetailsRef = FirebaseDatabase.getInstance().getReference("users/"+statusUpdateModel.getAuthor());
        UserModel[] commentUser = new UserModel[listdata.size()];

        //fetch post User Details
        commentAuthorUserDetailsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    commentUser[position] = dataSnapshot.getValue(UserModel.class);

                    //Set profile pic
                    Picasso.with(context).load(commentUser[position].getProfilePic()).fit().centerCrop()
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(holder.profilePic);

                    holder.profileName.setText(commentUser[position].getFirstname() + " " + commentUser[position].getLastname());
                } catch (Exception e){
                    Log.d(TAG, "onDataChange: "+e.getMessage());
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
                Intent slideactivity = new Intent(context, ViewImage.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                slideactivity.putExtra("imageURL", commentUser[position].getProfilePic());
                context.startActivity(slideactivity);
            }
        });

        /**
         * date string conversion to Date:
         * https://stackoverflow.com/questions/8573250/android-how-can-i-convert-string-to-date
         */
        //Format both current date and date status update was posted
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss:Z");
        try {
            //Get today's date
            GetCurrentDate currentDate = new GetCurrentDate();
            String currDate = currentDate.getDate();

            //Get date status update was posted
            String dtEnd = currDate;
            String dtStart = statusUpdateModel.getTimePosted();

            //Convert String date values to Date values
            Date dateEnd = format.parse(dtStart);
            Date dateStart = format.parse(dtEnd);

            /**
             * refer to: https://memorynotfound.com/calculate-relative-time-time-ago-java/
             */
            //Now compute timeAgo duration
            TimeAgo timeAgo = new TimeAgo();
            timeAgo.toRelative(dateStart, dateEnd);

            holder.timePosted.setText(timeAgo.toRelative(dateEnd, dateStart, 1));
            //Toast.makeText(context, "ago: " + timeAgo.toRelative(dateEnd, dateStart), Toast.LENGTH_LONG).show();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return listdata.size();
    }

    class MyHolder extends RecyclerView.ViewHolder{
        TextView profileName, userUpdate, likesTotal, commentsTotal, timePosted;
        ImageView profilePic, deleteBtn, likePost, comments, sharePost, imageShare;
        CardView cardView;

        public MyHolder(View itemView) {
            super(itemView);

            profileName = itemView.findViewById(R.id.profileName);
            userUpdate = itemView.findViewById(R.id.userUpdate);
            profilePic = itemView.findViewById(R.id.profilePic);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
            likePost = itemView.findViewById(R.id.likePost);
            comments = itemView.findViewById(R.id.comments);
            sharePost = itemView.findViewById(R.id.sharePost);
            imageShare = itemView.findViewById(R.id.imageShare);
            likesTotal = itemView.findViewById(R.id.likesTotal);
            commentsTotal = itemView.findViewById(R.id.commentsTotal);
            cardView = itemView.findViewById(R.id.card_view);
            timePosted = itemView.findViewById(R.id.timePosted);

            //Long Press
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    String myPhone = user.getPhoneNumber(); //Current logged in user phone number
                    if(listdata.get(getAdapterPosition()).getPostedTo().equals(myPhone) || listdata.get(getAdapterPosition()).getAuthor().equals(myPhone)){
                        //I (logged in user) can delete any comment on any post on my wall as i wish
                        final AlertDialog deleteComment = new AlertDialog.Builder(v.getContext())
                                //set message, title, and icon
                                .setMessage("Delete comment?")
                                //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                                //set three option buttons
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        postRef.child(listdata.get(getAdapterPosition()).getCommentKey()).child("comments").child(listdata.get(getAdapterPosition()).key).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                try {
                                                    listdata.remove(getAdapterPosition());
                                                    notifyItemRemoved(getAdapterPosition());
                                                } catch (Exception e){}
                                            }
                                        });
                                    }
                                }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        //do nothing

                                    }
                                })//setNegativeButton

                                .create();
                        deleteComment.show();
                    }
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
}
