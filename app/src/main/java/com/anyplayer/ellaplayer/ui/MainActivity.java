package com.anyplayer.ellaplayer.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.anyplayer.ellaplayer.R;

import java.io.IOException;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private SurfaceView mSurface;
    private SurfaceHolder mHolder;
    private MediaPlayer mMediaPlayer;
    private boolean isPrepared = false;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private boolean isLoop = true;
    private SeekBar mSeekBar;
    public boolean isCompelled;
    private boolean isStartTrackingTouch = false;
    private boolean isSeekComplete;
    private boolean isStopTrackingTouch = false;
    private int mCurrentProgress;
    private int mScreenWidth;
    private int mScreenHeight;
    private Button mButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        View decorView = getWindow().getDecorView();
        decorView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        getWindow().setVolumeControlStream(AudioManager.STREAM_MUSIC);

        */
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        //initView();
        //initPlayer();
    }

    private void initPlayer() {
        Log.i(TAG, "initPlayer: 1");
        mMediaPlayer = new MediaPlayer();
        mSurface.setKeepScreenOn(true);
        Log.i(TAG, "initPlayer: 2");

        Log.i(TAG, "initPlayer: 3");
        try {
            //mMediaPlayer.setDataSource("https://media.w3.org/2010/05/sintel/trailer.mp4");
            mMediaPlayer.setDataSource("http://192.168.61.112/4subtitle_4audio.ts");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "initPlayer: 4");
        mMediaPlayer.prepareAsync();
        Log.i(TAG, "initPlayer: 5");
        mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                Log.i(TAG, "onVideoSizeChanged: ");
            }
        });
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.i(TAG, "onPrepared: ");
                isPrepared = true;
                //mMediaPlayer.start();
            }
        });
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.i(TAG, "onError: ");
                return false;
            }
        });
        mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                Log.i(TAG, "onSeekComplete: ");
                //                if(isStopTrackingTouch){
                //                    isSeekComplete=true;
                //                }
                //                if(isSeekComplete){
                //                    if(mMediaPlayer!=null){
                //                        mMediaPlayer.start();
                //                    }
                //                }
                mMediaPlayer.start();

            }
        });
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.i(TAG, "onCompletion: ");
                isCompelled = true;
                if (isLoop) {
                    if (isPrepared || isPaused) {
                        if (mMediaPlayer != null) {
                            mMediaPlayer.start();
                            isPlaying = true;
                        }
                    }
                }
            }
        });
    }

    private void initView() {
        Log.i(TAG, "initView: ");
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        mSurface = (SurfaceView) findViewById(R.id.media_player_surfaceView);
        mSeekBar = (SeekBar) findViewById(R.id.seek_bar);
        mHolder = mSurface.getHolder();
        mButton = (Button) findViewById(R.id.text_button);
        mHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.i(TAG, "surfaceCreated: ");
                if (mHolder != null) {
                    mMediaPlayer.setDisplay(mHolder);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.i(TAG, "surfaceChanged: ");
                if (mMediaPlayer != null) {
                    mMediaPlayer.setDisplay(holder);
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.i(TAG, "surfaceDestroyed: ");
            }
        });
    }

    public void play(View view) {

        Log.i(TAG, "play: isPrepared=" + isPrepared + "   | isPaused=" + isPaused);
        if (isPrepared || isPaused) {
            if (mMediaPlayer != null) {
                mMediaPlayer.start();
                int currentPosition = mMediaPlayer.getCurrentPosition();
                int duration = mMediaPlayer.getDuration();
                mSeekBar.setMax(duration);
                mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        //Log.i(TAG, "onProgressChanged: ");
                        if (isStartTrackingTouch) {
                            Log.i(TAG, "onProgressChanged: progress=" + progress);
                            mCurrentProgress = progress;
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        Log.i(TAG, "onStartTrackingTouch: ");
                        if (isPlaying) {
                            isStartTrackingTouch = true;
                            if (mMediaPlayer != null) {
                                mMediaPlayer.pause();
                                isPaused = true;
                            }
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        Log.i(TAG, "onStopTrackingTouch: ");
                        isStartTrackingTouch = false;
                        isStopTrackingTouch = true;
                        mMediaPlayer.seekTo(mCurrentProgress);
                        //                        if(isStartTrackingTouch){
                        //                            new Thread(new Runnable() {
                        //                                @Override
                        //                                public void run() {
                        //                                    while(isSeekComplete){
                        //                                        Log.i(TAG, "run: start---------------------------《》");
                        //                                        mMediaPlayer.start();
                        //                                    }
                        //                                }
                        //                            }).start();
                        //
                        //                        }

                    }
                });
                Log.i(TAG, "play: currentPosition=" + currentPosition);
                Log.i(TAG, "play: duration=" + duration);
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        while (!isCompelled) {
                            mSeekBar.setProgress(mMediaPlayer.getCurrentPosition());
                            if (isLoop) {
                                isCompelled = false;
                            }
                        }
                    }
                }).start();
                isPlaying = true;
            }
        }
    }

    public void pause(View view) {
        Log.i(TAG, "pause: isPlaying=" + isPlaying);
        if (isPlaying) {
            if (mMediaPlayer != null) {
                mMediaPlayer.pause();
                isPaused = true;
            }
        }
    }

    public void stop(View view) {
        Log.i(TAG, "stop: ");
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop: ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

    }

    public void noFull(View view) {
        // changeSize();
        String textString = (String) mButton.getText();
        if ("窗口".equals(textString)) {
            Log.i(TAG, "noFull: ");
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mSurface.getLayoutParams();
            lp.width = 500;
            lp.height = 300;
            mSurface.setLayoutParams(lp);
            textString = "全屏";
            mButton.setText(textString);
        } else if ("全屏".equals(textString)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mSurface.setLayoutParams(layoutParams);
            mButton.setText("窗口");
        }
    }

    public void changeSize() {
        String textString = (String) mButton.getText();
        int width = mMediaPlayer.getVideoWidth();
        int height = mMediaPlayer.getVideoHeight();
        Log.i(TAG, "changeSize: width=" + width + " height=" + height);
        Log.i(TAG, "mScreenWidth=" + mScreenWidth + " mScreenHeight=" + mScreenHeight);
        if ("窗口".equals(textString)) {
            if (width > mScreenWidth || height > mScreenHeight) {
                float vWidth = (float) width / (float) mScreenWidth;
                float vHeight = (float) height / (float) mScreenHeight;
                float max = Math.max(vWidth, vHeight);
                width = (int) Math.ceil(width / max);
                height = (int) Math.ceil(height / max);
            }
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, height);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            mSurface.setLayoutParams(layoutParams);
            mButton.setText("全屏");
        } else if ("全屏".equals(textString)) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, height);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            mSurface.setLayoutParams(layoutParams);
            mButton.setText("窗口");
        }
    }

    public void ellaPlayer(View view) {
        Intent intent = new Intent(this, EllaPlayer.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
