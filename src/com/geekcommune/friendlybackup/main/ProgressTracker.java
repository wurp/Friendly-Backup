package com.geekcommune.friendlybackup.main;

public class ProgressTracker {

	private String message;

	public ProgressTracker(int numSteps) {
		// TODO Auto-generated constructor stub
	}

	public String getStatusMessage() {
		return message;
	}

	public synchronized boolean isFinished() {
		// TODO Auto-generated method stub
		return false;
	}

	public synchronized boolean isFailed() {
		// TODO Auto-generated method stub
		return false;
	}

	public void changeMessage(String message, int progressSinceLastMessage) {
		this.message = message;
		progress(progressSinceLastMessage);
	}

	public void rebase(int i) {
		// TODO Auto-generated method stub
		
	}

	public void progress(int i) {
		// TODO Auto-generated method stub
		
	}

}
