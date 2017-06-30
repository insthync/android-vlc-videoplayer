package com.insthync.vlc_videoplayer.sample;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.insthync.vlc_videoplayer.library.VLCVideoPlayer;

public class MainActivity extends AppCompatActivity {

    private VLCVideoPlayer videoPlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);

        videoPlayer = (VLCVideoPlayer)findViewById(R.id.videoPlayer);
        videoPlayer.setSource(Uri.parse("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"));
        videoPlayer.setLoop(true);
        videoPlayer.play();
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        videoPlayer.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        videoPlayer.play();
    }
}
