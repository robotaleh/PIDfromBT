package com.oprobots.robotaleh.pidfrombt;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Button;

import com.ogaclejapan.smarttablayout.SmartTabLayout;

/**
 * Created by alexe on 05/01/2018.
 */

public class OnboardingActivity extends FragmentActivity {

    private final int NUM_FRAGMENTS = 7;
    private ViewPager pager;
    private SmartTabLayout indicator;
    private Button skip;
    private Button next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        pager = (ViewPager) findViewById(R.id.pager);
        indicator = (SmartTabLayout) findViewById(R.id.indicator);
        skip = (Button) findViewById(R.id.skip);
        next = (Button) findViewById(R.id.next);

        final Fragment frag1 = new OnboardingFragment1();
        final Fragment frag2 = new OnboardingFragment2();
        final Fragment frag3 = new OnboardingFragment3();
        final Fragment frag4 = new OnboardingFragment4();
        final Fragment frag5 = new OnboardingFragment5();
        final Fragment frag6 = new OnboardingFragment6();
        final Fragment frag7 = new OnboardingFragment7();

        FragmentStatePagerAdapter adapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return frag1;
                    case 1:
                        return frag2;
                    case 2:
                        return frag3;
                    case 3:
                        return frag4;
                    case 4:
                        return frag5;
                    case 5:
                        return frag6;
                    case 6:
                        return frag7;
                    default:
                        return null;
                }
            }

            @Override
            public int getCount() {
                return NUM_FRAGMENTS;
            }
        };
        pager.setAdapter(adapter);
        indicator.setViewPager(pager);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishOnboarding();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (pager.getCurrentItem() == NUM_FRAGMENTS - 1) { // The last screen
                    finishOnboarding();
                } else {
                    pager.setCurrentItem(
                            pager.getCurrentItem() + 1,
                            true
                    );
                }
            }
        });
        indicator.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position == NUM_FRAGMENTS - 1) {
                    skip.setVisibility(View.GONE);
                    next.setText("Hecho");
                } else {
                    skip.setVisibility(View.VISIBLE);
                    next.setText("Siguiente");
                }
            }
        });
    }

    private void finishOnboarding() {
        // Get the shared preferences
        SharedPreferences preferences =
                getSharedPreferences("PIDfromBTsettings", MODE_PRIVATE);

        // Set onboarding_complete to true
        preferences.edit()
                .putBoolean("onboarding_complete", true).apply();

        // Launch the main Activity, called MainActivity
        Intent main = new Intent(this, MainActivity.class);
        startActivity(main);

        // Close the OnboardingActivity
        finish();
    }
}
