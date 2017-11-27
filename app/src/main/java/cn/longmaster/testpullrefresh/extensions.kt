package cn.longmaster.testpullrefresh

/**
 *
 * Created by wangyu on 2017/11/27.
 */
inline fun post(crossinline doRunnable: () -> Unit) {
    App.handler.post {
        doRunnable()
    }
}

inline fun postDelayed(crossinline doRunnable: () -> Unit, during: Long) {
    App.handler.postDelayed({
        doRunnable()
    }, during)
}