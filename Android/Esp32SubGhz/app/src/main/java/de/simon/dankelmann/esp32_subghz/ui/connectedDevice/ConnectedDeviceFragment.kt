package de.simon.dankelmann.esp32_subghz.ui.connectedDevice

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import de.simon.dankelmann.esp32_subghz.PermissionCheck.PermissionCheck
import de.simon.dankelmann.esp32_subghz.Services.BluetoothService
import de.simon.dankelmann.esp32_subghz.databinding.FragmentConnectedDeviceBinding

class ConnectedDeviceFragment: Fragment() {

    private var _binding: FragmentConnectedDeviceBinding? = null
    private var _viewModel: ConnectedDeviceViewModel? = null
    private var _bluetoothDevice: BluetoothDevice? = null
    private var _bluetoothService: BluetoothService? = null

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

        _bluetoothService = BluetoothService(requireContext(), bluetoothHandler())

        // GET DATA FROM BUNDLE
        var deviceFromBundle = arguments?.getParcelable("Device") as BluetoothDevice?
        if(deviceFromBundle != null){
            if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT)){
                _viewModel?.updateText(deviceFromBundle.name + " - " + deviceFromBundle.address)
                _bluetoothDevice = deviceFromBundle

                class SimpleThread: Thread() {
                    public override fun run() {
                        _bluetoothService?.createSocket(_bluetoothDevice!!)
                    }
                }

                var t = SimpleThread()
                t.run()

            }
        }

        val textView: TextView = binding.textConnectedDevice
        viewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        return root
    }

    private inner class bluetoothHandler : Handler(){

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}