package org.limlee.hiframeanimation;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.limlee.hiframeanimationlib.FrameAnimationView;
import org.limlee.hiframeanimationlib.FrameDrawable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String FRAME_NAME = "youting";

    private FrameAnimationView mFrameAnimationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFrameAnimationView = (FrameAnimationView) findViewById(R.id.frame_animation);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        List<String> frameList = null;
        try {
            final String[] frames = getAssets().list(FRAME_NAME);
            if (null != frames) {
                frameList = Arrays.asList(frames);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //按帧图片的序列号排序
        if (null != frameList) {
            Collections.sort(frameList, new Comparator<String>() {

                private final String MATCH_FRAME_NUM = String.format("(?<=%s_).*(?=.png)", FRAME_NAME);
                private final Pattern p = Pattern.compile(MATCH_FRAME_NUM);

                @Override
                public int compare(String lhs, String rhs) {
                    try {
                        final Matcher lhsMatcher = p.matcher(lhs);
                        final Matcher rhsMatcher = p.matcher(rhs);
                        if (lhsMatcher.find()
                                && rhsMatcher.find()) {
                            return Integer.valueOf(lhsMatcher.group()) - Integer.valueOf(rhsMatcher.group());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return 0;
                }
            });
            //添加序列帧
            List<FrameDrawable> frameDrawables = new ArrayList<>();
            for (String framePath : frameList) {
                FrameDrawable frameDrawable = new FrameDrawable(FRAME_NAME + "/" + framePath, 100);
                frameDrawables.add(frameDrawable);
            }
            mFrameAnimationView.setOneShot(false); //循环播放帧动画
            mFrameAnimationView.addFrameDrawable(frameDrawables); //添加序列帧
            mFrameAnimationView.setOnFrameListener(new FrameAnimationView.OnFrameListener() { //添加监听器
                @Override
                public void onFrameStart() {
                    Log.d(TAG, "帧动画播放开始！");
                }

                @Override
                public void onFrameEnd() {
                    Log.d(TAG, "帧动画播放结束！");
                }
            });
            mFrameAnimationView.start(); //开始播放
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFrameAnimationView.stop(); //停止播放
        mFrameAnimationView.setOnFrameListener(null); //移除监听器
    }
}
