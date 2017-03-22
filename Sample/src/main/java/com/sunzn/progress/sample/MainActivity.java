package com.sunzn.progress.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.sunzn.progress.library.DownLoadProgressButton;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    private DownLoadProgressButton downloadButton;
    private Subscription sub;
    private Observable<Long> obser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        downloadButton = (DownLoadProgressButton) findViewById(R.id.download);
        Button resetButton = (Button) findViewById(R.id.reset);
        downloadButton.setEnablePause(false);


//        downloadButton.setProgress(0);
        downloadButton.finish();


        obser = Observable.interval(700, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread());

        downloadButton.setOnDownLoadClickListener(new DownLoadProgressButton.OnDownLoadClickListener() {
            @Override
            public void clickDownload() {
                sub = obser.subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        if (downloadButton.getState() == DownLoadProgressButton.FINISH) {
                            sub.unsubscribe();
                            return;
                        }
                        int p = new Random().nextInt(10);
                        downloadButton.setProgress(downloadButton.getProgress() + p);
                    }
                });
            }

            @Override
            public void clickPause() {
                sub.unsubscribe();
            }

            @Override
            public void clickResume() {
                clickDownload();
            }

            @Override
            public void clickFinish() {
                if (sub != null) sub.unsubscribe();
                Toast.makeText(MainActivity.this, "阅读", Toast.LENGTH_SHORT).show();
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sub != null) {
                    sub.unsubscribe();
                }
                downloadButton.reset();
            }
        });
    }
}
