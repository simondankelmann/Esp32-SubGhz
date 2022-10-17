package de.simon.dankelmann.esp32_subghz.ui.remoteFileExplorer

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.opengl.Visibility
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
import de.simon.dankelmann.esp32_subghz.R
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
    private var _isBlocked = false

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
                _remoteFileExplorer = RemoteFileExplorer(requireContext(), ::connectionStateChangedCallback)
                _remoteFileExplorer?.connect(_bluetoothDevice?.address.toString(), ::receivedMessageCallback)
                //_remoteFileExplorer?.listDirectoryContent(_currentPath)
                _viewModel?.updateConnectionLabel("Connected")
                changeDirectory(_currentPath)
            }
        }

        // SETUP LISTVIEW ADAPTER
        val listview: ListView = binding.fileExplorerListView
        listview.onItemClickListener = this
        _viewModel?.remoteFileExplorerEntries!!.observe(viewLifecycleOwner) {
            _listItemAdapter = RemoteFileExplorerListViewAdapter(root.context, it) //ArrayAdapter(root.context, android.R.layout.simple_list_item_1, it)
            listview.adapter = _listItemAdapter
        }

        val textView: TextView = binding.textViewCurrentPath
        viewModel.currentPath.observe(viewLifecycleOwner) {
            textView.text = it
        }

        val textViewStatus: TextView = binding.textviewStatus
        viewModel.statusLabel.observe(viewLifecycleOwner) {
            textViewStatus.text = it
        }

        val textViewConnection: TextView = binding.textviewConnectionStatus
        viewModel.connectionLabel.observe(viewLifecycleOwner) {
            textViewConnection.text = it
        }

        return root
    }

    private fun connectionStateChangedCallback(connectionState: Int){
        Log.d(_logTag, "Connection Callback: " + connectionState)
        when(connectionState){
            0 -> {
                _viewModel?.updateStatusLabel("Disconnected")
            }
            1 -> {
                _viewModel?.updateStatusLabel("Connecting...")
            }
            2 -> {
                _viewModel?.updateStatusLabel("Connected")
            }
        }
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
                    "RunFlipperFile" -> {
                        handleRunFlipperFileResult(jsonObject)
                    }
                }
            }
        }
    }

    private fun handleRunFlipperFileResult(result: JSONObject){
        _isBlocked = false
        _viewModel?.updateStatusLabel("Transmittion completed")
        unblockUi()
    }

    private fun handleListDirResult(result: JSONObject){
        // ADD BACK BUTTON
        if(_currentPath != "/"){
            var parentPath = _currentPath.substring(0, _currentPath.lastIndexOf('/')+1)

            if(parentPath != "/" && parentPath.endsWith("/")){
                parentPath = parentPath.substring(0, parentPath.length - 1)
            }

            _viewModel?.addRemoteFileExplorerEntry("Up...", parentPath, true)
        }

        var directories = result.getJSONArray("directories")
        var files = result.getJSONArray("files")

        for (i in 0 until directories.length()) {
            val directory = directories.get(i).toString()
            Log.d(_logTag, "DIR: $directory")

            var fullPath = _currentPath + "/" + directory
            if(_currentPath.endsWith("/")){
                fullPath = _currentPath + directory
            }

            _viewModel?.addRemoteFileExplorerEntry(directory, fullPath, true)
        }

        for (i in 0 until files.length()) {
            val file = files.get(i).toString()
            Log.d(_logTag, "FILE: $file")

            var fullPath = _currentPath + "/" + file
            if(_currentPath.endsWith("/")){
                fullPath = _currentPath + file
            }

            _viewModel?.addRemoteFileExplorerEntry(file, fullPath, false)
        }

        _viewModel?.updateStatusLabel("Directory loaded")
        _isBlocked = false
        unblockUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        if(!_isBlocked){
            _isBlocked = true
            var selectedEntry = _viewModel?.getRemoteFileExplorerEntryModel(p2)

            if(selectedEntry!!.isDirectory){
                // LOAD SUBDIRECTORY
                changeDirectory(selectedEntry.path)
            } else {
                // RUN FILE
                runFlipperFile(selectedEntry.path)
            }
        } else {
            Log.e(_logTag, "Please wait...")
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun changeDirectory(path: String){
        blockUi(R.raw.dots)
        if(_remoteFileExplorer != null){
            _viewModel?.updateStatusLabel("Loading")
            _viewModel?.clearRemoteFileExplorerEntries()
            _remoteFileExplorer?.listDirectoryContent(path);
            _currentPath = path
            _viewModel!!.updateCurrentPath(path)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun runFlipperFile(path: String){
        blockUi(R.raw.sinus)
        _viewModel?.updateStatusLabel("Transmitting")
        if(_remoteFileExplorer != null){
            _remoteFileExplorer?.runFlipperFile(path);
            _currentPath = path
        }
    }

    fun blockUi(resourceId: Int = 0){
        activity?.runOnUiThread {
            if(resourceId > 0){
                _binding?.blockingAnimation!!.setAnimation(resourceId)
            }
            _binding?.blockingAnimation!!.visibility = View.VISIBLE
            _binding?.blockingAnimation!!.bringToFront()
        }
    }

    fun unblockUi(){
        activity?.runOnUiThread {
            _binding?.blockingAnimation!!.visibility = View.GONE
        }
    }
}