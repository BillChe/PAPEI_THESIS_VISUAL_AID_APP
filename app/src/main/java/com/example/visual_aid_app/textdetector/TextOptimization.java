package com.example.visual_aid_app.textdetector;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    // Method to optimize the recognized words using the dictionary
    public static List<String> optimizeText(List<String> words,Context context) {
        List<String> optimizedWords = new ArrayList<>();
        DICTIONARY  = loadEnglishWords(context);
        for (String word : words) {
            String cleanedWord = cleanWord(word);
            if (DICTIONARY.contains(cleanedWord.toLowerCase())) {
                optimizedWords.add(cleanedWord);
            } else {
                // Handle potentially erroneous words here (e.g., use spell-check suggestions)
                // For simplicity, we'll just keep the original unrecognized word
                optimizedWords.add(word);
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
