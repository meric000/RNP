package Praktikum2;

import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;

public class MailFile {

    public Properties props = new Properties();

    private String senderEmail;
    private String username;
    private String password;
    private String smtpHost;
    private int smtpPort;


    public void buildMail() {
        try (InputStream input = new FileInputStream("C:/Users/meric/OneDrive/Desktop/UNI/Sem6/RNP/src/Praktikum2/MailFile.ini")) {
            props.load(input);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        senderEmail = props.getProperty("SENDER_ADDRESS");
        username = props.getProperty("USER_ACCOUNT");
        password = props.getProperty("PASSWORD");
        smtpHost = props.getProperty("SMTP_ADDRESS");
        smtpPort = Integer.parseInt(props.getProperty("SMTP_PORT"));

    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(int smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Properties getProps() {
        return props;
    }

    public void setProps(Properties props) {
        this.props = props;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public static void main(String[] args) throws IOException {
        String recipientEmail = args[0];
        String filePath = args[1];
        MailFile mf = new MailFile();
        mf.buildMail();

        Socket socket = mf.getSmtpPort() == 465 ? ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(mf.getSmtpHost(), mf.getSmtpPort()) : new Socket(mf.getSmtpHost(), mf.getSmtpPort());
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // SMTP-Kommandos senden
        System.out.println("Server: " + reader.readLine());
        writer.println("HELO " + mf.getSmtpHost());
        System.out.println("Client: HELO " + mf.getSmtpHost());
        System.out.println("Server: " + reader.readLine());

        // AUTH LOGIN mit Base64-codierten Anmeldeinformationen
        writer.println("AUTH LOGIN");
        System.out.println("Client: AUTH LOGIN");
        System.out.println("Server: " + reader.readLine());

        writer.println(Base64.getEncoder().encodeToString(mf.getUsername().getBytes()));
        System.out.println("Client: " + Base64.getEncoder().encodeToString(mf.getUsername().getBytes()));
        System.out.println("Server: " + reader.readLine());

        writer.println(Base64.getEncoder().encodeToString(mf.getPassword().getBytes()));
        System.out.println("Client: " + Base64.getEncoder().encodeToString(mf.getPassword().getBytes()));
        System.out.println("Server: " + reader.readLine());

        // Absender- und Empfängeradresse
        writer.println("MAIL FROM:<" + mf.getSenderEmail() + ">");
        System.out.println("Client: MAIL FROM:<" + mf.getSenderEmail() + ">");
        System.out.println("Server: " + reader.readLine());

        writer.println("RCPT TO:<" + recipientEmail + ">");
        System.out.println("Client: RCPT TO:<" + recipientEmail + ">");
        System.out.println("Server: " + reader.readLine());

        // Beginn der Datenübertragung
        writer.println("DATA");
        System.out.println("Client: DATA");
        System.out.println("Server: " + reader.readLine());

        // Betreff und MIME-Header senden
        writer.println("Subject: Test Email with Attachment");
        writer.println("MIME-Version: 1.0");
        writer.println("Content-Type: multipart/mixed; boundary=\"boundary\"");
        writer.println();
        writer.println("--boundary");
        writer.println("Content-Type: text/plain");
        writer.println();
        writer.println("This is a test email with an attachment.");

        // Datei als Anhang hinzufügen
        writer.println("--boundary");
        writer.println("Content-Type: application/octet-stream; name=\"" + Paths.get(filePath).getFileName() + "\"");
        writer.println("Content-Transfer-Encoding: base64");
        writer.println("Content-Disposition: attachment; filename=\"" + Paths.get(filePath).getFileName() + "\"");
        writer.println();

        byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
        writer.println(Base64.getEncoder().encodeToString(fileContent));
        writer.println("--boundary--");

        // Ende der Datenübertragung
        writer.println(".");
        System.out.println("Client: .");
        System.out.println("Server: " + reader.readLine());

        // Beenden der Verbindung
        writer.println("QUIT");
        System.out.println("Client: QUIT");
        System.out.println("Server: " + reader.readLine());

        writer.close();
        reader.close();
        socket.close();
    }

}





