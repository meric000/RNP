package Praktikum4;

/*
 * UDPClient.java
 *
 * Version 2.1
 * Vorlesung Rechnernetze HAW Hamburg
 * Autor: M. Huebner (nach Kurose/Ross)
 * Zweck: UDP-Client Beispielcode:
 *        UDP-Socket erzeugen, einen vom Benutzer eingegebenen
 *        String in ein UDP-Paket einpacken und an den UDP-Server senden,
 *        den String in Grossbuchstaben empfangen und ausgeben
 *        Nach QUIT beenden, bei SHUTDOWN den Serverthread beenden
 */
import java.io.*;

import java.net.*;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;


public class DHCPv6Explorer {
    public final int SERVER_PORT = 547;
    public final String IPV6 = "fd00::dc87:5630:d0d3:e940";
    byte[] solicitbyteHeader;
    public final String HOSTNAME = "localhost";
    public final int BUFFER_SIZE = 1024;
    public final String CHARSET = "IBM-850"; // "UTF-8"
    private final String CRLF = "\r\n";

    // UDP-Socketklasse
    private DatagramSocket clientSocket;

    private boolean serviceRequested = true;
 //   public InetAddress SERVER_IP_ADDRESS;
    public Inet6Address SERVER_IP6_ADDRESS;

    /* Client starten. Ende, wenn quit eingegeben wurde */
    public void startJob() {
        Scanner inFromUser;

        String sentence;
        String modifiedSentence;

        try {
            /* IP-Adresse des Servers ermitteln --> DNS-Client-Aufruf! */
            SERVER_IP6_ADDRESS = (Inet6Address) InetAddress.getByName(IPV6);
            //SERVER_IP_ADDRESS = InetAddress.getByName(HOSTNAME);

            /* UDP-Socket erzeugen (kein Verbindungsaufbau!)
             * Socket wird an irgendeinen freien (Quell-)Port gebunden, da kein Port angegeben */
            clientSocket = new DatagramSocket();

            /* Konsolenstream (Standardeingabe) initialisieren */
            inFromUser = new Scanner(System.in, CHARSET);

            while (serviceRequested) {
                System.err.println("ENTER UDP-DATA: ");
                /* String vom Benutzer (Konsoleneingabe) holen */
                sentence = inFromUser.nextLine();

                /* Test, ob Client beendet werden soll */
                if (sentence.startsWith("quit")) {
                    serviceRequested = false;
                } else {

                    /* Sende den String als UDP-Paket zum Server */
                    writeToServer(sentence);

                    /* Modifizierten String vom Server empfangen */
                    modifiedSentence = readFromServer();
                }
            }

            /* Socket schliessen (freigeben)*/
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Connection aborted by server!");
        }

        System.err.println("UDP Client stopped!");
    }

    //TODO: write a Method to create a correct Solicit Request and sending it to the Server
    private void createSolcitHeader(){
        // Nachrichtentyp: 01 (Solicit)
        String messageType = "01";

        // Transaktions-ID: 3 Bytes (Beispiel: 123456)
        String transactionId = "123456";

        String clientID = buildClientIdString();

        solicitbyteHeader = hexStringtoByteArray(messageType+transactionId+clientID);
    }

    private String buildClientIdString(){
        // Option Code: 1 (Client Identifier) -> 0001
        String optionCode = "0001";

        // Länge der Option: Länge der DUID (10 Bytes) -> 000A
        String optionLength = "000A";

        String duid = "000300017085C29BA622";

        return optionCode+optionLength+duid;

    }


    private void writeToServer(String sendString) throws IOException {
        createSolcitHeader();
        /* Sende den String als UDP-Paket zum Server */

        /* String in Byte-Array umwandeln */
        //byte[] sendData = sendString.getBytes(CHARSET);

        /* Paket erzeugen mit Server-IP und Server-Zielport */
        DatagramPacket sendPacket = new DatagramPacket(solicitbyteHeader, solicitbyteHeader.length,
                SERVER_IP6_ADDRESS, SERVER_PORT);
        /* Senden des Pakets */
        clientSocket.send(sendPacket);

        System.err.println("UDP Client has sent the message: " + sendString);
    }

    private String readFromServer() throws IOException {
        /* Liefere den naechsten String vom Server */
        String receiveString = "";

        /* Paket fuer den Empfang erzeugen */
        byte[] receiveData = new byte[BUFFER_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, BUFFER_SIZE);

        /* Warte auf Empfang des Antwort-Pakets auf dem eigenen (Quell-)Port,
         * den der Server aus dem Nachrichten-Paket ermittelt hat */
        clientSocket.receive(receivePacket);

        /* Paket wurde empfangen --> auspacken und Inhalt anzeigen */
        receiveString = new String(receivePacket.getData(), 0,
                receivePacket.getLength(), CHARSET);

        System.err.println("UDP Client got from Server: " + receiveString);

        return receiveString;
    }


    private byte[] hexStringtoByteArray(String hex) {
        /* Konvertiere den String mit Hex-Ziffern in ein Byte-Array */
        byte[] val = new byte[hex.length() / 2];
        for (int i = 0; i < val.length; i++) {
            int index = i * 2;
            int num = Integer.parseInt(hex.substring(index, index + 2), 16);
            val[i] = (byte) num;
        }
        return val;
    }

    private String byteArraytoHexString(byte[] byteArray) {
        /* Konvertiere das Byte-Array in einen String mit Hex-Ziffern */
        String hex = "";
        if (byteArray != null) {
            for (int i = 0; i < byteArray.length; ++i) {
                hex = hex + String.format("%02X", byteArray[i]);
            }
        }
        return hex;
    }

    private void showNetwork() throws SocketException {
        /* Netzwerk-Infos fuer alle Interfaces ausgeben */
        Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            NetworkInterface ni = en.nextElement();
            System.out.println("\nDisplay Name = " + ni.getDisplayName());
            System.out.println(" Name = " + ni.getName());
            System.out.println(" Scope ID (Interface ID) = " + ni.getIndex());
            System.out.println(" Hardware (LAN) Address = " + byteArraytoHexString(ni.getHardwareAddress()));

            List<InterfaceAddress> list = ni.getInterfaceAddresses();
            Iterator<InterfaceAddress> it = list.iterator();

            while (it.hasNext()) {
                InterfaceAddress ia = it.next();
                System.out
                        .println(" Adress = " + ia.getAddress() + " with Prefix-Length " + ia.getNetworkPrefixLength());
            }
        }
    }

    public static void main(String[] args) throws SocketException {
        DHCPv6Explorer myClient = new DHCPv6Explorer();
        myClient.showNetwork();
        myClient.startJob();
    }
}
