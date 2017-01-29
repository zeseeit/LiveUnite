package com.liveunite.activities;

import android.content.Context;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.liveunite.R;
import com.liveunite.network.Urls;
import com.liveunite.utils.CheckInternetConnection;
import com.liveunite.utils.ShowProgressDialog;

import static java.lang.Thread.sleep;

public class TermsOfUse extends AppCompatActivity {

    WebView wbTerms;
    ShowProgressDialog showProgressDialog;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_of_use);
        context = TermsOfUse.this;

        setUpToolbar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(new CheckInternetConnection(context).isConnectedToInternet())
        {
            init();
        }else
        {
            new CheckInternetConnection(context).showDialog();
        }
    }

    private void setUpToolbar() {
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle("Terms of LiveUnite");
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
    }

    private void init() {
        showProgressDialog= new ShowProgressDialog();
        showProgressDialog.create(context);
        wbTerms = (WebView)findViewById(R.id.wbTerms);
        setWebTerms();
        loadUrl(Urls.TERMS_CONDITION);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    private void loadUrl(String url) {
        wbTerms.loadUrl(url);
        showProgressDialog.show();
    }



    private void setWebTerms() {
        wbTerms.setWebChromeClient(new MyBrowser());
    }

    private class MyBrowser extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (newProgress ==0)
            {
                showProgressDialog.show();
            }else if (newProgress ==100)
            {
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            sleep(2000);
                            showProgressDialog.dismiss();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                thread.start();
            }
        }
    }
}
