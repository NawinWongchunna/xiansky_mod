Xian Sky Walk - Fabric 1.20.1 (client-side)

BUILD
  1) ติดตั้ง JDK 17 และ Gradle 8.5+  (หรือเปิดโฟลเดอร์นี้ด้วย IntelliJ IDEA แล้ว import Gradle)
  2) รันในโฟลเดอร์นี้:   gradle build
  3) ได้ไฟล์ build/libs/xiansky-1.0.0.jar  -> ใส่ในโฟลเดอร์ mods
  4) ต้องมี Fabric Loader 0.15+ และ Fabric API 0.92.2+1.20.1

ใช้งาน
  กด G = เริ่ม/หยุดสกิล  (เปลี่ยนปุ่มได้ใน Options > Controls > Xian Sky)
  Shift = ยกเลิก

ไม่อยากลงอะไรในเครื่อง? (ใช้ GitHub บิลด์ให้)
  1) สร้าง repo ใหม่บน github.com แล้วอัปโหลดไฟล์ทั้งหมดในโฟลเดอร์นี้ (รวม .github)
  2) แท็บ Actions -> รอ workflow "build" เขียว -> ดาวน์โหลด artifact "xiansky-jar"
  3) แตก zip จะได้ xiansky-1.0.0.jar (ใช้ตัวที่ไม่มี -sources) ใส่โฟลเดอร์ mods

บิลด์ในเครื่อง (Windows): ดับเบิลคลิก build.bat
