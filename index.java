#include <FirebaseESP8266.h>
#include <ESP8266WiFi.h>
#include <Servo.h>

// Replace these with your Firebase and Wi-Fi credentials
#define FIREBASE_HOST "rainboot-default-rtdb.firebaseio.com/"  // Firebase Realtime Database URL
#define FIREBASE_AUTH "AaKfkcdWVdkH3OZGREjJqiqZLlfWNP5YxWOa46rw"  // Firebase database secret
#define WIFI_SSID "V2027"                          // Wi-Fi SSID
#define WIFI_PASSWORD "muneeb786"             // Wi-Fi Password

#define BUZZER_PIN D5

// Initialize Firebase and Wi-Fi
FirebaseData firebaseData;

// Define the rain sensor and servo pins
const int rainSensorPin = D1;  // Rain sensor pin
Servo windowServo;            // Servo object
const int servoPin = D2;      // Servo motor pin

// Flag to track the rain state
bool isRaining = false;

void setup() {
  Serial.begin(115200);
  pinMode(rainSensorPin, INPUT);
  pinMode(BUZZER_PIN, OUTPUT);

  // Attach the servo to the designated pin
  windowServo.attach(servoPin);
  windowServo.write(0);  // Initialize servo to open position (0 degrees)

  // Connect to Wi-Fi
  Serial.print("Connecting to Wi-Fi");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("Connected!");

  // Initialize Firebase connection
  Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
  Firebase.reconnectWiFi(true);

  Serial.println("Firebase initialized.");
}

void loop() {
  int rainStatus = digitalRead(rainSensorPin);  // Read rain sensor data (1 = no rain, 0 = rain)

  if (rainStatus == LOW && !isRaining) {
    // Rain detected for the first time
    isRaining = true;

    if (Firebase.setInt(firebaseData, "rain_status", 1)) {
      Serial.println("Rain detected. Updated status to 1 on Firebase.");

      // Turn the buzzer ON for 2 seconds
      digitalWrite(BUZZER_PIN, HIGH);
      delay(2000);
      digitalWrite(BUZZER_PIN, LOW);

      // Rotate the servo to 180 degrees
      windowServo.write(180);
      Serial.println("Window is closing.");
    } else {
      Serial.print("Firebase update failed: ");
      Serial.println(firebaseData.errorReason());
    }
  } else if (rainStatus == HIGH && isRaining) {
    // Rain stopped
    isRaining = false;

    if (Firebase.setInt(firebaseData, "rain_status", 0)) {
      Serial.println("No rain. Updated status to 0 on Firebase.");

      // Ensure the buzzer is off and reset the servo position
      digitalWrite(BUZZER_PIN, LOW);
      windowServo.write(0);
      Serial.println("Window is opening.");
    } else {
      Serial.print("Firebase update failed: ");
      Serial.println(firebaseData.errorReason());
    }
  }

  delay(1000);  // Small delay for stability
}