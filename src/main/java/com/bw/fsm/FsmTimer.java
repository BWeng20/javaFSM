package com.bw.fsm;

import java.util.TimerTask;

public class FsmTimer extends TimerTask {

    private final Runnable runnable;

    public FsmTimer(Runnable r) {
        this.runnable = r;
    }

    @Override
    public void run() {
        runnable.run();
    }
}
