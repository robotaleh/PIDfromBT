package com.oprobots.robotaleh.pidfrombt;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {


    private BluetoothAdapter BTAdapter = null;
    private ListView listPaired;
    private Set pairedDevices;
    private ArrayList foundDevices = new ArrayList();
    private final int BLUETOOTH = 0;
    private ProgressDialog findNewBTProgress;
    private final int ACT_PAIRED = 1;
    private final int ACT_SEARCH = 2;


    boolean DEBUG = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the shared preferences
        SharedPreferences preferences = getSharedPreferences("PIDfromBTsettings", MODE_PRIVATE);

// Check if onboarding_complete is false
        if (!preferences.getBoolean("onboarding_complete", false)) {
            // Start the onboarding Activity
            Intent onboarding = new Intent(this, OnboardingActivity.class);
            startActivity(onboarding);

            // Close the main Activity
            finish();
            return;
        }
        preferences.edit().putBoolean("onboarding_complete", false).apply();
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
        assignListeners(logoMain, listPaired, btnListPaired, btnSearch);

//        Regustra un Receiver para tener control de las acciones del Adaptador BT
        IntentFilter filt = new IntentFilter();
        filt.addAction(BluetoothDevice.ACTION_FOUND);
        filt.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filt.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filt.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        this.registerReceiver(mReceiver, filt);

    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(mReceiver);

        } catch (IllegalArgumentException e) {

        }
        super.onDestroy();
    }

    /**
     * Asigna los listeners de las acciones principales
     *
     * @param logoMain      Imagen de portada de la APP. Muestra información del proyecto.
     * @param listPaired    Lista de dispositivos BT pareados o encontrados. Se usa para conectarse a un BT o emparejar uno nuevo.
     * @param btnListPaired Botón para actualizar la lista de dispositivos vinculados.
     * @param btnSearch     Botón para realizar una búsqueda de nuevos dispositivos BT no vinculados.
     */
    private void assignListeners(ImageView logoMain, ListView listPaired, Button btnListPaired, Button btnSearch) {
        if (logoMain != null) logoMain.setOnLongClickListener(longClickInfo);
        if (listPaired != null) listPaired.setOnItemClickListener(listPairedItemClickListener);
        if (listPaired != null)
            listPaired.setOnItemLongClickListener(listPairedItemLongClickListener);
        if (btnListPaired != null) btnListPaired.setOnClickListener(listPairedBTsClickListener);
        if (btnSearch != null) btnSearch.setOnClickListener(searchNewBTDevicesClickListener);
    }

    /**
     * Inicializa el adaptador de BT, pidiéndole al usuario que lo active en caso de no estarlo ya.
     */
    private void initBT(int ACT_BT) {
        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        if (BTAdapter == null) {
            //Muestra un mensaje de que el dispositivo no tiene BT y finaliza la aplicación
            Toast.makeText(getApplicationContext(), "Dispositivo Bluetooth no disponible.", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            if (BTAdapter.isEnabled()) {
                if (ACT_BT == ACT_PAIRED) {
                    pairedDevicesList();
                } else {
                    searchNewBTDevices();
                }
            } else {
                //Ask to the user turn the bluetooth on
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon, BLUETOOTH);
            }
        }
    }

    /**
     * Lista los dispositivos BT ya vinculados para poder seleccionar a cual conectarse posteriormente.
     */
    private void pairedDevicesList() {
        if (BTEnabled()) {
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

            final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_selectable_list_item, list);
            listPaired.setAdapter(adapter);
            listPaired.setOnItemClickListener(listPairedItemClickListener);
            listPaired.setOnItemLongClickListener(listPairedItemLongClickListener);
        } else {
            initBT(ACT_PAIRED);
        }
    }

    /**
     * Lista los resultados de la búsqueda de nuevos dispositivos BT para su vinculación.
     *
     * @param finished Indica si ya se ha finalizado la búsqueda para mostrar un mensaje final en caso de no haber encontrado dispositivos nuevos.
     */
    private void foundDevicesList(boolean finished) {

        if (foundDevices.size() == 0 && finished) {
            Toast.makeText(getApplicationContext(), "No se han encontrado Dispositivos Bluetooth disponibles.", Toast.LENGTH_SHORT).show();
        }
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_selectable_list_item, foundDevices);
        listPaired.setAdapter(adapter);
        listPaired.setOnItemClickListener(foundListItemClickListener);
        listPaired.setOnLongClickListener(null);
    }

    /**
     * Inicia una búsqueda de nuevos dispositivos BT.
     * Nota: Si ya estuviese en modo búsqueda, lo cancela para iniciarlo de nuevo.
     */
    private void searchNewBTDevices() {
        if (BTEnabled()) {
            if (BTAdapter.isDiscovering()) {
                // El Bluetooth ya está en modo discover, lo cancelamos para iniciarlo de nuevo
                BTAdapter.cancelDiscovery();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
                switch (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    case PackageManager.PERMISSION_DENIED:
                        final SpannableString permissionLink = new SpannableString("Para encontrar dispositivos BT cercanos pulse \"Permitir\" en el siguiente cuadro de diálogo y pruebe de nuevo.\n\n" +
                                "Para más información:  http://developer.android.com/about/versions/marshmallow/android-6.0-changes.html#behavior-hardware-id");
                        Linkify.addLinks(permissionLink, Linkify.WEB_URLS);
                        final int REQUEST_ACCESS_COARSE_LOCATION = 1;
                        ((TextView) new AlertDialog.Builder(this)
                                .setIcon(R.drawable.logo_opr)
                                .setCancelable(false)
                                .setTitle("Permisos en tiempo de ejecución.")
                                .setMessage(permissionLink)
                                .setPositiveButton("Entiendo", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                            ActivityCompat.requestPermissions(MainActivity.this,
                                                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                                    REQUEST_ACCESS_COARSE_LOCATION);
                                        }
                                    }
                                })
                                .show()
                                .findViewById(android.R.id.message))
                                .setMovementMethod(LinkMovementMethod.getInstance());
                        break;
                    case PackageManager.PERMISSION_GRANTED:
                        BTAdapter.startDiscovery();
                        break;
                }
            } else {
                BTAdapter.startDiscovery();
            }
        } else {
            initBT(ACT_SEARCH);
        }
    }

    /**
     * Receiver para registrar todas las acciones que realize el Adaptador BT
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Se ha encontrado un dispositivo Bluetooth
                // Se obtiene la información del dispositivo del intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                foundDevices.add(device.getName() + "\n" + device.getAddress());
                foundDevicesList(false);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                foundDevicesList(true);
                if (findNewBTProgress != null && findNewBTProgress.isShowing()) {
                    findNewBTProgress.dismiss();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                foundDevices.clear();
                foundDevicesList(false);
                findNewBTProgress = new ProgressDialog(MainActivity.this);
                findNewBTProgress.setTitle("Buscando Dispositivos Bluetooth");
                findNewBTProgress.setIcon(R.drawable.logo_opr);
                findNewBTProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                findNewBTProgress.setMessage("Espere hasta que el proceso finalice...");
                findNewBTProgress.setCancelable(false);
                findNewBTProgress.show();
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    Toast.makeText(MainActivity.this, "Emparejado", Toast.LENGTH_SHORT).show();
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    Toast.makeText(MainActivity.this, "Desemparejado", Toast.LENGTH_SHORT).show();
                    if (BTAdapter.isDiscovering()) {
                        BTAdapter.cancelDiscovery();
                    }
                }
                pairedDevicesList();
            }
        }
    };

    /**
     * Comprueba si el BT está habilitado y activo en el dispositivo.
     *
     * @return Boolean indicando el estado del BT.
     */
    private boolean BTEnabled() {
        return BTAdapter != null && BTAdapter.isEnabled();
    }

    /**
     * Listener de pulsación larga sobre la imagen de portada de la APP.
     * Muestra información del proyecto: nombre del autor, contacto en Twitter, GitHub, e información varia.
     */
    private View.OnLongClickListener longClickInfo = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            final SpannableString gitLink = new SpannableString("Es un proyecto Open-Source de OPRobots principalmente orientado a la calibración del PID " +
                    "en robots siguelíneas o similares, aunque también se puede emplear en cualquier proyecto que " +
                    "haga uso de dicho algoritmo.\n\nDesarrollado por Alex Santos (@robotaleh).\n" + "http://github.com/robotaleh/PIDfromBT");
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

    /**
     * Listener de pulsación normal sobre el botón de actualización de lista de BTs vinculados.
     */
    private View.OnClickListener listPairedBTsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            pairedDevicesList();
        }
    };

    /**
     * Listener de pulsación normal sobre el botón de búsqueda de nuevos BTs para vincular.
     */
    private View.OnClickListener searchNewBTDevicesClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            searchNewBTDevices();
        }
    };

    /**
     * Listener de pulsación normal sobre un item de la lista de BTs vinculados para conectarse a él.
     */
    private AdapterView.OnItemClickListener listPairedItemClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView av, View v, int arg2, long arg3) {
            if (BTEnabled()) {
                // Get the device MAC address, the last 17 chars in the View
                String info = ((TextView) v).getText().toString();
                String address = info.substring(info.length() - 17);
                // Make an intent to start next activity.
                Intent i = new Intent(MainActivity.this, PIDManager.class);
                //Change the activity.
                if (!DEBUG)
                    i.putExtra("BT_ADDRESS", address);
                startActivity(i);
            } else {
                initBT(ACT_PAIRED);
            }

        }
    };

    /**
     * Listener de pulsación normal sobre un item de la lista de BTs nuevos no vinculados para emparejarlo.
     */
    private AdapterView.OnItemClickListener foundListItemClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView av, View v, int arg2, long arg3) {
            // Get the device MAC address, the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            System.out.println(address);
            BluetoothDevice device = BTAdapter.getRemoteDevice(address);
            try {
                Method method = device.getClass().getMethod("createBond", (Class[]) null);
                method.invoke(device, (Object[]) null);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Se ha producido un error:\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * Listener de pulsación larga sobre un item de la lista de BTs vinculados para desemparejarlo.
     */
    private AdapterView.OnItemLongClickListener listPairedItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);

            BluetoothDevice device = BTAdapter.getRemoteDevice(address);
            try {
                Method m = device.getClass()
                        .getMethod("removeBond", (Class[]) null);
                m.invoke(device, (Object[]) null);
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error al Desemparejar", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
    };
}
