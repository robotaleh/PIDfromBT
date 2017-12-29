package com.oprobots.robotaleh.pidfrombt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.Image;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {


    private BluetoothAdapter BTAdapter = null;
    private ListView listPaired;
    private Set pairedDevices;
    private final int BLUETOOTH = 0;


    boolean DEBUG = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

//        Lista de controles principales
        ImageView logoMain;
        Button btnListPaired, btnSearch;

//        Inicializado de los controles principales
        logoMain = (ImageView) findViewById(R.id.imageMain);
        btnListPaired = (Button) findViewById(R.id.btnListar);
        btnSearch = (Button) findViewById(R.id.btnBuscar);
        listPaired = (ListView) findViewById(R.id.listBT);


//        Asignación de los listeners a los controles
        assignListeners(logoMain, listPaired, btnListPaired);

    }

    private void assignListeners(ImageView logoMain, ListView listPaired, Button btnListPaired) {
        if (logoMain != null) logoMain.setOnLongClickListener(longClickInfo);
        if (listPaired != null) listPaired.setOnItemClickListener(listPairedItemClickListener);
        if (btnListPaired != null) btnListPaired.setOnClickListener(listPairedBTsClickListener);
    }

    private void initBT() {
        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (BTAdapter == null) {
            //Muestra un mensaje de que el dispositivo no tiene BT y finaliza la aplicación
            Toast.makeText(getApplicationContext(), "Dispositivo Bluetooth no disponible.", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            if (BTAdapter.isEnabled()) {
                pairedDevicesList();
            } else {
                //Ask to the user turn the bluetooth on
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon, BLUETOOTH);
            }
        }
    }

    private void pairedDevicesList() {
        if (BTAdapter != null && BTAdapter.isEnabled()) {
            if (BTAdapter.isDiscovering()) {
                BTAdapter.cancelDiscovery();
            }
            pairedDevices = BTAdapter.getBondedDevices();
            ArrayList list = new ArrayList();

            if (pairedDevices.size() > 0) {
                for (Object btO : pairedDevices) {
                    BluetoothDevice bt = (BluetoothDevice) btO;
                    list.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address
                }
            } else {
                Toast.makeText(getApplicationContext(), "No se han encontrado Dispositivos Bluetooth Vinculados.", Toast.LENGTH_SHORT).show();
            }

            final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
            listPaired.setAdapter(adapter);
        } else {
            initBT();
        }
    }

    private View.OnLongClickListener longClickInfo = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            final SpannableString gitLink = new SpannableString("Es un proyecto Open-Source principalmente orientado a la calibración del PID " +
                    "en robots siguelíneas o similares, aunque también se puede emplear en cualquier proyecto que " +
                    "haga uso de dicho algoritmo.\n\nDesarrollado por AlexSantos.\n" + "http://github.com/robotaleh/PIDfromBT");
            Linkify.addLinks(gitLink, Linkify.WEB_URLS);

            final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setIcon(R.drawable.logo_opr);
            alertDialog.setTitle("PIDfromBT");
            alertDialog.setMessage(gitLink);
            alertDialog.show();
            try {
                ((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
            } catch (java.lang.NullPointerException e) {
                Log.e("Alert", e.getMessage());
            }
            return false;
        }
    };

    private View.OnClickListener listPairedBTsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            pairedDevicesList();
        }
    };

    private AdapterView.OnItemClickListener listPairedItemClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView av, View v, int arg2, long arg3) {
            // Get the device MAC address, the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            // Make an intent to start next activity.
            /*Intent i = new Intent(MainActivity.this, PIDmanager.class);
            //Change the activity.
            if(!DEBUG)
                i.putExtra("EXTRA_ADDRESS", address); //this will be received at ledControl (class) Activity

            startActivity(i);*/
            Toast.makeText(MainActivity.this, address, Toast.LENGTH_SHORT).show();
        }
    };
}
