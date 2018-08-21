package com.anyplayer.ellaplayer.ui;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;

import com.anyplayer.ellaplayer.R;
import com.anyplayer.ellaplayer.ui.widget.EllaController;
import com.anyplayer.ellaplayer.ui.widget.EllaVideoView;

public class EllaPlayer extends Activity {

    private EllaVideoView mEllaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_ella_player);
        initView();
    }


    private void initView() {
        mEllaPlayer = (EllaVideoView) findViewById(R.id.video_view);
        mEllaPlayer.setVideoURI(Uri.parse("https://media.w3.org/2010/05/sintel/trailer.mp4"));
        EllaController ellaController = new EllaController(this);
        mEllaPlayer.setMediaController(ellaController);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mEllaPlayer.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
