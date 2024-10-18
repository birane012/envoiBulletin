Ce programme permet d'envoyer les bulletins de salaires des enployées stockés dans le config.json.

1. Le dossier *bulletins_ ce dossier est a mettre dans Documents* doit etre renommé en *bulletins* et 	plac2 dans ~/Documents.

2. Le chemin vers le dossier contenant les bulletins peut etre renseigné depuis l'application
   Ou directement dans le fichier ~/Documents/bulletins/_appFiles/config.json

3. S'assurer que les informations relatives *au serveur SMPT* et *aux employés* sont bien renseignées dans le fichier ~/Documents/bulletins/_appFiles/config.json

4. Le mot de passe de l'expediteur (*password* dans config.json) doit etre 3 fois encripter en base64 pour ne pas l'afficher en dure.

5. Dans le dossier contenant les bulletins, chaque employé doit avoir
   un sous dossier ayant comme nom son matricule.

6. Les bulletins ont pour nom, le numero de matricule du salarier_annee suivi du mois.
   Exemple: 0016_20241030

7. Touts les envois sont tracés dans le fichier ~/Documents/bulletins/_appFiles/log.txt



**NB: Le fichier envoiBulletins.exe et le Dossier JRE doivent tjr etre dans le meme dossier pour s'assurer de l'exécutabilité du programme quelque soit le PC**

