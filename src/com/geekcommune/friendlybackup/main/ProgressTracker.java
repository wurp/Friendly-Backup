package com.geekcommune.friendlybackup.main;

import java.util.ArrayList;
import java.util.List;

import com.geekcommune.util.Pair;

public class ProgressTracker {

	private String message;
    private int progress;
    private int totalSteps;
    //list of sub-trackers and the number of supertracker steps each represents
    private List<Pair<ProgressTracker, Integer>> subTrackers = new ArrayList<Pair<ProgressTracker,Integer>>();

	public ProgressTracker(int numSteps) {
		this.totalSteps = numSteps;
	}

	public synchronized String getStatusMessage() {
		return message;
	}

    public synchronized boolean isFinished() {
        return stepsRemaining() < 1;
    }

    public synchronized void setFinished(boolean finished) {
        rebase(0);
    }

	public synchronized boolean isFailed() {
		// TODO Auto-generated method stub
		return false;
	}

	public synchronized void changeMessage(String message, int progressSinceLastMessage) {
		this.message = message;
		progress(progressSinceLastMessage);
	}

	/**
	 * Reset tracker so steps remaining is 'i'
	 * @param i - new steps remaining
	 */
	public synchronized void rebase(int i) {
		this.totalSteps = getProgress() + i;
	}

	public synchronized void progress(int i) {
		this.progress += i;
		
		if( stepsRemaining() < 0 ) {
		    throw new RuntimeException("More progress logged than the total number of steps...");
		}
	}

	public synchronized int stepsRemaining() {
	    return this.totalSteps - getProgress();
	}

	public synchronized int getTotalSteps() {
	    return this.totalSteps;
	}
	
	public synchronized int getProgress() {
	    int retval = this.progress;
	    for(Pair<ProgressTracker, Integer> pt : subTrackers) {
	        int subtotal = pt.getFirst().getTotalSteps();
	        int subprog = pt.getFirst().getProgress();
	        retval += pt.getSecond() * (subprog / subtotal);
	    }
	    
	    return retval;
	}
	
    public ProgressTracker createSubTracker(int i) {
        ProgressTracker retval = new ProgressTracker(i);
        subTrackers.add(new Pair<ProgressTracker,Integer>(retval, i));
        return retval;
    }
}
