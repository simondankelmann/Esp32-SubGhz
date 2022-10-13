#include "Arduino.h"
#include "BluetoothSerial.h"

BluetoothSerial SerialBT;

void setup()
{
    Serial.begin(1000000);
    SerialBT.begin("Esp32-SubGhz"); //Bluetooth device name
    Serial.println("The device started, now you can pair it with bluetooth!");
}
void loop()
{
    if (SerialBT.available()){ 
      Serial.write(SerialBT.read()); 
    }

    if(Serial.available()){
      SerialBT.write(Serial.read());
    }
}