package com.insthync.vlc_videoplayer.library;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

/**
 * Created by Ittipon Teerapruettikulchai on 6/29/17.
 */

public class VLCVideoPlayer extends FrameLayout implements
        TextureView.SurfaceTextureListener,
        MediaPlayer.EventListener,
        IVLCVout.Callback,
        View.OnClickListener,
        SeekBar.OnSeekBarChangeListener {

    public static final String TAG = "VLCVideoPlayer";
    private TextureView mTextureView;
    private MediaPlayer mPlayer;
    private Uri mSource;
    private LibVLC mVlcInstance;
    private int mTextureViewWidth;
    private int mTextureViewHeight;
    private int mVideoWidth;
    private int mVideoHeight;

    private View mVideoViewFrame;
    private View mControlsFrame;
    private View mProgressFrame;
    private View mClickFrame;

    private SeekBar mSeeker;
    private TextView mLabelPosition;
    private TextView mLabelDuration;
    private ImageButton mBtnPlayPause;

    private Drawable mPlayDrawable;
    private Drawable mPauseDrawable;
    private boolean mHideControlsOnPlay = true;
    private boolean mAutoPlay = true;
    private boolean mControlsDisabled = false;
    private boolean mAutoFullscreen = false;
    private boolean mLoop = false;

    private boolean mWasPlayed = false;

    public VLCVideoPlayer(Context context) {
        super(context);
        init(context, null);
    }

    public VLCVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public VLCVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mVlcInstance = new LibVLC(context, new VlcOptions().get());

        mPlayDrawable = AppCompatResources.getDrawable(context, R.drawable.videoplayer_action_play);
        mPauseDrawable = AppCompatResources.getDrawable(context, R.drawable.videoplayer_action_pause);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.VLCVideoPlayer, 0, 0);

            try {
                String source = a.getString(R.styleable.VLCVideoPlayer_vvp_source);
                if (source != null && !source.trim().isEmpty()) mSource = Uri.parse(source);
                int playDrawableResId = a.getResourceId(R.styleable.VLCVideoPlayer_vvp_playDrawable, -1);
                int pauseDrawableResId = a.getResourceId(R.styleable.VLCVideoPlayer_vvp_pauseDrawable, -1);

                if (playDrawableResId != -1) {
                    mPlayDrawable = AppCompatResources.getDrawable(context, playDrawableResId);
                }

                if (pauseDrawableResId != -1) {
                    mPauseDrawable = AppCompatResources.getDrawable(context, pauseDrawableResId);
                }

                mHideControlsOnPlay = a.getBoolean(R.styleable.VLCVideoPlayer_vvp_hideControlsOnPlay, true);
                mAutoPlay = a.getBoolean(R.styleable.VLCVideoPlayer_vvp_autoPlay, false);
                mControlsDisabled = a.getBoolean(R.styleable.VLCVideoPlayer_vvp_disableControls, false);

                mAutoFullscreen = a.getBoolean(R.styleable.VLCVideoPlayer_vvp_autoFullscreen, false);
                mLoop = a.getBoolean(R.styleable.VLCVideoPlayer_vvp_loop, false);

            } finally {
                a.recycle();
            }
        }
    }

    public boolean isPlaying() {
        return mPlayer != null && mPlayer.isPlaying();
    }

    public void setSource(@NonNull Uri source) {
        boolean hadSource = mSource != null;

        if (hadSource)
            stop();

        mSource = source;
        if (mPlayer != null) {
            Media media = new Media(mVlcInstance, mSource);
            mPlayer.setMedia(media);

            if (mAutoPlay) {
                mPlayer.play();
            }
        }
    }

    public void setHideControlsOnPlay(boolean hideControlsOnPlay) {
        mHideControlsOnPlay = hideControlsOnPlay;
    }

    public void setAutoFullscreen(boolean autoFullscreen) {
        mAutoFullscreen = autoFullscreen;
    }

    public void setAutoPlay(boolean autoPlay) {
        mAutoPlay = autoPlay;
    }

    public void setLoop(boolean loop) {
        mLoop = loop;
    }

    public void seekTo(@IntRange(from = 0, to = Integer.MAX_VALUE) int pos) {
        if (mPlayer == null) return;
        mPlayer.setTime(pos);
    }

    public void play() {
        if (mPlayer != null)
            mPlayer.play();

        mBtnPlayPause.setImageDrawable(mPauseDrawable);
    }

    public void pause() {
        if (mPlayer != null)
            mPlayer.pause();

        mBtnPlayPause.setImageDrawable(mPlayDrawable);
    }

    public void stop() {
        if (mPlayer != null)
            mPlayer.stop();

        mBtnPlayPause.setImageDrawable(mPlayDrawable);
    }

    public void release() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void adjustAspectRatio(int viewWidth, int viewHeight, int videoWidth, int videoHeight) {
        final double aspectRatio = (double) videoHeight / videoWidth;
        int newWidth, newHeight;

        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }

        final int xoff = (viewWidth - newWidth) / 2;
        final int yoff = (viewHeight - newHeight) / 2;

        final Matrix txform = new Matrix();
        mTextureView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        txform.postTranslate(xoff, yoff);
        mTextureView.setTransform(txform);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (isInEditMode()) {
            return;
        }

        setKeepScreenOn(true);

        final LayoutInflater li = LayoutInflater.from(getContext());

        mPlayer = new MediaPlayer(mVlcInstance);
        mPlayer.setEventListener(this);
        mPlayer.setVideoTrackEnabled(true);

        // Inflate and add video view
        mVideoViewFrame = li.inflate(R.layout.videoplayer_include_view, this, false);
        addView(mVideoViewFrame);

        mTextureView = (TextureView)findViewById(R.id.videoView);
        mTextureView.setSurfaceTextureListener(this);

        // Inflate and add progress
        mProgressFrame = li.inflate(R.layout.videoplayer_include_progress, this, false);
        addView(mProgressFrame);

        // Instantiate and add click frame (used to toggle controls)
        mClickFrame = new FrameLayout(getContext());
        //noinspection RedundantCast
        ((FrameLayout) mClickFrame)
                .setForeground(Util.resolveDrawable(getContext(), R.attr.selectableItemBackground));
        addView(
                mClickFrame,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Inflate controls
        mControlsFrame = li.inflate(R.layout.videoplayer_include_controls, this, false);
        final FrameLayout.LayoutParams controlsLp =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        controlsLp.gravity = Gravity.BOTTOM;
        addView(mControlsFrame, controlsLp);

        if (mControlsDisabled) {
            mClickFrame.setOnClickListener(null);
            mControlsFrame.setVisibility(View.GONE);
        } else {
            mClickFrame.setOnClickListener(
                    new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            toggleControls();
                        }
                    });
        }

        // Retrieve controls
        mSeeker = (SeekBar) mControlsFrame.findViewById(R.id.seeker);
        mSeeker.setOnSeekBarChangeListener(this);

        mLabelPosition = (TextView) mControlsFrame.findViewById(R.id.position);
        mLabelPosition.setText(Util.getDurationString(0, false));

        mLabelDuration = (TextView) mControlsFrame.findViewById(R.id.duration);
        mLabelDuration.setText(Util.getDurationString(0, true));

        mBtnPlayPause = (ImageButton) mControlsFrame.findViewById(R.id.btnPlayPause);
        mBtnPlayPause.setOnClickListener(this);
        mBtnPlayPause.setImageDrawable(mPlayDrawable);

        setControlsEnabled(false);
        if (mSource != null)
            setSource(mSource);
    }

    private void setControlsEnabled(boolean enabled) {
        if (mSeeker == null) return;
        mSeeker.setEnabled(enabled);
        mBtnPlayPause.setEnabled(enabled);

        final float disabledAlpha = .4f;
        mBtnPlayPause.setAlpha(enabled ? 1f : disabledAlpha);

        mClickFrame.setEnabled(enabled);
    }

    public void showControls() {
        if (mControlsDisabled || isControlsShown() || mSeeker == null) return;

        mControlsFrame.animate().cancel();
        mControlsFrame.setAlpha(0f);
        mControlsFrame.setVisibility(View.VISIBLE);
        mControlsFrame
                .animate()
                .alpha(1f)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(
                        new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (mAutoFullscreen) setFullscreen(false);
                            }
                        })
                .start();
    }

    public void hideControls() {
        if (mControlsDisabled || !isControlsShown() || mSeeker == null) return;
        mControlsFrame.animate().cancel();
        mControlsFrame.setAlpha(1f);
        mControlsFrame.setVisibility(View.VISIBLE);
        mControlsFrame
                .animate()
                .alpha(0f)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(
                        new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                setFullscreen(true);

                                if (mControlsFrame != null) mControlsFrame.setVisibility(View.INVISIBLE);
                            }
                        })
                .start();
    }

    public boolean isControlsShown() {
        return !mControlsDisabled && mControlsFrame != null && mControlsFrame.getAlpha() > .5f;
    }

    public void toggleControls() {
        if (mControlsDisabled) return;
        if (isControlsShown()) {
            hideControls();
        } else {
            showControls();
        }
    }

    public void enableControls(boolean andShow) {
        mControlsDisabled = false;
        if (andShow) showControls();
        mClickFrame.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        toggleControls();
                    }
                });
        mClickFrame.setClickable(true);
    }

    public void disableControls() {
        mControlsDisabled = true;
        mControlsFrame.setVisibility(View.GONE);
        mClickFrame.setOnClickListener(null);
        mClickFrame.setClickable(false);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        mTextureViewWidth = width;
        mTextureViewHeight = height;
        adjustAspectRatio(mTextureViewWidth, mTextureViewHeight, mVideoWidth, mVideoHeight);

        IVLCVout vlcOut = mPlayer.getVLCVout();
        if (!vlcOut.areViewsAttached()) {
            vlcOut.addCallback(this);
            vlcOut.setVideoView(mTextureView);
            vlcOut.attachViews();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        mTextureViewWidth = width;
        mTextureViewHeight = height;
        adjustAspectRatio(mTextureViewWidth, mTextureViewHeight, mVideoWidth, mVideoHeight);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    @Override
    public void onNewLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        mVideoWidth = visibleWidth;
        mVideoHeight = visibleHeight;
        adjustAspectRatio(mTextureViewWidth, mTextureViewHeight, mVideoWidth, mVideoHeight);
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {

    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {

    }

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {

    }

    @Override
    public void onEvent(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaPlayer.Event.MediaChanged:
                Log.d(TAG, "MediaChanged");
                mProgressFrame.setVisibility(View.VISIBLE);
                mSeeker.setProgress(0);
                mSeeker.setEnabled(false);
                setControlsEnabled(false);
                mWasPlayed = false;
                break;
            case MediaPlayer.Event.Opening:
                Log.d(TAG, "Opening");
                mWasPlayed = false;
                break;
            case MediaPlayer.Event.Playing:
                Log.d(TAG, "Playing");
                if (!mWasPlayed) {
                    mProgressFrame.setVisibility(View.INVISIBLE);
                    mLabelPosition.setText(Util.getDurationString(0, false));
                    mLabelDuration.setText(Util.getDurationString(mPlayer.getLength(), false));
                    mSeeker.setProgress(0);
                    mSeeker.setMax((int)mPlayer.getLength());
                    setControlsEnabled(true);
                    mWasPlayed = true;
                }
                break;
            case MediaPlayer.Event.Buffering:
                float buffering = event.getBuffering();
                Log.d(TAG, "Buffering: " + buffering);
                if (mSeeker != null) {
                    if (buffering == 100) mSeeker.setSecondaryProgress(0);
                    else mSeeker.setSecondaryProgress(Math.round(mSeeker.getMax() * buffering));
                }
                break;
            case MediaPlayer.Event.TimeChanged:
                long pos = mPlayer.getTime();
                final long dur = mPlayer.getLength();
                if (pos > dur) pos = dur;
                mLabelPosition.setText(Util.getDurationString(pos, false));
                mLabelDuration.setText(Util.getDurationString(dur - pos, true));
                mSeeker.setProgress((int)pos);
                mSeeker.setMax((int)dur);
                break;
            case MediaPlayer.Event.EndReached:
                Log.d(TAG, "EndReached");
                stop();
                if (mLoop) {
                    play();
                }
                break;
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnPlayPause) {
            if (mPlayer.isPlaying()) {
                pause();
            } else {
                if (mHideControlsOnPlay && !mControlsDisabled) hideControls();
                play();
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
        if (fromUser) seekTo(value);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();

        mSeeker = null;
        mLabelPosition = null;
        mLabelDuration = null;
        mBtnPlayPause = null;

        mControlsFrame = null;
        mClickFrame = null;
        mProgressFrame = null;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setFullscreen(boolean fullscreen) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (mAutoFullscreen) {
                int flags = !fullscreen ? 0 : View.SYSTEM_UI_FLAG_LOW_PROFILE;

                ViewCompat.setFitsSystemWindows(mControlsFrame, !fullscreen);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    flags |=
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                    if (fullscreen) {
                        flags |=
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                                        | View.SYSTEM_UI_FLAG_IMMERSIVE;
                    }
                }

                mClickFrame.setSystemUiVisibility(flags);
            }
        }
    }
}
