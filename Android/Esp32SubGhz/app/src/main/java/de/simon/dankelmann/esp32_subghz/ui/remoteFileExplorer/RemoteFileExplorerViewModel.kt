package de.simon.dankelmann.esp32_subghz.ui.remoteFileExplorer

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.simon.dankelmann.esp32_subghz.Models.BluetoothDeviceModel
import de.simon.dankelmann.esp32_subghz.Models.RemoteFileExplorerEntryModel

class RemoteFileExplorerViewModel : ViewModel() {
    private var _logTag = "RemFilExpViewMod"

    private var _remoteFileExplorerEntries = MutableLiveData<MutableList<RemoteFileExplorerEntryModel>>().apply {
        value = mutableListOf()
    }

    private val _text = MutableLiveData<String>().apply {
        value = "This is remote file explorer Fragment"
    }

    fun addRemoteFileExplorerEntry(fileName:String, path: String, isDirectory:Boolean){
        var model = RemoteFileExplorerEntryModel(fileName, path, isDirectory)

        if(_remoteFileExplorerEntries.value == null) {
            Log.d(_logTag, "NEW LIST")
            _remoteFileExplorerEntries.value = mutableListOf(model)
        } else {
            var alreadyAdded = false
            var addedToIndex = -1
            _remoteFileExplorerEntries.value!!.forEachIndexed { index, it ->
                if (it.fileName == fileName && it.isDirectory == isDirectory && it.path == path) {
                    alreadyAdded = true
                    addedToIndex = index
                }
            }

            if(alreadyAdded){
                //_bluetoothDevices.value =  bluetoothDevices.value!!.toMutableList()
                Log.d(_logTag, "ALREADY ADDED")
                _remoteFileExplorerEntries.postValue(remoteFileExplorerEntries.value)
            } else {
                //_bluetoothDevices.value =  bluetoothDevices.value!!.plus(model).toMutableList()
                Log.d(_logTag, "NEWLY ADDED")
                _remoteFileExplorerEntries.postValue(remoteFileExplorerEntries.value!!.plus(model).toMutableList()) //=  bluetoothDevices.value!!.plus(model).toMutableList()
            }
        }
    }

    fun clearRemoteFileExplorerEntries(){
        _remoteFileExplorerEntries.postValue(mutableListOf())
    }

    fun getRemoteFileExplorerEntryModel(index: Int) : RemoteFileExplorerEntryModel?{
        if(this._remoteFileExplorerEntries.value != null){
            if(this._remoteFileExplorerEntries.value!!.count() >= index){
                return this._remoteFileExplorerEntries.value!![index]
            }
        }
        return null
    }

    val remoteFileExplorerEntries: LiveData<MutableList<RemoteFileExplorerEntryModel>> = _remoteFileExplorerEntries
    val text: LiveData<String> = _text
}