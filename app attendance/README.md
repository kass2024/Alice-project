# Teacher Attendance (Android)

A small Java app for enrolling and verifying **registered teachers**. It does not create new staff. The school website keeps the register, documents, and reports. This app is the only camera.

## Open it

1. Install Android Studio.
2. **File → Open** and choose the folder `app attendance` (this folder).
3. Let Gradle finish.
4. Run it on a phone, or on an emulator.

## Connect to the school API

The Spring Boot API must already be running (`start.bat` in the parent folder).

- Emulator on the same PC: `http://10.0.2.2:8080`
- A real phone on the same Wi-Fi: `http://<computer-ip>:8080` (for example `http://192.168.1.20:8080`)

Do not use `localhost` on a phone. That means the phone itself.

Type the laptop address, for example `http://192.168.1.67:8080`, and tap **Test connection** before you sign in. Use a website account. Demo password: `password123`.

| Who | What they do |
| --- | --- |
| admin | Enroll any teacher, then verify at the gate |
| teacher | Enroll only their own face, then check in |
| Head of Studies | Can verify. Enrollment is for an admin |

## What the screens do

- **Enroll** — loads registered teachers after you open it. **Take photo** stores that face signature. **Remove** deletes an enrolled face.
- **Verify** — the camera watches and names the teacher. The first clock of the day is check-in (Present, or Late after 08:15). A later clock is check-out.
- A teacher marked **on leave** is not changed by a scan.

The result appears on the website under **Attendance** and **Biometric**.
