# Ascension Rings (Fabric 1.20.1)

กดปุ่ม **P** → ลอยขึ้นฟ้าพร้อมวงแหวนไฟ → วงเวทย์กางใต้เท้า ดอกเถาวัลย์แดงผลิบาน → ดูดพลังรอบๆ เข้าแกนวอร์เท็กซ์ ท้องฟ้าเปลี่ยนเป็นสีแดงเข้ม กล้องสั่นหนักขึ้น → flash ขาว ปล่อยพลังออกรอบตัว (ดาเมจ + ไฟ + knockback) แล้วลอยลงช้าๆ

ความยาวทั้งหมด 160 tick (8 วินาที) — แผน A: Cinematic Ascension

## Requirements
- Minecraft **1.20.1**, Fabric Loader **0.20.1** (รองรับ >= 0.15), Fabric API (0.92.x สำหรับ 1.20.1), Java 17

## Build
```
gradle build          # หรือ  gradle wrapper && ./gradlew build
```
ไฟล์ jar อยู่ที่ `build/libs/ascension-rings-1.0.0.jar` ใส่ในโฟลเดอร์ `mods`
ทุกครั้งที่ push ขึ้น GitHub, workflow `.github/workflows/build.yml` จะ build jar ให้อัตโนมัติ (ดูที่แท็บ Actions → Artifacts) และถ้า push tag `v*` จะสร้าง Release พร้อมแนบ jar

## ควบคุม
- ปุ่ม **P** (เปลี่ยนได้ที่ Options → Controls → "Ascension Rings")
  - หมายเหตุ: vanilla ใช้ P เป็น Social Interactions ถ้าชน ให้เปลี่ยนอันใดอันหนึ่ง
- ระหว่างสกิลเดิน/กระโดดไม่ได้ และผู้เล่นได้ Resistance 5 กันตาย

## Config — `config/ascension_rings.json`
| key | ค่าเริ่มต้น | ความหมาย |
|---|---|---|
| `cinematic` | true | เปิด cutscene (กล้อง orbit/สั่น/FOV, ฟ้าแดง, แถบดำ, overlay) |
| `damage` | 14.0 | ดาเมจตอนปล่อยพลัง |
| `radius` | 14.0 | รัศมีดาเมจ |
| `cooldownTicks` | 400 | คูลดาวน์ (20 tick = 1 วิ) |

## Multiplayer / Server
- ใช้ได้ทั้ง singleplayer และ server (mod ต้องติดตั้งทั้งสองฝั่ง)
- ผู้เล่นคนอื่นที่เห็นเราอยู่จะเห็นวงแหวน วงเวทย์ ดอกไม้ ลูกพลัง ส่วน cutscene/ฟ้าแดงเห็นเฉพาะคนกด
- Dedicated server ควรตั้ง `allow-flight=true` ใน `server.properties` ไม่เช่นนั้นระบบกัน fly อาจเตะผู้เล่นที่ลอยนานเกิน ~5 วินาที

## โครงสร้าง
```
src/main/java/dev/ascension/rings/
├── AscensionMod.java          entrypoint (server+common)
├── AscensionConfig.java
├── Timeline.java              ไทม์ไลน์ + ความเร็วลอย (ใช้ร่วม)
├── network/Net.java           packet id
├── ability/AscensionServer.java   สถานะ/เสียง/ดาเมจ/คูลดาวน์
├── client/AscensionClient.java    keybind, event
├── client/ClientState.java        สถานะ client, ขับการลอย, particle
├── client/camera/CameraEffects.java  keyframe กล้อง/FOV/สั่น
├── client/vfx/AscensionRenderer.java วงแหวน/วงเวทย์/ดอกไม้/วอร์เท็กซ์/orb
├── client/vfx/AscensionHud.java      แถบดำ, vignette, flash, overlay ดอกไม้
└── mixin/                     Camera, GameRenderer(FOV), ClientWorld(ฟ้า/เมฆ), KeyboardInput(ล็อกการเดิน)
src/main/resources/assets/ascension_rings/textures/vfx/
    ring_0..3 (FireMage), vortex_0..1 + orb (BloodMage), magic_circle, lily
```

ปรับจังหวะ/ความแรง cutscene ได้ที่ `Timeline.java` และ `CameraEffects.java` (เป็น keyframe อ่านง่าย)
