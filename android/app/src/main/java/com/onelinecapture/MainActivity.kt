package com.onelinecapture

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

// 화면은 두 상태뿐이다: 연결 전(코드 입력)과 연결 후(한 줄 입력). 목록·요약은 웹을 연다 (spec.md §9)
class MainActivity : Activity() {

    private lateinit var store: Store
    private lateinit var linkPanel: View
    private lateinit var capturePanel: View
    private lateinit var codeInput: EditText
    private lateinit var linkButton: Button
    private lateinit var linkStatus: TextView
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var recent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store = Store(this)

        linkPanel = findViewById(R.id.link_panel)
        capturePanel = findViewById(R.id.capture_panel)
        codeInput = findViewById(R.id.code_input)
        linkButton = findViewById(R.id.link_button)
        linkStatus = findViewById(R.id.link_status)
        input = findViewById(R.id.input)
        status = findViewById(R.id.status)
        recent = findViewById(R.id.recent)

        linkButton.setOnClickListener { link() }
        codeInput.setOnEditorActionListener { _, actionId, event -> isDone(actionId, event).also { if (it) link() } }
        findViewById<Button>(R.id.get_code_button).setOnClickListener { openWeb("/#/account") }

        input.setOnEditorActionListener { _, actionId, event -> isDone(actionId, event).also { if (it) save() } }
        findViewById<Button>(R.id.open_web_button).setOnClickListener { openWeb("/#/list") }
        findViewById<Button>(R.id.sign_out_button).setOnClickListener { signOut() }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val linked = store.token != null
        linkPanel.visibility = if (linked) View.GONE else View.VISIBLE
        capturePanel.visibility = if (linked) View.VISIBLE else View.GONE
        recent.text = store.recent.joinToString("\n")
        if (linked) input.requestFocus() else codeInput.requestFocus()
    }

    private fun link() {
        val code = codeInput.text.toString().trim()
        if (code.isEmpty()) return
        linkButton.isEnabled = false
        linkStatus.text = "연결 중..."
        Thread {
            val result = runCatching { CaptureApi.link(code) }
            runOnUiThread {
                linkButton.isEnabled = true
                result.onSuccess { token ->
                    store.token = token
                    codeInput.text.clear()
                    linkStatus.text = ""
                    status.text = "연결했습니다. 한 줄을 던져 보세요"
                    render()
                }.onFailure { linkStatus.text = it.message }
            }
        }.start()
    }

    private fun save() {
        val text = input.text.toString().trim()
        if (text.isEmpty()) return
        // 웹과 같다: 입력창을 먼저 비워 다음 줄을 바로 치게 하고, 실패하면 되돌린다 (spec.md §3)
        input.text.clear()
        status.text = "저장 중..."
        Capture.save(this, text) { result ->
            when (result) {
                is Capture.Result.Saved -> {
                    status.text = result.message
                    recent.text = store.recent.joinToString("\n")
                }
                is Capture.Result.Failed -> {
                    if (input.text.isEmpty()) input.setText(text)
                    input.setSelection(input.text.length)
                    status.text = result.message
                    if (result.signedOut) render()
                }
            }
        }
    }

    private fun signOut() {
        val token = store.token
        store.signOut()
        // 서버에서 못 지워도 이 기기에서는 나간다. 로그아웃이 실패해서 못 나가는 일은 없어야 한다
        if (token != null) Thread { runCatching { CaptureApi.logout(token) } }.start()
        status.text = ""
        render()
    }

    private fun openWeb(path: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.BASE_URL + path)))
    }

    // 하드웨어 키보드의 엔터와 소프트 키보드의 완료를 같이 받는다
    private fun isDone(actionId: Int, event: KeyEvent?): Boolean =
        actionId == EditorInfo.IME_ACTION_DONE ||
            (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
}
