package com.jianyuyouhun.testpullrefresh

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.jianyuyouhun.testpullrefresh.view.PullRefreshLayout
import kotlinx.android.synthetic.main.activity_recycler.*

/**
 *
 * Created by wangyu on 2017/11/28.
 */

class RecyclerViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycler)
        val list = (1..10).map { "item_string $it" }
        val recyclerAdapter = RecyclerAdapter(this)
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = recyclerAdapter
        recyclerAdapter.setData(list)
        pullRefreshLayout_horizontal.setOnPullListener { type, fraction, changed ->
            if (!changed) return@setOnPullListener
            when (type) {
                PullRefreshLayout.PullType.TYPE_EDGE_LEFT -> {
                    if (fraction == 1f)
                        header_horizontal.text = "<<"
                    else
                        header_horizontal.text = ">>"
                }
                PullRefreshLayout.PullType.TYPE_EDGE_RIGHT -> {
                    if (fraction == 1f)
                        footer_horizontal.text = ">>"
                    else
                        footer_horizontal.text = "<<"
                }
            }
        }
        pullRefreshLayout_horizontal.setOnTriggerListener {
            when (it) {
                PullRefreshLayout.PullType.TYPE_EDGE_LEFT -> {
                    header_horizontal.text = "ing"
                }
                PullRefreshLayout.PullType.TYPE_EDGE_RIGHT -> {
                    footer_horizontal.text = "ing"
                }
            }
            postDelayed( {
                pullRefreshLayout_horizontal.stopRefresh()
            }, 3000)
        }
    }

    class RecyclerAdapter(val context: Context) : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

        private var list = ArrayList<String>()

        fun setData(list: List<String>) {
            this.list.clear()
            this.list.addAll(list)
            notifyDataSetChanged()
        }

        fun getData(): List<String> = ArrayList<String>(list)

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder =
                ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_string_horizontal, parent, false))

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
            holder?.tv?.text = list[position]
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            lateinit var tv: TextView

            init {
                tv = itemView.findViewById(R.id.tv)
            }
        }

    }
}
