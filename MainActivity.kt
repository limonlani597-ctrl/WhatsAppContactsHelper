package com.hamid.contactshelper

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var input: EditText
    private lateinit var status: TextView
    private var contacts: List<Pair<String, String>> = emptyList()
    private val createFile = 501

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 24)
            setBackgroundColor(0xFFF2FBF5.toInt())
        }
        val title = TextView(this).apply {
            text = "مرتب‌ساز مخاطبین واتساپ"
            textSize = 23f
            setTextColor(0xFF075E35.toInt())
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        root.addView(title, matchWrap())
        val hint = TextView(this).apply {
            text = "شماره‌ها یا متن کپی‌شده را اینجا بچسبانید. این برنامه خودش وارد واتساپ نمی‌شود و فقط متن واردشده را پردازش می‌کند."
            textSize = 15f
            setPadding(0, 18, 0, 14)
        }
        root.addView(hint, matchWrap())
        input = EditText(this).apply {
            hint = "مثال:\nعلی 09123456789\n+989123456789\nمریم: 09351234567"
            gravity = Gravity.TOP or Gravity.RIGHT
            minLines = 8
            textSize = 16f
            setPadding(14, 14, 14, 14)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        root.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        val parse = Button(this).apply {
            text = "مرتب‌سازی و حذف تکراری‌ها"
            setOnClickListener { processText() }
        }
        root.addView(parse, matchWrap())
        val export = Button(this).apply {
            text = "ساخت فایل مخاطبین VCF"
            setOnClickListener { exportVcf() }
        }
        root.addView(export, matchWrap())
        status = TextView(this).apply { text = "هنوز شماره‌ای پردازش نشده است."; textSize = 15f; setPadding(0, 12, 0, 0) }
        root.addView(status, matchWrap())
        setContentView(root)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 8 }

    private fun processText() {
        val raw = input.text.toString()
        val regex = Regex("(?<!\\d)(?:\\+?\\d[\\d\\s()\\-]{6,}\\d)(?!\\d)")
        val found = linkedMapOf<String, String>()
        for (match in regex.findAll(raw)) {
            val original = match.value.trim()
            var number = original.replace(Regex("[\\s()\\-]"), "")
            if (number.startsWith("00")) number = "+" + number.drop(2)
            if (number.startsWith("09") && number.length == 11) number = "+98" + number.drop(1)
            if (number.startsWith("9") && number.length == 10) number = "+98" + number
            val digits = number.filter { it.isDigit() }
            if (digits.length in 8..15) found.putIfAbsent(number, "مخاطب ${found.size + 1}")
        }
        contacts = found.map { it.key to it.value }
        status.text = "تعداد شماره‌های یکتا: ${contacts.size}\nبرای خروجی گرفتن، دکمه ساخت فایل VCF را بزنید."
    }

    private fun exportVcf() {
        if (contacts.isEmpty()) {
            Toast.makeText(this, "اول متن شماره‌ها را وارد و مرتب‌سازی کنید.", Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/vcard"
            putExtra(Intent.EXTRA_TITLE, "whatsapp_contacts.vcf")
        }
        startActivityForResult(intent, createFile)
    }

    @Deprecated("Deprecated in Android API, retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == createFile && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8).use { writer ->
                    contacts.forEachIndexed { index, item ->
                        writer?.append("BEGIN:VCARD\r\nVERSION:3.0\r\n")
                        writer?.append("FN:${item.second}\r\n")
                        writer?.append("TEL;TYPE=CELL:${item.first}\r\nEND:VCARD\r\n")
                    }
                }
                Toast.makeText(this, "فایل مخاطبین ساخته شد.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "ساخت فایل ناموفق بود: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
