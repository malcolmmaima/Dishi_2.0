package com.malcolmmaima.dishi.View.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.malcolmmaima.dishi.R;

public class NotificationSettings extends AppCompatActivity {

    String myPhone;
    DatabaseReference myRef;
    Switch socialSwitch, orderSwitch, chatSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle("Notifications");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        myPhone = user.getPhoneNumber(); //Current logged in user phone number

        //Set fb database reference
        myRef = FirebaseDatabase.getInstance().getReference("users/"+myPhone);

        socialSwitch = findViewById(R.id.SocialNotifications);
        orderSwitch = findViewById(R.id.OrderNotifications);
        chatSwitch = findViewById(R.id.ChatNotifications);

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });

        socialSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position

                if(isChecked == true && buttonView.isPressed()){
                    Toast.makeText(NotificationSettings.this, "Social notifications ON", Toast.LENGTH_SHORT).show();
                }

                else {
                    Toast.makeText(NotificationSettings.this, "Social notifications OFF", Toast.LENGTH_SHORT).show();
                }

            }
        });

        orderSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position

                if(isChecked == true && buttonView.isPressed()){
                    Toast.makeText(NotificationSettings.this, "Order notifications ON", Toast.LENGTH_SHORT).show();
                }

                else {
                    Toast.makeText(NotificationSettings.this, "Order notifications OFF", Toast.LENGTH_SHORT).show();
                }

            }
        });

        chatSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position

                if(isChecked == true && buttonView.isPressed()){
                    Toast.makeText(NotificationSettings.this, "Chat notifications ON", Toast.LENGTH_SHORT).show();
                }

                else {
                    Toast.makeText(NotificationSettings.this, "Chat notifications OFF", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }
}
