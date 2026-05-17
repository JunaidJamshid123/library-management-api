# Spring Data JPA — In‑Depth Notes

> **Goal:** Master Spring Data JPA from the ground up — entity mapping, relationships, repositories, derived queries, `@Query`, modifying queries, the service-layer pattern, DTOs, and MapStruct.
> Every section: **theory → diagram → code**.

---

## Table of Contents
1. [Where Spring Data JPA Fits](#1-where-spring-data-jpa-fits)
2. [`@Entity`, `@Table`, `@Id`, `@GeneratedValue`](#2-entity-table-id-generatedvalue)
3. [`@Column`, `@Transient` (and friends)](#3-column-transient-and-friends)
4. [Relationships](#4-relationships)
5. [Repositories: `CrudRepository`, `PagingAndSortingRepository`, `JpaRepository`](#5-repositories)
6. [Derived Query Methods](#6-derived-query-methods)
7. [`@Query` — JPQL and Native SQL](#7-query--jpql-and-native-sql)
8. [`@Modifying` + `@Transactional`](#8-modifying--transactional)
9. [Service Layer Pattern](#9-service-layer-pattern)
10. [DTOs (Data Transfer Objects)](#10-dtos-data-transfer-objects)
11. [MapStruct vs Manual Mapping](#11-mapstruct-vs-manual-mapping)
12. [End‑to‑End Example](#12-end-to-end-example)
13. [Cheat Sheet & Best Practices](#13-cheat-sheet--best-practices)

---

## 1. Where Spring Data JPA Fits

### 1.1 The Layered Stack

```
 ┌─────────────────────────────────────────────────────────────┐
 │   Controller   (@RestController)                            │  HTTP layer
 ├─────────────────────────────────────────────────────────────┤
 │   Service      (@Service, @Transactional)                   │  business rules
 ├─────────────────────────────────────────────────────────────┤
 │   Repository   (interface extends JpaRepository<T,ID>)      │  data access
 │   ─ Spring Data generates the impl at runtime               │
 ├─────────────────────────────────────────────────────────────┤
 │   JPA          (EntityManager, @Entity, JPQL)               │  spec
 ├─────────────────────────────────────────────────────────────┤
 │   Hibernate    (Session, dialect, caches)                   │  engine
 ├─────────────────────────────────────────────────────────────┤
 │   JDBC + HikariCP  →  Database                              │  plumbing
 └─────────────────────────────────────────────────────────────┘
```

**Spring Data JPA's job**: remove the boilerplate of writing DAO classes. You declare an *interface*; it generates the implementation that delegates to JPA / Hibernate.

### 1.2 Maven Dependency
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```
This pulls in JPA, Hibernate, HikariCP, and Spring Data JPA automatically.

---

## 2. `@Entity`, `@Table`, `@Id`, `@GeneratedValue`

### 2.1 `@Entity` — "This class is a row"

**Theory:**
- Marks a POJO as a **persistent class** managed by JPA/Hibernate.
- Required: a **no‑arg constructor** (Hibernate uses reflection to instantiate).
- Required: a **primary key** (`@Id`).
- The class must not be `final`; fields shouldn't be `final` either (Hibernate proxies subclass it).
- By default the entity name = simple class name; the table name = same (or snake‑cased depending on naming strategy).

### 2.2 `@Table` — Override Table Mapping

```java
@Entity
@Table(name = "book",
       schema = "library",
       uniqueConstraints = @UniqueConstraint(columnNames = {"isbn"}),
       indexes = @Index(name = "idx_book_title", columnList = "title"))
public class Book { ... }
```

| Attribute | Use |
|----------|-----|
| `name` | Physical table name |
| `schema` / `catalog` | DB schema / catalog |
| `uniqueConstraints` | Multi‑column unique keys |
| `indexes` | Indexes created when DDL is generated |

### 2.3 `@Id` — Primary Key

Every entity needs exactly one identifier. It can be:
- A **simple** field (`Long`, `UUID`, `String`).
- A **composite** key via `@EmbeddedId` or `@IdClass` (advanced).

```java
@Id
private Long id;
```

> **Prefer `Long` (or `UUID`) over `int`** — `Long`'s `null` distinguishes "not yet persisted" from "id = 0".

### 2.4 `@GeneratedValue` — How the PK Is Produced

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

#### Strategy Comparison

```
 ┌─────────────────────────────────────────────────────────────────────┐
 │ AUTO       │ Hibernate picks based on dialect. Today on most DBs   │
 │            │ this is "SEQUENCE". Fine for portability.             │
 ├─────────────────────────────────────────────────────────────────────┤
 │ IDENTITY   │ Uses the DB's auto‑increment column.                  │
 │            │ + Simple, works on MySQL.                             │
 │            │ – Defeats Hibernate's INSERT batching (must flush     │
 │            │   each INSERT to learn the id).                       │
 ├─────────────────────────────────────────────────────────────────────┤
 │ SEQUENCE   │ Uses a DB SEQUENCE. Best for Postgres/Oracle.         │
 │            │ + Allows ID generation BEFORE the INSERT → batching.  │
 │            │ + Configure allocation size for fewer round trips.    │
 ├─────────────────────────────────────────────────────────────────────┤
 │ TABLE      │ A dedicated "id generator" table. Portable but slow.  │
 │            │ Rarely used today.                                    │
 ├─────────────────────────────────────────────────────────────────────┤
 │ UUID       │ (JPA 3.1+) java.util.UUID; client‑side generation.    │
 │            │ Good for distributed systems.                          │
 └─────────────────────────────────────────────────────────────────────┘
```

#### SEQUENCE Example with Allocation
```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "book_seq")
@SequenceGenerator(name = "book_seq", sequenceName = "book_seq",
                   allocationSize = 50)         // grab 50 ids per round trip
private Long id;
```

#### UUID Example (JPA 3.1+)
```java
@Id @GeneratedValue
private UUID id;
```

### 2.5 Putting It Together
```java
@Entity
@Table(name = "book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    protected Book() {}                  // required by JPA
    public Book(String title) { this.title = title; }

    // getters/setters
}
```

---

## 3. `@Column`, `@Transient` (and friends)

### 3.1 `@Column` — Fine‑Tune the Column Mapping

```java
@Column(name = "title",
        nullable = false,
        length = 120,
        unique = false,
        updatable = true,
        insertable = true,
        columnDefinition = "VARCHAR(120) COLLATE utf8mb4_unicode_ci")
private String title;
```

| Attribute | Meaning | Affects |
|-----------|---------|---------|
| `name` | Column name | SQL & DDL |
| `nullable` | NOT NULL constraint | DDL + JPA validation |
| `length` | VARCHAR length | DDL |
| `precision` / `scale` | DECIMAL precision | DDL |
| `unique` | Unique constraint | DDL |
| `updatable` | Skip in UPDATE | Runtime SQL |
| `insertable` | Skip in INSERT | Runtime SQL |
| `columnDefinition` | Raw DDL snippet | DDL (use sparingly) |

> `@Column` is **optional**. Without it, the column inherits the field name (often snake‑cased by the naming strategy).

### 3.2 `@Transient` — "Don't persist this field"

```java
@Transient
private int displayOrder;          // computed in UI, not stored
```

`@Transient` skips persistence entirely. The field is **not** mapped to a column, **not** loaded, **not** saved.

> Don't confuse JPA `@Transient` with Java's `transient` keyword (which is for `Serializable`). The annotation is what JPA reads.

### 3.3 Other Useful Field Annotations

| Annotation | Purpose |
|-----------|---------|
| `@Enumerated(EnumType.STRING)` | Store enum as its name (NOT ordinal — never use ordinal). |
| `@Lob` | Map a large object (`@Lob String` → CLOB, `@Lob byte[]` → BLOB). |
| `@Temporal` | Legacy `java.util.Date`. Prefer `java.time.*`. |
| `@Version` | Optimistic locking column. |
| `@Embedded` / `@Embeddable` | Inline value object into the same table. |
| `@Convert(converter = ...)` | Custom Java↔SQL converter. |
| `@CreationTimestamp` / `@UpdateTimestamp` (Hibernate) | Auto‑set on insert/update. |
| `@CreatedDate` / `@LastModifiedDate` (Spring Data) | Auditing via `@EnableJpaAuditing`. |

### 3.4 Example Combining Everything
```java
@Entity
@Table(name = "book")
public class Book {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "isbn", length = 13, unique = true)
    private String isbn;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BookStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Transient
    private boolean highlighted;     // UI only — not persisted

    @Version
    private long version;            // optimistic locking
}
```

---

## 4. Relationships

The hardest, most common source of bugs. Get the *theory* right and the rest follows.

### 4.1 Two Axes: Cardinality & Direction

```
 Cardinality:    one‑to‑one,  one‑to‑many,  many‑to‑one,  many‑to‑many
 Direction:      unidirectional   (one side knows the other)
                 bidirectional    (both sides reference each other)
```

In bidirectional relations, **one side owns the foreign key** (the "owning side"); the other side is the **inverse** and uses `mappedBy`.

```
 owning side       holds the @JoinColumn / @JoinTable
                   updating its reference triggers the SQL change

 inverse side      uses mappedBy = "<owning field>"
                   updating it alone does NOTHING in DB
```

### 4.2 `@ManyToOne` & `@OneToMany`

**Theory:**
- The "many" side **owns the FK** (book holds `author_id`).
- The "one" side uses `mappedBy`.
- **Default fetch** of `@ManyToOne` = EAGER (⚠️ override to LAZY).
- **Default fetch** of `@OneToMany` = LAZY (good).

```
   Author (1) ─────────────────► (many) Book
     id                                  id
     name                                title
                                         author_id  ← FK lives here
```

```java
@Entity
public class Book {
    @Id @GeneratedValue Long id;
    String title;

    @ManyToOne(fetch = FetchType.LAZY)            // override default
    @JoinColumn(name = "author_id")               // FK column
    private Author author;
}

@Entity
public class Author {
    @Id @GeneratedValue Long id;
    String name;

    @OneToMany(mappedBy = "author",
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    private List<Book> books = new ArrayList<>();

    // helper methods keep both sides in sync
    public void addBook(Book b) { books.add(b); b.setAuthor(this); }
    public void removeBook(Book b) { books.remove(b); b.setAuthor(null); }
}
```

#### Why You Need Helper Methods
JPA does NOT auto‑sync both sides of a bidirectional relation. If you only `book.setAuthor(a)` but don't add it to `a.getBooks()`, the in‑memory state is inconsistent (DB will be correct because the owning side wins, but reads inside the same session can lie).

### 4.3 `@OneToOne`

**Theory:**
- One row in A ↔ one row in B.
- Owning side holds the FK (often with `@JoinColumn`); inverse uses `mappedBy`.
- Default fetch is **EAGER** (override to LAZY when reasonable).

```
   Member (1) ◄──────────────► (1) MembershipCard
     id                              id
     name                            issued_on
                                     member_id  ← FK (unique)
```

```java
@Entity
public class Member {
    @Id @GeneratedValue Long id;

    @OneToOne(mappedBy = "member",
              cascade = CascadeType.ALL,
              fetch = FetchType.LAZY,
              orphanRemoval = true)
    private MembershipCard card;
}

@Entity
public class MembershipCard {
    @Id @GeneratedValue Long id;
    LocalDate issuedOn;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", unique = true)
    private Member member;
}
```

> Note: Lazy `@OneToOne` on the *inverse* (non‑owning) side often doesn't truly lazy‑load because Hibernate needs to know whether the related row exists. Bytecode enhancement or a `@OneToOne(optional = false)` setup can help.

### 4.4 `@ManyToMany`

**Theory:**
- Modeled with a **join table** (no entity).
- One side is owning (has `@JoinTable`); the other uses `mappedBy`.
- **Avoid using `List`** for `@ManyToMany` — use `Set` to prevent duplicate join‑row inserts caused by Hibernate's update strategy.
- For many real cases, **promote the join table to its own entity** (so you can add columns like `addedAt`, `quantity`).

```
   Book (M) ◄────────────────► (M) Tag
     id                              id
     title                           label
        │                            │
        └─── join table:  book_tag (book_id, tag_id) ───┘
```

```java
@Entity
public class Book {
    @Id @GeneratedValue Long id;
    String title;

    @ManyToMany
    @JoinTable(name = "book_tag",
        joinColumns = @JoinColumn(name = "book_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();
}

@Entity
public class Tag {
    @Id @GeneratedValue Long id;
    String label;

    @ManyToMany(mappedBy = "tags")
    private Set<Book> books = new HashSet<>();
}
```

#### When to Promote to an Entity
If the join table has its own attributes (e.g. `addedAt`, `addedBy`, `quantity`):
```
   Order (1) ─► (many) OrderItem (many) ◄─ (1) Product
                     │
                     ├── quantity
                     └── price_at_purchase
```
Now `OrderItem` is a full `@Entity` with two `@ManyToOne`s. This is the **strongly recommended** pattern.

### 4.5 `@JoinColumn` vs `@JoinTable`

```
 @JoinColumn        Single FK column on the owning side
                    Used by @ManyToOne, @OneToOne, and unidirectional @OneToMany
                    Example: @JoinColumn(name = "author_id")

 @JoinTable         A separate table to hold (a_id, b_id) pairs
                    Used by @ManyToMany (always) and rare @OneToMany (avoid)
                    Example: @JoinTable(name = "book_tag",
                              joinColumns         = @JoinColumn(name = "book_id"),
                              inverseJoinColumns  = @JoinColumn(name = "tag_id"))
```

### 4.6 Cascade & Orphan Removal

```
 CascadeType.PERSIST   parent persist → child persist
 CascadeType.MERGE     parent merge   → child merge
 CascadeType.REMOVE    parent remove  → child remove
 CascadeType.REFRESH   parent refresh → child refresh
 CascadeType.DETACH    parent detach  → child detach
 CascadeType.ALL       all of the above
```
- `orphanRemoval = true` → removing a child from the parent's collection deletes the row.
- Use cascade carefully on `@ManyToMany` — usually only `PERSIST` and `MERGE`, *never* `REMOVE` (would delete shared entities).

### 4.7 Visual Summary

```
 @OneToOne     1 ─── 1                FK on either side (with unique)
 @OneToMany    1 ─── *                FK on the "many" side
 @ManyToOne    * ─── 1                FK on this side  (the owner)
 @ManyToMany   * ─── *                Join table  (or promoted entity)
```

---

## 5. Repositories

### 5.1 The Hierarchy

```
   Repository<T,ID>                  ← marker, no methods
        ▲
        │
   CrudRepository<T,ID>              save, findById, existsById,
        ▲                             findAll, count, delete...
        │
   PagingAndSortingRepository<T,ID>  adds pagination + sorting
        ▲
        │
   JpaRepository<T,ID>               adds JPA goodies: flush, saveAndFlush,
                                      deleteInBatch, findAll(Sort/Pageable)
```

> Spring Data 3.x also exposes **`ListCrudRepository`**, **`ListPagingAndSortingRepository`** — `findAll()` returns `List<T>` instead of `Iterable<T>`. Prefer those in new code.

### 5.2 What Each Adds

#### `CrudRepository<T, ID>`
```java
<S extends T> S          save(S entity);
<S extends T> Iterable<S> saveAll(Iterable<S> entities);
Optional<T>              findById(ID id);
boolean                  existsById(ID id);
Iterable<T>              findAll();
long                     count();
void                     deleteById(ID id);
void                     delete(T entity);
void                     deleteAll();
```

#### `PagingAndSortingRepository<T, ID>`
```java
Iterable<T> findAll(Sort sort);
Page<T>     findAll(Pageable pageable);
```

#### `JpaRepository<T, ID>`
```java
List<T>   findAll();
List<T>   findAll(Sort sort);
List<T>   findAllById(Iterable<ID> ids);
<S extends T> List<S> saveAll(Iterable<S> entities);
void      flush();
<S extends T> S saveAndFlush(S entity);
void      deleteAllInBatch(Iterable<T> entities);
T         getReferenceById(ID id);   // lazy proxy
```

### 5.3 How It Actually Works (Diagram)

```
   public interface BookRepo extends JpaRepository<Book, Long> {}
                  ▲
                  │  at startup, Spring scans @EnableJpaRepositories paths
                  │
   ┌──────────────┴─────────────────────────────┐
   │  RepositoryFactoryBean creates a Proxy     │
   │  implementing BookRepo.                    │
   │                                            │
   │   Proxy.findById(1L)                       │
   │       │ dispatched to                      │
   │       ▼                                    │
   │   SimpleJpaRepository  ──► EntityManager   │
   │                              ▼             │
   │                           Hibernate        │
   │                              ▼             │
   │                           JDBC / DB        │
   └────────────────────────────────────────────┘
```

You never write the impl; Spring generates it from method names + annotations.

### 5.4 Defining One
```java
public interface BookRepository extends JpaRepository<Book, Long> {
    // automatically gets save, findById, findAll, delete, paging, sorting...
}
```
Inject anywhere:
```java
@Service
public class CatalogService {
    private final BookRepository repo;
    public CatalogService(BookRepository repo) { this.repo = repo; }
}
```

### 5.5 Paging & Sorting Example
```java
Page<Book> page = repo.findAll(
    PageRequest.of(0, 20, Sort.by("title").ascending()));

page.getContent();      // List<Book>
page.getTotalElements();
page.getTotalPages();
page.hasNext();
```

---

## 6. Derived Query Methods

### 6.1 The Big Idea

Spring Data parses the **method name** at startup and generates a JPQL query.

```
   findBy   <Field><Op>   And/Or   <Field><Op>   OrderBy<Field>[Asc/Desc]
   ───────  ─────────────  ───────  ─────────────  ───────────────────────
   prefix      criterion    join     criterion         sort
```

### 6.2 Prefixes & Subjects

| Prefix | Returns |
|--------|---------|
| `findBy`, `getBy`, `readBy`, `queryBy` | `List<T>`, `Optional<T>`, `Stream<T>`, `Page<T>` |
| `existsBy` | `boolean` |
| `countBy` | `long` |
| `deleteBy`, `removeBy` | `int` / `long` (rows affected) — needs `@Transactional` |

### 6.3 Operators

```
 Equals      Is, Equals, (default)
 Comparison  LessThan, GreaterThan, Between
 Null tests  IsNull, IsNotNull
 String      Like, NotLike, StartingWith, EndingWith, Containing, IgnoreCase
 Collection  In, NotIn
 Boolean     True, False
 Logic       And, Or, Not
 Sort        OrderBy<Field>Asc/Desc
 Limit       Top<N>, First<N>, Distinct
```

### 6.4 Examples
```java
public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book>     findByTitle(String title);
    List<Book>     findByTitleContainingIgnoreCase(String fragment);
    Optional<Book> findByIsbn(String isbn);

    List<Book>     findByAuthorName(String name);                  // navigate relation
    List<Book>     findByAuthor_NameIgnoreCase(String name);       // explicit '_'

    List<Book>     findByPriceBetween(BigDecimal lo, BigDecimal hi);
    List<Book>     findByPublishedOnAfter(LocalDate d);

    List<Book>     findByStatusIn(Collection<BookStatus> statuses);

    long           countByStatus(BookStatus status);
    boolean        existsByIsbn(String isbn);

    List<Book>     findTop10ByOrderByCreatedAtDesc();

    Page<Book>     findByTitleContaining(String t, Pageable page);

    Stream<Book>   streamByStatus(BookStatus s);     // requires @Transactional + close
}
```

### 6.5 Underscore Disambiguation
When property names contain underscores or you want to navigate a relation explicitly:
```java
List<Book> findByAuthor_Name(String authorName);    // book.author.name
```

### 6.6 Pitfalls
- Method names get **long**. Beyond ~3 conditions, switch to `@Query`.
- Typos in property names fail at **startup** (good!) — Spring validates the query during context init.
- `deleteBy*` and `existsBy*` for *bulk* operations run as a separate JPQL — see §8.

---

## 7. `@Query` — JPQL and Native SQL

### 7.1 JPQL — Object‑Oriented SQL

**Theory:** JPQL (Java Persistence Query Language) looks like SQL but uses **entity names** and **field names**, not table/column names. It's portable across DBs because Hibernate translates it via the dialect.

```java
public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("SELECT b FROM Book b WHERE b.title LIKE %:q%")
    List<Book> search(@Param("q") String q);

    @Query("SELECT b FROM Book b JOIN FETCH b.author WHERE b.author.name = :name")
    List<Book> findByAuthorName(@Param("name") String name);

    @Query("SELECT COUNT(b) FROM Book b WHERE b.status = :s")
    long countByStatus(@Param("s") BookStatus s);
}
```

#### Named vs Positional Parameters
```java
@Query("SELECT b FROM Book b WHERE b.title = :t AND b.author.id = :aid")
List<Book> a(@Param("t") String t, @Param("aid") Long aid);    // ✅ named (preferred)

@Query("SELECT b FROM Book b WHERE b.title = ?1 AND b.author.id = ?2")
List<Book> b(String t, Long aid);                              // positional
```

### 7.2 Native SQL — When You Need the Real Thing

For DB‑specific features (window functions, CTEs, vendor functions):
```java
@Query(value = """
        SELECT * FROM book
        WHERE MATCH(title) AGAINST (:q IN NATURAL LANGUAGE MODE)""",
       nativeQuery = true)
List<Book> fullTextSearch(@Param("q") String q);
```

### 7.3 Projection: Return a DTO Directly

#### (a) Constructor expression (JPQL)
```java
public record BookView(Long id, String title, String authorName) {}

@Query("""
   SELECT new com.example.BookView(b.id, b.title, b.author.name)
   FROM Book b""")
List<BookView> listViews();
```

#### (b) Interface-based projection
```java
public interface BookSummary {
    Long getId();
    String getTitle();
    String getAuthorName();        // → b.author.name
}

List<BookSummary> findByStatus(BookStatus s);
```
Spring Data builds a proxy that reads only the columns you ask for — minimal data loaded.

### 7.4 Paging with `@Query`
```java
@Query("SELECT b FROM Book b WHERE b.status = :s")
Page<Book> pageByStatus(@Param("s") BookStatus s, Pageable page);

// Spring Data auto-derives a count query:  SELECT COUNT(b) FROM Book b WHERE b.status = :s
// If the auto-derived one is wrong, provide it explicitly:
@Query(value      = "SELECT b FROM Book b JOIN FETCH b.author WHERE b.status = :s",
       countQuery = "SELECT COUNT(b) FROM Book b WHERE b.status = :s")
Page<Book> page(@Param("s") BookStatus s, Pageable p);
```

### 7.5 JPQL vs Native — Decision Diagram
```
     ┌──────────────────────────────┐
     │ Need DB-specific SQL feature?│
     └──────────────┬───────────────┘
          no       │       yes
                   ▼
     ┌──────────────────────────────┐    ┌─────────────────┐
     │   Use JPQL (portable,        │    │ Native SQL with │
     │   refactor-safe, returns     │    │ nativeQuery=true│
     │   entities or DTOs)          │    └─────────────────┘
     └──────────────────────────────┘
```

---

## 8. `@Modifying` + `@Transactional`

### 8.1 Why You Need Both

By default, `@Query` is treated as a **SELECT**. To run a `UPDATE` / `DELETE` / `INSERT` (bulk JPQL), you must add `@Modifying`. And because writes need a transaction, you also need `@Transactional` — usually placed on the **service** layer (callers), but it's fine to put it on the repository method for self-contained operations.

```java
public interface BookRepository extends JpaRepository<Book, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE Book b SET b.status = :s WHERE b.id IN :ids")
    int updateStatus(@Param("s") BookStatus s, @Param("ids") List<Long> ids);

    @Modifying
    @Transactional
    @Query("DELETE FROM Book b WHERE b.status = :s")
    int deleteByStatus(@Param("s") BookStatus s);
}
```

### 8.2 The Gotcha — Bulk JPQL Bypasses the Persistence Context

```
     em.find(Book, 1) → loaded into context
                            │
     bulkUpdate(...)  ──────┘  changes the DB directly
                                but the entity in the context still has the OLD values!
```

Fixes:
- `@Modifying(clearAutomatically = true)` → clears the persistence context after the bulk op.
- `@Modifying(flushAutomatically = true)` → flushes pending changes before the bulk op runs.

```java
@Modifying(flushAutomatically = true, clearAutomatically = true)
@Transactional
@Query("UPDATE Book b SET b.title = :t WHERE b.id = :id")
int rename(@Param("id") Long id, @Param("t") String t);
```

### 8.3 Return Types for Modifying Queries

Allowed: `int` / `long` (rows affected), `void`, `boolean`. Anything else throws at startup.

### 8.4 Native Modifying
```java
@Modifying
@Transactional
@Query(value = "UPDATE book SET stock = stock - 1 WHERE id = ? AND stock > 0",
       nativeQuery = true)
int decrementStock(Long id);
```

---

## 9. Service Layer Pattern

### 9.1 Why Have a Service Layer?

```
   Controller  ──►  Service  ──►  Repository  ──►  DB
       │              │             │
       │              │             └── pure data access
       │              └── business rules, transactions, orchestration,
       │                  domain validation, cross‑cutting concerns
       └── HTTP concerns: parse/validate request, serialize response
```

Reasons:
- **Single responsibility** — controllers stay thin; repos stay focused.
- **Transactions** belong here (one HTTP call → one business txn).
- **Reusability** — multiple controllers / scheduled jobs / message handlers can call the same service.
- **Testability** — pure POJO that can be unit tested without the web.

### 9.2 Anatomy of a Spring Service

```java
@Service
public class BookService {

    private final BookRepository    bookRepo;
    private final AuthorRepository  authorRepo;
    private final BookMapper        mapper;

    // constructor injection — preferred over @Autowired field injection
    public BookService(BookRepository b, AuthorRepository a, BookMapper m) {
        this.bookRepo = b; this.authorRepo = a; this.mapper = m;
    }

    @Transactional(readOnly = true)
    public List<BookDto> search(String q) {
        return bookRepo.findByTitleContainingIgnoreCase(q)
                       .stream().map(mapper::toDto).toList();
    }

    @Transactional
    public BookDto create(CreateBookRequest req) {
        Author author = authorRepo.findById(req.authorId())
            .orElseThrow(() -> new NotFoundException("Author " + req.authorId()));

        Book book = new Book();
        book.setTitle(req.title());
        book.setIsbn(req.isbn());
        book.setAuthor(author);

        return mapper.toDto(bookRepo.save(book));
    }

    @Transactional
    public BookDto update(Long id, UpdateBookRequest req) {
        Book book = bookRepo.findById(id)
            .orElseThrow(() -> new NotFoundException("Book " + id));
        book.setTitle(req.title());        // dirty‑checked → UPDATE on commit
        return mapper.toDto(book);
    }
}
```

### 9.3 `@Transactional` — Where & How

```
 @Transactional placed on a @Service method:
        │
        │  Spring AOP proxy wraps the call:
        │    1. begin transaction
        │    2. invoke real method
        │    3. on success → commit
        │       on RuntimeException → rollback
        │
        ▼
 All Repository/JPA work inside runs within ONE transaction
 → one Persistence Context, one DB transaction.
```

Important rules:
- The proxy works only on **public** methods called **from outside** the class (self-calls bypass the proxy).
- Default rollback: **runtime / unchecked exceptions only**. Add `rollbackFor = Exception.class` for checked.
- `readOnly = true` is a hint to the driver/Hibernate (can skip dirty checking).
- Common propagation: `REQUIRED` (default — join if exists, else create), `REQUIRES_NEW` (suspend outer, start new).
- Common isolation: `READ_COMMITTED` is the typical default.

### 9.4 Diagram — One HTTP Request

```
   HTTP POST /books
        │
        ▼
   BookController.create(req)
        │  Bean Validation, then
        ▼
   BookService.create(req)        ◄── @Transactional starts here
        │
        ├─► AuthorRepository.findById(...)
        ├─► BookRepository.save(newBook)
        │
        ▼
   Mapper.toDto(book)
        │
   ◄────┘ method returns
        │  → commit (or rollback)
        ▼
   201 Created + JSON body
```

---

## 10. DTOs (Data Transfer Objects)

### 10.1 Why DTOs?

A **DTO** is a flat, dumb data‑carrying object designed to **cross a boundary** (HTTP, message queue, RPC). It is *not* an entity.

```
   Layers:
      Web   ◄── DTOs ──►   Service   ◄── Entities ──►   Repository   ◄──►   DB
```

Reasons to use DTOs over entities at the boundary:

| Problem with returning entities directly | DTO fix |
|------------------------------------------|---------|
| Serializing a managed entity triggers lazy loads → `LazyInitializationException` or N+1 | DTOs hold only loaded data |
| Entity exposes internal fields you don't want over the wire (password, internal flags) | DTOs expose only the public contract |
| Tight coupling: API schema changes whenever DB schema changes | DTOs decouple the two |
| Validation rules differ ("password length on signup" ≠ "stored hashed password") | Distinct request/response DTOs each get their own constraints |
| Bidirectional relations cause infinite JSON loops | DTOs are tree-shaped |

### 10.2 Two Flavors

**Request DTOs** (input):
```java
public record CreateBookRequest(
    @NotBlank @Size(max = 120) String title,
    @NotBlank String isbn,
    @NotNull Long authorId) {}
```

**Response DTOs** (output):
```java
public record BookDto(
    Long id,
    String title,
    String isbn,
    String authorName,
    LocalDateTime createdAt) {}
```

Use Java **records** — they're concise, immutable, ideal for DTOs.

### 10.3 Where Mapping Happens

```
   Controller            Service                 Repository
   ─────────             ─────────               ──────────
                                                                
   CreateBookRequest                                          
        │ pass straight through                                
        ▼                                                      
   create(req) ──── map to entity ───►  save(entity)          
                                              │                
                                              ▼                
                                            Book              
                                              │                
   BookDto  ◄──── map entity to DTO  ◄──────┘                
        │                                                      
        ▼ JSON                                                 
```

Mapping is the responsibility of the **service** (or a dedicated mapper class), **not** the controller and **not** the entity.

---

## 11. MapStruct vs Manual Mapping

### 11.1 Manual Mapping

Plain, explicit Java. Always works, no magic.

```java
@Component
public class BookMapper {

    public BookDto toDto(Book b) {
        return new BookDto(
            b.getId(),
            b.getTitle(),
            b.getIsbn(),
            b.getAuthor() != null ? b.getAuthor().getName() : null,
            b.getCreatedAt());
    }

    public Book toEntity(CreateBookRequest req, Author author) {
        Book b = new Book();
        b.setTitle(req.title());
        b.setIsbn(req.isbn());
        b.setAuthor(author);
        return b;
    }
}
```

**Pros:** zero dependencies, easy to debug, total control.
**Cons:** tedious for big DTOs, easy to forget a field.

### 11.2 MapStruct — Annotation‑Processor Code Generation

**Theory:** MapStruct generates a plain Java implementation of your mapper interface **at compile time** (annotation processor). The generated code is just direct getter/setter calls — fast, debuggable, no reflection.

#### Maven Setup
```xml
<dependency>
  <groupId>org.mapstruct</groupId>
  <artifactId>mapstruct</artifactId>
  <version>1.6.3</version>
</dependency>
<!-- in <build><plugins><plugin>maven-compiler-plugin</plugin></plugins></build> -->
<annotationProcessorPaths>
  <path>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.6.3</version>
  </path>
</annotationProcessorPaths>
```

#### Define a Mapper Interface
```java
@Mapper(componentModel = "spring")                 // makes it a @Component
public interface BookMapper {

    @Mapping(target = "authorName", source = "author.name")
    BookDto toDto(Book book);

    List<BookDto> toDtos(List<Book> books);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", source = "author")
    @Mapping(target = "createdAt", ignore = true)
    Book toEntity(CreateBookRequest req, Author author);
}
```

MapStruct generates `BookMapperImpl.java` that looks like the manual mapper — but you didn't write it.

#### Generated Output (Sketch)
```java
@Component
public class BookMapperImpl implements BookMapper {
    @Override
    public BookDto toDto(Book book) {
        if (book == null) return null;
        String authorName = (book.getAuthor() != null) ? book.getAuthor().getName() : null;
        return new BookDto(book.getId(), book.getTitle(), book.getIsbn(),
                           authorName, book.getCreatedAt());
    }
    // ...
}
```

### 11.3 Manual vs MapStruct — When to Use Which

| | Manual | MapStruct |
|---|---|---|
| Setup | None | Annotation processor + plugin |
| Boilerplate | High | Low |
| Compile‑time safety | Manual | ✅ Yes (missing fields warned/errored) |
| Performance | Fast | Fast (same generated code) |
| Debuggability | Plain code | Plain generated code |
| Complex mappings | Possible | `@Mapping(expression=...)`, custom methods, contexts |
| Learning curve | Zero | Small but real |

**Rule of thumb:**
- Tiny project, 1–2 entities → manual is fine.
- Anything with > 5 DTOs / nested mappings → MapStruct.

### 11.4 Other Options (Briefly)
- **ModelMapper** — reflection‑based; less safe and slower than MapStruct.
- **Records + canonical constructors** — sometimes you don't need a mapper at all.
- **JPA constructor expressions** (`SELECT new pkg.BookDto(...)`) — map *at query time*, skipping entity loading entirely. Great for read‑only views.

---

## 12. End‑to‑End Example

A complete vertical slice tying together everything above.

### 12.1 Entity
```java
@Entity @Table(name = "book")
public class Book {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 13, unique = true)
    private String isbn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;

    @Enumerated(EnumType.STRING) @Column(length = 20)
    private BookStatus status = BookStatus.AVAILABLE;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // getters/setters/no-arg ctor
}
```

### 12.2 DTOs
```java
public record CreateBookRequest(
    @NotBlank @Size(max = 120) String title,
    @NotBlank String isbn,
    @NotNull Long authorId) {}

public record BookDto(
    Long id, String title, String isbn,
    String authorName, BookStatus status,
    LocalDateTime createdAt) {}
```

### 12.3 Repository
```java
public interface BookRepository extends JpaRepository<Book, Long> {

    @EntityGraph(attributePaths = "author")
    Page<Book> findByStatus(BookStatus s, Pageable p);

    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);

    @Modifying @Transactional
    @Query("UPDATE Book b SET b.status = :s WHERE b.id IN :ids")
    int updateStatus(@Param("s") BookStatus s, @Param("ids") List<Long> ids);

    @Query("""
        SELECT new com.example.BookSummary(b.id, b.title, b.author.name)
        FROM Book b WHERE b.status = :s""")
    List<BookSummary> summaries(@Param("s") BookStatus s);
}
```

### 12.4 Mapper (MapStruct)
```java
@Mapper(componentModel = "spring")
public interface BookMapper {
    @Mapping(target = "authorName", source = "author.name")
    BookDto toDto(Book b);
}
```

### 12.5 Service
```java
@Service
public class BookService {

    private final BookRepository    books;
    private final AuthorRepository  authors;
    private final BookMapper        mapper;

    public BookService(BookRepository b, AuthorRepository a, BookMapper m) {
        this.books = b; this.authors = a; this.mapper = m;
    }

    @Transactional(readOnly = true)
    public Page<BookDto> list(BookStatus s, Pageable p) {
        return books.findByStatus(s, p).map(mapper::toDto);
    }

    @Transactional
    public BookDto create(CreateBookRequest req) {
        if (books.existsByIsbn(req.isbn()))
            throw new DuplicateException("ISBN exists");

        Author author = authors.findById(req.authorId())
            .orElseThrow(() -> new NotFoundException("Author " + req.authorId()));

        Book b = new Book();
        b.setTitle(req.title());
        b.setIsbn(req.isbn());
        b.setAuthor(author);
        return mapper.toDto(books.save(b));
    }
}
```

### 12.6 Controller
```java
@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService service;
    public BookController(BookService service) { this.service = service; }

    @GetMapping
    public Page<BookDto> list(@RequestParam(defaultValue = "AVAILABLE") BookStatus status,
                              Pageable pageable) {
        return service.list(status, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookDto create(@Valid @RequestBody CreateBookRequest req) {
        return service.create(req);
    }
}
```

### 12.7 Request Flow Diagram

```
  POST /books  {title,isbn,authorId}
        │
        ▼
  BookController.create
        │  @Valid → Bean Validation runs on CreateBookRequest
        ▼
  BookService.create              ── @Transactional → tx begins
        │
        ├─► BookRepository.existsByIsbn(...)
        ├─► AuthorRepository.findById(...)
        ├─► BookRepository.save(book)
        │       │  Hibernate flushes INSERT
        │       ▼
        │     book.id is now set
        ▼
  BookMapper.toDto(book)
        │
   ◄────┘ tx commits → response 201 Created + JSON
```

---

## 13. Cheat Sheet & Best Practices

### 13.1 Do / Don't

| ✅ Do | ❌ Don't |
|------|---------|
| Use `Long` (or `UUID`) ids | Use `int` |
| `@Enumerated(EnumType.STRING)` | Use `EnumType.ORDINAL` (catastrophic on reorder) |
| All `@ManyToOne`/`@OneToOne` → `LAZY` | Leave them EAGER |
| Use DTOs at the web boundary | Return entities to controllers |
| Use constructor injection | `@Autowired` field injection |
| `@Transactional` on services | Put it on controllers |
| `@Modifying(flushAutomatically=true, clearAutomatically=true)` for bulk updates | Forget the persistence-context staleness |
| Promote `@ManyToMany` join tables to an `@Entity` when they have attributes | Cram payload into the join table without entity |
| `Set` for `@ManyToMany` collections | `List` (causes Hibernate to issue delete+reinsert all) |
| Use records for DTOs | Use entities everywhere |
| Keep query method names ≤ 3 conditions, then go `@Query` | 8-word method names |

### 13.2 The Big Picture

```
   @RestController
        │  (DTOs in/out)
        ▼
   @Service @Transactional
        │  (maps DTO ↔ Entity via MapStruct/manual)
        ▼
   JpaRepository (Spring Data generated impl)
        │     - derived queries (findByX)
        │     - @Query (JPQL / native)
        │     - @Modifying + @Transactional for writes
        ▼
   EntityManager  (JPA spec)
        ▼
   Hibernate  (engine)
        ▼
   HikariCP → JDBC → Database
```

### 13.3 Mental Map of the Annotations

```
   @Entity / @Table          → "I'm a row in this table"
   @Id / @GeneratedValue     → "This is my PK and how it's made"
   @Column                   → column tuning
   @Transient                → don't store
   @ManyToOne / @OneToMany   → relationships (mind the owning side)
   @JoinColumn / @JoinTable  → where the FK lives
   @Query / @Modifying       → custom queries (read / write)
   @Param                    → bind named params
   @Transactional            → unit of work boundary
   @Mapper (MapStruct)       → generated DTO mappers
   @Valid + JSR-380          → request validation
```

---

## TL;DR

- **`@Entity` / `@Table` / `@Id` / `@GeneratedValue`** wire a class to a table and define its PK.
- **`@Column`** tunes the column; **`@Transient`** skips persistence.
- **Relationships**: cardinality + direction. The "many" side owns the FK; the inverse uses `mappedBy`. Default `@ManyToOne` is EAGER — override to LAZY. Use a join table for `@ManyToMany`; promote it to an entity when it has data.
- **Repository hierarchy**: `Repository` ⊂ `CrudRepository` ⊂ `PagingAndSortingRepository` ⊂ `JpaRepository`. Spring Data generates impls from interface declarations.
- **Derived query methods** parse method names → JPQL. Operators: `Containing`, `Between`, `In`, `IgnoreCase`, `OrderBy`, `Top10`, etc.
- **`@Query`** for JPQL (portable) or native SQL (vendor‑specific). Use **constructor expressions** or **interface projections** to load DTOs directly.
- **`@Modifying` + `@Transactional`** for `UPDATE`/`DELETE` JPQL. Add `clearAutomatically = true` to avoid stale persistence context.
- **Service Layer** owns transactions and business rules. Controllers stay thin.
- **DTOs** decouple the API contract from the entity model. Use records.
- **MapStruct** generates mapping code at compile time — preferred over manual mapping at scale.
