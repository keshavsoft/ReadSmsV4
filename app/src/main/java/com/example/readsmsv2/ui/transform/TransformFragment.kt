package com.example.readsmsv2.ui.transform

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.readsmsv2.databinding.FragmentTransformBinding
import com.example.readsmsv2.databinding.ItemTransformBinding
import android.net.Uri
import android.widget.Toast
import com.example.readsmsv2.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransformFragment : Fragment() {

    private var _binding: FragmentTransformBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransformBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val recyclerView = binding.recyclerviewTransform
        val smsAdapter = SmsAdapter()
        recyclerView.adapter = smsAdapter

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_SMS),
                PERMISSION_REQUEST_CODE
            )
        } else {
            loadSms(smsAdapter)
        }

        return root
    }

    private fun loadSms(adapter: SmsAdapter) {
        val groupedSms = getSmsMessages().map { (mobile, messages) ->
            val lastMessage = messages.firstOrNull()?.first ?: ""
            val lastTimestamp = messages.firstOrNull()?.second ?: 0L
            SmsGroup(
                mobile = mobile,
                lastMessage = lastMessage,
                lastTimestamp = lastTimestamp,
                messageCount = messages.size
            )
        }.sortedByDescending { it.lastTimestamp }

        if (groupedSms.isEmpty()) {
            Toast.makeText(requireContext(), "No SMS found", Toast.LENGTH_SHORT).show()
        }
        adapter.submitList(groupedSms)
    }

    // Returns a map of mobile -> list of message pairs (body, timestamp)
    private fun getSmsMessages(): Map<String, List<Pair<String, Long>>> {
        val smsMap = mutableMapOf<String, MutableList<Pair<String, Long>>>()
        val cursor = requireContext().contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("address", "body", "date"),
            null,
            null,
            "date DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val address = it.getString(it.getColumnIndexOrThrow("address"))
                val body = it.getString(it.getColumnIndexOrThrow("body"))
                val date = it.getLong(it.getColumnIndexOrThrow("date"))

                val list = smsMap.getOrPut(address) { mutableListOf() }
                list.add(body to date)
            }
        }
        return smsMap
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val recyclerView = binding.recyclerviewTransform
                val smsAdapter = SmsAdapter()
                recyclerView.adapter = smsAdapter
                loadSms(smsAdapter)
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Data class for grouped SMS
    data class SmsGroup(
        val mobile: String,
        val lastMessage: String,
        val lastTimestamp: Long,
        val messageCount: Int
    )

    // Adapter
    class SmsAdapter :
        ListAdapter<SmsGroup, SmsViewHolder>(object : DiffUtil.ItemCallback<SmsGroup>() {
            override fun areItemsTheSame(oldItem: SmsGroup, newItem: SmsGroup) = oldItem.mobile == newItem.mobile
            override fun areContentsTheSame(oldItem: SmsGroup, newItem: SmsGroup) = oldItem == newItem
        }) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmsViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transform, parent, false)
            return SmsViewHolder(view)
        }

        override fun onBindViewHolder(holder: SmsViewHolder, position: Int) {
            val smsGroup = getItem(position)

            val mobile = smsGroup.mobile
            val lastMessage = smsGroup.lastMessage
            val timestamp = smsGroup.lastTimestamp
            val messageCount = smsGroup.messageCount

            // format timestamp to "time ago"
            val time = if (timestamp != 0L) {
                val now = System.currentTimeMillis()
                val diff = now - timestamp
                val seconds = diff / 1000
                val minutes = seconds / 60
                val hours = minutes / 60
                val days = hours / 24

                when {
                    seconds < 60 -> "Just now"
                    minutes < 60 -> "$minutes min ago"
                    hours < 24 -> "$hours hrs ago"
                    days < 7 -> "$days day${if (days > 1) "s" else ""} ago"
                    else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
                }
            } else {
                ""
            }

            holder.mobileText.text = if (messageCount > 1) "$mobile ($messageCount)" else mobile
            holder.messageText.text = lastMessage
            holder.timeText.text = time
        }
    }

    class SmsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mobileText: TextView = itemView.findViewById(R.id.textMobile)
        val messageText: TextView = itemView.findViewById(R.id.textMessage)
        val timeText: TextView = itemView.findViewById(R.id.textTime)
    }
}
