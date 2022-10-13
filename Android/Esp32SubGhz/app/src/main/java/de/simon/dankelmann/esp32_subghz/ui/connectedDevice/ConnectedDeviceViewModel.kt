package de.simon.dankelmann.esp32_subghz.ui.connectedDevice

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ConnectedDeviceViewModel: ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "This is connected Device Fragment"
    }

    fun updateText(text:String){
        _text.postValue(text)
    }

    val text: LiveData<String> = _text
}