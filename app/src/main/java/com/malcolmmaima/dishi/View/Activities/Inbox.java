package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.malcolmmaima.dishi.Model.MessageModel;
import com.malcolmmaima.dishi.Model.UserModel;
import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.ChatListAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.fabric.sdk.android.services.common.SafeToast;

public class Inbox extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    ArrayList<UserModel> chatlist = new ArrayList<>();
    Menu myMenu;
    String myPhone;
    FirebaseUser user;
    TextView emptyTag;
    AppCompatImageView icon;
    SwipeRefreshLayout mSwipeRefreshLayout;
    DatabaseReference myMessagesRef;
    ValueEventListener myMessagesListener;
    UserModel contactDm;
    ListView chatList;
    ChatListAdapter adapter;
    MessageModel chatMessage;
    int count=0;
    public ActionMode actionMode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Inbox");

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number
        myMessagesRef = FirebaseDatabase.getInstance().getReference("messages/"+myPhone);


        icon = findViewById(R.id.inboxIcon);
        chatList = findViewById(R.id.chatList);
        emptyTag = findViewById(R.id.empty_tag);

        // SwipeRefreshLayout
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        /**
         * Showing Swipe Refresh animation on activity create
         * As animation won't start on onCreate, post runnable is used
         */
        mSwipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {

                mSwipeRefreshLayout.setRefreshing(true);
                //fetchMessages();

            }
        });

    }

    private void fetchMessages() {
        mSwipeRefreshLayout.setRefreshing(true);
        chatlist.clear();
        contactDm = null;
        chatMessage = null;
        myMessagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatlist.clear();
                contactDm = null;
                if(!dataSnapshot.hasChildren()){
                    mSwipeRefreshLayout.setRefreshing(false);
                    adapter = new ChatListAdapter(Inbox.this, chatlist, getApplicationContext());
                    chatList.setAdapter(adapter);
                    emptyTag.setVisibility(View.VISIBLE);
                    icon.setVisibility(View.VISIBLE);
                } else {
                    for(DataSnapshot userDm : dataSnapshot.getChildren()){
                        /**
                         * Get recipient user details
                         */
                        DatabaseReference userDetails = FirebaseDatabase.getInstance().getReference("users/"+userDm.getKey());
                        userDetails.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot users) {


                                /**
                                 * Get recipient's last message
                                 */
                                Query lastQuery = myMessagesRef.child(userDm.getKey()).orderByKey().limitToLast(1);
                                lastQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {

                                        for(DataSnapshot message : dataSnapshot.getChildren()){
                                            chatMessage = message.getValue(MessageModel.class);
                                            chatMessage.setKey(message.getKey());

                                            contactDm = users.getValue(UserModel.class);
                                            contactDm.setPhone(userDm.getKey());
                                            contactDm.timeStamp = chatMessage.getTimeStamp();
                                            contactDm.message = chatMessage.getMessage();
                                            chatlist.add(contactDm);
                                        }

                                        if(!chatlist.isEmpty()){
                                            try {
                                                Collections.sort(chatlist, (chat1, chat2) -> (chat2.timeStamp.compareTo(chat1.timeStamp)));
                                            } catch (Exception e){}
                                            mSwipeRefreshLayout.setRefreshing(false);
                                            adapter = new ChatListAdapter(Inbox.this, chatlist, getApplicationContext());
                                            chatList.setAdapter(adapter);
                                            emptyTag.setVisibility(View.GONE);
                                            icon.setVisibility(View.GONE);
                                        }

                                        else {
                                            mSwipeRefreshLayout.setRefreshing(false);
                                            adapter = new ChatListAdapter(Inbox.this, chatlist, getApplicationContext());
                                            chatList.setAdapter(adapter);
                                            emptyTag.setVisibility(View.VISIBLE);
                                            icon.setVisibility(View.VISIBLE);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        // Handle possible errors.
                                    }
                                });




                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        myMessagesRef.addListenerForSingleValueEvent(myMessagesListener);

        ArrayList<Integer> selectedMsgs = new ArrayList<>();
        chatList.setOnItemClickListener(this);
        chatList.setOnItemLongClickListener(this);
        chatList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        chatList.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                if(checked){
                    count++;
                    selectedMsgs.add(position);
                }
                else
                    count--;
                mode.setTitle(count+"");
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                actionMode = mode;
                MenuInflater inflater=actionMode.getMenuInflater();
                inflater.inflate(R.menu.chat_action_menu,menu);
//                setActionMode.isActionMode = true;
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch(item.getItemId()) {
                    case R.id.menu_delete:
                        for (int i=0; i<selectedMsgs.size(); i++){
                            myMessagesRef.child(chatlist.get(selectedMsgs.get(i)).getPhone()).removeValue();

                            if(i==selectedMsgs.size()-1){
                                selectedMsgs.clear();
                                count = 0;
                                mode.finish();
                                fetchMessages();
                            }
                        }
                        return true;

                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                count=0;
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_inbox, menu);
        myMenu = menu;

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.newChat:
                Intent mainActivity = new Intent(Inbox.this, NewChat.class);
                mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainActivity);
                return(true);

        }
        return(super.onOptionsItemSelected(item));
    }

    @Override
    public void onRefresh() {
        fetchMessages();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String name = chatlist.get(position).getFirstname()+" "+chatlist.get(position).getLastname();
        Intent intent=new Intent(Inbox.this, Chat.class);
        intent.putExtra("fromPhone", myPhone);
        intent.putExtra("toPhone", chatlist.get(position).getPhone());
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        view.setSelected(true);
        return false;
    }

    @Override
    public void onPause(){
        super.onPause();
        if(actionMode!=null) {
            chatList.clearChoices();
            adapter.notifyDataSetChanged();
            actionMode.finish();
            actionMode = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchMessages();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
