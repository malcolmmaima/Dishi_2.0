package com.malcolmmaima.dishi.View.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.malcolmmaima.dishi.R;

import org.jsoup.Jsoup;
import org.jsoup.examples.HtmlToPlainText;

public class WPPostDetails extends AppCompatActivity {
    WebView webView;
    String TAG = "WPPostDetails";
    ProgressDialog progressDialog ;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postdetails);

        //title = (TextView) findViewById(R.id.title);
        webView = (WebView) findViewById(R.id.postwebview);
        Intent i = getIntent();
        String url = i.getExtras().getString("url");
        String blogTitle = i.getExtras().getString("title");

        progressDialog = new ProgressDialog(WPPostDetails.this);
        progressDialog.setMessage("Loading...");
        progressDialog.show();

        Toolbar topToolBar = findViewById(R.id.toolbar);
        setSupportActionBar(topToolBar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        try {
            String plainTitle = new HtmlToPlainText().getPlainText(Jsoup.parse(blogTitle));
            setTitle(plainTitle);
        } catch (Exception e){
            setTitle("Blog");
        }

        //Back button on toolbar
        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); //Go back to previous activity
            }
        });

        try {
            webView.getSettings().setJavaScriptEnabled(true);
            // to open webview inside app -- otherwise It will open url in device browser

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return true;
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    finish();
                    Toast.makeText(WPPostDetails.this, "Error:" + description, Toast.LENGTH_SHORT).show();

                }
            });
            webView.loadUrl(url);
        } catch (Exception e){
            finish();
            Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onCreate: ", e);
        }

    }
}
