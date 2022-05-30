package jp.techacademy.shintaro.nakagawa.qa_app

import android.content.Context
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_answer_send.*
import kotlinx.android.synthetic.main.activity_answer_send.progressBar
import kotlinx.android.synthetic.main.activity_answer_send.sendButton
import kotlinx.android.synthetic.main.activity_question_send.*
import java.io.ByteArrayOutputStream

class AnswerSendActivity : AppCompatActivity(), View.OnClickListener, DatabaseReference.CompletionListener {

    private lateinit var mQuestion: Question

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_answer_send)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question

        // UIの準備
        sendButton.setOnClickListener(this)
    }

    override fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
        progressBar.visibility = View.GONE

        if (databaseError == null) {
            finish()
        } else {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.send_answer_failure), Snackbar.LENGTH_LONG).show()
        }

    }

    override fun onClick(v: View) {
        // キーボードが出てたら閉じる
        val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

        val dataBaseReference = FirebaseDatabase.getInstance().reference

        // UID
        val uid: String = FirebaseAuth.getInstance().currentUser!!.uid

        // 表示名
        // Preferenceから名前を取る
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val name = sp.getString(NameKEY, "")

        // 回答を取得する
        val answer = answerEditText.text.toString()

        if (answer.isEmpty()) {
            // 回答が入力されていない時はエラーを表示するだけ
            Snackbar.make(v, getString(R.string.answer_error_message), Snackbar.LENGTH_LONG).show()
            return
        }

        val fireStoreAnswer = Answer(answer, name!!, uid)

        // FirestoreQuestionのインスタンスを作成し、値を詰めていく
        var fireStoreQuestion = FireStoreQuestion()

        fireStoreQuestion.id = mQuestion.questionUid
        fireStoreQuestion.uid = mQuestion.uid
        fireStoreQuestion.title = mQuestion.title
        fireStoreQuestion.body = mQuestion.body
        fireStoreQuestion.name = mQuestion.name
        fireStoreQuestion.genre = mQuestion.genre
        fireStoreQuestion.answers = mQuestion.answers

        if (mQuestion.imageBytes != null) {
            val bitmapString = Base64.encodeToString(mQuestion.imageBytes, Base64.DEFAULT)
            fireStoreQuestion.image = bitmapString
        }

        fireStoreQuestion.answers.add(fireStoreAnswer)

        FirebaseFirestore.getInstance()
            .collection(ContentsPATH)
            .document(fireStoreQuestion.id)
            .set(fireStoreQuestion)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                finish()
            }
            .addOnFailureListener {
                it.printStackTrace()
                progressBar.visibility = View.GONE
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.question_send_error_message), Snackbar.LENGTH_LONG).show()
            }

        progressBar.visibility = View.VISIBLE
    }

}