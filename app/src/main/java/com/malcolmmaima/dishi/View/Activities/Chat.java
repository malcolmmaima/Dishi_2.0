package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alexzh.circleimageview.CircleImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.MessageModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.MyChatAdapter;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.fabric.sdk.android.services.common.SafeToast;

public class Chat extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener{

    DatabaseReference recipientRef, myRef, recipientMessagesRef, myMessagedRef;
    ValueEventListener recipientListener, myRefListener, recipientMessagesListener, myMessagesListener;
    UserModel recipientUser, senderUser;
    ArrayList<String> messages;
    EditText editText;
    ListView list;
    MyChatAdapter arrayAdapter;
    TextView name;
    Toolbar toolbar;
    CircleImageView profilePic;
    ImageButton sendBtn;
    int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        String fromPhone = getIntent().getStringExtra("fromPhone");
        String toPhone = getIntent().getStringExtra("toPhone");
        recipientRef = FirebaseDatabase.getInstance().getReference("users/"+toPhone);
        myRef = FirebaseDatabase.getInstance().getReference("users/"+fromPhone);
        recipientMessagesRef = FirebaseDatabase.getInstance().getReference("messages/"+toPhone+"/"+fromPhone);
        myMessagedRef = FirebaseDatabase.getInstance().getReference("messages/"+fromPhone+"/"+toPhone);


        toolbar = (Toolbar) findViewById(R.id.chat_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(null);
        toolbar.setTitle(null);
        LayoutInflater mInflater = LayoutInflater.from(this);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        sendBtn = findViewById(R.id.send);
        list= (ListView) findViewById(R.id.list);
        messages = new ArrayList<String>();
        messages.add("hello");
        messages.add("good bye");
        messages.add("good night");
        messages.add("good morning");

        arrayAdapter=new MyChatAdapter(this,messages);
        list.setAdapter(arrayAdapter);
        list.setOnItemClickListener(this);
        list.setOnItemLongClickListener(this);
        editText=(EditText)findViewById(R.id.chatBox);
        View mCustomView = mInflater.inflate(R.layout.chat_toolbar, null);
        getSupportActionBar().setCustomView(mCustomView);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        name = mCustomView.findViewById(R.id.name);
        profilePic = mCustomView.findViewById(R.id.profilePic);

        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        list.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
                if(checked){
                    count = count + 1;
                }
                else
                    count =count-1;
                mode.setTitle(count+"");
                mode.setSubtitle(null);
                mode.setTag(false);
                mode.setTitleOptionalHint(false);
            }

            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
                MenuInflater inflater=mode.getMenuInflater();
                inflater.inflate(R.menu.chat_cab,menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
                switch(item.getItemId()) {
                    case R.id.reply:
                        return true;
                    case R.id.star_message:
                        return true;
                    case R.id.info:
                        return true;
                    case R.id.delete:
                        return true;
                    case R.id.copy:
                        return true;
                    case R.id.forward:
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {

            }
        });

        /**
         * Get recipient's user details
         */
        recipientListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    recipientUser = dataSnapshot.getValue(UserModel.class);
                    recipientUser.setPhone(toPhone);
                    name.setText(recipientUser.getFirstname() + " " + recipientUser.getLastname());

                    Picasso.with(Chat.this).load(recipientUser.getProfilePic()).fit().centerCrop()
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(profilePic);
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        recipientRef.addListenerForSingleValueEvent(recipientListener);

        /**
         * Get sender's user details
         */
        myRefListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    senderUser = dataSnapshot.getValue(UserModel.class);
                    senderUser.setPhone(fromPhone);

                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myRef.addListenerForSingleValueEvent(myRefListener);


        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(recipientUser.getProfilePic() != null){
                    Intent slideactivity = new Intent(Chat.this, ViewImage.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("imageURL", recipientUser.getProfilePic());
                    startActivity(slideactivity);
                }

            }
        });


        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message=editText.getText().toString();
                if(null!=message&&message.length()>0) {
                    MessageModel dm = new MessageModel();
                    String key = recipientMessagesRef.push().getKey();
                    dm.setSender(fromPhone);
                    dm.setReciever(toPhone);
                    dm.setTimeStamp(getDate());
                    dm.setMessage(message.trim());
                    recipientMessagesRef.child(key).setValue(dm);
                    myMessagedRef.child(key).setValue(dm);

                    messages.add(message.trim());
                    editText.setText("");
                    arrayAdapter.notifyDataSetChanged();
                    list.setSelection(list.getAdapter().getCount()-1);
                }
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.chat_call:
                if(recipientUser.getPhone() != null){
                    final AlertDialog callAlert = new AlertDialog.Builder(Chat.this)
                            //set message, title, and icon
                            .setMessage("Call " + recipientUser.getFirstname() + " " + recipientUser.getLastname() + "?")
                            //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                            //set three option buttons
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String phone = recipientUser.getPhone();
                                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null));
                                    startActivity(intent);
                                }
                            }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //do nothing

                                }
                            })//setNegativeButton

                            .create();
                    callAlert.show();
                }

                return  true;
            case R.id.chat_view_profile:
                if(recipientUser.getPhone() != null){
                    Intent slideactivity = new Intent(Chat.this, ViewProfile.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    slideactivity.putExtra("phone", recipientUser.getPhone());
                    Bundle bndlanimation =
                            null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        bndlanimation = ActivityOptions.makeCustomAnimation(Chat.this, R.anim.animation,R.anim.animation2).toBundle();
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        startActivity(slideactivity, bndlanimation);
                    }
                }
                return true;
            case R.id.chat_media:
                return true;
            case R.id.chat_search:
                return true;
            case R.id.chat_block:
                //TODO add custom dialog box
                return true;
            case R.id.chat_clearchat:
                return true;
            case R.id.chat_emailchat:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Nullable
    @Override
    public ActionMode onWindowStartingSupportActionMode(ActionMode.Callback callback) {
        return null;
    }

    public void backPress(View v){
        super.onBackPressed();
    }


    private String getDate() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        TimeZone timeZone = TimeZone.getDefault();
        Calendar calendar = Calendar.getInstance(timeZone);
        String time = date+ ":" +
                String.format("%02d" , calendar.get(Calendar.HOUR_OF_DAY))+":"+
                String.format("%02d" , calendar.get(Calendar.MINUTE))+":"+
                String.format("%02d" , calendar.get(Calendar.SECOND)) +":"+
                timeZone.getDisplayName(false, TimeZone.SHORT);

        return time;
    }
}
