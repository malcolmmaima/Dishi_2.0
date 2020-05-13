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
import com.malcolmmaima.dishi.Model.MyDeviceModel;
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


public class MyDeviceAdapter extends RecyclerView.Adapter<MyDeviceAdapter.MyHolder>{

    String TAG = "ReceiptAdapter";
    Context context;
    List<MyDeviceModel> listdata;
    long DURATION = 200;

    public MyDeviceAdapter(Context context, List<MyDeviceModel> listdata) {
        this.listdata = listdata;
        this.context = context;
    }


    @Override
    public MyDeviceAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_my_device,parent,false);

        MyDeviceAdapter.MyHolder myHolder = new MyDeviceAdapter.MyHolder(view);
        return myHolder;
    }

    public void onBindViewHolder(final MyDeviceAdapter.MyHolder holder, final int position) {
        final MyDeviceModel myDevice = listdata.get(position);
        setAnimation(holder.itemView, position);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String myPhone = user.getPhoneNumber(); //Current logged in user phone number

        holder.blockedIcon.setVisibility(View.GONE);

        holder.deviceName.setText(""+myDevice.getDeviceModel());
        holder.deviceID.setText("ID: "+myDevice.getDeviceID());
        holder.deviceIP.setText("IP: "+myDevice.getIpAddress());

        //https://stackoverflow.com/questions/8573250/android-how-can-i-convert-string-to-date
        //Format both current date and date status update was posted
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss:Z");
        try {
            //Convert String date values to Date values
            Date loginDate;
            loginDate = format.parse(myDevice.getLastLogin());
            holder.loginDate.setText("Login: "+loginDate);

        } catch (ParseException e) {
            e.printStackTrace();
            Log.d(TAG, "timeStamp: "+ e.getMessage());
        }

        //creating a popup menu
        PopupMenu popup = new PopupMenu(context, holder.deviceOptions);
        //inflating menu from xml resource
        popup.inflate(R.menu.device_options_menu);

        Menu myMenu = popup.getMenu();
        MenuItem blockOption = myMenu.findItem(R.id.blockDevice);
        MenuItem unblockOption = myMenu.findItem(R.id.unblockDevice);

        try {

            if(myDevice.getBlocked() == true){
                unblockOption.setVisible(true);
                blockOption.setVisible(false);
                holder.blockedIcon.setVisibility(View.VISIBLE);
            }
             else {
                unblockOption.setVisible(false);
                blockOption.setVisible(true);
                holder.blockedIcon.setVisibility(View.GONE);
            }


        } catch (Exception e){
            Log.e(TAG, "onBindViewHolder: ",e);
        }

        holder.deviceOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //adding click listener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.blockDevice:
                                final AlertDialog block = new AlertDialog.Builder(context)
                                        //set message, title, and icon
                                        .setMessage("Block "+myDevice.getDeviceModel()+"?")
                                        //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                                        //set three option buttons
                                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                DatabaseReference myDevicesRef = FirebaseDatabase.getInstance().getReference("mydevices/"+myPhone+"/"+myDevice.getDeviceID());
                                                myDevicesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        if(dataSnapshot.exists()){ //A bit redundant but just to make sure
                                                            myDevicesRef.child("blocked").setValue(true).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void aVoid) {
                                                                    try {
                                                                        holder.blockedIcon.setVisibility(View.VISIBLE);
                                                                        unblockOption.setVisible(true);
                                                                        blockOption.setVisible(false);
                                                                    } catch (Exception e){
                                                                        Log.e(TAG, "onSuccess: ", e);
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                    }
                                                });
                                            }
                                        }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                //do nothing

                                            }
                                        })//setNegativeButton

                                        .create();
                                block.show();
                                return (true);

                            case R.id.unblockDevice:
                                final AlertDialog unBlock = new AlertDialog.Builder(context)
                                        //set message, title, and icon
                                        .setMessage("Unblock "+myDevice.getDeviceModel()+"?")
                                        //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                                        //set three option buttons
                                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                DatabaseReference myDevicesRef = FirebaseDatabase.getInstance().getReference("mydevices/"+myPhone+"/"+myDevice.getDeviceID());
                                                myDevicesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        if(dataSnapshot.exists()){ //A bit redundant but just to make sure
                                                            myDevicesRef.child("blocked").setValue(false).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void aVoid) {
                                                                    try {
                                                                        holder.blockedIcon.setVisibility(View.GONE);
                                                                        unblockOption.setVisible(false);
                                                                        blockOption.setVisible(true);
                                                                    } catch (Exception e){
                                                                        Log.e(TAG, "onSuccess: ", e);
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                    }
                                                });
                                            }
                                        }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                                //do nothing

                                            }
                                        })//setNegativeButton

                                        .create();
                                unBlock.show();
                                return (true);
                            default:
                                return false;
                        }
                    }
                });
                //displaying the popup
                popup.show();

            }
        });

        /**
         * Click listener on our card
         */
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
        MyTextView_Roboto_Medium deviceName;
        MyTextView_Roboto_Regular deviceID, deviceIP, loginDate;
        ImageView blockedIcon;
        TextView deviceOptions;
        CardView cardView;

        public MyHolder(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceID = itemView.findViewById(R.id.deviceID);
            deviceIP = itemView.findViewById(R.id.deviceIP);
            loginDate = itemView.findViewById(R.id.deviceLoginDate);
            blockedIcon = itemView.findViewById(R.id.blockedIcon);
            deviceOptions = itemView.findViewById(R.id.deviceOptions);
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