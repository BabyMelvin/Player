package com.anyplayer.ellaplayer.ui.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.anyplayer.ellaplayer.R;
import com.anyplayer.ellaplayer.base.player.EllaPlayer;

import java.util.Map;

/**********************************
 * Copyright 2018 SH-HG Inc.
 * Author:HangCao(Melvin)         
 * Email:hang.yasuo@gmail.com     
 * ProNm:EllaPlayer          
 * Date: 2018/8/13.                 
 **********************************/

public class EllaVideoView extends SurfaceView implements EllaController.EllaPlayerControl {

    private static final String TAG = "EllaVideoView";
    private int mTargetState;
    private int mCurrentState;
    private int mVideoWidth;
    private int mVideoHeight;
    private AudioManager mAudioManager;
    private AudioAttributes mAudioAttributes;

    //all possible internal status
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAYBACK_COMPLETED = 5;
    private SurfaceHolder mSurfaceHolder;
    private EllaPlayer mEllaPlayer;
    private MediaPlayer mMediaPlayer;
    private Uri mUri = null;
    private Map<String, String> mHeaders = null;

    // 从metaData 获得流的能力，是否具有这些功能.
    private boolean mCanPause;
    private boolean mCanSeekBack;
    private boolean mCanSeekForward;
    private EllaController mMediaController;
    private int mCurrentBufferPercentage;

    public EllaVideoView(Context context) {
        super(context);
        init();
    }

    private void init() {
        Log.i(TAG, "EllaVideoView init start.");
        mVideoHeight = 0;
        mVideoWidth = 0;
        //TODO 这里不能使用mSurfaceHolder，这个时候surface还没建立完成，使用的时候会出现 surface has been released
        //mSurfaceHolder = getHolder();
        //Log.i(TAG, "init: " + mSurfaceHolder);
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.i(TAG, "surfaceCreated: " + holder);
                mSurfaceHolder = holder;
                //EllaController ellaController = new EllaController(getContext());
                //setMediaController(ellaController);
                openVideo();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.i(TAG, "surfaceChanged: " + width + "/" + height);
                mSurfaceWidth = width;
                mSurfaceHeight = height;
                if (mMediaController != null) {
                    mMediaController.updateControl(mSurfaceWidth, mSurfaceHeight);
                }
                boolean isValidState = (mTargetState == STATE_PLAYING);
                boolean hasValidSate = (mVideoHeight == mSurfaceHeight && mVideoWidth == mSurfaceWidth);
                if (mMediaPlayer != null && isValidState && hasValidSate) {
                    if (mSeekWhenPrepared != 0) {
                        seekTo(mSeekWhenPrepared);
                    }
                    start();
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.i(TAG, "surfaceDestroyed: " + holder);
                //after we return from this we can't use the surface any more
                mSurfaceHolder = null;
                if (mMediaController != null)
                    mMediaController.hide();
                release(true);
            }
        });
        //这里不能使用mSurfaceHolder变量，因为这里没有完成surfaceCreated回调
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        mCurrentState = STATE_IDLE;
        mTargetState = STATE_IDLE;
    }

    public EllaVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EllaVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //TODO 当SurfaceView 附着的时候，字幕也要附着一下
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        //TODO 去掉字幕附着
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && isInPlaybackState() && mMediaController != null) {
            toggleMediaControllerVisibility();
        }
        return super.onTouchEvent(event);
    }

    private void toggleMediaControllerVisibility() {
        if (mMediaController.isShowing()) {
            mMediaController.hide();
        } else {
            mMediaController.show();
        }
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && isInPlaybackState() && mMediaController != null) {
            toggleMediaControllerVisibility();
        }
        return super.onTrackballEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean isKeySupported = keyCode != KeyEvent.KEYCODE_BACK &&
                keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                keyCode != KeyEvent.KEYCODE_MENU &&
                keyCode != KeyEvent.KEYCODE_CALL &&
                keyCode != KeyEvent.KEYCODE_ENDCALL;
        if (isInPlaybackState() && isKeySupported && mMediaController != null) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                pause();
                mMediaController.show();
            } else {
                start();
                mMediaController.hide();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (!mMediaPlayer.isPlaying()) {
                start();
                mMediaController.hide();
                return true;
            }
        } else {
            toggleMediaControllerVisibility();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.i(TAG, "onMeasure(" + MeasureSpec.toString(widthMeasureSpec) + "," + MeasureSpec.toString(heightMeasureSpec));
        Log.i(TAG, "onMeasure:mVideoWidth=" + mVideoWidth + ",mVideoHeight=" + mVideoHeight);
        //能够获得的尺寸
        int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
        int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
        if (mVideoWidth > 0 && mVideoHeight > 0) {
            int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
                //the size is fixed
                width = widthSpecSize;
                height = heightSpecSize;

                //for compatibility ,we adjust size base on aspect radio
                if (mVideoWidth * height < width * mVideoHeight) {
                    Log.i(TAG, "image too wide,correcting");
                    width = height * mVideoHeight / mVideoHeight;
                } else if (mVideoWidth * height > width * mVideoHeight) {
                    Log.i(TAG, "image too tall,correcting");
                    height = width * mVideoHeight / mVideoWidth;
                }
            } else if (widthSpecMode == MeasureSpec.EXACTLY) {
                // only the width is fixed,adjust the height to match aspect radio
                width = widthMeasureSpec;
                height = width * mVideoHeight / mVideoWidth;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightMeasureSpec) {
                    //couldn't match aspect ratio within the  constrains
                    height = heightSpecSize;
                }
            } else if (heightSpecMode == MeasureSpec.EXACTLY) {
                // only the height is fixed, adjust the width to match aspect ratio if possible
                height = heightSpecSize;
                width = height * mVideoWidth / mVideoHeight;
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    width = widthSpecSize;
                }
            } else {
                // neither the width nor the height are fixed, try to use actual video size
                width = mVideoWidth;
                height = mVideoHeight;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // too tall, decrease both width and height
                    height = heightSpecSize;
                    width = height * mVideoWidth / mVideoHeight;
                }
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // too wide, decrease both width and height
                    width = widthSpecSize;
                    height = width * mVideoHeight / mVideoWidth;
                }
            }
        } else {
            //no size yet just abort size
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        //TODO 操作字幕子控件
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        //TODO 字幕操作
    }

    private void release(boolean clearTargetState) {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            //设置字幕

            mCurrentState = STATE_IDLE;
            if (clearTargetState)
                mTargetState = STATE_IDLE;
            //音频设置
        }
    }

    private void openVideo() {
        if (mUri == null && mSurfaceHolder == null) {
            //not ready for playback just yet,will try again later
            return;
        }
        //we shouldn't clear the target state,because somebody might have called start() previously
        release(false);

        //todo 设置音频
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(mPrepareListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangeListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            Log.d(TAG, "setDataSource:" + mUri);
            mMediaPlayer.setDataSource(getContext(), mUri, mHeaders);
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            Log.d(TAG, "preparing");
            mMediaPlayer.prepareAsync();
            attachMediaController();
            //we don't set the target state here either,but preserve the target state that was there before
            mCurrentState = STATE_PREPARING;
        } catch (Exception e) {
            Log.w(TAG, "Unable to open content: " + mUri, e);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        } finally {
            //TODO 处理字幕
        }
    }

    //非ERROR IDLE PREPARING
    private boolean isInPlaybackState() {
        return (mMediaPlayer != null
                && mCurrentState != STATE_ERROR
                && mCurrentState != STATE_IDLE
                && mCurrentState != STATE_PREPARING);
    }

    private void attachMediaController() {
        if (mMediaPlayer != null && mMediaController != null) {
            //这里设置的是MediaPlayerController

            //将player传递给control
            mMediaController.setPlayer(this);
            mMediaController.setPlayerView(this);
            mMediaController.setEnabled(isInPlaybackState());
        }
    }

    private MediaPlayer.OnPreparedListener mOnPreparedListener = null;
    private MediaPlayer.OnInfoListener mOnInfoListener = null;
    private MediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener = null;
    private MediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = null;
    private MediaPlayer.OnCompletionListener mOnCompletionListener = null;
    private MediaPlayer.OnErrorListener mOnErrorListener = null;

    public void setOnPreparedListener(MediaPlayer.OnPreparedListener l) {
        mOnPreparedListener = l;
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener l) {
        mOnCompletionListener = l;
    }

    public void setOnErrorListener(MediaPlayer.OnErrorListener l) {
        mOnErrorListener = l;
    }

    public void setOnInfoListener(MediaPlayer.OnInfoListener l) {
        mOnInfoListener = l;
    }

    //监听的事件
    private MediaPlayer.OnInfoListener mInfoListener = new MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            if (mOnInfoListener != null) {
                mOnInfoListener.onInfo(mp, what, extra);
            }
            return true;
        }
    };

    private int mSeekWhenPrepared = 0;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private MediaPlayer.OnPreparedListener mPrepareListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            Log.d(TAG, "prepared");
            mCurrentState = STATE_PREPARED;
            //get the capabilities of the player for this stream:canPause?,canSeek?,canSeekForward
            mCanPause = mCanSeekBack = mCanSeekForward = true;
            if (mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(mMediaPlayer);
            }
            //TODO 可以设置控制的UI

            mVideoHeight = mp.getVideoHeight();
            mVideoWidth = mp.getVideoWidth();

            //mSeekWhenPrepared may be changed when seekTo() called
            int seekToPosition = mSeekWhenPrepared;
            if (seekToPosition != 0) {
                seekTo(seekToPosition);
            }
            if (mVideoWidth > 0 && mVideoHeight > 0) {
                Log.i(TAG, "videoSize:" + mVideoWidth + "/" + mVideoHeight);
                Log.i(TAG, "surfaceSize:" + mSurfaceWidth + "/" + mSurfaceHeight);
                getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                if (mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
                    //we didn't actually change the size(总是我们需要的),so we won't get a
                    // a 'surface changed' callback,so start the video here instead of in the callback
                    if (mTargetState == STATE_PLAYING) {
                        Log.i(TAG, "start");
                        start();
                        if (mMediaController != null) {
                            mMediaController.show();
                        }
                    } else if (!isPlaying() && (seekToPosition != 0 || getCurrentPosition() > 0)) {
                        if (mMediaController != null) {
                            //show the media controls when we're paused into a video and make 'em stick
                            mMediaController.show(0);
                        }
                    }
                }
            } else {
                // we don't know the video size yet,but should start anyway
                //the video size might be reported to us later.
                if (mTargetState == STATE_PLAYING) {
                    start();
                }
            }
        }
    };
    private MediaPlayer.OnVideoSizeChangedListener mSizeChangeListener = new MediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
            Log.i(TAG, "onVideoSizeChanged:" + width + "/" + height);
            mVideoHeight = height;
            mVideoWidth = width;
            if (mVideoWidth > 0 && mVideoHeight > 0) {
                getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                requestLayout();//onMeasure()方法被调用
            }
        }
    };
    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            Log.i(TAG, "onCompletion: ");
            mCurrentState = STATE_PLAYBACK_COMPLETED;
            mTargetState = STATE_PLAYBACK_COMPLETED;
            if (mMediaController != null) {
                mMediaController.hide();
            }
            if (mOnCompletionListener != null) {
                mOnCompletionListener.onCompletion(mp);
            }
            //TODO 去除音频绑定 待实现
        }
    };
    private MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.w(TAG, "Error: " + what + "," + extra);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            if (mMediaController != null) {
                mMediaController.hide();
            }

            //if an error handler has been supplied ,use it and finish
            if (mOnErrorListener != null) {
                if (mOnErrorListener.onError(mp, what, extra)) {
                    return true;
                }
            }
            //otherwise,pop up an error dialog so the user knows that something bad
            //has happened,Only try and pop up the dialog
            //if we're attached to a window,when we're going away and no longer have a window
            //don't bother showing the user an error
            if (getWindowToken() != null) {
                Resources resources = getContext().getResources();
                int messageId;
                if (what == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
                    messageId = R.string.VideoView_error_text_invalid_progressive_playback;
                } else {
                    messageId = R.string.VideoView_error_text_unknown;
                }
                new AlertDialog.Builder(getContext())
                        .setMessage(messageId)
                        .setPositiveButton(R.string.VideoView_error_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //if we get here ,there is no Error listener,so at
                                //least inform them that the video is over
                                if (mOnCompletionListener != null) {
                                    mOnCompletionListener.onCompletion(mMediaPlayer);
                                }
                            }
                        })
                        .setCancelable(false)
                        .show();
            }
            return false;
        }
    };
    private MediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            Log.i(TAG, "onBufferingUpdate: ");
            //回调给UI（MediaController）
            mCurrentBufferPercentage = percent;
        }
    };

    @Override
    public void start() {
        if (isInPlaybackState()) {
            mMediaPlayer.start();
            mCurrentState = STATE_PLAYING;
        }
        mTargetState = STATE_PLAYING;
    }

    @Override
    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
        mTargetState = STATE_PAUSED;
    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getDuration();
        }
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public void seekTo(int pos) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(pos);
            mSeekWhenPrepared = 0;
        } else {
            //TODO 这里记录seek 续播时候使用
            mSeekWhenPrepared = pos;
        }
    }

    @Override
    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    @Override
    public boolean canPause() {
        return mCanPause;
    }

    @Override
    public boolean canSeekBackward() {
        return mCanSeekBack;
    }

    @Override
    public boolean canSeekForward() {
        return mCanSeekForward;
    }

    @Override
    public int getAudioSessionId() {
        //TODO 待实现
        return 0;
    }

    //###############对外提供的方法#########################
    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    public void setVideoURI(Uri uri, Map<String, String> headers) {
        mUri = uri;
        mHeaders = headers;
        mSeekWhenPrepared = 0;
        //openVideo();
        requestLayout();
        invalidate();
    }

    public void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }

    public void stopPlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mCurrentState = STATE_IDLE;
            mTargetState = STATE_IDLE;
        }
    }

    public void setMediaController(EllaController mediaController) {
        Log.i(TAG, "setMediaController: ");
        if (mMediaController != null) {
            mMediaController.hide();
        }
        mMediaController = mediaController;
        //TODO 移除，在create 的时候再添加
        //attachMediaController();
    }
}
