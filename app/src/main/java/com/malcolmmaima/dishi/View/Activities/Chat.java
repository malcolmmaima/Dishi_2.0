package com.malcolmmaima.dishi.View.Activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
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

import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Adapter.MyChatAdapter;

import java.util.ArrayList;

public class Chat extends AppCompatActivity implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener{

    ArrayList<String> messages;
    EditText editText;
    ListView list;
    MyChatAdapter arrayAdapter;
    TextView name;
    Toolbar toolbar;
    Intent intent;
    ImageButton sendBtn;
    int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        String from = getIntent().getStringExtra("fromPhone");
        String to = getIntent().getStringExtra("toPhone");

        Toast.makeText(this, "fro: " + from + " to: " + to, Toast.LENGTH_SHORT).show();

        intent = getIntent();
        String contactName = intent.getStringExtra("name");
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

        name = (TextView) mCustomView.findViewById(R.id.name);
        name.setText(contactName);

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message=editText.getText().toString();
                if(null!=message&&message.length()>0) {
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
            case R.id.chat_viewcontact:
                //startActivity(new Intent(this,ViewContact.class));
                return true;
            case R.id.chat_media:
                return true;
            case R.id.chat_search:
                return true;
            case R.id.chat_mute:
                //TODO add custom dialog box
                return true;
            case R.id.chat_wallpaper:
                return true;
            case R.id.chat_block:
                //TODO add custom dialog box
                return true;
            case R.id.chat_clearchat:
                return true;
            case R.id.chat_emailchat:
                return true;
            case R.id.chat_addshortcut:
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

    public void profileClick(View v){
        //startActivity(new Intent(this, ViewContact.class));
    }
}
