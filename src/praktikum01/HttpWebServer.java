package praktikum01;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

public class HttpWebServer {
    public static void main(String[] args) throws IOException {
        HttpServer server1 = HttpServer.create(new InetSocketAddress(8080), 0);
        //Create a context for a specific path and set the default executor
        server1.createContext("/", new Myhandler());

        //starting the Server
        server1.setExecutor(null);
        server1.start();
    }

}

class Myhandler implements HttpHandler {
    private BufferedReader inFromClient;
    public final String CHARSET = "UTF-8";
    private static final String ALLOWED_BROWSER = "Firefox";


    // doGet-Methode, die nur HttpExchange verwendet
    protected boolean doGet(HttpExchange exchange) throws IOException {
        // Prüfen des User-Agent-Headers
        String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
        if (userAgent == null || !userAgent.contains(ALLOWED_BROWSER)) {
            sendResponse(exchange, 406, "Wrong Browser. Please use Firefox.");
            return false;
        }

        System.out.println("Client Headers:");
        exchange.getRequestHeaders().forEach((key, value) -> {
            System.out.println(key + ": " + value);
        });
        // Prüfen, ob die.htuser-Datei existiert
        File checkForHtuser = new File("/Users/muggi/IdeaProjects/RNP/src/praktikum01/Testweb/.htuser");
        if (checkForHtuser.exists()) {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null) {
                exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"Access to the site\"");
                sendResponse(exchange, 401, "AuthHeader missing");
                return false;
            }
            String base64Credentials = authHeader.replaceFirst("Basic ", "");
            String decodedCredentials = new String(Base64.getDecoder().decode(base64Credentials), CHARSET); // Dekodierung in username:password

// Vergleiche die dekodierten Anmeldedaten mit den Inhalten von .htuser
            try (BufferedReader loginReader = new BufferedReader(new FileReader(checkForHtuser))) {
                String user;
                while ((user = loginReader.readLine()) != null) {
                    if (user.equals(decodedCredentials)) {  // Vergleich mit dekodierten Anmeldedaten
                        exchange.getResponseHeaders().set("Location", "/index.html");
                        return true;
                    }
                }
                exchange.getResponseHeaders().set("WWW-Authenticate", "Basic realm=\"Access to the site\"");
                sendResponse(exchange, 403, "Forbidden Authorization failed");
                return false;
            }
        }




        return true;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!doGet(exchange)) {
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }

        inFromClient = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
        String answerFromClient;
        while ((answerFromClient= inFromClient.readLine()) != null) {
            System.out.println(answerFromClient);
        }
        File htmlFile = new File("/Users/muggi/IdeaProjects/RNP/src/praktikum01/Testweb/" + path);
        System.err.println("Content Length:" + htmlFile.length());
        System.err.println("Content Type: " + Files.probeContentType(htmlFile.toPath()));


        if (htmlFile.exists()) {
            String mimeType = Files.probeContentType(Paths.get(htmlFile.getPath()));
            exchange.getResponseHeaders().set("Content-Type", mimeType != null ? mimeType : "application/octet-stream");
            exchange.sendResponseHeaders(200, htmlFile.length());

            try (FileInputStream fis = new FileInputStream(htmlFile);
                 OutputStream os = exchange.getResponseBody()) {
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

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(CHARSET));
        }
    }

}
