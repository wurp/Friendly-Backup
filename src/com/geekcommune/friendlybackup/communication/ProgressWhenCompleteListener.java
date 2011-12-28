package com.geekcommune.friendlybackup.communication;

import com.geekcommune.communication.message.Message;
import com.geekcommune.communication.message.StateListener;
import com.geekcommune.friendlybackup.main.ProgressTracker;

public class ProgressWhenCompleteListener implements StateListener {

    private ProgressTracker progressTracker;
    private int numStepsWhenComplete;
    private boolean done;

    public ProgressWhenCompleteListener(ProgressTracker progressTracker, int numStepsWhenComplete) {
        this.progressTracker = progressTracker;
        this.numStepsWhenComplete = numStepsWhenComplete;
        this.done = false;
    }

    public void stateChanged(Message msg) {
        if( !done && msg.isComplete() ) {
            progressTracker.progress(numStepsWhenComplete);
        }
    }
}
