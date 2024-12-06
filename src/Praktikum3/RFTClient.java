package Praktikum3;
/* RFTClient.java
 Version 1.0 
 Praktikum Rechnernetze HAW Hamburg
 Autoren: 
 */
import java.io.*;
import java.net.*;

public class RFTClient extends Thread {
	/* --------- Constants ------------ */
	public final int SERVER_PORT = 53480;
	public final int UDP_PACKET_SIZE = 1008;  // 1000 Byte data + 8 Byte seq num
	public final int CONNECTION_TIMEOUT = 5000; // milliseconds
	public final static double X = 0.125;

	/* -------- Public parms ----------- */
	public String sourcePath;
	public String destPath;
	public long windowSize = 10000L;
	public long serverErrorRate = 1000L;
	public boolean fastRetransmitMode = false;
	public boolean testOutputMode = false;

	/* -------- Variables -------------- */

	// Current timeoutInterval in nanoseconds
	// TCP start value: 1 second [RFC 6298]
	public long timeoutInterval = 1000000000L;
	private double estimatedRTT = -1;
	private double deviation = 0;
	
	private long sampleRTT_Sum = 0;
	private long measuredRTTs = 0;

	public DatagramSocket clientSocket;
	public RFTSendBuffer sendBuf;

	private long nextSeqNum = 0;

	// -------- Streams
	private FileInputStream inFromFile;

	// Threads
	public RFTClientRcvThread rcvThread;
	public RFT_Timer rft_timer;

	// -------- Packet structures
	private DatagramPacket udpPacket;
	private RFTpacket rftSendPacket;
	private InetAddress ipAddress;

	// Sequence number: reserve 8 bytes of UDP packet
	byte[] sendData = new byte[UDP_PACKET_SIZE - 8];

	// destination adress
	public String servername;

	// Konstruktor
	public RFTClient(String serverArg, String sourcePathArg, String destPathArg, String windowSizeArg,
			String errorRateArg, String fastRetransmitModeArg, String testOutputModeArg) {
		servername = serverArg;
		sourcePath = sourcePathArg;
		destPath = destPathArg;
		windowSize = Long.parseLong(windowSizeArg);
		serverErrorRate = Long.parseLong(errorRateArg);
		if (fastRetransmitModeArg.equalsIgnoreCase("true")) {
			fastRetransmitMode = true;
		}
		if (testOutputModeArg.equalsIgnoreCase("true")) {
			testOutputMode = true;
		}

	}

	public void run() {
		long performanceStarttime;
		long performanceResult = 0;

		try {
			// open file
			inFromFile = new FileInputStream(sourcePath);
		} catch (IOException e) {
			System.err.println("File " + sourcePath + " not found!");
			return;
		}

		try {
			System.err.println("Starting RFT Client!");
			sendBuf = new RFTSendBuffer(this);

			clientSocket = new DatagramSocket(); // socket will be bound to any (source-)port
			ipAddress = InetAddress.getByName(servername);

			// Start ACK receive thread
			rcvThread = new RFTClientRcvThread(this);
			// rcvThread.setDaemon(true); // stop if main thread is finished
			rcvThread.setPriority(Thread.MAX_PRIORITY);
			rcvThread.start();

			// Start Timer thread
			rft_timer = new RFT_Timer(this);
			rft_timer.start();

			// --------- start of sending task ------------------------
			// First packet contains parameters for the server (handshake)
			rftSendPacket = makeControlPacket();
			sendPacket(rftSendPacket, false); // send control packet
			nextSeqNum = 0;

			// Start of performance measurement
			performanceStarttime = System.currentTimeMillis();

			/* Start RFT */
			int len;
			while ((len = inFromFile.read(sendData)) > 0) {
				testOut("Client: Bytes read from file: " + len);
				// make send packet
				rftSendPacket = new RFTpacket(nextSeqNum, sendData, len);
				sendBuf.enter(rftSendPacket); // may cause delay
				// send regular packet --> set timestamp for RTT measurement
				sendPacket(rftSendPacket, true);
				nextSeqNum = nextSeqNum + rftSendPacket.getLen();
				// start timer for detection of erroneous or lost packet, if not running
				rft_timer.startTimer(timeoutInterval, false);

			}
			sendBuf.waitForEmptyBuffer();
			// --------- end of protocol ------------------------
			rcvThread.interrupt();
			rft_timer.interrupt();
			// End of performance measurement
			performanceResult = System.currentTimeMillis() - performanceStarttime;
			clientSocket.close();
			inFromFile.close();
		} catch (IOException e) {
			System.err.println("Internal Error! " + e.toString());
			System.exit(-1);
		}

		// User information
      System.err.println("\n-------------- Results --------------");
		System.out.println(performanceResult + " milliseconds transmission time "
				+ "\n" + rft_timer.getTimeoutCounter() + " Timeouts" 
				+ "\n\n" + windowSize + " Windows Size "
				+ "\n" + serverErrorRate + " Error Rate "			
				+ "\n" + fastRetransmitMode + " FastRetransmit-Mode"
				+ "\n" + nextSeqNum + " Bytes for File " + sourcePath + " sent" );
		System.err.println("\nSuccessfully finished!\n");
	}

	public synchronized void sendPacket(RFTpacket rftSendPacket, boolean setTimestamp) {
		/* Create and send new UDP packet and start timer */
		udpPacket = new DatagramPacket(rftSendPacket.getSeqNumBytesAndData(), rftSendPacket.getLen() + 8, ipAddress,
				SERVER_PORT);

		// set timestamp for RTT measurement
		if (setTimestamp) {
			rftSendPacket.setTimestamp(System.nanoTime());
		} else {
			rftSendPacket.setTimestamp(-1); // no RTT measurement!
		}
		try {
			// send packet physically via UDP
			clientSocket.send(udpPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Unexspected Socket Error! " + e.toString());
			System.exit(-1);
		}
		testOut("Client: Packet " + rftSendPacket.getSeqNum() + " with " + rftSendPacket.getLen() + " bytes sent!");
	}

	/**
	 * Implementation specific task performed at timeout
	 */
	public void timeoutTask() {
     
     /* ToDo */
     
	}

	public void computeTimeoutInterval(long sampleRTT) {
      /* Computes the current timeoutInterval (in nanoseconds) 
       * Result: Variable timeoutInterval */

      /* ToDo */
      
	}

	public RFTpacket makeControlPacket() {
		/*
		 * Create first packet with seq num -1. Return value: RFTPacket with (-1
		 * destPath ; serverWindowSize ; errorRate)
		 */
		String sendString = destPath + ";" + windowSize + ";" + serverErrorRate + ";" + testOutputMode;
		byte[] sendData = null;

		try {
			sendData = sendString.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		testOut("Control packet of " + sendData.length + " Byte: " + sendString);
		return new RFTpacket(-1, sendData, sendData.length);
	}

	public void testOut(String out) {
		// Print String if test output mode
		if (testOutputMode) {
			//System.err.printf("%.2f %s: %s\n", System.nanoTime() / 1000000.0, Thread.currentThread().getName(), out);
			System.err.printf("%d %s: %s\n", System.currentTimeMillis(), Thread.currentThread().getName(), out);			
			
		}
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length < 7) {
			System.out.println(" RFTClient - Filetransfer zu einem Server\n Argumente:\n"
					+ "    1 Hostname des Servers\n"
					+ "    2 Quellpfad (inkl. Dateiname) der zu sendenden lokalen Datei\n"
					+ "    3 Zielpfad (inkl. Dateiname) der zu empfangenden Datei (falls bereits vorhanden, wird die Datei ueberschrieben)\n"
					+ "    4 Window Size (Sendepuffergroesse = Empfangspuffergroesse) in Anzahl Bytes\n"
					+ "    5 Fehlerrate zur Auswertung fuer den Server\n" + "    6 FastRetransmitMode (true/false)\n"
					+ "    7 Testoutput-Mode (true/false)\n");

		} else {
			RFTClient myClient = new RFTClient(argv[0], argv[1], argv[2], argv[3], argv[4], argv[5], argv[6]);
			myClient.start();
		}
	}
}
