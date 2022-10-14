package de.simon.dankelmann.esp32_subghz.ui.remoteFileExplorer

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import de.simon.dankelmann.esp32_subghz.Adapters.BluetoothDeviceListviewAdapter
import de.simon.dankelmann.esp32_subghz.Adapters.RemoteFileExplorerListViewAdapter
import de.simon.dankelmann.esp32_subghz.PermissionCheck.PermissionCheck
import de.simon.dankelmann.esp32_subghz.Services.BluetoothSerial
import de.simon.dankelmann.esp32_subghz.Services.RemoteFileExplorer
import de.simon.dankelmann.esp32_subghz.databinding.FragmentRemoteFileExplorerBinding
import org.json.JSONObject

class RemoteFileExplorerFragment: Fragment() , AdapterView.OnItemClickListener{
    private val _logTag = "RmtFileExplorerFragment"
    private var _binding: FragmentRemoteFileExplorerBinding? = null
    private var _viewModel: RemoteFileExplorerViewModel? = null
    private var _bluetoothDevice: BluetoothDevice? = null
    private var _currentPath: String = "/"
    private var _remoteFileExplorer:RemoteFileExplorer? = null
    private var _listItemAdapter: RemoteFileExplorerListViewAdapter? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel = ViewModelProvider(this).get(RemoteFileExplorerViewModel::class.java)
        _viewModel = viewModel

        _binding = FragmentRemoteFileExplorerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // GET DATA FROM BUNDLE
        var deviceFromBundle = arguments?.getParcelable("Device") as BluetoothDevice?
        if(deviceFromBundle != null){
            if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT)){
                _bluetoothDevice = deviceFromBundle
                _remoteFileExplorer = RemoteFileExplorer(requireContext())
                _remoteFileExplorer?.connect(_bluetoothDevice?.address.toString(), ::receivedMessageCallback)
                _remoteFileExplorer?.listDirectoryContent(_currentPath)
            }
        }

        // SETUP LISTVIEW ADAPTER
        val listview: ListView = binding.fileExplorerListView
        listview.onItemClickListener = this
        _viewModel?.remoteFileExplorerEntries!!.observe(viewLifecycleOwner) {
            _listItemAdapter = RemoteFileExplorerListViewAdapter(root.context, it) //ArrayAdapter(root.context, android.R.layout.simple_list_item_1, it)
            listview.adapter = _listItemAdapter
        }

        /*
        val textView: TextView = binding.textRemoteFileExplorer
        viewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }*/

        return root
    }

    private fun receivedMessageCallback(message: String){
        var jsonObject = JSONObject(message)
        if(jsonObject != null){
            var command = jsonObject.getString("Command")
            if(command != null){
                when(command){
                    "ListDir" -> {
                        handleListDirResult(jsonObject)
                    }
                }
            }
        }
    }

    private fun handleListDirResult(result: JSONObject){
        _viewModel?.clearRemoteFileExplorerEntries()

        var directories = result.getJSONArray("directories")
        var files = result.getJSONArray("files")

        for (i in 0 until directories.length()) {
            val directory = directories.get(i).toString()
            Log.d(_logTag, "DIR: $directory")
            _viewModel?.addRemoteFileExplorerEntry(directory.toString(), _currentPath, true)
        }

        for (i in 0 until files.length()) {
            val file = files.get(i).toString()
            Log.d(_logTag, "FILE: $file")
            _viewModel?.addRemoteFileExplorerEntry(file.toString(), _currentPath, false)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        TODO("Not yet implemented")
    }
}