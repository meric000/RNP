package praktikum01;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;

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

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
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
