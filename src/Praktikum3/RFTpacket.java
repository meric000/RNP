package Praktikum3;/* RFTpacket.java
 Version 1.1
 Praktikum Rechnernetze HAW Hamburg
 Autor: M. Huebner
 */

public class RFTpacket implements Comparable<RFTpacket> {
	/*
	 * Data structure for the representation of one data packet with seq num in the
	 * send/rec buffer (seq num and data will be sent in one UDP packet).
	 */
	private byte[] data;
	private int dataLen; // length of data array
	private long seqNumber; // sequence number as long
	private byte[] seqNumberBytes; // sequence number as byte
	private long timestamp = -1; // Can be used to store the send time

	/**
	 * Constructor for sending RFTpackets. The first <packetLen> bytes of the
	 * packetData byte array are copied and a new data byte array is generated.
	 */
	public RFTpacket(long seqNum, byte[] packetData, int packetLen) {
		data = new byte[packetLen];
		System.arraycopy(packetData, 0, data, 0, packetLen);
		dataLen = packetLen;
		seqNumber = seqNum;
		seqNumberBytes = makeByteArray(seqNum);
	}

	/**
	 * Constructor for received RFTpackets. The first 8 bytes of the packetData are
	 * treated as the sequence number. The other <packetLen-8> bytes of the
	 * packetData byte array are copied and a new data byte array is generated.
	 */
	public RFTpacket(byte[] packetData, int packetLen) {
		seqNumberBytes = reduce(packetData, 0, 8);
		seqNumber = makeLong(seqNumberBytes);
		if (packetLen >= 8) {
			data = reduce(packetData, 8, packetLen - 8);
			dataLen = packetLen - 8;
		} else {
			dataLen = 0;
		}
	}

	/**
	 * Save a timestamp for the RFTpacket
	 */
	public void setTimestamp(long time) {
		timestamp = time;
	}

	/**
	 * Returns the data of the RFTpacket.
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Returns the length of the data byte array of the RFTpacket
	 */
	public int getLen() {
		return dataLen;
	}

	/**
	 * Returns the sequence number of the RFTpacket as a long value
	 */
	public long getSeqNum() {
		return seqNumber;
	}

	/**
	 * Returns the sequence number of the RFTpacket as a byte array
	 */
	public byte[] getSeqNumBytes() {
		return seqNumberBytes;
	}

	/**
	 * Returns the sequence number of the RFTpacket as a byte[8] array concatenated
	 * with the data byte array (length = dataLen + 8)
	 */
	public byte[] getSeqNumBytesAndData() {
		return concatenate(seqNumberBytes, data);
	}

	/**
	 * Returns the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}

	// -------------------- standard methods ----------------------------------

	@Override
	public int compareTo(RFTpacket partner) {
		// Use seq num for comparison (sort)
		return (int) (this.seqNumber - partner.seqNumber);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (seqNumber ^ (seqNumber >>> 32));
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RFTpacket other = (RFTpacket) obj;
		if (seqNumber != other.seqNumber) {
			return false;
		}
		return true;
	}

	// -------------------- auxiliary methods ---------------------------------
	/**
	 * Reduce the given byte array to the given length starting at position offset
	 */
	public static byte[] reduce(byte[] ba, int offset, int len) {
		byte[] result = new byte[len];

		System.arraycopy(ba, offset, result, 0, len);

		return result;
	}

	/**
	 * Concatenate two byte arrays
	 */
	public static byte[] concatenate(byte[] ba1, byte[] ba2) {
		int len1 = ba1.length;
		int len2 = ba2.length;
		byte[] result = new byte[len1 + len2];

		// Fill with first array
		System.arraycopy(ba1, 0, result, 0, len1);
		// Fill with second array
		System.arraycopy(ba2, 0, result, len1, len2);

		return result;
	}

	/**
	 * Convert a byte array to a long.
	 */
	public static long makeLong(byte[] buf) {
		long result = 0;

		for (int j = 0; j < Long.BYTES; j++) {
			result = (result << Long.BYTES) | (buf[j] & 0xffL);
		}
		return result;
	}

	/**
	 * Convert a long to a byte array.
	 */
	public static byte[] makeByteArray(long source) {
		byte[] result = new byte[Long.BYTES];

		for (int j = Long.BYTES - 1; j >= 0; j--) {
			result[j] = (byte) source;
			source = source >>> Long.BYTES;
		}
		return result;
	}

}
