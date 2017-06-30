package com.insthync.vlc_videoplayer.sample;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.insthync.vlc_videoplayer.sample.R;
import com.insthync.vlc_videoplayer.library.VLCVideoPlayer;

public class MainActivity extends AppCompatActivity {

    private VLCVideoPlayer videoPlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoPlayer = (VLCVideoPlayer)findViewById(R.id.videoPlayer);
        videoPlayer.setSource(Uri.parse("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"));
    }

    @Override
    public void onPause() {
        super.onPause();
        // Make sure the player stops playing if the user presses the home button.
        videoPlayer.pause();
    }
}
