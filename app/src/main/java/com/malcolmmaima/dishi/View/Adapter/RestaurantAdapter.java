package com.malcolmmaima.dishi.View.Adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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
import com.squareup.picasso.Picasso;

import java.util.List;

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.MyHolder> {

    Context context;
    List<UserModel> listdata;
    long DURATION = 200;
    private boolean on_attach = true;

    public RestaurantAdapter(Context context, List<UserModel> listdata) {
        this.listdata = listdata;
        this.context = context;
    }

    @Override
    public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_restaurant,parent,false);

        MyHolder myHolder = new MyHolder(view);
        return myHolder;
    }


    public void onBindViewHolder(final MyHolder holder, final int position) {
        final UserModel restaurantDetails = listdata.get(position);

        /**
         * Adapter animation
         */
        setAnimation(holder.itemView, position);

        holder.restaurantName.setText(restaurantDetails.getFirstname() + " " + restaurantDetails.getLastname());
        holder.likeImageView.setTag(R.drawable.ic_like);

        final DatabaseReference myFavourites, restaurantRef;
        FirebaseDatabase db;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final String myPhone = user.getPhoneNumber(); //Current logged in user phone number

        // Assign FirebaseStorage instance to storageReference.

        db = FirebaseDatabase.getInstance();
        restaurantRef = db.getReference( "restaurant_favourites/"+ restaurantDetails.getPhone());
        myFavourites = db.getReference("my_favourites/"+myPhone);

        /**
         * Load image url onto imageview
         */
        try {
            //Load food image
            Picasso.with(context).load(restaurantDetails.getProfilePic()).fit().centerCrop()
                    //.placeholder(R.drawable.shop)
                    .error(R.drawable.shop)
                    .into(holder.profilePic);
        } catch (Exception e){

        }


        if(restaurantDetails.getDistance() < 1.0){
            holder.distAway.setText(restaurantDetails.getDistance()*1000 + "m away");
        } else {
            holder.distAway.setText(restaurantDetails.getDistance() + "km away");

        }

        //On laoding adapter fetch the like status
        myFavourites.child(restaurantDetails.getPhone()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String phone = dataSnapshot.getValue(String.class);
                try {
                    if (phone.equals("fav")) {
                        holder.likeImageView.setTag(R.drawable.ic_liked);
                        holder.likeImageView.setImageResource(R.drawable.ic_liked);
                    } else {
                        holder.likeImageView.setTag(R.drawable.ic_like);
                        holder.likeImageView.setImageResource(R.drawable.ic_like);
                    }
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        restaurantRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    int likesTotal = (int) dataSnapshot.getChildrenCount();
                    holder.likes.setText(""+likesTotal);
                } catch (Exception e){

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        holder.likeImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int id = (int)holder.likeImageView.getTag();
                if( id == R.drawable.ic_like){
                    //Add to my favourites
                    myFavourites.child(restaurantDetails.getPhone()).setValue("fav").addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            holder.likeImageView.setTag(R.drawable.ic_liked);
                            holder.likeImageView.setImageResource(R.drawable.ic_liked);

                            //Add to global restaurant likes
                            restaurantRef.child(myPhone).setValue("fav").addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //Add favourite to restaurant's node as well
                                }
                            });
                            //Toast.makeText(context,restaurantDetails.getName()+" added to favourites",Toast.LENGTH_SHORT).show();
                        }
                    });


                } else{
                    //Remove from my favourites
                    myFavourites.child(restaurantDetails.getPhone()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            holder.likeImageView.setTag(R.drawable.ic_like);
                            holder.likeImageView.setImageResource(R.drawable.ic_like);

                            restaurantRef.child(myPhone).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //remove favourite from restaurant's node as well
                                }
                            });
                            //Toast.makeText(context,restaurantDetails.getName()+" removed from favourites",Toast.LENGTH_SHORT).show();
                        }
                    });

                }

            }
        });

        holder.callRestaurant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog callAlert = new AlertDialog.Builder(v.getContext())
                        //set message, title, and icon
                        .setMessage("Call " + restaurantDetails.getFirstname() + " " + restaurantDetails.getLastname() + "?")
                        //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                        //set three option buttons
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String phone = restaurantDetails.getPhone();
                                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
                                context.startActivity(intent);
                            }
                        }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //do nothing

                            }
                        })//setNegativeButton

                        .create();
                callAlert.show();
            }
        });

        holder.profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(context, "Slide to intent", Toast.LENGTH_SHORT).show();

//                if(restaurantDetails.getPhone() != null){
//                    //Slide to new activity
//                    Intent slideactivity = new Intent(context, ViewRestaurant.class)
//                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
//                    slideactivity.putExtra("restaurant_phone", restaurantDetails.getPhone());
//                    Bundle bndlanimation =
//                            ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
//                    context.startActivity(slideactivity, bndlanimation);
//                } else {
//                    Toast.makeText(context, "Error fetching details, try again!", Toast.LENGTH_SHORT).show();
//                }

            }
        });



        holder.shareImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Share!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * @https://medium.com/better-programming/android-recyclerview-with-beautiful-animations-5e9b34dbb0fa
     */
    private void setAnimation(View itemView, int i) {
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


    @Override
    public int getItemCount() {
        return listdata.size();
    }

    class MyHolder extends RecyclerView.ViewHolder{
        TextView restaurantName, distAway, likes;
        ImageView profilePic, likeImageView, shareImageView, callRestaurant;

        public MyHolder(View itemView) {
            super(itemView);

            likeImageView = itemView.findViewById(R.id.likeImageView);
            shareImageView = itemView.findViewById(R.id.shareImageView);
            profilePic = itemView.findViewById(R.id.coverImageView);
            restaurantName = itemView.findViewById(R.id.titleTextView);
            distAway = itemView.findViewById(R.id.distanceAway);
            callRestaurant = itemView.findViewById(R.id.callRestaurant);
            likes = itemView.findViewById(R.id.likesTotal);

        }
    }
}
