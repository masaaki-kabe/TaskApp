package jp.techacademy.masaaki.kabe.taskapp

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_main.*
import io.realm.RealmChangeListener
import io.realm.Sort
import android.content.Intent
import android.support.v7.app.AlertDialog
import java.util.*
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.util.Log
import android.view.View


const val EXTRA_TASK="jp.techacademy.masaaki.kabe.tskapp.TASK"

class MainActivity : AppCompatActivity() {

    private lateinit var mRealm:Realm
    private val mRealmListener=object:RealmChangeListener<Realm>{
        override  fun onChange(element:Realm){
            reloadListView()
        }
    }

    private lateinit var mTaskAdapter: TaskAdapter


    private val mOnClickListener= View.OnClickListener {
        searchcategory()
    }

    private val mOnClickListener_2=View.OnClickListener {
        reloadListView()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        kensaku_button.setOnClickListener(mOnClickListener)
        modoru_button.setOnClickListener(mOnClickListener_2)


        fab.setOnClickListener { view ->
           val intent=Intent(this@MainActivity,inputActivity::class.java)
           startActivity(intent)
        }

        //Realmの設定
        mRealm= Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        //ListViewの設定
        mTaskAdapter= TaskAdapter(this@MainActivity)
        
        //ListViewをタップした時の処理
        listView1.setOnItemClickListener{parent,_, position, _ ->
            //入力・編集する画面に遷移させる
            val task=parent.adapter.getItem(position) as Task
            val intent=Intent(this,inputActivity::class.java)
            intent.putExtra(EXTRA_TASK,task.id)
            startActivity(intent)
        }

        //ListViewを長押ししたときの処理
        listView1.setOnItemLongClickListener{parent,_, position,_ ->
            //タスクを削除する
            val task=parent.adapter.getItem(position) as Task

            //ダイアログを表示する
            val builder=AlertDialog.Builder(this)

            builder.setTitle("削除")

            builder.setMessage(task.title+"を削除しますか")

            builder.setPositiveButton("OK"){_,_->
                val results=mRealm.where(Task::class.java) .equalTo("id",task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()


                val resultIntent=Intent(applicationContext,TaskAlarmReceiver::class.java)
                val resultPendigIntent=PendingIntent.getBroadcast(
                    this@MainActivity,
                            task.id,
                            resultIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager=getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendigIntent)

                reloadListView()
            }

            builder.setNegativeButton("CANCEL",null)

            val dialog=builder.create()
            dialog.show()

            true
        }

        reloadListView()
    }

    private fun reloadListView(){
        //Realmデータベースから「すべてのデータを取得して新しい日時順に並べた結果」を取得
        val taskRealmResults= mRealm.where(Task::class.java).findAll().sort("date",Sort.DESCENDING)



        //上記の結果をTaskListとしてセットする
        mTaskAdapter.taskList=mRealm.copyFromRealm(taskRealmResults)

        //TaskのListView用のアダプタに渡す
      listView1.adapter=mTaskAdapter

        //表示を更新するために、アダプターにデータが更新されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()
    }


    private fun searchcategory(){

        //val taskRealmResults=mRealm.where(Task::class.java).findAll()
        //val max_val=taskRealmResults.max("id")
        //Log.d("kotlintest",max_val.toString())
        
        val category_name=kensaku.text.toString()
        //Log.d("kotlintest",category_name)

        val taskRealmReslts_cate=mRealm.where(Task::class.java).equalTo("category", category_name).findAll()
        val max_val=taskRealmReslts_cate.max("id")
        Log.d("kotlintest",max_val.toString())

        if(max_val!=null) {
            mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmReslts_cate)
            listView1.adapter = mTaskAdapter
            mTaskAdapter.notifyDataSetChanged()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        mRealm.close()
    }


}
