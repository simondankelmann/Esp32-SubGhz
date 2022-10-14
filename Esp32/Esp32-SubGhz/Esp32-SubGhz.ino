#include "Arduino.h"
#include "BluetoothSerial.h"

BluetoothSerial SerialBT;

//String Test = "Ü Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero";

void setup()
{
    Serial.begin(1000000);
    SerialBT.begin("Esp32-SubGhz"); //Bluetooth device name
    SerialBT.register_callback(btCallback);
    Serial.println("The device started, now you can pair it with bluetooth!");
}

void btCallback(esp_spp_cb_event_t event, esp_spp_cb_param_t *param){
  if(event == ESP_SPP_SRV_OPEN_EVT){
    Serial.println("Client Connected!");
  }else if(event == ESP_SPP_DATA_IND_EVT){
        //Serial.printf("ESP_SPP_DATA_IND_EVT len=%d, handle=%d data=%d,\n\n", param->data_ind.len, param->data_ind.handle, param->data_ind.data);
        String stringRead = readBluetoothSerialString();
        Serial.print("String read: ");
        Serial.println(stringRead);
        // SEND SOME ECHO
        writeSerialBT("ECHO: " + stringRead);
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

void loop()
{

}