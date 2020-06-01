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
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishi.Controller.Fonts.MyTextView_Roboto_Regular;
import com.malcolmmaima.dishi.Controller.Utils.TimeAgo;
import com.malcolmmaima.dishi.Model.ProductDetailsModel;
import com.malcolmmaima.dishi.Model.ReceiptModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.AddMenu;
import com.malcolmmaima.dishi.View.Activities.ReceiptActivity;
import com.malcolmmaima.dishi.View.Activities.ReportAbuse;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.fabric.sdk.android.services.common.SafeToast;


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

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String myPhone = user.getPhoneNumber(); //Current logged in user phone number

        DatabaseReference myUserDetailsRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
        myUserDetailsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserModel myDetails = dataSnapshot.getValue(UserModel.class);
                if(myDetails.getAccount_type().equals("1")){
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
                }

                if(myDetails.getAccount_type().equals("2") || myDetails.getAccount_type().equals("3")){
                    DatabaseReference restaurantDetails = FirebaseDatabase.getInstance().getReference("users/"+receipt.getCustomer());
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
            //Log.d(TAG, "timeStamp: "+ e.getMessage());
        }
        /**
         * Click listener on our card
         */
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent slideactivity = new Intent(context, ReceiptActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    slideactivity.putExtra("key", receipt.key);
                    slideactivity.putExtra("restaurantName", holder.restaurantName.getText().toString());
                    slideactivity.putExtra("restaurantPhone", receipt.getRestaurant());
                    slideactivity.putExtra("customerPhone", receipt.getCustomer());
                    slideactivity.putExtra("orderID", receipt.getOrderID());
                    slideactivity.putExtra("deliveryCharge", receipt.getDeliveryCharge());
                    slideactivity.putExtra("orderOn", receipt.getInitiatedOn());
                    slideactivity.putExtra("deliveredOn", receipt.getDeliveredOn());
                    Bundle bndlanimation =
                            ActivityOptions.makeCustomAnimation(context, R.anim.animation, R.anim.animation2).toBundle();
                    context.startActivity(slideactivity, bndlanimation);
                } catch (Exception e){
                    Log.e(TAG, "onClick: ", e);
                }
            }
        });

        //creating a popup menu
        PopupMenu popup = new PopupMenu(context, holder.receiptOptions);
        //inflating menu from xml resource
        popup.inflate(R.menu.receipt_options_menu);

        Menu myMenu = popup.getMenu();
        MenuItem deleteOption = myMenu.findItem(R.id.delete);
        MenuItem downloadOption = myMenu.findItem(R.id.download);

        try {
            deleteOption.setVisible(true);
            downloadOption.setVisible(true);

        } catch (Exception e){
            Log.e(TAG, "onBindViewHolder: ",e);
        }

        holder.receiptOptions.setOnClickListener(new View.OnClickListener() {
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
                                        .setMessage("Delete receipt?")
                                        //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                                        //set three option buttons
                                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                DatabaseReference receiptRef = FirebaseDatabase.getInstance().getReference("receipts/"+myPhone+"/"+receipt.key);
                                                receiptRef.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        try {
                                                            listdata.remove(position);
                                                            notifyItemRemoved(position);
                                                        } catch (Exception e){
                                                            Log.e(TAG, "error ", e);
                                                        }
                                                        SafeToast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show();
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
                            case R.id.download:
                                final AlertDialog reportStatus = new AlertDialog.Builder(context)
                                        //set message, title, and icon
                                        .setMessage("Download this receipt?")
                                        //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                                        //set three option buttons
                                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                try {
                                                    Intent slideactivity = new Intent(context, ReceiptActivity.class)
                                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                                    slideactivity.putExtra("key", receipt.key);
                                                    slideactivity.putExtra("restaurantName", holder.restaurantName.getText().toString());
                                                    slideactivity.putExtra("restaurantPhone", receipt.getRestaurant());
                                                    slideactivity.putExtra("customerPhone", receipt.getCustomer());
                                                    slideactivity.putExtra("orderID", receipt.getOrderID());
                                                    slideactivity.putExtra("orderOn", receipt.getInitiatedOn());
                                                    slideactivity.putExtra("deliveredOn", receipt.getDeliveredOn());
                                                    slideactivity.putExtra("deliveryCharge", receipt.getDeliveryCharge());
                                                    slideactivity.putExtra("downloadRequest", true);
                                                    Bundle bndlanimation =
                                                            ActivityOptions.makeCustomAnimation(context, R.anim.animation, R.anim.animation2).toBundle();
                                                    context.startActivity(slideactivity, bndlanimation);
                                                } catch (Exception e){
                                                    Log.e(TAG, "onClick: ", e);
                                                }
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
        MyTextView_Roboto_Medium restaurantName;
        MyTextView_Roboto_Regular orderedOn;
        MyTextView_Roboto_Medium orderID;
        TextView receiptOptions;
        CardView cardView;

        public MyHolder(View itemView) {
            super(itemView);
            restaurantName = itemView.findViewById(R.id.restaurantName);
            orderID = itemView.findViewById(R.id.orderID);
            orderedOn = itemView.findViewById(R.id.orderedOn);
            receiptOptions = itemView.findViewById(R.id.receiptOptions);
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