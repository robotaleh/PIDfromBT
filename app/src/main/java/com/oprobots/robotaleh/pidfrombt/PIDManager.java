package com.oprobots.robotaleh.pidfrombt;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

public class PIDManager extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pidmanager);

        TextView consola = (TextView) findViewById(R.id.consola);
        consola.setMovementMethod(new ScrollingMovementMethod());
    }

    public void onChangeButton(View view) {
    }
}
