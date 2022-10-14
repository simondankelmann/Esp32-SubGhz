package de.simon.dankelmann.esp32_subghz.Adapters

import android.Manifest
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import de.simon.dankelmann.esp32_subghz.Models.BluetoothDeviceModel
import de.simon.dankelmann.esp32_subghz.Models.RemoteFileExplorerEntryModel
import de.simon.dankelmann.esp32_subghz.PermissionCheck.PermissionCheck
import de.simon.dankelmann.esp32_subghz.R

class RemoteFileExplorerListViewAdapter(private val context: Context, private var entryList: MutableList<RemoteFileExplorerEntryModel>) : BaseAdapter() {
    private lateinit var entryFileName: TextView
    private lateinit var entryIcon: ImageView

    override fun getCount(): Int {
        return entryList.size
    }

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun getRemoteFileExplorerEntryModel(position: Int):RemoteFileExplorerEntryModel?{
        if(entryList.size >= position){
            return entryList[position]
        }
        return null
    }

    fun removeRemoteFileExplorerEntryModel(itemIndex:Int){
        entryList.removeAt(itemIndex)
        //notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var convertView = convertView
        convertView = LayoutInflater.from(context).inflate(R.layout.listrow_remote_file_explorer_entry, parent, false)

        entryFileName = convertView!!.findViewById(R.id.fileFolderName)
        entryIcon = convertView.findViewById(R.id.folderFileIcon)

        entryFileName.text = entryList[position].fileName
        if(entryList[position].isDirectory){
            entryIcon.setImageDrawable(getDrawable(context, R.drawable.ic_folder_24))
        } else {
            entryIcon.setImageDrawable(getDrawable(context, R.drawable.ic_file_24))
        }

        return convertView
    }
}