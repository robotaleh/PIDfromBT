package com.oprobots.robotaleh.pidfrombt;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by alexe on 05/01/2018.
 */

public class OnboardingFragment2 extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle s) {
        //        final ImageView image = (ImageView) rootView.findViewById(R.id.imgSearchPair);
//        new Timer().scheduleAtFixedRate(new TimerTask(){
//            @Override
//            public void run(){
//                if(image.getDrawable() == getResources().getDrawable(R.drawable.screenshot_pairing_mask)){
//                    image.setImageDrawable(getResources().getDrawable(R.drawable.screenshot_searching_mask));
//                }else{
//                    image.setImageDrawable(getResources().getDrawable(R.drawable.screenshot_pairing_mask));
//                }
//
//            }
//        },0,5000);
        return inflater.inflate(R.layout.onboarding_screen2, container, false);

    }
}
