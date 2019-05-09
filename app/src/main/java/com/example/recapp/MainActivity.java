package com.example.recapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;

//Klasa główna aplikacji. Obsługuje nagrywanie ścierzki dźwiękowej, jej zapisywanie i ostwarzanie ostatnio nagranego pliku.
//tu też przydzielane są uprawnienia
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    
    private int PERMISSION_REQUEST_CODE = 1967;
    private Toolbar toolbar;
    private Chronometer chronometer; //wyświetlanie czasu nagrywania
    private ImageView imageViewRecord, imageViewPlay, imageViewStop;
    private SeekBar seekBar;
    private LinearLayout linearLayoutRecorder, linearLayoutPlay;
    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;
    private String filePath = null; //przechowuje sciezke do pliku wraz z nazwą
    private int lastProgress = 0; //pozycja na seekBar
    private Handler mHandler = new Handler();
    private boolean isPlaying = false;
    private long timeWhenStopped = 0;

    private PopupWindow popupWindow;
    private Button btnSave;
    private EditText etTitle;
    private String fileName = null;

    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom rnd = new SecureRandom();
    static final int fileNameLength = 6;

    //metoda wywoływana podczas tworzenia aktywności
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //jeśli wersja androida wymaga uprawnień
            getPermissionToRecordAudio();
        }
        initViews();
    }

    //sprawdzenie czy przyznane są uprawnienia - jeśli nie to wyświetlenie na ekranie aplikacji zapytania o przydzelenie
    //aplikacji uprawnień
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void getPermissionToRecordAudio() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);

        }
    }

    //odpowiedź na zarządanie przyznania uprawnień
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length == 3 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "Przyznano uprawnienia", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(this, "Musisz przyznać uprawnienia aby używać tej aplikacji", Toast.LENGTH_SHORT).show();
                finishAffinity();
            }
        }

    }

    //ustawienie elementów widoku tej aktywności i przypisanie ich do zmiennych
    private void initViews() {

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("RecApp");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.black));
        setSupportActionBar(toolbar);

        linearLayoutRecorder = (LinearLayout) findViewById(R.id.linearLayoutRecorder);
        chronometer = (Chronometer) findViewById(R.id.chronometerTimer);
        chronometer.setBase(SystemClock.elapsedRealtime());
        imageViewRecord = (ImageView) findViewById(R.id.imageViewRecord);
        imageViewStop = (ImageView) findViewById(R.id.imageViewStop);
        imageViewPlay = (ImageView) findViewById(R.id.imageViewPlay);
        linearLayoutPlay = (LinearLayout) findViewById(R.id.linearLayoutPlay);
        seekBar = (SeekBar) findViewById(R.id.seekBar);

        imageViewRecord.setOnClickListener(this);
        imageViewStop.setOnClickListener(this);
        imageViewPlay.setOnClickListener(this);
    }

    //obsługa odpowiedzi na zdarzenia kliknięcia poszczególnych elementów widoku
    //przycisków: record, stop, play i pauza
    @Override
    public void onClick(View view) {
        if (view == imageViewRecord) {
            startRecording();
        } else if (view == imageViewStop) {
            stopRecording();
            showPopup(view); //nadanie nazwy
        } else if (view == imageViewPlay) { //play
            if (!isPlaying && filePath != null) {
                isPlaying = true;
                startPlaying();
            } else { //pauza
                isPlaying = false;
                stopPlaying();
            }
        }
    }

    //rozpoczęcie nagrywania + zmiana wyświetlanego przycisku (na stop)
    //+ ustawienie obiektu MediaRecorder, pobranie ścierzki do katalogu aplikacji, nadanie nazwy plikowi
    //uruchomienie chronometra
    private void startRecording() {
        TransitionManager.beginDelayedTransition(linearLayoutRecorder);
        imageViewRecord.setVisibility(View.GONE);
        imageViewStop.setVisibility(View.VISIBLE);
        linearLayoutPlay.setVisibility(View.GONE);

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        File root = android.os.Environment.getExternalStorageDirectory(); //pobiera ścieżke
        File file = new File(root.getAbsolutePath() + "/RecApp"); //ustawiamy ścieżkę na folder aplikacji
        if (!file.exists()) {
            file.mkdirs();
        }
        fileName = String.valueOf(randomString(fileNameLength));
        filePath = root.getAbsolutePath() + "/RecApp/" +
                fileName + ".mp3";  //wpisywanie nazwy nagrania lub generowanie losowej nazwy
        mRecorder.setOutputFile(filePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        lastProgress = 0; //
        seekBar.setProgress(0);
        stopPlaying();
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
    }

    //zatrzymanie odtwarzania pliku + zmiana ikony na 'play' i zatrzymanie chronometra
    private void stopPlaying() {
        try {
            mPlayer.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mPlayer = null;
        imageViewPlay.setImageResource(R.drawable.ic_play);
        timeWhenStopped = chronometer.getBase() - SystemClock.elapsedRealtime();
        chronometer.stop();
    }

    //zakończenie nagrywania + zmiana wyświetlanego przycisku (na play) oraz wyświetlenie panelu do odtwarzania
    //właśnie nagranego pliku
    private void stopRecording() {
        TransitionManager.beginDelayedTransition(linearLayoutRecorder);
        imageViewRecord.setVisibility(View.VISIBLE);
        imageViewStop.setVisibility(View.GONE);
        linearLayoutPlay.setVisibility(View.VISIBLE);

        try {
            mRecorder.stop();
            mRecorder.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mRecorder = null;
        chronometer.stop();
        chronometer.setBase(SystemClock.elapsedRealtime());
        Toast.makeText(this, "Nagranie zapisane.", Toast.LENGTH_SHORT).show();
    }

    //odtwarzanie ostatnio nagranej ścierzki, wraz z obsługą seekBar i chronometer
    private void startPlaying() {
        if (lastProgress != 0) { //jeśli odtwarzanie nagrania było zatrzymane (pauza) to ustawiam czas zapisany przy zatrzymaniu odtwarzania
            chronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
        }
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(filePath);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        imageViewPlay.setImageResource(R.drawable.ic_pause);

        seekBar.setProgress(lastProgress);
        mPlayer.seekTo(lastProgress);
        seekBar.setMax(mPlayer.getDuration());
        seekUpdation();
        chronometer.start();

        //gdy odtwarzanie pliku zostanie zakończone
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                imageViewPlay.setImageResource(R.drawable.ic_play);
                isPlaying = false;
                chronometer.stop();
                chronometer.setBase(SystemClock.elapsedRealtime());
                timeWhenStopped = 0;
                lastProgress = 0;
                mPlayer.seekTo(lastProgress);
            }
        });

        //zmienia punkt odtwarzania nagrania gdy zostanie przesunięty seekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mPlayer != null && fromUser) {
                    mPlayer.seekTo(progress);
                    chronometer.setBase(SystemClock.elapsedRealtime() - mPlayer.getCurrentPosition());
                    lastProgress = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    //nowy wątek do aktualizacji seekBar
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            seekUpdation();
        }
    };

    //metoda uruchamiana w osobnym wątku. jej zadaniem jest aktualizacja SeekBar (paska postępu)
    //w celu jego synchronizacji z aktualnie odtwarzanym plikiem audio
    private void seekUpdation() { //leci w osobnym wątku
        if (mPlayer != null) {
            int mCurrentPosition = mPlayer.getCurrentPosition();
            seekBar.setProgress(mCurrentPosition);
            lastProgress = mCurrentPosition;
        }
        mHandler.postDelayed(runnable, 100);
    }

    //tworzenie opcji menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_menu, menu);
        return true;
    }

    //metoda wywoływana, gdy zostanie wybrana opcja z menu (jest tylko jedna, przenosząca do listy nagrań)
    //za pomocą obiektu Intent zostaje uruchomiona aktywność wyświetlajaca listę nagrać (RecListActivity)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.item_list:
                Intent intent = new Intent(this, RecListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }

    }

    //metoda do obsługi okna popup i zmiany nazwy pliku
    public void showPopup(View view) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_filename, null);
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // dotknięcie poza obszarem okna zamyka je
        popupWindow = new PopupWindow(popupView, width, height, focusable);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, -200);

        etTitle = (EditText) popupView.findViewById(R.id.etUserInput); //trzeba odnieść się do popupView!
        btnSave = (Button) popupView.findViewById(R.id.btn_save);
        etTitle.setText(fileName, TextView.BufferType.EDITABLE);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newName = etTitle.getText().toString();
                File root = android.os.Environment.getExternalStorageDirectory(); //pobieramy ścieżke
                File file = new File(root.getAbsolutePath() + "/RecApp");
                if (file.exists()) {
                    File from = new File(file, fileName + ".mp3");
                    File to = new File(file, newName + ".mp3");
                    if (from.exists())
                        from.renameTo(to);
                    fileName = newName;
                    filePath = root.getAbsolutePath() + "/RecApp/" + fileName + ".mp3";
                }
                popupWindow.dismiss();
            }
        });
    }

    //generuje losowy ciąg znaków, który używany jest do nadania nazwy plikom
    String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }
}