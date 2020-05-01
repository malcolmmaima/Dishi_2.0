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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.Chat;
import com.malcolmmaima.dishi.View.Activities.ViewCustomerOrder;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.malcolmmaima.dishi.View.Activities.ViewProfile;
import com.squareup.picasso.Picasso;

import java.util.List;

public class FollowerFollowingAdapter extends RecyclerView.Adapter<FollowerFollowingAdapter.MyHolder>{
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

        /**
         * Adapter animation
         */
        setAnimation(holder.itemView, position);

        /**
         * Set widget values
         **/

        holder.customerName.setText(userModel.getFirstname() + " " + userModel.getLastname());

        if(myPhone.equals(userModel.getPhone())){
            holder.followUnfollow.setVisibility(View.GONE);
        }

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
                DatabaseReference followerRef = FirebaseDatabase.getInstance().getReference("followers/"+userModel.getPhone()+"/"+myPhone);
                followingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(!dataSnapshot.exists()){
                            followingRef.setValue("follow").addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    followerRef.setValue("follow").addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            holder.followUnfollow.getBackground().setColorFilter(context.getResources().getColor(R.color.colorPrimaryDark), PorterDuff.Mode.MULTIPLY);
                                            holder.followUnfollow.setText("UNFOLLOW");
                                        }
                                    });

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    holder.followUnfollow.getBackground().setColorFilter(context.getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
                                    holder.followUnfollow.setText("FOLLOW");
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
                Intent slideactivity = new Intent(context, ViewImage.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                slideactivity.putExtra("imageURL", userModel.getProfilePic());
                context.startActivity(slideactivity);
            }
        });


        /**
         * Load image url onto imageview
         */
        try {
            //Load food image
            Picasso.with(context).load(userModel.getProfilePic()).fit().centerCrop()
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(holder.profilePic);
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
        TextView customerName;
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
