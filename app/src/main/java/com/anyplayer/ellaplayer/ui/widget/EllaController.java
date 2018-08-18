package com.anyplayer.ellaplayer.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Formatter;
import java.util.Locale;

/**********************************
 * Copyright 2018 SH-HG Inc.
 * Author:HangCao(Melvin)         
 * Email:hang.yasuo@gmail.com     
 * ProNm:EllaPlayer          
 * Date: 2018/8/16.                 
 **********************************/
public class EllaController extends FrameLayout {
    private boolean mFromXml;
    private boolean mUseFastForward;
    private Context mContext;
    private View mRoot;
    private boolean mShowing = false;

    private View mAnchor;
    //设置消失的时间
    public int mDefaultTimeout = 3000;
    private ImageButton mPauseButton;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mRootLayoutParams;
    private EllaPlayerControl mPlayer;
    private CharSequence mPlayDescription;
    private CharSequence mPauseDescription;
    private ImageButton mFfwdButton;
    private ImageButton mRewButton;
    private ImageButton mNextButton;
    private boolean mListenerSet = false;
    private ImageButton mPrevButton;
    private SeekBar mProgress;
    private TextView mEndTime;
    private TextView mCurrentTime;
    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;
    public OnClickListener mNextListener;
    public OnClickListener mPrevListener;

    public EllaController(@NonNull Context context) {
        this(context, true);
    }

    public EllaController(Context context, boolean useFastForward) {
        super(context);
        mContext = context;
        mUseFastForward = true;
        initFloatingWindowLayout();
        initFloatingWindow();
        //todo accessibilityManager
    }

    private void initFloatingWindow() {
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        assert mWindowManager != null;
        mWindowManager.addView(mRoot, mRootLayoutParams);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        requestFocus();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mRoot != null) {
            initControllerView(mRoot);
        }
    }

    private OnLayoutChangeListener mLayoutChangeListener = new OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            updateFlattingWindowLayout();
            if (mShowing) {
                mWindowManager.updateViewLayout(mRoot, mRootLayoutParams);
            }
        }
    };

    /**
     * 设置这个感觉像是view就是control view.
     * 可以使用videoView或者activity的主view
     * 当使用videoView的时候要用parentView
     *
     * @param view Activity's mainView or videoView's parentView
     */
    public void setAnchorView(View view) {
        if (mAnchor != null) {
            mAnchor.removeOnLayoutChangeListener(mLayoutChangeListener);
        }
        mAnchor = view;
        if (mAnchor != null) {
            mAnchor.addOnLayoutChangeListener(mLayoutChangeListener);
        }
        LayoutParams layoutParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        removeAllViews();
        View v = makeControllerView();
        addView(v, layoutParams);
    }

    protected View makeControllerView() {
        LayoutInflater LayoutInflater = (android.view.LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert LayoutInflater != null;
        mRoot = LayoutInflater.inflate(com.android.internal.R.layout.media_controller, null);
        initControllerView(mRoot);
        return mRoot;
    }

    private void initControllerView(View root) {
        Resources resources = mContext.getResources();
        mPlayDescription = resources.getText(com.android.internal.R.string.lockscreen_transport_play_description);
        mPauseDescription = resources.getText(com.android.internal.R.string.lockscreen_transport_pause_description);
        mPauseButton = (ImageButton) root.findViewById(com.android.internal.R.id.pause);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(mPauseListener);
        }

        mFfwdButton = (ImageButton) root.findViewById(com.android.internal.R.id.ffwd);
        if (mFfwdButton != null) {
            mFfwdButton.setOnClickListener(mFfwdListener);
            if (!mFromXml) {
                mFfwdButton.setVisibility(mUseFastForward ? View.VISIBLE : View.INVISIBLE);
            }
        }
        mRewButton = (ImageButton) root.findViewById(com.android.internal.R.id.rew);
        if (mRewButton != null) {
            mRewButton.setOnClickListener(mRewListener);
            if (!mFromXml) {
                mRewButton.setVisibility(mUseFastForward ? VISIBLE : INVISIBLE);
            }
        }
        //by default these are hidden,they will be enabled when setPrevNextListener() is called
        mNextButton = (ImageButton) root.findViewById(com.android.internal.R.id.next);
        if (mNextButton != null && !mFromXml && !mListenerSet) {
            mNextButton.setVisibility(View.GONE);
        }
        mPrevButton = (ImageButton) root.findViewById(com.android.internal.R.id.prev);
        if (mPrevButton != null && !mFromXml && !mListenerSet) {
            mPrevButton.setVisibility(View.GONE);
        }
        mProgress = (SeekBar) root.findViewById(com.android.internal.R.id.mediacontroller_progress);
        if (mProgress != null) {
            mProgress.setOnSeekBarChangeListener(mSeekListener);
            mProgress.setMax(10000);
        }
        mEndTime = (TextView) root.findViewById(com.android.internal.R.id.time);
        mCurrentTime = (TextView) root.findViewById(com.android.internal.R.id.time_current);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        installPrevNextListener();
    }

    public void setNextListener(OnClickListener nextListener) {
        mNextListener = nextListener;
    }

    public void setPrevListener(OnClickListener prevListener) {
        mPrevListener = prevListener;
    }

    private void installPrevNextListener() {
        if (mNextButton != null) {
            mNextButton.setOnClickListener(mNextListener);
            mNextButton.setEnabled(mNextListener != null);
        }
        if (mPrevButton != null) {
            mPrevButton.setOnClickListener(mPrevListener);
            mPrevButton.setEnabled(mPrevListener != null);
        }
    }

    /**
     * 有两种情形能够触发seekbar 监听
     * 第一种：利用触摸板调整seekbar的位置。onStartTrackingTouch被调用，当被调用的时候，会有一系列onProgressChanged的通知。
     * 通过onStopTrackingTouch，添加mDragging设置为true。避免不间断的播放
     * 第二种：滚动球，这个情况，没有onStartTracking和onStopTrackingTouch通知。只是
     * 简单更新位置不是
     */
    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            show(3600000);
            mDragging = true;
            //通过移除这些欲改变过程的消息，但用户完成之后再重新添加这个消息到消息队列中
            removeCallbacks(mShowingProgress);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mDragging = false;
            setProcess();
            updatePausePlay();
            show(mDefaultTimeout);
            //确保进度合适将来进行更新，调用show() 不能保证，如果我们已经显示，相当于没有其他操作
            post(mShowingProgress);
        }
    };
    private OnClickListener mRewListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int currentPosition = mPlayer.getCurrentPosition();
            currentPosition -= 5000;
            mPlayer.seekTo(currentPosition);
            setProcess();
            show(mDefaultTimeout);
        }
    };
    private OnClickListener mFfwdListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int currentPosition = mPlayer.getCurrentPosition();
            currentPosition += 5000;
            mPlayer.seekTo(currentPosition);
            setProcess();
            show(mDefaultTimeout);
        }
    };
    private OnClickListener mPauseListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            doPauseResume();
            show(mDefaultTimeout);
        }
    };

    private void doPauseResume() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        updatePausePlay();
    }

    /**
     * allocate and initialize the static parts of mRootLayoutParams.
     * must also call updateFloatingWindowLayout() to fill the dynamic parts(y and width)
     * before mRootLayoutParams can be used
     */
    private void initFloatingWindowLayout() {
        mRootLayoutParams = new WindowManager.LayoutParams();
        WindowManager.LayoutParams p = mRootLayoutParams;
        //TODO 这样设置p，简写？
        p.gravity = Gravity.TOP | Gravity.LEFT;
        p.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        p.x = 0;
        p.format = PixelFormat.TRANSLUCENT;
        p.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
        p.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH;
        p.token = null;
        p.windowAnimations = 0;
    }

    public EllaController(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mRoot = this;
        mContext = context;
        mUseFastForward = true;
        mFromXml = true;
        //TODO AccessibilityManager
    }

    public EllaController(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //show until hide is called
                show(0);
                break;
            case MotionEvent.ACTION_UP:
                //start timeout
                show(mDefaultTimeout);
                break;
            case MotionEvent.ACTION_CANCEL:
                hide();
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    public void hide() {
        if (mAnchor == null)
            return;
        if (mShowing) {
            removeCallbacks(mShowingProgress);
            mWindowManager.removeView(mRoot);
            mShowing = false;
        }
    }

    /**
     * 显示controller，在timeout后消失
     *
     * @param timeout
     */
    public void show(int timeout) {
        if (!mShowing && mAnchor != null) {
            setProcess();
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }
            disableUnsupportedButton();
            updateFlattingWindowLayout();
            mWindowManager.addView(this, mRootLayoutParams);
            mShowing = true;
        }
        updatePausePlay();
        //cause the progress bar to be updated even if mShowing
        //was already true.this happen ,for example,if we're
        //paused with the progress bar showing the user hits play
        //TODO post调用
        post(mShowingProgress);

        if (timeout != 0) {
            removeCallbacks(mFadeOut);
            postDelayed(mFadeOut, timeout);
        }
    }


    private Runnable mFadeOut = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    private boolean mDragging;
    private Runnable mShowingProgress = new Runnable() {
        @Override
        public void run() {
            int pos = setProcess();
            if (!mDragging && mShowing && mPlayer.isPlaying()) {
                postDelayed(mShowingProgress, 1000 - (pos % 1000));
            }
        }
    };

    private void updatePausePlay() {
        if (mRoot == null || mPauseButton == null) {
            return;
        }
        if (mPlayer.isPlaying()) {
            mPauseButton.setImageResource(com.android.internal.R.drawable.ic_media_pause);
            mPauseButton.setContentDescription(mPauseDescription);
        } else {
            mPauseButton.setImageResource(com.android.internal.R.drawable.ic_media_play);
            mPauseButton.setContentDescription(mPlayDescription);
        }
    }

    /**
     * update the dynamic parts of mRootLayoutParams
     * must be called with mAnchor !=NULL
     */
    private void updateFlattingWindowLayout() {
        int[] anchorPos = new int[2];
        //获取屏幕的位置
        mAnchor.getLocationOnScreen(anchorPos);
        //we need to know the size of the controller so we can properly position it within its space
        measure(MeasureSpec.makeMeasureSpec(mAnchor.getWidth(), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(mAnchor.getHeight(), MeasureSpec.AT_MOST));
        WindowManager.LayoutParams p = mRootLayoutParams;

        p.width = mAnchor.getWidth();
        p.x = anchorPos[0] + (mAnchor.getWidth() - p.width) / 2;
        p.y = anchorPos[1] + mAnchor.getHeight() - mRoot.getMeasuredHeight();
    }

    private void disableUnsupportedButton() {
        if (mPauseButton != null && !mPlayer.canPause()) {
            mPauseButton.setEnabled(false);
        }
        if (mRewButton != null && !mPlayer.canSeekBackward()) {
            mRewButton.setEnabled(false);
        }
        if (mFfwdButton != null && !mPlayer.canSeekForward()) {
            mFfwdButton.setEnabled(false);
        }

        if (mProgress != null && !mPlayer.canSeekBackward() && !mPlayer.canSeekForward()) {
            mProgress.setEnabled(false);
        }
    }

    private int setProcess() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
        int currentPosition = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                //使用长整型，避免溢出
                long pos = 1000L * currentPosition / duration;
                mProgress.setProgress((int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }
        if (mEndTime != null) {
            mEndTime.setText(stringForTime(duration));
        }
        if (mCurrentTime != null) {
            mCurrentTime.setText(stringForTime(currentPosition));
        }
        return currentPosition;
    }

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 50;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    public void setPlayerController(EllaPlayerControl playerController) {
        mPlayer = playerController;
        updatePausePlay();
    }

    public interface EllaPlayerControl {
        void start();

        void pause();

        int getDuration();

        int getCurrentPosition();

        void seekTo(int pos);

        boolean isPlaying();

        int getBufferPercentage();

        boolean canPause();

        boolean canSeekBackward();

        boolean canSeekForward();

        /**
         * Get the audio session id for the player used by this VideoView. This can be used to
         * apply audio effects to the audio track of a video.
         *
         * @return The audio session, or 0 if there was an error.
         */
        int getAudioSessionId();
    }
}
