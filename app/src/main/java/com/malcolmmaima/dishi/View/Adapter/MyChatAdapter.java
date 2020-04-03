package com.malcolmmaima.dishi.View.Adapter;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

        /**
         * if the intended recipient of the message opens it then update message state read = true
         */
        if(myPhone.equals(data.get(position).getReciever())){
            senderMessageRef = FirebaseDatabase.getInstance()
                    .getReference("messages/"+data.get(position).getSender()+"/"+myPhone);

            myMessageRef = FirebaseDatabase.getInstance()
                    .getReference("messages/"+myPhone+"/"+data.get(position).getSender());

            try {
                myMessageRef.child(data.get(position).getKey()).child("read").setValue(true);
                senderMessageRef.child(data.get(position).getKey()).child("read").setValue(true);
            } catch (Exception e){}
        }


        View view=convertView;
        if(!data.get(position).getSender().equals(myPhone))
        {
            view=activity.getLayoutInflater().inflate(R.layout.chat_row_left, null);
        }
        else
        {
            view=activity.getLayoutInflater().inflate(R.layout.chat_row_right, null);
        }
        TextView text = view.findViewById(R.id.text);
        TextView timeStamp = view.findViewById(R.id.timeStamp);

        text.setText(data.get(position).getMessage());

        //2020-04-03:23:22:00:GMT+03:00
        String[] timeS = Split(data.get(position).getTimeStamp());

        timeStamp.setText(timeS[1]+":"+timeS[2]);

        return view;
    }

    public String[] Split(String timeStamp){

        String[] arrSplit = timeStamp.split(":");
        for (int i=0; i < arrSplit.length; i++)
        {
            //Toast.makeText(activity, "val: " + arrSplit[1i, Toast.LENGTH_SHORT).show();
        }

        return arrSplit;
    }
}

