package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alexzh.circleimageview.CircleImageView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Controller.Utils.CommentKeyBoardFix;
import com.malcolmmaima.dishi.Model.MessageModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.MyChatAdapter;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import io.fabric.sdk.android.services.common.SafeToast;

public class Chat extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener{

    private static final String TAG = "ChatActivity";
    DatabaseReference recipientRef, recipientMessagesRef, myMessagedRef, followingRef, followerRef;
    ValueEventListener recipientListener, myMessagesListener, followingListener, followerListener, accountTypeListener;
    UserModel recipientUser;
    Menu myMenu;
    MessageModel chatMessage;
    ArrayList<MessageModel> messages;
    ImageButton emoji;
    EmojiconEditText emojiconEditText;
    EditText editText;
    ListView list;
    MyChatAdapter arrayAdapter;
    TextView name, userStatus;
    Toolbar toolbar;
    CircleImageView profilePic;
    ImageButton sendBtn;
    RelativeLayout mainContent;
    String myPhone;
    FirebaseUser user;
    int count = 0;
    View rootView;
    EmojIconActions emojIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        rootView = findViewById(R.id.parentlayout);
        try {
            user = FirebaseAuth.getInstance().getCurrentUser();
            myPhone = user.getPhoneNumber(); //Current logged in user phone number
        } catch (Exception e){}

        //keep toolbar pinned at top. push edittext on keyboard load
        new CommentKeyBoardFix(this);
        //Hide keyboard on activity load
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        String fromPhone = getIntent().getStringExtra("fromPhone");
        String toPhone = getIntent().getStringExtra("toPhone");
        //For debugging purposes
        //Toast.makeText(this, "from => "+fromPhone +" to => " +toPhone, Toast.LENGTH_LONG).show();

        //May God be with you as you try to understand some of my code... there are times i didn't know what i was doing ;-D

        //This is a bugfix since i noticed on clicking message notification i was getting some very innacurate/dirty data sent to the wrong nodes
        if(toPhone.equals(myPhone)){
            //For debugging purposes
            //Toast.makeText(this, "from = myphone", Toast.LENGTH_SHORT).show();
            recipientRef = FirebaseDatabase.getInstance().getReference("users/"+fromPhone);
            recipientMessagesRef = FirebaseDatabase.getInstance().getReference("messages/"+fromPhone+"/"+toPhone);
            myMessagedRef = FirebaseDatabase.getInstance().getReference("messages/"+toPhone+"/"+fromPhone);
            followingRef = FirebaseDatabase.getInstance().getReference("following/"+toPhone+"/"+fromPhone);
            followerRef = FirebaseDatabase.getInstance().getReference("followers/"+toPhone+"/"+fromPhone);
        }

        else {
            recipientRef = FirebaseDatabase.getInstance().getReference("users/"+toPhone);
            recipientMessagesRef = FirebaseDatabase.getInstance().getReference("messages/"+toPhone+"/"+fromPhone);
            myMessagedRef = FirebaseDatabase.getInstance().getReference("messages/"+fromPhone+"/"+toPhone);
            followingRef = FirebaseDatabase.getInstance().getReference("following/"+fromPhone+"/"+toPhone);
            followerRef = FirebaseDatabase.getInstance().getReference("followers/"+fromPhone+"/"+toPhone);
        }

        //You never know :-D ... you can't message yourself
        if(fromPhone.equals(toPhone)){
            SafeToast.makeText(this, "not allowed!", Toast.LENGTH_SHORT).show();
            finish();
        }

        mainContent = findViewById(R.id.main_content);
        toolbar = (Toolbar) findViewById(R.id.chat_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(null);
        toolbar.setTitle(null);
        LayoutInflater mInflater = LayoutInflater.from(this);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        sendBtn = findViewById(R.id.send);
        emoji = findViewById(R.id.emoji);
        list= (ListView) findViewById(R.id.list);
        messages = new ArrayList<>();
        myMessagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                messages.clear();
                for(DataSnapshot message : dataSnapshot.getChildren()){
                    chatMessage = message.getValue(MessageModel.class);
                    chatMessage.setKey(message.getKey());

                    if(chatMessage.getMessage() != null){
                        messages.add(chatMessage);
                    }

                }

                arrayAdapter=new MyChatAdapter(Chat.this,messages);
                list.setAdapter(arrayAdapter);
                list.setOnItemClickListener(Chat.this);
                list.setOnItemLongClickListener(Chat.this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myMessagedRef.addValueEventListener(myMessagesListener);
        emojiconEditText = findViewById(R.id.chatBox);
        View mCustomView = mInflater.inflate(R.layout.chat_toolbar, null);
        getSupportActionBar().setCustomView(mCustomView);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        name = mCustomView.findViewById(R.id.name);
        name.setText("loading...");
        profilePic = mCustomView.findViewById(R.id.profilePic);
        userStatus = mCustomView.findViewById(R.id.userStatus);

        ArrayList<Integer> selectedMsgs = new ArrayList<>();
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        list.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
                if(checked){
                    count = count + 1;
                    selectedMsgs.add(position);
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
                    /** case R.id.reply:
                        return true;
                    case R.id.star_message:
                        return true;
                     case R.id.forward:
                     return true;
                    case R.id.info:
                        return true; */
                    case R.id.delete:
                        for (int i=0; i<selectedMsgs.size(); i++){
                            //Toast.makeText(Chat.this, "delete: " + messages.get(deletes.get(i)).getKey() + " => "+messages.get(deletes.get(i)).getMessage(), Toast.LENGTH_SHORT).show();
                            try {
                                myMessagedRef.child(messages.get(selectedMsgs.get(i)).getKey()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        arrayAdapter.notifyDataSetChanged();
                                    }
                                });
                            } catch (Exception e){}

                            if(i==selectedMsgs.size()-1){
                                selectedMsgs.clear();
                                count = 0;
                                mode.finish();
                            }
                        }
                        return true;
                    case R.id.copy:
                        // Append all selected messages to a single string  variable and pass to setClipboard()
                        String [] selected = new String[selectedMsgs.size()];
                        StringBuffer sb = new StringBuffer();
                        for (int i=0; i<selectedMsgs.size(); i++){
                            selected[i] = messages.get(selectedMsgs.get(i)).getMessage();
                        }

                        String str = Arrays.toString(selected);
                        setClipboard(Chat.this, str, mode);
                        return true;

                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
                count = 0;
                selectedMsgs.clear();
            }
        });

        /**
         * https://www.androidhive.info/2016/11/android-integrate-emojis-keyboard-app/
         */
        emojIcon = new EmojIconActions(this, rootView, emojiconEditText, emoji);
        emojIcon.setUseSystemEmoji(false); //if we set this to true then the default emojis for chat wil be the system emojis
        //                              ic.action_keyboard //inbuilt in library
        emojIcon.setIconsIds(R.drawable.ic_keyboard_white_48dp, R.drawable.ic_sentiment_satisfied_white_48dp);
        emojIcon.setKeyboardListener(new EmojIconActions.KeyboardListener() {
            @Override
            public void onKeyboardOpen() {
                Log.e(TAG, "Keyboard opened!");
            }

            @Override
            public void onKeyboardClose() {
                emojIcon.closeEmojIcon();
                Log.e(TAG, "Keyboard closed");
            }
        });

        emojiconEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    //got focus
                    Log.d(TAG, "onFocusChange: keyboard open => "+hasFocus);

                    //Scroll to bottom, most recent chat on keyboard load
                    try {
                        arrayAdapter.notifyDataSetChanged();
                    } catch(Exception e){
                        Log.e(TAG, "onFocusChange: ", e);
                    }
                } else {
                    //lost focus
                }
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
                    Log.e(TAG, "onDataChange: ", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        recipientRef.addValueEventListener(recipientListener);

        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent slideactivity = new Intent(Chat.this, ViewImage.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    slideactivity.putExtra("imageURL", recipientUser.getProfilePic());
                    startActivity(slideactivity);
                } catch (Exception e){}

            }
        });

        emoji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emojIcon.ShowEmojIcon();
            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message=emojiconEditText.getText().toString();
                if(null!=message&&message.length()>0) {
                    MessageModel dm = new MessageModel();
                    String key = recipientMessagesRef.push().getKey();

                    //This is a feature fix, we need to perform this check
                    //since toPhone & fromPhone are bound to change due to notification activity
                    if(toPhone.equals(myPhone)){
                        dm.setSender(toPhone);
                        dm.setReciever(fromPhone);
                    }

                    else {
                        dm.setSender(fromPhone);
                        dm.setReciever(toPhone);
                    }

                    dm.setTimeStamp(getDate());
                    dm.setMessage(message.trim());
                    dm.setRead(false);
                    recipientMessagesRef.child(key).setValue(dm);
                    myMessagedRef.child(key).setValue(dm).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            emojiconEditText.setText(dm.getMessage());
                            SafeToast.makeText(Chat.this, "Something went wrong...", Toast.LENGTH_LONG).show();
                        }
                    });
                    messages.add(dm);
                    emojiconEditText.setText("");
                    arrayAdapter.notifyDataSetChanged();
                    list.setSelection(list.getAdapter().getCount()-1);
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        /**
         * onNewIntent() is triggered if user is already in the Chat activity and user clicks on mesg notification,
         * you want to refresh some of the key data
         */
        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        String fromPhone_ = intent.getStringExtra("fromPhone");
        String toPhone_ = intent.getStringExtra("toPhone");

        //For debugging purposes
        //Toast.makeText(this, "new from => "+fromPhone_ +" to => " +toPhone_, Toast.LENGTH_LONG).show();

        //You never know :-D ...
        if(fromPhone_.equals(toPhone_)){
            SafeToast.makeText(this, "not allowed!", Toast.LENGTH_SHORT).show();
            finish();
        }

        recipientRef = FirebaseDatabase.getInstance().getReference("users/"+fromPhone_);
        recipientMessagesRef = FirebaseDatabase.getInstance().getReference("messages/"+toPhone_+"/"+fromPhone_);
        myMessagedRef = FirebaseDatabase.getInstance().getReference("messages/"+fromPhone_+"/"+toPhone_);
        followingRef = FirebaseDatabase.getInstance().getReference("following/"+fromPhone_+"/"+toPhone_);
        followerRef = FirebaseDatabase.getInstance().getReference("followers/"+fromPhone_+"/"+toPhone_);

        myMessagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                messages.clear();
                for(DataSnapshot message : dataSnapshot.getChildren()){
                    chatMessage = message.getValue(MessageModel.class);
                    chatMessage.setKey(message.getKey());

                    if(chatMessage.getMessage() != null){
                        messages.add(chatMessage);
                    }

                }

                arrayAdapter=new MyChatAdapter(Chat.this,messages);
                list.setAdapter(arrayAdapter);
                list.setOnItemClickListener(Chat.this);
                list.setOnItemLongClickListener(Chat.this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myMessagedRef.addValueEventListener(myMessagesListener);

        ArrayList<Integer> selectedMsgs = new ArrayList<>();
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        list.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
                if(checked){
                    count = count + 1;
                    selectedMsgs.add(position);
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
                    /** case R.id.reply:
                     return true;
                     case R.id.star_message:
                     return true;
                     case R.id.forward:
                     return true;
                     case R.id.info:
                     return true; */
                    case R.id.delete:
                        for (int i=0; i<selectedMsgs.size(); i++){
                            //Toast.makeText(Chat.this, "delete: " + messages.get(deletes.get(i)).getKey() + " => "+messages.get(deletes.get(i)).getMessage(), Toast.LENGTH_SHORT).show();
                            myMessagedRef.child(messages.get(selectedMsgs.get(i)).getKey()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    arrayAdapter.notifyDataSetChanged();
                                }
                            });

                            if(i==selectedMsgs.size()-1){
                                selectedMsgs.clear();
                                count = 0;
                                mode.finish();
                            }
                        }
                        return true;
                    case R.id.copy:
                        // Append all selected messages to a single string  variable and pass to setClipboard()
                        String [] selected = new String[selectedMsgs.size()];
                        StringBuffer sb = new StringBuffer();
                        for (int i=0; i<selectedMsgs.size(); i++){
                            selected[i] = messages.get(selectedMsgs.get(i)).getMessage();
                        }

                        String str = Arrays.toString(selected);
                        setClipboard(Chat.this, str, mode);
                        return true;

                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
                count = 0;
                selectedMsgs.clear();
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
                    recipientUser.setPhone(toPhone_);
                    name.setText(recipientUser.getFirstname() + " " + recipientUser.getLastname());

                    Picasso.with(Chat.this).load(recipientUser.getProfilePic()).fit().centerCrop()
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(profilePic);
                } catch (Exception e){
                    Log.e(TAG, "onDataChange: ", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        recipientRef.addValueEventListener(recipientListener);

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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_menu, menu);
        myMenu = menu;
        MenuItem item = menu.findItem(R.id.chat_call);
        try {
            item.setVisible(false);
        } catch (Exception e){}

        /**
         * Get recipient's account type
         */
        accountTypeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    UserModel recipientUser_ = dataSnapshot.getValue(UserModel.class);
                    if(recipientUser_.getAccount_type().equals("2")){

                        //Automatically show phone for restaurant accounts
                        try {
                            myMenu.findItem(R.id.chat_call).setVisible(true);
                            myMenu.findItem(R.id.chat_call).setEnabled(true);
                        } catch (Exception e){ }
                    } else { //Rider and customer accounts
                        /**
                         * As a privacy concern, we don't want to expose people's phone numbers under chat ... only show phone if following/follower is mutual
                         */
                        followingListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()){
                                    //Toast.makeText(Chat.this, "Following => true", Toast.LENGTH_SHORT).show();
                                    followerListener = new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.exists()){
                                                //Toast.makeText(Chat.this, "Follower => true", Toast.LENGTH_SHORT).show();
                                                try {
                                                    myMenu.findItem(R.id.chat_call).setVisible(true);
                                                    myMenu.findItem(R.id.chat_call).setEnabled(true);
                                                } catch (Exception e){

                                                }
                                            } else {
                                                //Toast.makeText(Chat.this, "Follower => false", Toast.LENGTH_SHORT).show();
                                                try {
                                                    myMenu.findItem(R.id.chat_call).setVisible(false);
                                                    myMenu.findItem(R.id.chat_call).setEnabled(false);
                                                } catch (Exception e){

                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    };
                                    try {
                                        followerRef.addValueEventListener(followerListener);
                                    } catch (Exception e){}
                                } else {
                                    //Toast.makeText(Chat.this, "Following => false", Toast.LENGTH_SHORT).show();
                                    try {
                                        myMenu.findItem(R.id.chat_call).setVisible(false);
                                        myMenu.findItem(R.id.chat_call).setEnabled(false);
                                    } catch (Exception e){

                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        };

                        followingRef.addValueEventListener(followingListener);
                    }
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        recipientRef.addValueEventListener(accountTypeListener);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.chat_call:
                try {
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
                } catch (Exception e){
                    Log.e(TAG, "onOptionsItemSelected: ", e);
                }
                return  true;
            case R.id.chat_view_profile:
               try {
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
               } catch(Exception e){
                   Log.e(TAG, "onOptionsItemSelected: ", e);
               }
                return true;
            /**case R.id.chat_media:
                return true; */

            case R.id.chat_block:
                //TODO add custom dialog box
                Snackbar snackbar = Snackbar.make(findViewById(R.id.parentlayout), "In development", Snackbar.LENGTH_LONG);
                snackbar.show();
                return true;
            case R.id.chat_clearchat:
                AlertDialog clearChat = new AlertDialog.Builder(Chat.this)
                        //set message, title, and icon
                        .setMessage("Clear chat?")
                        //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                        //set three option buttons
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                myMessagedRef.removeValue();
                            }
                        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //do nothing

                            }
                        })//setNegativeButton

                        .create();
                clearChat.show();
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

    private void setClipboard(Context context, String text, android.view.ActionMode mode) {
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
            SafeToast.makeText(Chat.this, "Copied!", Toast.LENGTH_SHORT).show();
            mode.finish();

        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
            SafeToast.makeText(Chat.this, "Copied!", Toast.LENGTH_SHORT).show();
            mode.finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myMessagedRef.removeEventListener(myMessagesListener);

        try {
            //there may be instances where these two listeners are not triggered
            followerRef.removeEventListener(followerListener);
            followingRef.removeEventListener(followingListener);
        } catch (Exception e){

        }
        recipientRef.removeEventListener(recipientListener);
        recipientRef.removeEventListener(accountTypeListener);
    }
}
