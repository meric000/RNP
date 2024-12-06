package praktikum0;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class RNServer {

    public Semaphore workerSemaphore;

    public final int port;

    /* Anzeige, ob der Server-Dienst weiterhin benoetigt wird */
    public boolean serviceRequested = true;

    public RNServer(int portnummer, int anzahlCleints) {
        this.port = portnummer;
        this.workerSemaphore = new Semaphore(anzahlCleints);

    }

    public void startRNWebServer() {
        ServerSocket welcomeSocket;
        Socket connectionSocket;

        int nextThreadNumber = 0;
        try {
            System.err.println("Creating nerw TCP Server Socket port:" + port);
            welcomeSocket = new ServerSocket(port);
            while (serviceRequested) {
                workerSemaphore.acquire(); //blockt wenn max anzahl an Threads erreicht wird
                System.err.println("Server is waiting for Connection - on port:" + port);

                connectionSocket = welcomeSocket.accept();
                (new TCPWorkerThread(nextThreadNumber++,connectionSocket,this)).start();
            }

        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    public static void main(String[] args) {
        RNServer server = new RNServer(60000,2);
        server.startRNWebServer();
    }


}

class TCPWorkerThread extends Thread{
    /*
     * Arbeitsthread, der eine existierende Socket-Verbindung zur Bearbeitung
     * erhaelt
     */
    public final String CHARSET = "IBM-850"; // "UTF-8"

    /* Protokoll-Codierung des Zeilenendes: CRLF */
    private final String CRLF = "\r\n" ;

    private int name;
    private Socket socket;
    private RNServer server;
    private BufferedReader inFromClient;
    private DataOutputStream outToClient;
    boolean workerServiceRequested = true; // Arbeitsthread beenden?

    public TCPWorkerThread(int num, Socket sock, RNServer server) {
        /* Konstruktor */
        this.name = num;
        this.socket = sock;
        this.server = server;
    }
    public void run() {
        String capitalizedSentence;

        System.err.println("TCP Worker Thread " + name + " is running until QUIT is received!");
        System.err.println("           Source Port: " + socket.getLocalPort() + " - Destination Port: " + socket.getPort());

        try {
            /* Socket-Basisstreams durch spezielle Streams filtern */
            inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream(), CHARSET));
            outToClient = new DataOutputStream(socket.getOutputStream());

            while (workerServiceRequested) {
                /* String vom Client empfangen und in Grossbuchstaben umwandeln */
                capitalizedSentence = readFromClient().toUpperCase();

                /* Modifizierten String an Client senden */
                writeToClient(capitalizedSentence);

                /* Test, ob Arbeitsthread beendet werden soll */
                if (capitalizedSentence.startsWith("QUIT")) {
                    workerServiceRequested = false;
                }
            }

            /* Socket-Streams schliessen --> Verbindungsabbau */
            socket.close();
        } catch (IOException e) {
            System.err.println("Connection aborted by client!");
        } finally {
            System.err.println("TCP Worker Thread " + name + " stopped!");
            /* Platz fuer neuen Thread freigeben */
            server.workerSemaphore.release();
        }
    }

    private String readFromClient() throws IOException {
        /* Lies die naechste Anfrage-Zeile (request) vom Client */
        String request = inFromClient.readLine();
        System.err.println("TCP Worker Thread " + name + " detected job: " + request);

        return request;
    }

    private void writeToClient(String line) throws IOException {
        /* Sende die Antwortzeile (mit CRLF) zum Client */
        outToClient.write((line + CRLF).getBytes(CHARSET));
        System.err.println("TCP Worker Thread " + name + " has written the message: " + line);
    }
}
