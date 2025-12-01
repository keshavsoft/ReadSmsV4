package com.example.readsmsv2.ui.reflow

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

class ReflowFragment : Fragment() {

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
        val smsMessages = getSmsMessages()
        if (smsMessages.isEmpty()) {
            Toast.makeText(requireContext(), "No SMS found", Toast.LENGTH_SHORT).show()
        }
        adapter.submitList(smsMessages)
    }

    private fun getSmsMessages(): List<String> {
        val smsList = mutableListOf<String>()
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
                smsList.add("$address:$body:$date")
            }
        }
        return smsList
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

    // Adapter class for displaying SMS messages
    class SmsAdapter :
        ListAdapter<String, SmsViewHolder>(object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
            override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        }) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmsViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transform, parent, false)
            return SmsViewHolder(view)
        }



        override fun onBindViewHolder(holder: SmsViewHolder, position: Int) {
            val sms = getItem(position)
            val parts = sms.split(":", limit = 3) // include timestamp

            val mobile = parts.getOrNull(0)?.trim() ?: "Unknown"
            val message = parts.getOrNull(1)?.trim() ?: ""
            val timestamp = parts.getOrNull(2)?.toLongOrNull() ?: 0L

            // format timestamp to readable time
            val time = if (timestamp != 0L) {
                val sdf = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault())
                sdf.format(Date(timestamp))
            } else {
                ""
            }

            holder.mobileText.text = mobile
            holder.messageText.text = message
            holder.timeText.text = time
            holder.imageView.visibility = View.GONE
        }

    }

    class SmsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image_view_item_transform)
        val mobileText: TextView = itemView.findViewById(R.id.textMobile)
        val messageText: TextView = itemView.findViewById(R.id.textMessage)
        val timeText: TextView = itemView.findViewById(R.id.textTime)
    }


}
