package com.example.recapp;

//Klasa przechowująca informacje o pojedynczym nagraniu
public class Recording {

    String Uri, fileName;
    boolean isPlaying = false;

    //w konstruktorze ustawiane są informacje o nagraniu
    public Recording(String uri, String fileName, boolean isPlaying) {
        Uri = uri;
        this.fileName = fileName;
        this.isPlaying = isPlaying;
    }

    //pobranie informacji o ścieżce do pliku
    public String getUri() {
        return Uri;
    }

    //pobranie nazwy nagrania
    public String getFileName() {
        return fileName;
    }

    //pobranie informacji czy plik jest aktualnie odtwarzany
    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing){
        this.isPlaying = playing;
    }
}