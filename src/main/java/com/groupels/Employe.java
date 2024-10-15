package com.groupels;

public class Employe {
    private String natricule;
    private String nom;
    private String prenom;
    private String email;

    @Override
    public String toString() {
        return String.format(
                "{\n\t\"Matricule\": \"%s\",\n\t\"Prenom\": \"%s\",\n\t\"Nom\": \"%s\",\n\t\"email\": \"%s\"\n}",
                natricule, prenom, nom, email
        );
    }

    public Employe(String natricule, String prenom, String nom, String email) {
        this.natricule = natricule;
        this.prenom = prenom;
        this.nom = nom;
        this.email = email;
    }

    public String getNatricule() {
        return natricule;
    }

    public void setNatricule(String natricule) {
        this.natricule = natricule;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
