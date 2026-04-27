<p align="center">
  <img src="https://img.icons8.com/external-flaticons-flat-flat-icons/128/external-library-university-flaticons-flat-flat-icons.png" alt="Library Logo" width="120"/>
</p>

<h1 align="center">📚 Library Management System</h1>

<p align="center">
  A full-featured RESTful API for managing books, members, borrowing, fines, and analytics — built with Spring Boot & JPA.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/H2-Database-0000BB?style=for-the-badge&logo=databricks&logoColor=white" alt="H2"/>
  <img src="https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Maven"/>
  <img src="https://img.shields.io/badge/Lombok-Enabled-red?style=for-the-badge" alt="Lombok"/>
</p>

---

## 🏗️ Architecture

```
Controller  →  Service  →  Repository  →  H2 Database
    ↕              ↕
   DTO         Exception
```

```
src/main/java/com/library/library_management/
├── config/                  # Configuration classes
├── controller/              # REST Controllers (5)
│   ├── BookController       # /api/books
│   ├── MemberController     # /api/members
│   ├── BorrowController     # /api/borrow
│   ├── FineController       # /api/fines
│   └── StatsController      # /api/stats
├── dto/                     # Data Transfer Objects
│   ├── ApiResponse          # Unified API response wrapper
│   ├── BorrowRequest        # Borrow request body
│   └── CopiesRequest        # Book copies update body
├── exception/               # Custom exceptions + global handler
│   ├── GlobalExceptionHandler
│   ├── ResourceNotFoundException
│   ├── DuplicateResourceException
│   └── IllegalOperationException
├── model/                   # JPA Entities
│   ├── Book
│   ├── Member
│   ├── BorrowRecord
│   ├── Fine
│   ├── BorrowStatus         # enum: BORROWED, RETURNED, OVERDUE
│   └── MembershipType       # enum: BASIC, PREMIUM, STUDENT
├── repository/              # Spring Data JPA Repositories
│   ├── BookRepository
│   ├── MemberRepository
│   ├── BorrowRecordRepository
│   └── FineRepository
└── service/                 # Business Logic
    ├── BookService
    ├── MemberService
    ├── BorrowService
    ├── FineService
    └── StatsService
```

---

## 🚀 Quick Start

### Prerequisites

- **Java 17** or higher
- **Maven 3.8+** (or use the included Maven wrapper)

### Run the Application

```bash
# Clone the repository
git clone https://github.com/your-username/library-management.git
cd library-management

# Build & Run
./mvnw spring-boot:run
```

> **Windows:** use `mvnw.cmd spring-boot:run`

The server starts at **http://localhost:8080**

### Access H2 Console

Navigate to **http://localhost:8080/h2-console** with:

| Field       | Value                  |
|-------------|------------------------|
| JDBC URL    | `jdbc:h2:mem:librarydb`|
| Username    | `sa`                   |
| Password    | *(leave empty)*        |

---

## 📡 API Endpoints (25 Total)

All responses follow a unified format:

```json
{
  "status": 200,
  "message": "Books fetched successfully",
  "data": [ ... ],
  "timestamp": "2026-04-28T10:30:00"
}
```

### 📖 Books `/api/books`

| #  | Method   | Endpoint                  | Description                          |
|----|----------|---------------------------|--------------------------------------|
| 1  | `GET`    | `/api/books`              | Get all books                        |
| 2  | `GET`    | `/api/books/{id}`         | Get a book by ID                     |
| 3  | `GET`    | `/api/books/search`       | Search by `genre`, `author`, `title`, `available` |
| 4  | `POST`   | `/api/books`              | Create a new book                    |
| 5  | `PUT`    | `/api/books/{id}`         | Update a book                        |
| 6  | `PATCH`  | `/api/books/{id}/copies`  | Update total copies count            |
| 7  | `DELETE` | `/api/books/{id}`         | Delete a book                        |

### 👥 Members `/api/members`

| #  | Method   | Endpoint                          | Description                         |
|----|----------|-----------------------------------|-------------------------------------|
| 8  | `POST`   | `/api/members`                    | Register a new member               |
| 9  | `GET`    | `/api/members/{id}`               | Get member by ID                    |
| 10 | `GET`    | `/api/members?type=PREMIUM`       | Filter members by membership type   |
| 11 | `PUT`    | `/api/members/{id}`               | Update member details               |
| 12 | `PATCH`  | `/api/members/{id}/upgrade`       | Upgrade membership to PREMIUM       |
| 13 | `DELETE` | `/api/members/{id}`               | Soft delete (deactivate) a member   |
| 19 | `GET`    | `/api/members/{id}/borrow-history`| Get member's borrow history         |
| 22 | `GET`    | `/api/members/{id}/fines`         | Get member's fines                  |

### 🔄 Borrow `/api/borrow`

| #  | Method   | Endpoint                    | Description                              |
|----|----------|-----------------------------|------------------------------------------|
| 14 | `POST`   | `/api/borrow`               | Borrow a book                            |
| 15 | `PATCH`  | `/api/borrow/{id}/return`   | Return a borrowed book                   |
| 16 | `GET`    | `/api/borrow/overdue`       | Get all overdue borrow records           |
| 17 | `GET`    | `/api/borrow/active`        | Get all active (unreturned) borrows      |
| 18 | `PATCH`  | `/api/borrow/{id}/extend`   | Extend due date *(PREMIUM members only)* |

### 💰 Fines `/api/fines`

| #  | Method   | Endpoint               | Description              |
|----|----------|------------------------|--------------------------|
| 20 | `GET`    | `/api/fines/unpaid`    | Get all unpaid fines     |
| 21 | `PATCH`  | `/api/fines/{id}/pay`  | Pay a fine               |

### 📊 Stats `/api/stats`

| #  | Method | Endpoint                       | Description                        |
|----|--------|--------------------------------|------------------------------------|
| 23 | `GET`  | `/api/stats/summary`           | Library summary dashboard          |
| 24 | `GET`  | `/api/stats/top-books?limit=5` | Most borrowed books                |
| 25 | `GET`  | `/api/stats/overdue-rate`      | Overdue rate percentage            |

---

## 🧪 Example Requests

### Create a Book

```bash
curl -s -X POST http://localhost:8080/api/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "The Great Gatsby",
    "author": "F. Scott Fitzgerald",
    "isbn": "978-0743273565",
    "genre": "Fiction",
    "totalCopies": 5,
    "availableCopies": 5
  }'
```

### Register a Member

```bash
curl -s -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john.doe@email.com",
    "phone": "1234567890",
    "membershipType": "BASIC",
    "membershipDate": "2026-04-28"
  }'
```

### Borrow a Book

```bash
curl -s -X POST http://localhost:8080/api/borrow \
  -H "Content-Type: application/json" \
  -d '{ "bookId": 1, "memberId": 1 }'
```

> 📄 **Full curl commands for all 25 endpoints** are available in [`curl-requests.sh`](curl-requests.sh)

---

## ⚙️ Business Rules

| Rule | Description |
|------|-------------|
| **Borrow Period** | 14 days from borrow date |
| **Extension** | +7 days, only for `PREMIUM` members |
| **Overdue Fine** | $1.00 per day, auto-generated on return |
| **Soft Delete** | Members are deactivated, not removed from DB |
| **Copy Tracking** | `availableCopies` auto-adjusts on borrow/return |
| **Duplicate Check** | ISBN (books) and Email (members) must be unique |
| **Inactive Block** | Inactive members cannot borrow books |

---

## 🗃️ Database Schema

```
┌──────────────┐       ┌──────────────────┐       ┌──────────────┐
│    books     │       │  borrow_records  │       │   members    │
├──────────────┤       ├──────────────────┤       ├──────────────┤
│ id (PK)      │──┐    │ id (PK)          │   ┌──│ id (PK)      │
│ title        │  └───>│ book_id (FK)     │   │  │ name         │
│ author       │       │ member_id (FK)   │<──┘  │ email (UQ)   │
│ isbn (UQ)    │       │ borrow_date      │      │ phone        │
│ genre        │       │ due_date         │      │ membershipType│
│ totalCopies  │       │ return_date      │      │ membershipDate│
│ availableCopies│     │ status           │      │ active       │
└──────────────┘       └──────────────────┘      └──────────────┘
                              │
                              │ 1:1
                              ▼
                       ┌──────────────┐
                       │    fines     │
                       ├──────────────┤
                       │ id (PK)      │
                       │ borrow_record_id (FK)│
                       │ amount       │
                       │ paid         │
                       │ issued_date  │
                       │ paid_date    │
                       └──────────────┘
```

---

## 🛡️ Error Handling

All errors return a consistent `ApiResponse` structure:

| Status | Exception                    | When                                |
|--------|------------------------------|-------------------------------------|
| `400`  | `IllegalOperationException`  | Business rule violation             |
| `400`  | `IllegalArgumentException`   | Invalid input values                |
| `404`  | `ResourceNotFoundException`  | Entity not found by ID/ISBN/email   |
| `409`  | `DuplicateResourceException` | Duplicate ISBN or email             |
| `409`  | `DataIntegrityViolation`     | DB constraint violation             |
| `500`  | `Exception`                  | Unexpected server error             |

```json
{
  "status": 404,
  "message": "Book not found with id: 99",
  "data": null,
  "timestamp": "2026-04-28T10:30:00"
}
```

---

## 🛠️ Tech Stack

| Technology    | Purpose                    |
|---------------|----------------------------|
| Java 17       | Language                   |
| Spring Boot 3.5 | Framework               |
| Spring Data JPA | ORM & Repository layer  |
| H2 Database   | In-memory database         |
| Lombok         | Boilerplate reduction     |
| Maven          | Build & dependency mgmt   |

---

## 📬 Contact

Feel free to open an issue or submit a pull request for improvements!

---

<p align="center">
  Made with ❤️ using Spring Boot
</p>
