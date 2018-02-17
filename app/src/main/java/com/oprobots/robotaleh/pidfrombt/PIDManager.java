package com.oprobots.robotaleh.pidfrombt;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.ActionMenuItemView;
import android.text.Html;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.UUID;

import static java.lang.Float.parseFloat;

public class PIDManager extends AppCompatActivity {

    private boolean run = false;
    private boolean stopOnShake = true;

    private int maxCommandHistory;
    private ArrayList<String> commandHistory = new ArrayList<>();

    private String address = null;
    private ProgressDialog progress;
    private BluetoothAdapter BTAdapter = null;
    private BluetoothSocket BTSocket = null;
    private boolean isBTConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ConnectBT bt;
    private GetMsg get;

    private float[][] intervals = new float[3][2];
    private final int INT_P = 0;
    private final int INT_I = 1;
    private final int INT_D = 2;
    private final int HARD_INT = 0;
    private final int SOFT_INT = 1;

    private ArrayList<String> configs = new ArrayList<>();
    private String lastConfig = null;

    // ShakeDetector
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;
    private boolean isCalibrating;

    // Lista de controles principales
    private TextView console, txtP, txtI, txtD, txtX, txtV, txtS;
    private SeekBar seekX, seekV, seekS;
    private LinearLayout layoutS;
    private float shakeThresholdGravity;

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
        layoutS = (LinearLayout) findViewById(R.id.layoutS);

        // Obtener los ajustes
        getSettings();

        // Establece los valores iniciales: los últimos insertados
        setInitialValues();

        // Asigna los Listeners a los elementos
        assignListeners(seekX, seekV, seekS, txtP, txtI, txtD);

        // Inicia la conexión
        initBT();

        // Inicia el detector de movimiento
        initShakeDetector();
    }

    private void assignListeners(SeekBar seekX, SeekBar seekV, SeekBar seekS, TextView txtP, TextView txtI, TextView txtD) {
        seekX.setOnSeekBarChangeListener(seekXlistener);
        seekV.setOnSeekBarChangeListener(seekVlistener);
        seekS.setOnSeekBarChangeListener(seekSlistener);

        txtP.setOnClickListener(txtClickListener);
        txtI.setOnClickListener(txtClickListener);
        txtD.setOnClickListener(txtClickListener);
    }

    @Override
    protected void onResume() {
        getSettings();
        sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
        super.onResume();
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(shakeDetector);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (BTSocket != null && bt != null) //If the BTSocket is busy
        {
            try {
                if (get != null)
                    get.cancel(true);
                BTSocket.close(); //close connection

                if (get != null)
                    Toast.makeText(getBaseContext(), getText(R.string.connectionFinished), Toast.LENGTH_SHORT).show();
            } catch (IOException ignored) {
            }
            finish(); //return to the first layout }
        }
        sensorManager.unregisterListener(shakeDetector);
    }

    private void getSettings() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(PIDManager.this);
        SharedPreferences.Editor settingsEditor = settings.edit();
        stopOnShake = settings.getBoolean("stopOnShake", true);
        maxCommandHistory = Integer.parseInt(settings.getString("maxCommandHistory", "150"));
        boolean hasSuction = settings.getBoolean("hasSuction", true);
        if (hasSuction) {
            layoutS.setVisibility(View.VISIBLE);
        } else {
            layoutS.setVisibility(View.GONE);
        }
        String parsingErrors = "";
        try {
            intervals[INT_P][SOFT_INT] = parseFloat(settings.getString("softIntervalP", "1"));
        } catch (NumberFormatException e) {
            intervals[INT_P][SOFT_INT] = 1;
            settingsEditor.putString("softIntervalP", "1");
            parsingErrors += getString(R.string.softIntPErr) + "\n";
        }
        try {
            intervals[INT_P][HARD_INT] = parseFloat(settings.getString("hardIntervalP", "5"));
        } catch (NumberFormatException e) {
            intervals[INT_P][HARD_INT] = 5;
            settingsEditor.putString("hardIntervalP", "5");
            parsingErrors += getString(R.string.hardIntPErr) + "\n";
        }
        try {
            intervals[INT_I][SOFT_INT] = parseFloat(settings.getString("softIntervalI", "1"));
        } catch (NumberFormatException e) {
            intervals[INT_I][SOFT_INT] = 1;
            settingsEditor.putString("softIntervalI", "1");
            parsingErrors += getString(R.string.softIntIErr) + "\n";
        }
        try {
            intervals[INT_I][HARD_INT] = parseFloat(settings.getString("hardIntervalI", "5"));
        } catch (NumberFormatException e) {
            intervals[INT_I][HARD_INT] = 5;
            settingsEditor.putString("hardIntervalI", "5");
            parsingErrors += getString(R.string.hardIntIErr) + "\n";
        }
        try {
            intervals[INT_D][SOFT_INT] = parseFloat(settings.getString("softIntervalD", "1"));
        } catch (NumberFormatException e) {
            intervals[INT_D][SOFT_INT] = 1;
            settingsEditor.putString("softIntervalD", "1");
            parsingErrors += getString(R.string.softIntDErr) + "\n";
        }
        try {
            intervals[INT_D][HARD_INT] = parseFloat(settings.getString("hardIntervalD", "5"));
        } catch (NumberFormatException e) {
            intervals[INT_D][HARD_INT] = 5;
            settingsEditor.putString("hardIntervalD", "5");
            parsingErrors += getString(R.string.hardIntDErr) + "\n";
        }
        if (parsingErrors.length() > 0) {
            settingsEditor.apply();
            parsingErrors += getString(R.string.defaultValuesErr);
            Toast.makeText(PIDManager.this, parsingErrors, Toast.LENGTH_LONG).show();
        }


    }

    private void setInitialValues() {
        SharedPreferences sharedPref = PIDManager.this.getPreferences(Context.MODE_PRIVATE);
        txtP.setText(sharedPref.getString("txtP", "0"));
        txtI.setText(sharedPref.getString("txtI", "0"));
        txtD.setText(sharedPref.getString("txtD", "0"));
        txtX.setText(sharedPref.getString("txtX", "0"));
        seekX.setProgress(Integer.parseInt(txtX.getText().toString()) + 500);
        txtV.setText(sharedPref.getString("txtV", "0"));
        seekV.setProgress(Integer.parseInt(txtV.getText().toString()));
        txtS.setText(sharedPref.getString("txtS", "0"));
        seekS.setProgress(Integer.parseInt(txtS.getText().toString()));

        // Lista de nombres de configuraciones guardadas
        String[] names = sharedPref.getString("configs", "").split(";");
        for (String name : names) {
            if (!name.equals(""))
                configs.add(name);
        }
        shakeThresholdGravity = sharedPref.getFloat("shakeThresholdGravity", 1.5f);
    }

    private void initBT() {
        Intent intBT = getIntent();
        address = intBT.getStringExtra("BT_ADDRESS");
        if (address != null) {
            if (bt == null) {
                bt = new ConnectBT();
                bt.execute();
            }
        }
    }

    private void initShakeDetector() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) {
            Toast.makeText(PIDManager.this, getText(R.string.sensorManagerError), Toast.LENGTH_SHORT).show();
            return;
        }
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        shakeDetector = new ShakeDetector();
        shakeDetector.setShakeThresholdGravity(shakeThresholdGravity);
        shakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {

            @Override
            public void onShake(int count) {
                if (stopOnShake && !isCalibrating) {
                    emergecyStop();
                }
            }
        });
        sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
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
            lastConfig = null; // Anula el registro de última config cargada al modificar algún campo
            saveSharedPrefs("txt" + String.valueOf(type), txt.getText().toString());
            if (run)
                manageSend(String.valueOf(type) + (txt.getText().toString()));
        }

    }

    private SeekBar.OnSeekBarChangeListener seekXlistener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            txtX.setText(String.valueOf(progress - 500));
            if (run && fromUser)
                manageSend("X" + (txtX.getText().toString()));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int val = (int) (Math.rint((double) seekBar.getProgress() / 10) * 10);
            if (Math.abs(val) > (float) (seekBar.getMax())) return;
            seekBar.setProgress(val);
            txtX.setText(String.valueOf(val - 500));
            if (run)
                manageSend("X" + (txtX.getText().toString()));
            saveSharedPrefs("txtX", txtX.getText().toString());
            lastConfig = null; // Anula el registro de última config cargada al modificar algún campo
        }
    };

    private SeekBar.OnSeekBarChangeListener seekVlistener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            txtV.setText(String.valueOf(progress));
            if (run && fromUser)
                manageSend("V" + (txtV.getText().toString()));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int val = (int) (Math.rint((double) seekBar.getProgress() / 10) * 10);
            if (val > seekBar.getMax()) return;
            seekBar.setProgress(val);
            txtV.setText(String.valueOf(val));
            if (run)
                manageSend("V" + (txtV.getText().toString()));
            saveSharedPrefs("txtV", txtV.getText().toString());
            lastConfig = null; // Anula el registro de última config cargada al modificar algún campo
        }
    };

    private SeekBar.OnSeekBarChangeListener seekSlistener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            txtS.setText(String.valueOf(progress));
            if (run && fromUser)
                manageSend("S" + (txtS.getText().toString()));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int val = (int) (Math.rint((double) seekBar.getProgress() / 10) * 10);
            if (val > seekBar.getMax()) return;
            seekBar.setProgress(val);
            txtS.setText(String.valueOf(val));
            if (run)
                manageSend("S" + (txtS.getText().toString()));
            saveSharedPrefs("txtS", txtS.getText().toString());
            lastConfig = null; // Anula el registro de última config cargada al modificar algún campo
        }
    };

    TextView.OnClickListener txtClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View txt) {
            final AlertDialog.Builder builderTxtValue = new AlertDialog.Builder(PIDManager.this);

            builderTxtValue.setIcon(R.drawable.logo_opr);
            char type = ' ';
            switch (txt.getId()) {
                case R.id.txtP:
                    type = 'P';
                    builderTxtValue.setTitle(Html.fromHtml("<font color='#c62828'>"+getString(R.string.firstP)+"</font><font color='#ef5350'>"+getString(R.string.lastP)+":</font>"));
                    break;
                case R.id.txtI:
                    type = 'I';
                    builderTxtValue.setTitle(Html.fromHtml("<font color='#00c853'>"+getString(R.string.firstI)+"</font><font color='#81c784'>"+getString(R.string.lastI)+":</font>"));
                    break;
                case R.id.txtD:
                    type = 'D';
                    builderTxtValue.setTitle(Html.fromHtml("<font color='#1565c0'>"+getString(R.string.firstD)+"</font><font color='#7986cb'>"+getString(R.string.lastD)+":</font>"));
                    break;
            }
            final char typeFinal = type;

            final EditText input = new EditText(PIDManager.this);
            input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setKeyListener(DigitsKeyListener.getInstance("0123456789.-"));
            builderTxtValue.setView(input);
            input.setText(((TextView) txt).getText());
            input.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);


            builderTxtValue.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        float val = parseFloat(input.getText().toString());
                        ((TextView) txt).setText(String.valueOf(round(val, 3)));
                        if (run)
                            manageSend(String.valueOf(typeFinal) + (((TextView) txt).getText().toString()));
                    } catch (NumberFormatException e) {
                        Toast.makeText(PIDManager.this, getString(R.string.numberFormatError), Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builderTxtValue.setNegativeButton(getString(R.string.cancelDialog), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });


//            builderTxtValue.show();
            final AlertDialog dialog = builderTxtValue.create();
            input.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        Log.i("KEY", String.valueOf(keyCode));
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_ENTER:
                            case KeyEvent.KEYCODE_DPAD_CENTER:
                                try {
                                    float val = parseFloat(input.getText().toString());
                                    ((TextView) txt).setText(String.valueOf(round(val, 3)));
                                    if (run)
                                        manageSend(String.valueOf(typeFinal) + (((TextView) txt).getText().toString()));
                                } catch (NumberFormatException e) {
                                    Toast.makeText(PIDManager.this, getString(R.string.numberFormatError), Toast.LENGTH_SHORT).show();
                                }
                                dialog.dismiss();
                                return true;
                            default:
                                break;
                        }
                    }
                    return false;
                }
            });
            dialog.show();
            input.requestFocus();
            input.selectAll();
            input.postDelayed(new Runnable() {

                @Override
                public void run() {
                    InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (keyboard != null) {
                        keyboard.showSoftInput(input, 0);
                    }
                }
            }, 200);
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
                    sendPIDIVS();
                } else {
                    item.setIcon(R.drawable.ic_play_arrow_black_48dp);
                    sendStop();
                }
                break;

            case R.id.load:
                loadCurrentConfig();
                break;
            case R.id.save:
                saveCurrentConfig();
                break;
            case R.id.delete:
                deleteConfig();
                break;
            case R.id.calibrateThreshold:
                new AlertDialog.Builder(PIDManager.this)
                        .setTitle(getString(R.string.calibrationIntroTitle))
                        .setIcon(R.drawable.logo_opr)
                        .setMessage(getString(R.string.calibrationIntro))
                        .setPositiveButton(getString(R.string.acceptDialog), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                final Handler handlerCalibration = new Handler();
                                handlerCalibration.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        isCalibrating = true;
                                        new CalibrateThreshold().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                    }
                                }, 600);
                            }
                        })
                        .setNegativeButton(getString(R.string.cancelDialog), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                            }
                        })
                        .show();
                break;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return true;
    }

    private void saveSharedPrefs(String key, String value) {
        SharedPreferences sharedPref = PIDManager.this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private void saveSharedPrefs(String key, Float value) {
        SharedPreferences sharedPref = PIDManager.this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    private void loadCurrentConfig() {
        if (configs.isEmpty()) {
            Toast.makeText(getApplicationContext(), getString(R.string.noConfigsToLoad), Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builderLoad = new AlertDialog.Builder(PIDManager.this);
        builderLoad.setIcon(R.drawable.logo_opr);
        builderLoad.setTitle(getString(R.string.selectConfigToLoad));
        int selectedIndex = -1;
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(PIDManager.this, android.R.layout.simple_list_item_single_choice);
        for (int i = 0; i < configs.size(); i++) {
            arrayAdapter.add(configs.get(i));
            if (configs.get(i).equals(lastConfig)) {
                selectedIndex = i;
            }
        }

        builderLoad.setNegativeButton(getText(R.string.cancelDialog), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderLoad.setSingleChoiceItems(arrayAdapter, selectedIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                String config = arrayAdapter.getItem(which);
                lastConfig = config;
                SharedPreferences sharedPref = PIDManager.this.getPreferences(Context.MODE_PRIVATE);
                String pidv = sharedPref.getString(config, "");
                if (!pidv.equals("")) {
                    String[] pidvARR = pidv.split(";");
                    txtP.setText(pidvARR[0]);
                    txtI.setText(pidvARR[1]);
                    txtD.setText(pidvARR[2]);
                    txtV.setText(pidvARR[3]);
                    seekV.setProgress(Integer.parseInt(pidvARR[3]));
                    txtX.setText(pidvARR[4]);
                    seekX.setProgress(Integer.parseInt(pidvARR[4]) + 500);
                    txtS.setText(pidvARR[5]);
                    seekS.setProgress(Integer.parseInt(pidvARR[5]));

//                    if (run)
//                        enviarPIDV();
                    SharedPreferences sharedPrefSave = PIDManager.this.getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPrefSave.edit();
                    editor.putString("txtP", txtP.getText().toString());
                    editor.putString("txtI", txtI.getText().toString());
                    editor.putString("txtD", txtD.getText().toString());
                    editor.putString("txtV", txtV.getText().toString());
                    editor.putString("txtX", txtX.getText().toString());
                    editor.putString("txtS", txtS.getText().toString());
                    editor.apply();


                    final Handler handlerP = new Handler();
                    handlerP.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            if(run){
                                sendPIDIVS();
                            }
                        }
                    }, 200);
                } else {
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(), getString(R.string.loadConfigError), Toast.LENGTH_LONG).show();
                }
            }

        });
        builderLoad.show();
    }

    private void saveCurrentConfig() {
        final EditText editName = new EditText(getApplicationContext());
        editName.setInputType(InputType.TYPE_CLASS_TEXT);
        editName.setTextColor(getResources().getColor(R.color.black));
        editName.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.nameDialog))
                .setIcon(R.drawable.logo_opr)
                .setMessage(getString(R.string.configNameToSave))
                .setView(editName)
                .setPositiveButton(getString(R.string.acceptDialog), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String name = editName.getText().toString();
                        if (!configs.contains(name)) {
                            if (!name.equals("") && !name.contains(";")) {
                                SharedPreferences sharedPref = PIDManager.this.getPreferences(Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPref.edit();
                                // Guarda todos los ajustes separados por ';'
                                editor.putString(name, txtP.getText().toString() + ";" + txtI.getText().toString() + ";" + txtD.getText().toString() + ";" + String.valueOf(seekV.getProgress()) + ";" + String.valueOf(seekX.getProgress() - 500) + ";" + String.valueOf(seekS.getProgress()));
                                // Guarda una lista de todos los nombres de configuraciones para recuperarlos más tarde
                                editor.putString("configs", sharedPref.getString("configs", "") + (((sharedPref.getString("configs", "")).equals("")) ? name : (";" + name)));
                                configs.add(name);
                                lastConfig = name;
                                editor.apply();
                            } else {
                                Toast.makeText(getApplicationContext(), getString(R.string.configSpecialCharsError)+"\n"+getString(R.string.configTryAgainName), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), getString(R.string.configAlreadyUsedNameError)+"\n"+getString(R.string.configTryAgainName), Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancelDialog), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    private void deleteConfig() {
        if (configs.isEmpty()) {
            Toast.makeText(getApplicationContext(), getString(R.string.noConfigsToDelete), Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builderDelete = new AlertDialog.Builder(PIDManager.this);
        builderDelete.setIcon(R.drawable.logo_opr);
        builderDelete.setTitle(getString(R.string.selectConfigToDelete));

        final ArrayAdapter<String> arrayAdapterDel = new ArrayAdapter<>(PIDManager.this, android.R.layout.simple_list_item_single_choice);
        for (int i = 0; i < configs.size(); i++) {
            arrayAdapterDel.add(configs.get(i));
        }

        builderDelete.setNegativeButton(getString(R.string.cancelDialog), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderDelete.setAdapter(arrayAdapterDel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String config = arrayAdapterDel.getItem(which);
                SharedPreferences sharedPref = PIDManager.this.getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.remove(config);
                editor.remove("configs");
                configs.remove(config);
                String configsString = "";
                for (int i = 0; i < configs.size(); i++) {
                    configsString += ((i > 0) ? "" : ";") + configs.get(i);
                }
                editor.putString("configs", configsString);
                editor.apply();
            }
        });
        builderDelete.show();
    }

    public float round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    @SuppressLint("StaticFieldLeak")
    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        private ConnectBT() {

        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(PIDManager.this);
            progress.setTitle(getString(R.string.connectingBT));
            progress.setMessage(getString(R.string.plaseWait));
            progress.setIcon(R.drawable.logo_opr);
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try {
                if (BTSocket == null || !isBTConnected) {
                    BTAdapter = BluetoothAdapter.getDefaultAdapter();//Obtiene el adaptador BT del móvil
                    BluetoothDevice dispositivo = BTAdapter.getRemoteDevice(address);//Conecta con la dirección del dispositivo y comprueba si está disponible
                    BTSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//Crea una conexion RFCOMM (SPP)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    BTSocket.connect();//Inicia la conexión
                }
            } catch (IOException e) {
                ConnectSuccess = false;//Captura cualquier error en la conexión
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg(getString(R.string.BtConnectionError));
                finish();
            } else {
                msg(getString(R.string.connectedBT));
                isBTConnected = true;
                try {
                    get = new GetMsg();
                    get.execute(BTSocket.getInputStream());
                } catch (IOException e) {
                    msg("ERROR: " + e.getMessage());
                }
            }
            //progress.dismiss();
            try {
                if ((progress != null) && progress.isShowing()) {
                    progress.dismiss();
                }
            } catch (final Exception e) {
                // Handle or log or ignore
            } finally {
                progress = null;
            }
        }

        private void msg(String s) {
            Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class GetMsg extends AsyncTask<InputStream, String, Boolean> {


        private GetMsg() {
        }

        byte[] readBuffer = new byte[1024];
        int readBufferPosition = 0;

        @Override
        protected Boolean doInBackground(InputStream... btSocket) {

            do {

                InputStream inputStream = btSocket[0];
                try {
                    int bytesAvailable = inputStream.available();
                    if (bytesAvailable > 0) {
                        byte[] packetBytes = new byte[bytesAvailable];
                        inputStream.read(packetBytes);
                        for (int i = 0; i < bytesAvailable; i++) {
                            byte b = packetBytes[i];
                            if (b == 10) {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;

                                manageReceive(data);
                            } else {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                } catch (IOException e) {
                    Toast.makeText(null, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } while (btSocket[0] != null && !isCancelled());
            return false;
        }

        @Override
        protected void onProgressUpdate(String... values) {

        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onPostExecute(Boolean result) {

        }

        @Override
        protected void onCancelled() {

        }
    }

    @SuppressLint("StaticFieldLeak")
    class CalibrateThreshold extends AsyncTask<Void, Void, Float>{

        ProgressDialog progress;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress = new ProgressDialog(PIDManager.this);
            progress.setTitle(getString(R.string.shakeToCalibrate));
            progress.setIcon(R.drawable.logo_opr);
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setMax(1000);
            progress.setCancelable(false);
            progress.setProgress(0);
            progress.setProgressNumberFormat(null);
            isCalibrating = true;
            progress.show();
        }


        @Override
        protected Float doInBackground(Void... arg0) {
            float maxGForce = 1.3f;
            for (int i = 0; i < progress.getMax(); i ++) {
                try {
                    Thread.sleep(1);
                    progress.incrementProgressBy(1);
                    float gForce = shakeDetector.gForce;
                    if(gForce>maxGForce){
                        maxGForce = gForce;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return maxGForce;
        }


        @Override
        protected void onPostExecute(Float result) {
            super.onPostExecute(result);
            Log.i("gForce", String.valueOf(result));
            shakeThresholdGravity = round(result, 2);
            shakeDetector.setShakeThresholdGravity(shakeThresholdGravity);
            saveSharedPrefs("shakeThresholdGravity", shakeThresholdGravity);
            progress.dismiss();
            isCalibrating = false;
        }
    }

    @SuppressLint("RestrictedApi")
    private void emergecyStop() {
        if(run){
            run = false;
            ActionMenuItemView stop = (ActionMenuItemView) findViewById(R.id.toggleRun);
            stop.setIcon(getDrawable(R.drawable.ic_play_arrow_black_48dp));
            sendStop();
        }
    }

    private void sendPIDIVS() {
        final Handler handlerX = new Handler();
        handlerX.postDelayed(new Runnable() {
            @Override
            public void run() {
                    manageSend("X" + String.valueOf(seekX.getProgress()-500));

            }
        }, 0);
        final Handler handlerP = new Handler();
        handlerP.postDelayed(new Runnable() {
            @Override
            public void run() {
                    manageSend("P" + (txtP.getText().toString()));

            }
        }, 150);
        final Handler handlerI = new Handler();
        handlerI.postDelayed(new Runnable() {
            @Override
            public void run() {
                manageSend("I" + (txtI.getText().toString()));
            }
        }, 300);
        final Handler handlerD = new Handler();
        handlerD.postDelayed(new Runnable() {
            @Override
            public void run() {
                manageSend("D" + (txtD.getText().toString()));
            }
        }, 450);
        final Handler handlerV = new Handler();
        handlerV.postDelayed(new Runnable() {
            @Override
            public void run() {
                manageSend("V" + String.valueOf(seekV.getProgress()));
            }
        }, 600);
        final Handler handlerS = new Handler();
        handlerS.postDelayed(new Runnable() {
            @Override
            public void run() {
                manageSend("S" + String.valueOf(seekS.getProgress()));
            }
        }, 750);
    }

    private void sendStop(){
        final Handler handlerV = new Handler();
        handlerV.postDelayed(new Runnable() {
            @Override
            public void run() {
                manageSend("V0");

            }
        }, 150);
        final Handler handlerP = new Handler();
        handlerP.postDelayed(new Runnable() {
            @Override
            public void run() {
                manageSend("P0");
            }
        }, 300);
        final Handler handlerI = new Handler();
        handlerI.postDelayed(new Runnable() {
            @Override
            public void run() {
                manageSend("I0");
            }
        }, 450);
        final Handler handlerD = new Handler();
        handlerD.postDelayed(new Runnable() {
            @Override
            public void run() {
                manageSend("D0");
            }
        }, 600);
        final Handler handlerS = new Handler();
        handlerS.postDelayed(new Runnable() {
            @Override
            public void run() {
                manageSend("S0");
            }
        }, 750);
    }

    private void manageReceive(String msg) {
        msg = Normalizer.normalize(msg, Normalizer.Form.NFC);
        Log.e("Receive", msg);
        if (commandHistory.size() >= maxCommandHistory) {
            for (int i = 0; i <= commandHistory.size() - maxCommandHistory; i++) {
                commandHistory.remove(i);
            }
        }
        commandHistory.add(msg);
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                try {
                    console.setText(TextUtils.join("\n", commandHistory));
                } catch (ConcurrentModificationException ignored) {

                }
            }
        });
    }

    public void manageSend(String msg) {
        Log.e("Send", msg);
        if (BTSocket == null) return;
        try {
            BTSocket.getOutputStream().write(msg.getBytes());
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Error:\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
