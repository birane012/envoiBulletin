package com.groupels;

import com.google.gson.GsonBuilder;
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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnvoiBulletin extends JFrame {
    private final JTextArea logArea;
    private Map<String, Employe> employeeMap;
    private final JProgressBar progressBar;
    private JTextField cheminField;
    private JComboBox<Integer> anneeComboBox;
    private JComboBox<String> moisComboBox;
    private JButton choisirCheminButton;
    private JButton envoyerButton;
    private Map<String, String> moisMap;
    private JSONObject emailConfig;
    private JSONObject readedConfig;
    private FileWriter editConfig;
    BufferedWriter traceWriter;

    public EnvoiBulletin() {
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
        // Récupperer le mois en cours et mettre la premiere lettre du nom du mois en Majuscule
        // et le mettre comme mois par defaut.
        String moisLettre1Majuscule = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.FRANCE);
        moisComboBox.setSelectedItem(moisLettre1Majuscule.substring(0, 1).toUpperCase() + moisLettre1Majuscule.substring(1).toLowerCase());
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
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                cheminField.setText(fileChooser.getSelectedFile().getAbsolutePath().replace("\\","/"));
        });

        envoyerButton.addActionListener(e -> {
            envoyerButton.setEnabled(false);
                try {
                    File logFile= new File(System.getProperty("user.home") + "/Documents/bulletins/_appFiles/log.txt");
                    if(!logFile.exists()) {
                        try {
                            logFile.createNewFile();
                        } catch (IOException e1) {
                            throw new RuntimeException(e1);
                        }
                    }
                    traceWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(System.getProperty("user.home") + "/Documents/bulletins/_appFiles/log.txt",true), StandardCharsets.UTF_8));
                    //editLog =new FileWriter(logFile,true);
                    // Créer un OutputStreamWriter avec UTF-8
                    sendPDFs();
                    traceWriter.flush();
                    traceWriter.close();
                } catch (IOException ex) {
                    logArea.append("Erreur lors de l'envoi des PDFs: " + ex.getMessage() + "\n");
                }
                SwingUtilities.invokeLater(() -> envoyerButton.setEnabled(true));
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
        logArea.append("Veulliez cliquer sur le bouton **Envoyer les bulletins** ci-dessous pour effectuer l'envoi.\n\n");

        try (FileReader reader = new FileReader(System.getProperty("user.home") + "/Documents/bulletins/_appFiles/config.json")) {
            //Charger le fichier de config
            readedConfig = (JSONObject)parser.parse(reader);
            //Reccuperer le chemin ou les bulletin son stockées et les parametres du serveur SMTP
            emailConfig = (JSONObject) readedConfig.get("config");
            cheminField.setText((String) ((JSONObject) readedConfig.get("config")).get("path"));

            JSONArray employeeList = (JSONArray) readedConfig.get("usersData");
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
            }
        } catch (IOException | ParseException e) {
            logArea.append("Erreur lors de la lecture du fichier config.json: " + e.getMessage() + "\n");
        }

        return map;
    }

    private void sendPDFs() throws IOException {
        //employeeMap=loadEmployeesFromJson();
        if(!(cheminField.getText().isEmpty() || cheminField.getText()==null) && !cheminField.getText().equals((String)((JSONObject) readedConfig.get("config")).get("path"))) {
            ((JSONObject) readedConfig.get("config")).put("path", cheminField.getText());
            //Charger le fichier config.json en mode lecture
            editConfig = new FileWriter(System.getProperty("user.home") + "/Documents/bulletins/_appFiles/config.json", false);
            //Mettre a jour le fichier config.json avec un format lisible
            new GsonBuilder().setPrettyPrinting().create().toJson(readedConfig, editConfig);
            editConfig.flush();
            editConfig.close();
        }

        if(employeeMap != null && !employeeMap.isEmpty()){
            List<Employe> employes = new ArrayList<>(employeeMap.values());
            File employeDossier;

            // Define a French date format
            SimpleDateFormat frenchDateFormat = new SimpleDateFormat("EEEE d MMMM yyyy HH:mm:ss", Locale.FRANCE);
            String email;
            for (int i = 0; i < employes.size(); i++) {
                email = employes.get(i).getEmail();
                if (email != null) {
                    employeDossier = new File((String) emailConfig.get("path") + "/" + employes.get(i).getNatricule());
                    if(employeDossier.exists()){
                        //Nom du fichier=0001_202410 par exemple
                        int finalI = i;
                        File bulletinAenvoyer=Arrays.stream(employeDossier.listFiles()).filter(b -> b.getName().contains(employes.get(finalI).getNatricule() + "_" + anneeComboBox.getSelectedItem() + moisMap.get((String) moisComboBox.getSelectedItem())))
                                .collect(Collectors.toList()).get(0);
                        //Envoyer le mail a l'empoyer
                        boolean envoiReussie=sendEmail(employes.get(i), bulletinAenvoyer);
                        //Calculer le ourcentage du progress indicateur
                            //Mettre a jour le ourcentage du progress indicateur
                            progressBar.setValue((i + 1) * 100 / employes.size());
                            if(envoiReussie)
                                logArea.append(employes.get(i).getNatricule() + "_" + anneeComboBox.getSelectedItem() + moisMap.get((String) moisComboBox.getSelectedItem()) + ".pdf envoyé à " + employes.get(i).getEmail() + "\n");
                            else
                                logArea.append("Verifier egalement que vous et bien connecter a internet.\n");

                            try {
                                if(envoiReussie)
                                    traceWriter.append("Bulletin de "+employes.get(i).getPrenom() +" "+employes.get(i).getNom()+" envoyé le "+frenchDateFormat.format(new Date())+"\n");
                                else
                                    traceWriter.append("Verifier egalement que vous étes et bien connecter a internet. "+frenchDateFormat.format(new Date())+"\n");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                    }
                    else {
                        logArea.append("<<<Bulletin de " + employes.get(i).getPrenom() + " introuvable dans "+(String) emailConfig.get("path")+"\n");

                        try {
                            traceWriter.append("Warning: Bulletin de " + employes.get(i).getPrenom() + " "+employes.get(i).getNom()+" introuvable dans "+(String) emailConfig.get("path")+" "+frenchDateFormat.format(new Date())+"\n");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    logArea.append("Erreur: Pas d'email trouvé pour " + employes.get(i).getPrenom()+" "+employes.get(i).getNom() + "\n");
                    try {
                        traceWriter.append("Warning: Pas d'email trouvé pour " + employes.get(i).getPrenom() + " "+employes.get(i).getNom()+" "+frenchDateFormat.format(new Date())+"\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private boolean sendEmail(Employe employe, File attachment) {

        Properties properties = System.getProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", (String) emailConfig.get("EnableTTLS").toString());
        properties.setProperty("mail.smtp.host", (String) emailConfig.get("smtpServer"));
        properties.put("mail.smtp.port", emailConfig.get("smtpPort").toString());

        Session session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication((String) emailConfig.get("senderMail"), (String) emailConfig.get("password"));
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress((String) emailConfig.get("senderMail")));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(employe.getEmail()));
            LocalDate date = LocalDate.now();
            message.setSubject(emailConfig.get("emailObjet").toString().replace("#moisAnnee",date.getMonth().name()+ " "+date.getYear()));


            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(
                /*    "Bonjour "+employe.getPrenom() + "\n\n"+
                            "Veuillez trouver ci-joint votre bulletin de salaire de "+ date.getMonth().getDisplayName(TextStyle.FULL, Locale.FRANCE).toUpperCase()+ " "+ date.getYear()+
                            "\n\nCordialement";*/
            ((String) emailConfig.get("emailBody")).replace("#prenom",employe.getPrenom()).replace("#moisAnnee",date.getMonth().getDisplayName(TextStyle.FULL, Locale.FRANCE).toUpperCase()+ " "+ date.getYear())
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
            return true;
        } catch (MessagingException mex) {
            logArea.append("Erreur lors de l'envoi de l'email.\nVeulliez vous assurer que les bulletin des employés\nont tous étaient mis dans le dossier Téléchargements.\n");
            return false;
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EnvoiBulletin().setVisible(true));
    }
}
