# JDBC (Java Database Connectivity) — In‑Depth Notes

> **Goal of this document:** Teach JDBC from the ground up — the *theory* behind every concept, *ASCII diagrams* to visualize the flow, and *working Java code* for every idea.

---

## Table of Contents
1. [Big Picture & Theory](#1-big-picture--theory)
2. [JDBC Basics](#2-jdbc-basics)
3. [DriverManager, Connection, Statement](#3-drivermanager-connection-statement)
4. [PreparedStatement & SQL Injection](#4-preparedstatement--sql-injection)
5. [ResultSet — Deep Dive](#5-resultset--deep-dive)
6. [Transactions](#6-transactions)
7. [Connection Pooling (HikariCP)](#7-connection-pooling-hikaricp)
8. [JDBC in Spring Boot](#8-jdbc-in-spring-boot)
9. [Common Pitfalls & Best Practices](#9-common-pitfalls--best-practices)
10. [End‑to‑End Mini Project](#10-end-to-end-mini-project)

---

## 1. Big Picture & Theory

### 1.1 Why JDBC exists
Every relational database (MySQL, PostgreSQL, Oracle, SQL Server, H2, SQLite…) speaks its **own wire protocol**. Without a standard, every Java app would need DB‑specific code.

**JDBC** is Java's solution: a set of **interfaces** defined by the JDK (in `java.sql` / `javax.sql`). Each database vendor ships a **driver** — a JAR that *implements* those interfaces by translating Java calls into the DB's wire protocol.

So you write code against `Connection`, `Statement`, `ResultSet` — **not** against MySQL or Postgres. Swap the driver JAR + URL and the same code targets a different DB.

```
 Your code  ──►  JDBC interfaces  ──►  Vendor driver  ──►  Database
 (portable)      (java.sql.*)        (mysql-connector,    (wire protocol)
                                      postgresql, ...)
```

### 1.2 The Layered View
```
 ┌──────────────────────────────────────────────────────┐
 │  Application layer:                                  │
 │    Controllers / Services (Spring, plain Java, ...)  │
 ├──────────────────────────────────────────────────────┤
 │  Persistence layer (optional):                       │
 │    JPA/Hibernate, Spring Data, MyBatis, jOOQ         │
 ├──────────────────────────────────────────────────────┤
 │  JDBC API (java.sql, javax.sql)  ← the standard      │
 ├──────────────────────────────────────────────────────┤
 │  JDBC Driver (vendor jar) — implements the API       │
 ├──────────────────────────────────────────────────────┤
 │  Database server  (MySQL, Postgres, Oracle, …)       │
 └──────────────────────────────────────────────────────┘
```

### 1.3 Driver Types (historical)

| Type | Name | Description | Today |
|------|------|-------------|-------|
| 1 | JDBC‑ODBC bridge | Goes through native ODBC | ❌ obsolete |
| 2 | Native‑API | Native client libs (Oracle OCI) | rare |
| 3 | Network protocol | Talks to a middleware server | rare |
| 4 | **Thin / Pure Java** | Pure‑Java, talks DB protocol directly | ✅ **standard** |

Almost every driver you'll see today (MySQL Connector/J, PostgreSQL JDBC, Microsoft JDBC for SQL Server, etc.) is **Type 4**.

### 1.4 The JDBC Workflow — One‑Look Diagram
```
 ┌─────────────┐  1. load driver (auto since JDBC 4.0)
 │ Application │
 └──────┬──────┘
        │ 2. DriverManager.getConnection(url,user,pass)
        ▼
 ┌─────────────┐
 │ Connection  │  3. createStatement() / prepareStatement(sql)
 └──────┬──────┘
        │
        ▼
 ┌─────────────────────────┐ 4. executeQuery / executeUpdate
 │ Statement / Prepared    │
 └──────┬──────────────────┘
        │
        ▼
 ┌─────────────┐ 5. while(rs.next()) { rs.getX(...) }
 │ ResultSet   │
 └──────┬──────┘
        │ 6. close (rs → stmt → conn) — try‑with‑resources!
        ▼
   Resources freed
```

---

## 2. JDBC Basics

### 2.1 Required JAR (Maven)
```xml
<!-- MySQL -->
<dependency>
  <groupId>com.mysql</groupId>
  <artifactId>mysql-connector-j</artifactId>
  <version>8.4.0</version>
  <scope>runtime</scope>
</dependency>

<!-- PostgreSQL -->
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <version>42.7.3</version>
  <scope>runtime</scope>
</dependency>

<!-- H2 (in‑memory, great for learning) -->
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <version>2.2.224</version>
  <scope>runtime</scope>
</dependency>
```

> `runtime` scope = the driver isn't needed to compile (your code only references `java.sql.*`), only at runtime.

### 2.2 Auto‑Loading the Driver (JDBC 4.0+)
Driver JARs contain a file:
```
META-INF/services/java.sql.Driver
```
…which lists the driver class. The `ServiceLoader` mechanism discovers it on classpath, so:

```java
// ❌ Old way (still works, but unnecessary)
Class.forName("com.mysql.cj.jdbc.Driver");

// ✅ Modern way — just open the connection
Connection conn = DriverManager.getConnection(url, user, pass);
```

### 2.3 JDBC URL Anatomy
```
jdbc : subprotocol : subname

jdbc:mysql://localhost:3306/library?useSSL=false&serverTimezone=UTC
└──┘ └───┘  └──────────────┘└─────┘ └────────────────────────────┘
 │     │           │            │              params
 │     │       host:port      database
 │   driver/dialect
 protocol marker
```

Examples:
| DB | URL |
|----|-----|
| MySQL | `jdbc:mysql://localhost:3306/library` |
| PostgreSQL | `jdbc:postgresql://localhost:5432/library` |
| Oracle | `jdbc:oracle:thin:@//localhost:1521/XEPDB1` |
| SQL Server | `jdbc:sqlserver://localhost:1433;databaseName=library` |
| H2 in‑memory | `jdbc:h2:mem:testdb` |
| SQLite | `jdbc:sqlite:./library.db` |

### 2.4 A Complete "Hello DB" Program
```java
import java.sql.*;

public class HelloJdbc {
    public static void main(String[] args) {
        String url  = "jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1";
        String user = "sa";
        String pass = "";

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             Statement  stmt = conn.createStatement()) {

            // 1) DDL
            stmt.execute("""
                CREATE TABLE book (
                  id     IDENTITY PRIMARY KEY,
                  title  VARCHAR(120) NOT NULL,
                  author VARCHAR(80)
                )
            """);

            // 2) DML
            stmt.executeUpdate(
                "INSERT INTO book(title, author) VALUES "
              + "('Effective Java','Joshua Bloch'),"
              + "('Clean Code','Robert Martin')");

            // 3) Query
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT id, title, author FROM book ORDER BY id")) {
                while (rs.next()) {
                    System.out.printf("%d | %-20s | %s%n",
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("author"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

---

## 3. DriverManager, Connection, Statement

### 3.1 `DriverManager` — The Factory
- A **static class** that maintains a registry of `java.sql.Driver` instances.
- Iterates registered drivers and asks each one: *"can you handle this URL?"* The first that says *yes* is used to open a connection.

```
 DriverManager.getConnection("jdbc:mysql://...", user, pw)
        │
        │ asks each registered Driver: acceptsURL(url)?
        │
        ├─► MySQL Driver       ✓ yes  ──► returns Connection
        ├─► PostgreSQL Driver  ✗
        └─► Oracle Driver      ✗
```

Three overloads:
```java
DriverManager.getConnection(url);                       // creds in URL
DriverManager.getConnection(url, user, password);       // common
DriverManager.getConnection(url, propertiesObject);     // many params
```

### 3.2 `Connection` — A Session With the DB

Theory:
- Represents a single **TCP session** + authenticated DB user context.
- Backed by sockets and DB server resources → **expensive** to create.
- Is **not thread‑safe** — one connection per "unit of work."

Key methods:
```java
conn.createStatement();              // for static SQL
conn.prepareStatement(sql);          // for parameterized SQL
conn.prepareCall(sql);               // for stored procedures
conn.setAutoCommit(boolean);         // transaction control
conn.commit();
conn.rollback();
conn.setTransactionIsolation(level); // READ_COMMITTED, etc.
conn.getMetaData();                  // capabilities & schema info
conn.close();                        // return to pool / close socket
```

### 3.3 Inspecting Database Metadata
```java
try (Connection conn = DriverManager.getConnection(url, user, pass)) {
    DatabaseMetaData md = conn.getMetaData();
    System.out.println("DB: "      + md.getDatabaseProductName()
                   + " "           + md.getDatabaseProductVersion());
    System.out.println("Driver: "  + md.getDriverName()
                   + " "           + md.getDriverVersion());
    System.out.println("Max conns: "+ md.getMaxConnections());
}
```

### 3.4 `Statement` — Static SQL
A `Statement` sends a **fixed SQL string** to the DB each time.

| Method | Returns | Use case |
|--------|---------|----------|
| `executeQuery(sql)` | `ResultSet` | SELECT |
| `executeUpdate(sql)` | `int` (rows affected) | INSERT / UPDATE / DELETE / DDL |
| `execute(sql)` | `boolean` | Mixed / unknown result type |
| `addBatch(sql)` / `executeBatch()` | `int[]` | Batched DDL/DML |

Example — DDL + DML + Query:
```java
try (Connection conn = DriverManager.getConnection(url, user, pass);
     Statement st    = conn.createStatement()) {

    st.execute("CREATE TABLE IF NOT EXISTS member(" +
               "  id BIGINT PRIMARY KEY AUTO_INCREMENT," +
               "  name VARCHAR(100), joined DATE)");

    int rows = st.executeUpdate(
        "INSERT INTO member(name, joined) VALUES ('Alice', '2026-01-15')");

    System.out.println(rows + " row inserted");

    try (ResultSet rs = st.executeQuery("SELECT id, name FROM member")) {
        while (rs.next()) {
            System.out.println(rs.getLong("id") + " " + rs.getString("name"));
        }
    }
}
```

### 3.5 Why Plain `Statement` Is Dangerous
Anything from the user that you concatenate becomes **part of the SQL text**, so the DB parser cannot tell data from code.

```java
// 🚨 NEVER do this
String name = httpRequest.getParameter("name");
st.executeQuery("SELECT * FROM member WHERE name = '" + name + "'");
```
If `name = "' OR '1'='1"`, the query returns **everything**. Use `PreparedStatement` (§4) for any value that isn't a hard‑coded literal.

### 3.6 Object Lifecycle Diagram
```
 DriverManager  ──getConnection()──►  Connection
                                         │
                                         ├─ createStatement()      ─► Statement
                                         ├─ prepareStatement(sql)  ─► PreparedStatement
                                         └─ prepareCall(sql)       ─► CallableStatement
                                                     │
                                                     └─ executeQuery() ─► ResultSet

  close order (reverse):  ResultSet → Statement → Connection
```

---

## 4. PreparedStatement & SQL Injection

### 4.1 SQL Injection — How It Works (Theory)
The DB parser builds a **parse tree** from the SQL text. If user input is concatenated into that text, the user can **inject tokens** that change the tree's meaning.

```
Intended tree:                          Injected tree:
  SELECT                                   SELECT
    └─ FROM member                            └─ FROM member
        └─ WHERE name = 'alice'                   └─ WHERE name = ''
                                                       OR  '1' = '1'
                                                       --  (rest commented out)
```

Result: authentication bypass, data theft, `DROP TABLE`, etc.

### 4.2 PreparedStatement — How It Fixes It
A `PreparedStatement`:

1. **Pre‑compiles** the SQL with placeholders `?`. The parse tree is **fixed**.
2. Parameters are sent over the wire **separately**, as typed data, in a different protocol message.
3. The DB binds them into the *already‑parsed* tree — they can **never** become tokens.

```
            ┌─────────────────────────────────┐
SQL text ─► │ Parse + plan                    │  done ONCE
            │ "SELECT * FROM m WHERE name=?"  │
            └────────────────┬────────────────┘
                             │
   bind(1, "alice"); execute │       ◄──── data only, never code
                             ▼
                       Reused plan
```

### 4.3 Anatomy of a PreparedStatement Call
```java
String sql = "SELECT id, title FROM book WHERE author = ? AND year > ?";

try (PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setString(1, "Robert Martin");   // 1‑based index
    ps.setInt   (2, 2000);

    try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            System.out.printf("%d %s%n",
                rs.getInt("id"), rs.getString("title"));
        }
    }
}
```

Common setters:
| Setter | Java type | SQL type |
|--------|-----------|----------|
| `setString` | String | VARCHAR / TEXT |
| `setInt`, `setLong`, `setShort` | int/long/short | INTEGER / BIGINT |
| `setBoolean` | boolean | BOOLEAN |
| `setBigDecimal` | BigDecimal | DECIMAL |
| `setDouble` | double | DOUBLE |
| `setDate`, `setTimestamp` | java.sql.Date/Timestamp | DATE / TIMESTAMP |
| `setObject(i, value)` | any (incl. `LocalDate`, `LocalDateTime`) | inferred |
| `setNull(i, Types.X)` | — | NULL |
| `setBytes`, `setBlob`, `setClob` | byte[] / Blob / Clob | LOB |

### 4.4 Inserting & Getting the Generated Key
```java
String sql = "INSERT INTO book(title, author) VALUES (?, ?)";

try (PreparedStatement ps =
         conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

    ps.setString(1, "Effective Java");
    ps.setString(2, "Joshua Bloch");
    ps.executeUpdate();

    try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) {
            long id = keys.getLong(1);
            System.out.println("Inserted id = " + id);
        }
    }
}
```

### 4.5 Batch Inserts (Massive Speed‑Up)
Network round‑trips dominate JDBC performance. Batching groups N statements into 1 round trip.

```java
String sql = "INSERT INTO book(title, author) VALUES (?, ?)";
conn.setAutoCommit(false);

try (PreparedStatement ps = conn.prepareStatement(sql)) {
    int i = 0;
    for (Book b : books) {
        ps.setString(1, b.getTitle());
        ps.setString(2, b.getAuthor());
        ps.addBatch();
        if (++i % 500 == 0) ps.executeBatch();   // flush every 500
    }
    ps.executeBatch();   // flush remainder
    conn.commit();
} catch (SQLException e) {
    conn.rollback();
    throw e;
}
```

> For MySQL, also add `?rewriteBatchedStatements=true` to the JDBC URL — it rewrites N inserts into a single multi‑row insert.

### 4.6 Reusing the Same PreparedStatement
```java
String sql = "UPDATE book SET stock = stock - 1 WHERE id = ?";
try (PreparedStatement ps = conn.prepareStatement(sql)) {
    for (long id : borrowedIds) {
        ps.setLong(1, id);
        ps.executeUpdate();   // plan reused each time
    }
}
```

### 4.7 NULL Handling
```java
if (book.getYear() == null) ps.setNull(3, Types.INTEGER);
else                         ps.setInt (3, book.getYear());
```

Or simply:
```java
ps.setObject(3, book.getYear(), Types.INTEGER);  // null‑safe
```

### 4.8 Statement vs PreparedStatement — Summary
| Aspect | `Statement` | `PreparedStatement` |
|--------|-------------|----------------------|
| SQL text | Built fresh every call | Pre‑compiled once |
| Parameters | Concatenated as strings | Bound as typed values |
| SQL injection | ⚠️ Possible | ✅ Prevented |
| Performance (repeat) | Slower | Faster (plan cached) |
| Type safety | None | `setInt`, `setDate`, … |
| Use for | DDL, one‑off admin SQL | **Everything else** |

---

## 5. ResultSet — Deep Dive

### 5.1 Conceptual Model
A `ResultSet` is a **forward‑moving cursor** over rows produced by a query. It sits at one of three positions:
```
   beforeFirst    row 1    row 2    row 3    afterLast
       │                                         │
       └─────────────────►  next()  ─────────────►
```
Initial position is **beforeFirst**. The first `next()` moves it to row 1.

### 5.2 Basic Iteration Loop
```java
try (PreparedStatement ps = conn.prepareStatement(
        "SELECT id, title, author FROM book");
     ResultSet rs = ps.executeQuery()) {

    while (rs.next()) {
        long   id     = rs.getLong  ("id");
        String title  = rs.getString("title");
        String author = rs.getString("author");
        // ... do something
    }
}
```

### 5.3 Getter Methods — Full Reference

| Method | Java type | Typical SQL type |
|--------|-----------|------------------|
| `getBoolean` | boolean | BOOLEAN |
| `getByte` | byte | TINYINT |
| `getShort` | short | SMALLINT |
| `getInt` | int | INTEGER |
| `getLong` | long | BIGINT |
| `getFloat` | float | REAL |
| `getDouble` | double | DOUBLE |
| `getBigDecimal` | BigDecimal | DECIMAL / NUMERIC |
| `getString` | String | CHAR / VARCHAR / TEXT |
| `getBytes` | byte[] | BINARY / VARBINARY |
| `getDate` | java.sql.Date | DATE |
| `getTime` | java.sql.Time | TIME |
| `getTimestamp` | java.sql.Timestamp | TIMESTAMP |
| `getObject(col, Class)` | any (e.g. `LocalDate.class`) | (modern) |
| `getBlob` / `getClob` | Blob / Clob | LOB |

Both index (1‑based) and column‑name overloads exist:
```java
rs.getString(2);
rs.getString("title");
```

### 5.4 Modern `java.time` Mapping
```java
LocalDate     d   = rs.getObject("dob",        LocalDate.class);
LocalDateTime ldt = rs.getObject("created_at", LocalDateTime.class);
OffsetDateTime z  = rs.getObject("event_at",   OffsetDateTime.class);

ps.setObject(1, LocalDate.of(2026,1,1));
```
Avoid `getDate()` / `getTimestamp()` unless you must — they involve `java.util.Date` and timezone surprises.

### 5.5 NULL Handling — The Trap
For **primitive** getters, NULL returns the default value (`0`, `false`). To distinguish, call `wasNull()` **immediately after** the get:
```java
int qty = rs.getInt("qty");
boolean qtyWasNull = rs.wasNull();
```
Or use boxed types via `getObject`:
```java
Integer qty = rs.getObject("qty", Integer.class);
if (qty == null) { ... }
```

### 5.6 Scrollable & Updatable ResultSets
By default a `ResultSet` is **TYPE_FORWARD_ONLY** and **CONCUR_READ_ONLY**. You can ask for more:
```java
Statement st = conn.createStatement(
    ResultSet.TYPE_SCROLL_INSENSITIVE,
    ResultSet.CONCUR_UPDATABLE);

ResultSet rs = st.executeQuery("SELECT id, stock FROM book");
rs.last();                   // jump to last
int total = rs.getRow();
rs.beforeFirst();
while (rs.next()) {
    if (rs.getInt("stock") < 0) {
        rs.updateInt("stock", 0);
        rs.updateRow();      // pushes UPDATE to DB
    }
}
```

| Type | Meaning |
|------|---------|
| `TYPE_FORWARD_ONLY` | Only `next()` |
| `TYPE_SCROLL_INSENSITIVE` | Scrollable; ignores concurrent DB changes |
| `TYPE_SCROLL_SENSITIVE` | Scrollable; sees concurrent DB changes (driver permitting) |
| `CONCUR_READ_ONLY` | Read only |
| `CONCUR_UPDATABLE` | Allows `updateRow()`, `insertRow()`, `deleteRow()` |

### 5.7 Fetch Size — Streaming Big Results
By default the driver may load all rows into memory. For large queries:
```java
ps.setFetchSize(500);    // fetch in chunks of 500
```
On some drivers (e.g. PostgreSQL) you also need `conn.setAutoCommit(false)` to enable true streaming.

### 5.8 ResultSet Metadata
```java
ResultSetMetaData md = rs.getMetaData();
int cols = md.getColumnCount();
for (int i = 1; i <= cols; i++) {
    System.out.println(md.getColumnLabel(i) + " : " + md.getColumnTypeName(i));
}
```
Used to build generic row→Map mappers or to drive UI grids.

### 5.9 Mapping Rows to Domain Objects (Manual ORM)
```java
public final class BookMapper {
    public static Book map(ResultSet rs) throws SQLException {
        Book b = new Book();
        b.setId(rs.getLong("id"));
        b.setTitle(rs.getString("title"));
        b.setAuthor(rs.getString("author"));
        b.setPublishedOn(rs.getObject("published_on", LocalDate.class));
        return b;
    }
}

List<Book> all = new ArrayList<>();
try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM book");
     ResultSet rs = ps.executeQuery()) {
    while (rs.next()) all.add(BookMapper.map(rs));
}
```
Spring's `JdbcTemplate` + `RowMapper` is exactly this pattern, packaged.

### 5.10 Cursor Diagram with Code Mapping
```
  rs.executeQuery()
        │
        ▼
   beforeFirst
        │  rs.next() → true
        ▼
   ┌───────────────┐
   │ row 1         │  rs.getX(...) reads CURRENT row
   └───────────────┘
        │  rs.next() → true
        ▼
   ┌───────────────┐
   │ row 2         │
   └───────────────┘
        │  rs.next() → false
        ▼
    afterLast  →  loop exits
```

---

## 6. Transactions

### 6.1 Theory: ACID
- **A**tomicity — all or nothing.
- **C**onsistency — DB moves from one valid state to another.
- **I**solation — concurrent transactions don't see each other's intermediate state (level depends on setting).
- **D**urability — once committed, survives crash.

JDBC exposes transactions via the `Connection`:
- Default: **autoCommit = true** → each statement is its own transaction.
- For multi‑statement units, set autoCommit off and `commit()` / `rollback()` explicitly.

### 6.2 Diagram
```
 setAutoCommit(false)
        │
        ▼
   ┌────────────────────────────────────────┐
   │  stmt1                                 │
   │  stmt2          ALL OR NOTHING         │
   │  stmt3                                 │
   └────────────────┬───────────────────────┘
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
     commit()              rollback()
   (changes saved)       (changes discarded)
```

### 6.3 Code — Borrowing a Book Atomically
```java
try (Connection conn = ds.getConnection()) {
    conn.setAutoCommit(false);
    try (PreparedStatement decStock = conn.prepareStatement(
            "UPDATE book SET stock = stock - 1 WHERE id = ? AND stock > 0");
         PreparedStatement insBorrow = conn.prepareStatement(
            "INSERT INTO borrow(book_id, member_id, due) VALUES (?,?,?)")) {

        decStock.setLong(1, bookId);
        if (decStock.executeUpdate() != 1)
            throw new IllegalStateException("Out of stock");

        insBorrow.setLong  (1, bookId);
        insBorrow.setLong  (2, memberId);
        insBorrow.setObject(3, LocalDate.now().plusDays(14));
        insBorrow.executeUpdate();

        conn.commit();
    } catch (Exception ex) {
        conn.rollback();
        throw ex;
    } finally {
        conn.setAutoCommit(true);
    }
}
```

### 6.4 Isolation Levels
```java
conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
```
| Level | Dirty read | Non‑repeatable read | Phantom read |
|-------|:---------:|:-------------------:|:------------:|
| `READ_UNCOMMITTED` | allowed | allowed | allowed |
| `READ_COMMITTED` (typical default) | prevented | allowed | allowed |
| `REPEATABLE_READ` (MySQL default) | prevented | prevented | allowed |
| `SERIALIZABLE` | prevented | prevented | prevented |

### 6.5 Savepoints
```java
Savepoint sp = conn.setSavepoint("after-insert");
// ... more work
conn.rollback(sp);   // partial rollback
```

---

## 7. Connection Pooling (HikariCP)

### 7.1 Why You Need It

Opening a new JDBC connection is expensive:

| Step | Typical cost |
|------|--------------|
| TCP handshake | ~1 RTT |
| TLS handshake (if used) | 1–2 RTT |
| DB authentication | parse + check |
| Session setup (locale, vars) | a few queries |
| **Total** | **50–500 ms** |

Per‑request opening = unusable for a web app.

### 7.2 Theory: What a Pool Does

A **connection pool** is a fixed (or bounded) set of pre‑opened, authenticated connections kept warm in memory. Code "borrows" one, runs SQL, and "returns" it (logically `close()` → pool).

```
WITHOUT POOL                         WITH POOL
─────────────                         ─────────
 req ─► open ─► auth ─► sql ─► close   req ─► borrow ─► sql ─► return
       ▲___ slow every time ___▲             ▲___ μs ___▲
```

### 7.3 HikariCP at a Glance
- The **fastest, lightest** JDBC pool in Java.
- Default in **Spring Boot** (auto‑configured when `spring-jdbc` is on the classpath).
- Implements `javax.sql.DataSource`.

### 7.4 Internals (Simplified)
```
                ┌─────────────────────────────────────────┐
                │           HikariDataSource              │
                │                                         │
   getConnection│      ┌──────────────────────────────┐   │
   ────────────►│      │  ConcurrentBag (idle queue)  │   │
                │      │  [c1 idle][c2 idle][c3 idle] │   │
                │      └──────────────┬───────────────┘   │
                │                     │ borrow            │
                │              ┌──────▼──────┐            │
                │              │ in‑use set  │            │
                │              └──────┬──────┘            │
                │                     │ close() → return  │
                │      ┌──────────────▼───────────────┐   │
                │      │  HouseKeeper (timer thread)  │   │
                │      │  - evict idle > idleTimeout  │   │
                │      │  - retire age  > maxLifetime │   │
                │      │  - leak detection            │   │
                │      └──────────────────────────────┘   │
                └─────────────────────────────────────────┘
```

When you call `connection.close()` on a *pooled* connection, Hikari **proxies** the call and instead **resets** the connection (clear warnings, rollback open txn, restore autoCommit/iso) and puts it back in the bag.

### 7.5 Configuration — Spring Boot (`application.properties`)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/library
spring.datasource.username=root
spring.datasource.password=secret
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Hikari‑specific
spring.datasource.hikari.pool-name=LibraryHikariPool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.leak-detection-threshold=20000
spring.datasource.hikari.connection-test-query=SELECT 1
```

### 7.6 Key Settings — Theory + Recommendation

| Setting | What it does | Typical |
|---------|--------------|---------|
| `maximumPoolSize` | Hard cap on total conns. | `coreCount * 2` (Hikari's own guidance). **Don't over‑size.** |
| `minimumIdle` | Min kept warm. | Set = `maximumPoolSize` for steady latency. |
| `connectionTimeout` | Max wait when borrowing. | 30s default. |
| `idleTimeout` | Evict idle conns older than this (only above `minimumIdle`). | 10 min. |
| `maxLifetime` | Retire a connection after this age. Helps DB‑side timeouts. | 30 min. |
| `leakDetectionThreshold` | Logs stack trace of borrower if not returned within X ms. | 20 s in dev. |
| `connectionTestQuery` | Validation query for old JDBC drivers. Modern drivers use `isValid()` automatically. | usually leave unset. |

> **Pool sizing myth:** more is not better. Each open connection occupies DB‑side memory & a worker. A small pool with a queue often beats a huge pool because the DB itself becomes the bottleneck.

### 7.7 Programmatic Setup (no Spring)
```java
HikariConfig cfg = new HikariConfig();
cfg.setJdbcUrl("jdbc:mysql://localhost:3306/library");
cfg.setUsername("root");
cfg.setPassword("secret");
cfg.setMaximumPoolSize(10);
cfg.setMinimumIdle(10);
cfg.setPoolName("LibraryPool");

try (HikariDataSource ds = new HikariDataSource(cfg)) {
    try (Connection conn = ds.getConnection();
         PreparedStatement ps = conn.prepareStatement("SELECT 1")) {
        ps.executeQuery();
    }
}
```

### 7.8 Lifecycle of a Pooled Connection
```
 (1) Hikari opens N physical conns at startup
            │
            ▼
 (2) App calls ds.getConnection()
            │   returns a PROXY connection
            ▼
 (3) App runs SQL: ps.executeQuery(...)
            │
            ▼
 (4) App calls conn.close()
            │   → proxy intercepts:
            │     - rollback uncommitted txn
            │     - reset autoCommit / iso / catalog
            │     - clear warnings
            │   → returns physical conn to pool
            ▼
 (5) Housekeeper evicts when age > maxLifetime
     or idle > idleTimeout (subject to minimumIdle)
```

---

## 8. JDBC in Spring Boot

### 8.1 Auto‑Configuration Order
Spring Boot picks a pool from classpath in this order:
1. **HikariCP** ✅ (default; bundled with `spring-boot-starter-jdbc`)
2. Tomcat JDBC Pool
3. Apache Commons DBCP2
4. Oracle UCP

Override with `spring.datasource.type=com.zaxxer.hikari.HikariDataSource` (or any other).

### 8.2 What Gets Wired
With `spring-boot-starter-jdbc` (or `data-jpa`) + a driver on the classpath:
- A `DataSource` bean → a `HikariDataSource`.
- A `JdbcTemplate` bean → uses that `DataSource`.
- A `PlatformTransactionManager` (`DataSourceTransactionManager`) for `@Transactional`.

### 8.3 `JdbcTemplate` — JDBC Without the Boilerplate
```java
@Repository
public class BookDao {
    private final JdbcTemplate jdbc;

    public BookDao(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final RowMapper<Book> MAPPER = (rs, n) -> new Book(
        rs.getLong("id"),
        rs.getString("title"),
        rs.getString("author"));

    public List<Book> findAll() {
        return jdbc.query("SELECT id, title, author FROM book", MAPPER);
    }

    public Optional<Book> findById(long id) {
        return jdbc.query(
                "SELECT id, title, author FROM book WHERE id = ?",
                MAPPER, id)
            .stream().findFirst();
    }

    public int insert(Book b) {
        return jdbc.update(
            "INSERT INTO book(title, author) VALUES (?, ?)",
            b.getTitle(), b.getAuthor());
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM book WHERE id = ?", id);
    }
}
```
Under the hood `JdbcTemplate`:
1. Borrows a connection from the Hikari pool.
2. Creates a `PreparedStatement`.
3. Binds parameters.
4. Runs the query, mapping rows via your `RowMapper`.
5. Closes the statement and returns the connection to the pool.

### 8.4 Declarative Transactions
```java
@Service
public class BorrowService {
    private final BookDao   books;
    private final BorrowDao borrows;

    public BorrowService(BookDao b, BorrowDao br) {
        this.books = b; this.borrows = br;
    }

    @Transactional   // commit on return; rollback on RuntimeException
    public void borrow(long bookId, long memberId) {
        int updated = books.decrementStock(bookId);
        if (updated != 1) throw new IllegalStateException("Out of stock");
        borrows.create(bookId, memberId, LocalDate.now().plusDays(14));
    }
}
```

### 8.5 End‑to‑End Request Path
```
HTTP request
   │
   ▼
@RestController
   │ calls
   ▼
@Service (transaction starts via AOP)
   │ calls
   ▼
@Repository (JdbcTemplate / JPA repository)
   │ borrows conn from
   ▼
HikariCP pool
   │ runs SQL via
   ▼
JDBC driver
   │ over TCP
   ▼
Database
```

---

## 9. Common Pitfalls & Best Practices

### 9.1 Resource Leaks
**Always** use try‑with‑resources. `Connection`, `Statement`, `PreparedStatement`, `ResultSet` all implement `AutoCloseable`.

```java
try (Connection conn = ds.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql);
     ResultSet rs = ps.executeQuery()) {
    // ...
}   // closed in reverse declaration order — exactly right
```

### 9.2 Don't Hold Connections Across User Think‑Time
A pooled connection should be borrowed → used → returned within a single short operation. Never hold one open while waiting on a UI event, an HTTP call, or a file upload.

### 9.3 N+1 Queries
Looping over a parent list and running a child query per row → kills performance. Fetch with one JOIN instead, or batch lookup with `IN (...)`.

### 9.4 String Concatenation in SQL
Never. Use `?` placeholders. Even when "it's just an int" — habits matter.

### 9.5 Forgetting `setAutoCommit(false)` for Multi‑Statement Units
Otherwise each statement self‑commits and there's no rollback safety net.

### 9.6 Wrong Pool Size
- Too small → threads block waiting for a connection.
- Too large → DB CPU/memory thrashing.
- Start with `cores * 2`, measure, adjust.

### 9.7 Time Zone Pitfalls
Use `LocalDate` / `LocalDateTime` / `OffsetDateTime` via `getObject` / `setObject`. Avoid `java.util.Date`. Set `serverTimezone=UTC` for MySQL.

### 9.8 Checked Exception Hell
Plain JDBC throws `SQLException` everywhere. Wrap with Spring's `JdbcTemplate` (which converts to `DataAccessException` — unchecked) or a small helper.

---

## 10. End‑to‑End Mini Project

A self‑contained program demonstrating: pool, prepared statements, transactions, batch, RowMapper.

```java
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class LibraryDemo {

    /* ---------- Domain ---------- */
    record Book(Long id, String title, String author, int stock) {}

    /* ---------- DataSource (Hikari) ---------- */
    static DataSource buildDataSource() {
        HikariConfig c = new HikariConfig();
        c.setJdbcUrl("jdbc:h2:mem:lib;DB_CLOSE_DELAY=-1");
        c.setUsername("sa");
        c.setPassword("");
        c.setMaximumPoolSize(5);
        c.setPoolName("LibDemoPool");
        return new HikariDataSource(c);
    }

    /* ---------- Schema ---------- */
    static void initSchema(DataSource ds) throws SQLException {
        try (Connection conn = ds.getConnection();
             Statement  st   = conn.createStatement()) {
            st.execute("""
                CREATE TABLE book (
                    id     IDENTITY PRIMARY KEY,
                    title  VARCHAR(120) NOT NULL,
                    author VARCHAR(80),
                    stock  INT NOT NULL DEFAULT 0
                )""");
        }
    }

    /* ---------- DAO operations ---------- */
    static long insertBook(DataSource ds, String title, String author, int stock)
            throws SQLException {
        String sql = "INSERT INTO book(title, author, stock) VALUES (?,?,?)";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, author);
            ps.setInt   (3, stock);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    static void batchInsert(DataSource ds, List<Book> books) throws SQLException {
        String sql = "INSERT INTO book(title, author, stock) VALUES (?,?,?)";
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Book b : books) {
                    ps.setString(1, b.title());
                    ps.setString(2, b.author());
                    ps.setInt   (3, b.stock());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    static List<Book> findByAuthor(DataSource ds, String author) throws SQLException {
        String sql = "SELECT id, title, author, stock FROM book WHERE author = ?";
        List<Book> result = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, author);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Book(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("stock")));
                }
            }
        }
        return result;
    }

    /** Atomic "borrow": decrement stock + (pretend) insert borrow record. */
    static void borrow(DataSource ds, long bookId) throws SQLException {
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement dec = conn.prepareStatement(
                    "UPDATE book SET stock = stock - 1 " +
                    "WHERE id = ? AND stock > 0")) {
                dec.setLong(1, bookId);
                int n = dec.executeUpdate();
                if (n != 1) throw new SQLException("Book unavailable");
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    /* ---------- Main ---------- */
    public static void main(String[] args) throws Exception {
        DataSource ds = buildDataSource();
        initSchema(ds);

        long id = insertBook(ds, "Effective Java", "Joshua Bloch", 3);
        System.out.println("Inserted id=" + id);

        batchInsert(ds, List.of(
            new Book(null, "Clean Code",      "Robert Martin", 2),
            new Book(null, "The Pragmatic Programmer", "Hunt & Thomas", 4),
            new Book(null, "Refactoring",     "Martin Fowler", 1)));

        borrow(ds, id);

        findByAuthor(ds, "Joshua Bloch")
            .forEach(b -> System.out.println("Found: " + b));

        ((HikariDataSource) ds).close();
    }
}
```

Run it and you exercise:
- **HikariCP** pool (`getConnection()` is microseconds after warm‑up).
- **PreparedStatement** with positional params + generated keys.
- **Batch insert** inside a manual transaction.
- **Atomic update** with rollback on failure.
- **ResultSet** iteration with proper resource management.

---

## TL;DR Summary

- **JDBC** is Java's standard, driver‑backed API for talking to relational DBs.
- **DriverManager** → produces a **Connection** (a DB session).
- **Statement** runs static SQL; **PreparedStatement** runs parameterized SQL — **always prefer the latter** for safety and speed.
- **ResultSet** is a forward cursor; iterate with `next()`, read with `getX(...)`, close it (use try‑with‑resources).
- **Transactions**: `setAutoCommit(false)` → `commit()` / `rollback()` to make multi‑step changes atomic.
- **Connection pooling** (**HikariCP**, Spring Boot's default) makes JDBC fast enough for real apps by reusing pre‑opened connections.
- In Spring Boot you usually use `JdbcTemplate` (or JPA) — both sit on top of this same JDBC + Hikari stack.
