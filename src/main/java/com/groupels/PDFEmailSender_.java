package com.groupels;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PDFEmailSender_ extends JFrame {
    private final JTextArea logArea;
    private final Map<String, Employe> employeeMap;

    public PDFEmailSender_() {
        setTitle("PDF Email Sender");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JButton sendButton = new JButton("Envoyer les bulletins");
        logArea = new JTextArea();
        logArea.setEditable(false);

        setLayout(new BorderLayout());
        add(sendButton, BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        sendButton.addActionListener(_ -> sendPDFs());

        employeeMap = loadEmployeesFromJson();
    }

    private Map<String, Employe> loadEmployeesFromJson() {
        Map<String, Employe> map = new HashMap<>();
        JSONParser parser = new JSONParser();
        logArea.append("Veulliez cliquer sur le boutun ci-dessus pour \nenvoyer les bulletins.\n\n");

        try (FileReader reader = new FileReader(System.getProperty("user.home") + "/Downloads/employees.json")) {
            Object obj = parser.parse(reader);
            JSONArray employeeList = (JSONArray) obj;

            for (Object o : employeeList) {
                JSONObject employee = (JSONObject) o;
                String matricule=(String) employee.get("Matricule");
                map.put(
                    matricule,
                    new Employe(
                            matricule,
                            (String) employee.get("Prenom"),
                            (String) employee.get("Nom"),
                            (String) employee.get("email")

                    )
                );
                //System.out.println(map.get(matricule));
            }
        } catch (IOException | ParseException e) {
            logArea.append("Erreur lors de la lecture du fichier employees.json: " + e.getMessage() + "\n");
        }

        return map;
    }

    private void sendPDFs() {
       /* File[] pdfFiles =  new File(System.getProperty("user.home") + "/Downloads").
                listFiles((_, name) -> name.toLowerCase().endsWith(".pdf"));
*/
        employeeMap.forEach((matricule,employe)-> {
            if (employe.getEmail() != null) {
                sendEmail(employe, new File(System.getProperty("user.home") + "/Downloads/" + matricule + ".pdf"));
                logArea.append(matricule + ".pdf envoyé à " + employe.getEmail() + "\n");
            } else {
                logArea.append("Erreur: Pas d'email trouvé pour " + matricule + "\n");
            }

        });

        /*
        if (pdfFiles != null) {
            for (File pdf : pdfFiles) {
                String matricule = pdf.getName().replace(".pdf", "");
                String email = getEmailFromMatricule(matricule);
                if (email != null) {
                    sendEmail(email, pdf);
                    logArea.append("Envoyé: " + pdf.getName() + " à " + email + "\n");
                } else {
                    logArea.append("Erreur: Pas d'email trouvé pour " + matricule + "\n");
                }
            }
        } else {
            logArea.append("Aucun fichier PDF trouvé dans le dossier de téléchargement.\n");
        }*/
    }

    private String getEmailFromMatricule(String matricule) {
        return employeeMap.get(matricule).getEmail();
    }

    private void sendEmail(Employe employe, File attachment) {
        // Configurez ici les paramètres de votre serveur SMTP
        String host = "smtp.gmail.com";
        String from = "seinotifs@gmail.com";
        String password = "vioc amzw akpi kstn";

        Properties properties = System.getProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");

        Session session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(employe.getEmail()));
            LocalDate date = LocalDate.now();
            message.setSubject("Votre bulletin du mois de "+ date.getMonth().name()+ " "+date.getYear());

            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(
                    "Bonjour "+employe.getPrenom() + "\n\n"+
                    "Veuillez trouver ci-joint votre bulletin de salaire de "+ date.getMonth().name()+ " "+ date.getYear()+
                    "\n\nCordialement"
            );



            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            messageBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(attachment);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(attachment.getName());
            multipart.addBodyPart(messageBodyPart);


            message.setContent(multipart);

            Transport.send(message);
        } catch (MessagingException mex) {
            mex.printStackTrace();
            logArea.append("Erreur lors de l'envoi de l'email.\nVeulliez vous assurer que les bulletin des employés\nTouts etaient mis dans le dossier Téléchargements.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PDFEmailSender_().setVisible(true));
    }
}
