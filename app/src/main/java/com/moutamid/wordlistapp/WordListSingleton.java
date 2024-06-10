package com.moutamid.wordlistapp;

import java.util.List;

public class WordListSingleton {
    private static WordListSingleton instance;
    private List<String[]> wordsList;

    private WordListSingleton() {}

    public static synchronized WordListSingleton getInstance() {
        if (instance == null) {
            instance = new WordListSingleton();
        }
        return instance;
    }

    public List<String[]> getWordsList() {
        return wordsList;
    }

    public void setWordsList(List<String[]> wordsList) {
        this.wordsList = wordsList;
    }
}
