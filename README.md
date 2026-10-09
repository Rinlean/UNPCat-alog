# UNPCat-alog

UNPCat-alog is a Java Swing desktop application for managing and tracking campus cats at the University of Northern Philippines.

## Features

- Role-based access for **admin**, **caretaker**, and **guest** users
- Cat profile management (create, update, and remove records)
- Adoption and adopter tracking
- Caretaker assignment and management
- Campus area/map-based tracking
- QR code scanning and viewing workflows
- Dark/light UI theme support

## Tech Stack

- Java (NetBeans Ant project)
- Swing UI (`.form` + `.java`)
- MySQL/MariaDB database
- ZXing + webcam-capture libraries for QR features
- FlatLaf for theming

## Project Structure

- `/src/main` - application source code and UI forms
- `/src/main/stuff` - utility/services (database and QR helpers)
- `/lib` - bundled third-party JAR dependencies
- `/test/unpcat_alog(3).sql` - database schema and sample seed data
- `/build.xml` - Ant build entrypoint

## Prerequisites

- JDK compatible with the project settings in `nbproject/project.properties` (currently source/target set to `24`)
- Apache Ant (if building from CLI)
- MySQL or MariaDB server

## Database Setup

1. Create a database named `unpcat_alog`.
2. Import `/home/runner/work/UNPCat-alog/UNPCat-alog/test/unpcat_alog(3).sql`.
3. Update database connection settings in `/home/runner/work/UNPCat-alog/UNPCat-alog/src/main/stuff/dbconn.java` if your host, user, or password differs.

## Build and Run

### Option 1: NetBeans (recommended)

1. Open the project folder in NetBeans.
2. Build and run the project.
3. The configured main class is `main.qrMenu`.

### Option 2: Ant CLI

From `/home/runner/work/UNPCat-alog/UNPCat-alog`:

```bash
ant clean
ant jar
ant run
```

## Default Test Accounts (from seed SQL)

- Admin: `admin` / `admin`
- Caretaker: `Rin` / `Rin`

> Change or remove default credentials before using in a real environment.

## Notes

- The repository includes generated build output under `/build`.
- Some dependencies and paths were originally configured in a NetBeans desktop environment.
