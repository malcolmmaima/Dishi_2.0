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
        time.setText("11:53 P.M.");
        message.setText("Some message");

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
}
