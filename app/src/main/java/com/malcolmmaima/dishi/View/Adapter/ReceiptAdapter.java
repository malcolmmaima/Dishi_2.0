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

import androidx.annotation.NonNull;
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
import com.malcolmmaima.dishi.Controller.Utils.TimeAgo;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.Model.ReceiptModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.AddMenu;
import com.malcolmmaima.dishi.View.Activities.ReceiptActivity;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class ReceiptAdapter extends RecyclerView.Adapter<ReceiptAdapter.MyHolder>{

    String TAG = "ReceiptAdapter";
    Context context;
    List<ReceiptModel> listdata;
    long DURATION = 200;

    public ReceiptAdapter(Context context, List<ReceiptModel> listdata) {
        this.listdata = listdata;
        this.context = context;
    }


    @Override
    public ReceiptAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_receipt,parent,false);

        ReceiptAdapter.MyHolder myHolder = new ReceiptAdapter.MyHolder(view);
        return myHolder;
    }

    public void onBindViewHolder(final ReceiptAdapter.MyHolder holder, final int position) {
        final ReceiptModel receipt = listdata.get(position);
        DatabaseReference restaurantDetails = FirebaseDatabase.getInstance().getReference("users/"+receipt.getRestaurant());
        restaurantDetails.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    UserModel restaurant = dataSnapshot.getValue(UserModel.class);
                    holder.restaurantName.setText(restaurant.getFirstname() + " " + restaurant.getLastname());
                } catch (Exception e){
                    Log.e(TAG, "onDataChange: ", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        setAnimation(holder.itemView, position);

        holder.orderID.setText("#"+receipt.getOrderID());
        holder.orderedOn.setText(receipt.getInitiatedOn());

        //Get date status update was posted
        String dtEnd = receipt.getDeliveredOn();
        String dtStart = receipt.getInitiatedOn();

        //https://stackoverflow.com/questions/8573250/android-how-can-i-convert-string-to-date
        //Format both current date and date status update was posted
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss:Z");
        try {
            //Convert String date values to Date values
            Date dateStart;
            Date dateEnd;

            dateEnd = format.parse(dtEnd);

            //https://memorynotfound.com/calculate-relative-time-time-ago-java/
            //Now compute timeAgo duration
            TimeAgo timeAgo = new TimeAgo();

            holder.orderedOn.setText(""+dateEnd);

        } catch (ParseException e) {
            e.printStackTrace();
            Log.d(TAG, "timeStamp: "+ e.getMessage());
        }
        /**
         * Click listener on our card
         */
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent slideactivity = new Intent(context, ReceiptActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("key", receipt.key);
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(context, R.anim.animation, R.anim.animation2).toBundle();
                    context.startActivity(slideactivity, bndlanimation);
                } catch (Exception e){
                    Log.e(TAG, "onClick: ", e);
                }
            }
        });

    }

    /**
     * @lhttps://medium.com/better-programming/android-recyclerview-with-beautiful-animations-5e9b34dbb0fa
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

    @Override
    public int getItemCount() {
        return listdata.size();
    }

    class MyHolder extends RecyclerView.ViewHolder{
        TextView restaurantName , orderID, orderedOn;
        CardView cardView;

        public MyHolder(View itemView) {
            super(itemView);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            orderID = itemView.findViewById(R.id.orderID);
            orderedOn = itemView.findViewById(R.id.orderedOn);
            cardView = itemView.findViewById(R.id.card_view);

            //Long Press
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(final View v) {
                   //do something
                    return false;
                }
            });

        }
    }

    public String[] Split(String timeStamp){

        String[] arrSplit = timeStamp.split(":");

        return arrSplit;
    }


}