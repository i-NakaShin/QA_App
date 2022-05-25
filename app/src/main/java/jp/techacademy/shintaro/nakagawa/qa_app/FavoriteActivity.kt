package jp.techacademy.shintaro.nakagawa.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
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

    private  var mFavoriteMap = mutableMapOf<String, String>()
    private var questionUid: String? = null
    private var genre: String? = null
    private var mGenreRef: DatabaseReference? = null

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            var hasFavorite: Boolean = false
            var favoriteGenre: Int = 0

            for (i in mFavoriteMap) {
                if (dataSnapshot.key == i.key) {
                    hasFavorite = true
                    favoriteGenre = i.value.toInt()
                    break
                }
            }

            if (!hasFavorite) return
            for (question in mQuestionArrayList) {
                if (dataSnapshot.key == question.questionUid) return
            }

            val map = dataSnapshot.value as Map<String, String>
            val title = map["title"] ?: ""
            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""
            val imageString = map["image"] ?: ""
            val bytes =
                if (imageString.isNotEmpty()) {
                    Base64.decode(imageString, Base64.DEFAULT)
                } else {
                    byteArrayOf()
                }

            val answerArrayList = ArrayList<Answer>()
            val answerMap = map["answers"] as Map<String, String>?
            if (answerMap != null) {
                for (key in answerMap.keys) {
                    val temp = answerMap[key] as Map<String, String>
                    val answerBody = temp["body"] ?: ""
                    val answerName = temp["name"] ?: ""
                    val answerUid = temp["uid"] ?: ""
                    val answer = Answer(answerBody, answerName, answerUid, key)
                    answerArrayList.add(answer)
                }
            }

            val question = Question(title, body, name, uid, dataSnapshot.key ?: "",
                favoriteGenre, bytes, answerArrayList)
            mQuestionArrayList.add(question)
            mAdapter.notifyDataSetChanged()
        }



        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot as Map<String, String>

            // 変更があったQuestionを探す
            for (question in mQuestionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    // このアプリで変更がある可能性があるのは回答（Answer)のみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            question.answers.add(answer)
                        }
                    }

                    mAdapter.notifyDataSetChanged()
                }
            }
        }

        override fun onChildRemoved(p0: DataSnapshot) {

        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {

        }

        override fun onCancelled(p0: DatabaseError) {

        }
    }

    private val mfavListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            questionUid = dataSnapshot.key as String ?:""
            genre = dataSnapshot.value as String ?:""

            mFavoriteMap.plusAssign(questionUid.toString() to genre.toString())
            val qidRef = mDatabaseReference.child(ContentsPATH).child(genre.toString())
            qidRef.addChildEventListener(mEventListener)
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
        setContentView(R.layout.activity_favorite)
        //idがtoolbarがインポート宣言により取得されているので
        //id名でActionBarのサポートを依頼
        //ActionBarを設定する
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = getString(R.string.menu_favorite_label)
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

        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mFavoriteMap.clear()
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        listView.adapter = mAdapter

        mGenreRef = mDatabaseReference.child(FavoritePATH).child(user!!.uid)
        mGenreRef!!.addChildEventListener(mfavListener)
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