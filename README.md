📱 About the App
BlueChat Pro is a feature-rich Android chat application that combines Bluetooth-based messaging with SMS capabilities. The app enables seamless communication between nearby devices via Bluetooth while also providing traditional SMS functionality, creating a versatile communication platform that works both online and offline.

✨ Features
📡 Bluetooth Chat
Real-time messaging between paired Bluetooth devices

Automatic device discovery and pairing

Secure Bluetooth connections

Multi-device chat support

📨 SMS Integration
Send and receive traditional SMS messages

SMS to Bluetooth message conversion

Contact-based messaging

Message history and logging

💬 Chat Features
Real-time message delivery

Chat history with timestamps

User-friendly chat interface

Message notifications

Online/Offline status indicators

🔧 Technical Features
Background Bluetooth service

Auto-reconnect capabilities

Message encryption support

Low battery consumption

Network fallback options

🚀 Getting Started
Requirements
Android 5.0 (API 21) or higher

Bluetooth hardware (for Bluetooth chat)

SIM card (for SMS functionality)

Location permission for Bluetooth discovery

Installation
Download and install the APK file

Enable Bluetooth and grant necessary permissions

Allow location access for device discovery

Pair with nearby devices to start chatting

Basic Usage
Bluetooth Chat: Enable Bluetooth, discover devices, pair and chat

SMS Messaging: Access contacts, compose and send SMS

Hybrid Mode: Convert between Bluetooth and SMS as needed

🛠️ Technical Details
Platform: Android (Java/XML)
Minimum SDK: API 21 (Android 5.0)
Communication Protocols: Bluetooth RFCOMM, SMS
Database: SQLite for message storage
Permissions: Bluetooth, SMS, Contacts, Location, Storage
Architecture: Client-Server model over Bluetooth

📁 Project Structure
text
com.bluechat.pro/
├── MainActivity.java          # Main chat interface
├── BluetoothService.java      # Bluetooth connection handler
├── DeviceListActivity.java    # Bluetooth device discovery
├── ChatActivity.java          # Chat screen implementation
├── SMSHandler.java           # SMS sending/receiving
├── DatabaseHelper.java       # Local message storage
└── MessageAdapter.java       # Chat message display
🔐 Security Features
End-to-end encryption options

Secure Bluetooth pairing

Permission-based contact access

Private message storage

User authentication support

⚡ Performance
Low battery consumption

Fast Bluetooth connection setup

Efficient message queuing

Background service optimization

Minimal data usage

🔄 Compatibility
Tested On:

Android 5.0 to Android 13

Various Bluetooth versions (2.0 to 5.0)

Multiple device manufacturers

Different screen sizes and resolutions

⚠️ Important Notes
Bluetooth range is typically 10 meters

Both devices must have Bluetooth enabled

SMS functionality requires cellular network

Location permission is required for Bluetooth device discovery

Keep app in foreground for best Bluetooth performance

📄 License
This project is licensed under the MIT License - see the LICENSE file for details.

👨‍💻 Developer
Developed by Teshome Gebeyehu (Ozg)

🌐 Future Enhancements
Planned features for upcoming versions:

Group chat functionality

File transfer over Bluetooth

Voice messaging

Cloud backup integration

Custom themes and UI customization

