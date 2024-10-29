package praktikum01;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class HttpWebServer {
    //Die main, sie macht Main sachen wir lieben main <3
    public static void main(String[] args) throws IOException {
        HttpServer server1 = HttpServer.create(new InetSocketAddress(8080), 0);
        //Standardpfad wird angegeben mit einem übergebenen Handler
        server1.createContext("/", new Myhandler());

        //Mutitheading, Server erstellt auomatisch für jeden client eine neue Verbindung
        server1.setExecutor(null);
        server1.start();
    }

}

class Myhandler implements HttpHandler {
    public final String CHARSET = "UTF-8";
    private static final String ALLOWED_BROWSER = "Firefox";

    /**
     * Überprüft die von Client übergebenen Headerzeilen und gibt ein Fehlercode aus wenn nötig
     *
     * @param exchange hilft dabei die Anfragen von Client zu bearbeiten und erleichtert das senden an Daten an den Client
     * @return false-> wenn ein fehler aufkommt true-> wenn alles in Ordnung ist und der Client weiter geleitet werden kann
     * @throws IOException
     */
    protected boolean doGet(HttpExchange exchange) throws IOException {
        String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        File checkForHtuser = new File("C:\\Users\\meric\\OneDrive\\Desktop\\UNI\\Sem6\\RNP\\src\\praktikum01\\Testweb\\.htuser");

        if (!userAgent.contains(ALLOWED_BROWSER)) {
            sendResponse(exchange, 406, "Wrong Browser. Please use Firefox.");
            return false;
        }
        //Ausgabe der Headerfiles des Client auf der Konsole
        System.out.println("Client Headers:");
        exchange.getRequestHeaders().forEach((key, value) -> {
            System.out.println(key + ": " + value);
        });
        // Prüfen, ob die.htuser-Datei existiert
        if (checkForHtuser.exists()) {
            if (authHeader == null) {
                //Senden einer Authtifizierungsanfrage an den Client
                exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"Access to the site\"");
                sendResponse(exchange, 401, "AuthHeader missing");
                return false;
            }
            //Decodieren des Headers
            String base64Credentials = authHeader.replaceFirst("Basic ", "");
            String decodedCredentials = new String(Base64.getDecoder().decode(base64Credentials), CHARSET); // Dekodierung in username:password

            // Vergleiche die dekodierten Anmeldedaten mit den Inhalten von .htuser
            try (BufferedReader loginReader = new BufferedReader(new FileReader(checkForHtuser))) {
                String user;
                while ((user = loginReader.readLine()) != null) {
                    if (user.equals(decodedCredentials)) {// Vergleich mit dekodierten Anmeldedaten
                        //Weiterleitung wenn erfolgreich
                        exchange.getResponseHeaders().set("Location", "/index.html");
                        return true;
                    }
                }
                //Neuanfrage bei falschen Daten
                exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"Access to the site\"");
                sendResponse(exchange, 401, "Wrong Auth-Header Try again");
                return false;
            }
        }
        if (userAgent == null || authHeader == null) {
            sendResponse(exchange, 400, "Missing Headerdate please send again");
            return false;
        }

        return true;
    }

    /**
     * Handelt Http-Anfragen und zeigt die Seite Korrekt an
     *
     * @param exchange
     * @throws IOException
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!doGet(exchange)) {
            return;
        }
        //Setzten des Default pfads auf index.html
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }


        File htmlFile = new File("C:\\Users\\meric\\OneDrive\\Desktop\\UNI\\Sem6\\RNP\\src\\praktikum01\\Testweb" + path);
        System.err.println("Content Length:" + htmlFile.length());
        System.err.println("Content Type: " + Files.probeContentType(htmlFile.toPath()));


        if (htmlFile.exists()) {
            //Extrahieren der mimetypes der htmlFile
            String mimeType = Files.probeContentType(Paths.get(htmlFile.getPath()));
            //Senden von einem ResponseHeader mit den korrekten Contenttype
            exchange.getResponseHeaders().set("Content-Type", mimeType != null ? mimeType : "application/octet-stream");
            exchange.sendResponseHeaders(200, htmlFile.length());

            //Korrektes laden der HtmlFile
            try (FileInputStream fis = new FileInputStream(htmlFile); OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) > 0) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        } else {
            String errorResponse = "404 (Not Found)\nDie angeforderte Datei wurde nicht gefunden.";
            sendResponse(exchange, 404, "404 Not Found: The requested file was not found.");
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(404, errorResponse.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorResponse.getBytes());
            }
        }
    }

    /**
     * Sends a Response-Message to the Client
     * @param exchange
     * @param statusCode
     * @param response
     * @throws IOException
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(CHARSET));
        }
    }

}
