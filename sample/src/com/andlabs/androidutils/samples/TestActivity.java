package com.andlabs.androidutils.samples;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

import com.andlabs.androidutils.logging.L;

public class TestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        
        
        // Logging
        final String formattedTestString = "first argument = %s, second argument = %s";
        final String firstArgument = "abc";
        final Object secondArgument = new  Object() {
            public String toString() {
                return "123";
            };
        };
        
        L.d(formattedTestString, firstArgument, secondArgument);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.test, menu);
        return true;
    }

}
