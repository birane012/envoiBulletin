package com.groupels;

import com.google.gson.GsonBuilder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class EnvoiBulletin extends JFrame {
    private final JTextArea logArea;
    private Map<String, Employe> employeeMap;
    private JProgressBar progressBar;
    private JTextField cheminField;
    private JComboBox<Integer> anneeComboBox;
    private JComboBox<String> moisComboBox;
    private JButton choisirCheminButton;
    private JButton envoyerButton;
    private Map<String, String> moisMap;
    private JSONObject readedJsonConfigFile;//config.json
    private JSONObject config;//(config.json).config ->
    private FileWriter editConfig;
    private File logFile;
    private BufferedWriter traceWriter;
    private ButtonGroup origineGroup;
    private String[] origineOptions;

    public EnvoiBulletin() {
        setTitle("Envoi des bulletins de salaire Groupe LS");
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Création des composants existants
        cheminField = new JTextField(20);
        choisirCheminButton = new JButton("Choisir");
        anneeComboBox = new JComboBox<>();
        moisComboBox = new JComboBox<>(
                new String[]{
                        "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                        "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
                }
        );
        String moisLettre1Majuscule = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.FRANCE);
        moisComboBox.setSelectedItem(moisLettre1Majuscule.substring(0, 1).toUpperCase() + moisLettre1Majuscule.substring(1).toLowerCase());
        envoyerButton = new JButton("Envoyer les bulletins");

        // Configuration du JComboBox pour l'année
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        Stream.iterate(2019, n -> n + 1).limit(currentYear-2018).forEach(anneeComboBox::addItem);
        anneeComboBox.setSelectedItem(currentYear);

        // Création du panneau pour les champs
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

        // Ajout du champ Origine avec "classé" sélectionné par défaut
        gbc.gridx = 0; gbc.gridy = 3;
        inputPanel.add(new JLabel("Origine:"), gbc);
        gbc.gridx = 1;

        JPanel originePanel = new JPanel();
        origineGroup = new ButtonGroup();
        origineOptions = new String[]{"Un pdf", "Un seul dossier", "Dossier par employé"};
        for (String option : origineOptions) {
            JRadioButton radioButton = new JRadioButton(option);
            origineGroup.add(radioButton);
            originePanel.add(radioButton);
            if (option.equals(origineOptions[2])) {
                radioButton.setSelected(true);
            }
        }
        inputPanel.add(originePanel, gbc);

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
            fileChooser.setFileSelectionMode(
                    ! getSelectedOrigine().equals(origineOptions[0]) ?
                            JFileChooser.DIRECTORIES_ONLY :
                            JFileChooser.FILES_AND_DIRECTORIES
            );
            File dir = new File(cheminField.getText());
            fileChooser.setCurrentDirectory(dir.isDirectory()? dir: dir.getParentFile());
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                cheminField.setText(fileChooser.getSelectedFile().getAbsolutePath().replace("\\","/"));
        });

        envoyerButton.addActionListener(e -> {
            envoyerButton.setEnabled(false);
                try {
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
        logFile= new File(System.getProperty("user.home") + "/Documents/bulletins/_appFiles/log.txt");
        //getEmployeBulletinUnPDF();
        //System.out.println(getSelectedOrigine());
    }

    // Méthode pour obtenir la valeur sélectionnée de l'origine
    private String getSelectedOrigine() {
        for (Enumeration<AbstractButton> buttons = origineGroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();
            if (button.isSelected()) {
                return button.getText();
            }
        }
        return null;
    }

    private Map<String, Employe> loadEmployeesFromJson() {
        Map<String, Employe> map = new HashMap<>();
        JSONParser parser = new JSONParser();
        logArea.append("Veulliez cliquer sur le bouton **Envoyer les bulletins** ci-dessous pour effectuer l'envoi.\n\n");

        try (FileReader reader = new FileReader(System.getProperty("user.home") + "/Documents/bulletins/_appFiles/config.json")) {
            //Charger le fichier de config
            readedJsonConfigFile = (JSONObject)parser.parse(reader);
            //Reccuperer le chemin ou les bulletin son stockées et les parametres du serveur SMTP
            config = (JSONObject) readedJsonConfigFile.get("config");
            cheminField.setText((String) ((JSONObject) readedJsonConfigFile.get("config")).get("path"));

            JSONArray employeeList = (JSONArray) readedJsonConfigFile.get("usersData");
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
        if(!(cheminField.getText().isEmpty() || cheminField.getText()==null) && !cheminField.getText().equals((String)((JSONObject) readedJsonConfigFile.get("config")).get("path"))) {
            ((JSONObject) readedJsonConfigFile.get("config")).put("path", cheminField.getText());
            //Charger le fichier config.json en mode lecture
            editConfig = new FileWriter(System.getProperty("user.home") + "/Documents/bulletins/_appFiles/config.json", false);
            //Mettre a jour le fichier config.json avec un format lisible
            new GsonBuilder().setPrettyPrinting().create().toJson(readedJsonConfigFile, editConfig);
            editConfig.flush();
            editConfig.close();
        }

        if(employeeMap != null && !employeeMap.isEmpty()){
            List<Employe> employes = new ArrayList<>(employeeMap.values());
            // Define a French date format
            SimpleDateFormat frenchDateFormat = new SimpleDateFormat("EEEE d MMMM yyyy HH:mm:ss", Locale.FRANCE);
            String email;
            boolean envoiReussie;
            File employeDossier,bulletinAenvoyer;
            Map<String, File> employeBulletinDepuisUnPDF = Map.of();

            if(getSelectedOrigine().equals(origineOptions[0])) {
                //Au cas ou le fichier PDF des salaires n'existe pas, getEmployesBulletinDepuisUnPDF
                //declencher un IndexOutOfBoundsException et nous afficherons le message :
                //"Fichier des bulletins : "+cheminField.getText()+" introuvable" dans le UI et le fichier log.txt
                try {
                    employeBulletinDepuisUnPDF = getEmployesBulletinDepuisUnPDF();
                } catch (IndexOutOfBoundsException e) {
                    //"Fichier des bulletins : "+cheminField.getText()+" introuvable"
                    logArea.append(e.getMessage()+"\n");
                    traceWriter.append(e.getMessage()+"\n");
                }
            }

            int progressIndex = 0;
            //Ce boolean nous permettra de supprimer le PDF d'orignine si l'option choisie est **Un PDF**
            boolean deleteOrignePDF=true;
            for (int i = 0; i < employes.size(); i++) {
                email = employes.get(i).getEmail();
                if (email != null) {
                    //Recupperer le bulletin du salarié à envoyé
                    try {
                        if (getSelectedOrigine().equals(origineOptions[2]))
                            bulletinAenvoyer = getEmployeBulletinClassé(employes.get(i).getNatricule());
                        else if (getSelectedOrigine().equals(origineOptions[0]))
                            bulletinAenvoyer = employeBulletinDepuisUnPDF.get(employes.get(i).getNatricule());
                        else {
                            int finalI = i;
                            bulletinAenvoyer = Arrays.stream(new File(cheminField.getText()).listFiles()).filter(b -> b.getName().contains(employes.get(finalI).getNatricule() + "_" + anneeComboBox.getSelectedItem() + moisMap.get((String) moisComboBox.getSelectedItem())))
                                    .collect(Collectors.toList()).get(0);
                        }
                    } catch (IndexOutOfBoundsException e) {
                        bulletinAenvoyer=null;
                    }

                    if(bulletinAenvoyer !=null){
                        //Envoyer le mail a l'empoyer
                        envoiReussie=sendEmail(employes.get(i), bulletinAenvoyer);

                        if(envoiReussie) {
                            //Calculer le ourcentage du progress indicateur
                            //Mettre a jour le pourcentage du progress indicateur
                            int progress = (progressIndex + 1) * 100 / employes.size();
                            progressIndex++;
                            SwingUtilities.invokeLater(() -> progressBar.setValue(progress));

                            logArea.append(employes.get(i).getNatricule() + "_" + anneeComboBox.getSelectedItem() + moisMap.get((String) moisComboBox.getSelectedItem()) + ".pdf envoyé à " + employes.get(i).getEmail() + "\n");

                            //Si tous les bulletin sont dans un seul emplacement, les classer après l'envoi
                           if(getSelectedOrigine().equals(origineOptions[1])) {
                               //Créer le dossier de l'employé s'il n'existe pas. (cheminField.getText()/MatriculeEmployé)
                               createFolderIfNotExists(cheminField.getText()+"/"+employes.get(i).getNatricule());
                               //Déplacer le bulletin envoyé de cheminField.getText()/nomBulletin.pdf à cheminField.getText()/MatriculeEmployé/nomBulletin.pdf
                               var employeFolder= new ArrayList<>(Arrays.asList(bulletinAenvoyer.getAbsolutePath().split("[/\\\\]")));
                               employeFolder.add(employeFolder.size()-1,employes.get(i).getNatricule());
                               String nouveauEmplacementDuBulletin = String.join("/", employeFolder);
                               // Déplacer le fichier et le remplacer s'il existe déjà
                               Files.move(Paths.get(bulletinAenvoyer.getAbsolutePath()), Paths.get(nouveauEmplacementDuBulletin),REPLACE_EXISTING);
                           }
                        }
                        else {
                            logArea.append("Verifier egalement que vous êtes bien connecter a internet.\n");
                            if(deleteOrignePDF)
                                deleteOrignePDF=false;
                        }

                        try {
                            if(envoiReussie)
                                traceWriter.append("Bulletin de "+employes.get(i).getPrenom() +" "+employes.get(i).getNom()+" envoyé le "+frenchDateFormat.format(new Date())+"\n");
                            else
                                traceWriter.append("Verifier egalement que vous êtes et bien connecter a internet. "+frenchDateFormat.format(new Date())+"\n");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    else {
                        logArea.append("<<<Bulletin de " + employes.get(i).getPrenom() + " introuvable dans "+cheminField.getText()+"\n");

                        try {
                            traceWriter.append("Warning: Bulletin de " + employes.get(i).getPrenom() + " "+employes.get(i).getNom()+" introuvable dans "+cheminField.getText()+" "+frenchDateFormat.format(new Date())+"\n");
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

                if(deleteOrignePDF)
                    new File(cheminField.getText()).delete();
            }
        }
    }

    private String getFileNameFromPath(String path) {
        List<String> splitedPath = Arrays.asList(path.split("/"));
        //System.out.println(splitedPath.get(splitedPath.size()-1));
        return splitedPath.get(splitedPath.size()-1);
    }

    File getEmployeBulletinClassé(String matricule) throws IndexOutOfBoundsException {
        //Charger le dossier de l'employé
        File bulletinAenvoyer = new File(cheminField.getText() + "/" + matricule);
        if(bulletinAenvoyer.exists()) {
            //Trouver le bulletin de l'employer a envoyer selon le matricule, l'annee et le mois comme suit:
            //Nom du fichier=0001_202410 par exemple
                //Si les bullelins sont introuvables, un IndexOutOfBoundsException est
                bulletinAenvoyer = Arrays.stream(bulletinAenvoyer.listFiles()).filter(b -> b.getName().contains(matricule + "_" + anneeComboBox.getSelectedItem() + moisMap.get((String) moisComboBox.getSelectedItem())))
                        .collect(Collectors.toList()).get(0);
        }
        return bulletinAenvoyer;
    }

   /* File getEmployeBulletinUnSeulDossier(String matricule){
        File employeBulletin = new File(cheminField.getText()+"/"+matricule + "_" + anneeComboBox.getSelectedItem() + moisMap.get((String) moisComboBox.getSelectedItem()));
        if(employeBulletin.exists()) {
            //Nom du fichier=0001_202410 par exemple
            return employeBulletin;
        }
        return employeBulletin;
    }*/

    Map<String,File> getEmployesBulletinDepuisUnPDF() throws IndexOutOfBoundsException { //nomFichier as param
        File employesBulletin = new File(cheminField.getText());
        Map<String,File> employeBulletinAenvoyeMap = new HashMap<>();
        if (employesBulletin.exists()) {
            try (PDDocument document = PDDocument.load(employesBulletin)) {
                //int pageCount = document.getNumberOfPages();
                document.getPages().forEach(pdPage -> {
                    PDDocument newDoc = new PDDocument();
                    newDoc.addPage(pdPage);
                    String fileName;
                    String employeIDFromBulletin;
                    try {
                        // Extraire tout le texte du PDF
                        employeIDFromBulletin = findEmployeIDFromBulletin(newDoc, "Matricule");
                        // Vérifier si les répertoires parent existent, sinon les créer
                        //Nom du fichier=0001_202410 par exemple
                        fileName = createFolderIfNotExists(employesBulletin.getParent()+"/" + employeIDFromBulletin) +"/"+  employeIDFromBulletin +"_" + anneeComboBox.getSelectedItem() + moisMap.get((String) moisComboBox.getSelectedItem()) + document.getPages().indexOf(pdPage) + ".pdf";
                        newDoc.save(fileName);
                        //System.out.println(employeIDFromBulletin);
                        newDoc.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                    employeBulletinAenvoyeMap.put(employeIDFromBulletin,new File(fileName));
                });
                return employeBulletinAenvoyeMap;
            } catch (IOException ignored) {
            }
            return employeBulletinAenvoyeMap;
        }
        else
            throw new IndexOutOfBoundsException("Fichier des bulletins : "+cheminField.getText()+" introuvable");
    }

    private String createFolderIfNotExists(String cheminDossierAcreer) throws IOException {
        if(! new File(cheminDossierAcreer).exists())
            return Files.createDirectories(Paths.get(cheminDossierAcreer)).toString();
        return cheminDossierAcreer;
    }

    //matriculeLibelle: Ce champs correspond au libelle de l'identifiant de l'employé:
    //Exemple: "Maticule"
    String findEmployeIDFromBulletin(PDDocument pdf, String matriculeLibelle) throws IOException {
        String pdfText = new PDFTextStripper().getText(pdf);
        // Diviser le texte extrait en lignes
        String[] lines = pdfText.split("\\r?\\n");
        String matriculeValue = null;

        // Parcourir les lignes et chercher le mot "Matricule"
        for (String line : lines) {
            // Vérifier si la ligne contient "Matricule" ou "Matricule :"
            if (line.contains(matriculeLibelle)) {
                // Supprimer les espaces ou les caractères spéciaux supplémentaires
                String cleanedLine = line.trim().replaceAll(matriculeLibelle+"\\s*:?\\s*", matriculeLibelle+" ");
                // Diviser la ligne en mots
                String[] words = cleanedLine.split("\\s+");
                //System.out.println(Arrays.toString(words));
                // Parcourir les mots pour trouver le mot qui suit "Matricule"
                for (int i = 0; i < words.length; i++) {
                    if (words[i].equals(matriculeLibelle)) {
                        if (i + 1 < words.length) {
                            // Le mot suivant après "Matricule"
                            matriculeValue = words[i + 1];
                            break;
                        }
                    }
                }

                // Stopper la recherche une fois le matricule trouvé
                if (matriculeValue != null) {
                    break;
                }
            }
        }
        return matriculeValue;
    }


    private boolean sendEmail(Employe employe, File attachment) {

        Properties properties = System.getProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", config.get("EnableTTLS").toString());
        properties.setProperty("mail.smtp.host", (String) config.get("smtpServer"));
        properties.put("mail.smtp.port", config.get("smtpPort").toString());

        Session session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication(){
                return new PasswordAuthentication((String) config.get("senderMail"), recursiveDecodeBase64((String) config.get("password"),3));
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress((String) config.get("senderMail")));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(employe.getEmail()));
            LocalDate date = LocalDate.now();
            message.setSubject(config.get("emailObjet").toString().replace("#moisAnnee",date.getMonth().name()+ " "+date.getYear()));


            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(
                /*    "Bonjour "+employe.getPrenom() + "\n\n"+
                            "Veuillez trouver ci-joint votre bulletin de salaire de "+ date.getMonth().getDisplayName(TextStyle.FULL, Locale.FRANCE).toUpperCase()+ " "+ date.getYear()+
                            "\n\nCordialement";*/
            ((String) config.get("emailBody")).replace("#prenom",employe.getPrenom()).replace("#moisAnnee",date.getMonth().getDisplayName(TextStyle.FULL, Locale.FRANCE).toUpperCase()+ " "+ date.getYear())
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
            logArea.append("Erreur lors de l'envoi du mail à "+employe.getEmail()+".\nVeulliez vous assurer que le bulletin de employés : "+employe.getPrenom()+" "+employe.getNom()+"\nà bien été mis dans le dossier "+cheminField.getText()+".\n");
            return false;
        }
    }

    // Fonction récursive pour décoder la chaîne de caractères encodée en base64
    public static String recursiveDecodeBase64(String encodedString, int times) {
        // Cas de base: si "times" est égal à 0, retourner la chaîne
        if (times == 0)
            return encodedString;
        // Appel récursif avec "times" décrémenté
        return recursiveDecodeBase64(new String(Base64.getDecoder().decode(encodedString)), times - 1);
    }

    public static String recursiveEncodeBase64(String originalString, int times) {
        // Cas de base: si "times" est égal à 0, retourner la chaîne d'origine
        if (times == 0)
            return originalString;
        // Appel récursif avec "times" décrémenté
        return recursiveEncodeBase64(Base64.getEncoder().encodeToString(originalString.getBytes()), times - 1);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EnvoiBulletin().setVisible(true));
    }
}
