package com.malcolmmaima.dishi.View.Adapter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import com.alexzh.circleimageview.CircleImageView;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Activities.ViewImage;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ChatListAdapter extends BaseAdapter {
    Activity activity;
    ArrayList<UserModel> data;
    Context context;
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
        TextView name=view.findViewById(R.id.contact_name);
        TextView time=view.findViewById(R.id.message_time);
        TextView message=view.findViewById(R.id.message);
        CircleImageView picture =  view.findViewById(R.id.user_dp);
        ImageView mute = view.findViewById(R.id.mute);
        name.setText(data.get(position).getFirstname()+" "+data.get(position).getLastname());

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
}
