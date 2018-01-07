package com.oprobots.robotaleh.pidfrombt;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static java.lang.Float.parseFloat;

public class PIDManager extends AppCompatActivity {

    private boolean run = false;
    private boolean stopOnShake = true;
    private boolean hasSuction = true;

    private float[][] intervals = new float[3][2];
    private final int INT_P = 0;
    private final int INT_I = 1;
    private final int INT_D = 2;
    private final int HARD_INT = 0;
    private final int SOFT_INT = 1;

    // Lista de controles principales
    private TextView console, txtP, txtI, txtD, txtX, txtV, txtS;
    private SeekBar seekX, seekV, seekS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pidmanager);


        // Inicializado de los controles principales
        console = (TextView) findViewById(R.id.console);
        txtP = (TextView) findViewById(R.id.txtP);
        txtI = (TextView) findViewById(R.id.txtI);
        txtD = (TextView) findViewById(R.id.txtD);
        txtX = (TextView) findViewById(R.id.txtX);
        txtV = (TextView) findViewById(R.id.txtV);
        txtS = (TextView) findViewById(R.id.txtS);
        seekX = (SeekBar) findViewById(R.id.seekX);
        seekV = (SeekBar) findViewById(R.id.seekV);
        seekS = (SeekBar) findViewById(R.id.seekS);
        console.setMovementMethod(new ScrollingMovementMethod());

        // Obtener los ajustes
        getSettings();

        // Establece los valores iniciales: los últimos insertados
        setInitialValues();

        assignListeners(seekX, seekV, seekS);
    }

    private void assignListeners(SeekBar seekX, SeekBar seekV, SeekBar seekS){
        seekX.setOnSeekBarChangeListener(seekXlistener);
        seekV.setOnSeekBarChangeListener(seekVlistener);
        seekS.setOnSeekBarChangeListener(seekSlistener);
    }

    @Override
    protected void onResume() {
        getSettings();
        super.onResume();
    }

    private void getSettings() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(PIDManager.this);
        stopOnShake = settings.getBoolean("stopOnShake", true);
        hasSuction = settings.getBoolean("hasSuction", true);
        String parsingErrors = "";
        try {
            intervals[INT_P][SOFT_INT] = parseFloat(settings.getString("softIntervalP", "1"));
        } catch (NumberFormatException e) {
            intervals[INT_P][SOFT_INT] = 1;
            parsingErrors += "El intervalo suave de P es incorrecto.\n";
        }
        try {
            intervals[INT_P][HARD_INT] = parseFloat(settings.getString("hardIntervalP", "5"));
        } catch (NumberFormatException e) {
            intervals[INT_P][HARD_INT] = 5;
            parsingErrors += "El intervalo fuerte de P es incorrecto.\n";
        }
        try {
            intervals[INT_I][SOFT_INT] = parseFloat(settings.getString("softIntervalI", "1"));
        } catch (NumberFormatException e) {
            intervals[INT_I][SOFT_INT] = 1;
            parsingErrors += "El intervalo suave de I es incorrecto.\n";
        }
        try {
            intervals[INT_I][HARD_INT] = parseFloat(settings.getString("hardIntervalI", "5"));
        } catch (NumberFormatException e) {
            intervals[INT_I][HARD_INT] = 5;
            parsingErrors += "El intervalo fuerte de I es incorrecto.\n";
        }
        try {
            intervals[INT_D][SOFT_INT] = parseFloat(settings.getString("softIntervalD", "1"));
        } catch (NumberFormatException e) {
            intervals[INT_D][SOFT_INT] = 1;
            parsingErrors += "El intervalo suave de D es incorrecto.\n";
        }
        try {
            intervals[INT_D][HARD_INT] = parseFloat(settings.getString("hardIntervalD", "5"));
        } catch (NumberFormatException e) {
            intervals[INT_D][HARD_INT] = 5;
            parsingErrors += "El intervalo fuerte de D es incorrecto.\n";
        }
        if (parsingErrors.length() > 0) {
            parsingErrors += "Se han tomado los valores por defecto, revise los ajustes.";
            Toast.makeText(PIDManager.this, parsingErrors, Toast.LENGTH_LONG).show();
        }

    }

    private void setInitialValues(){
        SharedPreferences sharedPref = PIDManager.this.getPreferences(Context.MODE_PRIVATE);
        txtP.setText(sharedPref.getString("txtP", "0.0"));
        txtI.setText(sharedPref.getString("txtI", "0.0"));
        txtD.setText(sharedPref.getString("txtD", "0.0"));
        txtX.setText(sharedPref.getString("txtX", "0"));
        seekX.setProgress(Integer.parseInt(txtX.getText().toString())+500);
        txtV.setText(sharedPref.getString("txtV", "0"));
        seekV.setProgress(Integer.parseInt(txtV.getText().toString()));
        txtS.setText(sharedPref.getString("txtS", "0"));
        seekS.setProgress(Integer.parseInt(txtS.getText().toString()));
    }

    public void onChangeButton(View view) {

        TextView txt = null;
        float val = 0;
        char type = ' ';
        switch (view.getId()) {
            case R.id.softUpP:
                type = 'P';
                txt = txtP;
                val = intervals[INT_P][SOFT_INT];
                break;
            case R.id.softUpI:
                type = 'I';
                txt = txtI;
                val = intervals[INT_I][SOFT_INT];
                break;
            case R.id.softUpD:
                type = 'D';
                txt = txtD;
                val = intervals[INT_D][SOFT_INT];
                break;
            case R.id.hardUpP:
                type = 'P';
                txt = txtP;
                val = intervals[INT_P][HARD_INT];
                break;
            case R.id.hardUpI:
                type = 'I';
                txt = txtI;
                val = intervals[INT_I][HARD_INT];
                break;
            case R.id.hardUpD:
                type = 'D';
                txt = txtD;
                val = intervals[INT_D][HARD_INT];
                break;
            case R.id.softDownP:
                type = 'P';
                txt = txtP;
                val = -(intervals[INT_P][SOFT_INT]);
                break;
            case R.id.softDownI:
                type = 'I';
                txt = txtI;
                val = -(intervals[INT_I][SOFT_INT]);
                break;
            case R.id.softDownD:
                type = 'D';
                txt = txtD;
                val = -(intervals[INT_D][SOFT_INT]);
                break;
            case R.id.hardDownP:
                type = 'P';
                txt = txtP;
                val = -(intervals[INT_P][HARD_INT]);
                break;
            case R.id.hardDownI:
                type = 'I';
                txt = txtI;
                val = -(intervals[INT_I][HARD_INT]);
                break;
            case R.id.hardDownD:
                type = 'D';
                txt = txtD;
                val = -(intervals[INT_D][HARD_INT]);
                break;
        }
        if (txt != null) {
            float anterior = Float.parseFloat(txt.getText().toString());
            txt.setText(String.valueOf(round(anterior + val, 3)));
            saveSharedPrefs("txt"+String.valueOf(type), txt.getText().toString());
        }

    }

    private SeekBar.OnSeekBarChangeListener seekXlistener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            txtX.setText(String.valueOf(progress-500));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int val= (int)(Math.rint((double) seekBar.getProgress() / 10) * 10);
            if(Math.abs(val) > (float)(seekBar.getMax()))return;
            seekBar.setProgress(val);
            txtX.setText(String.valueOf(val-500));
            saveSharedPrefs("txtX", txtX.getText().toString());
        }
    };

    private SeekBar.OnSeekBarChangeListener seekVlistener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            txtV.setText(String.valueOf(progress));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int val= (int)(Math.rint((double) seekBar.getProgress() / 10) * 10);
            if(val > seekBar.getMax())return;
            seekBar.setProgress(val);
            txtV.setText(String.valueOf(val));
            saveSharedPrefs("txtV", txtV.getText().toString());
        }
    };

    private SeekBar.OnSeekBarChangeListener seekSlistener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            txtS.setText(String.valueOf(progress));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int val= (int)(Math.rint((double) seekBar.getProgress() / 10) * 10);
            if(val > seekBar.getMax())return;
            seekBar.setProgress(val);
            txtS.setText(String.valueOf(val));
            saveSharedPrefs("txtS", txtS.getText().toString());
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toggleRun:
                run = !run;
                if (run) {
                    item.setIcon(R.drawable.ic_stop_black_48dp);
                } else {
                    item.setIcon(R.drawable.ic_play_arrow_black_48dp);
                }
                break;


            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return true;
    }

    private void saveSharedPrefs(String key, String value){
        SharedPreferences sharedPref = PIDManager.this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public float round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.floatValue();
    }
}
