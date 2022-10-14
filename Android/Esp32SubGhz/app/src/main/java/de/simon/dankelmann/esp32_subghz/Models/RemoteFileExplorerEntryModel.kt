package de.simon.dankelmann.esp32_subghz.Models

class RemoteFileExplorerEntryModel (name:String, directoryPath: String, isFolder: Boolean ) {
    var fileName = name
    var path = directoryPath
    var isDirectory = isFolder
}