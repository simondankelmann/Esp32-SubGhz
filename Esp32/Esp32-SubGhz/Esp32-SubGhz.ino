// SD FAT LIB
#include <SdFat.h>
SdFat SD;
#define MICRO_SD_IO 5
File flipperFile;
String fileToTransmit = "";

// CC1101 
#include <ELECHOUSE_CC1101_SRC_DRV.h>
float cc1101_mhz = 433.92;
#define CCGDO0 2 //GPIO2
#define CCGDO2 4 //GPIO4

// JSON SUPPORT
#include <ArduinoJson.h>
#include <ArduinoJson.hpp>
DynamicJsonDocument inputJson(1024);
DynamicJsonDocument outputJson(1024);

// BLUETOOTH INCLUDES
#include "Arduino.h"
#include "BluetoothSerial.h"
BluetoothSerial SerialBT;

// FUNCTION HEADERS
void sendSamples(int samples[], int samplesLenght);

void setup()
{
    Serial.begin(1000000);
    pinMode(CCGDO0,OUTPUT);

    initBluetooth();
    Serial.println("The device started, now you can pair it with bluetooth!");
    initCC1101();
    Serial.println("CC1101 Connection OK");
    initSdCard();
    Serial.println("SD Card initialized");
}

void initBluetooth(){
  SerialBT.begin("Esp32-SubGhz"); //Bluetooth device name
  SerialBT.register_callback(btCallback);
}

void initSdCard(){
  if (!SD.begin(MICRO_SD_IO, SPI_HALF_SPEED)) {
      SD.initErrorHalt();
      Serial.println("Card Mount Failed");
  }
}

void initCC1101(){
    ELECHOUSE_cc1101.setSpiPin(14, 12, 13, 15); // (SCK, MISO, MOSI, CSN); 
    ELECHOUSE_cc1101.Init();
    ELECHOUSE_cc1101.setGDO(CCGDO0, CCGDO2);
    ELECHOUSE_cc1101.setMHZ(cc1101_mhz);        // Here you can set your basic frequency. The lib calculates the frequency automatically (default = 433.92).The cc1101 can: 300-348 MHZ, 387-464MHZ and 779-928MHZ. Read More info from datasheet.
    ELECHOUSE_cc1101.SetTx();               // set Transmit on
    ELECHOUSE_cc1101.setModulation(2);      // set modulation mode. 0 = 2-FSK, 1 = GFSK, 2 = ASK/OOK, 3 = 4-FSK, 4 = MSK.
    ELECHOUSE_cc1101.setDRate(512);         // Set the Data Rate in kBaud. Value from 0.02 to 1621.83. Default is 99.97 kBaud!
    ELECHOUSE_cc1101.setPktFormat(3);       // Format of RX and TX data. 0 = Normal mode, use FIFOs for RX and TX. 
                                            // 1 = Synchronous serial mode, Data in on GDO0 and data out on either of the GDOx pins. 
                                            // 2 = Random TX mode; sends random data using PN9 generator. Used for test. Works as normal mode, setting 0 (00), in RX. 
                                            // 3 = Asynchronous serial mode, Data in on GDO0 and data out on either of the GDOx pins.
  
    if(!ELECHOUSE_cc1101.getCC1101()){       // Check the CC1101 Spi connection.
      Serial.println("CC1101 Connection Error");
    }
}

void btCallback(esp_spp_cb_event_t event, esp_spp_cb_param_t *param){
  if(event == ESP_SPP_SRV_OPEN_EVT){
    Serial.println("Bluetooth Client Connected!");
  }else if(event == ESP_SPP_DATA_IND_EVT){
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

  if(command == "ListDir"){
      const char* path = inputJson["Parameters"][0];
      writeSerialBT(listDirJson(path, 0));
  }

  if(command == "RunFlipperFile"){
      const char* path = inputJson["Parameters"][0];
      fileToTransmit = String(path);
  }

}

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

String listDirJson(const char * dirname, byte tabulation) {
  initSdCard();
  outputJson.clear();
  outputJson["Command"] = "ListDir";
  JsonArray directories = outputJson.createNestedArray("directories");
  JsonArray files = outputJson.createNestedArray("files");

  File aDirectory;
  aDirectory.open(dirname);

  File file;
  char fileName[256];

  aDirectory.rewind();

  while (file.openNext(&aDirectory, O_READ)) {
    if (!file.isHidden()) {

      file.getName(fileName, sizeof(fileName));

      for (uint8_t i = 0; i < tabulation; i++) Serial.write('\t');
      Serial.print(fileName);

      if (file.isDir()) {
        directories.add(String(fileName));
      } else {
        files.add(String(fileName));
      }
    }
    file.close();
  }

  String result;
  outputJson.garbageCollect();
  serializeJson(outputJson, result);
  return result;
}

void loop()
{
    if(fileToTransmit != ""){
      transmitFlipperFile(fileToTransmit.c_str(), false);
      fileToTransmit = "";
      transmitSuccessToBtSerial();
    }
}

void transmitSuccessToBtSerial(){
    outputJson.clear();
    outputJson["Command"] = "RunFlipperFile";
    outputJson.garbageCollect();
    String result;
    serializeJson(outputJson, result);
    writeSerialBT(result);
}

void handleFlipperCommandLine(String command, String value){
    if(command == "Frequency"){
      float frequency = value.toFloat() / 1000000;   
      Serial.print("Setting Frequency:");
      Serial.println(frequency);
      //ELECHOUSE_cc1101.setMHZ(frequency);
      cc1101_mhz = frequency;
    }
}


void transmitFlipperFile(const char * filename, bool transmit){
  if(transmit){
    // SETUP CC1101 AGAIN
    initCC1101();
  }
  
  initSdCard();
  flipperFile = SD.open(filename);

  if (!flipperFile) {
    Serial.println("The file cannot be opened");
  } else {
    // PARSE CONTENT CHAR BY CHAR
    String command = "";
    String value = "";

    int data;
    char dataChar;
    bool appendCommand = true;
    bool breakLoop = false;
    int samples[512];
    int currentSample = 0;    

    while ((data = flipperFile.read()) >= 0 && breakLoop == false) {
        dataChar = data;

        switch (dataChar) {
          case ':':
              appendCommand = false;
              break;
          case '\n':
              // REMOVE SPACES IN FRONT OF VALUE
              while(value.startsWith(" ")){
                value = value.substring(1);
              }

              Serial.println("DUMP:");
              Serial.println(command + " | " + value);

              if(transmit == false){
                // SETUP CC1101 PARAMETERS
                handleFlipperCommandLine(command, value);
              } else {
                // TRANSMIT ON PREVIOUSLY SETUP CC1101
                if(command == "RAW_Data" && transmit){
                  sendSamples(samples, 512);
                }
              }

              // GET READY FOR THE NEXT ROW
              appendCommand = true;
              command = "";
              value = "";
              currentSample = 0;
              memset(samples, 0, sizeof(samples));       
              break;
          default:
              if(appendCommand){
                command += String(dataChar);
              } else {
                value += String(dataChar);

                if(command == "RAW_Data"){
                  if(dataChar == ' '){
                    // REPLACE SPACES IN CURRENT SAMPLE
                    value.replace(" ","");
                    if(value != ""){
                      samples[currentSample] = value.toInt();
                      currentSample++;
                      value = "";
                    }
                    
                  }
                } 
              }
              break;
        }
    }

    flipperFile.close();

    if(transmit == false){
      // START TRANSMITTING THE DATA
      fileToTransmit = filename;
      transmitFlipperFile(filename, true);
    }
  }
}

void sendSamples(int samples[], int samplesLenght) {
      Serial.print("Transmitting ");
      Serial.print(samplesLenght);
      Serial.println(" Samples");

      int totalDelay = 0;
      unsigned long time;
      byte n = 0;

      for (int i=0; i < samplesLenght; i++) {
        // TRANSMIT
        n = 1;
        
        totalDelay = samples[i]+0;
        if(totalDelay < 0){
          // DONT TRANSMIT
          totalDelay = totalDelay * -1;
          n = 0;
        }

        digitalWrite(CCGDO0,n);
        
        //time = micros();
        //while(micros() < time+totalDelay);
        delayMicroseconds(totalDelay);
      }

      // STOP TRANSMITTING
      digitalWrite(CCGDO0,0);

      Serial.println("Transmission completed.");
  }