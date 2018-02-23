package com.oprobots.robotaleh.pidfrombt;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntro2Fragment;
import com.ogaclejapan.smarttablayout.SmartTabLayout;

/**
 * Created by alexe on 05/01/2018.
 */

public class IntroActivity extends AppIntro2 {

    private ViewPager pager;
    private SmartTabLayout indicator;
    private Button skip;
    private Button next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(AppIntro2Fragment.newInstance(getResources().getString(R.string.bienvenido), getResources().getString(R.string.userNuevo),R.drawable.main_img, getResources().getColor(R.color.background)));
        addSlide(AppIntro2Fragment.newInstance(getResources().getString(R.string.encontrarVincular), getResources().getString(R.string.parPulsaciones),R.drawable.on_boarding_03_screenshot_list, getResources().getColor(R.color.background)));
        addSlide(AppIntro2Fragment.newInstance(getResources().getString(R.string.listarConectar), getResources().getString(R.string.pulsarConfigurar),R.drawable.on_boarding_04_screenshot_connect, getResources().getColor(R.color.background)));
        addSlide(AppIntro2Fragment.newInstance(getResources().getString(R.string.configuraTodo), getResources().getString(R.string.constantesConfig),R.drawable.on_boarding_10_screenshot_settings_01, getResources().getColor(R.color.background)));


        showSkipButton(false);

    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        finishOnboarding();
    }

    private void finishOnboarding() {
        // Get the shared preferences
        SharedPreferences preferences =
                getSharedPreferences("PIDfromBTsettings", MODE_PRIVATE);

        // Set onboarding_complete to true
        preferences.edit()
                .putBoolean("onboarding_complete",true).apply();

        // Launch the main Activity, called MainActivity
        Intent main = new Intent(this, MainActivity.class);
        startActivity(main);

        // Close the OnboardingActivity
        finish();
    }
}
