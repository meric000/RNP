package praktikum01;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Base64;

public class HttpWebClient {
    private final int serverPort;
    private final String hostname;
    private Socket clientSocket;
    private DataOutputStream outToServer;
    private BufferedReader inFromServer;
    private final String CRLF = "\r\n";

    public HttpWebClient(int serverPort, String hostname) throws URISyntaxException {
        this.serverPort = serverPort;
        this.hostname = hostname;
    }

    private static final String getBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + password;
        return valueToEncode;
    }


    HttpRequest request = HttpRequest.newBuilder()
            .GET().uri(new URI("http://localhost:8080/"))
            .header("Authorization", getBasicAuthenticationHeader("testuser1", "super"))
            .build();

    public void startJob() {
        try {
            clientSocket = new Socket(hostname, serverPort);
            outToServer = new DataOutputStream(clientSocket.getOutputStream());
            inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String authHeader = getBasicAuthenticationHeader("testuser1", "super");

            String request = "GET / HTTP/1.1" + CRLF +
                    "Host: " + hostname + CRLF +
                    "User-Agent: Firefox" + CRLF +
                    "Authorization: " + getBasicAuthenticationHeader("testuser1", "super") + CRLF +
                    CRLF;
            // GET-Anfrage formatieren
            outToServer.writeBytes(request);

            String responseLine;
            while ((responseLine = inFromServer.readLine()) != null) {
                System.out.println("Server response: " + responseLine);
                if (responseLine.contains("Location: /index.html")) {
                    System.out.println("Redirecting to /index.html...");
                    // Hier könnten Sie optional eine weitere GET-Anfrage an "http://localhost:8080/index.html" senden
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws URISyntaxException {
        /* Test: Erzeuge Client und starte ihn. */
        HttpWebClient myClient = new HttpWebClient(8080, "localhost");  // Loopback (localhost) bei IPv6: "::1"
        myClient.startJob();
    }
}
