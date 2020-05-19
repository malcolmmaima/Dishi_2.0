package com.malcolmmaima.dishi.View.Adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

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
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.PersonalDetails;
import com.malcolmmaima.dishi.View.Activities.ViewCustomerOrder;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.malcolmmaima.dishi.View.Activities.ViewRestaurant;
import com.squareup.picasso.Picasso;

import java.util.List;

public class RestaurantRiderRequestAdapter extends RecyclerView.Adapter<RestaurantRiderRequestAdapter.MyHolder>{
    Context context;
    List<UserModel> listdata;
    long DURATION = 200;
    private String [] restaurantActions = {"Accept", "Decline", "View"};
    private String [] restaurantActions2 = {"Remove", "View"};
    DatabaseReference restaurantRidersRef, myRestaurantsRef;
    String myPhone;
    FirebaseUser user;

    public RestaurantRiderRequestAdapter(Context context, List<UserModel> listdata) {
        this.listdata = listdata;
        this.context = context;
    }


    @Override
    public RestaurantRiderRequestAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_restaurant_rider_request,parent,false);

        RestaurantRiderRequestAdapter.MyHolder myHolder = new RestaurantRiderRequestAdapter.MyHolder(view);
        return myHolder;
    }

    public void onBindViewHolder(final RestaurantRiderRequestAdapter.MyHolder holder, final int position) {
        final UserModel restaurantDetails = listdata.get(position);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        restaurantRidersRef = FirebaseDatabase.getInstance().getReference("my_riders/"+restaurantDetails.getPhone()+"/"+myPhone);
        myRestaurantsRef = FirebaseDatabase.getInstance().getReference("my_restaurants/"+myPhone+"/"+restaurantDetails.getPhone());

        /**
         * Adapter animation
         */
        setAnimation(holder.itemView, position);

        /**
         * Set widget values
         **/

        holder.customerName.setText(restaurantDetails.getFirstname() + " " + restaurantDetails.getLastname());
        holder.distanceAway.setText("loading...");


        /**
         * Click listener on our card
         */
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                restaurantRidersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle(restaurantDetails.getFirstname()+" "+restaurantDetails.getLastname());
                            builder.setItems(restaurantActions2, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //Accept rider request
                                    if(which == 0){
                                        //Update the respective nodes
                                        restaurantRidersRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                myRestaurantsRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        try {
                                                            listdata.remove(position);
                                                            notifyItemRemoved(position);
                                                            Snackbar snackbar = Snackbar.make(v.getRootView(), "Removed Successfully", Snackbar.LENGTH_LONG);
                                                            snackbar.show();
                                                        } catch (Exception e){}
                                                    }
                                                });
                                            }
                                        });

                                    }
                                    if(which == 1){
                                        Intent slideactivity = new Intent(context, ViewRestaurant.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                        slideactivity.putExtra("restaurant_phone", restaurantDetails.getPhone());
                                        slideactivity.putExtra("distance", 0.0); //pass default value, distance will be computed in the Viewrestaurant activity
                                        slideactivity.putExtra("profilePic", restaurantDetails.getProfilePic());
                                        Bundle bndlanimation =
                                                null;
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                            bndlanimation = ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                                        }
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                            context.startActivity(slideactivity, bndlanimation);
                                        }
                                    }

                                }
                            });
                            builder.create();
                            builder.show();
                        }

                        else {
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setTitle(restaurantDetails.getFirstname()+" "+restaurantDetails.getLastname());
                            builder.setItems(restaurantActions, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    //Accept rider request
                                    if(which == 0){
                                        //Update the respective nodes
                                        restaurantRidersRef.setValue("inactive").addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                myRestaurantsRef.setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Snackbar snackbar = Snackbar.make(v.getRootView(), "Request Accepted", Snackbar.LENGTH_LONG);
                                                        snackbar.show();
                                                    }
                                                });

                                            }
                                        });
                                    }
                                    if(which == 1){
                                        myRestaurantsRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                try {
                                                    listdata.remove(position);
                                                    notifyItemRemoved(position);
                                                    Snackbar snackbar = Snackbar.make(v.getRootView(), "Request Declined", Snackbar.LENGTH_LONG);
                                                    snackbar.show();
                                                } catch (Exception e){}
                                            }
                                        });
                                    }

                                    if(which == 2){
                                        Snackbar snackbar = Snackbar.make(v.getRootView(), "In development", Snackbar.LENGTH_LONG);
                                        snackbar.show();
                                    }
                                }
                            });
                            builder.create();
                            builder.show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }
        });


        /**
         * View image click listener
         */
        holder.profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(restaurantDetails.getProfilePicBig() != null){
                    Intent slideactivity = new Intent(context, ViewImage.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("imageURL", restaurantDetails.getProfilePicBig());
                    context.startActivity(slideactivity);
                }

                else {
                    Intent slideactivity = new Intent(context, ViewImage.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("imageURL", restaurantDetails.getProfilePic());
                    context.startActivity(slideactivity);
                }
            }
        });


        /**
         * Load image url onto imageview
         */
        try {
            //Load image
            if(restaurantDetails.getProfilePicSmall() != null){
                Picasso.with(context).load(restaurantDetails.getProfilePicSmall()).fit().centerCrop()
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(holder.profilePic);
            }

            else {
                Picasso.with(context).load(restaurantDetails.getProfilePic()).fit().centerCrop()
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
        TextView orderQty,distanceAway;
        ImageView profilePic;
        CardView cardView;

        public MyHolder(View itemView) {
            super(itemView);
            customerName = itemView.findViewById(R.id.customerName);
            orderQty = itemView.findViewById(R.id.orderQty);
            profilePic = itemView.findViewById(R.id.profilePic);
            cardView = itemView.findViewById(R.id.card_view);
            distanceAway = itemView.findViewById(R.id.distanceAway);

//            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//            final String myPhone = user.getPhoneNumber(); //Current logged in user phone number

            //Long Press
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    return false;
                }
            });

        }
    }

}
