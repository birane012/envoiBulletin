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
    private Map<String, File> employeBulletinDepuisUnPDF;
    private final JProgressBar progressBar;
    private final JTextField cheminField;
    private final JComboBox<Integer> anneeComboBox;
    private final JComboBox<String> moisComboBox;
    private final JButton envoyerButton;
    private final Map<String, String> moisMap;
    private JSONObject readedJsonConfigFile;//config.json
    private JSONObject config;//(config.json).config ->
    private File logFile;
    private BufferedWriter traceWriter;
    private final ButtonGroup origineGroup;
    private final String[] origineOptions;
    private final JLabel matriculeLabel = new JLabel("Libellé Matricule: *");
    private final JTextField matriculeField;
    private final JComboBox<String> matriculeComboBox;
    private int progressIndex;
    private final SimpleDateFormat frenchDateFormat;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public EnvoiBulletin() throws FileNotFoundException {
        //System.out.println(recursiveDecodeBase64("VWpCNFZGRkVTbkpOYWxGM1RWUkpha2wzUFQwPQ==",3));
        setTitle("Envoi des bulletins de salaire Groupe LS");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Initialisation des composants
        cheminField = new JTextField(40);

        JButton choisirCheminButton = new JButton("Choisir");
        anneeComboBox = new JComboBox<>();
        moisComboBox = new JComboBox<>(new String[]{"Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"});
        String moisLettre1Majuscule = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.FRANCE);
        moisComboBox.setSelectedItem(moisLettre1Majuscule.substring(0, 1).toUpperCase() + moisLettre1Majuscule.substring(1).toLowerCase());

        // Configuration du JComboBox pour l'année
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        Stream.iterate(2019, n -> n + 1).limit(currentYear-2018).forEach(anneeComboBox::addItem);
        anneeComboBox.setSelectedItem(currentYear);

        matriculeField = new JTextField(10);
        matriculeComboBox = new JComboBox<>();

        JButton duplicataButton = new JButton("Duplicata");
        envoyerButton = new JButton("Envoyer tous les bulletins");

        // Création du panneau principal avec un padding à gauche
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0)); // 10 pixels de padding à gauche

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Ajout des composants au panneau principal
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Chemin:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        mainPanel.add(cheminField, gbc);
        gbc.gridx = 3; gbc.gridwidth = 1;
        mainPanel.add(choisirCheminButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(new JLabel("Année:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(anneeComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(new JLabel("Mois:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(moisComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        mainPanel.add(matriculeLabel, gbc);

        gbc.gridx = 1;
        mainPanel.add(matriculeField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        mainPanel.add(new JLabel("Origine:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        JPanel originePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        frenchDateFormat = new SimpleDateFormat("EEEE d MMMM yyyy HH:mm:ss", Locale.FRANCE);

        origineGroup = new ButtonGroup();
        origineOptions = new String[]{"Un pdf", "Un seul dossier", "Dossier par employé"};
        for (String option : origineOptions) {
            JRadioButton radioButton = new JRadioButton(option);
            origineGroup.add(radioButton);
            originePanel.add(radioButton);
            if (option.equals("Dossier par employé")) {
                radioButton.setSelected(true);
            }
            radioButton.addActionListener(e -> updateMatriculeFieldVisibility());
        }
        mainPanel.add(originePanel, gbc);

        // Création du panneau décoratif pour Matricule et Duplicata
        JPanel decorativePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        decorativePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2), // Steel Blue border
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        decorativePanel.setBackground(new Color(240, 248, 255)); // Alice Blue background

        JPanel matriculePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        matriculePanel.setOpaque(false);
        matriculePanel.add(new JLabel("Matricule :"));
        matriculePanel.add(matriculeComboBox);
        decorativePanel.add(matriculePanel);
        decorativePanel.add(duplicataButton);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 4;
        mainPanel.add(decorativePanel, gbc);

        // Configuration du reste de l'interface
        logArea = new JTextArea();
        logArea.setEditable(false);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(envoyerButton, BorderLayout.NORTH);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // Configuration des actions des boutons
        choisirCheminButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(
                    !Objects.equals(getSelectedOrigine(), origineOptions[0]) ?
                            JFileChooser.DIRECTORIES_ONLY :
                            JFileChooser.FILES_AND_DIRECTORIES
            );
            File dir = new File(cheminField.getText());
            fileChooser.setCurrentDirectory(dir.isDirectory()? dir: dir.getParentFile());
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                cheminField.setText(fileChooser.getSelectedFile().getAbsolutePath().replace("\\","/"));
        });

        duplicataButton.addActionListener(e -> {
            envoyerButton.setEnabled(false);
            try {
                traceWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(System.getProperty("user.home") + "/Documents/bulletins/_appFiles/log.txt",true), StandardCharsets.UTF_8));
                updateConfigFileIfChangesHappened();
                if(Objects.equals(getSelectedOrigine(), origineOptions[0]) && employeBulletinDepuisUnPDF==null)
                    employeBulletinDepuisUnPDF = getEmployesBulletinDepuisUnPDF();

                File bulletin=getEmployeBulletin((String)matriculeComboBox.getSelectedItem());
                if(bulletin!=null)
                    envoyerBulletinViaMailEtTracerCetteAction(employeeMap.get(Objects.requireNonNull(matriculeComboBox.getSelectedItem()).toString()),getEmployeBulletin((String) matriculeComboBox.getSelectedItem()),1);
                else {
                    logArea.append("<<<Bulletin de " + employeeMap.get(Objects.requireNonNull(matriculeComboBox.getSelectedItem()).toString()).getPrenom() +" "+employeeMap.get(matriculeComboBox.getSelectedItem().toString()).getNom()+ " introuvable dans "+cheminField.getText()+"\n");
                    traceWriter.append("Warning: Bulletin de ").append(employeeMap.get(matriculeComboBox.getSelectedItem().toString()).getPrenom()).append(" ").append(employeeMap.get(matriculeComboBox.getSelectedItem().toString()).getNom()).append(" introuvable dans ").append(cheminField.getText()).append(" ").append(frenchDateFormat.format(new Date())).append("\n");
                    traceWriter.flush();
                    traceWriter.close();
                }
            } catch (IOException ex) {
                logArea.append(ex.getMessage());
                try {
                    traceWriter.append(ex.getMessage());
                    traceWriter.flush();
                    traceWriter.close();
                } catch (IOException ignored) {
                }
            }
            SwingUtilities.invokeLater(() -> envoyerButton.setEnabled(true));
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
                envoyerTousLesBulletins();
                traceWriter.flush();
                traceWriter.close();
            } catch (IOException ex) {
                logArea.append("Erreur lors de l'envoi des bulletins: " + ex.getMessage() + "\n");
            }
            SwingUtilities.invokeLater(() -> envoyerButton.setEnabled(true));
        });

        //Map qui a tout matricle associe les information de l'employé
        employeeMap = loadEmployeesFromJson();
        //Ajouter les matricule au combobox Maticule
        employeeMap.keySet().forEach(matriculeComboBox::addItem);

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

        // Initialiser la visibilité du champ Matricule
        updateMatriculeFieldVisibility();
    }

    private void updateMatriculeFieldVisibility() {
        boolean isUnPdfSelected = Objects.equals(getSelectedOrigine(), "Un pdf");
        matriculeField.setVisible(isUnPdfSelected);
        matriculeLabel.setVisible(isUnPdfSelected);
        revalidate();
        repaint();
    }

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
            matriculeField.setText((String) ((JSONObject) readedJsonConfigFile.get("config")).get("matriculeLibelle"));

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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void envoyerTousLesBulletins() throws IOException {
        updateConfigFileIfChangesHappened();
        if(employeeMap != null && !employeeMap.isEmpty()){
            List<Employe> employes = new ArrayList<>(employeeMap.values());
            // Define a French date format
            String email;
            boolean succes;
            File bulletinAenvoyer;

            employeBulletinDepuisUnPDF = Map.of();
            if(Objects.equals(getSelectedOrigine(), origineOptions[0])) {
                //Au cas ou le fichier PDF des salaires n'existe pas, getEmployesBulletinDepuisUnPDF
                //declencher un IndexOutOfBoundsException et nous afficherons le message :
                //"Fichier des bulletins : "+cheminField.getText()+" introuvable" dans le UI et le fichier log.txt
                try {
                    employeBulletinDepuisUnPDF = getEmployesBulletinDepuisUnPDF();
                } catch (IndexOutOfBoundsException e) {
                    //"Fichier des bulletins : "+cheminField.getText()+" introuvable"
                    logArea.append(e.getMessage()+"\n");
                    traceWriter.append(e.getMessage()).append("\n");
                }
            }

            //Ce boolean nous permettra de supprimer le PDF d'orignine si l'option choisie est **Un PDF**
            boolean deleteOrignePDF=true;
            progressIndex=0;
            for (int i = 0; i < employes.size(); i++) {
                email = employes.get(i).getEmail();
                if (email != null) {
                    //Reccupérer le bulletin a envoyé selon l'orignine choisie
                    bulletinAenvoyer = getEmployeBulletin(employes.get(i).getNatricule());
                    if(bulletinAenvoyer !=null){
                        succes= envoyerBulletinViaMailEtTracerCetteAction(employes.get(i),bulletinAenvoyer,employes.size());
                        if(!succes)
                            deleteOrignePDF=false;
                    }
                    else {
                        logArea.append("<<<Bulletin de " + employes.get(i).getPrenom() + " introuvable dans "+cheminField.getText()+"\n");
                        try {
                            traceWriter.append("Warning: Bulletin de ").append(employes.get(i).getPrenom()).append(" ").append(employes.get(i).getNom()).append(" introuvable dans ").append(cheminField.getText()).append(" ").append(frenchDateFormat.format(new Date())).append("\n");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    logArea.append("Erreur: Pas d'email trouvé pour " + employes.get(i).getPrenom()+" "+employes.get(i).getNom() + "\n");
                    try {
                        traceWriter.append("Warning: Pas d'email trouvé pour ").append(employes.get(i).getPrenom()).append(" ").append(employes.get(i).getNom()).append(" ").append(frenchDateFormat.format(new Date())).append("\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                if(deleteOrignePDF)
                    new File(cheminField.getText()).delete();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void updateConfigFileIfChangesHappened() throws IOException {
        boolean cheminChanged = !(cheminField.getText().isEmpty() || cheminField.getText() == null) && !cheminField.getText().equals(((JSONObject) readedJsonConfigFile.get("config")).get("path")) && !Objects.equals(getSelectedOrigine(),origineOptions[0]);
        boolean matriculeChanged = !(matriculeField.getText().isEmpty() || matriculeField.getText() == null) && !matriculeField.getText().equals(((JSONObject) readedJsonConfigFile.get("config")).get("matriculeLibelle"));
        //employeeMap=loadEmployeesFromJson();
        if(cheminChanged && !Objects.equals(getSelectedOrigine(),origineOptions[0]))
            ((JSONObject) readedJsonConfigFile.get("config")).put("path", cheminField.getText());

        if(matriculeChanged)
            ((JSONObject) readedJsonConfigFile.get("config")).put("matriculeLibelle", matriculeField.getText());

        if(cheminChanged || matriculeChanged) {
            //Charger le fichier config.json en mode lecture
            FileWriter editConfig = new FileWriter(System.getProperty("user.home") + "/Documents/bulletins/_appFiles/config.json", false);
            //Mettre a jour le fichier config.json avec un format lisible
            new GsonBuilder().setPrettyPrinting().create().toJson(readedJsonConfigFile, editConfig);
            editConfig.flush();
            editConfig.close();
        }
    }

    Boolean envoyerBulletinViaMailEtTracerCetteAction(Employe employe, File bulletinAenvoyer, int nombreEmployes) throws IOException {
        //Envoyer le mail a l'empoyer
        boolean envoiReussie=sendEmail(employe, bulletinAenvoyer);

        if(envoiReussie) {
            //Calculer le ourcentage du progress indicateur
            //Mettre a jour le pourcentage du progress indicateur
            int progress = (progressIndex + 1) * 100 / nombreEmployes;
            progressIndex++;
            SwingUtilities.invokeLater(() -> progressBar.setValue(progress));

            logArea.append(employe.getNatricule() + "_" + anneeComboBox.getSelectedItem() + moisMap.get((String) moisComboBox.getSelectedItem()) + ".pdf envoyé à " + employe.getEmail() + "\n");

            //Si tous les bulletin sont dans un seul emplacement, les classer après l'envoi
            if(Objects.equals(getSelectedOrigine(), origineOptions[1])) {
                //Créer le dossier de l'employé s'il n'existe pas. (cheminField.getText()/MatriculeEmployé)
                createFolderIfNotExists(cheminField.getText()+"/"+employe.getNatricule());
                //Déplacer le bulletin envoyé de cheminField.getText()/nomBulletin.pdf à cheminField.getText()/MatriculeEmployé/nomBulletin.pdf
                var employeFolder= new ArrayList<>(Arrays.asList(bulletinAenvoyer.getAbsolutePath().split("[/\\\\]")));
                employeFolder.add(employeFolder.size()-1,employe.getNatricule());
                String nouveauEmplacementDuBulletin = String.join("/", employeFolder);
                // Déplacer le fichier et le remplacer s'il existe déjà
                Files.move(Paths.get(bulletinAenvoyer.getAbsolutePath()), Paths.get(nouveauEmplacementDuBulletin),REPLACE_EXISTING);
            }
            return true;
        }
        else {
            logArea.append("Verifier egalement que vous êtes bien connecter a internet.\n\n");
            traceWriter.append("Verifier egalement que vous êtes et bien connecter a internet. ").append(frenchDateFormat.format(new Date())).append("\n");
            return false;
        }
    }

    private File getEmployeBulletin(String matricule) throws IndexOutOfBoundsException {
        File bulletinAenvoyer;
        //Recupperer le bulletin du salarié à envoyé
        try {
            if (Objects.equals(getSelectedOrigine(), origineOptions[2]))
                bulletinAenvoyer = getClassifiedEmployeBulletin(matricule);
            else if (Objects.equals(getSelectedOrigine(), origineOptions[0])) {
                bulletinAenvoyer = employeBulletinDepuisUnPDF!=null? employeBulletinDepuisUnPDF.get(matricule): null;
            }
            else {
                bulletinAenvoyer = Arrays.stream(Objects.requireNonNull(new File(cheminField.getText()).listFiles())).filter(b -> b.getName().contains(matricule + "_" + anneeComboBox.getSelectedItem() + moisMap.get((String) moisComboBox.getSelectedItem())))
                        .collect(Collectors.toList()).get(0);
            }
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            bulletinAenvoyer=null;
        }
        return bulletinAenvoyer;
    }

    private String getFileNameFromPath(String path) {
        List<String> splitedPath = Arrays.asList(path.split("/"));
        //System.out.println(splitedPath.get(splitedPath.size()-1));
        return splitedPath.get(splitedPath.size()-1);
    }

    File getClassifiedEmployeBulletin(String matricule) throws IndexOutOfBoundsException {
        //Charger le dossier de l'employé
        File bulletinAenvoyer = new File(cheminField.getText() + "/" + matricule);
        if(bulletinAenvoyer.exists()) {
            //Trouver le bulletin de l'employer a envoyer selon le matricule, l'annee et le mois comme suit:
            //Nom du fichier=0001_202410 par exemple
            //Si les bullelins sont introuvables, un IndexOutOfBoundsException est
            bulletinAenvoyer = Arrays.stream(Objects.requireNonNull(bulletinAenvoyer.listFiles())).filter(b -> b.getName().contains(matricule + "_" + anneeComboBox.getSelectedItem() + moisMap.get((String) moisComboBox.getSelectedItem())))
                    .collect(Collectors.toList()).get(0);
        }
        return bulletinAenvoyer;
    }

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
                        employeIDFromBulletin = findEmployeIDFromBulletin(newDoc);
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
    String findEmployeIDFromBulletin(PDDocument pdf) throws IOException {
        String pdfText = new PDFTextStripper().getText(pdf);
        // Diviser le texte extrait en lignes
        String[] lines = pdfText.split("\\r?\\n");
        String matriculeValue = null;

        // Parcourir les lignes et chercher le mot "Matricule"
        for (String line : lines) {
            // Vérifier si la ligne contient "Matricule" ou "Matricule :"
            if (line.contains(matriculeField.getText())) {
                // Supprimer les espaces ou les caractères spéciaux supplémentaires
                String cleanedLine = line.trim().replaceAll(matriculeField.getText()+"\\s*:?\\s*", matriculeField.getText()+" ");
                // Diviser la ligne en mots
                String[] words = cleanedLine.split("\\s+");
                //System.out.println(Arrays.toString(words));
                // Parcourir les mots pour trouver le mot qui suit "Matricule"
                for (int i = 0; i < words.length; i++) {
                    if (words[i].equals(matriculeField.getText())) {
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
            //Objet du mail
            message.setSubject(config.get("emailObjet").toString().replace("#moisAnnee",date.getMonth().name()+ " "+date.getYear()));
            BodyPart messageBodyPart = new MimeBodyPart();
            ////Body du mail
            messageBodyPart.setText(((String) config.get("emailBody")).replace("#prenom",employe.getPrenom()).replace("#moisAnnee",date.getMonth().getDisplayName(TextStyle.FULL, Locale.FRANCE).toUpperCase()+ " "+ date.getYear()));

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
            logArea.append("Erreur lors de l'envoi du mail à "+employe.getEmail()+".\nVeulliez vous assurer que le bulletin de employés : "+employe.getPrenom()+" "+employe.getNom()+"\nse trouve bien dans le "+(Objects.equals(getSelectedOrigine(), origineOptions[0]) ? "fichier ":"dossier ")+ cheminField.getText()+".\n");
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
        try {
            new EnvoiBulletin().setVisible(true);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
