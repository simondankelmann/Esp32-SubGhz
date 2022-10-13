package de.simon.dankelmann.esp32_subghz.Services

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import de.simon.dankelmann.esp32_subghz.AppContext.AppContext
import de.simon.dankelmann.esp32_subghz.PermissionCheck.PermissionCheck

@RequiresApi(Build.VERSION_CODES.M)
class BluetoothService (context: Context){
    private val _logTag = "BluetoothService"
    private var _context:Context = context

    private var bluetoothManager: BluetoothManager
    private var bluetoothAdapter: BluetoothAdapter? = null

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

}