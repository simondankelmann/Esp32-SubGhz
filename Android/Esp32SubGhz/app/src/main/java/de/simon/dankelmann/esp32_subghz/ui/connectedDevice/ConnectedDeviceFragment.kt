package de.simon.dankelmann.esp32_subghz.ui.connectedDevice

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import de.simon.dankelmann.esp32_subghz.PermissionCheck.PermissionCheck
import de.simon.dankelmann.esp32_subghz.R
import de.simon.dankelmann.esp32_subghz.Services.BluetoothSerial
import de.simon.dankelmann.esp32_subghz.Services.BluetoothService
import de.simon.dankelmann.esp32_subghz.databinding.FragmentConnectedDeviceBinding

class ConnectedDeviceFragment: Fragment() {
    private val _logTag = "ConnectedDeviceFragment"
    private var _binding: FragmentConnectedDeviceBinding? = null
    private var _viewModel: ConnectedDeviceViewModel? = null
    private var _bluetoothDevice: BluetoothDevice? = null
    //private var _bluetoothSerial: BluetoothSerial? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get(ConnectedDeviceViewModel::class.java)
        _viewModel = viewModel

        _binding = FragmentConnectedDeviceBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // GET DATA FROM BUNDLE
        var deviceFromBundle = arguments?.getParcelable("Device") as BluetoothDevice?
        if(deviceFromBundle != null){
            if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT)){
                _viewModel?.updateText(deviceFromBundle.name + " - " + deviceFromBundle.address)
                _bluetoothDevice = deviceFromBundle
                /*
                _bluetoothSerial = BluetoothSerial(requireContext())
                _bluetoothSerial?.connect(_bluetoothDevice?.address.toString(), ::receivedMessageCallback)
                */
            }
        }

        val btn: Button = binding.button
        btn.setOnClickListener { view ->
            // GO TO FILE EXPLORER FRAGMENT
            val bundle = Bundle()
            bundle.putString("DeviceAddress", _bluetoothDevice?.address)
            bundle.putParcelable("Device", _bluetoothDevice!!)
            requireActivity().findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_nav_connected_device_to_nav_remote_file_explorer, bundle)
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}