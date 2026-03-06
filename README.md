# 🎟️ Event Booking Management API

## 📖 Deskripsi Project

**Event Booking Management API** adalah RESTful API untuk platform pemesanan tiket event. Sistem ini memungkinkan pengguna untuk mendaftar, login, melihat daftar event, membuat event, dan melakukan pemesanan tiket secara aman.

API ini dibangun menggunakan **Spring Boot 3.5.11** dengan arsitektur berlapis (*layered architecture*) yang mengikuti prinsip **SOLID** dan best practice pengembangan backend modern.

---

## 🛠️ Tech Stack

| Teknologi | Versi | Kegunaan |
|---|---|---|
| Java | 17 | Bahasa pemrograman utama |
| Spring Boot | 3.5.11 | Framework aplikasi |
| Spring Security | 6.x | Autentikasi & otorisasi |
| Spring Data JPA | 3.x | ORM & query database |
| PostgreSQL | 16 | Database utama |
| Flyway | 10.x | Database migration |
| JWT (jjwt) | 0.12.3 | Token autentikasi stateless |
| Lombok | Latest | Mengurangi boilerplate code |
| Springdoc OpenAPI | 2.8.8 | Dokumentasi API (Swagger UI) |
| JUnit 5 | 5.x | Unit testing |
| Mockito | Latest | Mocking dependencies |
| Maven | 3.x | Build tool & dependency management |

---

## ⚙️ Prasyarat

Sebelum menjalankan aplikasi, pastikan sudah terinstal:

- **Java 17** atau lebih baru
- **Maven 3.6+**
- **PostgreSQL 14+**
- **Git**

---

## 🚀 Setup & Menjalankan Aplikasi

### 1. Clone Repository

```bash
git clone https://github.com/username/event-booking-management.git
cd event-booking-management
```

### 2. Buat Database PostgreSQL

```sql
CREATE DATABASE event_booking_db;
```

### 3. Konfigurasi Environment Variables

Buat file `.env` atau set environment variables berikut:

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/event_booking_db
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# JWT
JWT_SECRET=your_secret_key_minimum_32_characters_long
JWT_EXPIRATION_MS=86400000
```

Atau edit langsung di `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/event_booking_db
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password

app.jwt.secret=your_secret_key_minimum_32_characters_long
app.jwt.expiration-ms=86400000
```

### 4. Jalankan Aplikasi

```bash
mvn spring-boot:run
```

Flyway akan otomatis membuat semua tabel saat aplikasi pertama kali dijalankan.

### 5. Verifikasi Aplikasi Berjalan

```bash
curl http://localhost:8080/swagger-ui/index.html
```

Atau buka browser dan akses: `http://localhost:8080/swagger-ui/index.html`

---

## 🧪 Menjalankan Unit Test

```bash
# Jalankan semua test
mvn test

# Jalankan test dengan laporan detail
mvn test -Dsurefire.failIfNoSpecifiedTests=false
```

**Total: 56 unit test** — semua passing ✅

| Test Class | Jumlah Test | FR yang Dicover |
|---|---|---|
| `AuthServiceImplTest` | 13 | FR01, FR02, FR03 |
| `EventServiceImplTest` | 32 | FR04, FR05, FR06, FR07, FR08, FR09 |
| `BookingServiceImplTest` | 19 | FR10, FR11, FR12, FR13, FR14 |

---

## 📌 Ringkasan API Endpoint

Base URL: `http://localhost:8080`

### 🔐 Auth

| Method | Endpoint | Akses | Deskripsi |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Registrasi user baru |
| `POST` | `/api/auth/login` | Public | Login & dapatkan JWT token |
| `GET` | `/api/users/me` | Protected | Lihat profil user yang login |

### 🎪 Events

| Method | Endpoint | Akses | Deskripsi |
|---|---|---|---|
| `GET` | `/api/events` | Public | List semua upcoming event (pagination + search) |
| `GET` | `/api/events/{id}` | Public | Detail satu event |
| `POST` | `/api/events` | Protected | Buat event baru |
| `PUT` | `/api/events/{id}` | Protected (creator) | Update event |
| `DELETE` | `/api/events/{id}` | Protected (creator) | Soft delete event |

**Query Parameters untuk `GET /api/events`:**

| Parameter | Tipe | Default | Deskripsi |
|---|---|---|---|
| `page` | `int` | `0` | Nomor halaman |
| `size` | `int` | `10` | Jumlah item per halaman |
| `title` | `String` | - | Filter judul (partial, case-insensitive) |
| `location` | `String` | - | Filter lokasi (partial, case-insensitive) |
| `startDate` | `LocalDateTime` | - | Filter dari tanggal (format: `yyyy-MM-dd'T'HH:mm:ss`) |
| `endDate` | `LocalDateTime` | - | Filter sampai tanggal |

### 🎫 Bookings

| Method | Endpoint | Akses | Deskripsi |
|---|---|---|---|
| `POST` | `/api/bookings` | Protected | Buat booking tiket |
| `GET` | `/api/bookings/me` | Protected | List semua booking milik user |
| `PATCH` | `/api/bookings/{id}/cancel` | Protected (owner) | Batalkan booking |

---

## 🗄️ Skema Database

```
users
├── id (PK)
├── name
├── email (UNIQUE)
├── password (BCrypt)
├── created_at
└── updated_at

events
├── id (PK)
├── title
├── description
├── location
├── event_date
├── available_seats
├── ticket_price
├── is_active (soft delete flag)
├── version (optimistic locking)
├── creator_id (FK → users.id)
├── created_at
└── updated_at

bookings
├── id (PK)
├── reference_number (UNIQUE)
├── num_tickets
├── total_price
├── status (ACTIVE | CANCELLED)
├── user_id (FK → users.id)
├── event_id (FK → events.id)
├── UNIQUE (user_id, event_id)
├── created_at
└── updated_at
```

---

## 🏛️ Keputusan Desain & Trade-off

### 1. JWT Stateless Authentication

**Keputusan:** Menggunakan JWT stateless dibanding session-based authentication.

**Alasan:** Lebih scalable karena server tidak perlu menyimpan session state. Cocok untuk arsitektur microservice di masa depan.

**Trade-off:** Token tidak bisa di-invalidate sebelum expired. Jika token bocor, hanya bisa ditunggu sampai expired atau implementasi token blacklist (tidak diimplementasikan di versi ini).

---

### 2. Soft Delete pada Event

**Keputusan:** Event yang dihapus di-set `isActive = false`, tidak dihapus secara fisik dari database.

**Alasan:** Data historis tetap tersimpan untuk audit trail. Mencegah foreign key violation karena tabel `bookings` mereferensikan tabel `events`.

**Trade-off:** Tabel `events` akan terus bertambah seiring waktu. Perlu periodic archiving untuk production.

---

### 3. Optimistic Locking untuk Booking

**Keputusan:** Menggunakan `@Version` pada entity `Event` untuk mencegah overselling.

**Alasan:** Lebih efisien dibanding pessimistic locking karena tidak ada row lock di database. Cocok untuk read-heavy workload seperti event booking.

**Trade-off:** Client perlu retry jika mendapat `409 Conflict` akibat concurrent update. Pengalaman user sedikit terganggu jika terjadi conflict.

---

### 4. Specification Pattern untuk Search

**Keputusan:** Menggunakan `JpaSpecificationExecutor` dan Criteria API untuk filter dinamis pada `GET /api/events`.

**Alasan:** JPQL dengan `IS NULL` check menyebabkan PostgreSQL error tipe data (`lower(bytea) does not exist`). Specification hanya membuat predicate jika filter diisi, sehingga tidak ada parameter `null` yang dikirim ke PostgreSQL.

**Trade-off:** Kode lebih verbose dibanding JPQL sederhana.

---

### 5. FR05 dan FR09 Digabung

**Keputusan:** List Events dan Search Events dijadikan satu endpoint `GET /api/events` dengan query parameter opsional.

**Alasan:** Lebih RESTful dan efisien. Filter search hanya query parameter tambahan — tidak perlu endpoint terpisah.

**Trade-off:** Tidak ada. Pendekatan ini adalah best practice REST.

---

### 6. 404 bukan 403 untuk Resource Milik Orang Lain

**Keputusan:** Ketika user mencoba cancel booking milik orang lain, return `404 Not Found` bukan `403 Forbidden`.

**Alasan:** Mencegah *information disclosure* — attacker tidak bisa mengetahui apakah resource dengan id tertentu ada di sistem atau tidak.

**Trade-off:** Pesan error kurang informatif untuk debugging, tapi lebih aman untuk production.

---

## 📐 Asumsi yang Dibuat

1. **Satu user satu booking per event** — setiap user hanya boleh memiliki satu booking aktif per event. Jika ingin tambah tiket, harus cancel dulu lalu booking ulang.

2. **Batas pembatalan 24 jam** — pembatalan hanya diperbolehkan lebih dari 24 jam sebelum event dimulai.

3. **Event gratis diperbolehkan** — `ticketPrice = 0` valid untuk event gratis.

4. **Creator bisa booking event miliknya sendiri** — tidak ada larangan creator booking event yang dia buat sendiri.

5. **Available seats tidak bisa negatif** — dijaga di level aplikasi (validasi) dan level database (CHECK constraint).

6. **Reference number tidak dijamin 100% unik** — menggunakan format `BK-YYYYMMDD-XXXXXX` dengan 6 digit random. Probabilitas collision sangat kecil untuk skala aplikasi ini. DB constraint `UNIQUE` sebagai safety net.

7. **JWT secret disimpan di environment variable** — tidak di-hardcode di source code untuk keamanan.

8. **Password tidak pernah di-log** — hanya email dan id yang dicatat di application log.

---

## 📚 Dokumentasi API

API docs (JSON) tersedia di:

```
http://localhost:8080/api-docs
```

---

## 👤 Author

**Agora Booking Management**
- GitHub: [@deep4n](https://github.com/deep4n/Agora-Challenge)
