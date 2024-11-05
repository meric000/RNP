package Praktikum2;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;
import java.util.Scanner;

public class MailFile {

    private String empfaenerMail;
    private String pfadName;
    private String userMail = "wif635@haw-hamburg.de";
    private String userName = "wif635@haw-hamburg.de";
    private String password = "SunnyMilou123.";
    /* Protokoll-Codierung des Zeilenendes: CRLF */
    private final String CRLF = "\r\n" ;

    /* Portnummer */
    private final int serverPort = 25;

    /* Hostname */
    private final String hostname = "smtp-mail.outlook.com";

    /* TCP-Standard-Socketklasse */
    private Socket clientSocket;

    /* Ausgabestream zum Server */
    private DataOutputStream outToServer;

    /* Eingabestream vom Server
       Wenn Binärdaten verarbeitet werden müssen, kann auch DataInputStream verwendet werden */
    private BufferedReader inFromServer;

    private boolean serviceRequested = true; // Client beenden?

    Properties properties = new Properties();

    String betreff = "Cooler Betreff";
    String inhalt = "Noch coolerer Inhalt";

    public void sendMail() throws IOException {
        //Todo: Richtige Konsoleneingabe nachbearbeiten
        System.out.println("Gib daten ein mit leerzeiche");
        Scanner input = new Scanner(System.in);
        String userInput = input.nextLine();
        String[] data = userInput.split(" ");
        empfaenerMail = data[0];
        pfadName = data[1];
        System.out.println("mail: "+empfaenerMail+"pfad: "+pfadName);
        buildMail();


    }

    public void buildMail() throws IOException {
        try (Socket socket = new Socket(hostname, serverPort);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())))
        {
            //Handshake
            reader.readLine();
            writer.write("EHLO " + hostname + "\r\n");
            writer.flush();
            readResponse(reader);

            // Startet die TLS-Verschlüsselung, falls der Port 465 verwendet wird (optional)
            writer.write("STARTTLS\r\n");
            writer.flush();
            readResponse(reader);

            // AUTH LOGIN für Authentifizierung mit Base64-kodiertem Benutzername und Passwort
            writer.write("AUTH LOGIN\r\n");
            writer.flush();
            readResponse(reader);
            writer.write(Base64.getEncoder().encodeToString(userName.getBytes()) + "\r\n");
            writer.flush();
            readResponse(reader);
            writer.write(Base64.getEncoder().encodeToString(password.getBytes()) + "\r\n");
            writer.flush();
            readResponse(reader);

            // MAIL FROM
            writer.write("MAIL FROM:<" + userMail + ">\r\n");
            writer.flush();
            readResponse(reader);

            // RCPT TO
            writer.write("RCPT TO:<" + empfaenerMail + ">\r\n");
            writer.flush();
            readResponse(reader);

            // DATA-Befehl starten
            writer.write("DATA\r\n");
            writer.flush();
            readResponse(reader);

            // Nachrichtentext senden
            writer.write("Subject: " + betreff + "\r\n");
            writer.write("To: " + empfaenerMail + "\r\n");
            writer.write("From: " + userMail + "\r\n");
            writer.write("\r\n"); // Leere Zeile vor dem eigentlichen Nachrichtentext
            writer.write(inhalt + "\r\n");
            writer.write(".\r\n"); // Punkt alleine auf einer Zeile beendet DATA-Befehl
            writer.flush();
            readResponse(reader);

            // QUIT zum Beenden der SMTP-Session
            writer.write("QUIT\r\n");
            writer.flush();
            readResponse(reader);


        }
    }

    private static void readResponse(BufferedReader reader) throws IOException {
        String response = reader.readLine();
        System.out.println("Server: " + response);
    }

    public static void main(String[] args) throws IOException {
        MailFile mf = new MailFile();
        mf.sendMail();
    }
}




