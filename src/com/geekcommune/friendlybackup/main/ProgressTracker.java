package com.geekcommune.friendlybackup.main;

public class ProgressTracker {

	private String message;
    private int progress;
    private int totalSteps;
    private boolean finished;

	public ProgressTracker(int numSteps) {
		this.totalSteps = numSteps;
	}

	public synchronized String getStatusMessage() {
		return message;
	}

    public synchronized boolean isFinished() {
        return this.finished;
    }

    public synchronized void setFinished(boolean finished) {
        this.finished = finished;
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
		this.totalSteps = this.progress + i;
	}

	public synchronized void progress(int i) {
		this.progress += i;
	}

	public synchronized int stepsRemaining() {
	    return this.totalSteps - this.progress;
	}
}
