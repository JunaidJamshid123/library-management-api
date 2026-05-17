# Hibernate — Deep‑Dive Notes (v3)

> **Goal:** A complete, intuition‑first guide to Hibernate.
> Each section explains: **(1) the *why* (theory)**, **(2) the *picture* (ASCII diagrams)**, **(3) the *how* (code)**, and **(4) gotchas**.

---

## Table of Contents
1. [ORM — The Impedance Mismatch](#1-orm--the-impedance-mismatch)
2. [Hibernate vs JDBC](#2-hibernate-vs-jdbc)
3. [Hibernate Architecture](#3-hibernate-architecture)
4. [SessionFactory & Session — The Two Pillars](#4-sessionfactory--session--the-two-pillars)
5. [Entity States — The Lifecycle](#5-entity-states--the-lifecycle)
6. [Lazy vs Eager Loading](#6-lazy-vs-eager-loading)
7. [The N+1 Problem & Solutions](#7-the-n1-problem--solutions)
8. [Caches — L1, L2, Query Cache](#8-caches--l1-l2-query-cache)
9. [Transactions, Flush Modes & Locking](#9-transactions-flush-modes--locking)
10. [Hibernate in Spring Boot](#10-hibernate-in-spring-boot)
11. [Best Practices & Mental Models](#11-best-practices--mental-models)
12. [TL;DR](#tldr)

---

## 1. ORM — The Impedance Mismatch

### 1.1 The Core Problem (Theory)

Java code reasons in **graphs of typed objects** with reference identity, inheritance, polymorphism, and direct navigation (`book.getAuthor().getName()`).
SQL reasons in **flat, untyped tables** identified by primary keys, joined by foreign keys, with no inheritance and no navigation — only set algebra.

This gap is called the **object–relational impedance mismatch**. Six fundamental conflicts:

```
   ┌─────────────────┬─────────────────────────┬───────────────────────────┐
   │ Concern         │ Object world (Java)     │ Relational world (SQL)    │
   ├─────────────────┼─────────────────────────┼───────────────────────────┤
   │ Identity        │ reference (==)          │ primary key value         │
   │ Granularity     │ many small classes      │ few wide tables           │
   │ Inheritance     │ extends, polymorphism   │ none (must simulate)      │
   │ Relationships   │ object references       │ foreign keys + JOINs      │
   │ Navigation      │ a.b.c.d (path)          │ JOIN ... ON ...           │
   │ Data types      │ enums, generics, null   │ INTEGER, VARCHAR, NULL    │
   └─────────────────┴─────────────────────────┴───────────────────────────┘
```

### 1.2 The Two Worlds Side‑by‑Side

```
        OBJECT WORLD                                  RELATIONAL WORLD
 ┌──────────────────────────────┐            ┌─────────────────────────────┐
 │  class Book {                │            │  TABLE book                 │
 │     Long      id;            │            │  ─────────────────────────  │
 │     String    title;         │            │  id        BIGINT  PK       │
 │     Author    author;  ──┐   │            │  title     VARCHAR          │
 │     List<Tag> tags;      │   │            │  author_id BIGINT  FK ─┐    │
 │  }                       │   │            └────────────────────────┼────┘
 │                          │   │                                     │
 │  class Author {          │   │            ┌────────────────────────▼────┐
 │     Long     id;         └──►│            │  TABLE author               │
 │     String   name;           │            │  id   BIGINT PK             │
 │     List<Book> books;        │            │  name VARCHAR               │
 │  }                           │            └─────────────────────────────┘
 │                              │            ┌─────────────────────────────┐
 │  class Tag { … }             │            │  TABLE book_tag  (join)     │
 │                              │            │  book_id  FK                │
 └──────────────────────────────┘            │  tag_id   FK                │
                                              └─────────────────────────────┘
   identity: reference                          identity: PK value
   navigation: a.b.c                            navigation: JOIN .. ON ..
```

### 1.3 What an ORM Promises

An **ORM** is a layer that automates the round‑trip between these two worlds:

```
   ┌─────────── you write ───────────┐    ┌──────── ORM handles ────────┐
   │ @Entity classes + annotations   │ →  │ SQL generation              │
   │ session.persist(book)           │ →  │ INSERT INTO book ...        │
   │ book.setTitle("…")              │ →  │ dirty check → UPDATE        │
   │ book.getAuthor().getName()      │ →  │ lazy SELECT FROM author     │
   │ session.remove(book)            │ →  │ DELETE FROM book ...        │
   └─────────────────────────────────┘    └─────────────────────────────┘
```

Eight responsibilities of an ORM:
1. **Mapping** classes ↔ tables (declarative).
2. **CRUD generation** — `persist/find/remove`.
3. **Identity map** — same row ⇒ same Java object inside one session.
4. **Dirty checking** — UPDATE generated automatically when fields change.
5. **Relationship handling** — `@ManyToOne` / `@OneToMany` translate to FKs/JOINs.
6. **Lazy loading** — proxies defer SELECTs until first access.
7. **Transactions** — orchestrate JDBC commit/rollback.
8. **Portability** — dialects translate to MySQL/Postgres/Oracle SQL.

### 1.4 Where Hibernate Sits in the Stack

```
 ┌──────────────────────────────────────────────────────────────┐
 │  Spring Data JPA          (repositories, derived queries)    │  high
 ├──────────────────────────────────────────────────────────────┤
 │  JPA (Jakarta Persistence)  (spec: EntityManager, @Entity)   │  contract
 ├──────────────────────────────────────────────────────────────┤
 │  Hibernate ORM      (engine: Session, HQL, dialect, caches)  │  engine
 ├──────────────────────────────────────────────────────────────┤
 │  JDBC + HikariCP    (driver, connection pool, raw SQL)       │  plumbing
 ├──────────────────────────────────────────────────────────────┤
 │  Database                                                    │
 └──────────────────────────────────────────────────────────────┘
```
- **JPA** = interface (contract).
- **Hibernate** = the most popular implementation.
- **Spring Data JPA** = repository convenience layer *on top of* JPA.

---

## 2. Hibernate vs JDBC

### 2.1 Same Task, Two Worlds

**JDBC — manual everything:**
```java
String sql = "SELECT b.id, b.title, a.id, a.name " +
             "FROM book b JOIN author a ON b.author_id = a.id WHERE b.id = ?";
try (Connection c = ds.getConnection();
     PreparedStatement ps = c.prepareStatement(sql)) {
    ps.setLong(1, id);
    try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
            Author a = new Author(rs.getLong(3), rs.getString(4));
            Book   b = new Book(rs.getLong(1), rs.getString(2), a);
        }
    }
}
```

**Hibernate — declarative:**
```java
Book b = em.find(Book.class, id);
String authorName = b.getAuthor().getName();      // loaded transparently
```

### 2.2 Where the Effort Goes

```
        JDBC                                  HIBERNATE
   ─────────────                          ────────────────
   80% writing SQL & row mapping         80% modeling entities & relationships
   15% transaction boilerplate           10% tuning fetches/caches
    5% business logic                    10% business logic

   ⊕ tight control                       ⊕ productivity
   ⊕ predictable performance              ⊕ dirty checking, caching
   ⊖ verbose, repetitive                 ⊖ hidden SQL (must learn to debug)
                                          ⊖ N+1, LazyInit, merge gotchas
```

### 2.3 Round‑trip Flow Comparison

```
 JDBC                                  HIBERNATE
 ─────                                 ─────────
 you ──► SQL ──► driver ──► DB         you ──► API call ──► mapping engine
       (build, bind, send)                     (generate SQL, bind, send)
                                                       │
 ResultSet ◄──── rows                                  │
 ──► your row→object code ─►            ─► automatic row→entity mapping
                                                       │
 commit() yourself                       ─► dirty‑check, flush, commit

```

### 2.4 Comparison Matrix

| Aspect | Plain JDBC | Hibernate |
|--------|-----------|-----------|
| API level | SQL strings, `ResultSet` | Domain objects |
| SQL | You write | Generated (dialect aware) |
| Row → object | Manual `RowMapper` | Automatic |
| Relationships | Hand‑joined | `@ManyToOne` / `@OneToMany` |
| Caching | None | L1 (always), L2 (opt), query cache |
| Change detection | Manual UPDATE | Dirty checking |
| Portability | Mostly portable SQL | Switch dialect → switch DB |
| Bulk perf | Direct | Needs care (batch, StatelessSession) |
| Learning curve | Small | Steep |
| Best fit | Reports, hot paths, ETL | CRUD on rich domain |

### 2.5 When NOT to Use Hibernate
- Heavy reporting/analytics → JDBC, jOOQ, `JdbcTemplate`.
- Streaming millions of rows → `StatelessSession` or raw JDBC.
- Non‑relational data (graph, document) → wrong tool.

> **Pragmatic rule:** *mix them*. ~95 % of CRUD via Hibernate, ~5 % of hot paths via JDBC.

---

## 3. Hibernate Architecture

### 3.1 The Runtime Object Graph (Big Picture)

```
 ┌────────────────────────────────────────────────────────────────────────┐
 │                            APPLICATION                                 │
 │   @Controller │ @Service │ @Repository                                 │
 └────────────────────────────────┬───────────────────────────────────────┘
                                  │ uses
                                  ▼
 ┌────────────────────────────────────────────────────────────────────────┐
 │                EntityManagerFactory  ≈  SessionFactory                 │
 │  ┌──────────────────────────────────────────────────────────────────┐  │
 │  │  Metamodel (mappings, @Entity descriptors)                       │  │
 │  │  Dialect (MySQL / Postgres / Oracle SQL flavours)                │  │
 │  │  Query plan cache (parsed HQL → SQL)                             │  │
 │  │  ConnectionProvider  ──►  HikariCP DataSource                    │  │
 │  │  Optional 2nd‑level cache  (Ehcache / Caffeine / Infinispan)     │  │
 │  │  Optional query cache                                            │  │
 │  └──────────────────────────────────────────────────────────────────┘  │
 │   ONE per application │ HEAVY to build │ THREAD‑SAFE │ immutable       │
 └────────────────────────────────┬───────────────────────────────────────┘
                  creates many  ▼   (one per unit of work)
 ┌────────────────────────────────────────────────────────────────────────┐
 │                  EntityManager  ≈  Session                             │
 │  ┌──────────────────────────────────────────────────────────────────┐  │
 │  │           PERSISTENCE CONTEXT  (1st‑level cache)                 │  │
 │  │                                                                  │  │
 │  │   Identity Map   { (Book,1)→Book@a1, (Author,5)→Author@b2 }      │  │
 │  │   Snapshots      { Book@a1 → {title:"v1", price:10, …} }         │  │
 │  │   Action Queue   [ INSERT Book{new}, UPDATE Author{5}, DELETE… ] │  │
 │  │   Event Listeners (auto‑flush, cascade, interceptors)            │  │
 │  └──────────────────────────────────────────────────────────────────┘  │
 │   ONE per unit of work │ LIGHT │ NOT thread‑safe                       │
 └────────────────────────────────┬───────────────────────────────────────┘
                                  │ borrows
                                  ▼
                          JDBC Connection (HikariCP)
                                  │
                                  ▼
                              DATABASE
```

### 3.2 Roles at a Glance

| Object | How many | Lifetime | Thread‑safe | Holds |
|--------|----------|----------|-------------|-------|
| `EntityManagerFactory` / `SessionFactory` | 1 per app | App lifetime | ✅ | Mappings, caches, pool |
| `EntityManager` / `Session` | 1 per unit of work | Short | ❌ | Persistence context |
| `EntityTransaction` | 1 per session | Short | ❌ | JDBC txn |
| Entity (`@Entity`) | Many | Varies (4 states) | n/a | Domain data |

### 3.3 Annotation Cheat Map

```
 @Entity                       ← "this class is persistent"
 @Table(name, schema, indexes) ← override table metadata
  │
  ├── @Id                                  ← PK field
  │   @GeneratedValue(IDENTITY/SEQUENCE/AUTO/UUID)
  │
  ├── @Column(name, length, nullable, unique, updatable, insertable)
  ├── @Transient                           ← skip persistence
  ├── @Version                             ← optimistic lock
  ├── @Enumerated(EnumType.STRING)         ← enum mapping
  ├── @Lob                                 ← BLOB/CLOB
  ├── @Embedded / @Embeddable              ← inline value object
  ├── @CreationTimestamp / @UpdateTimestamp
  │
  └── Relationships
       ├── @ManyToOne(fetch=LAZY) @JoinColumn(name="author_id")
       ├── @OneToMany(mappedBy="author", cascade=ALL, orphanRemoval=true)
       ├── @ManyToMany @JoinTable(name="book_tag", joinColumns=…, inverseJoinColumns=…)
       └── @OneToOne (mappedBy / @JoinColumn unique=true)
```

### 3.4 Minimal Entity Pair
```java
@Entity @Table(name = "author")
public class Author {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    @OneToMany(mappedBy = "author",
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    private List<Book> books = new ArrayList<>();
    // getters/setters/no-arg ctor
}

@Entity @Table(name = "book")
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;
    // getters/setters/no-arg ctor
}
```

---

## 4. SessionFactory & Session — The Two Pillars

### 4.1 `SessionFactory` — Built Once, Used Forever

**Theory:**
- Constructed once at startup; scans every `@Entity` and compiles a **metamodel**.
- Owns the JDBC pool, dialect, types, query plan cache, and optional 2nd‑level cache.
- **Heavy** to build (hundreds of ms) but **immutable** and **thread‑safe** thereafter.
- All threads share it; each grabs a `Session` for its own work.

```
                       Boot time
                          │
       scan @Entity ──┐   │
                     ┌▼───┴───────────────────┐
       parse maps ──►│  build metamodel       │
                     │  build query‑plan cache│
       validate   ──►│  init dialect          │
       schema?       │  init connection pool  │
                     └─────────┬──────────────┘
                               ▼
                  ┌─────────────────────────┐
                  │     SessionFactory      │  ─► shared by every request
                  └─────────────────────────┘     for the rest of app life
```

### 4.2 `Session` — One Unit of Work

A **Session** = one short conversation with the DB. It borrows a JDBC connection, runs the SQL for one logical operation, and is discarded.

```
   sessionFactory.openSession()
              │
              ▼
   ┌──────────────────────────────────────────────────────────┐
   │                      SESSION                             │
   │                                                          │
   │  ╔══════════════ Persistence Context (L1) ════════════╗  │
   │  ║                                                    ║  │
   │  ║   Identity Map                                     ║  │
   │  ║     (Book,1)   → Book@a1                           ║  │
   │  ║     (Author,5) → Author@b2                         ║  │
   │  ║     (Book,2)   → Book@c3                           ║  │
   │  ║                                                    ║  │
   │  ║   Snapshots (for dirty checking)                   ║  │
   │  ║     Book@a1 → {title:"v1", price:10, …}            ║  │
   │  ║                                                    ║  │
   │  ║   Action queue                                     ║  │
   │  ║     [ INSERT Book{new}, UPDATE Author{5}, DELETE…] ║  │
   │  ╚════════════════════════════════════════════════════╝  │
   │                                                          │
   │            borrows JDBC Connection from pool             │
   └───────────────────────────┬──────────────────────────────┘
                               │
                               ▼ on flush/commit
                       sorted SQL stream
                               │
                               ▼
                            DATABASE
```

### 4.3 Why the Persistence Context Is the Heart of Hibernate

The persistence context is **simultaneously** four things — that's why Hibernate feels magical:

```
   ┌──────────────────────────────────────────────────────────────┐
   │ 1. Identity map        same (type,id) → same Java object     │
   │ 2. 1st‑level cache     repeat lookups served from memory     │
   │ 3. Dirty‑check engine  diff entity vs snapshot at flush      │
   │ 4. Write‑behind buffer reorder SQL, batch, deferred flush    │
   └──────────────────────────────────────────────────────────────┘
```

#### (1) Identity Uniqueness
```java
Book a = session.get(Book.class, 1L);
Book b = session.get(Book.class, 1L);
assert a == b;          // SAME reference — not just equal, identical
```
Inside the same session, two lookups of the same row return the **same** Java object.

#### (2) 1st‑level Cache
Second `get` doesn't even hit the DB:
```
   first  get(Book,1) → SELECT * FROM book WHERE id=1
   second get(Book,1) → cache hit, no SQL
```

#### (3) Dirty Checking — Auto UPDATE
```java
Book b = session.get(Book.class, 1L);
b.setTitle("new title");        // no save() call!
// on flush:  UPDATE book SET title='new title' WHERE id=1
```
At flush, Hibernate compares each managed entity to its **snapshot** (taken when first loaded) and emits an UPDATE for changed columns only.

#### (4) Write‑Behind Ordering
SQL is **deferred and reordered**:
- INSERT parents *before* INSERT children.
- DELETE children *before* DELETE parents.
- Group identical INSERTs into JDBC batches.

Result: fewer FK violations, fewer round trips.

### 4.4 Method Map — Hibernate ↔ JPA

| Hibernate (`Session`) | JPA (`EntityManager`) | Effect | When to use |
|-----------------------|------------------------|--------|--------------|
| `persist(e)` | `persist(e)` | Transient → Persistent (INSERT queued) | inserting a new entity |
| `get(C, id)` | `find(C, id)` | Load by PK (cache + DB) | when you need the data now |
| `load(C, id)` | `getReference(C, id)` | Proxy only; SELECT deferred | when you just need the id to set a FK |
| `merge(e)` | `merge(e)` | Detached → returns managed copy | reattaching after a round trip |
| `delete(e)` | `remove(e)` | Persistent → Removed | deletion |
| `evict(e)` | `detach(e)` | Persistent → Detached | manually detach one |
| `clear()` | `clear()` | Detach all | mid-batch cleanup |
| `flush()` | `flush()` | Push pending SQL now | force ordering / generate id |
| `refresh(e)` | `refresh(e)` | Reload from DB, overwrite local | discard local changes |
| `createQuery(hql)` | `createQuery(jpql)` | Build query | HQL/JPQL |

### 4.5 Canonical Code Pattern
```java
try (Session session = sessionFactory.openSession()) {
    Transaction tx = session.beginTransaction();
    try {
        Book b = new Book();
        b.setTitle("Effective Java");
        session.persist(b);                       // transient → persistent

        Book loaded = session.get(Book.class, b.getId());
        loaded.setTitle("Effective Java, 3rd");   // dirty → UPDATE on commit

        tx.commit();                              // flush + commit
    } catch (RuntimeException ex) {
        tx.rollback();
        throw ex;
    }
}
// session closed → every entity above is now DETACHED
```

### 4.6 Flush vs Commit — Often Confused

```
  Application                    Session                          DB
  ───────────                    ───────                          ──
  persist(b)        ────────► queue: [INSERT b]                (no SQL)
  b.title = "x"     ────────► mark dirty: b                    (no SQL)
  query(...) ?      ────────► auto-flush if needed             SELECT
  flush()           ────────► emit INSERT b…                   ─►INSERT
                              emit UPDATE b…                   ─►UPDATE
                                  (txn still open)
  commit()          ────────► flush if needed + COMMIT         ─►COMMIT
  close()           ────────► return connection to pool
                              entities become DETACHED
```

- **Flush** = sync Java state → DB SQL. Doesn't end the transaction.
- **Commit** = flush + JDBC commit.
- **Auto‑flush** kicks in before a query that could be affected by pending changes (so queries see your own writes).

### 4.7 Lifecycle of a Single `Session`

```
   open ──► begin tx ──► load/persist/modify ──► (auto)flush ──► commit ──► close
                              │
                              └──► or rollback ──► close
                                              all entities → DETACHED
```

---

## 5. Entity States — The Lifecycle

### 5.1 The Four States and Their Transitions

```
                                ┌──────────────────────┐
              new Book()        │      TRANSIENT       │
              ───────────────►  │  (no DB row,         │
                                │   not in any context)│
                                └──────────┬───────────┘
                                           │
                            session.persist(b)
                                           ▼
   ┌────────────────┐ close()/clear()    ┌──────────────────────┐
   │   DETACHED     │ ◄───── evict() ─── │     PERSISTENT       │
   │  has id; no    │                    │  managed in context, │
   │  session sees  │                    │  dirty‑checked, in   │
   │  changes       │ ── merge(b) ────►  │  L1 cache            │
   └──────┬─────────┘  returns NEW       └──────────┬───────────┘
          │            managed copy                 │
          │            (b itself stays detached!)   │ session.remove(b)
          │                                         ▼
          │                              ┌──────────────────────┐
          │                              │       REMOVED        │
          │                              │  tracked until flush │
          │                              │  then DELETE issued  │
          │                              └──────────────────────┘
          │
          │ Garbage collect when no references remain.
          ▼
        gone
```

### 5.2 State Properties Table

| State | In context? | Has DB row? | Dirty‑checked? | On commit | Can navigate lazy fields? |
|-------|:-----------:|:-----------:|:--------------:|-----------|:--------------------------:|
| **Transient** | ❌ | ❌ | ❌ | nothing | n/a (none mapped yet) |
| **Persistent** | ✅ | ✅ (after flush) | ✅ | INSERT or UPDATE | ✅ |
| **Detached** | ❌ | ✅ | ❌ | nothing | ❌ `LazyInitializationException` |
| **Removed** | ✅ | ✅ (until flush) | n/a | DELETE | depends |

### 5.3 Walking Through Code

```java
EntityManager em = ...;
em.getTransaction().begin();

Book b = new Book();              // ── TRANSIENT
b.setTitle("Clean Code");

em.persist(b);                    // ── PERSISTENT  (INSERT queued)
b.setTitle("Clean Code v2");      // dirty: UPDATE will be queued at flush

em.flush();                       // INSERT + UPDATE go to DB now

em.detach(b);                     // ── DETACHED
b.setTitle("ignored — no session watches me anymore");

Book reattached = em.merge(b);    // ── PERSISTENT (NEW managed copy!)
                                  //    `b` itself stays detached!
reattached.setTitle("dirty‑tracked");

em.remove(reattached);            // ── REMOVED  (DELETE queued)
em.getTransaction().commit();     // flush + commit → INSERT, UPDATE, DELETE
```

### 5.4 `persist()` vs `merge()` — The Most Common Bug

```
 persist(e)
   ─► requires e to be TRANSIENT
   ─► makes e itself MANAGED
   ─► if e already has an id row, IllegalStateException

 merge(e)
   ─► e may be TRANSIENT or DETACHED
   ─► reads/inserts and returns a NEW MANAGED copy
   ─► the argument `e` stays DETACHED — modifying it does nothing
```

```java
Book detached = ...;                  // came from somewhere outside the session
Book managed  = em.merge(detached);   // use the RETURN VALUE from now on

managed.setTitle("ok");               // dirty‑tracked → UPDATE
detached.setTitle("ignored");         // ❌ nothing happens at flush
```

### 5.5 Detached Objects — Why They Exist

Detached objects survive **after** the session closes. They're plain POJOs with state. Useful for:

- Returning data from a service to a controller / view layer.
- Caching across HTTP requests.
- Reattaching later via `merge` when the user submits an edit form.

---

## 6. Lazy vs Eager Loading

### 6.1 Theory — What Hibernate Has to Decide

When loading entity *A*, Hibernate must choose what to do with each of A's associations:

```
   EAGER : load related entity NOW (extra SELECTs or a JOIN)
   LAZY  : return a PROXY; load on FIRST ACCESS (inside an open session)
```

A proxy is a runtime subclass of your entity. It looks like an `Author` but every getter is intercepted:

```
              ┌────────────────────────────┐
              │  Author proxy (uninitialized)│
              │  id = 5                    │
              │  name = ???  ── getName() ─┼─► trigger SELECT, hydrate, return
              └────────────────────────────┘
```

### 6.2 Defaults (Memorize)

| Annotation | Default | What you should set |
|-----------|---------|---------------------|
| `@ManyToOne` | **EAGER** ⚠️ | `FetchType.LAZY` |
| `@OneToOne` | **EAGER** ⚠️ | `FetchType.LAZY` |
| `@OneToMany` | LAZY | leave LAZY |
| `@ManyToMany` | LAZY | leave LAZY |
| `@Basic` | EAGER | usually fine |

> **Golden rule:** make everything **LAZY by default**, opt into eager fetching **per query** when you actually need the data.

### 6.3 LAZY vs EAGER — Side by Side

```
 LAZY  (recommended)
 ──────────────────────────────────────────────────────────────
  em.find(Book.class, 1)
        │
        │  SQL #1:  SELECT * FROM book WHERE id = 1
        ▼
   ┌────────────────────────────────────────────┐
   │ Book{ id:1, title:"…", author: Proxy(5) }  │
   └─────────────────────┬──────────────────────┘
                         │ book.getAuthor().getName()    ← first access
                         │ SQL #2:  SELECT * FROM author WHERE id = 5
                         ▼
                Author{ id:5, name:"Bloch" }


 EAGER  (default @ManyToOne — surprise!)
 ──────────────────────────────────────────────────────────────
  em.find(Book.class, 1)
        │
        │  SQL #1:
        │     SELECT b.*, a.*
        │     FROM book b
        │     LEFT JOIN author a ON a.id = b.author_id
        │     WHERE b.id = 1
        ▼
   Book{ id:1, title:"…", author: Author{ id:5, … } already loaded }
```

### 6.4 Code Example
```java
@Entity
public class Book {
    @Id @GeneratedValue Long id;
    String title;

    @ManyToOne(fetch = FetchType.LAZY)      // override the EAGER default
    @JoinColumn(name = "author_id")
    Author author;
}

@Entity
public class Author {
    @Id @GeneratedValue Long id;
    String name;

    @OneToMany(mappedBy = "author")          // already LAZY
    List<Book> books = new ArrayList<>();
}
```

### 6.5 The Famous `LazyInitializationException`

A lazy proxy needs an **open session** to fetch data. Touching it after the session closes blows up:

```
                    SESSION OPEN
   ┌──────────────────────────────────────────────────────────┐
   │  Book b = em.find(Book.class, 1)                         │
   │  // b.author is a proxy, NOT yet loaded                  │
   └──────────────────────────────────────────────────────────┘
                          │
                          ▼  end of @Transactional method
                    SESSION CLOSED
                          │
                          ▼
   controller / view:  b.getAuthor().getName()
                            │
                            ▼
              ✗  LazyInitializationException
                 ("could not initialize proxy — no Session")
```

**Fixes (preferred order):**
1. **Access the field inside the transactional method.** Easiest.
2. **`JOIN FETCH`** the association in the query.
3. **`@EntityGraph`** on the repository method.
4. **Return a DTO** (no proxies in flight).
5. *Last resort* — `spring.jpa.open-in-view=true` (keeps session open across the HTTP response). **Hides the problem**; disable in production.

### 6.6 `JOIN FETCH` — Per‑Query Eager
```java
List<Book> rs = em.createQuery("""
        SELECT b FROM Book b
        JOIN FETCH b.author
        WHERE b.title LIKE :t""", Book.class)
    .setParameter("t", "%Java%")
    .getResultList();
```
One SQL, no proxies, safe outside the session.

### 6.7 `@EntityGraph` — Declarative Alternative
Named on the entity:
```java
@NamedEntityGraph(name = "Book.withAuthor",
    attributeNodes = @NamedAttributeNode("author"))
@Entity
public class Book { ... }
```
Used in a Spring Data repository:
```java
public interface BookRepo extends JpaRepository<Book, Long> {
    @EntityGraph(attributePaths = "author")
    List<Book> findByTitleContaining(String t);
}
```

---

## 7. The N+1 Problem & Solutions

### 7.1 What It Is — and Why It's Quiet but Deadly

**1 query** to load N parents → **N more queries**, one per parent, to load its lazy association. Total = **N + 1** round trips.

```java
List<Book> books = em.createQuery("SELECT b FROM Book b", Book.class)
                     .getResultList();          // 1 query → N books

for (Book b : books) {
    System.out.println(b.getAuthor().getName()); // 1 SELECT per book = N
}
// Total: 1 + N
```

### 7.2 SQL Trace Diagram

```
   #1   SELECT * FROM book;
              │  returns N rows
              ▼
        ┌─────────────────────────────────────────────┐
        │  For each Book in result:                   │
        │    (lazy access to .author triggers a SQL)  │
        │                                             │
        │  #2     SELECT * FROM author WHERE id = ?   │
        │  #3     SELECT * FROM author WHERE id = ?   │
        │  ...                                        │
        │  #N+1   SELECT * FROM author WHERE id = ?   │
        └─────────────────────────────────────────────┘

   Round trips: 1 + N.    Latency: O(N · RTT).
   Example: 100 books × 50 ms RTT  ≈  5 s on one endpoint.
```

### 7.3 Why It Happens
- **LAZY** + a loop = one fetch per iteration.
- **EAGER `@ManyToOne` doesn't save you** — Hibernate often issues *one SELECT per relation* rather than a JOIN.
- It's silent: works in tests with 1 row, melts down in prod with 10 000.

### 7.4 The Five Solutions

#### (a) `JOIN FETCH` — one query, one JOIN
```java
List<Book> books = em.createQuery("""
    SELECT b FROM Book b JOIN FETCH b.author""", Book.class)
  .getResultList();
```
```
   SQL #1
   ──────
   SELECT b.*, a.*
   FROM book b JOIN author a ON a.id = b.author_id;

   ┌────────────────────────────────────┐
   │ 1 SQL → N books, all authors set   │
   └────────────────────────────────────┘
```
⚠️ For `@OneToMany` join fetches, duplicates appear (one row per child). Use `SELECT DISTINCT`. Don't combine with `setMaxResults` on `@OneToMany` joins (Hibernate falls back to in‑memory paging with a warning).

#### (b) `@EntityGraph` — Spring Data declarative
```java
@EntityGraph(attributePaths = "author")
List<Book> findAll();
```
Same SQL effect as JOIN FETCH.

#### (c) `@BatchSize` — N becomes ceil(N/batch)
```java
@Entity
@BatchSize(size = 20)            // class-level (also works on collections)
public class Author { ... }
```
```
   #1   SELECT * FROM book;                              (1)
   #2   SELECT * FROM author WHERE id IN (?, ?, …, ?);   (20 ids at a time)
   #3   SELECT * FROM author WHERE id IN (?, …);
   …
   Round trips: 1 + ceil(N/20)
```
Best zero‑code‑change mitigation across the whole app.

#### (d) `FetchMode.SUBSELECT` — one extra SELECT for *all* children
```java
@OneToMany(mappedBy = "author")
@Fetch(FetchMode.SUBSELECT)
List<Book> books;
```
```
   #1   SELECT * FROM author WHERE …;
   #2   SELECT * FROM book
        WHERE author_id IN (SELECT id FROM author WHERE …);
   Total: 2 queries, regardless of N.
```

#### (e) DTO Projection — skip entities entirely
```java
record BookView(String title, String authorName) {}

List<BookView> rs = em.createQuery("""
    SELECT new com.example.BookView(b.title, b.author.name)
    FROM Book b""", BookView.class)
  .getResultList();
```
One SQL, zero proxies, ideal for read‑only views (lists, dashboards).

### 7.5 Solutions Compared

| Strategy | Queries | When to use | Caveats |
|----------|---------|-------------|---------|
| `JOIN FETCH` | 1 | One‑off query needing the relation | OneToMany + paging issues |
| `@EntityGraph` | 1 | Repository methods | same |
| `@BatchSize` | 1 + N/size | Global mitigation | not as tight as JOIN |
| `FetchMode.SUBSELECT` | 2 | Known parent set + children | runs on every collection load |
| DTO projection | 1 | Read‑only views | no entity caching/dirty |
| EAGER everywhere | varies | ❌ Don't | hides the problem |

### 7.6 How to Spot N+1 Early

Enable SQL logging in dev:
```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE
```
Helpers:
- `datasource-proxy` / `p6spy` — counts every SQL.
- **Hibernate Statistics** (`hibernate.generate_statistics=true`).
- **Hypersistence Utils** — assertions like `assertSelectCount(1)` in tests.

### 7.7 N+1 Detection Workflow
```
   slow endpoint?
        │
        ▼
   turn on SQL log  ─►  count queries vs result rows
        │
        ▼
   1 + N ?
        │
        ▼
   choose: JOIN FETCH / @EntityGraph / @BatchSize / DTO
        │
        ▼
   re-run, count again, verify
        │
        ▼
   add test that asserts query count
```

---

## 8. Caches — L1, L2, Query Cache

### 8.1 Cache Hierarchy

```
   ┌──────────────────────────────────────────────────────┐
   │ QUERY CACHE  (optional, per SessionFactory)          │   "this exact query
   │   key:  query text + params                          │    returned IDs X,Y"
   │   value: list of entity IDs                          │
   ├──────────────────────────────────────────────────────┤
   │ 2nd‑LEVEL CACHE (optional, per SessionFactory)       │   shared across sessions
   │   key:   (entityType, id)                            │   provider plug-in:
   │   value: dehydrated entity state                     │   Ehcache / Caffeine / …
   ├──────────────────────────────────────────────────────┤
   │ 1st‑LEVEL CACHE  (ALWAYS on, per Session)            │   identity map +
   │   The persistence context                            │   dirty‑check snapshots
   ├──────────────────────────────────────────────────────┤
   │ DATABASE                                             │
   └──────────────────────────────────────────────────────┘
```

Read path:
```
   em.find(Book, 1)
       │
       ├─► L1 hit?    yes ──► return cached object (no SQL)
       │     │ no
       │     ▼
       ├─► L2 hit?    yes ──► hydrate into L1, return (no SQL)
       │     │ no
       │     ▼
       └─► SELECT FROM book WHERE id=1 ──► hydrate, store in L1 (+L2 if on)
```

### 8.2 Enabling L2 (Spring Boot + Ehcache)
```xml
<dependency>
  <groupId>org.hibernate.orm</groupId>
  <artifactId>hibernate-jcache</artifactId>
</dependency>
<dependency>
  <groupId>org.ehcache</groupId>
  <artifactId>ehcache</artifactId>
  <classifier>jakarta</classifier>
</dependency>
```
```properties
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=jcache
spring.jpa.properties.hibernate.javax.cache.provider=org.ehcache.jsr107.EhcacheCachingProvider
```
```java
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Author { ... }
```

### 8.3 When to Use L2
- **Reference data** that rarely changes (countries, currencies, roles).
- **Hot read** entities with known TTL.
- Never as a substitute for proper queries / indexes.

---

## 9. Transactions, Flush Modes & Locking

### 9.1 Transaction Boundary

```
   begin tx ─────────────┐
                         │  persistence context lives here
   work, work, work      │  (entities are managed)
                         │
   commit ───────────────┘ → flush, COMMIT, close, all entities detached
```

In Spring you usually mark service methods `@Transactional` and the proxy does begin/commit.

### 9.2 Flush Modes

| Mode | When auto‑flush fires |
|------|----------------------|
| `AUTO` (default) | Before any query that could see pending changes, and on commit |
| `COMMIT` | Only on commit |
| `MANUAL` | Only when you call `flush()` |
| `ALWAYS` (rare) | Before every query |

Most apps want `AUTO`. `MANUAL` is occasionally useful for read‑only views.

### 9.3 Locking

**Optimistic** (default — recommended): `@Version` column.

```java
@Entity
public class Book {
    @Id Long id;
    String title;
    @Version long version;
}
```
On UPDATE: `UPDATE book SET title=?, version=? WHERE id=? AND version=?`.
If the row's version changed by another tx, 0 rows are updated → `OptimisticLockException`.

```
   Tx A reads Book{id:1, version:7}      Tx B reads Book{id:1, version:7}
   modifies title                         modifies title
   commits → version 8                    commits → version mismatch
                                         OptimisticLockException
```

**Pessimistic**: actual DB locks (`SELECT … FOR UPDATE`).

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM Book b WHERE b.id = :id")
Book findForUpdate(@Param("id") Long id);
```

Use pessimistic only when you genuinely have contention; it blocks other transactions.

---

## 10. Hibernate in Spring Boot

### 10.1 Dependency
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```
Transitively brings Hibernate + JPA + Spring Data JPA + HikariCP.

### 10.2 Typical `application.properties`
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/library
spring.datasource.username=root
spring.datasource.password=secret

# Schema: none | validate | update | create | create-drop
spring.jpa.hibernate.ddl-auto=update

# Dev visibility
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Force you to write correct fetching
spring.jpa.open-in-view=false

# Performance knobs
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.generate_statistics=true
```

### 10.3 The Spring Boot Stack

```
   HTTP request
        │
        ▼
   @RestController
        │ delegates to
        ▼
   @Service @Transactional       ◄── proxy starts tx + EntityManager
        │
        ▼
   JpaRepository                 ◄── Spring Data generates the impl
        │
        ▼
   EntityManager (JPA spec)
        │
        ▼
   Session (Hibernate engine)    ◄── persistence context lives here
        │ borrows
        ▼
   HikariCP pool
        │ runs SQL via
        ▼
   JDBC Driver
        │
        ▼
   DATABASE
```

### 10.4 Repository + Service Example
```java
public interface BookRepository extends JpaRepository<Book, Long> {

    @EntityGraph(attributePaths = "author")
    List<Book> findByTitleContainingIgnoreCase(String fragment);

    @Query("""
        SELECT b FROM Book b JOIN FETCH b.author
        WHERE b.author.name = :name""")
    List<Book> findByAuthorName(@Param("name") String name);
}

@Service
public class CatalogService {

    private final BookRepository repo;
    public CatalogService(BookRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public List<Book> search(String q) {
        return repo.findByTitleContainingIgnoreCase(q);
    }

    @Transactional
    public Book add(String title, Author author) {
        Book b = new Book();
        b.setTitle(title);
        b.setAuthor(author);
        return repo.save(b);                    // INSERT on flush
    }
}
```

### 10.5 What Happens Inside `repo.save(b)`?

```
   repo.save(b)
        │
        ▼
   SimpleJpaRepository.save
        │
        ├─ if b.id == null   ─► em.persist(b)   queue INSERT
        └─ else              ─► em.merge(b)      returns managed copy

   end of @Transactional method
        │
        ▼
   flush  → ordered SQL → DB
   commit → end JDBC transaction
   close  → entities now DETACHED
```

---

## 11. Best Practices & Mental Models

### 11.1 Do / Don't

| ✅ Do | ❌ Don't |
|------|---------|
| Make all `@ManyToOne` / `@OneToOne` `LAZY` | Leave them EAGER "just in case" |
| Use `JOIN FETCH` / `@EntityGraph` per query | Loop & touch lazy fields outside txn |
| Return DTOs from controllers | Serialize entities to JSON (Jackson + lazy = pain) |
| `@Transactional` on services | Manage `EntityManager` manually in Spring |
| `spring.jpa.open-in-view=false` | Hide N+1 with open‑in‑view |
| Use the **return value** of `merge()` | Assume the argument is now managed |
| `@Version` for optimistic locking | Hope concurrent edits "just work" |
| Enable SQL logging + stats in dev | Ship without seeing actual SQL |
| Configure `jdbc.batch_size` for bulk | Insert 10k rows one by one |
| `@Enumerated(EnumType.STRING)` | `EnumType.ORDINAL` (breaks on reorder) |
| Use `Set` for `@ManyToMany` | `List` (delete+reinsert all rows) |

### 11.2 Mental Model — One Picture
```
   SessionFactory        (1, heavy, thread-safe)
         │ produces
         ▼
   Session               (per request/txn, light, NOT thread-safe)
         │ holds
         ▼
   Persistence Context   (identity map + dirty-check snapshots + action queue)
         │ syncs at
         ▼
   flush / commit  →  ordered SQL via JDBC  →  Database
```

### 11.3 Entity Lifecycle Mantra
> **Transient** → `persist` → **Persistent** → close/evict → **Detached** → `merge` → **Persistent (copy)** → `remove` → **Removed**

### 11.4 The N+1 Reflex
Every `findAll` + loop access of a lazy field, ask:
1. Will I touch a lazy association in the loop?
2. If yes → `JOIN FETCH` / `@EntityGraph` / DTO.
3. Add a test that asserts the query count for hot endpoints.

### 11.5 Hibernate Debug Toolbox

| What | How |
|------|-----|
| See SQL fired | `spring.jpa.show-sql=true` + `format_sql=true` |
| See parameter bindings | `logging.level.org.hibernate.orm.jdbc.bind=TRACE` |
| Count queries / cache hits | `hibernate.generate_statistics=true` |
| Detect N+1 in tests | Hypersistence Utils `assertSelectCount(1)` |
| Inspect schema diff | `ddl-auto=validate` in CI |

---

## TL;DR
- **ORM** bridges objects ↔ tables. **JPA** is the *spec*; **Hibernate** is the *engine*; **Spring Data JPA** is *convenience on top*.
- **Hibernate vs JDBC**: productivity, caching, dirty checking, relationship handling — at the price of hidden SQL and several well-known gotchas.
- **`SessionFactory`** = heavy, one per app, thread-safe. **`Session`/`EntityManager`** = light, one per unit of work, holds the **persistence context** (identity map + dirty-check snapshots + action queue).
- **Entity states**: Transient → Persistent → Detached → Removed. Use `persist`, `merge`, `remove`, `detach`. `merge` returns a NEW managed object — use the return value.
- **Lazy** is the right default; `@ManyToOne` / `@OneToOne` default to EAGER — override them. `LazyInitializationException` = touched a proxy after the session closed.
- **N+1** = 1 parent query + N child queries. Fix with **`JOIN FETCH`**, **`@EntityGraph`**, **`@BatchSize`**, **`FetchMode.SUBSELECT`**, or **DTO projection**. Always log SQL in dev to spot it.
- **Caches**: L1 always on (per session), L2 optional (shared, plug-in), query cache rare.
- **Transactions**: live on the service layer. Prefer **optimistic locking** with `@Version`.
