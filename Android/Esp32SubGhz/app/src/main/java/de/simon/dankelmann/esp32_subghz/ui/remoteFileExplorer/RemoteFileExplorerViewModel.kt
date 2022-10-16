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

    private val _statusLabel = MutableLiveData<String>().apply {
        value = "Idle"
    }

    private val _connectionLabel = MutableLiveData<String>().apply {
        value = "Disconnected"
    }

    private val _text = MutableLiveData<String>().apply {
        value = "This is remote file explorer Fragment"
    }

    private val _currentPath = MutableLiveData<String>().apply {
        value = "/"
    }

    fun updateCurrentPath(newPath:String){
        _currentPath.postValue(newPath)
    }

    fun updateStatusLabel(newText:String){
        _statusLabel.postValue(newText)
    }

    fun updateConnectionLabel(newText:String){
        _connectionLabel.postValue(newText)
    }

    fun addRemoteFileExplorerEntry(fileName:String, path: String, isDirectory:Boolean){
        var model = RemoteFileExplorerEntryModel(fileName, path, isDirectory)
        var alreadyAdded = false
        _remoteFileExplorerEntries.value!!.map {
            if(it.isDirectory == model.isDirectory && it.fileName == model.fileName && it.path == model.path){
                alreadyAdded = true
            }
        }
        if(alreadyAdded == false){
            _remoteFileExplorerEntries.value!!.add(model)
            _remoteFileExplorerEntries.postValue(_remoteFileExplorerEntries.value!!)
        }
        //_remoteFileExplorerEntries.value!!.add(model)
        /*
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
                _remoteFileExplorerEntries.postValue(_remoteFileExplorerEntries.value!!.plus(model).toMutableList()) //=  bluetoothDevices.value!!.plus(model).toMutableList()
                //_remoteFileExplorerEntries.value!!.add(model)
            }
        }*/
    }

    fun clearRemoteFileExplorerEntries(){
        Log.d(_logTag, "Clearing")
        remoteFileExplorerEntries.value!!.clear()
        /*
        remoteFileExplorerEntries.value!!.map {
            Log.d(_logTag, "CLEARING: " + it.fileName)
            remoteFileExplorerEntries.value!!.remove(it)
        }*/
    }

    fun getRemoteFileExplorerEntryModel(index: Int) : RemoteFileExplorerEntryModel?{
        if(this._remoteFileExplorerEntries.value != null){
            if(this._remoteFileExplorerEntries.value!!.count() >= index){
                return this._remoteFileExplorerEntries.value!![index]
            }
        }
        return null
    }

    var remoteFileExplorerEntries: LiveData<MutableList<RemoteFileExplorerEntryModel>> = _remoteFileExplorerEntries
    var text: LiveData<String> = _text
    var currentPath: LiveData<String> = _currentPath
    var statusLabel: LiveData<String> = _statusLabel
    var connectionLabel: LiveData<String> = _connectionLabel
}