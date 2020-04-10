package com.malcolmmaima.dishi.View.Adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;

import com.alexzh.circleimageview.CircleImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.MessageModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ChatListAdapter extends BaseAdapter {
    Activity activity;
    ArrayList<UserModel> data;
    Context context;
    long DURATION = 200;
    DatabaseReference messageRef;
    String myPhone;
    FirebaseUser user;
    int unreadCount = 0;

    public ChatListAdapter(Activity activity, ArrayList<UserModel> data, Context context)
    {
        this.activity=activity;
        this.data=data;
        this.context = context;
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
        View view=convertView;
        view=activity.getLayoutInflater().inflate(R.layout.chat_list, null);

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        messageRef = FirebaseDatabase.getInstance().getReference("messages/"+myPhone+"/"+data.get(position).getPhone());

        /**
         * Adapter animation
         */
        setAnimation(view, position);

        TextView name=view.findViewById(R.id.contact_name);
        TextView time=view.findViewById(R.id.message_time);
        TextView unread=view.findViewById(R.id.unreadCount);
        TextView message=view.findViewById(R.id.message);
        CircleImageView picture =  view.findViewById(R.id.user_dp);
        ImageView mute = view.findViewById(R.id.mute);
        name.setText(data.get(position).getFirstname()+" "+data.get(position).getLastname());

        /**
         * unread count for messages
         */
        messageRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                unreadCount = 0;
                for(DataSnapshot messages : dataSnapshot.getChildren()){
                    try {
                        MessageModel message_ = messages.getValue(MessageModel.class);
                        /**
                         * Check to see if message from sender to me has been marked as read
                         */
                        if (message_.getSender().equals(data.get(position).getPhone()) && message_.getRead() != true) {
                            unreadCount++;
                            unread.setVisibility(View.VISIBLE);
                            unread.setText("" + unreadCount);

                            if(message_.getMessage().length() > 35) {
                                message.setText(message_.getMessage().substring(0, 35) + "...");
                            } else {
                                message.setText(message_.getMessage());
                            }
                        } else {
                            unread.setVisibility(View.GONE);
                        }
                    } catch (Exception e){}


                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //2020-04-03:23:22:00:GMT+03:00
        try {
            String[] timeS = Split(data.get(position).timeStamp);

            /**
             * timeS[0] = date
             * timeS[1] = hr
             * timeS[2] = min
             * timeS[3] = seconds
             * timeS[4] = timezone
             */
            time.setText(timeS[1]+":"+timeS[2]);

            if(data.get(position).message.length() > 35) {
                message.setText(data.get(position).message.substring(0, 35) + "...");
            } else {
                message.setText(data.get(position).message);
            }


        } catch (Exception e){}



        try {
            //Load food image
            Picasso.with(context).load(data.get(position).getProfilePic()).fit().centerCrop()
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(picture);
        } catch (Exception e){

        }
        picture.setOnClickListener(v -> {
            Intent slideactivity = new Intent(context, ViewImage.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            slideactivity.putExtra("imageURL", data.get(position).getProfilePic());
            context.startActivity(slideactivity);
        });

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
