package Praktikum3;/* RFTSendBuffer.java
 Version 1.0
 Praktikum Rechnernetze HAW Hamburg
 Autor: M. Huebner
 */

import java.util.*;

public class RFTSendBuffer {
	private LinkedList<RFTpacket> buffer;
	private RFTClient myRFTC;

	// -------- Protocol variables
	private long BUFFER_SIZE; // Maximum Bytes
	private long curBufferSize; // Currently allocated Bytes

	public RFTSendBuffer(RFTClient rftclient) {
		myRFTC = rftclient;
		buffer = new LinkedList<RFTpacket>();
		BUFFER_SIZE = myRFTC.windowSize;
		curBufferSize = 0;
	}

	// sender thread calls method ENTER
	public synchronized void enter(RFTpacket packet) {
		// if buffer full ==> Wait!
		while (curBufferSize + packet.getLen() > BUFFER_SIZE) {
			myRFTC.testOut("SendBuffer: waiting because buffer full (window size)!");
			try {
				wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}

		buffer.add(packet);
		curBufferSize = curBufferSize + packet.getLen();

		myRFTC.testOut("SendBuffer: Packet added with seqNum " + packet.getSeqNum());
	}

	// receiver thread calls method REMOVE
	// remove from the beginning all packets with seqNum < sendbase
	public synchronized void remove(long sendbase) {
		// if buffer empty ==> Error!
		if (buffer.isEmpty()) {
			myRFTC.testOut("SendBuffer: ERROR! Buffer empty!");
		}
		while (!buffer.isEmpty() && (buffer.getFirst().getSeqNum() < sendbase)) {
			myRFTC.testOut("SendBuffer: Removed packet " + buffer.getFirst().getSeqNum());
			curBufferSize = curBufferSize - buffer.getFirst().getLen();
			buffer.removeFirst();
		}

		// release buffer and awake waiting send thread
		notify();
	}

	public synchronized void waitForEmptyBuffer() {
		/* Sender thread has to wait until send buffer is empty */
		while (!buffer.isEmpty()) {
			try {
				myRFTC.testOut("SendBuffer: waiting because buffer not empty!");
				wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public synchronized RFTpacket getSendbasePacket() {
		/*
		 * Returns the first packet in this list (or null if buffer is empty)
		 */
		RFTpacket sendbase = null;
		if (!buffer.isEmpty()) {
			sendbase = buffer.getFirst();
		}
		return sendbase;
	}

}
