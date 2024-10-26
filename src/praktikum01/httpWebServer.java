package praktikum01;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

public class httpWebServer {
    public static void main(String[] args) throws IOException {
        //Create Server
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        //Create a context for a specific path and set the default executor
        server.createContext("/", new Myhandler());

        //starting the Server
        server.setExecutor(null);
        server.start();


        System.out.println("Server is running on port 8080");
    }
}

class Myhandler implements HttpHandler {
    private static final String ALLOWED_BROWSER = "Firefox";

    // doGet-Methode, die nur HttpExchange verwendet
    protected boolean doGet(HttpExchange exchange) throws IOException {
        // Prüfen, ob die .htuser-Datei existiert
        File checkForHtuser = new File("C:\\Users\\meric\\OneDrive\\Desktop\\UNI\\Sem6\\RNP\\src\\praktikum01\\Testweb\\.htusers");
        if (checkForHtuser.exists()) {
            String authenticationQuestion = "Please send password with you header";
            exchange.sendResponseHeaders(200, authenticationQuestion.length());
            OutputStream os = exchange.getResponseBody();
            os.write(authenticationQuestion.getBytes());
            os.close();

            try (BufferedReader loginReader = new BufferedReader(new FileReader(checkForHtuser))) {
                String user;
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                while ((user = loginReader.readLine()) != null) {
                    if (user.contains(authHeader)) {
                        return true;
                    }
                }
                return false;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // Prüfen des User-Agent-Headers
        String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
        if (userAgent == null || !userAgent.contains(ALLOWED_BROWSER)) {
            String errorMessage = "406 Not Acceptable. Browser nicht akzeptiert. Bitte Firefox nutzen.";
            exchange.sendResponseHeaders(406, errorMessage.length());
            exchange.getResponseBody().write(errorMessage.getBytes());
            exchange.close();
            return false;
        }

        return true;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        doGet(exchange);

        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        File htmlFile = new File("C:\\Users\\meric\\OneDrive\\Desktop\\UNI\\Sem6\\RNP\\src\\praktikum01\\Testweb"+path);


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
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(404, errorResponse.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorResponse.getBytes());
            }
        }


    }
}
