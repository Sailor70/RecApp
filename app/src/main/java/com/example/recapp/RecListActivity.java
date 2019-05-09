package com.example.recapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

//Klasa inicjalizująca wyświetlanie listy nagrań. ustawia elementy widoku wraz z RecyclerView,
//pobiera z folderu aplikacji nagrania i tworzy listę obiektów dla tych plików.
public class RecListActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerViewRecordings;
    private ArrayList<Recording> recordingArraylist; //przechowuje aktualnie dostępną listę nagrań
    private RecordingAdapter recordingAdapter;
    private TextView textViewNoRecordings;

    //metoda wywoływana podczas tworzenia aktywności
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rec_list);

        initViews();
        fetchRecordings();
    }

    //ustawianie elementów widoku aktywności
    private void initViews() {
        recordingArraylist = new ArrayList<Recording>();
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Lista nagrań");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.black));
        setSupportActionBar(toolbar);

        //ustawianie strzałki powrotu
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //ustawianie RecyclerView
        recyclerViewRecordings = (RecyclerView) findViewById(R.id.recyclerViewRecordings);
        recyclerViewRecordings.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL, false));
        recyclerViewRecordings.setHasFixedSize(true);

        textViewNoRecordings = (TextView) findViewById(R.id.textViewNoRecordings);

    }

    //wykonuje pobranie z folderu aplikacji listy plików oraz tworzy obiekt klasy Recording dla każdego z plików
    //w przypadku braku plików wyświetla na ekranie informaje o braku plików
    private void fetchRecordings() {
        File root = android.os.Environment.getExternalStorageDirectory();
        String path = root.getAbsolutePath() + "/RecApp";
        Log.d("Files", "Path: " + path);
        File directory = new File(path);
        File[] files = directory.listFiles();
        if( files.length>0 ){
            for (int i = 0; i < files.length; i++) {

                Log.d("Files", "FileName:" + files[i].getName());
                String fileName = files[i].getName();
                String recordingUri = root.getAbsolutePath() + "/RecApp/" + fileName;

                Recording recording = new Recording(recordingUri,fileName,false);
                recordingArraylist.add(recording);
            }

            textViewNoRecordings.setVisibility(View.GONE);
            recyclerViewRecordings.setVisibility(View.VISIBLE);
            setAdaptertoRecyclerView();

        }else{
            textViewNoRecordings.setVisibility(View.VISIBLE);
            recyclerViewRecordings.setVisibility(View.GONE);
        }

    }

    //tworzy nowy adapter dla plików z arrayList i ustawia ten adapter dla RecyclerView
    private void setAdaptertoRecyclerView() {
        recordingAdapter = new RecordingAdapter(this,recordingArraylist);
        recyclerViewRecordings.setAdapter(recordingAdapter);
    }

    //metoda wywoływana, gdy zostanie wybrana opcja powrotu do poprzedniej aktywności (strzałka wstecz)
    //w tym momencie aktywność RecListActivity zostaje zakończona
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){

            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }

    }
}
