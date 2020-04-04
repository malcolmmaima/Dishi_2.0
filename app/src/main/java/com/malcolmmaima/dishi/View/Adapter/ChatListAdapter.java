package com.malcolmmaima.dishi.View.Adapter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
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
        TextView name=(TextView)view.findViewById(R.id.contact_name);
        TextView time=(TextView)view.findViewById(R.id.message_time);
        TextView message=(TextView)view.findViewById(R.id.message);
        ImageView picture = (ImageView) view.findViewById(R.id.user_dp);
        ImageView mute = (ImageView) view.findViewById(R.id.mute);
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
        picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialog = new Dialog(context);
                dialog.setTitle("");
                dialog.setContentView(R.layout.image_click);
                dialog.show();
            }
        });

        return view;
    }
}
