package de.simon.dankelmann.esp32_subghz.Services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.net.MacAddress
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import de.simon.dankelmann.esp32_subghz.PermissionCheck.PermissionCheck
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.reflect.KFunction1

@RequiresApi(Build.VERSION_CODES.M)
class BluetoothSerial (context: Context){
    //PRIVATE VAR
    private val _logTag = "BluetoothSerial"
    private var _context:Context = context
    private var _bluetoothManager: BluetoothManager
    private var _bluetoothAdapter: BluetoothAdapter? = null
    private var _bluetoothDevice: BluetoothDevice? = null
    private var _bluetoothSocket: BluetoothSocket? = null
    private var _bluetoothSocketInputStream: InputStream? = null
    private var _bluetoothSocketOutputStream: OutputStream? = null
    private var _bluetoothSerialUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
    private var _inputReaderThread:Thread? = null

    // PUBLIC VAR
    var isConnected = false

    init{
        _bluetoothManager = _context.getSystemService(BluetoothManager::class.java)
        _bluetoothAdapter = _bluetoothManager.adapter
    }

    fun connect(macAddress: String, receivedDataCallback: KFunction1<String, Unit>){
        if(_bluetoothAdapter != null){
            _bluetoothDevice = _bluetoothAdapter?.getRemoteDevice(macAddress)
            if(_bluetoothDevice != null){
                if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT)){

                    _bluetoothSocket = _bluetoothDevice?.createInsecureRfcommSocketToServiceRecord(_bluetoothSerialUuid)
                    if(_bluetoothSocket != null){
                        _bluetoothSocket?.connect()
                        _bluetoothSocketOutputStream = _bluetoothSocket?.getOutputStream()
                        _bluetoothSocketInputStream = _bluetoothSocket?.getInputStream()
                        isConnected = true
                        // BEGIN LISTENING
                        beginListeningOnInputStream(receivedDataCallback)
                    }

                    /*
                    _bluetoothSocket = _bluetoothDevice?.createInsecureL2capChannel(5)
                    if(_bluetoothSocket != null){
                        _bluetoothSocket?.connect()
                        _bluetoothSocketOutputStream = _bluetoothSocket?.getOutputStream()
                        _bluetoothSocketInputStream = _bluetoothSocket?.getInputStream()
                        isConnected = true
                        // BEGIN LISTENING
                        beginListeningOnInputStream(receivedDataCallback)
                    }*/
                }
            }
        }
    }

    fun disconnect(){
        stopListeningOnInputStream()
        isConnected = false
    }

    fun sendString(message:String) {
        if(isConnected && _bluetoothSocketOutputStream != null){
            Thread(Runnable {
                _bluetoothSocketOutputStream!!.write(message.toByteArray())
            }).start()
        }
    }

    private fun beginListeningOnInputStream(receivedDataCallback: (input: String) -> Unit){
        _inputReaderThread = Thread(Runnable {
            val delimiter: Byte = 10 //This is the ASCII code for a newline character
            var receivedBytes: MutableList<Byte>  = mutableListOf()
            while (!Thread.currentThread().isInterrupted && isConnected){
                try{
                    val bytesAvailable: Int = _bluetoothSocketInputStream!!.available()
                    if (bytesAvailable > 0) {
                        val packetBytes = ByteArray(bytesAvailable)
                        _bluetoothSocketInputStream?.read(packetBytes)
                        for (i in 0 until bytesAvailable) {
                            val b = packetBytes[i]
                            if (b == delimiter) {
                                val data = String(receivedBytes.toByteArray(), Charsets.UTF_8)
                                Log.d(_logTag, "RECEIVED: $data")
                                // PASS RECEIVED DATA TO CALLBACK
                                receivedDataCallback(data)
                                receivedBytes= mutableListOf()
                            } else {
                                // CONTINUE READING TO BUFFER
                                receivedBytes += b
                            }
                        }
                    }
                } catch (ex: IOException) {
                    Log.e(_logTag, ex.message.toString())
                    isConnected = false
                }
            }
        })
        _inputReaderThread?.start()
    }

    private fun stopListeningOnInputStream(){
        if(_inputReaderThread != null){
            _inputReaderThread?.stop()
        }
    }
}