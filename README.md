# HiFrameAnimation
低内存消耗的序列帧库，只占用了一张序列帧图片的内存，异步绘制不占用UI线程资源，同时复用序列帧Bitmap，减少内存抖动，避免了频繁的GC，提高动画的流畅性。可用于直播大礼物的展示。

![image](https://github.com/hidaron/HiFrameAnimation/blob/master/demo.gif) 

##使用了inBitmap复用内存
为了节省内存，我在每一帧绘制前才从本地读取并且编码图片，绘制完成后就释放，但这样的话会频繁地进行IO操作，造成内存抖动。为了减少内存抖动，我使用了inBitmap来复用当前帧所占用内存，效果还是很不错，内存抖动明显减缓了。

没有使用inBitmap</br>
![image](https://github.com/hidaron/HiFrameAnimation/blob/master/inbitmap_before.png)

使用inBitmap</br>
![image](https://github.com/hidaron/HiFrameAnimation/blob/master/inbitmap_after.png)

##怎么使用
### 1.添加帧动画视图布局

````
 <org.limlee.hiframeanimationlib.FrameAnimationView
        android:id="@+id/frame_animation"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

````

### 2.添加序列帧资源

````
 List<FrameDrawable> frameDrawables = new ArrayList<>();
 for (String framePath : frameList) {
 	FrameDrawable frameDrawable = new FrameDrawable(FRAME_NAME + "/" + framePath, 100);
 	frameDrawables.add(frameDrawable);
 }
 mFrameAnimationView.addFrameDrawable(frameDrawables);

````

### 3.播放帧动画

````
 @Override
 protected void onPostCreate(@Nullable Bundle savedInstanceState) {
 	....
 	mFrameAnimationView.setOneShot(false); //循环播放帧动画
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

````

### 4.停止播放

````
 @Override
 protected void onDestroy() {
 	super.onDestroy();
 	mFrameAnimationView.stop(); //停止播放
 	mFrameAnimationView.setOnFrameListener(null); //移除监听器
 }

````


