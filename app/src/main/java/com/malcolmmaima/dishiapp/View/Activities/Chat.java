package com.malcolmmaima.dishiapp.View.Activities;

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
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Light;
import com.malcolmmaima.dishiapp.Controller.Fonts.MyTextView_Roboto_Medium;
import com.malcolmmaima.dishiapp.Controller.Utils.CommentKeyBoardFix;
import com.malcolmmaima.dishiapp.Model.MessageModel;
import com.malcolmmaima.dishiapp.Model.UserModel;
import com.malcolmmaima.dishiapp.R;
import com.malcolmmaima.dishiapp.View.Adapter.MyChatAdapter;
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


public class Chat extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener{

    private static final String TAG = "ChatActivity";
    DatabaseReference recipientRef, recipientMessagesRef,
            myMessagedRef, followingRef, followerRef, myRef,
            recipientBlockedUsers, myBlockedUsers, activeOrdersRef;
    ValueEventListener recipientListener, myMessagesListener,
            followingListener, followerListener, accountTypeListener, blockedUsersListener;
    UserModel recipientUser;
    Menu myMenu;
    MessageModel chatMessage;
    ArrayList<MessageModel> messages;
    ImageButton emoji;
    EmojiconEditText emojiconEditText;
    EditText editText;
    ListView list;
    MyChatAdapter arrayAdapter;
    MyTextView_Roboto_Medium name;
    MyTextView_Roboto_Light userStatus;
    Toolbar toolbar;
    CircleImageView profilePic;
    ImageButton sendBtn;
    RelativeLayout mainContent;
    String myPhone;
    FirebaseUser user;
    int count = 0;
    View rootView;
    EmojIconActions emojIcon;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            Toast.makeText(this, "Not logged in!", Toast.LENGTH_SHORT).show();
        } else {
            try {
                user = FirebaseAuth.getInstance().getCurrentUser();
                myPhone = user.getPhoneNumber(); //Current logged in user phone number
                myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
                activeOrdersRef = FirebaseDatabase.getInstance().getReference("orders/"+myPhone);
                myBlockedUsers = FirebaseDatabase.getInstance().getReference("blocked/"+myPhone);
                myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    Boolean locked = dataSnapshot.getValue(Boolean.class);

                                    if(locked == true){
                                        Intent slideactivity = new Intent(Chat.this, SecurityPin.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        slideactivity.putExtra("pinType", "resume");
                                        startActivity(slideactivity);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                loadChat();
            } catch (Exception e){}
        }

    }

    private void loadChat() {
        rootView = findViewById(R.id.parentlayout);

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
            recipientBlockedUsers = FirebaseDatabase.getInstance().getReference("blocked/"+fromPhone);
            myMessagedRef = FirebaseDatabase.getInstance().getReference("messages/"+toPhone+"/"+fromPhone);
            followingRef = FirebaseDatabase.getInstance().getReference("following/"+toPhone+"/"+fromPhone);
            followerRef = FirebaseDatabase.getInstance().getReference("followers/"+toPhone+"/"+fromPhone);
        }

        else {
            recipientRef = FirebaseDatabase.getInstance().getReference("users/"+toPhone);
            recipientMessagesRef = FirebaseDatabase.getInstance().getReference("messages/"+toPhone+"/"+fromPhone);
            recipientBlockedUsers = FirebaseDatabase.getInstance().getReference("blocked/"+toPhone);
            myMessagedRef = FirebaseDatabase.getInstance().getReference("messages/"+fromPhone+"/"+toPhone);
            followingRef = FirebaseDatabase.getInstance().getReference("following/"+fromPhone+"/"+toPhone);
            followerRef = FirebaseDatabase.getInstance().getReference("followers/"+fromPhone+"/"+toPhone);
        }

        //You never know :-D ... you can't message yourself
        if(fromPhone.equals(toPhone)){
            Toast.makeText(this, "not allowed!", Toast.LENGTH_SHORT).show();
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
                            } catch (Exception e){
                                Log.e(TAG, "onActionItemClicked: ", e);
                            }

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
                    //Log.d(TAG, "onFocusChange: keyboard open => "+hasFocus);

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

                    if(recipientUser.getProfilePicSmall() != null){
                        Picasso.with(Chat.this).load(recipientUser.getProfilePicSmall()).fit().centerCrop()
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(profilePic);
                    }

                    else {
                        Picasso.with(Chat.this).load(recipientUser.getProfilePic()).fit().centerCrop()
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(profilePic);
                    }
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
                    if(recipientUser.getProfilePicBig() != null){
                        Intent slideactivity = new Intent(Chat.this, ViewImage.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        slideactivity.putExtra("imageURL", recipientUser.getProfilePicBig());
                        startActivity(slideactivity);
                    }

                    else {
                        Intent slideactivity = new Intent(Chat.this, ViewImage.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        slideactivity.putExtra("imageURL", recipientUser.getProfilePic());
                        startActivity(slideactivity);
                    }
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

                    recipientBlockedUsers.child(myPhone).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()){ //I have been blocked by recipient
                                myMessagedRef.child(key).setValue(dm).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        try {
                                            arrayAdapter.notifyDataSetChanged();
                                        } catch (Exception er){
                                            Log.e(TAG, "onFailure: ", er);
                                        }
                                        emojiconEditText.setText(dm.getMessage());
                                        Toast.makeText(Chat.this, "Something went wrong...", Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else { //not blocked
                                recipientMessagesRef.child(key).setValue(dm);
                                myMessagedRef.child(key).setValue(dm).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        try {
                                            arrayAdapter.notifyDataSetChanged();
                                        } catch (Exception er){
                                            Log.e(TAG, "onFailure: ", er);
                                        }
                                        emojiconEditText.setText(dm.getMessage());
                                        Toast.makeText(Chat.this, "Something went wrong...", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

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
    protected void onResume() {
        super.onResume();
        myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Boolean locked = dataSnapshot.getValue(Boolean.class);

                            if(locked == true){
                                Intent slideactivity = new Intent(Chat.this, SecurityPin.class)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                slideactivity.putExtra("pinType", "resume");
                                startActivity(slideactivity);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getInstance().getCurrentUser() == null){
            finish();
            Toast.makeText(this, "Not logged in!", Toast.LENGTH_SHORT).show();
        } else {
            try {
                user = FirebaseAuth.getInstance().getCurrentUser();
                myPhone = user.getPhoneNumber(); //Current logged in user phone number
                myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);
                myRef.child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            myRef.child("appLocked").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    Boolean locked = dataSnapshot.getValue(Boolean.class);

                                    if(locked == true){
                                        Intent slideactivity = new Intent(Chat.this, SecurityPin.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        slideactivity.putExtra("pinType", "resume");
                                        startActivity(slideactivity);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                loadChat();
            } catch (Exception e){}
        }
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
        MenuItem call = menu.findItem(R.id.chat_call);
        MenuItem blockOption = menu.findItem(R.id.chat_block);
        MenuItem unblockOption = menu.findItem(R.id.chat_unblock);

        try {
            call.setVisible(false);
            blockOption.setVisible(false);
            unblockOption.setVisible(false);
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
                            call.setVisible(true);
                            call.setEnabled(true);
                        } catch (Exception e){ }
                    } else { //Rider and customer accounts
                        /**
                         * As a privacy concern, we don't want to expose people's phone numbers under chat ... only show phone if following/follower is mutual
                         */

                        String phoneVisibilty = dataSnapshot.child("phoneVisibility").getValue(String.class);
                        if(phoneVisibilty.equals("none")){
                            call.setVisible(false);
                            call.setEnabled(false);

                            /**
                             * if I am a vendor, check if i have an active order with this recipient, if true show phone if not dont't
                             */

                            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    try {
                                        UserModel myDetails = dataSnapshot.getValue(UserModel.class);
                                        if (myDetails.getAccount_type().equals("2")) {
                                            //I am vendor. now check if i have an active order with the recipient
                                            activeOrdersRef.child(recipientUser_.getPhone()).addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    //(i) a vendor has an active order in progress with recipient
                                                    if (dataSnapshot.exists()) {
                                                        call.setVisible(true);
                                                        call.setEnabled(true);
                                                    } else { //(i) vendor do not have an active order with recipient
                                                        call.setVisible(false);
                                                        call.setEnabled(false);
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });
                                        }
                                    } catch (Exception e){
                                        Log.e(TAG, "onDataChange: ", e);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }

                        if(phoneVisibilty.equals("mutual")){
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
                                                        call.setVisible(true);
                                                        call.setEnabled(true);
                                                    } catch (Exception e){

                                                    }
                                                } else {
                                                    //Toast.makeText(Chat.this, "Follower => false", Toast.LENGTH_SHORT).show();
                                                    try {
                                                        call.setVisible(false);
                                                        call.setEnabled(false);
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
                                            call.setVisible(false);
                                            call.setEnabled(false);
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

                        if(phoneVisibilty.equals("everyone")){
                            call.setVisible(true);
                            call.setEnabled(true);
                        }

                    }
                } catch (Exception e){

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        recipientRef.addValueEventListener(accountTypeListener);

        /**
         * blocked Users listener
         * */
        blockedUsersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    blockOption.setVisible(true);
                    unblockOption.setVisible(false);
                } else {
                    for(DataSnapshot blocked : dataSnapshot.getChildren()){
                        if(blocked.getKey().equals(recipientUser.getPhone())){
                            unblockOption.setVisible(true);
                            blockOption.setVisible(false);
                            call.setEnabled(false);
                        } else {
                            blockOption.setVisible(true);
                            unblockOption.setVisible(false);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myBlockedUsers.addValueEventListener(blockedUsersListener);

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
                AlertDialog blockuser = new AlertDialog.Builder(Chat.this)
                        //set message, title, and icon
                        .setMessage("Block "+recipientUser.getFirstname() + " " + recipientUser.getLastname()+"?")
                        //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                        //set three option buttons
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                myBlockedUsers.child(recipientUser.getPhone()).setValue("blocked").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        try {
                                            Snackbar snackbar = Snackbar.make(findViewById(R.id.parentlayout), "Blocked", Snackbar.LENGTH_LONG);
                                            snackbar.show();
                                        } catch (Exception er){
                                            Log.e(TAG, "onSuccess: ", er);
                                        }
                                    }
                                });
                            }
                        })
                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //do nothing
                            }
                        })//setNegativeButton

                        .create();
                blockuser.show();
                return true;

            case R.id.chat_unblock:
                AlertDialog unBlockUser = new AlertDialog.Builder(Chat.this)
                        //set message, title, and icon
                        .setMessage("Unblock "+recipientUser.getFirstname() + " " + recipientUser.getLastname()+"?")
                        //.setIcon(R.drawable.icon) will replace icon with name of existing icon from project
                        //set three option buttons
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                myBlockedUsers.child(recipientUser.getPhone()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        try {
                                            Snackbar snackbar = Snackbar.make(findViewById(R.id.parentlayout), "Unblocked", Snackbar.LENGTH_LONG);
                                            snackbar.show();
                                        } catch (Exception er){
                                            Log.e(TAG, "onSuccess: ", er);
                                        }
                                    }
                                });
                            }
                        })
                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                //do nothing
                            }
                        })//setNegativeButton

                        .create();
                unBlockUser.show();
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
            Toast.makeText(Chat.this, "Copied!", Toast.LENGTH_SHORT).show();
            mode.finish();

        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(Chat.this, "Copied!", Toast.LENGTH_SHORT).show();
            mode.finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(rootView != null){
            rootView = null;
            list.setAdapter(null);
        }

        try {
            myMessagedRef.removeEventListener(myMessagesListener);
            myBlockedUsers.removeEventListener(blockedUsersListener);
            //there may be instances where these two listeners are not triggered
            followerRef.removeEventListener(followerListener);
            followingRef.removeEventListener(followingListener);
            recipientRef.removeEventListener(recipientListener);
            recipientRef.removeEventListener(accountTypeListener);
        } catch (Exception e){

        }

    }
}
