package com.groupels;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FolderFileObjectMapper {
    public static void main(String[] args) {
        String downloadFolder = "chemin/vers/votre/dossier/Download";
        Map<File, List<File>> folderFileMap = mapFolderToFiles(new File(downloadFolder));
        
        // Afficher le résultat
        for (Map.Entry<File, List<File>> entry : folderFileMap.entrySet()) {
            System.out.println("Dossier : " + entry.getKey().getAbsolutePath());
            System.out.println("Fichiers :");
            for (File file : entry.getValue()) {
                System.out.println("  - " + file.getName());
            }
            System.out.println();
        }
    }

    public static Map<File, List<File>> mapFolderToFiles(File rootDir) {
        Map<File, List<File>> folderFileMap = new HashMap<>();
        
        if (rootDir.exists() && rootDir.isDirectory()) {
            mapFolderToFilesRecursive(rootDir, folderFileMap);
        }
        
        return folderFileMap;
    }

    private static void mapFolderToFilesRecursive(File folder, Map<File, List<File>> folderFileMap) {
        File[] files = folder.listFiles();
        if (files != null) {
            List<File> fileList = new ArrayList<>();
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file);
                } else if (file.isDirectory()) {
                    mapFolderToFilesRecursive(file, folderFileMap);
                }
            }
            if (!fileList.isEmpty()) {
                folderFileMap.put(folder, fileList);
            }
        }
    }
}
