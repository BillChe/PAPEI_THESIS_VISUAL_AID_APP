package com.example.visual_aid_app.textdetector;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class TextOptimization {

    // Sample English dictionary (you can replace this with your own or load from a file)
    private static List<String> DICTIONARY = Arrays.asList(
            "hello", "world", "example", "java", "program", "optimize", "ML", "Kit", "Google"
            // Add more words as needed
    );

    public static void main(String[] args) {
        String recognizedText = "hello example program from ML Kit"; // Replace this with the recognized text


    }

    // Method to extract individual words from the recognized text
    public static List<String> extractWords(String text) {
        return Arrays.asList(text.split("\\s+"));
    }
    private static int calculateLevenshteinDistance(String word1, String word2) {
        int m = word1.length();
        int n = word2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    int cost = (word1.charAt(i - 1) == word2.charAt(j - 1)) ? 0 : 1;
                    dp[i][j] = Math.min(dp[i - 1][j] + 1, Math.min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost));
                }
            }
        }

        return dp[m][n];
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static List<String> getWordSuggestions(String word, Context context) {
        List<String> suggestions = new ArrayList<>();
        int maxDistance = 2; // Maximum Levenshtein distance for suggestions
        //DICTIONARY  = loadEnglishWords(context);
        for (String dictWord : DICTIONARY) {
            int distance = calculateLevenshteinDistance(word, dictWord);
            if (distance <= maxDistance) {
                suggestions.add(dictWord);
            }
        }

        // Sort suggestions based on the Levenshtein distance
        suggestions.sort(Comparator.comparingInt(s -> calculateLevenshteinDistance(word, s)));

        return suggestions;
    }
    // Method to optimize the recognized words using the dictionary
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static List<String> optimizeText(List<String> words, Context context) {

        List<String> optimizedWords = new ArrayList<>();
        DICTIONARY  = loadEnglishWords(context);
        for (String word : words) {
            String cleanedWord = cleanWord(word);
            if (DICTIONARY.contains(cleanedWord.toLowerCase())) {
                optimizedWords.add(cleanedWord);
            } else {
                // Handle potentially erroneous words here (e.g., use spell-check suggestions)
                // For simplicity, we'll just keep the original unrecognized word
               optimizedWords.add(cleanedWord);

              /*      System.out.println(cleanedWord);
                    List<String> suggestions = getWordSuggestions(cleanedWord,context);
                    if (!suggestions.isEmpty()) {
                        System.out.println("Suggestions:");
                        System.out.println("Suggestion for word:"+cleanedWord);
                 *//*       for (String suggestion : suggestions) {*//*
                            System.out.println("Suggestion"+
                                    suggestions.get(0));
                            optimizedWords.remove(cleanedWord);
                            optimizedWords.add(suggestions.get(0));
                        //}
                    }*/

            }
        }

        return optimizedWords;
    }
    private static List<String> loadEnglishWords(Context context) {
        List<String> englishWords = new ArrayList<>();
        try {
            InputStream inputStream = context.getAssets().open("words.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String word;
            while ((word = reader.readLine()) != null) {
                englishWords.add(word);
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return englishWords;
    }


    // Method to clean the word from any special characters or symbols
    private static String cleanWord(String word) {
        return word.replaceAll("[^a-zA-Z]", "");
    }
}
