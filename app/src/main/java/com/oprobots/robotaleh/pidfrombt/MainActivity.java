package com.oprobots.robotaleh.pidfrombt;

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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Lista de controles principales
        ImageView logoMain;

//        Inicializado de los controles principales
        logoMain = (ImageView) findViewById(R.id.imageMain);


//        Asignación de los listeners a los controles
        assignListeners(logoMain);
    }

    private void assignListeners(ImageView logoMain) {
        logoMain.setOnLongClickListener(longClickInfo);
    }

    private View.OnLongClickListener longClickInfo = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            final SpannableString gitLink = new SpannableString("Es un proyecto Open-Source principalmente orientado a la calibración del PID " +
                    "en robots siguelíneas o similares, aunque también se puede emplear en cualquier proyecto que " +
                    "haga uso de dicho algoritmo.\n\nDesarrollado por AlexSantos.\n"+"http://github.com/robotaleh/PIDfromBT");
            Linkify.addLinks(gitLink, Linkify.WEB_URLS);

            final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setIcon(R.drawable.logo_opr);
            alertDialog.setTitle("PIDfromBT");
            alertDialog.setMessage(gitLink);
            alertDialog.show();
            try{
                ((TextView)alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
            }catch (java.lang.NullPointerException e){
                Log.e("Alert", e.getMessage());
            }
            return false;
        }
    };
}
