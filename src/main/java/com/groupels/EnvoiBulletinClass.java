package com.groupels;

import org.json.simple.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public class EnvoiBulletinClass extends JFrame {
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
    private ButtonGroup origineGroup; // Nouveau

    public EnvoiBulletinClass() {
        setTitle("Envoi des bulletins de salaire Groupe LS");
        setSize(600, 450); // Augmenté la taille pour accommoder le nouveau champ
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

        // Ajout du nouveau champ Origine
        gbc.gridx = 0; gbc.gridy = 3;
        inputPanel.add(new JLabel("Origine:"), gbc);
        gbc.gridx = 1;
        JPanel originePanel = new JPanel();
        origineGroup = new ButtonGroup();
        String[] origineOptions = {"1pdf", "Emp unique", "classé"};
        for (String option : origineOptions) {
            JRadioButton radioButton = new JRadioButton(option);
            origineGroup.add(radioButton);
            originePanel.add(radioButton);
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
    }

    // Méthode pour obtenir la valeur sélectionnée de l'origine
    public String getSelectedOrigine() {
        for (Enumeration<AbstractButton> buttons = origineGroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();
            if (button.isSelected()) {
                return button.getText();
            }
        }
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EnvoiBulletinClass().setVisible(true));
    }
}
