package com.example.readsmsv2.ui.transform

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.readsmsv2.R
import com.google.android.material.textview.MaterialTextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsDetailActivity : AppCompatActivity() {

    data class SmsDetail(val body: String, val date: Long)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_detail)

        val mobile = intent.getStringExtra("mobile") ?: ""

        // Title + back button
        supportActionBar?.title = mobile
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerDetail)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val messages = loadMessagesForSender(mobile)
        recyclerView.adapter = SmsDetailAdapter(messages)
    }

    private fun loadMessagesForSender(mobile: String): List<SmsDetail> {
        val result = mutableListOf<SmsDetail>()
        if (mobile.isEmpty()) return result

        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("body", "date")
        val selection = "address = ?"
        val selectionArgs = arrayOf(mobile)

        val cursor = contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "date DESC"
        )

        cursor?.use {
            val bodyIdx = it.getColumnIndexOrThrow("body")
            val dateIdx = it.getColumnIndexOrThrow("date")
            while (it.moveToNext()) {
                val body = it.getString(bodyIdx)
                val date = it.getLong(dateIdx)
                result.add(SmsDetail(body, date))
            }
        }
        return result
    }

    class SmsDetailAdapter(private val items: List<SmsDetail>) :
        RecyclerView.Adapter<SmsDetailAdapter.SmsDetailViewHolder>() {

        class SmsDetailViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val smsBody: MaterialTextView = view.findViewById(R.id.smsDetailBody)
            val smsTime: MaterialTextView = view.findViewById(R.id.smsDetailTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmsDetailViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_sms_detail, parent, false)
            return SmsDetailViewHolder(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: SmsDetailViewHolder, position: Int) {
            val item = items[position]
            holder.smsBody.text = item.body

            val df = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            holder.smsTime.text = df.format(Date(item.date))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
