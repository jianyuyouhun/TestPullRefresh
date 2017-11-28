package com.jianyuyouhun.testpullrefresh

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import com.jianyuyouhun.testpullrefresh.view.PullRefreshLayout
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val list = (1..30).map { "item_string $it" }
        listView.adapter = ArrayAdapter<String>(this, R.layout.item_string, R.id.tv, list)

        pullRefreshLayout.setOnPullListener { type, fraction, changed ->
            if (!changed) return@setOnPullListener
            when (type) {
                PullRefreshLayout.PullType.TYPE_EDGE_TOP -> {
                    if (fraction == 1f)
                        header.text = "松开刷新"
                    else
                        header.text = "下拉刷新"
                }
                PullRefreshLayout.PullType.TYPE_EDGE_BOTTOM -> {
                    if (fraction == 1f)
                        footer.text = "松开加载"
                    else
                        footer.text = "上拉加载"
                }
            }
        }
        pullRefreshLayout.setOnTriggerListener {
            when (it) {
                PullRefreshLayout.PullType.TYPE_EDGE_TOP -> {
                    header.text = "正在刷新"
                }
                PullRefreshLayout.PullType.TYPE_EDGE_BOTTOM -> {
                    footer.text = "正在加载"
                }
            }
            postDelayed( {
                pullRefreshLayout.stopRefresh()
            }, 3000)
        }
    }
}
