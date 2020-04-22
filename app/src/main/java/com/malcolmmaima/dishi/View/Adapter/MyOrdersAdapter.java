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
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.malcolmmaima.dishi.View.Activities.ViewMyOrders;
import com.squareup.picasso.Picasso;

import java.util.List;

import io.fabric.sdk.android.services.common.SafeToast;

public class MyOrdersAdapter extends RecyclerView.Adapter<MyOrdersAdapter.MyHolder>{
    Context context;
    List<UserModel> listdata;
    DatabaseReference orderRef;
    String myPhone;
    FirebaseUser user;
    long DURATION = 200;

    public MyOrdersAdapter(Context context, List<UserModel> listdata) {
        this.listdata = listdata;
        this.context = context;
    }


    @Override
    public MyOrdersAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.my_order_card,parent,false);

        MyOrdersAdapter.MyHolder myHolder = new MyOrdersAdapter.MyHolder(view);
        return myHolder;
    }

    public void onBindViewHolder(final MyOrdersAdapter.MyHolder holder, final int position) {
        final UserModel orderDetails = listdata.get(position);
        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        orderRef = FirebaseDatabase.getInstance().getReference("my_orders/"+myPhone+"/"+orderDetails.getPhone());

        orderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){

                    try {
                        listdata.remove(holder.getAdapterPosition());
                        notifyItemRemoved(holder.getAdapterPosition());
                        SafeToast.makeText(context, orderDetails.getFirstname() +" "+ orderDetails.getLastname() + " order complete!", Toast.LENGTH_LONG).show();
                    } catch (Exception e){}
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

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


        /**
         * Click listener on our card
         */
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent slideactivity = new Intent(context, ViewMyOrders.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                slideactivity.putExtra("phone", orderDetails.getPhone());
                slideactivity.putExtra("name", orderDetails.getFirstname() + " " +  orderDetails.getLastname());
                Bundle bndlanimation =
                        ActivityOptions.makeCustomAnimation(context, R.anim.animation,R.anim.animation2).toBundle();
                context.startActivity(slideactivity, bndlanimation);
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
        TextView orderQty, customerName,distanceAway;
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
