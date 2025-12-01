package com.example.readsmsv2.ui.transform

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.readsmsv2.R
import com.example.readsmsv2.databinding.FragmentTransformBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransformFragment : Fragment() {

    private var _binding: FragmentTransformBinding? = null
    private val binding get() = _binding!!

    // keep adapter as a field so we reuse it
    private lateinit var smsAdapter: SmsAdapter

    // modern permission request API
    private val requestSmsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("SMS_PERM", "READ_SMS granted? $isGranted")
        if (isGranted) {
            loadSms()
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransformBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val recyclerView = binding.recyclerviewTransform

        smsAdapter = SmsAdapter { smsGroup ->
            val intent = Intent(requireContext(), SmsDetailActivity::class.java)
            intent.putExtra("mobile", smsGroup.mobile)
            startActivity(intent)
        }
        recyclerView.adapter = smsAdapter

        // check permission first
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasSmsPermission) {
            loadSms()
        } else {
            requestSmsPermission.launch(Manifest.permission.READ_SMS)
        }

        return root
    }

    private fun loadSms() {
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
        smsAdapter.submitList(groupedSms)
    }

    // Returns a map of mobile -> list of message pairs (body, timestamp)
    private fun getSmsMessages(): Map<String, List<Pair<String, Long>>> {
        val smsMap = mutableMapOf<String, MutableList<Pair<String, Long>>>()

        try {
            val cursor = requireContext().contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date"),
                null,
                null,
                "date DESC"
            )

            cursor?.use {
                Log.d("SMS_TEST", "Inbox count = ${it.count}")
                while (it.moveToNext()) {
                    val address = it.getString(it.getColumnIndexOrThrow("address"))
                    val body = it.getString(it.getColumnIndexOrThrow("body"))
                    val date = it.getLong(it.getColumnIndexOrThrow("date"))

                    val list = smsMap.getOrPut(address) { mutableListOf() }
                    list.add(body to date)
                }
            }
        } catch (e: SecurityException) {
            // happens if OS still blocks SMS even with permission
            Log.e("SMS_TEST", "SecurityException when reading SMS: ${e.message}", e)
            Toast.makeText(requireContext(), "Cannot read SMS on this device", Toast.LENGTH_LONG)
                .show()
        } catch (e: Exception) {
            Log.e("SMS_TEST", "Unexpected error when reading SMS: ${e.message}", e)
        }

        return smsMap
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

    // Adapter with click handler
    class SmsAdapter(
        private val onItemClick: (SmsGroup) -> Unit
    ) : ListAdapter<SmsGroup, SmsViewHolder>(
        object : DiffUtil.ItemCallback<SmsGroup>() {
            override fun areItemsTheSame(oldItem: SmsGroup, newItem: SmsGroup) =
                oldItem.mobile == newItem.mobile

            override fun areContentsTheSame(oldItem: SmsGroup, newItem: SmsGroup) =
                oldItem == newItem
        }
    ) {

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

            holder.mobileText.text =
                if (messageCount > 1) "$mobile ($messageCount)" else mobile
            holder.messageText.text = lastMessage
            holder.timeText.text = time

            holder.itemView.setOnClickListener {
                onItemClick(smsGroup)
            }
        }
    }

    class SmsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mobileText: TextView = itemView.findViewById(R.id.textMobile)
        val messageText: TextView = itemView.findViewById(R.id.textMessage)
        val timeText: TextView = itemView.findViewById(R.id.textTime)
    }
}
