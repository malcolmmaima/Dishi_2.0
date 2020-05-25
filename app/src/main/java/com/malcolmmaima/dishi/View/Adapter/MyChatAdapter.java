package com.malcolmmaima.dishi.View.Adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.MessageModel;
import com.malcolmmaima.dishi.R;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class MyChatAdapter extends BaseAdapter {
    DatabaseReference myMessageRef, senderMessageRef;
    Activity activity;
    ArrayList<MessageModel> data;
    FirebaseUser user;
    String myPhone;
    long DURATION = 200;
    String TAG = "MyChatAdapter";

    public MyChatAdapter(Activity activity, ArrayList<MessageModel> data)
    {
        this.activity=activity;
        this.data=data;
    }
    @Override
    public int getCount() {
        return data.size();
    }
    @Override
    public Object getItem(int position) {
        return data.get(position);
    }
    @Override
    public long getItemId(int position) {
        return 0;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        //notifyDataSetChanged();
        /**
         * if the intended recipient of the message opens it then update message state read = true
         */

        try {
            if (myPhone.equals(data.get(position).getReciever())) {
                senderMessageRef = FirebaseDatabase.getInstance()
                        .getReference("messages/" + data.get(position).getSender() + "/" + myPhone);

                myMessageRef = FirebaseDatabase.getInstance()
                        .getReference("messages/" + myPhone + "/" + data.get(position).getSender());

                myMessageRef.child(data.get(position).getKey()).child("message").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            try {
                                myMessageRef.child(data.get(position).getKey()).child("read").setValue(true);
                            } catch (Exception e){}
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                senderMessageRef.child(data.get(position).getKey()).child("message").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            try {
                                senderMessageRef.child(data.get(position).getKey()).child("read").setValue(true);
                            } catch (Exception e){
                                Log.e(TAG, "onDataChange: ", e);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }
        } catch (Exception e){
            Log.e(TAG, "getView: ", e);
        }


        View view=convertView;

        try {
            if (!data.get(position).getSender().equals(myPhone)) {
                view = activity.getLayoutInflater().inflate(R.layout.chat_row_left, null);
            } else {
                view = activity.getLayoutInflater().inflate(R.layout.chat_row_right, null);

                ImageView readReceipt = view.findViewById(R.id.readReceipt);
                if(data.get(position).getRead() == true){
                    readReceipt.setImageResource(R.drawable.ic_done_all_black_48dp);
                } else {
                    readReceipt.setImageResource(R.drawable.ic_done_black_48dp);
                }
            }

            TextView text = view.findViewById(R.id.text);

            try {
                Linkify.addLinks(text, Linkify.ALL);
            } catch(Exception e){
                Log.e(TAG, "ChatLink: ", e);
            }
            text.setText(data.get(position).getMessage());
            TextView timeStamp = view.findViewById(R.id.timeStamp);

            //2020-04-03:23:22:00:GMT+03:00
            String[] timeS = Split(data.get(position).getTimeStamp());

            /**
             * timeS[0] = date
             * timeS[1] = hr
             * timeS[2] = min
             * timeS[3] = seconds
             * timeS[4] = timezone
             */
            timeStamp.setText(timeS[1]+":"+timeS[2]);
        } catch (Exception e){
            Log.e(TAG, "getView: ", e);
        }

        return view;
    }


    public String[] Split(String timeStamp){

        String[] arrSplit = timeStamp.split(":");
//        for (int i=0; i < arrSplit.length; i++)
//        {
//            //Toast.makeText(activity, "val: " + arrSplit[1i, Toast.LENGTH_SHORT).show();
//        }

        return arrSplit;
    }

}

