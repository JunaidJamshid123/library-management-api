# 🌐 REST APIs WITH SPRING BOOT — Complete In-Depth Guide

> **Spring Boot Version:** 3.x | **Java Version:** 17+  
> Covers theory, real-world analogies, diagrams, code examples, common mistakes, and interview Q&A.

---

## Table of Contents

1. [@RestController](#1-restcontroller)
2. [@RequestMapping](#2-requestmapping)
3. [HTTP Method Annotations](#3-http-method-annotations--getmapping-postmapping-putmapping-patchmapping-deletemapping)
4. [@PathVariable](#4-pathvariable)
5. [@RequestParam (Query Parameters)](#5-requestparam-query-parameters)
6. [@RequestBody](#6-requestbody)
7. [@ResponseStatus](#7-responsestatus)
8. [ResponseEntity\<T\>](#8-responseentityt)
9. [HTTP Status Codes in Spring](#9-http-status-codes-in-spring)
10. [Global Exception Handling](#10-global-exception-handling--controlleradvice--exceptionhandler)
11. [Complete CRUD Example — Putting It All Together](#11-complete-crud-example--putting-it-all-together)
12. [Common Mistakes & Troubleshooting](#12-common-mistakes--troubleshooting)
13. [Interview Questions & Answers](#13-interview-questions--answers)

---

## 1. @RestController

### What is @RestController?

`@RestController` is a specialized version of `@Controller` that combines `@Controller` + `@ResponseBody`. Every method in a `@RestController` automatically **serializes the return value to JSON** (or XML) and writes it directly to the HTTP response body.

### 💡 Real-World Analogy

- **@Controller** = A **librarian** who gives you a **reference card** (view name) → you go to the shelf yourself to get the book (HTML page).
- **@RestController** = A **delivery person** who brings the **actual book** (data as JSON) directly to your door.

### @Controller vs @RestController

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│   @Controller (MVC — returns VIEWS)                              │
│   ┌────────────────────────────────────────────────────────┐     │
│   │  @Controller                                           │     │
│   │  public class BookController {                         │     │
│   │                                                        │     │
│   │      @GetMapping("/books")                             │     │
│   │      public String listBooks(Model model) {            │     │
│   │          model.addAttribute("books", bookService...);  │     │
│   │          return "book-list";  ← VIEW NAME (HTML)       │     │
│   │      }                                                 │     │
│   │  }                                                     │     │
│   └────────────────────────────────────────────────────────┘     │
│   Client ←── HTML Page (rendered from template)                  │
│                                                                  │
│   @RestController (REST API — returns DATA)                      │
│   ┌────────────────────────────────────────────────────────┐     │
│   │  @RestController                                       │     │
│   │  public class BookRestController {                     │     │
│   │                                                        │     │
│   │      @GetMapping("/api/books")                         │     │
│   │      public List<Book> getAllBooks() {                  │     │
│   │          return bookService.findAll();  ← DATA (JSON)  │     │
│   │      }                                                 │     │
│   │  }                                                     │     │
│   └────────────────────────────────────────────────────────┘     │
│   Client ←── JSON: [{"id":1,"title":"Spring in Action"}, ...]   │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Under the Hood

```java
// @RestController is a META-ANNOTATION — it's composed of two annotations:

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Controller        // ← Registers as a Spring MVC controller bean
@ResponseBody      // ← Every method's return value is written to HTTP response body
public @interface RestController {
}
```

This means:
```java
// THESE TWO ARE IDENTICAL:

// Option 1: @RestController
@RestController
public class BookController {
    @GetMapping("/books")
    public List<Book> getAll() { return books; }
}

// Option 2: @Controller + @ResponseBody on each method
@Controller
public class BookController {
    @GetMapping("/books")
    @ResponseBody    // ← Must add this to EVERY method
    public List<Book> getAll() { return books; }
}
```

### How JSON Serialization Works

```
┌──────────────────────────────────────────────────────────────────┐
│           JSON SERIALIZATION FLOW                                │
│                                                                  │
│   Your Method Returns:                                           │
│   ┌──────────────────────────────────┐                           │
│   │  Book object                     │                           │
│   │  { id=1, title="Clean Code",    │                           │
│   │    author="Robert Martin" }      │                           │
│   └────────────────┬─────────────────┘                           │
│                    ▼                                             │
│   Spring checks: @ResponseBody present?                          │
│                    │ YES                                         │
│                    ▼                                             │
│   HttpMessageConverter kicks in                                  │
│   ┌──────────────────────────────────┐                           │
│   │  MappingJackson2HttpMessage      │                           │
│   │  Converter (uses Jackson)        │                           │
│   │                                  │                           │
│   │  Book → ObjectMapper.writeValue  │                           │
│   │      → JSON string              │                           │
│   └────────────────┬─────────────────┘                           │
│                    ▼                                             │
│   HTTP Response:                                                 │
│   ┌──────────────────────────────────┐                           │
│   │  Status: 200 OK                 │                           │
│   │  Content-Type: application/json  │                           │
│   │  Body:                           │                           │
│   │  {                               │                           │
│   │    "id": 1,                      │                           │
│   │    "title": "Clean Code",        │                           │
│   │    "author": "Robert Martin"     │                           │
│   │  }                               │                           │
│   └──────────────────────────────────┘                           │
│                                                                  │
│   Jackson (included in spring-boot-starter-web) does this       │
│   automatically. No manual JSON conversion needed!               │
└──────────────────────────────────────────────────────────────────┘
```

### Basic @RestController Example

```java
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.findAll();
        // Automatically converted to JSON array
    }

    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Long id) {
        return bookService.findById(id);
        // Automatically converted to JSON object
    }

    @PostMapping
    public Book createBook(@RequestBody Book book) {
        return bookService.save(book);
    }
}
```

---

## 2. @RequestMapping

### What is @RequestMapping?

`@RequestMapping` is the **base annotation** for mapping HTTP requests to handler methods. It can specify the URL path, HTTP method, content type, headers, and more.

### 💡 Real-World Analogy

`@RequestMapping` is like an **address system in a building**:
- Building address = Class-level `@RequestMapping("/api/books")` → all rooms in this building handle "books"
- Room number = Method-level `@GetMapping("/{id}")` → this specific room handles fetching a book by ID

### Class-Level vs Method-Level Mapping

```java
@RestController
@RequestMapping("/api/books")     // ← CLASS-LEVEL: base path for ALL methods
public class BookController {

    @GetMapping                    // ← GET /api/books
    public List<Book> getAll() { ... }

    @GetMapping("/{id}")           // ← GET /api/books/5
    public Book getById(@PathVariable Long id) { ... }

    @PostMapping                   // ← POST /api/books
    public Book create(@RequestBody Book book) { ... }

    @PutMapping("/{id}")           // ← PUT /api/books/5
    public Book update(@PathVariable Long id, @RequestBody Book book) { ... }

    @DeleteMapping("/{id}")        // ← DELETE /api/books/5
    public void delete(@PathVariable Long id) { ... }
}
```

```
┌──────────────────────────────────────────────────────────────────┐
│               URL PATH COMPOSITION                               │
│                                                                  │
│   Class: @RequestMapping("/api/books")                           │
│                    │                                             │
│                    ├── @GetMapping           → GET  /api/books   │
│                    ├── @GetMapping("/{id}")  → GET  /api/books/5 │
│                    ├── @PostMapping          → POST /api/books   │
│                    ├── @PutMapping("/{id}")  → PUT  /api/books/5 │
│                    └── @DeleteMapping("/{id}")→ DEL /api/books/5 │
│                                                                  │
│   Class path + Method path = FULL URL                            │
└──────────────────────────────────────────────────────────────────┘
```

### @RequestMapping Attributes

```java
@RequestMapping(
    value = "/api/books",              // URL path (or 'path')
    method = RequestMethod.GET,         // HTTP method
    produces = "application/json",      // Response content type
    consumes = "application/json",      // Accepted request content type
    headers = "X-API-VERSION=1",        // Required headers
    params = "type=fiction"             // Required query parameters
)
public List<Book> getBooks() { ... }
```

| Attribute    | Description                                          | Example                           |
| ------------ | ---------------------------------------------------- | --------------------------------- |
| `value/path` | URL pattern(s)                                       | `"/books"`, `"/books/{id}"`       |
| `method`     | HTTP method(s)                                       | `RequestMethod.GET`               |
| `produces`   | Response content type(s)                             | `"application/json"`              |
| `consumes`   | Accepted request content type(s)                     | `"application/json"`              |
| `headers`    | Required request headers                             | `"X-API-VERSION=1"`               |
| `params`     | Required query parameters                            | `"type=fiction"`                   |

### @RequestMapping with Multiple Paths and Methods

```java
// Maps to both /api/books AND /api/library/books
@RequestMapping(value = {"/api/books", "/api/library/books"})

// Maps to both GET and POST
@RequestMapping(value = "/api/books", method = {RequestMethod.GET, RequestMethod.POST})
```

---

## 3. HTTP Method Annotations — @GetMapping, @PostMapping, @PutMapping, @PatchMapping, @DeleteMapping

### What are They?

These are **shortcuts** for `@RequestMapping(method = ...)`. Instead of writing the verbose version, you use the specific annotation for each HTTP method.

### HTTP Methods & Their Purpose

```
┌──────────────────────────────────────────────────────────────────┐
│                 REST HTTP METHODS                                │
│                                                                  │
│  ┌──────────┐  ┌──────────────────────────────────────────────┐  │
│  │   GET    │  │ READ / Retrieve data                         │  │
│  │          │  │ • Idempotent (same result every time)        │  │
│  │          │  │ • Safe (doesn't modify data)                 │  │
│  │          │  │ • Cacheable                                  │  │
│  │          │  │ • No request body                            │  │
│  └──────────┘  └──────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────┐  ┌──────────────────────────────────────────────┐  │
│  │   POST   │  │ CREATE a new resource                        │  │
│  │          │  │ • NOT idempotent (creates new each time)     │  │
│  │          │  │ • Has request body (JSON data)               │  │
│  │          │  │ • Returns 201 Created                        │  │
│  └──────────┘  └──────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────┐  ┌──────────────────────────────────────────────┐  │
│  │   PUT    │  │ UPDATE entire resource (full replacement)    │  │
│  │          │  │ • Idempotent                                 │  │
│  │          │  │ • Has request body                           │  │
│  │          │  │ • Replaces ALL fields                        │  │
│  └──────────┘  └──────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────┐  ┌──────────────────────────────────────────────┐  │
│  │  PATCH   │  │ UPDATE partial resource (specific fields)    │  │
│  │          │  │ • Has request body (only changed fields)     │  │
│  │          │  │ • More efficient than PUT for small changes  │  │
│  └──────────┘  └──────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────┐  ┌──────────────────────────────────────────────┐  │
│  │  DELETE  │  │ DELETE a resource                             │  │
│  │          │  │ • Idempotent                                 │  │
│  │          │  │ • Usually no request body                    │  │
│  │          │  │ • Returns 204 No Content                     │  │
│  └──────────┘  └──────────────────────────────────────────────┘  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### CRUD Mapping to HTTP Methods

```
┌──────────────────────────────────────────────────────────────────┐
│            CRUD  ←→  HTTP METHODS  ←→  SPRING ANNOTATIONS        │
│                                                                  │
│   CREATE  ←→  POST    ←→  @PostMapping     ←→  POST /api/books  │
│   READ    ←→  GET     ←→  @GetMapping      ←→  GET  /api/books  │
│   UPDATE  ←→  PUT     ←→  @PutMapping      ←→  PUT  /api/books/1│
│   UPDATE  ←→  PATCH   ←→  @PatchMapping    ←→  PATCH/api/books/1│
│   DELETE  ←→  DELETE  ←→  @DeleteMapping   ←→  DEL  /api/books/1│
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Complete Code Example — All Methods

```java
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // ============ GET — Read ALL books ============
    // GET http://localhost:8080/api/books
    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.findAll();
    }

    // ============ GET — Read ONE book by ID ============
    // GET http://localhost:8080/api/books/5
    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Long id) {
        return bookService.findById(id);
    }

    // ============ POST — Create a new book ============
    // POST http://localhost:8080/api/books
    // Body: { "title": "Clean Code", "author": "Robert Martin", "isbn": "9780132350884" }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)   // Returns 201 instead of 200
    public Book createBook(@Valid @RequestBody Book book) {
        return bookService.save(book);
    }

    // ============ PUT — Update ENTIRE book ============
    // PUT http://localhost:8080/api/books/5
    // Body: { "title": "Clean Code 2nd Ed", "author": "Robert Martin", "isbn": "9780132350884" }
    @PutMapping("/{id}")
    public Book updateBook(@PathVariable Long id, @Valid @RequestBody Book book) {
        return bookService.update(id, book);
    }

    // ============ PATCH — Update PARTIAL book ============
    // PATCH http://localhost:8080/api/books/5
    // Body: { "title": "Clean Code 2nd Edition" }   ← only the changed field
    @PatchMapping("/{id}")
    public Book patchBook(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return bookService.partialUpdate(id, updates);
    }

    // ============ DELETE — Remove a book ============
    // DELETE http://localhost:8080/api/books/5
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)   // Returns 204
    public void deleteBook(@PathVariable Long id) {
        bookService.delete(id);
    }
}
```

### PUT vs PATCH — The Key Difference

```
┌──────────────────────────────────────────────────────────────────┐
│                  PUT vs PATCH                                    │
│                                                                  │
│   Current book in DB:                                            │
│   { "id": 1, "title": "Clean Code", "author": "Bob", "year": 2008 }
│                                                                  │
│   ──────────────── PUT /api/books/1 ────────────────             │
│   Request Body:                                                  │
│   { "title": "Clean Code 2e", "author": "Robert", "year": 2020 }│
│                                                                  │
│   Result: ALL fields replaced                                    │
│   { "id": 1, "title": "Clean Code 2e", "author": "Robert",      │
│     "year": 2020 }                                               │
│                                                                  │
│   ⚠️ If you forget a field, it becomes NULL!                     │
│   PUT Body: { "title": "Clean Code 2e" }                         │
│   Result:   { "id": 1, "title": "Clean Code 2e",                │
│              "author": NULL, "year": NULL }  ← DATA LOST!        │
│                                                                  │
│   ──────────────── PATCH /api/books/1 ──────────────             │
│   Request Body:                                                  │
│   { "title": "Clean Code 2e" }   ← only the changed field       │
│                                                                  │
│   Result: ONLY title updated, other fields PRESERVED             │
│   { "id": 1, "title": "Clean Code 2e", "author": "Bob",         │
│     "year": 2008 }                                               │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 4. @PathVariable

### What is @PathVariable?

`@PathVariable` extracts values from the **URL path** itself. It's used for resource identifiers like IDs, slugs, or usernames embedded in the URL.

### 💡 Real-World Analogy

Think of a **library shelf system**:
- `/shelf/3/book/42` = "Go to shelf 3, pick book 42"
- `3` and `42` are **path variables** — they're part of the address/path itself

### How It Works

```
┌──────────────────────────────────────────────────────────────────┐
│              @PathVariable — URL EXTRACTION                      │
│                                                                  │
│   URL: GET /api/books/42                                         │
│                       ──                                         │
│                        │                                         │
│   Mapping: @GetMapping("/{id}")                                  │
│                          ──                                      │
│                           │                                      │
│   Method: public Book getById(@PathVariable Long id)             │
│                                              ──                  │
│                                               │                  │
│                                               ▼                  │
│                                          id = 42                 │
│                                                                  │
│   Spring extracts "42" from URL, converts to Long, assigns to id│
└──────────────────────────────────────────────────────────────────┘
```

### Basic Usage

```java
// Single path variable
@GetMapping("/{id}")
public Book getBook(@PathVariable Long id) {
    return bookService.findById(id);
}
// GET /api/books/42  →  id = 42
```

### Multiple Path Variables

```java
// Multiple path variables
@GetMapping("/{category}/{id}")
public Book getBook(@PathVariable String category, @PathVariable Long id) {
    return bookService.findByCategoryAndId(category, id);
}
// GET /api/books/fiction/42  →  category = "fiction", id = 42
```

### Custom Variable Name

```java
// When path variable name differs from parameter name
@GetMapping("/{book-id}")
public Book getBook(@PathVariable("book-id") Long bookId) {
    return bookService.findById(bookId);
}
// GET /api/books/42  →  bookId = 42
// "book-id" can't be a Java variable name, so we map it explicitly
```

### Optional Path Variable

```java
// Optional path variable (Spring 4.3.3+)
@GetMapping({"/", "/{id}"})
public Object getBooks(@PathVariable(required = false) Long id) {
    if (id == null) {
        return bookService.findAll();      // GET /api/books/
    }
    return bookService.findById(id);       // GET /api/books/42
}
```

### Path Variable with Regex Validation

```java
// Only match numeric IDs
@GetMapping("/{id:\\d+}")
public Book getBook(@PathVariable Long id) {
    return bookService.findById(id);
}
// GET /api/books/42    →  ✅ Matches (digits)
// GET /api/books/abc   →  ❌ 404 (not digits)

// Only match ISBN format
@GetMapping("/isbn/{isbn:\\d{13}}")
public Book getByIsbn(@PathVariable String isbn) {
    return bookService.findByIsbn(isbn);
}
// GET /api/books/isbn/9780132350884  →  ✅ Matches (13 digits)
```

---

## 5. @RequestParam (Query Parameters)

### What is @RequestParam?

`@RequestParam` extracts values from **query parameters** — the key-value pairs after the `?` in a URL. Used for filtering, searching, sorting, and pagination.

### 💡 Real-World Analogy

Think of a **Google search**:
- `google.com/search?q=spring+boot&lang=en&page=2`
- `q`, `lang`, `page` are **query parameters** — they filter/modify the search
- The base URL (`/search`) stays the same; parameters customize the result

### @PathVariable vs @RequestParam

```
┌──────────────────────────────────────────────────────────────────┐
│         @PathVariable vs @RequestParam                           │
│                                                                  │
│   @PathVariable — Part of the URL PATH                           │
│   GET /api/books/42                                              │
│                 ──                                               │
│   Used for: Resource identification (ID, slug, name)             │
│   "WHICH book? → Book #42"                                      │
│                                                                  │
│   @RequestParam — After the ? (QUERY STRING)                     │
│   GET /api/books?author=Martin&sort=title&page=2                 │
│                  ─────────────────────────────────                │
│   Used for: Filtering, sorting, searching, pagination            │
│   "HOW to filter? → by author, sorted by title, page 2"         │
│                                                                  │
│   COMBINED:                                                      │
│   GET /api/categories/fiction/books?author=Martin&sort=title      │
│                       ───────       ────────────────────          │
│                     @PathVariable        @RequestParam            │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Basic Usage

```java
// Single query parameter
@GetMapping("/search")
public List<Book> searchBooks(@RequestParam String title) {
    return bookService.findByTitle(title);
}
// GET /api/books/search?title=Spring
// → title = "Spring"
```

### Multiple Query Parameters

```java
@GetMapping("/search")
public List<Book> searchBooks(
        @RequestParam String author,
        @RequestParam(required = false) String title,
        @RequestParam(defaultValue = "2000") int yearFrom
) {
    return bookService.search(author, title, yearFrom);
}
// GET /api/books/search?author=Martin&title=Clean&yearFrom=2010
// GET /api/books/search?author=Martin   ← title=null, yearFrom=2000 (default)
```

### All @RequestParam Options

```java
@GetMapping("/filter")
public List<Book> filterBooks(

    // Required (default) — request FAILS if missing
    @RequestParam String genre,

    // Optional — null if not provided
    @RequestParam(required = false) String author,

    // With default value — used if not provided
    @RequestParam(defaultValue = "title") String sortBy,

    // With default value for numeric types
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,

    // Custom parameter name mapping
    @RequestParam(name = "min-price") double minPrice,

    // List of values
    @RequestParam List<String> tags
    // GET /api/books/filter?tags=java&tags=spring&tags=boot

) {
    return bookService.filter(genre, author, sortBy, page, size, minPrice, tags);
}
```

### Pagination Example

```java
@GetMapping
public Page<Book> getAllBooks(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "asc") String direction
) {
    Sort sort = direction.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

    Pageable pageable = PageRequest.of(page, size, sort);
    return bookRepository.findAll(pageable);
}
// GET /api/books?page=0&size=5&sortBy=title&direction=asc
```

**Response:**
```json
{
    "content": [
        { "id": 1, "title": "Clean Code", "author": "Robert Martin" },
        { "id": 2, "title": "Effective Java", "author": "Joshua Bloch" }
    ],
    "totalElements": 50,
    "totalPages": 10,
    "number": 0,
    "size": 5,
    "first": true,
    "last": false
}
```

---

## 6. @RequestBody

### What is @RequestBody?

`@RequestBody` tells Spring to **read the HTTP request body** and **deserialize (convert) it from JSON into a Java object**. Used with POST, PUT, and PATCH methods to receive data from the client.

### 💡 Real-World Analogy

Imagine you're filling out a **form to register a new book** at the library. You write down the book details on paper (JSON) and hand it to the librarian. The librarian reads the form (`@RequestBody`), and converts your handwritten info into a structured record (Java object).

### How @RequestBody Works

```
┌──────────────────────────────────────────────────────────────────┐
│           @RequestBody — JSON → Java Object                      │
│                                                                  │
│   CLIENT sends:                                                  │
│   POST /api/books                                                │
│   Content-Type: application/json                                 │
│   Body:                                                          │
│   {                                                              │
│       "title": "Clean Code",                                     │
│       "author": "Robert Martin",                                 │
│       "isbn": "9780132350884",                                   │
│       "price": 29.99                                             │
│   }                                                              │
│        │                                                         │
│        ▼                                                         │
│   Spring receives the raw JSON string                            │
│        │                                                         │
│        ▼                                                         │
│   HttpMessageConverter (Jackson ObjectMapper)                    │
│   ┌──────────────────────────────────────┐                       │
│   │  JSON string → Book.class instance   │                       │
│   │                                      │                       │
│   │  Matches JSON keys to Java fields:   │                       │
│   │  "title"  → book.setTitle(...)       │                       │
│   │  "author" → book.setAuthor(...)      │                       │
│   │  "isbn"   → book.setIsbn(...)        │                       │
│   │  "price"  → book.setPrice(...)       │                       │
│   └──────────────────────────────────────┘                       │
│        │                                                         │
│        ▼                                                         │
│   Your method receives a fully populated Book object             │
│   public Book create(@RequestBody Book book) {                   │
│       // book.getTitle() == "Clean Code"                         │
│       // book.getAuthor() == "Robert Martin"                     │
│   }                                                              │
└──────────────────────────────────────────────────────────────────┘
```

### The Java Object (Entity/DTO)

```java
public class Book {

    private Long id;
    private String title;
    private String author;
    private String isbn;
    private double price;
    private int year;

    // Default constructor (REQUIRED for Jackson deserialization)
    public Book() {}

    public Book(String title, String author, String isbn, double price) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.price = price;
    }

    // Getters and Setters (REQUIRED for Jackson)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
}
```

### @RequestBody with @Valid — Input Validation

```java
// DTO (Data Transfer Object) with validation constraints
public class CreateBookRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @NotBlank(message = "Author is required")
    private String author;

    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "\\d{13}", message = "ISBN must be exactly 13 digits")
    private String isbn;

    @Positive(message = "Price must be positive")
    private double price;

    @Min(value = 1450, message = "Year must be after 1450")
    @Max(value = 2030, message = "Year must be reasonable")
    private int year;

    // Getters and setters...
}
```

```java
@PostMapping
public ResponseEntity<Book> createBook(@Valid @RequestBody CreateBookRequest request) {
    //  @Valid triggers validation BEFORE your method runs
    //  If validation fails → 400 Bad Request (automatic)
    Book book = bookService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(book);
}
```

**Validation error response (automatic with @Valid):**
```json
{
    "timestamp": "2026-04-25T10:00:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed",
    "errors": [
        { "field": "title", "message": "Title is required" },
        { "field": "isbn", "message": "ISBN must be exactly 13 digits" }
    ]
}
```

### Common Validation Annotations

| Annotation               | Description                              | Example                              |
| ------------------------ | ---------------------------------------- | ------------------------------------ |
| `@NotNull`               | Field must not be null                   | `@NotNull String name`               |
| `@NotBlank`              | String must not be null/empty/whitespace | `@NotBlank String title`             |
| `@NotEmpty`              | Collection/String must not be empty      | `@NotEmpty List<String> tags`        |
| `@Size(min, max)`        | String/Collection size limits            | `@Size(min=1, max=100) String name`  |
| `@Min(value)`            | Minimum numeric value                    | `@Min(0) int quantity`               |
| `@Max(value)`            | Maximum numeric value                    | `@Max(1000) int pages`               |
| `@Positive`              | Must be > 0                              | `@Positive double price`             |
| `@PositiveOrZero`        | Must be >= 0                             | `@PositiveOrZero int stock`          |
| `@Email`                 | Must be valid email format               | `@Email String email`                |
| `@Pattern(regexp)`       | Must match regex                         | `@Pattern(regexp="\\d{10}") String phone` |
| `@Past`                  | Date must be in the past                 | `@Past LocalDate birthDate`          |
| `@Future`                | Date must be in the future               | `@Future LocalDate dueDate`          |

---

## 7. @ResponseStatus

### What is @ResponseStatus?

`@ResponseStatus` sets the **HTTP status code** returned by a controller method or thrown by an exception. By default, successful responses return `200 OK` — this annotation overrides that default.

### Usage on Controller Methods

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)  // Returns 201 instead of 200
public Book createBook(@RequestBody Book book) {
    return bookService.save(book);
}

@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)  // Returns 204 (no body)
public void deleteBook(@PathVariable Long id) {
    bookService.delete(id);
}
```

### Usage on Exception Classes

```java
// When this exception is thrown, Spring automatically returns 404
@ResponseStatus(HttpStatus.NOT_FOUND)
public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(Long id) {
        super("Book not found with id: " + id);
    }
}

// When this exception is thrown, Spring returns 409 Conflict
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateIsbnException extends RuntimeException {

    public DuplicateIsbnException(String isbn) {
        super("A book with ISBN " + isbn + " already exists");
    }
}
```

```java
// Now in your service:
@Service
public class BookService {

    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
        // Throws BookNotFoundException → Spring returns 404 automatically!
    }
}
```

```
┌──────────────────────────────────────────────────────────────────┐
│         @ResponseStatus ON EXCEPTION — FLOW                      │
│                                                                  │
│   Client: GET /api/books/999                                     │
│        │                                                         │
│        ▼                                                         │
│   Controller → bookService.findById(999)                         │
│        │                                                         │
│        ▼                                                         │
│   Service → bookRepository.findById(999) → NOT FOUND             │
│        │                                                         │
│        ▼                                                         │
│   throw new BookNotFoundException(999)                            │
│        │                                                         │
│        ▼                                                         │
│   Spring sees: @ResponseStatus(HttpStatus.NOT_FOUND)             │
│        │                                                         │
│        ▼                                                         │
│   HTTP Response:                                                 │
│   ┌────────────────────────────────────┐                         │
│   │  Status: 404 Not Found            │                         │
│   │  Body: {                           │                         │
│   │    "timestamp": "...",             │                         │
│   │    "status": 404,                  │                         │
│   │    "error": "Not Found",           │                         │
│   │    "message": "Book not found      │                         │
│   │              with id: 999"         │                         │
│   │  }                                 │                         │
│   └────────────────────────────────────┘                         │
└──────────────────────────────────────────────────────────────────┘
```

---

## 8. ResponseEntity\<T\>

### What is ResponseEntity?

`ResponseEntity<T>` represents the **entire HTTP response** — you have full control over the **status code**, **headers**, and **body**. It's more flexible than `@ResponseStatus` because you can dynamically set different responses.

### 💡 Real-World Analogy

- `@ResponseStatus` = A **stamp machine** — always stamps the same status code.
- `ResponseEntity` = A **custom envelope** — you choose the envelope type (status), attach notes (headers), and put whatever content inside (body).

### @ResponseStatus vs ResponseEntity

```
┌──────────────────────────────────────────────────────────────────┐
│       @ResponseStatus vs ResponseEntity                          │
│                                                                  │
│   @ResponseStatus:                                               │
│   • Fixed status code (always the same)                          │
│   • Cannot set custom headers                                    │
│   • Cannot conditionally change the status                       │
│   • Simpler for straightforward cases                            │
│                                                                  │
│   ResponseEntity<T>:                                             │
│   • Dynamic status code (can change based on logic)              │
│   • Full control over headers                                    │
│   • Can return different status codes from same method           │
│   • Preferred for production REST APIs                           │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Basic ResponseEntity Usage

```java
@GetMapping("/{id}")
public ResponseEntity<Book> getBookById(@PathVariable Long id) {
    Book book = bookService.findById(id);

    if (book == null) {
        return ResponseEntity.notFound().build();          // 404
    }
    return ResponseEntity.ok(book);                        // 200 + body
}
```

### ResponseEntity Builder API — All Methods

```java
@RestController
@RequestMapping("/api/books")
public class BookController {

    // ============ 200 OK with body ============
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBook(@PathVariable Long id) {
        Book book = bookService.findById(id);
        return ResponseEntity.ok(book);
        // Same as: ResponseEntity.status(HttpStatus.OK).body(book)
    }

    // ============ 201 CREATED with Location header ============
    @PostMapping
    public ResponseEntity<Book> createBook(@Valid @RequestBody Book book) {
        Book savedBook = bookService.save(book);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(savedBook.getId())
                .toUri();
        // Location: http://localhost:8080/api/books/42

        return ResponseEntity
                .created(location)    // 201 + Location header
                .body(savedBook);
    }

    // ============ 204 NO CONTENT (no body) ============
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();   // 204
    }

    // ============ 400 BAD REQUEST ============
    @PostMapping("/validate")
    public ResponseEntity<String> validate(@RequestBody Book book) {
        if (book.getTitle() == null) {
            return ResponseEntity.badRequest().body("Title is required");
        }
        return ResponseEntity.ok("Valid");
    }

    // ============ Custom headers ============
    @GetMapping("/export")
    public ResponseEntity<List<Book>> exportBooks() {
        List<Book> books = bookService.findAll();

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(books.size()))
                .header("X-Export-Date", LocalDate.now().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(books);
    }

    // ============ Dynamic status based on logic ============
    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable Long id,
                                            @Valid @RequestBody Book book) {
        boolean exists = bookService.existsById(id);

        if (!exists) {
            // Create new (201 Created)
            Book created = bookService.save(book);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        }

        // Update existing (200 OK)
        Book updated = bookService.update(id, book);
        return ResponseEntity.ok(updated);
    }
}
```

### ResponseEntity Quick Reference

```
┌──────────────────────────────────────────────────────────────────┐
│           ResponseEntity BUILDER METHODS                         │
│                                                                  │
│   ResponseEntity.ok()                      → 200 OK              │
│   ResponseEntity.ok(body)                  → 200 OK + body       │
│   ResponseEntity.created(uri)              → 201 Created         │
│   ResponseEntity.accepted()                → 202 Accepted        │
│   ResponseEntity.noContent()               → 204 No Content     │
│   ResponseEntity.badRequest()              → 400 Bad Request     │
│   ResponseEntity.notFound()                → 404 Not Found       │
│   ResponseEntity.status(HttpStatus.XXX)    → Any custom status   │
│                                                                  │
│   Chain with:                                                    │
│   .header("key", "value")                  → Add header          │
│   .headers(httpHeaders)                    → Add multiple headers│
│   .contentType(MediaType.APPLICATION_JSON) → Set content type    │
│   .body(data)                              → Set response body   │
│   .build()                                 → Build with no body  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 9. HTTP Status Codes in Spring

### Status Code Categories

```
┌──────────────────────────────────────────────────────────────────┐
│               HTTP STATUS CODES — OVERVIEW                       │
│                                                                  │
│   1xx — INFORMATIONAL (rare in REST APIs)                        │
│   ┌──────────────────────────────────────────────────┐           │
│   │  100 Continue                                    │           │
│   │  101 Switching Protocols                         │           │
│   └──────────────────────────────────────────────────┘           │
│                                                                  │
│   2xx — SUCCESS ✅                                               │
│   ┌──────────────────────────────────────────────────┐           │
│   │  200 OK           — Request succeeded            │           │
│   │  201 Created      — Resource created             │           │
│   │  202 Accepted     — Processing (async)           │           │
│   │  204 No Content   — Success, no body (DELETE)    │           │
│   └──────────────────────────────────────────────────┘           │
│                                                                  │
│   3xx — REDIRECTION ↪                                            │
│   ┌──────────────────────────────────────────────────┐           │
│   │  301 Moved Permanently                           │           │
│   │  302 Found (temporary redirect)                  │           │
│   │  304 Not Modified (cache hit)                    │           │
│   └──────────────────────────────────────────────────┘           │
│                                                                  │
│   4xx — CLIENT ERROR ❌ (caller's mistake)                       │
│   ┌──────────────────────────────────────────────────┐           │
│   │  400 Bad Request       — Invalid input/JSON      │           │
│   │  401 Unauthorized      — Not authenticated       │           │
│   │  403 Forbidden         — Authenticated but       │           │
│   │                          not authorized          │           │
│   │  404 Not Found         — Resource doesn't exist  │           │
│   │  405 Method Not Allowed— Wrong HTTP method       │           │
│   │  409 Conflict          — Duplicate/state conflict│           │
│   │  422 Unprocessable     — Validation failed       │           │
│   │  429 Too Many Requests — Rate limit exceeded     │           │
│   └──────────────────────────────────────────────────┘           │
│                                                                  │
│   5xx — SERVER ERROR 🔥 (our mistake)                            │
│   ┌──────────────────────────────────────────────────┐           │
│   │  500 Internal Server Error — Unexpected error    │           │
│   │  502 Bad Gateway           — Upstream failure    │           │
│   │  503 Service Unavailable   — Server overloaded   │           │
│   └──────────────────────────────────────────────────┘           │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### REST API — Which Status Code to Use When?

| Operation              | Success Code          | Error Codes                        |
| ---------------------- | --------------------- | ---------------------------------- |
| `GET /books`           | `200 OK`              | `500 Internal Server Error`        |
| `GET /books/{id}`      | `200 OK`              | `404 Not Found`                    |
| `POST /books`          | `201 Created`         | `400 Bad Request`, `409 Conflict`  |
| `PUT /books/{id}`      | `200 OK`              | `400 Bad Request`, `404 Not Found` |
| `PATCH /books/{id}`    | `200 OK`              | `400 Bad Request`, `404 Not Found` |
| `DELETE /books/{id}`   | `204 No Content`      | `404 Not Found`                    |
| Auth required          | —                     | `401 Unauthorized`                 |
| Permission denied      | —                     | `403 Forbidden`                    |
| Validation failed      | —                     | `400 Bad Request` / `422`          |

### Spring HttpStatus Enum — Common Values

```java
// Success
HttpStatus.OK                    // 200
HttpStatus.CREATED               // 201
HttpStatus.ACCEPTED              // 202
HttpStatus.NO_CONTENT            // 204

// Client Errors
HttpStatus.BAD_REQUEST           // 400
HttpStatus.UNAUTHORIZED          // 401
HttpStatus.FORBIDDEN             // 403
HttpStatus.NOT_FOUND             // 404
HttpStatus.METHOD_NOT_ALLOWED    // 405
HttpStatus.CONFLICT              // 409
HttpStatus.UNPROCESSABLE_ENTITY  // 422
HttpStatus.TOO_MANY_REQUESTS     // 429

// Server Errors
HttpStatus.INTERNAL_SERVER_ERROR // 500
HttpStatus.BAD_GATEWAY           // 502
HttpStatus.SERVICE_UNAVAILABLE   // 503
```

---

## 10. Global Exception Handling — @ControllerAdvice & @ExceptionHandler

### The Problem: Exception Handling Without @ControllerAdvice

```java
// WITHOUT global handling — duplicated try-catch in EVERY controller method ❌
@GetMapping("/{id}")
public ResponseEntity<Book> getBook(@PathVariable Long id) {
    try {
        Book book = bookService.findById(id);
        return ResponseEntity.ok(book);
    } catch (BookNotFoundException e) {
        return ResponseEntity.notFound().build();
    } catch (Exception e) {
        return ResponseEntity.internalServerError().build();
    }
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
    try {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    } catch (BookNotFoundException e) {
        return ResponseEntity.notFound().build();          // DUPLICATE!
    } catch (Exception e) {
        return ResponseEntity.internalServerError().build(); // DUPLICATE!
    }
}
// Imagine doing this for 50+ endpoints... 😰
```

### The Solution: @ControllerAdvice + @ExceptionHandler

```
┌──────────────────────────────────────────────────────────────────┐
│        GLOBAL EXCEPTION HANDLING FLOW                            │
│                                                                  │
│   Client Request                                                 │
│        │                                                         │
│        ▼                                                         │
│   ┌──────────────┐                                               │
│   │  Controller   │── throws BookNotFoundException               │
│   └──────────────┘                                               │
│        │                                                         │
│        │  Exception propagates                                   │
│        ▼                                                         │
│   ┌──────────────────────────────────────────────┐               │
│   │  @ControllerAdvice (GlobalExceptionHandler)  │               │
│   │                                              │               │
│   │  Catches the exception and decides:          │               │
│   │  • What HTTP status code to return           │               │
│   │  • What error message to send                │               │
│   │  • What format to use (ErrorResponse)        │               │
│   │                                              │               │
│   │  @ExceptionHandler(BookNotFoundException)    │               │
│   │  → returns 404 + error details               │               │
│   │                                              │               │
│   │  @ExceptionHandler(ValidationException)      │               │
│   │  → returns 400 + field errors                │               │
│   │                                              │               │
│   │  @ExceptionHandler(Exception)                │               │
│   │  → returns 500 + generic message             │               │
│   └──────────────────────────┬───────────────────┘               │
│                              ▼                                   │
│   HTTP Response:                                                 │
│   {                                                              │
│       "status": 404,                                             │
│       "error": "Not Found",                                      │
│       "message": "Book not found with id: 999",                  │
│       "timestamp": "2026-04-25T10:30:00",                        │
│       "path": "/api/books/999"                                   │
│   }                                                              │
└──────────────────────────────────────────────────────────────────┘
```

### 💡 Real-World Analogy

- **Without @ControllerAdvice** = Every cashier in a store handles complaints differently — inconsistent customer experience.
- **With @ControllerAdvice** = One centralized **Customer Service desk** handles ALL complaints uniformly — same format, same tone, same resolution process.

### Step-by-Step Implementation

---

### Step 1: Define Custom Exceptions

```java
// 404 — Resource not found
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    // Getters
    public String getResourceName() { return resourceName; }
    public String getFieldName() { return fieldName; }
    public Object getFieldValue() { return fieldValue; }
}

// 409 — Duplicate resource
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}

// 400 — Bad request / business rule violation
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
```

---

### Step 2: Create a Standard Error Response DTO

```java
// This is the CONSISTENT error format sent to clients
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<FieldError> fieldErrors;  // For validation errors

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    // Getters and Setters...
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public List<FieldError> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(List<FieldError> fieldErrors) { this.fieldErrors = fieldErrors; }

    // Inner class for validation field errors
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;

        public FieldError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        // Getters...
        public String getField() { return field; }
        public String getMessage() { return message; }
        public Object getRejectedValue() { return rejectedValue; }
    }
}
```

---

### Step 3: Create the Global Exception Handler

```java
@RestControllerAdvice  // = @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {

    // ============ 404 — Resource Not Found ============
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        return new ErrorResponse(
                404,
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    // ============ 409 — Conflict (Duplicate) ============
    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        return new ErrorResponse(
                409,
                "Conflict",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    // ============ 400 — Bad Request ============
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request) {

        return new ErrorResponse(
                400,
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    // ============ 400 — Validation Errors (@Valid failed) ============
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.FieldError(
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()
                ))
                .toList();

        ErrorResponse response = new ErrorResponse(
                400,
                "Validation Failed",
                "One or more fields have invalid values",
                request.getRequestURI()
        );
        response.setFieldErrors(fieldErrors);
        return response;
    }

    // ============ 400 — Invalid JSON format ============
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        return new ErrorResponse(
                400,
                "Bad Request",
                "Invalid JSON format: " + ex.getMostSpecificCause().getMessage(),
                request.getRequestURI()
        );
    }

    // ============ 405 — Method Not Allowed ============
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        return new ErrorResponse(
                405,
                "Method Not Allowed",
                "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint. "
                        + "Supported methods: " + ex.getSupportedHttpMethods(),
                request.getRequestURI()
        );
    }

    // ============ 500 — Catch-All (Unexpected Errors) ============
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAllUnhandledExceptions(
            Exception ex,
            HttpServletRequest request) {

        // Log the full stack trace for debugging
        // log.error("Unhandled exception", ex);

        return new ErrorResponse(
                500,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
        // NEVER expose internal details (stack trace, SQL) to clients!
    }
}
```

---

### Step 4: Clean Controller (No Try-Catch!)

```java
@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getBook(@PathVariable Long id) {
        // No try-catch! If BookNotFoundException is thrown,
        // GlobalExceptionHandler catches it and returns 404.
        Book book = bookService.findById(id);
        return ResponseEntity.ok(book);
    }

    @PostMapping
    public ResponseEntity<Book> createBook(@Valid @RequestBody CreateBookRequest request) {
        // No try-catch! Validation errors → 400 (handled globally)
        // DuplicateIsbn → 409 (handled globally)
        Book book = bookService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(book);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        // No try-catch!
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### Service That Throws Exceptions

```java
@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", id));
        // → GlobalExceptionHandler catches → returns 404
    }

    public Book create(CreateBookRequest request) {
        // Check for duplicate ISBN
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new DuplicateResourceException(
                    "Book with ISBN " + request.getIsbn() + " already exists");
            // → GlobalExceptionHandler catches → returns 409
        }

        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        return bookRepository.save(book);
    }

    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book", "id", id);
        }
        bookRepository.deleteById(id);
    }
}
```

### Example Responses from GlobalExceptionHandler

**404 — Not Found:**
```json
{
    "timestamp": "2026-04-25T10:30:00",
    "status": 404,
    "error": "Not Found",
    "message": "Book not found with id: '999'",
    "path": "/api/books/999",
    "fieldErrors": null
}
```

**400 — Validation Failed:**
```json
{
    "timestamp": "2026-04-25T10:31:00",
    "status": 400,
    "error": "Validation Failed",
    "message": "One or more fields have invalid values",
    "path": "/api/books",
    "fieldErrors": [
        {
            "field": "title",
            "message": "Title is required",
            "rejectedValue": null
        },
        {
            "field": "isbn",
            "message": "ISBN must be exactly 13 digits",
            "rejectedValue": "123"
        }
    ]
}
```

**409 — Conflict:**
```json
{
    "timestamp": "2026-04-25T10:32:00",
    "status": 409,
    "error": "Conflict",
    "message": "Book with ISBN 9780132350884 already exists",
    "path": "/api/books",
    "fieldErrors": null
}
```

**500 — Internal Server Error:**
```json
{
    "timestamp": "2026-04-25T10:33:00",
    "status": 500,
    "error": "Internal Server Error",
    "message": "An unexpected error occurred. Please try again later.",
    "path": "/api/books",
    "fieldErrors": null
}
```

### @ControllerAdvice vs @RestControllerAdvice

```
┌──────────────────────────────────────────────────────────────────┐
│   @ControllerAdvice vs @RestControllerAdvice                     │
│                                                                  │
│   @ControllerAdvice                                              │
│   • Returns views (for MVC apps with Thymeleaf/JSP)              │
│   • Must add @ResponseBody on each method for JSON               │
│                                                                  │
│   @RestControllerAdvice                                          │
│   • = @ControllerAdvice + @ResponseBody                          │
│   • Every method automatically returns JSON                      │
│   • Use this for REST APIs (most common)                         │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Scope @ControllerAdvice to Specific Packages or Controllers

```java
// Apply only to controllers in a specific package
@RestControllerAdvice(basePackages = "com.library.controller")

// Apply only to specific controller classes
@RestControllerAdvice(assignableTypes = {BookController.class, MemberController.class})

// Apply only to controllers annotated with a specific annotation
@RestControllerAdvice(annotations = RestController.class)
```

---

## 11. Complete CRUD Example — Putting It All Together

### Project Structure

```
com.library.library_management
├── LibraryManagementApplication.java
├── controller/
│   └── BookController.java             @RestController
├── service/
│   └── BookService.java                @Service
├── repository/
│   └── BookRepository.java             @Repository (JpaRepository)
├── model/
│   └── Book.java                       @Entity
├── dto/
│   ├── CreateBookRequest.java          Request DTO with validation
│   └── UpdateBookRequest.java          Request DTO for updates
├── exception/
│   ├── ResourceNotFoundException.java  Custom 404 exception
│   ├── DuplicateResourceException.java Custom 409 exception
│   ├── BadRequestException.java        Custom 400 exception
│   ├── ErrorResponse.java              Standard error response DTO
│   └── GlobalExceptionHandler.java     @RestControllerAdvice
```

### The Flow — End to End

```
┌──────────────────────────────────────────────────────────────────┐
│              COMPLETE REQUEST-RESPONSE FLOW                      │
│                                                                  │
│   CLIENT                                                         │
│   POST /api/books                                                │
│   Body: { "title": "Clean Code", "author": "Robert Martin" }    │
│        │                                                         │
│        ▼                                                         │
│   ┌──────────────────────────────────────────────────┐           │
│   │  DispatcherServlet                               │           │
│   │  • Routes request to matching controller method  │           │
│   └──────────────────┬───────────────────────────────┘           │
│                      ▼                                           │
│   ┌──────────────────────────────────────────────────┐           │
│   │  Jackson (HttpMessageConverter)                  │           │
│   │  • Converts JSON body → CreateBookRequest object │           │
│   │  • @Valid triggers Bean Validation               │           │
│   └──────────────────┬───────────────────────────────┘           │
│                      ▼                                           │
│   ┌──────────────────────────────────────────────────┐           │
│   │  BookController.createBook()                     │           │
│   │  • Receives validated DTO                        │           │
│   │  • Calls bookService.create(request)             │           │
│   └──────────────────┬───────────────────────────────┘           │
│                      ▼                                           │
│   ┌──────────────────────────────────────────────────┐           │
│   │  BookService.create()                            │           │
│   │  • Business logic + validation                   │           │
│   │  • Calls bookRepository.save(book)               │           │
│   └──────────────────┬───────────────────────────────┘           │
│                      ▼                                           │
│   ┌──────────────────────────────────────────────────┐           │
│   │  BookRepository.save() (Spring Data JPA)         │           │
│   │  • Hibernate generates SQL                       │           │
│   │  • INSERT INTO books (...) VALUES (...)          │           │
│   └──────────────────┬───────────────────────────────┘           │
│                      ▼                                           │
│   ┌──────────────────────────────────────────────────┐           │
│   │  Response flows back up                          │           │
│   │  • Book entity → Jackson → JSON                  │           │
│   │  • ResponseEntity wraps status + headers + body  │           │
│   └──────────────────────────────────────────────────┘           │
│                      ▼                                           │
│   CLIENT receives:                                               │
│   HTTP 201 Created                                               │
│   Location: /api/books/42                                        │
│   { "id": 42, "title": "Clean Code", "author": "Robert Martin" }│
│                                                                  │
│   ─── IF EXCEPTION OCCURS ANYWHERE ───                           │
│                      ▼                                           │
│   ┌──────────────────────────────────────────────────┐           │
│   │  GlobalExceptionHandler                          │           │
│   │  • Catches exception                             │           │
│   │  • Maps to appropriate HTTP status + error body  │           │
│   │  • Returns consistent ErrorResponse              │           │
│   └──────────────────────────────────────────────────┘           │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 12. Common Mistakes & Troubleshooting

### ❌ Mistake 1: Returning void from GET endpoint

```java
// WRONG ❌ — returns 200 OK with empty body (confusing)
@GetMapping("/{id}")
public void getBook(@PathVariable Long id) {
    bookService.findById(id);  // result is thrown away!
}

// RIGHT ✅
@GetMapping("/{id}")
public Book getBook(@PathVariable Long id) {
    return bookService.findById(id);
}
```

---

### ❌ Mistake 2: Forgetting @RequestBody

```java
// WRONG ❌ — book will be null/empty (Spring doesn't know to read the body)
@PostMapping
public Book create(Book book) {
    return bookService.save(book);  // book fields are all null!
}

// RIGHT ✅
@PostMapping
public Book create(@RequestBody Book book) {
    return bookService.save(book);
}
```

---

### ❌ Mistake 3: Using @PathVariable for optional filters

```java
// WRONG ❌ — Path variables are for resource identifiers, not filters
@GetMapping("/{author}/{year}/{genre}")
public List<Book> search(@PathVariable String author,
                         @PathVariable int year,
                         @PathVariable String genre) { ... }

// RIGHT ✅ — Use @RequestParam for filters
@GetMapping("/search")
public List<Book> search(@RequestParam String author,
                         @RequestParam(required = false) Integer year,
                         @RequestParam(required = false) String genre) { ... }
```

---

### ❌ Mistake 4: Missing default constructor in DTO

```java
// WRONG ❌ — Jackson can't deserialize without a no-arg constructor
public class BookRequest {
    private String title;

    public BookRequest(String title) {  // Only parameterized constructor
        this.title = title;
    }
}
// Error: com.fasterxml.jackson.databind.exc.InvalidDefinitionException

// RIGHT ✅ — Add default constructor
public class BookRequest {
    private String title;

    public BookRequest() {}  // Required for Jackson

    public BookRequest(String title) {
        this.title = title;
    }

    // Getters and setters...
}
```

---

### ❌ Mistake 5: Exposing entity directly (no DTO)

```java
// RISKY ❌ — Exposes all entity fields including sensitive ones
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id);
    // Response includes: password, internalNotes, etc.!
}

// RIGHT ✅ — Use a DTO to control what's exposed
@GetMapping("/{id}")
public UserResponse getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    return new UserResponse(user.getId(), user.getName(), user.getEmail());
    // Only safe fields are returned
}
```

---

### ❌ Mistake 6: Returning 200 for POST (should be 201)

```java
// NOT IDEAL ❌ — 200 OK for creation
@PostMapping
public Book create(@RequestBody Book book) {
    return bookService.save(book);  // Returns 200 by default
}

// RIGHT ✅ — 201 Created with Location header
@PostMapping
public ResponseEntity<Book> create(@RequestBody Book book) {
    Book saved = bookService.save(book);
    URI location = URI.create("/api/books/" + saved.getId());
    return ResponseEntity.created(location).body(saved);  // 201
}
```

---

## 13. Interview Questions & Answers

### Q1: What is the difference between @Controller and @RestController?

**Answer:**
- `@Controller` returns **view names** (HTML templates). You need `@ResponseBody` on each method to return data as JSON.
- `@RestController` = `@Controller` + `@ResponseBody`. Every method automatically serializes return values to JSON and writes them to the HTTP response body.
- Use `@Controller` for server-side rendered apps (Thymeleaf/JSP). Use `@RestController` for REST APIs.

---

### Q2: What is the difference between @PathVariable and @RequestParam?

**Answer:**
- `@PathVariable` extracts values from the **URL path** (e.g., `/books/42` → `id=42`). Used for resource identification.
- `@RequestParam` extracts values from the **query string** (e.g., `/books?author=Martin` → `author="Martin"`). Used for filtering, sorting, pagination.
- Path variables are mandatory by default. Request params can be optional with `required=false` or `defaultValue`.

---

### Q3: What is the difference between PUT and PATCH?

**Answer:**
- `PUT` **replaces the entire resource**. All fields must be sent. Missing fields become null.
- `PATCH` **partially updates** a resource. Only the fields being changed need to be sent. Other fields remain unchanged.
- PUT is idempotent. PATCH may or may not be idempotent depending on implementation.

---

### Q4: What is ResponseEntity and when should you use it?

**Answer:**
- `ResponseEntity<T>` represents the complete HTTP response: status code, headers, and body.
- Use it when you need to:
  - Dynamically set different status codes based on logic
  - Add custom response headers
  - Return `201 Created` with a `Location` header
  - Return `204 No Content` with no body
- For simple cases where the status is always the same, `@ResponseStatus` is sufficient.

---

### Q5: How does @RequestBody work internally?

**Answer:**
- Spring's `DispatcherServlet` passes the request to an `HttpMessageConverter`.
- Since `spring-boot-starter-web` includes Jackson, the `MappingJackson2HttpMessageConverter` is used.
- Jackson's `ObjectMapper` deserializes the JSON string into a Java object by matching JSON keys to Java field names (via getters/setters).
- The Java object requires: a no-arg constructor and getters/setters (or public fields).
- If `@Valid` is present, Bean Validation runs before the method executes.

---

### Q6: What is @ControllerAdvice and why is it important?

**Answer:**
- `@ControllerAdvice` is a class-level annotation that allows you to handle exceptions **globally** across all controllers in one centralized place.
- Without it, you'd need try-catch blocks in every controller method.
- Benefits:
  - **DRY** — exception handling logic is written once
  - **Consistent** — all error responses follow the same format
  - **Clean controllers** — controllers only contain happy-path logic
  - **Maintainable** — change error format in one place, affects all endpoints
- `@RestControllerAdvice` = `@ControllerAdvice` + `@ResponseBody` (for REST APIs).

---

### Q7: What HTTP status code should POST return on success?

**Answer:**
- `201 Created` — not `200 OK`.
- The response should include:
  - The created resource in the body
  - A `Location` header pointing to the new resource's URL (e.g., `/api/books/42`)
- Using `200 OK` for creation is technically valid but violates REST conventions and makes the API less informative.

---

### Q8: How does validation work with @Valid and @RequestBody?

**Answer:**
- Add validation annotations (`@NotBlank`, `@Size`, `@Min`, etc.) to DTO fields.
- Add `@Valid` before `@RequestBody` in the controller method parameter.
- Spring validates the object before the method executes.
- If validation fails, Spring throws `MethodArgumentNotValidException` (status 400).
- You can customize the error response using `@ExceptionHandler(MethodArgumentNotValidException.class)` in a `@ControllerAdvice`.
- Requires `spring-boot-starter-validation` dependency.

---

### Q9: What is the difference between @RequestMapping and @GetMapping?

**Answer:**
- `@RequestMapping` is the general-purpose annotation that can map any HTTP method, headers, params, content types, etc.
- `@GetMapping` is a **shortcut** for `@RequestMapping(method = RequestMethod.GET)`.
- Similarly: `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping` are all shortcuts.
- Prefer the specific annotations for readability. Use `@RequestMapping` only at the class level for base paths or when you need advanced configuration.

---

### Q10: How do you handle 404 Not Found globally in Spring Boot?

**Answer:**
Three approaches:
1. **Custom exception + @ControllerAdvice:** Throw `ResourceNotFoundException`, catch it in `@ExceptionHandler`.
2. **@ResponseStatus on exception class:** Annotate `ResourceNotFoundException` with `@ResponseStatus(HttpStatus.NOT_FOUND)`.
3. **application.properties:** Set `spring.mvc.throw-exception-if-no-handler-found=true` and `spring.web.resources.add-mappings=false` to throw `NoHandlerFoundException` for unmapped URLs.

The first approach (custom exception + @ControllerAdvice) is the most flexible and widely used.

---

## Quick Reference Cheat Sheet

```
┌────────────────────────────────────────────────────────────────┐
│          REST APIs WITH SPRING BOOT — CHEAT SHEET              │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  @RestController  = @Controller + @ResponseBody (returns JSON) │
│  @RequestMapping  = Base path for all methods in a controller  │
│                                                                │
│  @GetMapping      = GET     = Read / Retrieve                  │
│  @PostMapping     = POST    = Create new resource              │
│  @PutMapping      = PUT     = Full update (replace)            │
│  @PatchMapping    = PATCH   = Partial update                   │
│  @DeleteMapping   = DELETE  = Remove resource                  │
│                                                                │
│  @PathVariable    = Extract from URL path    /books/{id}       │
│  @RequestParam    = Extract from query string ?key=value       │
│  @RequestBody     = Read JSON body → Java object               │
│  @Valid           = Trigger validation on @RequestBody          │
│                                                                │
│  @ResponseStatus  = Set fixed HTTP status code                 │
│  ResponseEntity<T>= Full control: status + headers + body      │
│                                                                │
│  Status Codes:                                                 │
│  200 OK | 201 Created | 204 No Content                         │
│  400 Bad Request | 401 Unauthorized | 403 Forbidden            │
│  404 Not Found | 409 Conflict | 500 Internal Server Error      │
│                                                                │
│  @RestControllerAdvice = Global exception handler              │
│  @ExceptionHandler     = Catch specific exception types        │
│                                                                │
│  POST → 201 Created + Location header                          │
│  DELETE → 204 No Content                                       │
│  GET/PUT/PATCH → 200 OK                                        │
│  Validation fail → 400 Bad Request                             │
│  Not found → 404 Not Found                                     │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

> **Author:** REST API with Spring Boot Study Notes  
> **Covers:** Spring Boot 3.x / Spring Framework 6.x / Java 17+
