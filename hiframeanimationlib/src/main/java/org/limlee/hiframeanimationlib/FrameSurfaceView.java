package org.limlee.hiframeanimationlib;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

abstract class FrameSurfaceView extends SurfaceView {
    private static final String TAG = FrameSurfaceView.class.getSimpleName();
    private static final int MIN_UPDATE_RATE = 16;
    private static RectF RECT = new RectF();
    private static Paint PAINT = new Paint();

    private int mFrameUpdateRate = MIN_UPDATE_RATE;
    private volatile boolean mIsSurfaceCreated;

    private UpdateThread mUpdateThread;

    private boolean mIsUpdateStarted;

    private volatile int mSurfaceWidth;
    private volatile int mSurfaceHeight;

    static {
        PAINT.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        PAINT.setColor(Color.TRANSPARENT);
    }

    /**
     * 如何绘制交给它的子类去实现
     *
     * @param canvas
     */
    protected abstract void drawFrame(Canvas canvas);

    public FrameSurfaceView(Context context) {
        this(context, null);
    }

    public FrameSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        if (!isInEditMode()) {
            setZOrderMediaOverlay(true);
            setZOrderOnTop(true);
        }
        setWillNotCacheDrawing(true);
        setDrawingCacheEnabled(false);
        setWillNotDraw(true);
        getHolder().setFormat(PixelFormat.TRANSPARENT);//透明背景
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mIsSurfaceCreated = true;
                clearSurface();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mIsSurfaceCreated = true;
                mSurfaceWidth = width;
                mSurfaceHeight = height;
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mIsSurfaceCreated = false;
            }
        });
    }

    public synchronized void start() {
        startUpdate();
    }

    public synchronized void stop() {
        stopUpdate();
    }

    /**
     * 是否当前绘制线程在跑
     *
     * @return
     */
    public boolean isRunning() {
        return mIsUpdateStarted;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopUpdate();
    }

    final protected void clearSurface() {
        if (mIsSurfaceCreated) {
            Canvas canvas = getHolder().lockCanvas();
            if (null != canvas) {
                clearCanvas(canvas);
                if (mIsSurfaceCreated) {
                    getHolder().unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    final protected void clearCanvas(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT,
                PorterDuff.Mode.CLEAR);
        RECT.set(0, 0, canvas.getWidth(), canvas.getHeight());
        canvas.drawRect(RECT, PAINT);
    }

    final protected long drawSurface() {
        if (!mIsSurfaceCreated) {
            return 0;
        }
        if (mSurfaceWidth == 0
                || mSurfaceHeight == 0) {
            return 0;
        }
        if (!isShown()) {
            clearSurface();
            return 0;
        }
        final long startTime = SystemClock.uptimeMillis();
        if (mIsSurfaceCreated) {
            Canvas canvas = getHolder().lockCanvas();
            if (null != canvas) {
                drawFrame(canvas);
                if (mIsSurfaceCreated) {
                    getHolder().unlockCanvasAndPost(canvas);
                }
            }
        }
        return SystemClock.uptimeMillis() - startTime;
    }

    protected void stopUpdate() {
        mIsUpdateStarted = false;
        if (null != mUpdateThread) {
            UpdateThread updateThread = mUpdateThread;
            mUpdateThread = null;
            updateThread.quit();
            try {
                updateThread.join(6);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            updateThread.interrupt();
        }
    }

    protected void startUpdate() {
        if (mIsUpdateStarted) return;
        mUpdateThread = new UpdateThread("Animator Update Thread") {

            @Override
            public void run() {
                try {
                    while (!isQuited()
                            && !Thread.currentThread().isInterrupted()) {
                        long drawTime = drawSurface();
                        long diffTime = mFrameUpdateRate - drawTime;
                        if (isQuited()) {
                            break;
                        }
                        if (diffTime > 0) {
                            SystemClock.sleep(diffTime);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        mIsUpdateStarted = true;
        mUpdateThread.start();
    }
}
