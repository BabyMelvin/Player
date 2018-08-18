package com.anyplayer.ellaplayer.model.subtitle;

/**********************************
 * Copyright 2018 SH-HG Inc.
 * Author:HangCao(Melvin)         
 * Email:hang.yasuo@gmail.com     
 * ProNm:EllaPlayer          
 * Date: 2018/8/14.                 
 **********************************/

import android.content.Context;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.view.accessibility.CaptioningManager;

import java.util.Locale;
import java.util.Vector;

/**
 * The subtitle controller provides the architecture to display subtitles for a
 * media source.  It allows specifying which tracks to display, on which anchor
 * to display them, and also allows adding external, out-of-band subtitle tracks.
 *
 * @hide
 */
public class SubtitleController {
    private Context mContext;
    private MediaTimeProvider mTimeProvider;
    private Vector<Renderer> mRenderers;
    private Vector<SubtitleTrack> mTracks;
    private SubtitleTrack mSelectedTrack;
    private boolean mShowing;
    private final CaptioningManager mCaptioningManager;

    /**
     * Creates a subtitle controller for a media playback object that implements
     * the MediaTimeProvider interface.
     *
     * @param timeProvider
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public SubtitleController(
            Context context,
            MediaTimeProvider timeProvider,
            Listener listener) {
        mContext = context;
        mCaptioningManager = (CaptioningManager) mContext.getSystemService(Context.CAPTIONING_SERVICE);
        mTimeProvider = timeProvider;
        mListener = listener;
        mRenderers = new Vector<Renderer>();
        mShowing = false;
        mTracks = new Vector<SubtitleTrack>();
    }

    /**
     * @return the available subtitle tracks for this media. These include
     * the tracks found by {@link MediaPlayer} as well as any tracks added
     * manually via {@link #addTrack}.
     */
    public SubtitleTrack[] getTracks() {
        SubtitleTrack[] tracks = new SubtitleTrack[mTracks.size()];
        mTracks.toArray(tracks);
        return tracks;
    }

    /**
     * @return the currently selected subtitle track
     */
    public SubtitleTrack getSelectedTrack() {
        return mSelectedTrack;
    }

    private View getSubtitleView() {
        if (mSelectedTrack == null) {
            return null;
        }
        return mSelectedTrack.getView();
    }

    /**
     * Selects a subtitle track.  As a result, this track will receive
     * in-band data from the {@link MediaPlayer}.  However, this does
     * not change the subtitle visibility.
     *
     * @param track The subtitle track to select.  This must be one of the
     *              tracks in {@link #getTracks}.
     * @return true if the track was successfully selected.
     */
    public boolean selectTrack(SubtitleTrack track) {
        if (track != null && !mTracks.contains(track)) {
            return false;
        }
        mTrackIsExplicit = true;
        if (mSelectedTrack == track) {
            return true;
        }
        if (mSelectedTrack != null) {
            mSelectedTrack.hide();
            mSelectedTrack.setTimeProvider(null);
        }
        mSelectedTrack = track;
        mAnchor.setSubtitleView(getSubtitleView());
        if (mSelectedTrack != null) {
            mSelectedTrack.setTimeProvider(mTimeProvider);
            mSelectedTrack.show();
        }
        if (mListener != null) {
            mListener.onSubtitleTrackSelected(track);
        }
        return true;
    }

    /**
     * @return the default subtitle track based on system preferences, or null,
     * if no such track exists in this manager.
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public SubtitleTrack getDefaultTrack() {
        Locale locale = mCaptioningManager.getLocale();
        for (SubtitleTrack track : mTracks) {
            MediaFormat format = track.getFormat();
            String language = format.getString(MediaFormat.KEY_LANGUAGE);
            // TODO: select track with best renderer.  For now, we select first
            // track with local's language or first track if locale has none
            if (locale == null ||
                    locale.getLanguage().equals("") ||
                    locale.getISO3Language().equals(language) ||
                    locale.getLanguage().equals(language)) {
                return track;
            }
        }
        return null;
    }

    private boolean mTrackIsExplicit = false;
    private boolean mVisibilityIsExplicit = false;

    /**
     * @hide
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void selectDefaultTrack() {
        if (mTrackIsExplicit) {
            return;
        }
        SubtitleTrack track = getDefaultTrack();
        if (track != null) {
            selectTrack(track);
            mTrackIsExplicit = false;
            if (!mVisibilityIsExplicit) {
                if (mCaptioningManager.isEnabled()) {
                    show();
                } else {
                    hide();
                }
                mVisibilityIsExplicit = false;
            }
        }
    }

    /**
     * @hide
     */
    public void reset() {
        hide();
        selectTrack(null);
        mTracks.clear();
        mTrackIsExplicit = false;
        mVisibilityIsExplicit = false;
    }

    /**
     * Adds a new, external subtitle track to the manager.
     *
     * @param format the format of the track that will include at least
     *               the MIME type {@link MediaFormat@KEY_MIME}.
     * @return the created {@link SubtitleTrack} object
     */
    public SubtitleTrack addTrack(MediaFormat format) {
        for (Renderer renderer : mRenderers) {
            if (renderer.supports(format)) {
                SubtitleTrack track = renderer.createTrack(format);
                if (track != null) {
                    mTracks.add(track);
                    return track;
                }
            }
        }
        return null;
    }

    /**
     * Show the selected (or default) subtitle track.
     */
    public void show() {
        mShowing = true;
        mVisibilityIsExplicit = true;
        if (mSelectedTrack != null) {
            mSelectedTrack.show();
        }
    }

    /**
     * Hide the selected (or default) subtitle track.
     */
    public void hide() {
        mVisibilityIsExplicit = true;
        if (mSelectedTrack != null) {
            mSelectedTrack.hide();
        }
        mShowing = false;
    }

    /**
     * Interface for supporting a single or multiple subtitle types in {@link
     * MediaPlayer}.
     */
    public abstract static class Renderer {
        /**
         * Called by {@link MediaPlayer}'s {@link SubtitleController} when a new
         * subtitle track is detected, to see if it should use this object to
         * parse and display this subtitle track.
         *
         * @param format the format of the track that will include at least
         *               the MIME type {@link MediaFormat@KEY_MIME}.
         * @return true if and only if the track format is supported by this
         * renderer
         */
        public abstract boolean supports(MediaFormat format);

        /**
         * Called by {@link MediaPlayer}'s {@link SubtitleController} for each
         * subtitle track that was detected and is supported by this object to
         * create a {@link SubtitleTrack} object.  This object will be created
         * for each track that was found.  If the track is selected for display,
         * this object will be used to parse and display the track data.
         *
         * @param format the format of the track that will include at least
         *               the MIME type {@link MediaFormat@KEY_MIME}.
         * @return a {@link SubtitleTrack} object that will be used to parse
         * and render the subtitle track.
         */
        public abstract SubtitleTrack createTrack(MediaFormat format);
    }

    /**
     * Add support for a subtitle format in {@link MediaPlayer}.
     *
     * @param renderer a {@link SubtitleController.Renderer} object that adds
     *                 support for a subtitle format.
     */
    public void registerRenderer(Renderer renderer) {
        // TODO how to get available renderers in the system
        if (!mRenderers.contains(renderer)) {
            // TODO should added renderers override existing ones (to allow replacing?)
            mRenderers.add(renderer);
        }
    }

    /**
     * Subtitle anchor, an object that is able to display a subtitle view,
     * e.g. a VideoView.
     */
    public interface Anchor {
        /**
         * Anchor should set the subtitle view to the supplied view,
         * or none, if the supplied view is null.
         *
         * @param view subtitle view, or null
         */
        public void setSubtitleView(View view);
    }

    private Anchor mAnchor;

    /**
     * @hide
     */
    public void setAnchor(Anchor anchor) {
        if (mAnchor == anchor) {
            return;
        }
        if (mAnchor != null) {
            mAnchor.setSubtitleView(null);
        }
        mAnchor = anchor;
        if (mAnchor != null) {
            mAnchor.setSubtitleView(getSubtitleView());
        }
    }

    public interface Listener {
        /**
         * Called when a subtitle track has been selected.
         *
         * @param track selected subtitle track or null
         * @hide
         */
        public void onSubtitleTrackSelected(SubtitleTrack track);
    }

    private Listener mListener;
}
