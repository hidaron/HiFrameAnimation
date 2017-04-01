package org.limlee.hiframeanimationlib;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import okio.BufferedSource;
import okio.Okio;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();
    private static Set<SoftReference<Bitmap>> mReusableBitmaps; //复用bitmap对象池

    private static boolean isReusableBitmap = true;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mReusableBitmaps = Collections.synchronizedSet(new HashSet<SoftReference<Bitmap>>());
        }
    }

    public static Bitmap loadBitmap(String drawableResPath) {
        Bitmap frameBitmap = null;
        BufferedSource bufferedSource = null;
        try {
            final InputStream frameInputStream = HolderApplication.getInstance().getAssets().open(drawableResPath);
            bufferedSource = Okio.buffer(Okio.source(frameInputStream));
            byte[] imageBytes = bufferedSource.readByteArray();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            if(isReusableBitmap) {
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
                options.inJustDecodeBounds = false;
                addInBitmapOptions(options);
            }
            frameBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
            if (isReusableBitmap) {
                reuseBitmap(frameBitmap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != bufferedSource) {
                try {
                    bufferedSource.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return frameBitmap;
    }

    /**
     * 是否要重用该bitmap，如果发现bitmap缓存池里没有可复用的bitmap
     * 或者该bitmap比缓存池所有的bitmap还大，就添加进入缓存池中
     *
     * @param frameBitmap
     */
    private static void reuseBitmap(Bitmap frameBitmap) {
        if (null != frameBitmap
                && frameBitmap.isMutable()
                && !frameBitmap.isRecycled()) {
            if (null != mReusableBitmaps) {
                if (mReusableBitmaps.isEmpty()) {
                    mReusableBitmaps.add(new SoftReference<>(frameBitmap));
                } else {
                    Bitmap maxBitmapItem = null;
                    for (SoftReference<Bitmap> bitmapRef : mReusableBitmaps) {
                        if (null == maxBitmapItem) {
                            maxBitmapItem = bitmapRef.get();
                        } else {
                            final Bitmap bitmapItem = bitmapRef.get();
                            if (null != bitmapItem
                                    && getBitmapByteCount(bitmapItem) > getBitmapByteCount(maxBitmapItem)) {
                                maxBitmapItem = bitmapItem;
                            }
                        }
                    }
                    if (null == maxBitmapItem
                            || getBitmapByteCount(frameBitmap) > getBitmapByteCount(maxBitmapItem)) {
                        mReusableBitmaps.add(new SoftReference<>(frameBitmap));
                    }
                }
            }
        }
    }

    private static int getBitmapByteCount(Bitmap bitmap) {
        if (null == bitmap) return 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return bitmap.getAllocationByteCount();
        }
        return bitmap.getByteCount();
    }

    private static void addInBitmapOptions(BitmapFactory.Options options) {
        options.inMutable = true;
        if (null != mReusableBitmaps
                && !mReusableBitmaps.isEmpty()) {
            Bitmap inBitmap = getInBitmapFormReusableSet(options);
            if (null != inBitmap) {
                options.inBitmap = inBitmap;
            }
        }
    }

    private static Bitmap getInBitmapFormReusableSet(BitmapFactory.Options options) {
        Bitmap inBitmap = null;
        if (null != mReusableBitmaps
                && !mReusableBitmaps.isEmpty()) {
            synchronized (mReusableBitmaps) {
                final Iterator<SoftReference<Bitmap>> iterator = mReusableBitmaps.iterator();
                Bitmap bitmapItem;
                while (iterator.hasNext()) {
                    bitmapItem = iterator.next().get();
                    if (null != bitmapItem
                            && bitmapItem.isMutable()) {
                        if (canUseForInBitmap(bitmapItem, options)) {
                            inBitmap = bitmapItem;
                            iterator.remove();
                            break;
                        }
                    } else {
                        iterator.remove();
                    }
                }
            }
        }
        return inBitmap;
    }

    private static boolean canUseForInBitmap(Bitmap bitmapItem, BitmapFactory.Options options) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //在api19以上，只要可以复用的bitmap比将要解码的大，就可以复用
            final int width = options.outWidth / options.inSampleSize;
            final int height = options.outHeight / options.inSampleSize;
            final int byteCount = width * height * getBytesPerPixel(bitmapItem.getConfig());
            return byteCount <= bitmapItem.getAllocationByteCount();
        }
        return bitmapItem.getWidth() == options.outWidth
                && bitmapItem.getHeight() == options.outHeight
                && options.inSampleSize == 1;
    }

    private static int getBytesPerPixel(Bitmap.Config config) {
        if (config == Bitmap.Config.ARGB_8888) {
            return 4;
        } else if (config == Bitmap.Config.RGB_565) {
            return 2;
        } else if (config == Bitmap.Config.ARGB_4444) {
            return 2;
        } else if (config == Bitmap.Config.ALPHA_8) {
            return 1;
        }
        return 1;
    }

}
