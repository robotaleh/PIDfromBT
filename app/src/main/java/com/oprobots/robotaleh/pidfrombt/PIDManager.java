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
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    // Lista de controles principales
    private TextView console, txtP, txtI, txtD, txtX, txtV, txtS;
    private SeekBar seekX, seekV, seekS;
    private LinearLayout layoutS;

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
        assignListeners(seekX, seekV, seekS);

        // Inicia la conexión
        initBT();
    }

    private void assignListeners(SeekBar seekX, SeekBar seekV, SeekBar seekS) {
        seekX.setOnSeekBarChangeListener(seekXlistener);
        seekV.setOnSeekBarChangeListener(seekVlistener);
        seekS.setOnSeekBarChangeListener(seekSlistener);
    }

    @Override
    protected void onResume() {
        getSettings();
        super.onResume();
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
                    Toast.makeText(getBaseContext(), "Conexión finalizada.", Toast.LENGTH_SHORT).show();
            } catch (IOException ignored) {
            }
            finish(); //return to the first layout }
        }
    }

    private void getSettings() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(PIDManager.this);
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

    private void setInitialValues() {
        SharedPreferences sharedPref = PIDManager.this.getPreferences(Context.MODE_PRIVATE);
        txtP.setText(sharedPref.getString("txtP", "0.0"));
        txtI.setText(sharedPref.getString("txtI", "0.0"));
        txtD.setText(sharedPref.getString("txtD", "0.0"));
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

            case R.id.load:
                loadCurrentConfig();
                break;
            case R.id.save:
                saveCurrentConfig();
                break;
            case R.id.delete:
                deleteConfig();
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

    private void loadCurrentConfig() {
        if (configs.isEmpty()) {
            Toast.makeText(getApplicationContext(), "No hay configuraciones para cargar.", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builderLoad = new AlertDialog.Builder(PIDManager.this);
        builderLoad.setIcon(R.drawable.logo_opr);
        builderLoad.setTitle("Seleccione la configuración que desea cargar");
        int selectedIndex = -1;
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(PIDManager.this, android.R.layout.simple_list_item_single_choice);
//                arrayAdapter.add("Hardik");
        for (int i = 0; i < configs.size(); i++) {
            arrayAdapter.add(configs.get(i));
            if (configs.get(i).equals(lastConfig)) {
                selectedIndex = i;
            }
        }

        builderLoad.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
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
                        }
                    }, 200);
                } else {
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(), "No se han podido recuperar los datos guardados.", Toast.LENGTH_LONG).show();
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
                .setTitle("Nombre")
                .setIcon(R.drawable.logo_opr)
                .setMessage("Indique el nombre de la configuración que desea guardar.")
                .setView(editName)
                .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
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
                                Toast.makeText(getApplicationContext(), "El nombre no puede estar en blanco, ni contener ';'.\nInténtelo de nuevo.", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "El nombre ya se encuentra asignado a otra configuración.\nInténtelo de nuevo con otro nombre.", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    private void deleteConfig() {
        if (configs.isEmpty()) {
            Toast.makeText(getApplicationContext(), "No hay configuraciones para borrar.", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builderDelete = new AlertDialog.Builder(PIDManager.this);
        builderDelete.setIcon(R.drawable.logo_opr);
        builderDelete.setTitle("Seleccione la configuración que desea eliminar");

        final ArrayAdapter<String> arrayAdapterDel = new ArrayAdapter<>(PIDManager.this, android.R.layout.simple_list_item_single_choice);
        for (int i = 0; i < configs.size(); i++) {
            arrayAdapterDel.add(configs.get(i));
        }

        builderDelete.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
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
            progress = ProgressDialog.show(PIDManager.this, "Conectando...", "Por favor, espere");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try {
                if (BTSocket == null || !isBTConnected) {
                    BTAdapter = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = BTAdapter.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    BTSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    BTSocket.connect();//start connection
                }
            } catch (IOException e) {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("La conexión ha fallado. Es un Bluetooth SPP? Inténtelo de nuevo.");
                finish();
            } else {
                msg("Conectado.");
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


        private GetMsg(){
        }

        byte[] readBuffer = new byte[1024];
        int readBufferPosition =0;
        @Override
        protected Boolean doInBackground(InputStream... btSocket) {

            do {

                InputStream inputStream = btSocket[0];
                try {
                    int bytesAvailable = inputStream.available();
                    if(bytesAvailable > 0)
                    {
                        byte[] packetBytes = new byte[bytesAvailable];
                        inputStream.read(packetBytes);
                        for(int i=0;i<bytesAvailable;i++)
                        {
                            byte b = packetBytes[i];
                            if(b == 10)
                            {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;

                                manageReceive(data);
                            }
                            else
                            {
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

    private void manageReceive(String msg) {
        msg = Normalizer.normalize(msg, Normalizer.Form.NFC);
        Log.e("Receive", msg);
        if(commandHistory.size()>=maxCommandHistory){
            for (int i=0;i<=commandHistory.size()-maxCommandHistory;i++){
                commandHistory.remove(i);
            }
        }
        commandHistory.add(msg);
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                try{
                    console.setText(TextUtils.join("\n", commandHistory));
                }catch (ConcurrentModificationException ignored){

                }
            }
        });
    }

    public void manageSend(String msg) {
        Log.e("Send", msg);
        try {
            BTSocket.getOutputStream().write(msg.getBytes());
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Error:\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
