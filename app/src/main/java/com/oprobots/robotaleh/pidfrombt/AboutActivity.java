package com.oprobots.robotaleh.pidfrombt;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

/**
 * Created by @robotaleh on 21/01/2018.
 */

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String version = "";
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        View aboutPage = new AboutPage(this)
                .isRTL(false)
                .setDescription(getString(R.string.aboutPIDfromBT)+"\n\n"+getString(R.string.developedBy))
                .setImage(R.drawable.main_img)
//                        .addItem(adsElement)
                .addGroup("Contacto")
                .addEmail("alex.santos.ete@gmail.com")
                .addTwitter("robotaleh", "Twitter Álex Santos")
                .addTwitter("oprobots", "Twitter OPRobots")
                .addGroup("Más información")
//                .addPlayStore("com.ideashower.readitlater.pro")
                .addGitHub("robotaleh", "Ver otros proyectos en GitHub")
                .addGitHub("robotaleh/PIDfromBT", "Ver el proyecto en GitHub")
                .addItem(new Element().setTitle("Version "+version).setIconDrawable(R.drawable.ic_code_black_48dp).setIconTint(R.color.colorPrimary))
                .create();
        setContentView(aboutPage);
    }
}
