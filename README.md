# Smart Teacher Attendance & Pedagogical Document Management System

A final-year school system: teacher attendance, face check-in in a separate Android app, teaching documents, approval, class monitoring, and reports.

The website is React. The API is Spring Boot. The database is MariaDB from XAMPP (the same screen as MySQL). The database name is `alice_teacher`. There is no Docker.

Public clone (no password):

```
git clone https://github.com/kass2024/Alice-project.git
```

The full install steps for the database, Spring Boot, and the website are in `setup-guide.html`. On this laptop that file is `C:\methode\water_level\Alice project\setup-guide.html`.

## What you need

- Java 17 (JDK). The project compiles as Java 11.
- Maven 3.9, on your Path
- Node.js
- XAMPP, with MySQL/MariaDB. This project uses port **3307**

## Run it

1. Start **MySQL** in the XAMPP Control Panel.
2. Double-click `start.bat`.
3. Wait until the API window says `Smart Teacher API is ready`.
4. Open http://localhost:5173

A full local setup, including how the phone reaches the laptop, is in `setup-guide.html`. Double-click that file.

Demo password for every account: `password123`

| Username | Role |
| --- | --- |
| admin | Admin |
| patrick | Head of Studies |
| alice | Teacher (Mathematics) |
| jean | Teacher (English) |
| marie | Teacher (Biology) |
| eric | Teacher (History) |
| divine | Teacher (ICT) |
| grace | Teacher (Physics) |

## Face check-in (Android app)

The website does not open a camera. Enrollment and verification are in the folder `app attendance`. Open that folder in Android Studio and run it on a phone.

1. On the phone login screen, set the server to this computer, for example `http://192.168.1.67:8080`, then tap **Test connection**. The phone and the computer must be on the same Wi-Fi. An emulator on this PC can use `http://10.0.2.2:8080`.
2. Sign in as `admin` / `password123`.
3. Tap **Enroll**. The staff list loads then. **Take photo** saves one face. **Remove** clears an enrolled face.
4. Tap **Verify**. The camera names the teacher. The first clock of the day is check-in. A later clock is check-out. After **08:15** a new check-in is Late. A teacher already marked on leave is not changed by a scan.
5. The website **Attendance** and **Biometric** pages then show the result.

Saving the daily register on the website does not wipe a face check-in when the status stays the same.

## Pedagogical documents and Gemini

Open **AI analysis**.

- **REB:** upload a Syllabus and a Weeks breakdown. The screen does not call the weeks file a chronogram.
- **TVET:** upload a Curriculum and a Chronogram.

Click **Load sample files** if you want a short demo, then **Analyse**. Gemini returns units or modules and a scheme of work. **Save scheme to repository** stores an Excel file as a draft. The teacher can submit it, and the Head of Studies approves it from **Approvals**.

The Gemini key is the same Google AI key used by Xander School. It is already set in `backend/src/main/resources/application.properties`. Do not publish that file.

## If something does not start

- Database error: XAMPP MySQL is not running, or the port is not 3307. Change `spring.datasource.url` and `app.db-port` together.
- Root password is not empty: set `spring.datasource.password` and `app.db-password`.
- Port 8080 is busy: change `server.port`, and the proxy in `frontend/vite.config.js`.
- The phone cannot reach the API: allow Java through the Windows firewall for port 8080, and use the computer’s Wi-Fi address, not `localhost`.
- Run `npm install` once inside `frontend` if `start.bat` says npm is missing packages.
