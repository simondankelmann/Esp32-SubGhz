package de.simon.dankelmann.esp32_subghz.Services

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlin.reflect.KFunction1

class RemoteFileExplorer (context: Context, connectionChangedCallback:KFunction1<Int, Unit>?) {
    private var _context:Context = context
    private val _logTag = "RemoteFileExplorer"
    @RequiresApi(Build.VERSION_CODES.M)
    private var _bluetoothSerial:BluetoothSerial? = null
    private var _isConnected = false
    private var _isBlocked = false
    private var _connectionChangedCallback = connectionChangedCallback

    @RequiresApi(Build.VERSION_CODES.M)
    fun connect(macAddress:String, receivedDataCallback: KFunction1<String, Unit>){
        _bluetoothSerial = BluetoothSerial(_context, _connectionChangedCallback)
        _bluetoothSerial?.connect(macAddress, receivedDataCallback)
        _isConnected = true
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun listDirectoryContent(path: String){
        if(_isConnected){
            var jsonString = "{\"Command\":\"ListDir\",\"Parameters\":[\"\\$path\"]}"
            _bluetoothSerial?.sendString(jsonString);
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun runFlipperFile(path: String){
        if(_isConnected){
            var jsonString = "{\"Command\":\"RunFlipperFile\",\"Parameters\":[\"\\$path\"]}"
            _bluetoothSerial?.sendString(jsonString);
        }
    }

}