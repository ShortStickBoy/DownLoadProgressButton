package com.sunzn.progress.library;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.View;

public class DownLoadProgressButton extends AppCompatTextView {

    private int mTextImageMargin;

    public interface OnDownLoadClickListener {
        void clickDownload();

        void clickPause();

        void clickResume();

        void clickFinish();
    }

    public static class SimpleOnDownLoadClickListener implements OnDownLoadClickListener {
        @Override
        public void clickDownload() {

        }

        @Override
        public void clickPause() {

        }

        @Override
        public void clickResume() {

        }

        @Override
        public void clickFinish() {

        }
    }

    //背景画笔
    private Paint mBackgroundPaint;
    //背景边框画笔
    private Paint mBorderPaint;
    //按钮文字画笔
    private volatile Paint mTextPaint;

    private int mBorderColor;

    private int mLaunchTextColor, mFinishTextColor;
    private int mLaunchTextImage, mFinishTextImage;
    private int mLaunchBackgroundColor, mFinishBackgroundColor;
    private int mUpperBackgroundColor, mLowerBackgroundColor;

    //文字颜色
    private int mLowerTextColor;
    //覆盖后颜色
    private int mUpperTextColor;

    private float mProgress = -1;
    private float mToProgress;
    private int mMaxProgress;
    private int mMinProgress;
    private float mProgressPercent;

    private float mButtonRadius;

    private RectF mBackgroundBounds;
    private LinearGradient mProgressBgGradient;
    private LinearGradient mProgressTextGradient;

    private ValueAnimator mProgressAnimation;

    private CharSequence mCurrentText;

    public static final int NORMAL = 1;
    public static final int DOWNLOADING = 2;
    public static final int PAUSE = 3;
    public static final int FINISH = 4;

    private int mState = -1;
    private float mTextSize;
    private float mBorderWidth;
    private String mNormalText, mDowningText, mFinishText, mPauseText;
    private long mAnimationDuration;
    private OnDownLoadClickListener mOnDownLoadClickListener;
    private boolean mEnablePause = false;


    public DownLoadProgressButton(Context context) {
        this(context, null);
    }

    public DownLoadProgressButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            initAttrs(context, attrs);
            init();
            setupAnimations();
        }
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DownloadProgressButton);
        mTextSize = a.getDimension(R.styleable.DownloadProgressButton_text_size, 10);
        mButtonRadius = a.getFloat(R.styleable.DownloadProgressButton_button_radius, getMeasuredHeight() / 2);
        mAnimationDuration = a.getInt(R.styleable.DownloadProgressButton_animation_duration, 500);

        mBorderColor = a.getColor(R.styleable.DownloadProgressButton_border_color, Color.parseColor("#6699ff"));
        mBorderWidth = a.getDimension(R.styleable.DownloadProgressButton_border_width, 2F);

        mTextImageMargin = a.getInt(R.styleable.DownloadProgressButton_text_image_margin, 5);

        mLaunchTextColor = a.getColor(R.styleable.DownloadProgressButton_launch_text_color, Color.parseColor("#6699ff"));
        mLaunchTextImage = a.getResourceId(R.styleable.DownloadProgressButton_launch_text_image, 0);
        mLaunchBackgroundColor = a.getColor(R.styleable.DownloadProgressButton_launch_background_color, Color.parseColor("#6699ff"));

        mFinishTextColor = a.getColor(R.styleable.DownloadProgressButton_finish_text_color, Color.parseColor("#6699ff"));
        mFinishTextImage = a.getResourceId(R.styleable.DownloadProgressButton_finish_text_image, 0);
        mFinishBackgroundColor = a.getColor(R.styleable.DownloadProgressButton_finish_background_color, Color.parseColor("#6699ff"));

        mLowerBackgroundColor = a.getColor(R.styleable.DownloadProgressButton_process_lower_background_color, Color.parseColor("#6699ff"));
        mUpperBackgroundColor = a.getColor(R.styleable.DownloadProgressButton_process_upper_background_color, Color.parseColor("#6699ff"));

        mLowerTextColor = a.getColor(R.styleable.DownloadProgressButton_process_lower_text_color, mUpperBackgroundColor);
        mUpperTextColor = a.getColor(R.styleable.DownloadProgressButton_process_upper_text_color, Color.WHITE);

        mNormalText = a.getString(R.styleable.DownloadProgressButton_text_normal);
        mDowningText = a.getString(R.styleable.DownloadProgressButton_text_downing);
        mFinishText = a.getString(R.styleable.DownloadProgressButton_text_finish);
        mPauseText = a.getString(R.styleable.DownloadProgressButton_text_pause);
        a.recycle();
    }

    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
        mTextPaint.setTextSize(getTextSize());
        invalidate();
    }

    private void init() {
        mMaxProgress = 100;
        mMinProgress = 0;
        mProgress = 0;
        if (mNormalText == null) {
            mNormalText = "下载原版";
        }
        if (mDowningText == null) {
            mDowningText = "已下载";
        }
        if (mFinishText == null) {
            mFinishText = "阅读原版";
        }
        if (mPauseText == null) {
            mPauseText = "继续";
        }

        //设置背景画笔
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setStyle(Paint.Style.FILL);

        mBorderPaint = new Paint();
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(mBorderWidth);
        mBorderPaint.setColor(mBorderColor);
        //设置文字画笔
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        //解决文字有时候画不出问题
        setLayerType(LAYER_TYPE_SOFTWARE, mTextPaint);

        //初始化状态设为NORMAL
        setState(NORMAL);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnDownLoadClickListener == null) {
                    return;
                }
                if (getState() == NORMAL) {
                    mOnDownLoadClickListener.clickDownload();
                    setState(DOWNLOADING);
                    setProgressText(0);
                } else if (getState() == DOWNLOADING) {
                    if (mEnablePause) {
                        mOnDownLoadClickListener.clickPause();
                        setState(PAUSE);
                    }
                } else if (getState() == PAUSE) {
                    mOnDownLoadClickListener.clickResume();
                    setState(DOWNLOADING);
                    setProgressText((int) mProgress);
                } else if (getState() == FINISH) {
                    mOnDownLoadClickListener.clickFinish();
                }
            }
        });
    }

    private void setupAnimations() {
        mProgressAnimation = ValueAnimator.ofFloat(0, 1).setDuration(mAnimationDuration);
        mProgressAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float timePercent = (float) animation.getAnimatedValue();
                mProgress = ((mToProgress - mProgress) * timePercent + mProgress);
                setProgressText((int) mProgress);
            }
        });
        mProgressAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (mToProgress < mProgress) {
                    mProgress = mToProgress;
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mProgress == mMaxProgress) {
                    setState(FINISH);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isInEditMode()) {
            drawing(canvas);
        }
    }

    private void drawing(Canvas canvas) {
        drawBackground(canvas);
        drawTextAbove(canvas);
    }

    private void drawBackground(Canvas canvas) {
        if (mBackgroundBounds == null) {
            mBackgroundBounds = new RectF();
            if (mButtonRadius == 0) {
                mButtonRadius = getMeasuredHeight() / 2;
            }
            mBackgroundBounds.left = mBorderWidth;
            mBackgroundBounds.top = mBorderWidth;
            mBackgroundBounds.right = getMeasuredWidth() - mBorderWidth;
            mBackgroundBounds.bottom = getMeasuredHeight() - mBorderWidth;
        }
        switch (mState) {
            case NORMAL:
                mBackgroundPaint.setShader(null);
                mBackgroundPaint.setColor(mLaunchBackgroundColor);
                canvas.drawRoundRect(mBackgroundBounds, mButtonRadius, mButtonRadius, mBackgroundPaint);
                break;
            case DOWNLOADING:
            case PAUSE:
                mProgressPercent = mProgress / (mMaxProgress + 0f);
                mProgressBgGradient = new LinearGradient(mBorderWidth, 0, getMeasuredWidth() - mBorderWidth, 0,
                        new int[]{mUpperBackgroundColor, mLowerBackgroundColor},
                        new float[]{mProgressPercent, mProgressPercent + 0.001f},
                        Shader.TileMode.CLAMP);
                mBackgroundPaint.setColor(mUpperBackgroundColor);
                mBackgroundPaint.setShader(mProgressBgGradient);
                canvas.drawRoundRect(mBackgroundBounds, mButtonRadius, mButtonRadius, mBackgroundPaint);
                break;
            case FINISH:
                mBackgroundPaint.setShader(null);
                mBackgroundPaint.setColor(mFinishBackgroundColor);
                canvas.drawRoundRect(mBackgroundBounds, mButtonRadius, mButtonRadius, mBackgroundPaint);
                break;
        }
        canvas.drawRoundRect(mBackgroundBounds, mButtonRadius, mButtonRadius, mBorderPaint);//绘制边框
    }

    private void drawTextAbove(Canvas canvas) {
        mTextPaint.setTextSize(getTextSize());

        mTextPaint.setTextSize(mTextSize);

        final float y = canvas.getHeight() / 2 - (mTextPaint.descent() / 2 + mTextPaint.ascent() / 2);
        if (mCurrentText == null) {
            mCurrentText = "";
        }
        final float textWidth = mTextPaint.measureText(mCurrentText.toString());
        switch (mState) {
            case NORMAL:
                mTextPaint.setShader(null);
                mTextPaint.setColor(mLaunchTextColor);
                if (mFinishTextImage == 0) {
                    canvas.drawText(mCurrentText.toString(), (getMeasuredWidth() - textWidth) / 2, y, mTextPaint);
                } else {
                    Bitmap icon = BitmapFactory.decodeResource(getResources(), mLaunchTextImage);
                    float start = (getMeasuredWidth() - icon.getWidth() - textWidth - mTextImageMargin) / 2;
                    canvas.drawBitmap(icon, start, (getMeasuredHeight() - icon.getHeight()) / 2, mTextPaint);
                    canvas.drawText(mCurrentText.toString(), start + icon.getWidth() + mTextImageMargin, y, mTextPaint);
                }
                break;
            case DOWNLOADING:
            case PAUSE:
                float w = getMeasuredWidth() - 2 * mBorderWidth;
                //进度条压过距离
                float coverlength = w * mProgressPercent;
                //开始渐变指示器
                float indicator1 = w / 2 - textWidth / 2;
                //结束渐变指示器
                float indicator2 = w / 2 + textWidth / 2;
                //文字变色部分的距离
                float coverTextLength = textWidth / 2 - w / 2 + coverlength;
                float textProgress = coverTextLength / textWidth;
                if (coverlength <= indicator1) {
                    mTextPaint.setShader(null);
                    mTextPaint.setColor(mLowerTextColor);
                } else if (indicator1 < coverlength && coverlength <= indicator2) {
                    mProgressTextGradient = new LinearGradient((w - textWidth) / 2 + mBorderWidth, 0, (w + textWidth) / 2 + mBorderWidth, 0, new int[]{mUpperTextColor, mLowerTextColor}, new float[]{textProgress, textProgress + 0.001f}, Shader.TileMode.CLAMP);
                    mTextPaint.setColor(mLowerTextColor);
                    mTextPaint.setShader(mProgressTextGradient);
                } else {
                    mTextPaint.setShader(null);
                    mTextPaint.setColor(mUpperTextColor);
                }
                canvas.drawText(mCurrentText.toString(), (w - textWidth) / 2 + mBorderWidth, y, mTextPaint);
                break;
            case FINISH:
                mTextPaint.setColor(mFinishTextColor);
                if (mFinishTextImage == 0) {
                    canvas.drawText(mCurrentText.toString(), (getMeasuredWidth() - textWidth) / 2, y, mTextPaint);
                } else {
                    Bitmap icon = BitmapFactory.decodeResource(getResources(), mFinishTextImage);
                    float start = (getMeasuredWidth() - icon.getWidth() - textWidth - mTextImageMargin) / 2;
                    canvas.drawBitmap(icon, start, (getMeasuredHeight() - icon.getHeight()) / 2, mTextPaint);
                    canvas.drawText(mCurrentText.toString(), start + icon.getWidth() + mTextImageMargin, y, mTextPaint);
                }
                break;
        }
    }

    public int getState() {
        return mState;
    }

    public void reset() {
        setState(NORMAL);
    }

    public void finish() {
        setState(FINISH);
    }

    public void setState(int state) {
        if (mState != state) {//状态确实有改变
            this.mState = state;
            if (state == FINISH) {
                setCurrentText(mFinishText);
                mProgress = mMaxProgress;
            } else if (state == NORMAL) {
                mProgress = mToProgress = mMinProgress;
                setCurrentText(mNormalText);
            } else if (state == PAUSE) {
                setCurrentText(mPauseText);
            }
            invalidate();
        }
    }

    public void setCurrentText(CharSequence charSequence) {
        mCurrentText = charSequence;
        invalidate();
    }

    public CharSequence getCurrentText() {
        return mCurrentText;
    }

    public float getProgress() {
        return mProgress;
    }

    public void setProgress(float progress) {
//        if (progress <= mMinProgress || progress <= mToProgress || getState() == FINISH) {
//            return;
//        }
        if (getState() == FINISH) {
            return;
        }
        mToProgress = Math.min(progress, mMaxProgress);
        setState(DOWNLOADING);
        if (mProgressAnimation.isRunning()) {
            mProgressAnimation.end();
            mProgressAnimation.start();
        } else {
            mProgressAnimation.start();
        }
    }

    private void setProgressText(int progress) {
        if (getState() == DOWNLOADING) {
            setCurrentText(mDowningText + progress + "%");
        }
    }

    public void pause() {
        setState(PAUSE);
    }

    public float getButtonRadius() {
        return mButtonRadius;
    }

    public void setButtonRadius(float buttonRadius) {
        mButtonRadius = buttonRadius;
    }

    public int getTextColor() {
        return mLowerTextColor;
    }

    @Override
    public void setTextColor(int textColor) {
        mLowerTextColor = textColor;
    }

    public int getTextCoverColor() {
        return mUpperTextColor;
    }

    public void setTextCoverColor(int textCoverColor) {
        mUpperTextColor = textCoverColor;
    }

    public int getMinProgress() {
        return mMinProgress;
    }

    public void setMinProgress(int minProgress) {
        mMinProgress = minProgress;
    }

    public int getMaxProgress() {
        return mMaxProgress;
    }

    public void setMaxProgress(int maxProgress) {
        mMaxProgress = maxProgress;
    }

    public long getAnimationDuration() {
        return mAnimationDuration;
    }

    public void setAnimationDuration(long animationDuration) {
        mAnimationDuration = animationDuration;
        mProgressAnimation.setDuration(animationDuration);
    }

    public OnDownLoadClickListener getOnDownLoadClickListener() {
        return mOnDownLoadClickListener;
    }

    public void setOnDownLoadClickListener(OnDownLoadClickListener onDownLoadClickListener) {
        mOnDownLoadClickListener = onDownLoadClickListener;
    }

    public boolean isEnablePause() {
        return mEnablePause;
    }

    public void setEnablePause(boolean enablePause) {
        mEnablePause = enablePause;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mState = ss.state;
        mProgress = ss.progress;
        mCurrentText = ss.currentText;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, (int) mProgress, mState, mCurrentText.toString());
    }

    public static class SavedState extends BaseSavedState {

        private int progress;
        private int state;
        private String currentText;

        public SavedState(Parcelable parcel, int progress, int state, String currentText) {
            super(parcel);
            this.progress = progress;
            this.state = state;
            this.currentText = currentText;
        }

        private SavedState(Parcel in) {
            super(in);
            progress = in.readInt();
            state = in.readInt();
            currentText = in.readString();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(progress);
            out.writeInt(state);
            out.writeString(currentText);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}