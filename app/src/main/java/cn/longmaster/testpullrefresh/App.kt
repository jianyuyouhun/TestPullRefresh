package cn.longmaster.testpullrefresh

import android.app.Application
import android.os.Handler
import android.os.Looper

/**
 *
 * Created by wangyu on 2017/11/27.
 */
class App: Application() {
   companion object {
       val handler by lazy { Handler(Looper.getMainLooper()) }
   }
}