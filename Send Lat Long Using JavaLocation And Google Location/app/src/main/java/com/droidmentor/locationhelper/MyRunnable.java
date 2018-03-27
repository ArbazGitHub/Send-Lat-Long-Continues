package com.droidmentor.locationhelper;

/**
 * Created by thetaubuntu5 on 28/11/17.
 */

public class MyRunnable implements Runnable {
    private boolean killMe = false;

    public void run() {
        if (killMe)
            return;

      /* do your work */
    }

    private void killRunnable() {
        killMe = true;
    }
}
