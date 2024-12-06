package Praktikum3;/* RFT_Timer.java
 Version 1.0
 Praktikum Rechnernetze HAW Hamburg
 Autor: M. Huebner
 */

public class RFT_Timer extends Thread {
	/* Special timer for RFT_Clients */
	private RFTClient myRFTC;
	private long delay;
	private boolean isRunning;
	private boolean taskIsCancelled;
	private boolean isRestarted;
	private long timeoutCounter;

	/**
	 * Constructor for RFT_Timer-Objects
	 * 
	 * RFTClient must implement a method public void timeoutTask()
	 * This method is called after the given timeoutInterval (delay), if timerTask is
	 * not cancelled
	 */
	public RFT_Timer(RFTClient rftClient) {
		this.myRFTC = rftClient;
		this.isRunning = false;
		this.taskIsCancelled = false;
		this.isRestarted = false;
		this.delay = 0;
		this.timeoutCounter = 0;
	}

	public synchronized void run() {
		/*
		 * Timer thread sleeps until delay is over (--> timeoutTask - call!) or is
		 * cancelled
		 */
		while (!isInterrupted()) {
			try {
				// Timer State: Stopped
				while (!isRunning) {
					//myRFTC.testOut(" RFT_Timer stopped!");
					wait();
				}
				isRestarted = true;

				// Timer State: Running
				while (isRestarted && delay > 0) {
					taskIsCancelled = false;
					isRestarted = false;
					long millis = delay / 1000000L;
					int nanos = (int) (delay % 1000000L);
					//myRFTC.testOut("------ Going to wait for " + millis + "," + nanos + "  isRunning: " + isRunning);
					wait(millis, nanos);

					/* Timeout: Perform task if not cancelled */
					if (!taskIsCancelled) {
						timeoutCounter++;
						myRFTC.testOut("------ Timeout detected!");
						myRFTC.timeoutTask();
					}
				}
				isRunning = false;
			} catch (InterruptedException e) {
				/* Timer thread terminated */
				this.interrupt();
				myRFTC.testOut(" RFT_Timer interrupted!");
			}
		}
	}

	public synchronized void startTimer(long timeoutInterval, boolean restartIfRunning) {
		/* TimeoutInterval (delay) must be given in nanoseconds [see System.nanoTime()] */
		if (!isRunning) {
			/* Start Timer if not already running */
			delay = timeoutInterval;
			isRunning = true;
			notify();
			//myRFTC.testOut("------------------ RFT_Timer started: " + timeoutInterval);
		} else {
			if (restartIfRunning) {
				/* Start Timer if already running */
				delay = timeoutInterval;
				taskIsCancelled = true;
				isRestarted = true;
				notify();
				//myRFTC.testOut("------------------ RFT_Timer restarted: " + timeoutInterval);
			}
		}
	}

	public synchronized void cancelTimer() {
		/* Cancel timer task and stop timer if already running */
		if (isRunning) {
			taskIsCancelled = true;
			notify();
			//myRFTC.testOut("------------------ RFT_Timer cancelled!");
		}
	}

	public synchronized long getTimeoutCounter() {
		return timeoutCounter;
	}
}
