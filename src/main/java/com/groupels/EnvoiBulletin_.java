package com.groupels;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnvoiBulletin_ extends JFrame {
    private final JTextArea logArea;
    private Map<String, Employe> employeeMap;
    private final JProgressBar progressBar;
    private JTextField cheminField;
    private JComboBox<Integer> anneeComboBox;
    private JComboBox<String> moisComboBox;
    private JButton choisirCheminButton;
    private JButton envoyerButton;
    Map<String, String> moisMap;
    public EnvoiBulletin_() {
        setTitle("Envoi des bulletins de salaire Groupe LS");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Création des nouveaux composants
        cheminField = new JTextField(20);
        choisirCheminButton = new JButton("Choisir");
        anneeComboBox = new JComboBox<>();
        moisComboBox = new JComboBox<>(
            new String[]{
                "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
            }
        );

        envoyerButton = new JButton("Envoyer les bulletins");

        // Configuration du JComboBox pour l'année
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        Stream.iterate(2019, n -> n + 1).limit(currentYear-2018).forEach(anneeComboBox::addItem);
        anneeComboBox.setSelectedItem(currentYear);

        // Création du panneau pour les nouveaux champs
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Chemin:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(cheminField, gbc);
        gbc.gridx = 2;
        inputPanel.add(choisirCheminButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Année:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(anneeComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Mois:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(moisComboBox, gbc);

        // Configuration du reste de l'interface
        logArea = new JTextArea();
        logArea.setEditable(false);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        setLayout(new BorderLayout());
        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(envoyerButton, BorderLayout.NORTH);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // Configuration des actions des boutons
        choisirCheminButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                cheminField.setText(selectedFile.getAbsolutePath());
            }
        });

        envoyerButton.addActionListener(e -> {
            envoyerButton.setEnabled(false);
            new Thread(() -> {
                try {
                    sendPDFs();
                } catch (IOException ex) {
                    logArea.append("Erreur lors de l'envoi des PDFs: " + ex.getMessage() + "\n");
                }
                SwingUtilities.invokeLater(() -> envoyerButton.setEnabled(true));
            }).start();
        });

        employeeMap = loadEmployeesFromJson();
        moisMap = new HashMap<>();
        moisMap.put("Janvier", "01");
        moisMap.put("Février", "02");
        moisMap.put("Mars", "03");
        moisMap.put("Avril", "04");
        moisMap.put("Mai", "05");
        moisMap.put("Juin", "06");
        moisMap.put("Juillet", "07");
        moisMap.put("Août", "08");
        moisMap.put("Septembre", "09");
        moisMap.put("Octobre", "10");
        moisMap.put("Novembre", "11");
        moisMap.put("Décembre", "12");
    }

    private Map<String, Employe> loadEmployeesFromJson() {
        Map<String, Employe> map = new HashMap<>();
        JSONParser parser = new JSONParser();
        logArea.append("Veulliez cliquer sur le bouton ci-dessus pour \nenvoyer les bulletins.\n\n");

        try (FileReader reader = new FileReader(System.getProperty("user.home") + "/Documents/bulletins/_appFiles/employees.json")) {
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
            List<Employe> employes = new ArrayList<>(employeeMap.values());
            File employeDossier;
            File logFile= new File(System.getProperty("user.home") + "/Documents/bulletins/_appFiles/log.txt");
            FileWriter log=new FileWriter(logFile,true);
            // Define a French date format
            SimpleDateFormat frenchDateFormat = new SimpleDateFormat("EEEE d MMMM yyyy hh:mm:ss", Locale.FRANCE);


            String email;
            for (int i = 0; i < employes.size(); i++) {
                email = employes.get(i).getEmail();
                if (email != null) {
                    employeDossier = new File(System.getProperty("user.home") + "/Documents/bulletins/" + employes.get(i).getNatricule());
                    if(employeDossier.exists()){
                        int finalI3 = i;
                        //Nom du fichier=0001_24_01 par exemple
                        File bulletinAenvoyer =Arrays.stream(employeDossier.listFiles()).filter(b -> b.getName().equals(employes.get(finalI3).getNatricule()+"_"+((String)anneeComboBox.getSelectedItem()).substring(0,2)+"_"+moisMap.get((String)moisComboBox.getSelectedItem())))
                                .collect(Collectors.toList()).get(0);
                        sendEmail(employes.get(i), bulletinAenvoyer);
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
            logArea.append("Erreur lors de l'envoi de l'email.\nVeulliez vous assurer que les bulletin des employés\nont tous étaient mis dans le dossier Téléchargements.\n");
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EnvoiBulletin_().setVisible(true));
    }
}
