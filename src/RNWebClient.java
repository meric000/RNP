import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Scanner;

public class RNWebClient {
    public final String CHARSET = "IBM-850"; // "UTF-8"

    /* Protokoll-Codierung des Zeilenendes: CRLF */
    private final String CRLF = "\r\n";

    /* Portnummer */
    private final int serverPort;

    /* Hostname */
    private final String hostname;

    /* TCP-Standard-Socketklasse */
    private Socket clientSocket;

    /* Ausgabestream zum Server */
    private DataOutputStream outToServer;

    /* Eingabestream vom Server
       Wenn Binärdaten verarbeitet werden müssen, kann auch DataInputStream verwendet werden */
    private BufferedReader inFromServer;

    private boolean serviceRequested = true; // Client beenden?

    public RNWebClient(String hostname, int serverPort) {
        this.serverPort = serverPort;
        this.hostname = hostname;
    }

    public void startJob() {
        Scanner inFromUser;
        String sentence;
        String modSentence;
        /**
         * Needs to be redone not for our purpose just for testing the basic functionality and concepts of client/Server
         */
        try {
            clientSocket = new Socket(hostname, serverPort);
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            inFromServer = new BufferedReader(new InputStreamReader(
                    clientSocket.getInputStream(), CHARSET));

            inFromUser = new Scanner(System.in, CHARSET);
            System.err.println("Client startet with Source Port: " + clientSocket.getLocalPort() + " - Destination Port: " + clientSocket.getPort());

            while (serviceRequested) {
                System.err.println("ENTER TCP-DATA: ");
                /* String vom Benutzer (Konsoleneingabe) holen */
                sentence = inFromUser.nextLine();

                /* String an den Server senden */
                writeToServer(sentence);

                /* Modifizierten String vom Server empfangen */
                modSentence = readFromServer();

                /* Test, ob Client beendet werden soll */
                if (modSentence.startsWith("QUIT")) {
                    serviceRequested = false;
                }
            }
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("Connection aborted by server!");
        }
        System.err.println("TCP Client stopped!");


    }
    private void writeToServer(String line) throws IOException {
        /* Sende eine Zeile (mit CRLF) zum Server */
        outToServer.write((line + CRLF).getBytes(CHARSET));
        System.err.println("TCP Client has sent the message: " + line);
    }
    private String readFromServer() throws IOException {
        /* Lies die Antwortzeile (reply) vom Server */
        String reply = inFromServer.readLine();
        System.err.println("TCP Client got from Server: " + reply);
        return reply;
    }
    public static void main(String[] args) {
        /* Test: Erzeuge Client und starte ihn. */
        RNWebClient myClient = new RNWebClient("localhost", 60000);  // Loopback (localhost) bei IPv6: "::1"
        myClient.startJob();
    }


}
