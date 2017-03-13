package org.limlee.hiframeanimationlib;

import android.app.Application;

/**
 * 只是用于方便取到Application对象(Utils中用到)，不用使用这个类
 */
@Deprecated
public class HolderApplication extends Application {

    private static HolderApplication mApplicationInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mApplicationInstance = this;
    }

    public static HolderApplication getInstance() {
        return mApplicationInstance;
    }
}
