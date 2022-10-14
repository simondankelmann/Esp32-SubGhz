// JSON SUPPORT
#include <ArduinoJson.h>
#include <ArduinoJson.hpp>

// BLUETOOTH INCLUDES
#include "Arduino.h"
#include "BluetoothSerial.h"

// MICRO SD INCLUDES
#include "FS.h"
#include "SD.h"
#include "SPI.h"

// DEFINITIONS
#define MICRO_SD_IO 25

BluetoothSerial SerialBT;

void setup()
{
    Serial.begin(1000000);
    SerialBT.begin("Esp32-SubGhz"); //Bluetooth device name
    SerialBT.register_callback(btCallback);
    Serial.println("The device started, now you can pair it with bluetooth!");

    // MICRO SD CARD SETUP:
    if(!SD.begin(MICRO_SD_IO)){
      Serial.println("Card Mount Failed");
      return;
    }
}

void btCallback(esp_spp_cb_event_t event, esp_spp_cb_param_t *param){
  if(event == ESP_SPP_SRV_OPEN_EVT){
    Serial.println("Client Connected!");
  }else if(event == ESP_SPP_DATA_IND_EVT){
        //Serial.printf("ESP_SPP_DATA_IND_EVT len=%d, handle=%d data=%d,\n\n", param->data_ind.len, param->data_ind.handle, param->data_ind.data);
        String stringRead = readBluetoothSerialString();
        Serial.print("String read: ");
        Serial.println(stringRead);
        parseJsonCommand(stringRead);
    }
}

void parseJsonCommand(String json){
  StaticJsonDocument<256> doc;
  DeserializationError error = deserializeJson(doc, json);
  if (error) {
    Serial.print(F("deserializeJson() failed: "));
    Serial.println(error.f_str());
    return;
  }

  const char* commandPtr = doc["Command"];
  String command = String(commandPtr);

  /* DEBUGGING
  Serial.println("Command detected: ");
  Serial.println(command);
  int parameterSize = doc["Parameters"].size();
  Serial.println("Parameters detected: ");
  for (int i = 0; i < parameterSize; i++) {
    const char* parameterPtr = doc["Parameters"][i];
    String parameterValue = String(parameterPtr);
    Serial.println(parameterValue);
  }*/

  if(command == "ListDir"){
      const char* path = doc["Parameters"][0];
      //String path = String(parameterPtr);
      writeSerialBT(listDirJson(SD, path));
  }

}

#pragma region BluetoothSerial IO
void writeSerialBT(String message){
  SerialBT.println(message);
}

String readBluetoothSerialString(){
  String returnValue = "";
  while(SerialBT.available()){ 
    returnValue += SerialBT.readString();
  }
  return returnValue;
}
#pragma endregion

#pragma region MicroSdCode

  String listDirJson(fs::FS &fs, const char * dirname){
    StaticJsonDocument<256> doc;

    doc["Command"] = "ListDir";

    JsonArray directories = doc.createNestedArray("directories");
    JsonArray files = doc.createNestedArray("files");

    // GET FILES AND DIRECTORIES
    File root = fs.open(dirname);
    if(root && root.isDirectory()){
       File file = root.openNextFile();
        while(file){
          const char * filename = file.name();
          if(file.isDirectory()){
            if(filename[0] != '.'){
              directories.add(filename);
            }
          } else {
            files.add(filename);
          }
          file = root.openNextFile();
        }
    }


    String result;
    serializeJson(doc, result);
    //Serial.println("JSON RESULT: " + result);
    return result;
  }

#pragma endregion

void loop()
{

}