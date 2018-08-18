package com.anyplayer.ellaplayer.model.subtitle;

/**********************************
 * Copyright 2018 SH-HG Inc.
 * Author:HangCao(Melvin)         
 * Email:hang.yasuo@gmail.com     
 * ProNm:EllaPlayer          
 * Date: 2018/8/14.                 
 **********************************/
/** @hide */
public interface MediaTimeProvider {
    // we do not allow negative media time
    /**
     * Presentation time value if no timed event notification is requested.
     */
    public final static long NO_TIME = -1;
    /**
     * Cancels all previous notification request from this listener if any.  It
     * registers the listener to get seek and stop notifications.  If timeUs is
     * not negative, it also registers the listener for a timed event
     * notification when the presentation time reaches (becomes greater) than
     * the value specified.  This happens immediately if the current media time
     * is larger than or equal to timeUs.
     *
     * @param timeUs presentation time to get timed event callback at (or
     *               {@link #NO_TIME})
     */
    public void notifyAt(long timeUs, OnMediaTimeListener listener);
    /**
     * Cancels all previous notification request from this listener if any.  It
     * registers the listener to get seek and stop notifications.  If the media
     * is stopped, the listener will immediately receive a stop notification.
     * Otherwise, it will receive a timed event notificaton.
     */
    public void scheduleUpdate(OnMediaTimeListener listener);
    /**
     * Cancels all previous notification request from this listener if any.
     */
    public void cancelNotifications(OnMediaTimeListener listener);
    /**
     * Get the current presentation time.
     *
     * @param precise   Whether getting a precise time is important. This is
     *                  more costly.
     * @param monotonic Whether returned time should be monotonic: that is,
     *                  greater than or equal to the last returned time.  Don't
     *                  always set this to true.  E.g. this has undesired
     *                  consequences if the media is seeked between calls.
     * @throw IllegalStateException if the media is not initialized
     */
    public long getCurrentTimeUs(boolean precise, boolean monotonic)
            throws IllegalStateException;
    /** @hide */
    public static interface OnMediaTimeListener {
        /**
         * Called when the registered time was reached naturally.
         *
         * @param timeUs current media time
         */
        void onTimedEvent(long timeUs);
        /**
         * Called when the media time changed due to seeking.
         *
         * @param timeUs current media time
         */
        void onSeek(long timeUs);
        /**
         * Called when the playback stopped.  This is not called on pause, only
         * on full stop, at which point there is no further current media time.
         */
        void onStop();
    }
}

