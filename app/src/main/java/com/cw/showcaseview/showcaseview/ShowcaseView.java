package com.cw.showcaseview.showcaseview;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.cw.showcaseview.R;
import com.cw.showcaseview.showcaseview.animation.AlphaAnimationFactory;
import com.cw.showcaseview.showcaseview.animation.IAnimationFactory;
import com.cw.showcaseview.showcaseview.queue.ShowcaseQueue;
import com.cw.showcaseview.showcaseview.shape.CircleShape;
import com.cw.showcaseview.showcaseview.shape.IShape;
import com.cw.showcaseview.showcaseview.shape.OvalShape;
import com.cw.showcaseview.showcaseview.shape.RectangleShape;
import com.cw.showcaseview.showcaseview.target.ViewTarget;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Cw
 * @date 2017/7/24
 */
public class ShowcaseView extends FrameLayout implements View.OnClickListener {

    public static final int CIRCLE_SHAPE = 0;
    public static final int RECTANGLE_SHAPE = 1;
    public static final int OVAL_SHAPE = 2;

    private String mMaskColor = "#99000000";//蒙版的背景颜色
    private boolean mDismissOnTouch;//是否触摸任意地方消失
    private int mTargetPadding;//透明块的内边距
    private long mShowDuration;//show的渐显时间
    private long mMissDuration;//miss的渐隐时间

    private Activity mActivity;
    private Bitmap mBitmap;
    private Paint mPaint;
    private Canvas mCanvas;
    private ViewGroup mDecorView;
    private AbsoluteLayout mContentView;
    private Map<ViewTarget, IShape> mTargets = new HashMap<>();
    private AlphaAnimationFactory mAnimationFactory = new AlphaAnimationFactory();

    public ShowcaseView(@NonNull Activity act) {
        this(act, null);
    }

    public ShowcaseView(@NonNull Activity act, @Nullable AttributeSet attrs) {
        this(act, attrs, 0);
    }

    public ShowcaseView(@NonNull Activity act, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(act, attrs, defStyleAttr);
        init(act);
    }

    private void init(Activity act) {
        this.mActivity = act;
        //ViewGroup重写onDraw，需要调用setWillNotDraw(false)
        setWillNotDraw(false);
        setOnClickListener(this);
        setVisibility(INVISIBLE);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.showcase_content, this, true);
        mContentView = (AbsoluteLayout) view.findViewById(R.id.content_box);
        mDecorView = (ViewGroup) mActivity.getWindow().getDecorView();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mBitmap == null) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        }
        if (mCanvas == null) {
            mCanvas = new Canvas(mBitmap);
        }
        if (mPaint == null) {
            mPaint = new Paint();
            mPaint.setColor(Color.TRANSPARENT);
            //将canvas置为透明的重要方法
            mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mCanvas.setBitmap(mBitmap);
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mCanvas.drawColor(Color.parseColor(mMaskColor));

        for (Map.Entry<ViewTarget, IShape> entry : mTargets.entrySet()) {
            ViewTarget target = entry.getKey();
            IShape shape = entry.getValue();
            shape.draw(mCanvas, mPaint, target, mTargetPadding);
        }
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    @Override
    public void onClick(View v) {
        if (mDismissOnTouch) {
            removeFromWindow();
        }
    }

    /**
     * Builder创建类,对showcaseView创建并进行一些配置
     */
    public static class Builder {

        private ShowcaseView showcaseView;

        public Builder(Activity activity) {
            showcaseView = new ShowcaseView(activity);
        }

        /**
         * 设置蒙版颜色
         */
        public Builder setMaskColor(String color) {
            showcaseView.setMaskColor(color);
            return this;
        }

        /**
         * 设置渐显时间
         *
         * @param showDur show的渐显时间
         * @param missDur miss的渐隐时间
         */
        public Builder setDuration(long showDur, long missDur) {
            showcaseView.setDuration(showDur, missDur);
            return this;
        }

        /**
         * 触摸任意地方消失
         */
        public Builder setDismissOnTouch(boolean dismiss) {
            showcaseView.setDismissOnTouch(dismiss);
            return this;
        }

        /**
         * 设置透明块的内边距 dp
         */
        public Builder setTargetPadding(int padding) {
            showcaseView.setTargetPadding(padding);
            return this;
        }

        /**
         * 设置点击蒙版消失的View
         */
        public Builder setDismissView(View view) {
            showcaseView.setDismissView(view);
            return this;
        }

        /**
         * 设置透明块的样式
         * <p>
         * 默认 CIRCLE_SHAPE 圆形
         */
        public Builder addTarget(View view) {
            showcaseView.addTarget(view);
            return this;
        }

        /**
         * 设置透明块的样式
         *
         * @param view      view
         * @param shapeMode CIRCLE_SHAPE 圆形 RECTANGLE_SHAPE 矩形 OVAL_SHAPE 椭圆
         */
        public Builder addTarget(View view, int shapeMode) {
            showcaseView.addTarget(view, shapeMode);
            return this;
        }

        /**
         * 增加展示的图片
         *
         * @param resId   resId
         * @param xWeight x坐标-权重（总共10.0f，例如5.0f就在屏幕中间）
         * @param yWeight y坐标-权重（总共10.0f）
         * @param scale   缩放比例
         * @param miss    是否点击蒙版消失
         */
        public Builder addImage(int resId, float xWeight, float yWeight, float scale, boolean miss) {
            showcaseView.addImage(resId, xWeight, yWeight, scale, miss);
            return this;
        }

        /**
         * 增加展示的View
         *
         * @param view    view
         * @param width   view width
         * @param height  view height
         * @param xWeight x坐标-权重（总共10.0f）
         * @param yWeight y坐标-权重（总共10.0f）
         */
        public Builder addShowView(View view, int width, int height, float xWeight, float yWeight) {
            showcaseView.addShowView(view, width, height, xWeight, yWeight);
            return this;
        }

        /**
         * 监听show和dismiss的事件
         */
        public Builder addShowcaseListener(ShowcaseListener listener) {
            showcaseView.addShowcaseListener(listener);
            return this;
        }

        /**
         * 添加到展示队列
         */
        public Builder addShowcaseQueue() {
            showcaseView.addShowQueue();
            String maskColor = showcaseView.mMaskColor;
            boolean dismissOnTouch = showcaseView.mDismissOnTouch;
            int targetPadding = showcaseView.mTargetPadding;
            long showDuration = showcaseView.mShowDuration;
            long missDuration = showcaseView.mMissDuration;
            //重建ShowcaseView，保留set系列的属性
            showcaseView = new ShowcaseView(showcaseView.mActivity);
            showcaseView.mMaskColor = maskColor;
            showcaseView.mDismissOnTouch = dismissOnTouch;
            showcaseView.mTargetPadding = targetPadding;
            showcaseView.mShowDuration = showDuration;
            showcaseView.mMissDuration = missDuration;
            return this;
        }

        public ShowcaseView build() {
            return showcaseView;
        }
    }


    //----------------------------------------------------------------------------------------------


    /**
     * 显示ShowcaseView
     */
    public void show() {
        mAnimationFactory.fadeInView(this, mShowDuration, new IAnimationFactory.AnimationStartListener() {
            @Override
            public void onAnimationStart() {
                mDecorView.post(new Runnable() {
                    @Override
                    public void run() {
                        mDecorView.removeView(ShowcaseView.this);
                        mDecorView.addView(ShowcaseView.this);
                        setVisibility(VISIBLE);
                        if (mListener != null) {
                            mListener.onDisplay(ShowcaseView.this);
                        }
                    }
                });
            }
        });
    }

    /**
     * 将ShowcaseView从Window移除
     */
    public void removeFromWindow() {
        mAnimationFactory.fadeOutView(this, mMissDuration, new IAnimationFactory.AnimationEndListener() {
            @Override
            public void onAnimationEnd() {
                mDecorView.removeView(ShowcaseView.this);
                setVisibility(INVISIBLE);
                if (mListener != null) {
                    mListener.onDismiss(ShowcaseView.this);
                }
                if (mQueueListener != null) {
                    mQueueListener.onDismiss();
                }
                if (mBitmap != null) {
                    mBitmap.recycle();
                    mBitmap = null;
                }
                mTargets = null;
                mCanvas = null;
                mPaint = null;
            }
        });
    }

    /**
     * 添加到显示队列
     */
    public ShowcaseQueue addShowQueue() {
        ShowcaseQueue showcaseQueue = ShowcaseQueue.getInstance();
        showcaseQueue.add(this);
        return showcaseQueue;
    }

    /**
     * 依次展示队列里的showcaseView
     */
    public void showQueue() {
        ShowcaseQueue.getInstance().showQueue();
    }

    /**
     * 设置蒙版颜色
     */
    public void setMaskColor(String color) {
        mMaskColor = color;
    }

    /**
     * 设置渐显时间
     *
     * @param showDur show的渐显时间
     * @param missDur miss的渐隐时间
     */
    public void setDuration(long showDur, long missDur) {
        mShowDuration = showDur;
        mMissDuration = missDur;
    }

    /**
     * 触摸任意地方消失
     */
    public void setDismissOnTouch(boolean dismiss) {
        mDismissOnTouch = dismiss;
    }

    /**
     * 设置透明块的内边距 dp
     */
    public void setTargetPadding(int padding) {
        //dip转换px
        float scale = mActivity.getResources().getDisplayMetrics().density;
        mTargetPadding = (int) (padding * scale + 0.5f);
    }

    /**
     * 设置点击蒙版消失的View
     */
    public void setDismissView(View view) {
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                removeFromWindow();
            }
        });
    }

    /**
     * 设置透明块的样式
     * <p>
     * 默认 CIRCLE_SHAPE 圆形
     */
    public void addTarget(View view) {
        addTarget(view, CIRCLE_SHAPE);
    }

    /**
     * 设置透明块的样式
     *
     * @param view      view
     * @param shapeMode CIRCLE_SHAPE 圆形 RECTANGLE_SHAPE 矩形 OVAL_SHAPE 椭圆
     */
    public void addTarget(View view, int shapeMode) {
        if (view == null) {
            throw new IllegalArgumentException("view == null");
        }
        switch (shapeMode) {
            case CIRCLE_SHAPE:
                mTargets.put(new ViewTarget(view), new CircleShape());
                break;
            case RECTANGLE_SHAPE:
                mTargets.put(new ViewTarget(view), new RectangleShape());
                break;
            case OVAL_SHAPE:
                mTargets.put(new ViewTarget(view), new OvalShape());
                break;
            default:
                mTargets.put(new ViewTarget(view), new CircleShape());
                break;
        }
    }

    /**
     * 增加展示的图片
     *
     * @param resId   resId
     * @param xWeight x坐标-权重（总共10.0f，例如5.0f就在屏幕中间）
     * @param yWeight y坐标-权重（总共10.0f）
     * @param scale   缩放比例
     * @param miss    是否点击蒙版消失
     */
    public void addImage(int resId, float xWeight, float yWeight, float scale, boolean miss) {
        ImageView imageView = new ImageView(mActivity.getApplicationContext());
        imageView.setImageResource(resId);
        if (miss) setDismissView(imageView);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), resId, options);
        options.inJustDecodeBounds = false;
        float width = options.outWidth * scale;
        float height = options.outHeight * scale;
        addShowView(imageView, (int) width, (int) height, xWeight, yWeight);
    }

    /**
     * 增加展示的View
     *
     * @param view    view
     * @param width   view width
     * @param height  view height
     * @param xWeight x坐标-权重（总共10.0f）
     * @param yWeight y坐标-权重（总共10.0f）
     */
    public void addShowView(View view, int width, int height, float xWeight, float yWeight) {
        WindowManager wm = mActivity.getWindowManager();
        int windowWidth = wm.getDefaultDisplay().getWidth();
        int windowHeight = wm.getDefaultDisplay().getHeight();
        float x = (float) windowWidth / 10 * xWeight;
        float y = (float) windowHeight / 10 * yWeight;
        addShowView(view, width, height, (int) x, (int) y);
    }

    /**
     * 增加展示的View（不建议使用，px不利于屏幕适配）
     *
     * @param view   view
     * @param width  view width
     * @param height view height
     * @param x      展示位置的x坐标（px）
     * @param y      展示位置的y坐标（px）
     */
    @Deprecated
    public void addShowView(View view, int width, int height, int x, int y) {
        if (view != null) {
            AbsoluteLayout.LayoutParams layoutParams = new AbsoluteLayout.LayoutParams(
                    width, height, x - width / 2, y - height / 2);
            mContentView.addView(view, layoutParams);
        }
    }

    /**
     * 监听show和dismiss的事件
     */
    public void addShowcaseListener(ShowcaseListener listener) {
        mListener = listener;
    }

    private ShowcaseListener mListener;

    public interface ShowcaseListener {
        void onDisplay(ShowcaseView showcaseView);

        void onDismiss(ShowcaseView showcaseView);
    }

    /**
     * 不建议外部使用，只用于给ShowcaseQueue内部监听onDismiss。
     */
    @Deprecated
    public void addQueueListener(QueueListener queueListener) {
        mQueueListener = queueListener;
    }

    private QueueListener mQueueListener;

    public interface QueueListener {
        void onDismiss();
    }

}
