package de.simon.dankelmann.esp32_subghz.Services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi
import de.simon.dankelmann.esp32_subghz.PermissionCheck.PermissionCheck
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*
import kotlin.text.Charsets.US_ASCII


@RequiresApi(Build.VERSION_CODES.M)
class BluetoothService (context: Context, private val handler: Handler){
    private val _logTag = "BluetoothService"
    private var _context:Context = context
    private var _uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
    private var _bluetoothSocket:BluetoothSocket? = null

    private var bluetoothManager: BluetoothManager
    private var bluetoothAdapter: BluetoothAdapter? = null

    // BT SERIAL VARS
    var mmOutputStream: OutputStream? = null
    var mmInputStream: InputStream? = null
    var workerThread: Thread? = null
    lateinit var readBuffer: ByteArray
    var readBufferPosition = 0
    var counter = 0

    @Volatile
    var stopWorker = false

    init{
        bluetoothManager = _context.getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
    }

    fun startDiscovery(){
        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_SCAN)){
            if (bluetoothAdapter?.isDiscovering == false){
                Log.d(_logTag, "Starting Discovery")
                bluetoothAdapter?.startDiscovery()
            }
        }
    }

    fun stopDiscovery(){
        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_SCAN)){
            if (bluetoothAdapter?.isDiscovering == true){
                bluetoothAdapter?.cancelDiscovery()
            }
        }
    }

    fun createSocket(device:BluetoothDevice){
        if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT)){
            _bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(_uuid)
            if(_bluetoothSocket != null){
                _bluetoothSocket?.connect()
                mmOutputStream = _bluetoothSocket?.getOutputStream();
                mmInputStream = _bluetoothSocket?.getInputStream();

                beginListenForData()

                sendData()
            }
        }
    }

    @Throws(IOException::class)
    fun sendData() {
        var msg: String = "TESTDATA SENT"
        msg += "\n"
        mmOutputStream!!.write(msg.toByteArray())
        //myLabel.setText("Data Sent")
        Log.d(_logTag, "Data Sent")
    }

    fun beginListenForData() {
        val handler = Handler()
        val delimiter: Byte = 10 //This is the ASCII code for a newline character
        stopWorker = false
        readBufferPosition = 0
        readBuffer = ByteArray(1024)
        workerThread = Thread {
            while (!Thread.currentThread().isInterrupted && !stopWorker) {
                try {
                    val bytesAvailable: Int = mmInputStream!!.available()
                    if (bytesAvailable > 0) {
                        val packetBytes = ByteArray(bytesAvailable)
                        mmInputStream?.read(packetBytes)
                        for (i in 0 until bytesAvailable) {
                            val b = packetBytes[i]
                            if (b == delimiter) {
                                val encodedBytes = ByteArray(readBufferPosition)
                                System.arraycopy(
                                    readBuffer,
                                    0,
                                    encodedBytes,
                                    0,
                                    encodedBytes.size
                                )
                                val data = String(encodedBytes, US_ASCII)
                                readBufferPosition = 0
                                Log.d(_logTag, "RECEIVED: $data")
                                handler.post {
                                    //myLabel.setText(data)
                                }
                            } else {
                                var next = readBuffer.get(readBufferPosition++)
                                next = b
                            }
                        }
                    }
                } catch (ex: IOException) {
                    stopWorker = true
                }
            }
        }
        workerThread!!.start()
    }










    // GOOGLE EXAMPLE CODE
    val MESSAGE_READ: Int = 0
    val MESSAGE_WRITE: Int = 1
    val MESSAGE_TOAST: Int = 2

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    Log.d(_logTag, "Input stream was disconnected", e)
                    break
                }

                // Send the obtained bytes to the UI activity.
                val readMsg = handler.obtainMessage(
                    MESSAGE_READ, numBytes, -1,
                    mmBuffer)

                readMsg.sendToTarget()
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e(_logTag, "Error occurred when sending data", e)

                // Send a failure message back to the activity.
                val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
                val bundle = Bundle().apply {
                    putString("toast", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                handler.sendMessage(writeErrorMsg)
                return
            }

            // Share the sent message with the UI activity.
            val writtenMsg = handler.obtainMessage(
                MESSAGE_WRITE, -1, -1, mmBuffer)
            writtenMsg.sendToTarget()
        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(_logTag, "Could not close the connect socket", e)
            }
        }
    }

}