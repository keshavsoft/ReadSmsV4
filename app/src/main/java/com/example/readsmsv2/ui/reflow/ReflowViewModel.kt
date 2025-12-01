package com.example.readsmsv2.ui.transform

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ReflowViewModel : ViewModel() {

    private val _texts = MutableLiveData<List<String>>().apply {
        value = (1..2).mapIndexed { _, i ->
            "This is item # $i"
        }
    }

    val texts: LiveData<List<String>> = _texts
}