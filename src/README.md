# android高仿微信表情输入与键盘输入详解
&emsp;&emsp;最近公司在项目上要使用到表情与键盘的切换输入，自己实现了一个，还是存在些缺陷，比如说键盘与表情切换时出现跳闪问题，这个相当困扰我，不过所幸在Github（其中一个不错的[开源项目](https://github.com/dss886/Android-EmotionInputDetector)，其代码整体结构很不错）并且在论坛上找些解决方案，再加上我也是研究了好多个开源项目的代码，最后才苦逼地整合出比较不错的实现效果，可以说跟微信基本一样（嘿嘿，只能说目前还没发现大Bug，若发现大家一起日后慢慢完善，这里我也只是给出了实现方案，拓展其他表情我并没有实现哈，不过代码中我实现了一个可拓展的fragment模板以便大家实现自己的表情包），我只是实现了一页表情，代码我也进行另外的封装与拓展，大家需要多表情的话只需要实现自己的表情fragment界面，然后根据工厂类获取即可，废话不多说先上图看效果:
![](http://img.blog.csdn.net/20160119154602745?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQv/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)

![](http://img.blog.csdn.net/20160119155153188?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQv/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)
效果还不错吧，哈哈。下面开始介绍：
本篇主要分析的核心类EmotionKeyboard.java,EmotionComplateFragment.java,EmotionMainFragment.java,FragmentFactory.java,还有一个是工具类里的EmotionUtils.java和GlobalOnItemClickManagerUtils.java 这几个类我会重点分析一下，其他的大家自行看源码哈。下面就开始咯，先来看看本篇主要内容以及大概思路：
![](http://img.blog.csdn.net/20160119155717336?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQv/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/Center)
##<font color='#F55F5C'>1.解决表情与键盘切换跳闪问题</font>
####<font color='#F55F5C'> 1.1跳闪问题概述</font>
&emsp;&emsp;为了让大家对这个问题有一定了解，我先来个简单案例，用红色面板代表表情面板，效果如下：
![](http://img.blog.csdn.net/20160119155811203?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQv/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)
&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;<font color='#27844F'>**图（1-1）**</font>
&emsp;&emsp;我们先来看图（1-1），即上图，通过上图我们可以看出，当表情显示时，我们点击表情按钮，隐藏表情显示软件盘时，内容Bar有一个明显的先向下后恢复的跳闪现象，这样用户体验相当的差，我们希望的是下图（1-2）的效果，无论怎么切换都不会有跳闪现象，这就是我所有说的键盘与表情切换的跳闪问题。
![](http://img.blog.csdn.net/20160119155824947?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQv/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)
&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;<font color='#27844F'>**图（1-2）**</font>
到这里，我们对这个问题有了大概了解后，再来深入分析如何实现图（1-2）的不跳闪效果。这里我们做个约定，我们把含有表情那个bar统称为内容Bar。
#### <font color='#F55F5C'> 1.2 解决跳闪问题的思路：</font>
 &emsp;&emsp;android系统在弹出软键盘时，会把我们的内容 Bar 顶上去，因此只有表情面板的高度与软键盘弹出时高度一致时，才有可能然切换时高度过渡更自然，所以我们必须计算出软键盘的高度并设置给表情面板。仅仅有这一步跳闪问题还是依旧存在，因此这时我们必须想其他办法固定内容Bar,因为所有的跳闪都是表情面板隐藏，而软键盘往上托出瞬间，Activity高度变高(为什么会变高后面会说明)，内容Bar往下滑后，又被软键盘顶回原来位置造成的。因此只要固定了内容Bar的位置，闪跳问题就迎刃而解了。那么如何固定内容Bar的位置呢？**我们知道在一个布局中一个控件的位置其实是由它上面所有控件的高度决定的，如果其上面其他控件的高度不变，那么当前控件的高度自然也不会变化，即使到时Activity的高度发生了变化也也不会影响该控件的位置**(整个界面的显示是挂载在window窗体上的，而非Activity，不了解的可以先研究一下窗体的创建过程)，因此我们只要在软键盘弹出前固定内容Bar上面所有控件高度，从而达到固定内容Bar位置(高度)的目的。好了，有思路了，我们接下来一步步按上面思路解决问题。
#### <font color='#F55F5C'> 1.3 解决跳闪问题的套路：</font>
* <font color='#F55F5C'> 1.3.1 先获取键盘高度，并设置表情面板的高度为软键盘的高度</font>

&emsp;&emsp;Android系统在界面上弹出软键盘时会将整个Activity的高度压缩，此时windowSoftInputMode属性设置为adjustResize(对windowSoftInputMode不清楚的话，请自行查阅相关资料哈),这个属性表示Activity的主窗口总是会被调整大小，从而保证软键盘显示空间。在这种情况下我们可以通过以下方法计算软键盘的高度：

```java
Rect r = new Rect();
/*
* decorView是window中的最顶层view，可以从window中通过getDecorView获取到decorView。
* 通过decorView获取到程序显示的区域，包括标题栏，但不包括状态栏。
*/
mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
//获取屏幕的高度
int screenHeight = mActivity.getWindow().getDecorView().getRootView().getHeight();
//计算软件盘的高度
int softInputHeight = screenHeight - r.bottom;
```
这里我们队对r.bottom和mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(r)进行简单解释，直接上图吧：
![](	http://img.blog.csdn.net/20160805000812561)
这下就清晰了吧，右边是Rect参数解析图，辅助我们对rect的理解。

```java
Rect r = new Rect();
mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(r)
```
&emsp;&emsp;这两句其实将左图中蓝色边框( 其实也就是actvity的大小)的size大小参数封装到Rect中，以便我们后续使用。虽然计算出来的区域大小不包含状态栏，但是r.bottom（红色箭头长度）的大小是从屏幕顶部开始计算的所以包含了状态栏的高度。需要注意的是，区域大小是这样计算出来的：
<font size=5 color='#27844F'>`区域的高：r.bottom-r.top
区域的宽：r.right-r.left `</font>
当然这个跟计算软键盘高度没关系，只是顺带提一下。因此我们可以通过即可获取到软以下方式获取键盘高度：
<font size=5 color='#27844F'>`键盘高度=屏幕高度－r.bottom`</font>

* <font color='#F55F5C'> 1.3.2 固定内容Bar的高度，解决闪跳问题</font>

&emsp;&emsp;软键盘高度解决后，现在剩下的问题关键就在于控制内容Bar的高度了，那么如何做呢？我们先来看一个布局文件

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical" android:layout_width="match_parent"
    android:layout_height="match_parent">
    <ListView
        android:id="@+id/listview"
        android:layout_weight="1"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        />
    <FrameLayout
        android:id="@+id/fl_emotionview_main"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
</LinearLayout>
```
&emsp;&emsp;其中ListView的layout_height为0dp、layout_weight为1，这样这个ListView就会自动充满整个布局，这里ListView可以替换成任意控件,FrameLayout则为表情布局（也可认为就是我们前面所说的内容Bar,只不过这里最终会被替换成整个表情布局），我们的目的就是在弹出软键盘时固定FrameLayout的高度，以便去除跳闪问题。根据我们前面的思路，FrameLayout的高度是由其上面的控件决定的也就是由ListView决定的，也就是说我们只要在软键盘弹出前固定ListView的内容高度即可。因此我们可以通过下面的方法来锁定ListView的高度，（mContentView就是我们所指的ListView,这些方法都封装在EmotionKeyboard.java类中）

```java
/**
  * 锁定内容高度，防止跳闪
  */
 private void lockContentHeight(){
    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mContentView.getLayoutParams();
    params.height = mContentView.getHeight();
    params.weight = 0.0F;
  }
```
&emsp;&emsp;将weight置0，然后将height设置为当前的height，在父控件（LinearLayout）的高度变化时它的高度也不再会变化。释放ListView的高度：

```java
private void unlockContentHeightDelayed() {
    mEditText.postDelayed(new Runnable() {
        @Override
        public void run() {
            ((LinearLayout.LayoutParams) mContentView.getLayoutParams()).weight = 1.0F;
        }
    }, 200L);
}
```
&emsp;&emsp;其中的LinearLayout.LayoutParams.weight = 1.0F;，在代码里动态更改LayoutParam的weight，会导致父控件重新onLayout()，也就达到改变控件的高度的目的。到此两个主要问题都解决了，我们直接上核心类代码，该类[来自github上的开源项目](https://github.com/dss886/Android-EmotionInputDetector  )我在使用中直接从该项目中抽取了该类, 并做了细微修改，也添加了代码注释。

```java
package com.zejian.emotionkeyboard.emotionkeyboardview;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.zejian.emotionkeyboard.utils.LogUtils;
/**
 * author : zejian
 * time : 2016年1月5日 上午11:14:27
 * email : shinezejian@163.com
 * description :源码来自开源项目https://github.com/dss886/Android-EmotionInputDetector
 * 				本人仅做细微修改以及代码解析
 */
public class EmotionKeyboard {

    	private static final String SHARE_PREFERENCE_NAME = "EmotionKeyboard";
        private static final String SHARE_PREFERENCE_SOFT_INPUT_HEIGHT = "soft_input_height";
	    private Activity mActivity;
	    private InputMethodManager mInputManager;//软键盘管理类
	    private SharedPreferences sp;
	    private View mEmotionLayout;//表情布局
	    private EditText mEditText;//
	    private View mContentView;//内容布局view,即除了表情布局或者软键盘布局以外的布局，用于固定bar的高度，防止跳闪
	    private EmotionKeyboard(){
	    }

	    /**
	     * 外部静态调用
	     * @param activity
	     * @return
	     */
	    public static EmotionKeyboard with(Activity activity) {
	    	EmotionKeyboard emotionInputDetector = new EmotionKeyboard();
	        emotionInputDetector.mActivity = activity;
	        emotionInputDetector.mInputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
	        emotionInputDetector.sp = activity.getSharedPreferences(SHARE_PREFERENCE_NAME, Context.MODE_PRIVATE);
	        return emotionInputDetector;
	    }

	    /**
	     * 绑定内容view，此view用于固定bar的高度，防止跳闪
	     * @param contentView
	     * @return
	     */
	    public EmotionKeyboard bindToContent(View contentView) {
	        mContentView = contentView;
	        return this;
	    }

	    /**
	     * 绑定编辑框
	     * @param editText
	     * @return
	     */
	    public EmotionKeyboard bindToEditText(EditText editText) {
	        mEditText = editText;
	        mEditText.requestFocus();
	        mEditText.setOnTouchListener(new View.OnTouchListener() {
	            @Override
	            public boolean onTouch(View v, MotionEvent event) {
	                if (event.getAction() == MotionEvent.ACTION_UP && mEmotionLayout.isShown()) {
	                    lockContentHeight();//显示软件盘时，锁定内容高度，防止跳闪。
	                    hideEmotionLayout(true);//隐藏表情布局，显示软件盘
	                    //软件盘显示后，释放内容高度
	                    mEditText.postDelayed(new Runnable() {
	                        @Override
	                        public void run() {
	                            unlockContentHeightDelayed();
	                        }
	                    }, 200L);
	                }
	                return false;
	            }
	        });
	        return this;
	    }

	    /**
	     * 绑定表情按钮
	     * @param emotionButton
	     * @return
	     */
	    public EmotionKeyboard bindToEmotionButton(View emotionButton) {
	        emotionButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mEmotionLayout.isShown()) {
						lockContentHeight();//显示软件盘时，锁定内容高度，防止跳闪。
						hideEmotionLayout(true);//隐藏表情布局，显示软件盘
						unlockContentHeightDelayed();//软件盘显示后，释放内容高度
					} else {
						if (isSoftInputShown()) {//同上
							lockContentHeight();
							showEmotionLayout();
							unlockContentHeightDelayed();
						} else {
							showEmotionLayout();//两者都没显示，直接显示表情布局
						}
					}
				}
			});
	        return this;
	    }
	    /**
	     * 设置表情内容布局
	     * @param emotionView
	     * @return
	     */
	    public EmotionKeyboard setEmotionView(View emotionView) {
	        mEmotionLayout = emotionView;
	        return this;
	    }
	    public EmotionKeyboard build(){
//设置软件盘的模式：SOFT_INPUT_ADJUST_RESIZE  这个属性表示Activity的主窗口总是会被调整大小，从而保证软键盘显示空间。
	   //从而方便我们计算软件盘的高度
	   mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN |
	                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
	        //隐藏软件盘
	        hideSoftInput();
	        return this;
	    }
	    /**
	     * 点击返回键时先隐藏表情布局
	     * @return
	     */
	    public boolean interceptBackPress() {
	        if (mEmotionLayout.isShown()) {
	            hideEmotionLayout(false);
	            return true;
	        }
	        return false;
	    }
	    private void showEmotionLayout() {
	        int softInputHeight = getSupportSoftInputHeight();
	        if (softInputHeight == 0) {
	            softInputHeight = sp.getInt(SHARE_PREFERENCE_SOFT_INPUT_HEIGHT, 400);
	        }
	        hideSoftInput();
	        mEmotionLayout.getLayoutParams().height = softInputHeight;
	        mEmotionLayout.setVisibility(View.VISIBLE);
	    }
	    /**
	     * 隐藏表情布局
	     * @param showSoftInput 是否显示软件盘
	     */
	    private void hideEmotionLayout(boolean showSoftInput) {
	        if (mEmotionLayout.isShown()) {
	            mEmotionLayout.setVisibility(View.GONE);
	            if (showSoftInput) {
	                showSoftInput();
	            }
	        }
	    }
	    /**
	     * 锁定内容高度，防止跳闪
	     */
	    private void lockContentHeight() {
	        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mContentView.getLayoutParams();
	        params.height = mContentView.getHeight();
	        params.weight = 0.0F;
	    }
	    /**
	     * 释放被锁定的内容高度
	     */
	    private void unlockContentHeightDelayed() {
	        mEditText.postDelayed(new Runnable() {
	            @Override
	            public void run() {
	                ((LinearLayout.LayoutParams) mContentView.getLayoutParams()).weight = 1.0F;
	            }
	        }, 200L);
	    }
	    /**
	     * 编辑框获取焦点，并显示软件盘
	     */
	    private void showSoftInput() {
	        mEditText.requestFocus();
	        mEditText.post(new Runnable() {
				@Override
				public void run() {
					mInputManager.showSoftInput(mEditText, 0);
				}
			});
	    }
	    /**
	     * 隐藏软件盘
	     */
	    private void hideSoftInput() {
	        mInputManager.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
	    }
	    /**
	     * 是否显示软件盘
	     * @return
	     */
	    private boolean isSoftInputShown() {
	        return getSupportSoftInputHeight() != 0;
	    }
	    /**
	     * 获取软件盘的高度
	     * @return
	     */
	    private int getSupportSoftInputHeight() {
	        Rect r = new Rect();
	        /**
	         * decorView是window中的最顶层view，可以从window中通过getDecorView获取到decorView。
	         * 通过decorView获取到程序显示的区域，包括标题栏，但不包括状态栏。
	         */
	        mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
	        //获取屏幕的高度
	        int screenHeight = mActivity.getWindow().getDecorView().getRootView().getHeight();
	        //计算软件盘的高度
	        int softInputHeight = screenHeight - r.bottom;
	        /**
	         * 某些Android版本下，没有显示软键盘时减出来的高度总是144，而不是零，
	         * 这是因为高度是包括了虚拟按键栏的(例如华为系列)，所以在API Level高于20时，
	         * 我们需要减去底部虚拟按键栏的高度（如果有的话）
	         */
	        if (Build.VERSION.SDK_INT >= 20) {
	            // When SDK Level >= 20 (Android L), the softInputHeight will contain the height of softButtonsBar (if has)
	            softInputHeight = softInputHeight - getSoftButtonsBarHeight();
	        }
	        if (softInputHeight < 0) {
	            LogUtils.w("EmotionKeyboard--Warning: value of softInputHeight is below zero!");
	        }
	        //存一份到本地
	        if (softInputHeight > 0) {
	            sp.edit().putInt(SHARE_PREFERENCE_SOFT_INPUT_HEIGHT, softInputHeight).apply();
	        }
	        return softInputHeight;
	    }
	    /**
	     * 底部虚拟按键栏的高度
	     * @return
	     */
	    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	    private int getSoftButtonsBarHeight() {
	        DisplayMetrics metrics = new DisplayMetrics();
	        //这个方法获取可能不是真实屏幕的高度
	        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
	        int usableHeight = metrics.heightPixels;
	        //获取当前屏幕的真实高度
	        mActivity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
	        int realHeight = metrics.heightPixels;
	        if (realHeight > usableHeight) {
	            return realHeight - usableHeight;
	        } else {
	            return 0;
	        }
	    }
	/**
	 * 获取软键盘高度
	 * @return
	 */
	public int getKeyBoardHeight(){
		return sp.getInt(SHARE_PREFERENCE_SOFT_INPUT_HEIGHT, 400);
	}
}
```
&emsp;&emsp;EmotionKeyboard类使用的是设计模式中的builder模式来创建对象。其中mEmotionLayout是表情布局，mContentView是内容布局view,即除了表情布局或者软键盘布局以外的布局，用于固定bar的高度，防止跳闪，当然mContentView可以是任意布局。

```java
/**
    * 绑定表情按钮
    * @param emotionButton
    * @return
    */
    public EmotionKeyboard bindToEmotionButton(View emotionButton) {
        emotionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEmotionLayout.isShown()) {
                    lockContentHeight();//显示软件盘时，锁定内容高度，防止跳闪。
                    hideEmotionLayout(true);//隐藏表情布局，显示软件盘
                    unlockContentHeightDelayed();//软件盘显示后，释放内容高度
                } else {
                    if (isSoftInputShown()) {//同上
                        lockContentHeight();
                        showEmotionLayout();
                        unlockContentHeightDelayed();
                    } else {
                        showEmotionLayout();//两者都没显示，直接显示表情布局
                    }
                }
            }
        });
        return this;
    }
```
&emsp;&emsp;这里我们主要重点说明一下点击表情按钮时，显示或者隐藏表情布局以及软键盘的逻辑。首先我们通过<font size=4>`mEmotionLayout.isShown()`</font>去判断表情是否已经显示，如果返回true,这时肯定要去切换成软键盘，因此必须先通过<font size=4>`lockContentHeight()`</font>方法锁定mContentView内容高度，然后通过<font size=4>`hideEmotionLayout(true)`</font>方法因此表情布局并显示软键盘，这里传入true表示显示软键盘，如果传入false则表示不显示软键盘，软键盘显示后通过<font size=4>`unlockContentHeightDelayed()`</font>方法去解锁mContentView内容高度。但如果<font size=4>`mEmotionLayout.isShown()`</font>返回了false，这有两种情况，第1种是如果此时软键盘已经显示，则需先锁定mContentView内容高度，再去隐藏软键盘，然后显示表情布局，最后再解锁mContentView内容高度。第2种情况是软键盘和表情都没显示，这下就简单了，直接显示表情布局即可。好，这个类解析到这，其他直接看源码哈，注释杠杠的哈。最后我们来看看在外部使用该类的例子代码如下：

```java
mEmotionKeyboard = EmotionKeyboard.with(getActivity())
                .setEmotionView(rootView.findViewById(R.id.ll_emotion_layout))//绑定表情面板
                .bindToContent(contentView)//绑定内容view
                .bindToEditText(!isBindToBarEditText ? ((EditText) contentView) : ((EditText) rootView.findViewById(R.id.bar_edit_text)))//判断绑定那种EditView
                .bindToEmotionButton(rootView.findViewById(R.id.emotion_button))//绑定表情按钮
                .build();
```

##<font color='#F55F5C'>2.实现表情表情面板切换的思路</font>
&emsp;&emsp;这里我们主要采用NoHorizontalScrollerViewPager+RecyclerView+Fragment实现，思路是这样的，我们以NoHorizontalScrollerViewPager作为载体，fragment作为展示界面，RecyclerView作为底部滚动条，每当点击RecyclerView的item时，我们使用<font size=4>`viewPager.setCurrentItem(position,false)`</font>方法来切换fragment界面即可(这里传入false是表示不需要viewPager的切换动画)。这样我们就可以实现不同类表情的切换了。（提示一下这里所指的fragment其实是就工程目录中的EmotiomComplateFragment.java类）这个比较简单，就不多啰嗦了。实现代码稍后会一起提供。下面是不可横向滑动的ViewPager的实现代码，非常简单，不拦截子类事件即可。

```java
package com.zejian.emotionkeyboard.emotionkeyboardview;
import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
/**
 * Created by zejian
 * Time  16/1/7 上午11:12
 * Email shinezejian@163.com
 * Description:不可横向滑动的ViewPager
 */
public class NoHorizontalScrollerViewPager extends ViewPager{
    public NoHorizontalScrollerViewPager(Context context) {
        super(context);
    }
    public NoHorizontalScrollerViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    /**
     * 重写拦截事件，返回值设置为false，这时便不会横向滑动了。
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }
    /**
     * 重写拦截事件，返回值设置为false，这时便不会横向滑动了。
     * @param ev
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }
}
```
##<font color='#F55F5C'>3.单个表情面板的实现思路</font>
#### <font color='#F55F5C'> 3.1 表情图片的本质与显示</font>
&emsp;&emsp;表情的显示从直观上看确实是一个图片，但实际只是一种特殊的文本（ImageSpan），比如微博里表情就是"[表情名字]"的接口,可爱的表情就是[可爱]...因此这里我们也打算利用"[表情名字]"作为key，图片的R值作为内容进行存取，EmotionUtils类如下

```java
package com.zejian.emotionkeyboard.utils;
import android.support.v4.util.ArrayMap;
import com.zejian.emotionkeyboard.R;
/**
 * @author : zejian
 * @time : 2016年1月5日 上午11:32:33
 * @email : shinezejian@163.com
 * @description :表情加载类,可自己添加多种表情，分别建立不同的map存放和不同的标志符即可
 */
public class EmotionUtils {
	/**
	 * 表情类型标志符
	 */
	public static final int EMOTION_CLASSIC_TYPE=0x0001;//经典表情
	/**
	 * key-表情文字;
	 * value-表情图片资源
	 */
	public static ArrayMap<String, Integer> EMPTY_MAP;
	public static ArrayMap<String, Integer> EMOTION_CLASSIC_MAP;
	static {
		EMPTY_MAP = new ArrayMap<>();
		EMOTION_CLASSIC_MAP = new ArrayMap<>();
		EMOTION_CLASSIC_MAP.put("[呵呵]", R.drawable.d_hehe);
		EMOTION_CLASSIC_MAP.put("[嘻嘻]", R.drawable.d_xixi);
		EMOTION_CLASSIC_MAP.put("[哈哈]", R.drawable.d_haha);
		EMOTION_CLASSIC_MAP.put("[爱你]", R.drawable.d_aini);
		EMOTION_CLASSIC_MAP.put("[挖鼻屎]", R.drawable.d_wabishi);
		EMOTION_CLASSIC_MAP.put("[吃惊]", R.drawable.d_chijing);
		EMOTION_CLASSIC_MAP.put("[晕]", R.drawable.d_yun);
		EMOTION_CLASSIC_MAP.put("[泪]", R.drawable.d_lei);
		EMOTION_CLASSIC_MAP.put("[馋嘴]", R.drawable.d_chanzui);
		EMOTION_CLASSIC_MAP.put("[抓狂]", R.drawable.d_zhuakuang);
		EMOTION_CLASSIC_MAP.put("[哼]", R.drawable.d_heng);
		EMOTION_CLASSIC_MAP.put("[可爱]", R.drawable.d_keai);
		EMOTION_CLASSIC_MAP.put("[怒]", R.drawable.d_nu);
		EMOTION_CLASSIC_MAP.put("[汗]", R.drawable.d_han);
		EMOTION_CLASSIC_MAP.put("[害羞]", R.drawable.d_haixiu);
		EMOTION_CLASSIC_MAP.put("[睡觉]", R.drawable.d_shuijiao);
		EMOTION_CLASSIC_MAP.put("[钱]", R.drawable.d_qian);
		EMOTION_CLASSIC_MAP.put("[偷笑]", R.drawable.d_touxiao);
		EMOTION_CLASSIC_MAP.put("[笑cry]", R.drawable.d_xiaoku);
		EMOTION_CLASSIC_MAP.put("[doge]", R.drawable.d_doge);
		EMOTION_CLASSIC_MAP.put("[喵喵]", R.drawable.d_miao);
		EMOTION_CLASSIC_MAP.put("[酷]", R.drawable.d_ku);
		EMOTION_CLASSIC_MAP.put("[衰]", R.drawable.d_shuai);
		EMOTION_CLASSIC_MAP.put("[闭嘴]", R.drawable.d_bizui);
		EMOTION_CLASSIC_MAP.put("[鄙视]", R.drawable.d_bishi);
		EMOTION_CLASSIC_MAP.put("[花心]", R.drawable.d_huaxin);
		EMOTION_CLASSIC_MAP.put("[鼓掌]", R.drawable.d_guzhang);
		EMOTION_CLASSIC_MAP.put("[悲伤]", R.drawable.d_beishang);
		EMOTION_CLASSIC_MAP.put("[思考]", R.drawable.d_sikao);
		EMOTION_CLASSIC_MAP.put("[生病]", R.drawable.d_shengbing);
		EMOTION_CLASSIC_MAP.put("[亲亲]", R.drawable.d_qinqin);
		EMOTION_CLASSIC_MAP.put("[怒骂]", R.drawable.d_numa);
		EMOTION_CLASSIC_MAP.put("[太开心]", R.drawable.d_taikaixin);
		EMOTION_CLASSIC_MAP.put("[懒得理你]", R.drawable.d_landelini);
		EMOTION_CLASSIC_MAP.put("[右哼哼]", R.drawable.d_youhengheng);
		EMOTION_CLASSIC_MAP.put("[左哼哼]", R.drawable.d_zuohengheng);
		EMOTION_CLASSIC_MAP.put("[嘘]", R.drawable.d_xu);
		EMOTION_CLASSIC_MAP.put("[委屈]", R.drawable.d_weiqu);
		EMOTION_CLASSIC_MAP.put("[吐]", R.drawable.d_tu);
		EMOTION_CLASSIC_MAP.put("[可怜]", R.drawable.d_kelian);
		EMOTION_CLASSIC_MAP.put("[打哈气]", R.drawable.d_dahaqi);
		EMOTION_CLASSIC_MAP.put("[挤眼]", R.drawable.d_jiyan);
		EMOTION_CLASSIC_MAP.put("[失望]", R.drawable.d_shiwang);
		EMOTION_CLASSIC_MAP.put("[顶]", R.drawable.d_ding);
		EMOTION_CLASSIC_MAP.put("[疑问]", R.drawable.d_yiwen);
		EMOTION_CLASSIC_MAP.put("[困]", R.drawable.d_kun);
		EMOTION_CLASSIC_MAP.put("[感冒]", R.drawable.d_ganmao);
		EMOTION_CLASSIC_MAP.put("[拜拜]", R.drawable.d_baibai);
		EMOTION_CLASSIC_MAP.put("[黑线]", R.drawable.d_heixian);
		EMOTION_CLASSIC_MAP.put("[阴险]", R.drawable.d_yinxian);
		EMOTION_CLASSIC_MAP.put("[打脸]", R.drawable.d_dalian);
		EMOTION_CLASSIC_MAP.put("[傻眼]", R.drawable.d_shayan);
		EMOTION_CLASSIC_MAP.put("[猪头]", R.drawable.d_zhutou);
		EMOTION_CLASSIC_MAP.put("[熊猫]", R.drawable.d_xiongmao);
		EMOTION_CLASSIC_MAP.put("[兔子]", R.drawable.d_tuzi);
	}
	/**
	 * 根据名称获取当前表情图标R值
	 * @param EmotionType 表情类型标志符
	 * @param imgName 名称
	 * @return
	 */
	public static int getImgByName(int EmotionType,String imgName) {
		Integer integer=null;
		switch (EmotionType){
			case EMOTION_CLASSIC_TYPE:
				integer = EMOTION_CLASSIC_MAP.get(imgName);
				break;
			default:
				LogUtils.e("the emojiMap is null!!");
				break;
		}
		return integer == null ? -1 : integer;
	}
	/**
	 * 根据类型获取表情数据
	 * @param EmotionType
	 * @return
	 */
	public static ArrayMap<String, Integer> getEmojiMap(int EmotionType){
		ArrayMap EmojiMap=null;
		switch (EmotionType){
			case EMOTION_CLASSIC_TYPE:
				EmojiMap=EMOTION_CLASSIC_MAP;
				break;
			default:
				EmojiMap=EMPTY_MAP;
				break;
		}
		return EmojiMap;
	}
}
```
&emsp;&emsp;ArrayMap<String, Integer> EMPTY_MAP是一个空集合这个主要是防止空指针情况的，而ArrayMap<String, Integer> EMOTION_CLASSIC_MAP则是我们前所显示经典表情集合，这个集合有一个标志(public static final int EMOTION_CLASSIC_TYPE=0x0001)这个标志是在获取表情集合和表情图片的唯一标识，也就是说我们需要从EMOTION_CLASSIC_MAP集合中获取表情时必须通过标志符辨别，我们往下看可以看到getEmojiMap(int EmotionType)，这个方法就是根据EmotionType这个标志类型来获取我们表情集合的，同时这也说明了，如果我们自己需要添加别的表情类型，这时就需要在这个类中创建一个新的集合，同时还必须创建一个新的标志类型来区别这个集合。可能我们还是不是很理解这点，这里贴一下工厂类的获取方法我们或许就明白了

```java
/**
* 获取fragment的方法
* @param emotionType 表情类型，用于判断使用哪个map集合的表情
*/
public Fragment getFragment(int emotionType){
   Bundle bundle = new Bundle();
   bundle.putInt(FragmentFactory.EMOTION_MAP_TYPE,emotionType);
   EmotiomComplateFragment fragment= EmotiomComplateFragment.newInstance(EmotiomComplateFragment.class,bundle);
   return fragment;
}
```
调用时，如下：

```java
//创建fragment的工厂类
 FragmentFactory factory=FragmentFactory.getSingleFactoryInstance();
 //创建修改实例
 EmotiomComplateFragment f1= (EmotiomComplateFragment) factory.getFragment(EmotionUtils.EMOTION_CLASSIC_TYPE);
```
 &emsp;&emsp;这里我们通过工厂类getFragment(int emotionType)方法的创建出模版表情类EmotiomComplateFragment，为什么说是模版呢，因为只要我们创建时传递集合标志不同，例如经典表情传递的就是EmotionUtils.EMOTION_CLASSIC_TYPE，这时EmotiomComplateFragment类内部就会根据传递的集合类型去EmotionUtils类中获取相对应的集合，这样也就会创建出我们所需要的表情面板。这里小结一下：通过上术分析我们可以知道如果我们要添加自己的其他类型表情，只需以下步骤：

* 步骤1.在EmotionUtils类创建一个表情集合，并赋予这个集合唯一标志
* 步骤2.在EmotionUtils类中的两个获取方法中完善相应的代码。
* 步骤3.在创建新的EmotiomComplateFragment模板类时，传递相应的集合标志符即可创建相应的表情面板。

 &emsp;&emsp;接下来的问题就是表情如何显示呢？其实这里主要用到了SpannableString拓展性字符串相关知识点，SpannableString可以让一段字符串在显示的时候,将其中某小段文字附着上其他内容或替换成其他内容,拓展内容可以是图片或者是文字格式,比如加粗，显示特殊颜色等。对于SpannableString不熟悉，可先看看[这篇文章 ](http://blog.csdn.net/lan410812571/article/details/9083023 )，下面我只对本篇需要用到的SpannableString作简要介绍：
&emsp;&emsp;<font color='#F55F5C'>ImageSpan</font>，这个是可以将指定的特殊字符替换成我们所需要的图片。也就是我们可以使用"[表情名字]"这个key作为指定的特殊字符，然后在文本中替换成该key所对应的特殊表情即可。
简单实例如下：

```java
SpannableString spannableString = new SpannableString(source);
int size = (int) tv.getTextSize()*13/10;
Bitmap bitmap = BitmapFactory.decodeResource(res, imgRes);
Bitmap scaleBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true);
ImageSpan span = new ImageSpan(context, scaleBitmap);
spannableString.setSpan(span, start, start + key.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
```
&emsp;&emsp;首先将我们要替换的字符串转换成SpannableString再创建一个ImageSpan并把我们的表情图片包含在内，最后利用SpannableString的setSpan方法,将span对象设置在对应位置，这样就完成了特殊字符与文字的转换。参数解析如下，

* <font color='#F55F5C'>start</font> 是需要附着的内容的开始位置
* <font color='#F55F5C'>end</font> 是需要附着的内容的开始位置
* <font color='#F55F5C'>flag</font> 标志位,这里是最常用的EXCLUSIVE_EXCLUSIVE的表示span拓展文本不包含前后（这个参数还有其他类型，这里不过多介绍）

#### <font color='#F55F5C'> 3.2 利用正则表达式找出特殊字符便于转换成表情</font>
&emsp;&emsp;这里我们利用正则表达式找出特殊字符，根据我们自己的需求编写特定的正则表达式，如下：
<font size=5>`String regex = "\\[[\u4e00-\u9fa5\\w]+\\]";`</font>
其中［］是我们特殊需要的字符，因此必须使用“//”进行转义，\u4e00-\u9fa5表示中文，\\w表示下划线的任意单词字符，+ 代表一个或者多个。因此这段正则就代表,**匹配方括号内有一或多个文字和单词字符的文本**。有了正则表达式，剩下就是找匹配的问题了，这里我们可以先用matcher.find()获取到匹配的开始位置,作为setSpan的start值,再使用matcher.group()方法获取到匹配规则的具体表情文字。对于matcher.find()和matcher.group()这里简单介绍一下：

* matcher.find()，代表部分匹配，从当前位置开始匹配，找到一个匹配的子串，将移动下次匹配的位置。因此我们可以通过这个方法获取到匹配的开始位置,作为setSpan的start值（如果字符串中有多个表情就会执行多次匹配）。
* matcher.group()，获取匹配到的具体字符。

下面直接上SpanStringUtils.java类对代码：

```java
package com.zejian.emotionkeyboard.utils;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.widget.TextView;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @author : zejian
 * @time : 2016年1月5日 上午11:30:39
 * @email : shinezejian@163.com
 * @description :文本中的emojb字符处理为表情图片
 */
public class SpanStringUtils {

	public static SpannableString getEmotionContent(int emotion_map_type,final Context context, final TextView tv, String source) {
		SpannableString spannableString = new SpannableString(source);
		Resources res = context.getResources();
		String regexEmotion = "\\[([\u4e00-\u9fa5\\w])+\\]";
		Pattern patternEmotion = Pattern.compile(regexEmotion);
		Matcher matcherEmotion = patternEmotion.matcher(spannableString);
		while (matcherEmotion.find()) {
			// 获取匹配到的具体字符
			String key = matcherEmotion.group();
			// 匹配字符串的开始位置
			int start = matcherEmotion.start();
			// 利用表情名字获取到对应的图片
			Integer imgRes = EmotionUtils.getImgByName(emotion_map_type,key);
			if (imgRes != null) {
				// 压缩表情图片
				int size = (int) tv.getTextSize()*13/10;
				Bitmap bitmap = BitmapFactory.decodeResource(res, imgRes);
				Bitmap scaleBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true);
ImageSpan span = new ImageSpan(context, scaleBitmap);
				spannableString.setSpan(span, start, start + key.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
		return spannableString;
	}
}
```
代码相对比较简单，这里就不啰嗦啦。



#### <font color='#F55F5C'> 3.3 表情面板的实现(ViewPager+GridView)</font>
&emsp;&emsp;这里的自然就是使用到ViewPager和GridView相结合实现多界面滑动的效果，参考了微信的实现，每页都是一个GridView显示20个表情,末尾还有一个删除按钮。实现思路入下：
&emsp;&emsp;利用ViewPager作为滑动控件，同时结合GridView来布局每个表情，GridView会显示3行7列,共21个Item,即每页都是一个GridView显示20个表情,末尾还有一个删除按钮。为了让Item能大小合适,我们在这里利用动态计算的方式设置宽高,因为屏幕宽度各有不同。每个item宽度的计算方式，由(屏幕的宽度－左右边距大小(如果有的话就减去)－每个item间隙距离)/7，最终便得到item的宽度。至于表情面板的高度＝(item宽度＊3+间隙＊6),即可获取中高度，为什么间隙*6？这里并没有什么计算原理，纯粹是我在调试的过程中试出来的值，这个值相对比较合理，也比较美观，当然大家也可根据自己需要调整。最后就是有多少页的问题了，这里可以通过for循环表情集合的所有元素，把每次循环获取的元素添加到一个集合中，每次判断集合是否满20个元素，每满20个集合就利用该集合去创建一个GridView的表情面板View，同时再新建一个集合存放新获取到的元素，以次循环。最后把所有表情生成的一个个GridView放到一个总view集合中,利用ViewPager显示即可。要注意的是在GridView的适配器和点击事件中,都利用position判断,如果是最后一个就进行特殊的显示(删除按钮)和点击处理。

```java
package com.zejian.emotionkeyboard.fragment;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.LinearLayout;
import com.zejian.emotionkeyboard.R;
import com.zejian.emotionkeyboard.adapter.EmotionGridViewAdapter;
import com.zejian.emotionkeyboard.adapter.EmotionPagerAdapter;
import com.zejian.emotionkeyboard.emotionkeyboardview.EmojiIndicatorView;
import com.zejian.emotionkeyboard.utils.DisplayUtils;
import com.zejian.emotionkeyboard.utils.EmotionUtils;
import com.zejian.emotionkeyboard.utils.GlobalOnItemClickManagerUtils;
import java.util.ArrayList;
import java.util.List;
/**
 * Created by zejian
 * Time  16/1/5 下午4:32
 * Email shinezejian@163.com
 * Description:可替换的模板表情，gridview实现
 */
public class EmotiomComplateFragment extends BaseFragment {
    private EmotionPagerAdapter emotionPagerGvAdapter;
    private ViewPager vp_complate_emotion_layout;
    private EmojiIndicatorView ll_point_group;//表情面板对应的点列表
    private int emotion_map_type;
    /**
     * 创建与Fragment对象关联的View视图时调用
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_complate_emotion, container, false);
        initView(rootView);
        initListener();
        return rootView;
    }
    /**
     * 初始化view控件
     */
    protected void initView(View rootView){
        vp_complate_emotion_layout = (ViewPager) rootView.findViewById(R.id.vp_complate_emotion_layout);
        ll_point_group= (EmojiIndicatorView) rootView.findViewById(R.id.ll_point_group);
        //获取map的类型
        emotion_map_type=args.getInt(FragmentFactory.EMOTION_MAP_TYPE);
        initEmotion();
    }
    /**
     * 初始化监听器
     */
    protected void initListener(){
        vp_complate_emotion_layout.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            int oldPagerPos=0;
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }
            @Override
            public void onPageSelected(int position) {
                ll_point_group.playByStartPointToNext(oldPagerPos,position);
                oldPagerPos=position;
            }
            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }
    /**
     * 初始化表情面板
     * 思路：获取表情的总数，按每行存放7个表情，动态计算出每个表情所占的宽度大小（包含间距），
     *      而每个表情的高与宽应该是相等的，这里我们约定只存放3行
     *      每个面板最多存放7*3=21个表情，再减去一个删除键，即每个面板包含20个表情
     *      根据表情总数，循环创建多个容量为20的List，存放表情，对于大小不满20进行特殊
     *      处理即可。
     */
    private void initEmotion() {
        // 获取屏幕宽度
        int screenWidth = DisplayUtils.getScreenWidthPixels(getActivity());
        // item的间距
        int spacing = DisplayUtils.dp2px(getActivity(), 12);
        // 动态计算item的宽度和高度
        int itemWidth = (screenWidth - spacing * 8) / 7;
        //动态计算gridview的总高度
        int gvHeight = itemWidth * 3 + spacing * 6;
        List<GridView> emotionViews = new ArrayList<>();
        List<String> emotionNames = new ArrayList<>();
        // 遍历所有的表情的key
        for (String emojiName : EmotionUtils.getEmojiMap(emotion_map_type).keySet()) {
            emotionNames.add(emojiName);
            // 每20个表情作为一组,同时添加到ViewPager对应的view集合中
            if (emotionNames.size() == 20) {
                GridView gv = createEmotionGridView(emotionNames, screenWidth, spacing, itemWidth, gvHeight);
                emotionViews.add(gv);
                // 添加完一组表情,重新创建一个表情名字集合
                emotionNames = new ArrayList<>();
            }
        }
        // 判断最后是否有不足20个表情的剩余情况
        if (emotionNames.size() > 0) {
            GridView gv = createEmotionGridView(emotionNames, screenWidth, spacing, itemWidth, gvHeight);
            emotionViews.add(gv);
        }
        //初始化指示器
        ll_point_group.initIndicator(emotionViews.size());
        // 将多个GridView添加显示到ViewPager中
        emotionPagerGvAdapter = new EmotionPagerAdapter(emotionViews);
        vp_complate_emotion_layout.setAdapter(emotionPagerGvAdapter);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(screenWidth, gvHeight);
        vp_complate_emotion_layout.setLayoutParams(params);
    }
    /**
     * 创建显示表情的GridView
     */
    private GridView createEmotionGridView(List<String> emotionNames, int gvWidth, int padding, int itemWidth, int gvHeight) {
        // 创建GridView
        GridView gv = new GridView(getActivity());
        //设置点击背景透明
        gv.setSelector(android.R.color.transparent);
        //设置7列
        gv.setNumColumns(7);
        gv.setPadding(padding, padding, padding, padding);
        gv.setHorizontalSpacing(padding);
        gv.setVerticalSpacing(padding * 2);
        //设置GridView的宽高
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(gvWidth, gvHeight);
        gv.setLayoutParams(params);
        // 给GridView设置表情图片
        EmotionGridViewAdapter adapter = new EmotionGridViewAdapter(getActivity(), emotionNames, itemWidth,emotion_map_type);
        gv.setAdapter(adapter);
        //设置全局点击事件
        gv.setOnItemClickListener(GlobalOnItemClickManagerUtils.getInstance(getActivity()).getOnItemClickListener(emotion_map_type));
        return gv;
    }
}
```
注释非常清晰哈。我就不啰嗦了。但这有个要注意的是在for循环时是通过EmotionUtils的getEmojiMap(emotion_map_type).keySet()获取集合，这也印证前面我们所说的EmotiomComplateFragment内部是通过集合标志判断集合类型，最终获取到所需的集合数据，也就生成了不同表情类型的面板。

#### <font color='#F55F5C'> 3.4 表情的输入框插入和删除</font>
&emsp;&emsp;思路：在表情框输入一个表情实际上是在当前光标位置插入一个表情，添加完表情后再把当前光标移动到表情之后，所以我们首先要获取到光标到首位置，这个可以利用EditText.setSelectionStart()方法，添加完表情后要设置光标的位置到表情之后，这个可以使用EditText.setSelection(position)方法。当然如果点击的是删除按钮，那么直接调用系统的 Delete 按钮事件即可。下面直接上代码：

```java
// 点击的是表情
EmotionGridViewAdapter emotionGvAdapter = (EmotionGridViewAdapter) itemAdapter;
if (position == emotionGvAdapter.getCount() - 1) {
    // 如果点击了最后一个回退按钮,则调用删除键事件
    mEditText.dispatchKeyEvent(new KeyEvent(
            KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
} else {
    // 如果点击了表情,则添加到输入框中
    String emotionName = emotionGvAdapter.getItem(position);
    // 获取当前光标位置,在指定位置上添加表情图片文本
    int curPosition = mEditText.getSelectionStart();
    StringBuilder sb = new StringBuilder(mEditText.getText().toString());
    sb.insert(curPosition, emotionName);
    // 特殊文字处理,将表情等转换一下
    mEditText.setText(SpanStringUtils.getEmotionContent(emotion_map_type,
            mContext, mEditText, sb.toString()));
    // 将光标设置到新增完表情的右侧
    mEditText.setSelection(curPosition + emotionName.length());
}
```
&emsp;&emsp;这里要理解一点就是让控件调用系统事件的方法为EditText.displatchKeyEvent(new KeyEvent(action, code));其中action就是动作,用ACTION_DOWN按下动作就可以了而
code为按钮事件码,删除对应的就是KEYCODE_DEL。

##<font color='#F55F5C'> 4.表情点击事件全局监听的实现</font>
&emsp;&emsp;上面弄明白了表情的输入与删除操作后，我们就要考虑一个问题了，那就是在哪里设置监听？直接在创建GridView时，这个确实行得通，不过我们还要再考虑一个问题，那就是如果我们存在多个GridView呢？多复制几遍咯。但我们是高级工程师对吧，这样重复代码显然是不可出现在我们眼前的，因此这里我们决定使用全局监听来设置点击事件，当然这个并非我想到的，这个是在github开源项目我在阅读源码时，发现的，这种方式挺不错，我就拿来用咯。直接上代码：

```java
package com.zejian.emotionkeyboard.utils;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import com.zejian.emotionkeyboard.adapter.EmotionGridViewAdapter;
/**
 * Created by zejian
 * Time  16/1/8 下午5:05
 * Email shinezejian@163.com
 * Description:点击表情的全局监听管理类
 */
public class GlobalOnItemClickManagerUtils {
    private static GlobalOnItemClickManagerUtils instance;
    private EditText mEditText;//输入框
    private static Context mContext;
    public static GlobalOnItemClickManagerUtils getInstance(Context context) {
        mContext=context;
        if (instance == null) {
            synchronized (GlobalOnItemClickManagerUtils.class) {
                if(instance == null) {
                    instance = new GlobalOnItemClickManagerUtils();
                }
            }
        }
        return instance;
    }
    public void attachToEditText(EditText editText) {
        mEditText = editText;
    }
    public AdapterView.OnItemClickListener getOnItemClickListener(final int emotion_map_type) {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object itemAdapter = parent.getAdapter();
                if (itemAdapter instanceof EmotionGridViewAdapter) {
                    // 点击的是表情
                    EmotionGridViewAdapter emotionGvAdapter = (EmotionGridViewAdapter) itemAdapter;
                    if (position == emotionGvAdapter.getCount() - 1) {
                        // 如果点击了最后一个回退按钮,则调用删除键事件
                        mEditText.dispatchKeyEvent(new KeyEvent(
                                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
                    } else {
                        // 如果点击了表情,则添加到输入框中
                        String emotionName = emotionGvAdapter.getItem(position);
                        // 获取当前光标位置,在指定位置上添加表情图片文本
                        int curPosition = mEditText.getSelectionStart();
                        StringBuilder sb = new StringBuilder(mEditText.getText().toString());
                        sb.insert(curPosition, emotionName);
                        // 特殊文字处理,将表情等转换一下
                        mEditText.setText(SpanStringUtils.getEmotionContent(emotion_map_type,
                                mContext, mEditText, sb.toString()));
                        // 将光标设置到新增完表情的右侧
                        mEditText.setSelection(curPosition + emotionName.length());
                    }
                }
            }
        };
    }
}
```
&emsp;&emsp;代码相当简单，就是创建一个AdapterView.OnItemClickListener的全局监听器，然后在里面实现表情的输入与删除操作即可。那么怎么使用呢？我们在EmotionMainFragment类中使用创建GlobalOnItemClickManagerUtils，并绑定编辑框，部分代码如下:

```java
 //创建全局监听
GlobalOnItemClickManagerUtils globalOnItemClickManager= GlobalOnItemClickManagerUtils.getInstance(getActivity());
if(isBindToBarEditText){
  //绑定当前Bar的编辑框
  globalOnItemClickManager.attachToEditText(bar_edit_text);
}else{
  // false,则表示绑定contentView, 此时外部提供的contentView必定也是EditText
  globalOnItemClickManager.attachToEditText((EditText) contentView);
  mEmotionKeyboard.bindToEditText((EditText)contentView);
}
```
&emsp;&emsp;绑定的编辑框可能有两种情况，可能是Bar上的编辑框，但也可能是contentView，此时外部提供的contentView是EditText(可以直接理解为是把之前所说的listview替换成了edittext)。最后别忘记在EmotiomComplateFragment类种创建GridView时注册该监听器，

```java
//设置全局点击事件
gv.setOnItemClickListener(GlobalOnItemClickManagerUtils.getInstance(getActivity()).getOnItemClickListener(emotion_map_type));
```
好了，到此本篇也完结了，[点击源码地址下载](http://download.csdn.net/detail/javazejian/9409811)，大家其他看源码吧，稍后我会上传一份到github。






