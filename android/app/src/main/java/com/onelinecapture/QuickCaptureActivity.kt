package com.onelinecapture

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

// 빠른 설정 타일에서 여는 작은 입력창. 엔터 한 번이면 저장하고 닫힌다
class QuickCaptureActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 쓰기만 하는 창이라 잠금화면 위에 띄운다. 기록을 읽는 화면은 여기서 열리지 않는다
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        if (Store(this).token == null) {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
            return
        }

        setContentView(R.layout.activity_quick)
        val input = findViewById<EditText>(R.id.quick_input)
        val status = findViewById<TextView>(R.id.quick_status)

        input.setOnEditorActionListener { _, actionId, event ->
            val done = actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            if (done) {
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    input.isEnabled = false
                    status.text = "저장 중..."
                    Capture.save(this, text) { result ->
                        when (result) {
                            is Capture.Result.Saved -> {
                                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            // 실패하면 창을 닫지 않는다. 친 글자가 사라지면 다시 칠 이유가 없어진다
                            is Capture.Result.Failed -> {
                                input.isEnabled = true
                                status.text = result.message
                            }
                        }
                    }
                }
            }
            done
        }
    }
}
