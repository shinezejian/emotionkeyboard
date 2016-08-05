package com.zejian.emotionkeyboard.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * Created by zejian
 * Time  16/1/5 下午3:51
 * Email shinezejian@163.com
 * Description:封装代码的基类
 *             基类BaseFragment中的传递参数args可以供子类选择性使用
 */
public class BaseFragment extends Fragment{

    //传递过来的参数Bundle，供子类使用
    protected Bundle args;

    /**
     * 创建fragment的静态方法，方便传递参数
     * @param args 传递的参数
     * @return
     */
    public static <T extends Fragment>T newInstance(Class clazz,Bundle args) {
        T mFragment=null;
        try {
            mFragment= (T) clazz.newInstance();
        } catch (java.lang.InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        mFragment.setArguments(args);
        return mFragment;
    }

    /**
     * 初始创建Fragment对象时调用
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        args = getArguments();
    }




}
