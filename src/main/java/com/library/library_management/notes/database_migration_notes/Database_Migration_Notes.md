# Database Migration — Flyway & Liquibase (Deep‑Dive Notes v2)

> **Goal:** Understand *why* database migrations matter, *how* Flyway and Liquibase work internally, and *when* to use each — with richer theory, more diagrams, and runnable examples for our Spring Boot `library-management` project.

---

## Table of Contents
1. [Why Database Migration Exists](#1-why-database-migration-exists)
2. [The Mental Model](#2-the-mental-model)
3. [Flyway — Concepts](#3-flyway--concepts)
4. [Flyway — Spring Boot Integration & Examples](#4-flyway--spring-boot-integration--examples)
5. [Liquibase — Concepts](#5-liquibase--concepts)
6. [Liquibase — Spring Boot Integration & Examples](#6-liquibase--spring-boot-integration--examples)
7. [Flyway vs Liquibase](#7-flyway-vs-liquibase)
8. [Zero‑Downtime Migration Patterns](#8-zero-downtime-migration-patterns)
9. [Best Practices & Pitfalls](#9-best-practices--pitfalls)
10. [Testing & CI Strategy](#10-testing--ci-strategy)
11. [TL;DR](#tldr)

---

## 1. Why Database Migration Exists

### 1.1 The Problem of Drift

Your **code** is versioned in Git with branches, tags, and reproducible builds. Your **database schema** lives inside the database server — but it changes too (new columns, dropped indexes, renamed tables, seed data). Without a process, schema drifts across environments:

```
   ┌────────────┐        ┌────────────┐        ┌────────────┐
   │ Developer  │        │   Staging  │        │ Production │
   │  schema?   │   ≠    │  schema?   │   ≠    │  schema?   │
   └────────────┘        └────────────┘        └────────────┘
        │                      │                     │
        │  "works on my        │  "missing column"   │  "boom at 3 am"
        │   machine"           │                     │
```

Three forces make this worse:
- **Many developers** changing the schema concurrently.
- **Many environments** (dev / test / staging / prod / replicas).
- **Long‑lived data** that can't be wiped and recreated.

### 1.2 What "Database Migration" Means

A **migration** = a small, ordered, **versioned** SQL/DSL script that evolves the schema from one state to the next. Tools (Flyway, Liquibase) maintain a **history table** inside the database recording which migrations have been applied. At every startup they:

1. Look at migration scripts on the classpath.
2. Compare them with the history table.
3. Apply the missing ones, **in order**, **in a transaction**.
4. Record the result (version, checksum, timestamp, success).

```
   code (V1, V2, V3, V4)             history table in DB
   ─────────────────────             ─────────────────────────
   V1__init.sql              ──►     V1  ✓  2026‑01‑02
   V2__add_isbn.sql          ──►     V2  ✓  2026‑02‑10
   V3__add_fines.sql         ──►     V3  ✓  2026‑03‑05
   V4__add_index.sql         ──►     V4  ← missing, apply now ➜ ✓ today
```

### 1.3 Why Not Just `ddl-auto=update`?

Hibernate's `spring.jpa.hibernate.ddl-auto=update` *can* alter the schema, but:

| Problem | Why migrations win |
|---------|--------------------|
| Non‑deterministic order | Migrations are explicit & versioned. |
| Can't drop or rename | Migrations express *any* DDL. |
| No history / audit | History table records who / when / checksum. |
| No data migration | Migrations can include DML / backfill. |
| Risky in production | `update` is disabled there anyway. |

> **Rule:** in production `ddl-auto=validate` (or `none`), all schema changes via Flyway or Liquibase.

### 1.4 What "Good" Looks Like

```
   developer ──► writes entity + migration ──► PR
                                  │
                                  ▼
              CI: build → unit tests → spin Postgres in Docker
                       → run migrations → integration tests
                                  │
                                  ▼
                          deploy to staging
                                  │
                                  ▼
                          deploy to production
                 (migration runs once, in a transaction,
                  before app accepts traffic)
```

Repeatable, reviewable, auditable, reversible.

---

## 2. The Mental Model

```
   ┌─────────────────────────────────────────────────────────────────┐
   │                         APP STARTS UP                           │
   │  Spring Boot autoconfig sees Flyway/Liquibase on the classpath  │
   │                              │                                  │
   │                              ▼                                  │
   │   Migration tool connects via DataSource                        │
   │                              │                                  │
   │                              ▼                                  │
   │   ┌─────────────────────────────────────────────────────────┐   │
   │   │  1. Ensure history table exists                         │   │
   │   │     (flyway_schema_history  /  DATABASECHANGELOG)       │   │
   │   │  2. Scan classpath for migrations                       │   │
   │   │  3. Compute "pending" = on disk − already applied       │   │
   │   │  4. For each pending, in order:                         │   │
   │   │        BEGIN; run SQL; record row; COMMIT;              │   │
   │   │  5. Validate checksums of already‑applied migrations    │   │
   │   └─────────────────────────────────────────────────────────┘   │
   │                              │                                  │
   │                              ▼                                  │
   │             Hand control back to Spring                         │
   │      (JPA / Hibernate now sees a correct schema)                │
   └─────────────────────────────────────────────────────────────────┘
```

Two big ideas every team must internalize:

- **Immutability of applied migrations.** Once V3 is in production, you **never edit** V3. You add V4. This is what makes migrations reproducible across environments.
- **Checksum validation.** Each applied migration's content hash is stored. If you change V3 later, the tool refuses to start.

State machine of a single migration:

```
                discovered on classpath
                        │
                        ▼
                   ┌─────────┐
                   │ PENDING │
                   └────┬────┘
              BEGIN tx  │
                        ▼
                   ┌──────────┐   error
                   │ RUNNING  │ ─────────► ┌──────────┐
                   └────┬─────┘            │  FAILED  │ (rollback;
              COMMIT    │                  └──────────┘  history row
                        ▼                                 marked failed
                   ┌──────────┐                           in some tools)
                   │ APPLIED  │ ── checksum stored
                   └──────────┘
```

---

## 3. Flyway — Concepts

### 3.1 What It Is
Flyway = a small Java library that runs **SQL files** (or Java callbacks) against your DB in a fixed order, and tracks them in a single history table. Philosophy: *"SQL is the lingua franca. Stay close to it."*

### 3.2 File Naming Convention

```
  V<version>__<description>.sql       ← versioned, runs once, in order
  U<version>__<description>.sql       ← undo (paid Teams edition)
  R__<description>.sql                ← repeatable (re-runs when content changes)
```

Visual mental map:

```
   V1__init.sql       ──┐
   V1.1__patch.sql    ──┼──► versioned (immutable once applied)
   V2__add_fines.sql  ──┘

   R__views.sql       ──── repeatable (hash‑tracked, re‑runs on change)

   U2__undo_fines.sql ──── undo (Teams only)
```

Rules:
- **Two underscores** between version and description.
- Version uses dot/underscore segments: `1`, `1.1`, `2_0_3`.
- Sorted naturally; `V1` < `V1.1` < `V2`.

Examples for our project:
```
  src/main/resources/db/migration/
    V1__init_schema.sql
    V2__add_isbn_to_book.sql
    V2.1__add_fines.sql
    V3__seed_membership_types.sql
    R__refresh_top_borrowers_view.sql
```

### 3.3 Lifecycle / Internal Flow

```
  ┌────────────────────┐
  │  flyway migrate    │  (auto on Spring Boot start)
  └─────────┬──────────┘
            ▼
  ┌─────────────────────────────────────────────────────────┐
  │ 1. Acquire JDBC connection                              │
  │ 2. Create flyway_schema_history if absent               │
  │ 3. SCAN classpath:/db/migration/*.sql                   │
  │ 4. Read history → set of applied versions               │
  │ 5. Diff → pending list, ordered                         │
  │ 6. VALIDATE: re‑hash each applied script,               │
  │              compare with stored checksum               │
  │ 7. For each pending:                                    │
  │       BEGIN                                             │
  │         execute statements                              │
  │         insert row in history (version, checksum, ts)   │
  │       COMMIT  (or ROLLBACK on error)                    │
  │ 8. Release connection                                   │
  └─────────────────────────────────────────────────────────┘
```

### 3.4 The History Table

```
  flyway_schema_history
  ─────────────────────────────────────────────────────────────────────
  installed_rank | version | description       | type | script              | checksum   | success | execution_time
  ───────────────|─────────|───────────────────|──────|─────────────────────|────────────|─────────|───────────────
  1              | 1       | init schema       | SQL  | V1__init_schema.sql | 9384571023 |  TRUE   | 142 ms
  2              | 2       | add isbn to book  | SQL  | V2__add_isbn…sql    | 4471092834 |  TRUE   |  31 ms
```

### 3.5 Key Commands

| Command | Effect | Safe in prod? |
|---------|--------|---------------|
| `migrate`  | Apply pending migrations (default at startup). | ✅ |
| `info`     | Show status of each migration.                 | ✅ read‑only |
| `validate` | Re‑check checksums; fail if drift.             | ✅ read‑only |
| `clean`    | **DROP all objects** — dev only!                | ❌ never |
| `repair`   | Fix history table (checksum / failed row).     | ⚠️ with care |
| `baseline` | Mark current DB as baseline V1 (legacy DBs).   | ⚠️ once per DB |

### 3.6 Callbacks & Java‑Based Migrations
Beyond SQL files, Flyway supports:
- **Java migrations** — class `V4__SomeChange implements JdbcMigration`, useful for complex backfills.
- **Callbacks** — `beforeMigrate`, `afterEachMigrate`, `afterMigrateError` (SQL files in `callbacks/` or `Callback` Java classes).

---

## 4. Flyway — Spring Boot Integration & Examples

### 4.1 Dependency
```xml
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-core</artifactId>
</dependency>
<!-- MySQL 8+ requires this extra module -->
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-mysql</artifactId>
</dependency>
```

### 4.2 Configuration (`application.properties`)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/library
spring.datasource.username=root
spring.datasource.password=secret

# Migration toggle (default true when on classpath)
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true   # for an existing DB
spring.flyway.validate-on-migrate=true   # verify checksums
spring.flyway.out-of-order=false         # production default

# JPA: never touch DDL
spring.jpa.hibernate.ddl-auto=validate
```

### 4.3 Example Migrations for `library-management`

`src/main/resources/db/migration/V1__init_schema.sql`
```sql
CREATE TABLE member (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(120)   NOT NULL,
    email           VARCHAR(160)   NOT NULL UNIQUE,
    membership_type VARCHAR(20)    NOT NULL,
    joined_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE book (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    title     VARCHAR(200)  NOT NULL,
    author    VARCHAR(160)  NOT NULL,
    isbn      VARCHAR(20)   NOT NULL UNIQUE,
    copies    INT           NOT NULL DEFAULT 1
);

CREATE TABLE borrow_record (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT NOT NULL,
    book_id     BIGINT NOT NULL,
    borrowed_at TIMESTAMP NOT NULL,
    due_at      TIMESTAMP NOT NULL,
    returned_at TIMESTAMP NULL,
    status      VARCHAR(20) NOT NULL,
    CONSTRAINT fk_br_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT fk_br_book   FOREIGN KEY (book_id)   REFERENCES book(id)
);
```

`V2__add_fines.sql`
```sql
CREATE TABLE fine (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    borrow_record_id  BIGINT NOT NULL UNIQUE,
    amount            DECIMAL(10,2) NOT NULL,
    paid              BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fine_br FOREIGN KEY (borrow_record_id) REFERENCES borrow_record(id)
);

CREATE INDEX idx_borrow_record_status ON borrow_record(status);
```

`V3__seed_membership_types.sql`
```sql
INSERT INTO member (name, email, membership_type)
VALUES ('Admin Seed', 'admin@library.local', 'PREMIUM');
```

`R__refresh_top_borrowers_view.sql`  (repeatable — re‑runs whenever its hash changes)
```sql
CREATE OR REPLACE VIEW top_borrowers AS
SELECT m.id, m.name, COUNT(br.id) AS total_borrows
FROM member m
LEFT JOIN borrow_record br ON br.member_id = m.id
GROUP BY m.id, m.name;
```

### 4.4 Startup Log You Should See
```
o.f.c.internal.license.VersionPrinter   : Flyway Community Edition 9.x by Redgate
o.f.c.i.database.base.BaseDatabaseType  : Database: jdbc:mysql://localhost:3306/library (MySQL 8.0)
o.f.core.internal.command.DbMigrate     : Current version of schema `library`: << Empty Schema >>
o.f.core.internal.command.DbMigrate     : Migrating schema `library` to version "1 - init schema"
o.f.core.internal.command.DbMigrate     : Migrating schema `library` to version "2 - add fines"
o.f.core.internal.command.DbMigrate     : Successfully applied 2 migrations to schema `library`
```

### 4.5 Day‑in‑the‑Life Workflow
```
  developer edits entity ──►  add V4__add_column.sql
                                       │
                                       ▼
                          git commit + PR
                                       │
                                       ▼
   CI builds → tests run → Flyway applies V4 on test DB ──► ✓
                                       │
                                       ▼
                            deploy to staging → V4 applied
                                       │
                                       ▼
                              deploy to prod → V4 applied
```

### 4.6 Common Errors & Fixes
| Symptom | Cause | Fix |
|--------|-------|-----|
| `Migration checksum mismatch` | You edited an already‑applied script | Revert edit, OR `flyway repair`, OR add new migration |
| `Found non-empty schema(s) without schema history table` | Existing DB | `spring.flyway.baseline-on-migrate=true` |
| `Detected resolved migration not applied to database` | Out‑of‑order version on disk | Set `out-of-order=true` carefully, or rename |
| Hangs forever on startup | Another instance holds connection / lock | Single‑migrator pod; check `SHOW PROCESSLIST` |

---

## 5. Liquibase — Concepts

### 5.1 What It Is
Liquibase = a DB migration tool centered on **changelogs**: structured documents (XML / YAML / JSON / SQL) of **changesets**. Each changeset is identified by `(id, author, filename)` — not by a filename version. Philosophy: *"Describe changes abstractly so they're portable, introspectable, and reversible."*

### 5.2 Hierarchy

```
  databaseChangeLog  (master file)
   │
   ├── include: changes/2026/01-init.yaml
   │     ├── changeSet id="1" author="ali"   ──► CREATE TABLE member
   │     └── changeSet id="2" author="ali"   ──► CREATE TABLE book
   │
   ├── include: changes/2026/02-add-fines.yaml
   │     └── changeSet id="3" author="ali"   ──► CREATE TABLE fine
   │
   └── include: changes/2026/03-seed.yaml
         └── changeSet id="4" author="ali" context="dev"
                                                  ──► INSERT seed data
```

A **changeset** can contain:
- Built‑in **change types** (`createTable`, `addColumn`, `addForeignKeyConstraint`, …) → dialect‑independent.
- Raw SQL (`<sql>` / `sql:` block) or formatted SQL files.
- Annotations: `context` (env tag), `labels` (orthogonal tag), `preConditions`, `runAlways`, `runOnChange`, `rollback`.

### 5.3 Tracking Tables

```
  DATABASECHANGELOG            ── one row per applied changeset
  DATABASECHANGELOGLOCK        ── single‑row advisory lock so two
                                  instances don't migrate concurrently
```

`DATABASECHANGELOG` columns include: `ID`, `AUTHOR`, `FILENAME`, `DATEEXECUTED`, `ORDEREXECUTED`, `EXECTYPE`, `MD5SUM`, `DESCRIPTION`, `COMMENTS`, `TAG`, `LIQUIBASE`, `CONTEXTS`, `LABELS`, `DEPLOYMENT_ID`.

### 5.4 Internal Lifecycle

```
  ┌────────────────────────────┐
  │ liquibase update           │
  └────────────┬───────────────┘
               ▼
  1. Connect via DataSource
  2. Acquire DATABASECHANGELOGLOCK row (advisory lock)
  3. Ensure DATABASECHANGELOG exists
  4. Parse master changelog (recurse <include> / <includeAll>)
  5. For each changeSet in order:
       a. Compute key = (id, author, filename)
       b. If already in DATABASECHANGELOG → skip
             (unless runAlways="true" or content changed & runOnChange="true")
       c. Else:
            BEGIN
              evaluate preConditions
              generate SQL via change types (dialect aware)
              execute
              insert DATABASECHANGELOG row with MD5
            COMMIT  (or ROLLBACK)
  6. Release lock
```

### 5.5 Killer Features
- **Rollback** built‑in: every change has an inverse (`dropTable` ↔ `createTable`); custom rollback per changeset.
- **Preconditions** — run only if certain SQL/state is true (`columnExists`, `sqlCheck`, `rowCount`, …).
- **Contexts / labels** — apply seed data only in `dev`, only in `eu‑west`, etc.
- **Diff tool** — generate a changelog from an existing DB or compare two DBs.
- **Multi‑format** — XML / YAML / JSON / SQL; mix per team taste.
- **Tags** — checkpoint the DB state ("release‑2026.05.01") and roll back to it.

### 5.6 Context vs Label — Don't Confuse Them

```
   contexts  ──── set at run time (env / phase):  --contexts=dev,seed
                  changeSet runs if its context matches any of those

   labels    ──── boolean expression at run time: --labels="(eu OR us) AND !legacy"
                  more powerful filter; orthogonal to contexts
```

Use **contexts** for *where/when* (dev/test/prod), **labels** for *feature flags* (`feature-fines`, `pilot`).

---

## 6. Liquibase — Spring Boot Integration & Examples

### 6.1 Dependency
```xml
<dependency>
  <groupId>org.liquibase</groupId>
  <artifactId>liquibase-core</artifactId>
</dependency>
```

### 6.2 Configuration
```properties
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml
spring.liquibase.contexts=dev
spring.liquibase.default-schema=library
spring.jpa.hibernate.ddl-auto=validate
```

### 6.3 Master Changelog (YAML)

`src/main/resources/db/changelog/db.changelog-master.yaml`
```yaml
databaseChangeLog:
  - include: { file: db/changelog/changes/01-init.yaml }
  - include: { file: db/changelog/changes/02-add-fines.yaml }
  - include: { file: db/changelog/changes/03-seed.yaml }
```

### 6.4 First Changelog — `01-init.yaml`
```yaml
databaseChangeLog:
  - changeSet:
      id: 1
      author: ali
      changes:
        - createTable:
            tableName: member
            columns:
              - column: { name: id, type: BIGINT, autoIncrement: true,
                          constraints: { primaryKey: true, nullable: false } }
              - column: { name: name,            type: VARCHAR(120),
                          constraints: { nullable: false } }
              - column: { name: email,           type: VARCHAR(160),
                          constraints: { nullable: false, unique: true } }
              - column: { name: membership_type, type: VARCHAR(20),
                          constraints: { nullable: false } }
              - column: { name: joined_at,       type: TIMESTAMP,
                          defaultValueComputed: CURRENT_TIMESTAMP,
                          constraints: { nullable: false } }

  - changeSet:
      id: 2
      author: ali
      changes:
        - createTable:
            tableName: book
            columns:
              - column: { name: id,     type: BIGINT, autoIncrement: true,
                          constraints: { primaryKey: true, nullable: false } }
              - column: { name: title,  type: VARCHAR(200), constraints: { nullable: false } }
              - column: { name: author, type: VARCHAR(160), constraints: { nullable: false } }
              - column: { name: isbn,   type: VARCHAR(20),
                          constraints: { nullable: false, unique: true } }
              - column: { name: copies, type: INT, defaultValueNumeric: 1,
                          constraints: { nullable: false } }

  - changeSet:
      id: 3
      author: ali
      changes:
        - createTable:
            tableName: borrow_record
            columns:
              - column: { name: id,          type: BIGINT, autoIncrement: true,
                          constraints: { primaryKey: true } }
              - column: { name: member_id,   type: BIGINT, constraints: { nullable: false } }
              - column: { name: book_id,     type: BIGINT, constraints: { nullable: false } }
              - column: { name: borrowed_at, type: TIMESTAMP, constraints: { nullable: false } }
              - column: { name: due_at,      type: TIMESTAMP, constraints: { nullable: false } }
              - column: { name: returned_at, type: TIMESTAMP }
              - column: { name: status,      type: VARCHAR(20), constraints: { nullable: false } }
        - addForeignKeyConstraint:
            baseTableName: borrow_record
            baseColumnNames: member_id
            referencedTableName: member
            referencedColumnNames: id
            constraintName: fk_br_member
        - addForeignKeyConstraint:
            baseTableName: borrow_record
            baseColumnNames: book_id
            referencedTableName: book
            referencedColumnNames: id
            constraintName: fk_br_book
```

### 6.5 `02-add-fines.yaml`  with rollback
```yaml
databaseChangeLog:
  - changeSet:
      id: 4
      author: ali
      labels: feature-fines
      changes:
        - createTable:
            tableName: fine
            columns:
              - column: { name: id, type: BIGINT, autoIncrement: true,
                          constraints: { primaryKey: true } }
              - column: { name: borrow_record_id, type: BIGINT,
                          constraints: { nullable: false, unique: true } }
              - column: { name: amount, type: DECIMAL(10,2),
                          constraints: { nullable: false } }
              - column: { name: paid,   type: BOOLEAN, defaultValueBoolean: false,
                          constraints: { nullable: false } }
              - column: { name: created_at, type: TIMESTAMP,
                          defaultValueComputed: CURRENT_TIMESTAMP }
        - addForeignKeyConstraint:
            baseTableName: fine
            baseColumnNames: borrow_record_id
            referencedTableName: borrow_record
            referencedColumnNames: id
            constraintName: fk_fine_br
      rollback:
        - dropTable: { tableName: fine }

  - changeSet:
      id: 5
      author: ali
      changes:
        - createIndex:
            tableName: borrow_record
            indexName: idx_borrow_record_status
            columns:
              - column: { name: status }
      rollback:
        - dropIndex:
            tableName: borrow_record
            indexName: idx_borrow_record_status
```

### 6.6 `03-seed.yaml`  with context + precondition
```yaml
databaseChangeLog:
  - changeSet:
      id: 6
      author: ali
      context: dev          # only runs when spring.liquibase.contexts=dev
      preConditions:
        - onFail: MARK_RAN
        - sqlCheck:
            expectedResult: 0
            sql: SELECT COUNT(*) FROM member WHERE email='admin@library.local'
      changes:
        - insert:
            tableName: member
            columns:
              - column: { name: name,            value: "Admin Seed" }
              - column: { name: email,           value: "admin@library.local" }
              - column: { name: membership_type, value: "PREMIUM" }
```

### 6.7 Startup Log
```
liquibase.executor.jvm.JdbcExecutor : SELECT * FROM library.DATABASECHANGELOGLOCK
liquibase.changelog                 : Reading from library.DATABASECHANGELOG
liquibase.changelog                 : ChangeSet 01-init.yaml::1::ali ran successfully in 86ms
liquibase.changelog                 : ChangeSet 01-init.yaml::2::ali ran successfully in 22ms
...
liquibase.lockservice               : Successfully released change log lock
```

### 6.8 Rollback Demo
```
  ┌─────────────┐    update    ┌─────────────┐
  │  schema v3  │ ───────────► │  schema v4  │
  └─────────────┘              └──────┬──────┘
                                      │ rollbackCount 1  (or rollback to tag)
                                      ▼
                               ┌─────────────┐
                               │  schema v3  │
                               └─────────────┘
```
CLI:
```
liquibase --changeLogFile=db/changelog/db.changelog-master.yaml rollbackCount 1
liquibase --changeLogFile=...                                  tag release-2026-05-01
liquibase --changeLogFile=...                                  rollback release-2026-05-01
```

---

## 7. Flyway vs Liquibase

### 7.1 Side‑by‑Side Architecture

```
   ┌────────────────────────────┐      ┌──────────────────────────────────┐
   │           FLYWAY           │      │           LIQUIBASE              │
   ├────────────────────────────┤      ├──────────────────────────────────┤
   │ Scripts: SQL files         │      │ Changelogs: XML/YAML/JSON/SQL    │
   │ Identity: filename version │      │ Identity: (id, author, filename) │
   │ History:  flyway_schema_   │      │ History:  DATABASECHANGELOG      │
   │           history          │      │           + LOCK table           │
   │ Rollback: undo files (pro) │      │ Rollback: built‑in inverse ops   │
   │ Style:   procedural / SQL  │      │ Style:   declarative DSL         │
   │ Diff:    no                │      │ Diff:    yes (generate changelog)│
   │ Best for: SQL‑savvy teams  │      │ Best for: multi-DB, governed env │
   └────────────────────────────┘      └──────────────────────────────────┘
```

### 7.2 Behavior Comparison

| Concern | Flyway | Liquibase |
|--------|--------|-----------|
| Onboarding (SQL devs) | trivial | requires DSL |
| Cross‑DB portability | manual | strong (change types per dialect) |
| Rollback | paid / manual | free, declarative |
| Conditional migrations | limited | strong (`context`, `labels`, `preConditions`) |
| File format flexibility | SQL (+Java) | XML/YAML/JSON/SQL |
| Diff against live DB | no | yes |
| Lock during run | implicit (single tx) | explicit lock table |
| Speed of learning | very fast | medium |
| Tooling ecosystem | huge, simple | huge, enterprise-friendly |

### 7.3 Decision Flowchart

```
                ┌──────────────────────────────┐
                │ Do you need rollback         │
                │ or multi‑DB portability?     │
                └──────────────┬───────────────┘
                          yes  │   no
                  ┌────────────┴────────────┐
                  ▼                         ▼
            ┌──────────┐              ┌──────────┐
            │Liquibase │              │  Flyway  │
            └────┬─────┘              └────┬─────┘
                 │                         │
                 ▼                         ▼
       SQL‑averse team?              SQL‑heavy team
       Governed enterprise?          Single DB engine?
       (Liquibase wins)              (Flyway wins)
```

### 7.4 Rule of Thumb
- **Just MySQL/Postgres, small team, SQL‑first** → **Flyway**.
- **Multiple target DBs, audit/governance, rollback critical** → **Liquibase**.
- **Never use both in the same project.**

---

## 8. Zero‑Downtime Migration Patterns

DDL on a busy table can lock writers for seconds → minutes. Use the **expand → migrate → contract** pattern across multiple releases.

### 8.1 The Pattern

```
   release N           release N+1               release N+2
   ──────────          ─────────────             ────────────
   EXPAND              MIGRATE                   CONTRACT
   add new column      app writes both old&new   drop old column
   (nullable)          backfill old → new        (after read traffic
                       dual reads switch          fully on new)
                       new‑first
```

### 8.2 Common Recipes

| Change | Safe steps |
|--------|-----------|
| **Add column** | Always nullable first → backfill async → set NOT NULL in a later migration. |
| **Rename column** | Add new → dual‑write app code → backfill → switch reads → drop old. |
| **Change type** | Add new typed column → backfill → switch app → drop old. |
| **Drop column** | Stop writing in app → stop reading → drop in a *separate* release. |
| **Add index on big table** | Use `CREATE INDEX CONCURRENTLY` (Postgres) / online DDL (MySQL 8). |
| **Add FK on big table** | Add column nullable → backfill → add FK `NOT VALID` → `VALIDATE CONSTRAINT`. |

### 8.3 The "Big Backfill" Anti‑Pattern

Long DML inside a startup migration blocks the whole deploy. Split it:

```
   V10__add_nullable_column.sql       (fast DDL)
   V11__backfill_chunked.sql          (small / NOOP at startup)
        ──► async background job fills data in batches
   V12__not_null_and_index.sql        (after data is filled)
```

### 8.4 Why a Single Tx Isn't Always Enough
- Many DBs **can't roll back DDL** (MySQL pre‑8.0 auto‑commits each DDL).
- Splitting DDL across small migrations limits blast radius.
- Postgres can run DDL in a transaction, but `CREATE INDEX CONCURRENTLY` **cannot** — keep it in its own migration.

---

## 9. Best Practices & Pitfalls

### 9.1 Do / Don't

| ✅ Do | ❌ Don't |
|------|---------|
| Treat applied migrations as immutable | Edit V3 after it shipped |
| Keep migrations small & focused | Pack 12 unrelated changes in one file |
| Run migrations in CI against a real DB | Trust only the dev DB |
| Use `ddl-auto=validate` in prod | Leave `update` / `create` in prod |
| Add destructive ops as **separate** migrations | Mix `DROP COLUMN` with a feature |
| Make migrations idempotent where possible | Assume "table not exists" |
| One transaction per migration (default) | Cram DDL + huge data backfill together |
| Backfill large data via batched jobs | Run a 10M‑row UPDATE inside startup |
| Tag releases (`flyway info` / `liquibase tag`) | Lose track of what's deployed where |
| Test rollback on dev | Discover rollback is broken in prod |
| Single migrator pod / leader election | Race conditions across replicas |

### 9.2 Hibernate Coexistence Checklist
- `spring.jpa.hibernate.ddl-auto=validate` (or `none`).
- Run Flyway/Liquibase **before** JPA initialization (default in Spring Boot — autoconfig orders it).
- Keep entities + migrations in sync — CI fails if `validate` complains.
- Naming convention alignment (snake_case in DB ↔ `@Column(name="...")` in code, or use a `PhysicalNamingStrategy`).

### 9.3 Common Pitfalls

| Pitfall | Symptom | Remedy |
|--------|---------|--------|
| Editing applied migration | `Migration checksum mismatch` (Flyway) / `MD5 sum check failed` (Liquibase) | Revert + new migration, or `repair`/`clearCheckSums` |
| Two pods migrating in parallel | Random deadlocks / duplicate DDL errors | Liquibase lock table handles this; for Flyway, single migrator (init container, leader election, or k8s `Job`) |
| Using `ddl-auto=update` alongside migrations | Schema diverges silently | Set to `validate` |
| No baseline on legacy DB | "Found non‑empty schema" error | `baseline-on-migrate=true` |
| Long migration blocking startup | Health checks fail, k8s rollback loop | Move heavy DML to background jobs |
| Mixing seed and schema | Seed re‑applied in prod | Liquibase: context `dev`. Flyway: gate with profile `spring.flyway.locations` |
| Dropping a column still read by old pods (rolling deploy) | `column not found` in still‑running instances | Use expand/contract pattern |

### 9.4 Naming Conventions That Save Pain
- Lowercase, snake_case for all DB identifiers.
- Plural or singular table names — pick one, enforce.
- Foreign keys: `fk_<table>_<refTable>`.
- Indexes:     `idx_<table>_<col1>_<col2>`.
- Uniques:     `uq_<table>_<col>`.
- Migration descriptions in present tense (`add_isbn_to_book`, not `added_isbn_…`).

---

## 10. Testing & CI Strategy

### 10.1 Locally
- **Testcontainers** spin a fresh DB per test class — migrations always run from scratch → catches "works on existing dev DB" bugs.
```java
@Testcontainers
@SpringBootTest
class MigrationIT {
    @Container
    static MySQLContainer<?> db = new MySQLContainer<>("mysql:8");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", db::getJdbcUrl);
        r.add("spring.datasource.username", db::getUsername);
        r.add("spring.datasource.password", db::getPassword);
    }

    @Test void contextLoads() {}    // migration ran during boot
}
```

### 10.2 In CI
Pipeline order:

```
   1. unit tests           (no DB)
   2. start ephemeral DB   (Docker)
   3. run migrations       (Flyway/Liquibase)
   4. integration tests    (JPA, repositories, services)
   5. teardown DB
```

### 10.3 Before Production
- `flyway info` / `liquibase status` in pre‑deploy step prints pending migrations → reviewed by ops.
- Optional dry‑run: `flyway migrate -dryRunOutput=plan.sql` / `liquibase updateSql` to see exact SQL.
- Tag the release: `liquibase tag release-2026-05-01`.

### 10.4 Disaster Recovery Drill
Practice once per quarter:
- Restore a prod snapshot in staging.
- Run latest migrations.
- Roll back one release (Liquibase) or hand‑rolled undo (Flyway).
- Verify app boots and a smoke test passes.

---

## TL;DR
- **Why migrations:** keep schema in lockstep with code, across environments, reproducibly.
- **Mental model:** versioned, immutable scripts + a history table; tool diff‑applies pending ones in a transaction at startup.
- **Flyway:** SQL files named `V<n>__desc.sql` (versioned) or `R__desc.sql` (repeatable); history in `flyway_schema_history`; simple, SQL‑first, ideal default.
- **Liquibase:** changelogs of changesets identified by `(id, author, file)` in XML/YAML/JSON/SQL; history in `DATABASECHANGELOG` (+ lock table); rich features: rollback, contexts/labels, preconditions, diff, tags.
- **Pick:** Flyway for SQL‑heavy teams on one DB engine; Liquibase for multi‑DB, governed, rollback‑heavy environments. Never both.
- **Always:** `ddl-auto=validate` in prod, never edit applied migrations, keep them small, plan zero‑downtime DDL via **expand → migrate → contract**, run migrations in CI with Testcontainers.
