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
import com.google.firebase.firestore.auth.User;
import com.malcolmmaima.dishi.Model.StatusUpdateModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.malcolmmaima.dishi.View.Activities.ViewProfile;
import com.malcolmmaima.dishi.View.Activities.ViewStatus;
import com.squareup.picasso.Picasso;

import java.util.List;


import static com.crashlytics.android.core.CrashlyticsCore.TAG;


public class StatusUpdateAdapter extends RecyclerView.Adapter<StatusUpdateAdapter.MyHolder> {

    Context context;
    List<StatusUpdateModel> listdata;
    long DURATION = 200;

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

        //fetch post User Details
        postUserDetails.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
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

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //set post details
        holder.userUpdate.setText(statusUpdateModel.getStatus());

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
        postDetails.child("likes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    int totalLikes = (int) dataSnapshot.getChildrenCount();
                    holder.likesTotal.setText("" + totalLikes);

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
        postDetails.child("comments").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    int totalComments = (int) dataSnapshot.getChildrenCount();
                    holder.commentsTotal.setText("" + totalComments);
                } catch (Exception e){}
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        holder.profileName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!myPhone.equals(statusUpdateModel.getAuthor())){
                    Intent slideactivity = new Intent(context, ViewProfile.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

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
                slideactivity.putExtra("imageURL", postUser[position].getProfilePic());
                context.startActivity(slideactivity);
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
                            holder.likePost.setTag(R.drawable.liked);
                            holder.likePost.setImageResource(R.drawable.liked);
                            //Toast.makeText(context,restaurantDetails.getName()+" added to favourites",Toast.LENGTH_SHORT).show();
                        }
                    });


                } else{
                    //Remove from my favourites
                    postDetails.child("likes").child(myPhone).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            holder.likePost.setTag(R.drawable.unliked);
                            holder.likePost.setImageResource(R.drawable.unliked);
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
                Bundle bndlanimation =
                        ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                context.startActivity(slideactivity, bndlanimation);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listdata.size();
    }

    class MyHolder extends RecyclerView.ViewHolder{
        TextView profileName, userUpdate, likesTotal, commentsTotal, timePosted;
        ImageView profilePic, deleteBtn, likePost, comments, sharePost;
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
                        //I (logged in user) can delete any post on my wall as i wish
                        final AlertDialog deletePost = new AlertDialog.Builder(v.getContext())
                                //set message, title, and icon
                                .setMessage("Delete post?")
                                //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                                //set three option buttons
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        DatabaseReference postDetails = FirebaseDatabase.getInstance().getReference("posts/"+listdata.get(getAdapterPosition()).getPostedTo()+"/"+listdata.get(getAdapterPosition()).key);
                                        postDetails.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                listdata.remove(getAdapterPosition());
                                                notifyItemRemoved(getAdapterPosition());
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
