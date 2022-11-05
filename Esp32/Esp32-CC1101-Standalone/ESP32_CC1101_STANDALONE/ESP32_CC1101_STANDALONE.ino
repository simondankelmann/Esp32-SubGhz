#include <ELECHOUSE_CC1101_SRC_DRV.h>

// M1101
#define PIN_GDO0 12
#define PIN_GDO2 4

// BUTTONS
#define THRESHOLD_BTN_CLICK 4000
#define PIN_BTN_1 32
#define PIN_BTN_2 25
#define PIN_BTN_3 27
#define PIN_BTN_4 14

// LED
#define PIN_LED_RX 33
#define PIN_LED_TX 26
#define PIN_LED_ONBOARD 2

#define LENGTH_SAMPLES_SIGNAL_1 5
#define LENGTH_SAMPLES_SIGNAL_2 5
#define LENGTH_SAMPLES_SIGNAL_3 5
#define LENGTH_SAMPLES_SIGNAL_4 5

int samples_signal_1[LENGTH_SAMPLES_SIGNAL_1] = {250,-250,250,-250,250};
int samples_signal_2[LENGTH_SAMPLES_SIGNAL_1] = {250,-250,250,-250,250};
int samples_signal_3[LENGTH_SAMPLES_SIGNAL_1] = {250,-250,250,-250,250};
int samples_signal_4[LENGTH_SAMPLES_SIGNAL_1] = {250,-250,250,-250,250};

void setup() {
  Serial.begin(9600);

  pinMode(PIN_LED_ONBOARD, OUTPUT);
  pinMode(PIN_LED_RX, OUTPUT);
  pinMode(PIN_LED_TX, OUTPUT);

  pinMode(PIN_BTN_1, INPUT);
  pinMode(PIN_BTN_2, INPUT);
  pinMode(PIN_BTN_3, INPUT);
  pinMode(PIN_BTN_4, INPUT);

  //CC1101 SETUP
  initCC1101(433.92);
  
  if (ELECHOUSE_cc1101.getCC1101()){      
    // Check the CC1101 Spi connection.
    Serial.println("Connection OK");
  }

  Serial.println("Setup done.");
}

void initCC1101(float mhz){
    ELECHOUSE_cc1101.Init();
    ELECHOUSE_cc1101.setGDO(PIN_GDO0, PIN_GDO2);
    ELECHOUSE_cc1101.setMHZ(mhz);        // Here you can set your basic frequency. The lib calculates the frequency automatically (default = 433.92).The cc1101 can: 300-348 MHZ, 387-464MHZ and 779-928MHZ. Read More info from datasheet.
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

void loop() {
  // READ BTN STATES
  int state_btn_1 = analogRead(PIN_BTN_1);
  int state_btn_2 = analogRead(PIN_BTN_2);
  int state_btn_3 = analogRead(PIN_BTN_3);
  int state_btn_4 = analogRead(PIN_BTN_4);

  // BUTTON 1  
  if(state_btn_1 >= THRESHOLD_BTN_CLICK){
    while(analogRead(PIN_BTN_1) >= THRESHOLD_BTN_CLICK) {
      delay(10);
    }
    //BTN 1 CLICKED
    sendSamples(samples_signal_1, LENGTH_SAMPLES_SIGNAL_1, 433.92);
  }

  // BUTTON 2
  if(state_btn_2 >= THRESHOLD_BTN_CLICK){
    while(analogRead(PIN_BTN_2) >= THRESHOLD_BTN_CLICK) {
      delay(10);
    }
    //BTN 2 CLICKED
    sendSamples(samples_signal_2, LENGTH_SAMPLES_SIGNAL_2, 315);
  }
  
  // BUTTON 3
  if(state_btn_3 >= THRESHOLD_BTN_CLICK){
    while(analogRead(PIN_BTN_3) >= THRESHOLD_BTN_CLICK) {
      delay(10);
    }
    //BTN 3 CLICKED
    //sendSamples(samples_signal_3, LENGTH_SAMPLES_SIGNAL_3, 433.92);
  }

  // BUTTON 4
  if(state_btn_4 >= THRESHOLD_BTN_CLICK){
    while(analogRead(PIN_BTN_4) >= THRESHOLD_BTN_CLICK) {
      delay(10);
    }
    //BTN 4 CLICKED
    //sendSamples(samples_signal_4, LENGTH_SAMPLES_SIGNAL_4, 433.92);
  }
}

void sendSamples(int samples[], int samplesLenght, float mhz) {
  initCC1101(mhz);
  digitalWrite(PIN_LED_TX, HIGH);
  Serial.print("Transmitting ");
  Serial.print(samplesLenght);
  Serial.println(" Samples");

  int delay = 0;
  unsigned long time;
  byte n = 0;

  for (int i=0; i < samplesLenght; i++) {
    // TRANSMIT
    n = 1;
    delay = samples[i];
    if(delay < 0){
      // DONT TRANSMIT
      delay = delay * -1;
      n = 0;
    }

    digitalWrite(PIN_GDO0,n);
    
    delayMicroseconds(delay);
  }

  // STOP TRANSMITTING
  digitalWrite(PIN_GDO0,0);

  Serial.println("Transmission completed.");
  digitalWrite(PIN_LED_TX, LOW);
}