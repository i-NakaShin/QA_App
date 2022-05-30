package jp.techacademy.shintaro.nakagawa.qa_app

import java.io.Serializable
import java.util.*

class Answer (
    val body: String = "",
    val name: String = "",
    val uid: String = "",
    val answerUid: String = UUID.randomUUID().toString()
): Serializable