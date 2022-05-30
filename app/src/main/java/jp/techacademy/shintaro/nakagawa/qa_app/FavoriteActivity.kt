package jp.techacademy.shintaro.nakagawa.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
// findViewById()を呼び出さずに該当Viewを取得するために必要となるインポート宣言
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.activity_favorite.*
import kotlinx.android.synthetic.main.app_bar_main.fab
import kotlinx.android.synthetic.main.app_bar_main.toolbar
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.content_main.listView

class FavoriteActivity : AppCompatActivity() {

    // --- ここから ---
    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter
    private lateinit var mQuestion: Question

    private var questionUid: String? = null
    private var genre: String? = null
    private var mGenreRef: DatabaseReference? = null

    var favoriteList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)
        //idがtoolbarがインポート宣言により取得されているので
        //id名でActionBarのサポートを依頼
        //ActionBarを設定する
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.menu_favorite_label)
        }

        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        listView.setOnItemClickListener{parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            finish()
        }
        
        val favDb = FirebaseFirestore.getInstance()
        favDb.collection(FavoritePATH)
            .document(user!!.uid)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful && it.result.data?.get(user!!.uid) != null) {
                    favoriteList = it.result.data?.get(user!!.uid) as MutableList<String>
                    Log.d("kotlintest", favoriteList[0])
                    Log.d("kotlintest", "fav list")

                    mQuestionArrayList.clear()
                    mAdapter.setQuestionArrayList(mQuestionArrayList)
                    listView.adapter = mAdapter

                    val db = FirebaseFirestore.getInstance()
                    if (favoriteList != null) {
                        Log.d("kotlintest", "not null")
                        for (qid in favoriteList) {
                            Log.d("kotlintest", qid)
                            db.collection(ContentsPATH)
                                .document(qid)
                                .get()
                                .addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        val fQuestion = it.result.toObject(FireStoreQuestion::class.java)
                                        val bytes =
                                            if (fQuestion!!.image.isNotEmpty()) {
                                                Base64.decode(fQuestion.image, Base64.DEFAULT)
                                            } else {
                                                byteArrayOf()
                                            }
                                        val question = Question(
                                            fQuestion.title, fQuestion.body, fQuestion.name, fQuestion.uid,
                                            fQuestion.id, fQuestion.genre, bytes, fQuestion.answers
                                        )
                                        mQuestionArrayList.add(question)
                                    } else {
                                        finish()
                                    }
                                    mAdapter.notifyDataSetChanged()
                                }

                        }
                    }
                }
            }

//        mQuestionArrayList.clear()
//        mAdapter.setQuestionArrayList(mQuestionArrayList)
//        listView.adapter = mAdapter

//        val db = FirebaseFirestore.getInstance()
//        if (favoriteList != null) {
//            Log.d("kotlintest", "not null")
//            for (qid in favoriteList) {
//                Log.d("kotlintest", qid)
//                db.collection(ContentsPATH)
//                    .document(qid)
//                    .get()
//                    .addOnCompleteListener {
//                        if (it.isSuccessful) {
//                            val fQuestion = it.result.toObject(FireStoreQuestion::class.java)
//                            val bytes =
//                                if (fQuestion!!.image.isNotEmpty()) {
//                                    Base64.decode(fQuestion.image, Base64.DEFAULT)
//                                } else {
//                                    byteArrayOf()
//                                }
//                            val question = Question(
//                                fQuestion.title, fQuestion.body, fQuestion.name, fQuestion.uid,
//                                fQuestion.id, fQuestion.genre, bytes, fQuestion.answers
//                            )
//                            mQuestionArrayList.add(question)
//                        } else {
//                            finish()
//                        }
//                    }
//                
//            }
//        }

    }

    // --- ここまで追加する ---

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
}