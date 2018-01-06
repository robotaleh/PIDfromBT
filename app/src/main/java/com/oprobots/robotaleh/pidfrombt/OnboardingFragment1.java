package com.oprobots.robotaleh.pidfrombt;

import android.os.Handler;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

/**
 * Created by alexe on 05/01/2018.
 */

public class OnboardingFragment1 extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle s) {
        final View rootView = inflater.inflate(R.layout.onboarding_screen1, container, false);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final ImageView image = (ImageView) rootView.findViewById(R.id.swipeHand);
                image.setX(image.getX()+50);
//                image.animate().alpha(1);

                TranslateAnimation anim = new TranslateAnimation(0, -150, 0, 0);
                anim.setDuration(1000);
                anim.setRepeatCount(-1);
                anim.setRepeatMode(Animation.REVERSE);
                anim.setInterpolator(new AccelerateInterpolator());
                anim.setFillAfter(true);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        image.animate().alpha(1);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                image.startAnimation(anim);
            }
        }, 3000);
        return rootView;

    }

}
