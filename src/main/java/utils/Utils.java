package utils;

import config.IniConfig;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class Utils {
    public static Utils utilsInstance;
    static {
        utilsInstance = new Utils();
    }

    public Utils() {
        loadStopWords();
    }

    public Set<String> stopWords = new HashSet<>();

    public static List<String> getFilesInDirectory(String directory) {
        List<String> filesInDirectory = new ArrayList<>();
        Path path = Paths.get(directory);
        if (Files.isDirectory(path)) {
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                        filesInDirectory.add(filePath.getFileName().toString());
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            filesInDirectory.add(path.getFileName().toString());
        }
        return filesInDirectory;
    }

    private void loadStopWords() {
        IniConfig config = IniConfig.configInstance;
        String stopWordsPath = config.stopWords;

        try {
            BufferedReader input = new BufferedReader(new FileReader(stopWordsPath));
            String word;

            while ((word = input.readLine()) != null)
                stopWords.add(word);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String filterAlphaNum (String str) {
        str = str.replaceAll("[^A-Za-z0-9]", " ");
        str = str.replaceAll("^ +| +$|( )+", "$1");
        return str.toLowerCase();
    }

    public String removeStopWordsFromString (String str) {
        List<String> strWithoutStopWords = new ArrayList<>();
        String[] split = str.split(" ");

        for (String splitStr : split) {
            if (!stopWords.contains(splitStr))
                strWithoutStopWords.add(splitStr);
        }
        return String.join(" ", strWithoutStopWords);
    }

    public List<String> getListRemovedStopWords (String str) {
        List<String> strWithoutStopWords = new ArrayList<>();
        String[] split = str.split(" ");

        for (String splitStr : split) {
            if (!stopWords.contains(splitStr))
                strWithoutStopWords.add(splitStr);
        }
        return strWithoutStopWords;
    }
}
