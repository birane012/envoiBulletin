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
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class EnvoiBulletin extends JFrame {
    private final JTextArea logArea;
    private  Map<String, Employe> employeeMap;
    private final JProgressBar progressBar;

    public EnvoiBulletin() {
        setTitle("Envoi des bulletins de salaire Groupe LS");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JButton sendButton = new JButton("Envoyer les PDF");
        logArea = new JTextArea();
        logArea.setEditable(false);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(sendButton, BorderLayout.NORTH);
        topPanel.add(progressBar, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        sendButton.addActionListener((e)  -> {
            sendButton.setEnabled(false);
            new Thread(() -> {
                try {
                    sendPDFs();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                SwingUtilities.invokeLater(() -> sendButton.setEnabled(true));
            }).start();
        });

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

    private void sendPDFs() throws IOException {
        if(employeeMap != null && !employeeMap.isEmpty()){
            List<Employe> employes = employeeMap.values().stream().collect(Collectors.toList());
            File bulletin;
            File logFile= new File(System.getProperty("user.home") + "/Downloads/log.txt");
            FileWriter log=new FileWriter(logFile,true);
            // Define a French date format
            SimpleDateFormat frenchDateFormat = new SimpleDateFormat("EEEE d MMMM yyyy hh:mm:ss", Locale.FRANCE);
            // Format the date
            String formattedDate;


            String email;
            for (int i = 0; i < employes.size(); i++) {
                email = employes.get(i).getEmail();
                if (email != null) {
                    bulletin = new File(System.getProperty("user.home") + "/Downloads/" + employes.get(i).getNatricule() + ".pdf");
                    if(bulletin.exists()){
                        sendEmail(employes.get(i), bulletin);
                        int progress = (i + 1) * 100 / employes.size();
                        int finalI2 = i;
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(progress);

                            logArea.append(employes.get(finalI2).getNatricule() + ".pdf envoyé à " + employes.get(finalI2).getEmail() + "\n");
                            if(!logFile.exists()) {
                                try {
                                    logFile.createNewFile();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            try {
                                log.write("Bulletin de "+employes.get(finalI2).getPrenom() +" "+employes.get(finalI2).getNom()+" envoyé le "+frenchDateFormat.format(new Date())+"\n");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                    else {
                        int finalI1 = i;
                        SwingUtilities.invokeLater(() ->
                                logArea.append("<<<Bulletin de " + employes.get(finalI1).getPrenom() + " introuvable dans "+System.getProperty("user.home") + "/Downloads\n")
                        );

                        try {
                            log.write("Warning: Bulletin de " + employes.get(finalI1).getPrenom() + " "+employes.get(finalI1).getNom()+" introuvable dans "+System.getProperty("user.home") + "/Downloads\n"+frenchDateFormat.format(new Date())+"\n");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    int finalI = i;
                    SwingUtilities.invokeLater(() ->
                            logArea.append("Erreur: Pas d'email trouvé pour " + employes.get(finalI).getPrenom()+" "+employes.get(finalI).getNom() + "\n")
                    );
                    try {
                        log.write("Warning: Pas d'email trouvé pour " + employes.get(finalI).getPrenom() + " "+employes.get(finalI).getNom()+" "+frenchDateFormat.format(new Date())+"\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            log.close();
        }
    }

    private void sendEmail(Employe employe, File attachment) {
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
            logArea.append("Erreur lors de l'envoi de l'email.\nVeulliez vous assurer que les bulletin des employés\nont tous étaient mis dans le dossier Téléchargements.\n");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EnvoiBulletin().setVisible(true));
    }
}


