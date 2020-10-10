package com.malcolmmaima.dishi.View.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.malcolmmaima.dishi.R;
import com.malcolmmaima.dishi.View.Fragments.ExploreFragment;

public class WPPostDetails extends AppCompatActivity {
    WebView webView;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postdetails);

        //title = (TextView) findViewById(R.id.title);
        webView = (WebView) findViewById(R.id.postwebview);
        Intent i = getIntent();
        int position = i.getExtras().getInt("itemPosition");

        Log.e("WpPostDetails ", "  title is " + ExploreFragment.mListPost.get(position).getTitle().getRendered());

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        setTitle(ExploreFragment.mListPost.get(position).getTitle().getRendered());

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(ExploreFragment.mListPost.get(position).getLink());
        // to open webview inside app -- otherwise It will open url in device browser
        webView.setWebViewClient(new WebViewClient());

    }
}
