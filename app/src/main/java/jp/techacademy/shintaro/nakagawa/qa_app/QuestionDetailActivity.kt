package jp.techacademy.shintaro.nakagawa.qa_app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var mDataBaseReference: DatabaseReference

    private var isFavorite: Boolean = false
    private lateinit var answerUid: String

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    private val mfavListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val questionUid = dataSnapshot.key

            if (questionUid == mQuestion.questionUid) {
                isFavorite = true
            }

            favorite_fab.setImageResource(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border)
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser
        mDataBaseReference = FirebaseDatabase.getInstance().reference

        if (user == null) {
            favorite_fab.visibility = View.GONE
        }

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

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

        favorite_fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser
            val favRef = mDataBaseReference.child(FavoritePATH).child(user!!.uid).child(mQuestion.questionUid)

            if (isFavorite) {
                favRef.setValue(null)
                isFavorite = false
            } else {
                favRef.setValue(mQuestion.genre.toString())
                isFavorite = true
            }
            favorite_fab.setImageResource(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border)
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)
    }

    override fun onResume() {
        super.onResume()

        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser
        mDataBaseReference = FirebaseDatabase.getInstance().reference

        if (user == null) {
            favorite_fab.visibility = View.GONE
        } else {
            val favRef = mDataBaseReference.child(FavoritePATH).child(user!!.uid)
            favRef.addChildEventListener(mfavListener)

            favorite_fab.visibility = View.VISIBLE
        }
        favorite_fab.setImageResource(if (isFavorite) R.drawable.ic_star else R.drawable.ic_star_border)
    }
}