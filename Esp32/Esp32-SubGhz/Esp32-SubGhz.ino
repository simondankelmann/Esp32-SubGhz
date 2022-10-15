// CC1101 
#include <ELECHOUSE_CC1101_SRC_DRV.h>

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

// RF CONSTANTS
#define ONBOARD_LED  2
#define CCGDO0 2 //GPIO2
#define CCGDO2 4 //GPIO4
#define RESET443 32000 //32ms

BluetoothSerial SerialBT;

DynamicJsonDocument inputJson(1024);
DynamicJsonDocument outputJson(1024);

// FUNCTION HEADERS
void sendSamples(int samples[], int samplesLenght);

void setup()
{
    Serial.begin(1000000);

    pinMode(ONBOARD_LED,OUTPUT);
    
    

    SerialBT.begin("Esp32-SubGhz"); //Bluetooth device name
    SerialBT.register_callback(btCallback);
    Serial.println("The device started, now you can pair it with bluetooth!");


    ELECHOUSE_cc1101.Init();
    ELECHOUSE_cc1101.setGDO(CCGDO0, CCGDO2);
    ELECHOUSE_cc1101.setMHZ(433.92);           // Here you can set your basic frequency. The lib calculates the frequency automatically (default = 433.92).The cc1101 can: 300-348 MHZ, 387-464MHZ and 779-928MHZ. Read More info from datasheet.
    ELECHOUSE_cc1101.SetTx();               // set Transmit on
    ELECHOUSE_cc1101.setModulation(2);      // set modulation mode. 0 = 2-FSK, 1 = GFSK, 2 = ASK/OOK, 3 = 4-FSK, 4 = MSK.
    ELECHOUSE_cc1101.setDRate(512);         // Set the Data Rate in kBaud. Value from 0.02 to 1621.83. Default is 99.97 kBaud!
    ELECHOUSE_cc1101.setPktFormat(3);       // Format of RX and TX data. 0 = Normal mode, use FIFOs for RX and TX. 
                                            // 1 = Synchronous serial mode, Data in on GDO0 and data out on either of the GDOx pins. 
                                            // 2 = Random TX mode; sends random data using PN9 generator. Used for test. Works as normal mode, setting 0 (00), in RX. 
                                            // 3 = Asynchronous serial mode, Data in on GDO0 and data out on either of the GDOx pins.
  
  
    if (ELECHOUSE_cc1101.getCC1101()){       // Check the CC1101 Spi connection.
      Serial.println("Connection OK");
    }else{
      Serial.println("Connection Error");
    }

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
  
  inputJson.clear();  
  DeserializationError error = deserializeJson(inputJson, json);
  if (error) {
    Serial.print(F("deserializeJson() failed: "));
    Serial.println(error.f_str());
    return;
  }

  const char* commandPtr = inputJson["Command"];
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
      const char* path = inputJson["Parameters"][0];
      //String path = String(parameterPtr);
      writeSerialBT(listDirJson(SD, path));
  }

  if(command == "RunFlipperFile"){
      const char* path = inputJson["Parameters"][0];
      //String path = String(parameterPtr);
      transmitFlipperFile(path);
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
    //DynamicJsonDocument doc(1024);
    outputJson.clear();

    outputJson["Command"] = "ListDir";

    JsonArray directories = outputJson.createNestedArray("directories");
    JsonArray files = outputJson.createNestedArray("files");

    // GET FILES AND DIRECTORIES
    File root = fs.open(dirname);
    if(root && root.isDirectory()){
       File file = root.openNextFile();
        while(file){
          if(file.isDirectory()){
            if(file.name()[0] != '.'){
              Serial.println("Adding Folder: ");
              Serial.println(file.name());
              directories.add(String(file.name()));
            }
          } else {
            Serial.println("Adding File: ");
            Serial.println(file.name());
            files.add(String(file.name()));
          }
          file = root.openNextFile();
        }
    }


    String result;
    outputJson.garbageCollect();
    serializeJson(outputJson, result);
    Serial.println("JSON RESULT: " + result);
    return result;
  }

#pragma endregion

void loop()
{

}

void transmitFlipperFile(const char * filename){
  File flipperFile = SD.open(filename);

  if (!flipperFile) {
    Serial.println("The file cannot be opened");
  } else {
    // PARSE CONTENT
    Serial.println("The file is opened");

    while (flipperFile.available()) {
      //String buffer = flipperFile.readStringUntil('\n');
      //parseFlipperFileLine(buffer);

      String command = flipperFile.readStringUntil(':');
      flipperFile.readStringUntil(' ');
      String value = flipperFile.readStringUntil('\n');

      parseFlipperFileLine(command, value);
    }

    flipperFile.close();
  }

}

void parseFlipperFileLine(String command, String value){

  Serial.println("Command:");
  Serial.println(command);

  Serial.println("Value:");
  Serial.println(value);

  if(command == "RAW_Data"){

    int samplesLength = 0;

    // GET SAMPLES LENGTH
    for(auto x : value)
    {
      if(x == ' '){
        samplesLength++;
      }
    }

    int samples[samplesLength];
    String singleValue = "";
    int counter = 0;

    for(auto x : value)
    {
      if(x == ' '){
        //Serial.println("parsed single value:");
        //Serial.println(singleValue);
        samples[counter] = singleValue.toInt();
        singleValue = "";
        counter++;
      } else {
        singleValue += String(x);
      }
    }

    //Serial.println("Samples Length: ");
    //Serial.println(samplesLength);

    sendSamples(samples, samplesLength);
    
  }
}

void sendSamples(int samples[], int samplesLenght) {
      Serial.print("Transmitting ");
      Serial.print(samplesLenght);
      Serial.println(" Samples");

      int totalDelay = 0;
      byte n = 0;

      for (int i=0; i < samplesLenght; i++) {
      
        digitalWrite(CCGDO0,n);
        totalDelay = samples[i]+0;
        if(totalDelay < 0){
          totalDelay = totalDelay * -1;
        }
        
        delayMicroseconds(totalDelay);

        if (samples[i] < RESET443) {
          n = !n;       
        }
        
      }
      digitalWrite(CCGDO0,0);

      Serial.println("Transmission completed.");
      //currentCommand = "---";
  }