package com.example.recapp;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
//Klasa adaptera dla RecyclerView -> umożliwia automatyczne wyświetlanie listy nagrań z przekazywanej w konstruktorze
//tablicy obieków typu Recording
public class RecordingAdapter extends RecyclerView.Adapter<RecordingAdapter.ViewHolder> {

    private ArrayList<Recording> recordingArrayList;
    private Context context;
    private MediaPlayer mPlayer;
    private boolean isPlaying = false;
    private int last_index = -1; //zawiera index ostatnio odtwarzanego pliku

    public RecordingAdapter(Context context, ArrayList<Recording> recordingArrayList) {
        this.context = context;
        this.recordingArrayList = recordingArrayList;
    }

    //Metoda tworzy nowy obiekt typu ViewHolder dla elementów widoku pojedynczego nagrania
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.recording_item_layout, parent, false);

        return new ViewHolder(view);
    }

    //metoda wywoływana automatycznie podczas przewijania ekranu aktywności
    //przypisuje nagranie do obiektu typu ViewHolder. Obiekty pojawiające się na ekranie podczas przewijania
    //są przypisywane do ViewHolder zamiast tych starych które przestają być widoczne (recykling)
    //sprawdza czy aktualny plik jest odtwarzany, zmienia obrazek, załącza update seekbara
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Recording recording = recordingArrayList.get(position);
        holder.textViewName.setText(recording.getFileName()); //ustawia nazwę pliku w polu tekstowym

        if (recording.isPlaying()) {
            holder.imageViewPlay.setImageResource(R.drawable.ic_pause);
            TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView);
            holder.seekBar.setVisibility(View.VISIBLE);
            holder.seekUpdation(holder);
        } else {
            holder.imageViewPlay.setImageResource(R.drawable.ic_play);
            TransitionManager.beginDelayedTransition((ViewGroup) holder.itemView);
            holder.seekBar.setVisibility(View.GONE);
        }


        holder.manageSeekBar(holder);
    }

    //zwraca rozmiar listy nagrań
    @Override
    public int getItemCount()
    {
        return recordingArrayList.size();
    }

    //klasa zarządzająca wyświetlanymi nagraniami oraz ich elementami widoku
    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageViewPlay, imageViewDelete;
        SeekBar seekBar;
        TextView textViewName;
        private String recordingUri;
        private int lastProgress = 0;
        private Handler mHandler = new Handler();
        ViewHolder holder;

        public ViewHolder(View itemView) {
            super(itemView);

            imageViewPlay = itemView.findViewById(R.id.imageViewPlay);
            imageViewDelete = itemView.findViewById(R.id.imageViewDelete);
            seekBar = itemView.findViewById(R.id.seekBar);
            textViewName = itemView.findViewById(R.id.textViewRecordingname);

            //obsługa zdarzenia kliknięcia na odtworzenie danego pliku
            imageViewPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    Recording recording = recordingArrayList.get(position);

                    recordingUri = recording.getUri();

                    if (isPlaying) { //jeśli plik jest aktualnie odtwarzany. (zmienna globalna)
                        stopPlaying();
                        if (position == last_index) { //jeśli wybrany plik to ten sam który ostatnio był odtwarzany (kliknięto pauzę)
                            recording.setPlaying(false);
                            stopPlaying();
                            notifyItemChanged(position);
                        } else { //wybrano nowy plik do odtwarzania podczas gdy jest odtwarzany inny plik
                            markAllPaused();
                            recording.setPlaying(true);
                            notifyItemChanged(position);
                            startPlaying(recording, position);
                            last_index = position;
                        }

                    } else { //odtworzenie wybranego pliku gdy nic aktualnie nie jest odtwarzane
                        startPlaying(recording, position);
                        recording.setPlaying(true);
                        seekBar.setMax(mPlayer.getDuration());
                        Log.d("isPlayin", "False");
                        notifyItemChanged(position);
                        last_index = position;
                    }

                }
            });

            //obsługa zdarzenia wybrania przycisku usuń dla danego nagrania
            //usuwa plik z ArrayList, RecyclerView oraz z pamięci urządzenia
            imageViewDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    Recording recording = recordingArrayList.get(position);
                    recordingUri = recording.getUri();
                    recordingArrayList.remove(recording);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, recordingArrayList.size());

                    File file = new File(recording.getUri());
                    boolean deleted = file.delete();
                }
            });
        }

        //do zmiany pozycji odtwarzania nagrania gdy przesunięty zostanie seekBar
        public void manageSeekBar(ViewHolder holder) {
            holder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (mPlayer != null && fromUser) {
                        mPlayer.seekTo(progress);
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

        //ustawia stan odtwarzania wszystkich nagrań na false
        private void markAllPaused() {
            for (int i = 0; i < recordingArrayList.size(); i++) {
                recordingArrayList.get(i).setPlaying(false);
                recordingArrayList.set(i, recordingArrayList.get(i));
            }
            notifyDataSetChanged();
        }

        //nowy wątek do aktualizacji seekBar
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                seekUpdation(holder);
            }
        };

        //metoda uruchamiana w osobnym wątku. jej zadaniem jest aktualizacja SeekBar (paska postępu)
        //w celu jego synchronizacji z aktualnie odtwarzanym plikiem audio
        private void seekUpdation(ViewHolder holder) {
            this.holder = holder;
            if (mPlayer != null) {
                int mCurrentPosition = mPlayer.getCurrentPosition();
                holder.seekBar.setMax(mPlayer.getDuration());
                holder.seekBar.setProgress(mCurrentPosition);
                lastProgress = mCurrentPosition;
            }
            mHandler.postDelayed(runnable, 100);
        }

        //zatrzymanie odtwarzania pliku
        private void stopPlaying() {
            try {
                mPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mPlayer = null;
            isPlaying = false;
        }

        //odtwarzanie wybranego pliku
        private void startPlaying(final Recording audio, final int position) {
            mPlayer = new MediaPlayer();
            try {
                mPlayer.setDataSource(recordingUri);
                mPlayer.prepare();
                mPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            seekBar.setMax(mPlayer.getDuration());
            isPlaying = true;

            //gdy odtwarzanie pliku zostanie zakończone
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    audio.setPlaying(false);
                    notifyItemChanged(position);
                }
            });
        }


    }
}