package com.malcolmmaima.dishi.View.Adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.CalculateDistance;
import com.malcolmmaima.dishi.Model.LiveLocation;
import com.malcolmmaima.dishi.Model.StaticLocation;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.ViewCustomerOrder;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.malcolmmaima.dishi.View.Activities.ViewProfile;
import com.squareup.picasso.Picasso;

import java.util.List;

import io.fabric.sdk.android.services.common.SafeToast;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.MyHolder>{
    Context context;
    List<UserModel> listdata;
    long DURATION = 200;
    LiveLocation liveLocation, customerLive;
    StaticLocation customerStatic;
    Double dist;
    DatabaseReference restaurantUserDetails, myLocationRef, customerLiveLocationRef, customerStaticLocationRef;
    ValueEventListener myLocationRefListener, customerLiveLocationListener,customerStaticLocationListener;
    UserModel restaurant;
    ChildEventListener myRideOrderRequestsChildListener;
    DatabaseReference myRideOrderRequests;

    public OrdersAdapter(Context context, List<UserModel> listdata) {
        this.listdata = listdata;
        this.context = context;
    }


    @Override
    public OrdersAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_order,parent,false);

        OrdersAdapter.MyHolder myHolder = new OrdersAdapter.MyHolder(view);
        return myHolder;
    }

    public void onBindViewHolder(final OrdersAdapter.MyHolder holder, final int position) {
        final UserModel orderDetails = listdata.get(position);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final String myPhone = user.getPhoneNumber(); //Current logged in user phone number
        restaurantUserDetails = FirebaseDatabase.getInstance().getReference("users/"+orderDetails.restaurantPhone);
        myLocationRef = FirebaseDatabase.getInstance().getReference("location/"+myPhone);
        customerLiveLocationRef = FirebaseDatabase.getInstance().getReference("location/"+orderDetails.getPhone());
        customerStaticLocationRef = FirebaseDatabase.getInstance().getReference("orders/"+orderDetails.restaurantPhone+"/"+orderDetails.getPhone()+"/static_address");

        if(orderDetails.getAccount_type().equals("3")){
            myRideOrderRequests = FirebaseDatabase.getInstance().getReference("my_ride_requests/"+orderDetails.riderPhone);

            /**
             * Listener to check if there are new ride requests
             */

            myRideOrderRequestsChildListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                    //SafeToast.makeText(context, "Removed " + dataSnapshot.getKey(), Toast.LENGTH_SHORT).show();
                    if(orderDetails.getPhone().equals(dataSnapshot.getKey())){
                        listdata.remove(holder.getLayoutPosition());
                        notifyItemRemoved(holder.getLayoutPosition());
                    }
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            myRideOrderRequests.child(orderDetails.restaurantPhone).addChildEventListener(myRideOrderRequestsChildListener);
        }


        /**
         * On create view fetch my location coordinates
         */

        liveLocation = null;
        myLocationRefListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                liveLocation = dataSnapshot.getValue(LiveLocation.class);
                //SafeToast.makeText(context, "myLocation: " + liveLocation.getLatitude() + "," + liveLocation.getLongitude(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myLocationRef.addValueEventListener(myLocationRefListener);

        /**
         * Now fetch customer order location co-ordinates
         */

        customerLiveLocationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    customerLive = dataSnapshot.getValue(LiveLocation.class);

                    computeDistance(holder, liveLocation.getLatitude(),
                            liveLocation.getLongitude(),
                            customerLive.getLatitude(),
                            customerLive.getLongitude());
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        customerLiveLocationRef.addValueEventListener(customerLiveLocationListener);


        customerStaticLocationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //compute static location coordinates
                if(dataSnapshot.exists()) {
                    try {
                        customerStatic = dataSnapshot.getValue(StaticLocation.class);

                        computeDistance(holder, liveLocation.getLatitude(),
                                liveLocation.getLongitude(),
                                customerStatic.getLatitude(),
                                customerStatic.getLongitude());

                    } catch (Exception e){

                    }
                }
                // compute live location coordindates
                else {
                    try {
                        computeDistance(holder, liveLocation.getLatitude(),
                                liveLocation.getLongitude(),
                                customerLive.getLatitude(),
                                customerLive.getLongitude());
                    } catch (Exception e){

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        customerStaticLocationRef.addValueEventListener(customerStaticLocationListener);

        /**
         * Adapter animation
         */
        setAnimation(holder.itemView, position);

        /**
         * Set widget values
         **/

        holder.customerName.setText(orderDetails.getFirstname() + " " + orderDetails.getLastname());
        holder.orderQty.setText("#"+orderDetails.itemCount);
        holder.distanceAway.setText("loading...");


        if(orderDetails.getAccount_type().equals("3")){
            holder.restaurantIcon.setVisibility(View.VISIBLE);
            holder.restaurantName.setVisibility(View.VISIBLE);

            restaurantUserDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        restaurant = dataSnapshot.getValue(UserModel.class);
                        holder.restaurantName.setText(restaurant.getFirstname() + " " + restaurant.getLastname());
                    } catch (Exception e){
                        holder.restaurantName.setText("Error...");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        else {
            holder.restaurantIcon.setVisibility(View.GONE);
            holder.restaurantName.setVisibility(View.GONE);
        }


        /**
         * Click listener on our card
         */
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(orderDetails.getPhone() != null){

                        Intent slideactivity = new Intent(context, ViewCustomerOrder.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        slideactivity.putExtra("phone", orderDetails.getPhone());
                        slideactivity.putExtra("name", orderDetails.getFirstname() + " " +  orderDetails.getLastname());
                        slideactivity.putExtra("restaurantPhone", orderDetails.restaurantPhone);
                        slideactivity.putExtra("restaurantName", holder.restaurantName.getText());
                        slideactivity.putExtra("accountType", orderDetails.getAccount_type());
                        try {
                            slideactivity.putExtra("restaurantProfile", restaurant.getProfilePic()); //This value will be null for account type 2
                        } catch (Exception e){

                        }
                        Bundle bndlanimation =
                                ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                        context.startActivity(slideactivity, bndlanimation);
                } else {
                    SafeToast.makeText(context, "fetching data...", Toast.LENGTH_SHORT).show();
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
                slideactivity.putExtra("imageURL", orderDetails.getProfilePic());
                context.startActivity(slideactivity);
            }
        });


        /**
         * Load image url onto imageview
         */
        try {
            //Load food image
            Picasso.with(context).load(orderDetails.getProfilePic()).fit().centerCrop()
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(holder.profilePic);
        } catch (Exception e){

        }

    }

    private void computeDistance(MyHolder holder, Double x1, Double y1, Double x2, Double y2) {
        CalculateDistance calculateDistance = new CalculateDistance();
        dist = calculateDistance.distance(x1, y1, x2, y2, "K");
        try {
            if (dist < 1.0) {
                holder.distanceAway.setText(dist * 1000 + "m away");
            } else {
                holder.distanceAway.setText(dist + "km away");

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
        TextView orderQty, customerName,distanceAway, restaurantName;
        ImageView profilePic, restaurantIcon, distanceAwayIcon;
        CardView cardView;

        public MyHolder(View itemView) {
            super(itemView);
            customerName = itemView.findViewById(R.id.customerName);
            orderQty = itemView.findViewById(R.id.orderQty);
            profilePic = itemView.findViewById(R.id.profilePic);
            cardView = itemView.findViewById(R.id.card_view);
            distanceAway = itemView.findViewById(R.id.distanceAway);
            restaurantIcon = itemView.findViewById(R.id.restaurantTag);
            distanceAwayIcon = itemView.findViewById(R.id.locationTag);
            restaurantName = itemView.findViewById(R.id.restaurantName);

//

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
