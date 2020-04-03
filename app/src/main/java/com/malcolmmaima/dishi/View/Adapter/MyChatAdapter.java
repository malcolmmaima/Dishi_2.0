package com.malcolmmaima.dishi.View.Adapter;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.malcolmmaima.dishi.Model.MessageModel;
import com.malcolmmaima.dishi.R;

import java.util.ArrayList;

public class MyChatAdapter extends BaseAdapter {
    Activity activity;
    ArrayList<MessageModel> data;
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
        View view=convertView;
        if(position%2==0)
        {
            view=activity.getLayoutInflater().inflate(R.layout.chat_row_left, null);
        }
        else
        {
            view=activity.getLayoutInflater().inflate(R.layout.chat_row_right, null);
        }
        TextView text=(TextView)view.findViewById(R.id.text);
        text.setText(data.get(position).toString());

        return view;
    }
}

