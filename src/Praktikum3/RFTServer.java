package Praktikum3;
/* RFTServer.java
 Version 1.0
 Praktikum Rechnernetze HAW Hamburg
 Autor: Prof. Dr.-Ing. M. Huebner
 */
import java.io.*;

import java.net.*;
import java.util.Collections;
import java.util.LinkedList;

public class RFTServer {
	// -------- Constants
	public final static int SERVER_PORT = 53480;
	public final static int UDP_PACKET_SIZE = 1008;
	public final static int CONNECTION_TIMEOUT = 3000; // milliseconds

	// -------- Parameters (will be adjusted with values in the first packet)
	public String destPath = "";
	private long rcvWindow; // Free Bytes in receive buffer
	public long errorRate = 10000;
	public boolean testOutputMode = false;

	// -------- Socket structures
	private InetAddress clientAdress = null; // connection state
	private int clientPort = -1; // connection state
	private DatagramSocket serverSocket;
	private byte[] receiveData;
	private LinkedList<RFTpacket> recBuf;

	// -------- Streams
	private FileOutputStream outToFile;

	// Protocol variables
	private long rcvbase = 0;

	private int recPacketCounter = 0;
	private int deliveredCounter = 0;
	private int corruptedCounter = 0;

	// Constructor
	public RFTServer() {
		receiveData = new byte[UDP_PACKET_SIZE];
	}

	public void runRFTServer() throws IOException {
		InetAddress receivedIPAddress;
		int receivedPort;
		DatagramPacket udpReceivePacket;
		RFTpacket rftReceivePacket;
		boolean handshake = false; // protocol state info
		boolean connectionEstablished = false; // protocol state info
		boolean running = true;

		serverSocket = new DatagramSocket(SERVER_PORT);
		System.err.println("Waiting for connection using port " + SERVER_PORT);

		while (running) {
			try {
				udpReceivePacket = new DatagramPacket(receiveData, UDP_PACKET_SIZE);
				// Wait for data packet
				serverSocket.receive(udpReceivePacket);
				receivedIPAddress = udpReceivePacket.getAddress();
				receivedPort = udpReceivePacket.getPort();

				if (!connectionEstablished) {
					// Prepare new connection
					clientAdress = receivedIPAddress;
					clientPort = receivedPort;
					serverSocket.setSoTimeout(CONNECTION_TIMEOUT);
					rcvbase = 0;
					recPacketCounter = 0;
					deliveredCounter = 0;
					corruptedCounter = 0;
					recBuf = new LinkedList<RFTpacket>();
					handshake = true;
					System.err.println("Handshaking with " + clientAdress.toString());
				}

				// Test if sender is the right one
				if ((clientAdress.equals(receivedIPAddress)) && (clientPort == receivedPort)) {
					// extract sequence number and data
					rftReceivePacket = new RFTpacket(udpReceivePacket.getData(), udpReceivePacket.getLength());

					long seqNum = rftReceivePacket.getSeqNum();

					// Handle first packet --> read and set parameters
					if (handshake) {
						if (seqNum == -1 && setParameters(rftReceivePacket)) {
							// Handshake successful
							handshake = false;
							connectionEstablished = true;
							System.err.println("New connection established with " + clientAdress.toString());

							// open destination file
							outToFile = new FileOutputStream(destPath);
						} else {
							// Wrong parameter packet --> End!
							System.err.println("Parameter Packet Error --> Server Shutdown!");
							running = false;
						}
					} else {
						// Data packet received
						recPacketCounter++;

						// Test on simulated error (packet checksum simulation)
						if ((recPacketCounter % errorRate) == 0) {
							corruptedCounter++;
							testOut("---- Packet " + seqNum + " corrupted! --------- " + recPacketCounter);
						} else {
							// Packet correct
							testOut("Server: Packet " + seqNum
									+ " correctly received! Expected for order delivery (rcvbase): " + rcvbase);

							// Packet in order --> write to file!
							if (seqNum == rcvbase) {
								writePacket(rftReceivePacket);
								rcvbase = rcvbase + rftReceivePacket.getLen();
								// packet filling a gap?
								deliverBufferPackets(); // adjust rcvbase
							} else if (seqNum > rcvbase) {
								// save packet in receive buffer
								insertPacketintoBuffer(rftReceivePacket);
							}
						}
						// send current rcvbase as ACK
						sendAck();
					}
				}
			} catch (java.net.SocketTimeoutException e) {
				// Copy job successfully finished
				outToFile.close();
				handshake = false;
				connectionEstablished = false;
				System.err.println("\nConnection successfully closed!");
				
				// Result: user information
				System.out.println(recPacketCounter + " packets received\n"
						+ deliveredCounter + " packets delivered\n"
						+ corruptedCounter + " packets corrupted\n"						
						+ rcvbase + " Bytes saved in file " + destPath + "\n");
				// reset connection timeout
				serverSocket.setSoTimeout(0);
				System.err.println("Waiting for connection using port  " + SERVER_PORT);
			} catch (IOException e) {
				System.err.println("XXXXXXXXXXXXXXX File Error: " + destPath);
				running = false;
			}
		}

		// --------- End ------------------------
		serverSocket.close();
	}

	private void sendAck() {
		/* Create and send UDP packet with ACK rcvbase */
		DatagramPacket udpAckPacket = new DatagramPacket(RFTpacket.makeByteArray(rcvbase), Long.BYTES, clientAdress,
				clientPort);
		 try {
			serverSocket.send(udpAckPacket);
		} catch (IOException e) {
			e.printStackTrace();
		} 
		testOut("ACK " + rcvbase + " sent!");
	}

	private void insertPacketintoBuffer(RFTpacket insertPacket) {
		/*
		 * Insert the packet into the receive buffer at the right position, if space
		 * available
		 */
		if (rcvWindow >= insertPacket.getLen() && !recBuf.contains(insertPacket)) { // no duplicates!
			recBuf.add(insertPacket);
			rcvWindow = rcvWindow - insertPacket.getLen();
			testOut("Packet " + insertPacket.getSeqNum() + " saved in buffer! RcvWindow: " + rcvWindow);

			// sort in ascending order using the seq num
			Collections.sort(recBuf);
		}
	}

	private void deliverBufferPackets() throws IOException {
		/*
		 * Deliver all packets which are in order, starting with rcvbase, remove all
		 * delivered packets from the recBuffer and adjust the rcvbase appropriately
		 */
		while (!recBuf.isEmpty() && (recBuf.getFirst().getSeqNum() == rcvbase)) {
			writePacket(recBuf.getFirst());
			rcvbase = rcvbase + recBuf.getFirst().getLen();
			rcvWindow = rcvWindow + recBuf.getFirst().getLen();
			recBuf.removeFirst();
		}
	}

	private void writePacket(RFTpacket deliverPacket) throws IOException {
		/* Deliver single RFTpacket: append packet data to outfile */
		deliveredCounter++;
		outToFile.write(deliverPacket.getData(), 0, deliverPacket.getLen());

		testOut("Packet " + deliverPacket.getSeqNum() + " delivered! Block of length " + deliverPacket.getLen()
				+ " appended to File " + destPath);
		if (!testOutputMode && deliveredCounter % 10 == 0) {
			System.err.print(".");  // Progress indicator for 10 delivered packets
		}
	}

	private boolean setParameters(RFTpacket controlPacket) {
		/* Evaluate packet with seqNum -1 */
		String parameters = "";
		String[] parameterArray;

		try {
			parameters = new String(controlPacket.getData(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		// Extract parameters
		parameterArray = parameters.split(";");

		if (parameterArray.length == 4) {
			// Adjust parameters
			destPath = parameterArray[0];

			try {
				rcvWindow = Long.parseLong(parameterArray[1]);
				errorRate = Long.parseLong(parameterArray[2]);
				if (parameterArray[3].equalsIgnoreCase("true")) {
					testOutputMode = true;
				} else {
					testOutputMode = false;
				}
			} catch (NumberFormatException e) {
				System.err
						.println("Control Packet (seqNum -1): syntax error! No numeric parameter found: " + parameters);

				// Parameter wrong
				return false;
			}

			System.err.println("Server-Parameters set: " + destPath + " - rcvWindow: " + rcvWindow + " - ErrorRate: "
					+ errorRate + " - TestOutputMode: " + testOutputMode);

			// Parameter OK!
			return true;
		} else {
			System.err.println("Control Packet (seqNum -1) has wrong number of parameters: " + parameters);

			// Parameter wrong
			return false;
		}
	}

	private void testOut(String out) {
		if (testOutputMode) {
			//System.err.printf("%.2f: %s\n", System.nanoTime() / 1000000.0, out);
			System.err.printf("%d: %s\n", System.currentTimeMillis(), out);			
		}
	}

	public static void main(String[] argv) throws Exception {
		RFTServer myServer = new RFTServer();
		myServer.runRFTServer();
	}
}
