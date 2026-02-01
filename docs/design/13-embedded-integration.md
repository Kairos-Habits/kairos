# Embedded Device Integration

## Overview

Kairos supports integration with ESP32-based devices via MQTT for physical habit interactions—completion buttons, NFC triggers, status lights, and more. This enables ambient habit tracking without touching a phone.

---

## Use Cases

```mermaid
mindmap
  root((Embedded Devices))
    Completion Button
      Physical button press
      Instant habit completion
      No phone needed
    NFC Tag
      Tap tag to complete
      Location-based triggers
      Medication bottle tag
    Status Light
      LED shows habits remaining
      Color indicates status
      Ambient awareness
    Smart Mirror Display
      Morning routine view
      Today's habits
      Progress indicator
```

### Why Physical Devices?

| ADHD Challenge | Physical Solution |
|----------------|-------------------|
| Phone is distracting | Button doesn't have apps |
| Forgot to log | Button at habit location |
| Visual reminder needed | Status light always visible |
| Context switching | NFC at trigger location |

---

## Architecture

```mermaid
flowchart TB
    subgraph Devices["ESP32 Devices"]
        Button["🔘 Completion Button"]
        NFC["📡 NFC Reader"]
        LED["💡 Status Light"]
    end
    
    subgraph Server["Kairos Server (Docker)"]
        Mosquitto["Mosquitto<br/>MQTT Broker"]
        Bridge["MQTT Bridge<br/>(Python/Go)"]
        Postgres[(Postgres)]
        PowerSync["PowerSync"]
    end
    
    subgraph Clients["Mobile Clients"]
        Phone["📱 Phone"]
        Watch["⌚ Watch"]
    end
    
    Button -->|"MQTT publish"| Mosquitto
    NFC -->|"MQTT publish"| Mosquitto
    Mosquitto -->|"MQTT subscribe"| LED
    
    Mosquitto <--> Bridge
    Bridge <--> Postgres
    Postgres <--> PowerSync
    
    PowerSync <--> Phone
    Phone <--> Watch
```

### Data Flow: Completion Button

```mermaid
sequenceDiagram
    participant Button as ESP32 Button
    participant MQTT as Mosquitto
    participant Bridge as MQTT Bridge
    participant DB as Postgres
    participant Sync as PowerSync
    participant Phone as Phone App
    
    Button->>MQTT: PUBLISH kairos/complete/habit-123
    MQTT->>Bridge: Message received
    Bridge->>Bridge: Validate device token
    Bridge->>DB: INSERT completion
    DB-->>Bridge: Success
    Bridge->>MQTT: PUBLISH kairos/status/habit-123 "completed"
    
    Note over Sync,Phone: PowerSync detects change
    Sync->>Phone: Sync new completion
    Phone->>Phone: Update UI
    
    MQTT->>Button: (Optional) ACK via status topic
    Button->>Button: LED flash confirmation
```

### Data Flow: Status Light

```mermaid
sequenceDiagram
    participant Phone as Phone App
    participant Sync as PowerSync
    participant DB as Postgres
    participant Bridge as MQTT Bridge
    participant MQTT as Mosquitto
    participant LED as ESP32 Status Light
    
    Phone->>Sync: Complete habit
    Sync->>DB: Write completion
    
    Bridge->>DB: Poll for changes (or trigger)
    DB-->>Bridge: Habits remaining: 2
    Bridge->>MQTT: PUBLISH kairos/status/summary
    Note over MQTT: {"remaining": 2, "total": 5}
    
    MQTT->>LED: Deliver message
    LED->>LED: Update display
    Note over LED: 2 LEDs lit = 2 remaining
```

---

## MQTT Topic Structure

### Topic Naming Convention

```
kairos/{action}/{entity_type}/{entity_id}
```

### Topics Reference

| Topic Pattern | Direction | Payload | Description |
|---------------|-----------|---------|-------------|
| `kairos/complete/{habit_id}` | Device → Server | `{"device_id": "...", "token": "..."}` | Complete a habit |
| `kairos/skip/{habit_id}` | Device → Server | `{"device_id": "...", "reason": "..."}` | Skip a habit |
| `kairos/status/{habit_id}` | Server → Device | `{"status": "pending\|completed\|skipped"}` | Single habit status |
| `kairos/status/summary` | Server → Device | `{"remaining": 3, "total": 5, "habits": [...]}` | All habits summary |
| `kairos/routine/start/{routine_id}` | Device → Server | `{"device_id": "..."}` | Start a routine |
| `kairos/routine/step` | Device → Server | `{"action": "done\|skip"}` | Routine step action |
| `kairos/routine/status` | Server → Device | `{"step": 2, "total": 4, "habit": "..."}` | Current routine state |
| `kairos/device/ping/{device_id}` | Device → Server | `{}` | Heartbeat |
| `kairos/device/pong/{device_id}` | Server → Device | `{"timestamp": ...}` | Heartbeat response |

### Payload Schemas

#### Complete Habit

```json
{
  "device_id": "button-kitchen-01",
  "token": "device-auth-token",
  "timestamp": "2024-01-20T07:30:00Z",
  "type": "FULL"
}
```

#### Status Summary (Server → Device)

```json
{
  "user_id": "user-123",
  "remaining": 3,
  "completed": 2,
  "total": 5,
  "habits": [
    {"id": "habit-1", "name": "Take medication", "status": "completed"},
    {"id": "habit-2", "name": "Drink water", "status": "pending"},
    {"id": "habit-3", "name": "Stretch", "status": "pending"}
  ],
  "updated_at": "2024-01-20T07:30:00Z"
}
```

---

## Device Types

### 1. Completion Button

Single-purpose device for completing one habit.

```
┌─────────────────────────────────────┐
│                                     │
│         [   BUTTON   ]              │
│                                     │
│            (●) LED                  │
│                                     │
│  ESP32 + Button + Status LED        │
│  WiFi connected                     │
│  Battery or USB powered             │
│                                     │
└─────────────────────────────────────┘
```

**Behavior**:
- Press → Publishes completion
- LED blinks → Sending
- LED solid → Confirmed
- LED off → Idle (or shows pending status)

**Configuration** (via web portal or hardcoded):
- WiFi credentials
- MQTT broker address
- Device token
- Habit ID to complete

### 2. Multi-Habit Button Board

Multiple buttons for multiple habits.

```
┌─────────────────────────────────────┐
│                                     │
│  [Medication] [Water] [Stretch]     │
│      (●)        (●)      (●)        │
│                                     │
│  3-5 buttons, each with LED         │
│  Shows which habits are pending     │
│                                     │
└─────────────────────────────────────┘
```

**Behavior**:
- LED on → Habit pending
- Press → Complete habit
- LED off → Habit done for today

### 3. NFC Reader

Tap NFC tag to complete associated habit.

```
┌─────────────────────────────────────┐
│                                     │
│          [NFC READER]               │
│              )))                    │
│                                     │
│  Mounted near habit location        │
│  e.g., on medication cabinet        │
│                                     │
│  Tags placed on items:              │
│  - Pill bottle                      │
│  - Water bottle                     │
│  - Exercise mat                     │
│                                     │
└─────────────────────────────────────┘
```

**Behavior**:
- Tag scanned → Look up tag ID → habit mapping
- Complete associated habit
- Beep/LED confirms

**Mapping** (stored in bridge or device):
```json
{
  "tag_mappings": {
    "04:A3:2B:1C:00:00:00": "habit-medication-123",
    "04:B7:3C:2D:00:00:00": "habit-water-456"
  }
}
```

### 4. Status Light Strip

Ambient display of habit progress.

```
┌─────────────────────────────────────┐
│                                     │
│  ████████████░░░░░░░░░░░░░░░░░░░░  │
│  ▲                              ▲   │
│  Completed (green)    Remaining     │
│                       (dim/off)     │
│                                     │
│  Or: Individual LEDs per habit      │
│  ●●●○○  (3 done, 2 remaining)      │
│                                     │
└─────────────────────────────────────┘
```

**Modes**:
- Progress bar (WS2812B strip)
- Individual habit LEDs
- Color coding (green=done, yellow=pending, red=lapsed)

### 5. E-Ink Display

Low-power habit dashboard.

```
┌─────────────────────────────────────┐
│  Today          3/5 ████████░░░░   │
│  ─────────────────────────────────  │
│  ✓ Take medication                  │
│  ✓ Drink water                      │
│  ○ Morning stretch                  │
│  ○ Review calendar                  │
│  ✓ Journal                          │
│                                     │
│  E-Ink: Updates every few minutes   │
│  Battery lasts months               │
└─────────────────────────────────────┘
```

---

## MQTT Bridge Service

### Responsibilities

```mermaid
flowchart TB
    subgraph Bridge["MQTT Bridge Service"]
        Auth["Device Authentication"]
        Validate["Payload Validation"]
        Write["Database Writes"]
        Notify["Status Broadcasting"]
        Poll["Change Polling"]
    end
    
    MQTT["Mosquitto"] <--> Auth
    Auth --> Validate
    Validate --> Write
    Write --> DB[(Postgres)]
    DB --> Poll
    Poll --> Notify
    Notify --> MQTT
```

### Implementation (Python Example)

```python
# mqtt_bridge.py (simplified)
import asyncio
import asyncpg
import aiomqtt
from datetime import datetime, timezone

class MQTTBridge:
    def __init__(self, mqtt_broker: str, db_url: str):
        self.mqtt_broker = mqtt_broker
        self.db_url = db_url
        
    async def run(self):
        self.db = await asyncpg.connect(self.db_url)
        
        async with aiomqtt.Client(self.mqtt_broker) as mqtt:
            await mqtt.subscribe("kairos/complete/#")
            await mqtt.subscribe("kairos/skip/#")
            await mqtt.subscribe("kairos/device/ping/#")
            
            # Start status broadcaster
            asyncio.create_task(self.broadcast_status(mqtt))
            
            async for message in mqtt.messages:
                await self.handle_message(mqtt, message)
    
    async def handle_message(self, mqtt, message):
        topic = str(message.topic)
        payload = json.loads(message.payload)
        
        if topic.startswith("kairos/complete/"):
            habit_id = topic.split("/")[-1]
            await self.handle_completion(mqtt, habit_id, payload)
        elif topic.startswith("kairos/skip/"):
            habit_id = topic.split("/")[-1]
            await self.handle_skip(mqtt, habit_id, payload)
    
    async def handle_completion(self, mqtt, habit_id: str, payload: dict):
        # Validate device token
        device_id = payload.get("device_id")
        token = payload.get("token")
        
        if not await self.validate_device(device_id, token):
            return  # Ignore invalid requests
        
        # Get user_id for this device
        user_id = await self.get_device_user(device_id)
        
        # Insert completion
        await self.db.execute("""
            INSERT INTO completions (id, habit_id, date, completed_at, type, sync_status, version)
            VALUES ($1, $2, $3, $4, 'FULL', 'SYNCED', 1)
            ON CONFLICT (habit_id, date) DO UPDATE SET
                completed_at = $4,
                type = 'FULL',
                version = completions.version + 1
        """, uuid4(), habit_id, date.today(), datetime.now(timezone.utc))
        
        # Publish confirmation
        await mqtt.publish(f"kairos/status/{habit_id}", json.dumps({
            "status": "completed",
            "completed_at": datetime.now(timezone.utc).isoformat()
        }))
    
    async def broadcast_status(self, mqtt):
        """Periodically broadcast habit status for status lights"""
        while True:
            # Get all devices and their users
            devices = await self.db.fetch("SELECT * FROM devices WHERE type = 'status_light'")
            
            for device in devices:
                summary = await self.get_user_habit_summary(device['user_id'])
                await mqtt.publish(
                    f"kairos/status/summary/{device['id']}", 
                    json.dumps(summary)
                )
            
            await asyncio.sleep(30)  # Every 30 seconds
```

### Docker Compose Addition

```yaml
  mqtt-bridge:
    build: ./mqtt-bridge
    container_name: kairos-mqtt-bridge
    restart: unless-stopped
    environment:
      DATABASE_URL: postgresql://kairos:${DB_PASSWORD}@postgres:5432/kairos
      MQTT_BROKER: mosquitto
      MQTT_PORT: 1883
    depends_on:
      - postgres
      - mosquitto
```

---

## Device Authentication

### Token-Based Auth

Each device has a pre-shared token configured during setup.

```sql
CREATE TABLE devices (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id),
    name TEXT NOT NULL,
    type TEXT NOT NULL,  -- 'button', 'nfc_reader', 'status_light', 'display'
    token TEXT NOT NULL,
    config JSONB,  -- Device-specific config (habit_id, tag_mappings, etc.)
    last_seen_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Example device registration
INSERT INTO devices (id, user_id, name, type, token, config) VALUES
('button-kitchen-01', 'user-123', 'Kitchen Medication Button', 'button', 
 'secure-random-token-here', '{"habit_id": "habit-medication-123"}');
```

### Device Provisioning Flow

```mermaid
sequenceDiagram
    actor User
    participant Admin as PocketBase Admin
    participant DB as Postgres
    participant Device as ESP32
    
    User->>Admin: Create new device
    Admin->>DB: INSERT device with token
    DB-->>Admin: Device created
    Admin-->>User: Show token + config
    
    User->>Device: Flash firmware
    User->>Device: Enter WiFi + broker + token
    Device->>Device: Store config in NVS
    
    Device->>MQTT: Connect + ping
    MQTT->>Bridge: Validate token
    Bridge->>DB: Check device exists
    DB-->>Bridge: Valid
    Bridge->>MQTT: Pong (confirmed)
    MQTT->>Device: Connected
```

---

## ESP32 Firmware

### Basic Button Firmware (Arduino/PlatformIO)

```cpp
// main.cpp (simplified)
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

const char* WIFI_SSID = "your-wifi";
const char* WIFI_PASS = "your-password";
const char* MQTT_BROKER = "kairos.home.local";
const char* DEVICE_ID = "button-kitchen-01";
const char* DEVICE_TOKEN = "secure-token";
const char* HABIT_ID = "habit-medication-123";

const int BUTTON_PIN = 0;  // GPIO0
const int LED_PIN = 2;     // GPIO2

WiFiClient wifi;
PubSubClient mqtt(wifi);

void setup() {
    pinMode(BUTTON_PIN, INPUT_PULLUP);
    pinMode(LED_PIN, OUTPUT);
    
    WiFi.begin(WIFI_SSID, WIFI_PASS);
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
    }
    
    mqtt.setServer(MQTT_BROKER, 1883);
    mqtt.setCallback(onMessage);
    
    reconnect();
}

void loop() {
    if (!mqtt.connected()) {
        reconnect();
    }
    mqtt.loop();
    
    if (digitalRead(BUTTON_PIN) == LOW) {
        completeHabit();
        delay(1000);  // Debounce
    }
}

void completeHabit() {
    digitalWrite(LED_PIN, HIGH);  // Blink LED
    
    StaticJsonDocument<200> doc;
    doc["device_id"] = DEVICE_ID;
    doc["token"] = DEVICE_TOKEN;
    doc["type"] = "FULL";
    
    char payload[200];
    serializeJson(doc, payload);
    
    String topic = String("kairos/complete/") + HABIT_ID;
    mqtt.publish(topic.c_str(), payload);
    
    delay(200);
    digitalWrite(LED_PIN, LOW);
}

void onMessage(char* topic, byte* payload, unsigned int length) {
    // Handle status updates, ACKs, etc.
}

void reconnect() {
    while (!mqtt.connected()) {
        if (mqtt.connect(DEVICE_ID)) {
            String statusTopic = String("kairos/status/") + HABIT_ID;
            mqtt.subscribe(statusTopic.c_str());
        } else {
            delay(5000);
        }
    }
}
```

### Project Structure

```
kairos-embedded/
├── devices/
│   ├── completion-button/
│   │   ├── src/
│   │   │   └── main.cpp
│   │   ├── include/
│   │   │   └── config.h
│   │   └── platformio.ini
│   ├── nfc-reader/
│   │   ├── src/
│   │   │   └── main.cpp
│   │   └── platformio.ini
│   ├── status-light/
│   │   ├── src/
│   │   │   └── main.cpp
│   │   └── platformio.ini
│   └── e-ink-display/
│       ├── src/
│       │   └── main.cpp
│       └── platformio.ini
├── common/
│   ├── mqtt_client/
│   ├── wifi_manager/
│   └── config_portal/        # Web-based config
└── README.md
```

---

## Security Considerations

### MQTT Security

| Layer | Implementation |
|-------|----------------|
| Transport | TLS optional (internal network) |
| Authentication | Device tokens |
| Authorization | Bridge validates device → user mapping |
| Topic isolation | Devices can only publish to their topics |

### Token Rotation

```mermaid
sequenceDiagram
    participant Admin
    participant DB
    participant Device
    
    Admin->>DB: Generate new token
    Admin->>Device: OTA update or manual
    Device->>Device: Store new token
    Device->>MQTT: Reconnect with new token
    Admin->>DB: Invalidate old token
```

### Network Isolation

For maximum security:
- MQTT broker on IoT VLAN
- Devices can't reach internet
- Only bridge can reach Postgres

---

## Future Considerations

### Home Assistant Integration

```yaml
# configuration.yaml
mqtt:
  sensor:
    - name: "Kairos Habits Remaining"
      state_topic: "kairos/status/summary"
      value_template: "{{ value_json.remaining }}"
      
automation:
  - alias: "Habit reminder light"
    trigger:
      - platform: mqtt
        topic: "kairos/status/summary"
    condition:
      - condition: template
        value_template: "{{ trigger.payload_json.remaining > 0 }}"
    action:
      - service: light.turn_on
        target:
          entity_id: light.habit_reminder
        data:
          color_name: "yellow"
```

### Bluetooth Support

For devices without WiFi access:
- ESP32 BLE → Phone → Server
- Phone acts as bridge
- Good for portable devices

### Battery Optimization

- Deep sleep between presses
- Wake on button interrupt
- Report battery level via MQTT
