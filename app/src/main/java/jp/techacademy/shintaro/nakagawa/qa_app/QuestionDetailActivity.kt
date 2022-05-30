package jp.techacademy.shintaro.nakagawa.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlinx.android.synthetic.main.activity_question_detail.listView
import kotlinx.android.synthetic.main.content_main.*

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter

    private var favoriteList = mutableListOf<String>()
    private var isFavorite: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question

        title = mQuestion.title

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                // --- ここから ---
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
                // --- ここまで ---
            }
        }

        favorite_fab.setOnClickListener { v ->
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser
            val db = FirebaseFirestore.getInstance()

            if (isFavorite) {
                for (i in favoriteList.indices) {
                    if (mQuestion.questionUid == favoriteList[i]) {
                        favoriteList.removeAt(i)
                        break
                    }
                }
                val map: Map<String, MutableList<String>> = mapOf(user!!.uid to favoriteList)
                db.collection(FavoritePATH)
                    .document(user!!.uid)
                    .set(map)
                    .addOnSuccessListener {
                        Snackbar.make(v, "お気に入り削除しました", Snackbar.LENGTH_LONG).show()
                    }
                isFavorite = false
            } else {
                favoriteList.add(mQuestion.questionUid)
                val map: Map<String, MutableList<String>> = mapOf(user!!.uid to favoriteList)
                db.collection(FavoritePATH)
                    .document(user!!.uid)
                    .set(map)
                    .addOnSuccessListener {
                        Snackbar.make(v, "お気に入り登録しました", Snackbar.LENGTH_LONG).show()
                    }
                isFavorite = true
            }
            favorite_fab.setImageResource(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border)
        }
    }

    override fun onResume() {
        super.onResume()

        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            favorite_fab.visibility = View.GONE
        } else {
            favorite_fab.visibility = View.VISIBLE

            val db = FirebaseFirestore.getInstance()
            db.collection(FavoritePATH)
                .document(user!!.uid)
                .get()
                .addOnCompleteListener {
                    if (it.isSuccessful && it.result.data?.get(user!!.uid) != null) {
                        favoriteList = it.result.data?.get(user!!.uid) as MutableList<String>
                        for (qid in favoriteList) {
                            if (mQuestion.questionUid == qid) {
                                isFavorite = true
                                break
                            }
                            isFavorite = false
                        }
                    } else {
                        isFavorite = false
                    }

                    favorite_fab.setImageResource(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border)
                }
        }

        val db = FirebaseFirestore.getInstance()
        db.collection(ContentsPATH)
            .document(mQuestion.questionUid)
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
                    mQuestion = Question(fQuestion.title, fQuestion.body, fQuestion.name, fQuestion.uid,
                                        fQuestion.id, fQuestion.genre, bytes, fQuestion.answers)

                    // ListViewの準備
                    mAdapter = QuestionDetailListAdapter(this, mQuestion)
                    listView.adapter = mAdapter
                    Log.d("kotlintest", "list update!")
                    mAdapter.notifyDataSetChanged()
                } else {
                    finish()
                }
            }
    }
}