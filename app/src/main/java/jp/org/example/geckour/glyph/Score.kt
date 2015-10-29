package jp.org.example.geckour.glyph

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView

import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker

import java.util.ArrayList

class Score : Activity() {
    internal var db: SQLiteDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_score)

        val t: Tracker? = (application as Analytics).getTracker(Analytics.TrackerName.APP_TRACKER)
        t?.setScreenName("ScoreActivity")
        t?.send(HitBuilders.AppViewBuilder().build())

        val scores = ArrayList<Scores>()
        val dbHelper = DBHelper(this)
        db = dbHelper.readableDatabase
        val c: Cursor? = db?.query(DBHelper.TABLE_NAME3, null, null, null, null, null, "cast(correct_times as double) / total_times desc")
        if (c != null) {
            c.moveToFirst()
            //val first = c.getInt(0)
            while (true) {
                if (c.getInt(c.getColumnIndex("total_times")) > 0) {
                    scores.add(Scores(c.getInt(0), c.getInt(c.getColumnIndex("correct_times")), c.getInt(c.getColumnIndex("total_times"))))
                } else {
                    scores.add(Scores(c.getInt(0)))
                }
                if (c.isLast) {
                    break
                }
                c.moveToNext()
            }
            c.close()
        }

        val customAdapter = CustomAdapter(this, 0, scores)
        val listView = findViewById(R.id.listView) as ListView
        listView.adapter = customAdapter
    }

    private inner class Scores {
        internal var key = -1
        internal var value = -1
        internal var corrects = -1
        internal var total = -1

        constructor(key: Int, corrects: Int, total: Int) {
            this.key = key
            this.value = (100 * corrects.toDouble() / total).toInt()
            this.corrects = corrects
            this.total = total
        }

        constructor(key: Int) {
            this.key = key
        }
    }

    /*
    private class MyComparator implements Comparator<Scores> {
        @Override
        public int compare(Scores s1, Scores s2) {
            return s1.value > s2.value ? 1 : -1;
        }
    }
    */
    inner class CustomAdapter(context: Context, textViewResourceId: Int, objects: List<Scores>) : ArrayAdapter<Scores>(context, textViewResourceId, objects) {
        internal var layoutInflater: LayoutInflater

        init {
            layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            val item = getItem(position)

            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.view_list, null)
            }

            val textView1 = convertView!!.findViewById(R.id.text1) as TextView
            val c: Cursor? = db?.query(DBHelper.TABLE_NAME2, null, "id = " + item.key, null, null, null, null)
            if (c != null) {
                c.moveToFirst()
                textView1.text = c.getString(c.getColumnIndex("sequence"))
                c.close()
                val textView2 = convertView.findViewById(R.id.text2) as TextView
                val text = "result: " + (if (item.value != -1) item.value.toString() + "%" else " - ")
                textView2.text = text
            }

            return convertView
        }
    }
}
