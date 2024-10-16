package com.groupels;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FolderFileMapper {
    public static void main(String[] args) {
        String downloadFolder = "chemin/vers/votre/dossier/Download";
        Map<String, List<String>> folderFileMap = mapFolderToFiles(downloadFolder);
        
        // Afficher le résultat
        for (Map.Entry<String, List<String>> entry : folderFileMap.entrySet()) {
            System.out.println("Dossier : " + entry.getKey());
            System.out.println("Fichiers :");
            for (String file : entry.getValue()) {
                System.out.println("  - " + file);
            }
            System.out.println();
        }
    }

    public static Map<String, List<String>> mapFolderToFiles(String rootPath) {
        Map<String, List<String>> folderFileMap = new HashMap<>();
        File rootDir = new File(rootPath);
        
        if (rootDir.exists() && rootDir.isDirectory()) {
            mapFolderToFilesRecursive(rootDir, folderFileMap);
        }
        
        return folderFileMap;
    }

    private static void mapFolderToFilesRecursive(File folder, Map<String, List<String>> folderFileMap) {
        File[] files = folder.listFiles();
        if (files != null) {
            List<String> fileList = new ArrayList<>();
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file.getName());
                } else if (file.isDirectory()) {
                    mapFolderToFilesRecursive(file, folderFileMap);
                }
            }
            if (!fileList.isEmpty()) {
                folderFileMap.put(folder.getAbsolutePath(), fileList);
            }
        }
    }
}
