# 🌱 SPRING CORE — Complete In-Depth Guide

> **Spring Framework Version:** 6.x | **Spring Boot Version:** 3.x  
> **Java Version:** 17+  
> This guide covers theory, real-world analogies, diagrams, code, common mistakes, interview Q&A, and unit testing examples.

---

## Table of Contents

1. [Inversion of Control (IoC)](#1-inversion-of-control-ioc)
2. [Dependency Injection (DI)](#2-dependency-injection-di)
3. [Spring Beans](#3-spring-beans)
4. [Bean Scopes](#4-bean-scopes)
5. [Stereotype Annotations](#5-stereotype-annotations--component-service-repository-controller)
6. [@Autowired](#6-autowired)
7. [Component Scanning](#7-component-scanning)
8. [ApplicationContext vs BeanFactory](#8-applicationcontext-vs-beanfactory)
9. [@Configuration & @Bean](#9-configuration--bean)
10. [Common Mistakes & Troubleshooting](#10-common-mistakes--troubleshooting)
11. [Interview Questions & Answers](#11-interview-questions--answers)

---

## 1. Inversion of Control (IoC)

### What is IoC?

**Inversion of Control** is a design principle where the **control of creating and managing objects is transferred from the programmer to the Spring Framework (container).**

In traditional programming, **YOU** create objects. With IoC, the **Spring Container** creates and manages them for you.

### 💡 Real-World Analogy

Think of a **restaurant**:
- **Without IoC (cooking at home):** You buy ingredients, prepare food, cook it, serve it, and clean up — YOU control everything.
- **With IoC (eating at a restaurant):** You just place an order (declare what you need). The restaurant (Spring Container) prepares the food, serves it, and handles cleanup. You **inverted the control** to the restaurant.

```
┌────────────────────────────────────────────────────┐
│           RESTAURANT ANALOGY                       │
│                                                    │
│  WITHOUT IoC (Cooking at Home):                    │
│  You ──▶ Buy ──▶ Prepare ──▶ Cook ──▶ Serve       │
│  (You control EVERY step)                          │
│                                                    │
│  WITH IoC (Restaurant):                            │
│  You ──▶ "I want pasta" ──▶ 🍝 (Ready!)           │
│  (Restaurant handles everything)                   │
│                                                    │
│  Spring Container = Restaurant                     │
│  Your Class       = Customer                       │
│  Dependencies     = Food/Ingredients               │
└────────────────────────────────────────────────────┘
```

### Without IoC (Traditional Way)

```java
public class OrderService {
    // YOU are creating the dependency manually
    private PaymentService paymentService = new PaymentService();

    public void placeOrder() {
        paymentService.processPayment();
    }
}
```

**Problem:** `OrderService` is tightly coupled to `PaymentService`. If you want to change `PaymentService`, you must modify `OrderService`.

### With IoC (Spring Way)

```java
@Service
public class OrderService {
    // Spring Container INJECTS the dependency for you
    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;  // Spring provides this
    }

    public void placeOrder() {
        paymentService.processPayment();
    }
}
```

### IoC Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                   TRADITIONAL WAY                       │
│                                                         │
│   Developer Code                                        │
│   ┌──────────────┐         ┌──────────────┐             │
│   │ OrderService │──new──▶ │PaymentService│             │
│   └──────────────┘         └──────────────┘             │
│                                                         │
│   Developer creates + manages ALL objects               │
│   (Tight Coupling)                                      │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                   SPRING IoC WAY                        │
│                                                         │
│   ┌───────────────────────────────────┐                 │
│   │        Spring IoC Container       │                 │
│   │                                   │                 │
│   │  ┌──────────────┐                 │                 │
│   │  │PaymentService│ (Bean)          │                 │
│   │  └──────┬───────┘                 │                 │
│   │         │ injects                 │                 │
│   │         ▼                         │                 │
│   │  ┌──────────────┐                 │                 │
│   │  │ OrderService │ (Bean)          │                 │
│   │  └──────────────┘                 │                 │
│   │                                   │                 │
│   │  Container creates + manages ALL  │                 │
│   │  objects (Loose Coupling)         │                 │
│   └───────────────────────────────────┘                 │
└─────────────────────────────────────────────────────────┘
```

### Key Benefits of IoC

| Benefit           | Description                                                |
| ----------------- | ---------------------------------------------------------- |
| Loose Coupling    | Classes don't depend on concrete implementations           |
| Easy Testing      | You can easily mock dependencies in unit tests             |
| Flexibility       | Swap implementations without changing dependent classes    |
| Centralized Mgmt  | All object creation logic is in one place (the container)  |

### How IoC Container Works Internally — Step by Step

```
┌──────────────────────────────────────────────────────────────────┐
│             SPRING IoC CONTAINER — INTERNAL FLOW                 │
│                                                                  │
│  Step 1: READ Configuration                                     │
│  ┌──────────────────────────────────────┐                        │
│  │  Sources:                            │                        │
│  │  • @Configuration classes            │                        │
│  │  • @Component-scanned classes        │                        │
│  │  • application.properties / .yml     │                        │
│  │  • XML files (legacy)                │                        │
│  └──────────────┬───────────────────────┘                        │
│                 ▼                                                │
│  Step 2: CREATE BeanDefinition objects                           │
│  ┌──────────────────────────────────────┐                        │
│  │  BeanDefinition = metadata about     │                        │
│  │  each bean (class, scope, deps,      │                        │
│  │  init-method, destroy-method, etc.)  │                        │
│  └──────────────┬───────────────────────┘                        │
│                 ▼                                                │
│  Step 3: INSTANTIATE beans (call constructors)                   │
│  ┌──────────────────────────────────────┐                        │
│  │  Spring uses reflection to create    │                        │
│  │  objects: Class.newInstance() or      │                        │
│  │  Constructor.newInstance(args)        │                        │
│  └──────────────┬───────────────────────┘                        │
│                 ▼                                                │
│  Step 4: INJECT dependencies (DI)                                │
│  ┌──────────────────────────────────────┐                        │
│  │  Resolve @Autowired, constructor     │                        │
│  │  args, setter injection, etc.        │                        │
│  └──────────────┬───────────────────────┘                        │
│                 ▼                                                │
│  Step 5: POST-PROCESS (BeanPostProcessors)                       │
│  ┌──────────────────────────────────────┐                        │
│  │  AOP proxies, @Transactional,        │                        │
│  │  @Async, validation, etc.            │                        │
│  └──────────────┬───────────────────────┘                        │
│                 ▼                                                │
│  Step 6: BEANS READY — Application runs                          │
└──────────────────────────────────────────────────────────────────┘
```

### IoC Without Spring (Pure Java — Understanding the Principle)

You can implement IoC manually — it's just a design principle, not Spring-specific:

```java
// Interface — abstracts the dependency
public interface MessageService {
    void sendMessage(String message);
}

// Concrete Implementation
public class EmailService implements MessageService {
    @Override
    public void sendMessage(String message) {
        System.out.println("Email sent: " + message);
    }
}

// This class does NOT create its own dependency — IoC!
public class NotificationManager {
    private final MessageService messageService;

    // Dependency is INJECTED from outside (IoC principle)
    public NotificationManager(MessageService messageService) {
        this.messageService = messageService;
    }

    public void notify(String msg) {
        messageService.sendMessage(msg);
    }
}

// Main — the "container" (you, manually)
public class Main {
    public static void main(String[] args) {
        MessageService service = new EmailService();   // YOU create
        NotificationManager mgr = new NotificationManager(service); // YOU inject
        mgr.notify("Hello!");
    }
}
// Spring automates this entire wiring process!
```

---

## 2. Dependency Injection (DI)

### What is DI?

**Dependency Injection** is the **technique/mechanism** used to implement IoC. It means **the Spring Container injects (provides) the required dependencies into a class** rather than the class creating them itself.

> **IoC** = Principle (What) — "Don't call us, we'll call you"  
> **DI** = Implementation (How) — "We'll give you what you need"

### 💡 Real-World Analogy

Think of a **newborn baby**:
- **Without DI:** The baby goes to the market, buys milk, warms it, and feeds itself. (Absurd! The baby creates its own dependencies.)
- **With DI:** The parent (Spring Container) prepares the milk and feeds the baby. The baby just **declares** it needs food, and the parent **injects** it.

```
┌─────────────────────────────────────────────────┐
│  WITHOUT DI:                                    │
│  Baby ──▶ new Milk() ──▶ new Bottle() ──▶ Feed │
│  (Baby creates its own dependencies — BAD!)     │
│                                                 │
│  WITH DI:                                       │
│  Parent ──▶ prepares Milk ──▶ gives to Baby     │
│  Baby just says: "I need milk" (declaration)    │
│  (Parent injects the dependency — GOOD!)        │
└─────────────────────────────────────────────────┘
```

### What is a "Dependency"?

A **dependency** is any object that another object needs to function:

```java
public class BookService {
    // BookRepository is a DEPENDENCY of BookService
    // BookService DEPENDS ON BookRepository to work
    private final BookRepository bookRepository;
}
```

```
┌───────────────────────────────────────────┐
│  BookService  ───depends on───▶  BookRepository
│  (Consumer)                      (Dependency)
│
│  "BookService cannot work without BookRepository"
└───────────────────────────────────────────┘
```

### Types of Dependency Injection

```
┌──────────────────────────────────────────────────────────┐
│              Dependency Injection Types                   │
│                                                          │
│    ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│    │ Constructor  │  │   Setter     │  │    Field     │  │
│    │  Injection   │  │  Injection   │  │  Injection   │  │
│    └──────────────┘  └──────────────┘  └──────────────┘  │
│     (RECOMMENDED)      (Optional       (Least            │
│                        Dependencies)    Recommended)      │
└──────────────────────────────────────────────────────────┘
```

---

### 2.1 Constructor Injection (RECOMMENDED)

Dependencies are provided through the **class constructor**.

```java
@Service
public class BookService {

    private final BookRepository bookRepository;
    private final NotificationService notificationService;

    // Constructor Injection — Spring injects both dependencies
    // @Autowired is optional when there is only ONE constructor
    @Autowired
    public BookService(BookRepository bookRepository,
                       NotificationService notificationService) {
        this.bookRepository = bookRepository;
        this.notificationService = notificationService;
    }

    public void addBook(Book book) {
        bookRepository.save(book);
        notificationService.sendNotification("New book added: " + book.getTitle());
    }
}
```

**Why Constructor Injection is Best:**

| Reason                 | Explanation                                                 |
| ---------------------- | ----------------------------------------------------------- |
| Immutability           | Fields can be `final` — cannot be changed after creation    |
| Required Dependencies  | Object cannot be created without all dependencies           |
| Testability            | Easy to pass mock objects in unit tests                     |
| No Reflection Needed   | Works with plain Java — no framework magic required         |

---

### 2.2 Setter Injection

Dependencies are provided through **setter methods**. Used for **optional** dependencies.

```java
@Service
public class LibraryService {

    private BookRepository bookRepository;
    private EmailService emailService;  // optional dependency

    // Setter Injection for required dependency
    @Autowired
    public void setBookRepository(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // Setter Injection for optional dependency
    @Autowired(required = false)
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void issueBook(String bookId) {
        Book book = bookRepository.findById(bookId);
        // emailService might be null if not available
        if (emailService != null) {
            emailService.sendEmail("Book issued: " + book.getTitle());
        }
    }
}
```

---

### 2.3 Field Injection (Least Recommended)

Dependencies are injected directly into **fields** using `@Autowired`. No constructor or setter needed.

```java
@Service
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;  // injected directly

    @Autowired
    private BookService bookService;  // injected directly

    public void registerMember(Member member) {
        memberRepository.save(member);
    }
}
```

**Why Field Injection is NOT Recommended:**

| Problem            | Explanation                                                    |
| ------------------ | -------------------------------------------------------------- |
| No Immutability    | Fields can't be `final`                                       |
| Hidden Dependencies| Hard to see what a class depends on                            |
| Hard to Test       | Need reflection or Spring context for testing                  |
| Tight to Framework | Only works with Spring — can't create objects normally          |

---

### DI Comparison Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                  CONSTRUCTOR INJECTION                          │
│                                                                │
│   Spring Container                                             │
│        │                                                       │
│        │  new BookService(bookRepo, notifService)              │
│        ▼                                                       │
│   ┌──────────────┐                                             │
│   │ BookService  │ ← dependencies provided at creation time    │
│   │  - bookRepo  │    (IMMUTABLE, fields are final)            │
│   │  - notifSvc  │                                             │
│   └──────────────┘                                             │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                    SETTER INJECTION                             │
│                                                                │
│   Spring Container                                             │
│        │                                                       │
│        │  1. new LibraryService()          (empty object)      │
│        │  2. setBookRepository(bookRepo)   (inject step 1)    │
│        │  3. setEmailService(emailSvc)     (inject step 2)    │
│        ▼                                                       │
│   ┌────────────────┐                                           │
│   │ LibraryService │ ← dependencies set AFTER creation         │
│   │  - bookRepo    │   (MUTABLE, can be changed later)         │
│   │  - emailSvc    │                                           │
│   └────────────────┘                                           │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                    FIELD INJECTION                              │
│                                                                │
│   Spring Container                                             │
│        │                                                       │
│        │  1. new MemberService()           (empty object)      │
│        │  2. Uses REFLECTION to directly                       │
│        │     set private fields                                │
│        ▼                                                       │
│   ┌────────────────┐                                           │
│   │ MemberService  │ ← Spring uses Java Reflection API         │
│   │  - memberRepo  │   to bypass private access                │
│   │  - bookService │   (HIDDEN dependencies)                   │
│   └────────────────┘                                           │
└────────────────────────────────────────────────────────────────┘
```

### How Each DI Type Affects Unit Testing

**Constructor Injection — EASY to test (no Spring needed):**

```java
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private NotificationService notificationService;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        // Just pass mocks through the constructor — SIMPLE!
        bookService = new BookService(bookRepository, notificationService);
    }

    @Test
    void shouldAddBook() {
        Book book = new Book("Spring in Action", "Craig Walls");
        when(bookRepository.save(book)).thenReturn(book);

        bookService.addBook(book);

        verify(bookRepository).save(book);
        verify(notificationService).sendNotification(contains("Spring in Action"));
    }
}
```

**Field Injection — HARD to test (needs reflection or Spring context):**

```java
// With field injection, you CANNOT create the object normally
// You need @SpringBootTest or ReflectionTestUtils — ugly!
@SpringBootTest  // Loads entire Spring context — SLOW
class MemberServiceTest {

    @Autowired
    private MemberService memberService; // Can't use without Spring

    @Test
    void shouldRegisterMember() {
        // test...
    }
}

// OR use reflection (hacky):
class MemberServiceTest {
    @Test
    void shouldRegisterMember() {
        MemberService service = new MemberService();
        MemberRepository mockRepo = mock(MemberRepository.class);
        // Inject into private field using reflection — UGLY!
        ReflectionTestUtils.setField(service, "memberRepository", mockRepo);
    }
}
```

---

## 3. Spring Beans

### What is a Spring Bean?

A **Spring Bean** is simply a **Java object that is created, configured, and managed by the Spring IoC Container**. Any class registered with Spring becomes a "bean."

### 💡 Real-World Analogy

Think of a **company HR department**:
- **Employee (Bean):** A person managed by the company
- **HR Department (IoC Container):** Hires (creates), assigns roles (injects dependencies), manages, and eventually handles exit (destroys)
- **Not every person is an employee** — similarly, not every Java object is a Spring Bean. Only those **registered with Spring** are beans.

### Bean vs Regular Object

```
┌──────────────────────────────────────────────────────────────┐
│                  BEAN vs REGULAR OBJECT                       │
│                                                              │
│  Regular Java Object:                                        │
│  ┌────────────────────────────────────┐                      │
│  │  Book book = new Book();           │                      │
│  │  • Created by YOU                  │                      │
│  │  • Managed by YOU                  │                      │
│  │  • No DI, no lifecycle callbacks   │                      │
│  │  • Garbage collected when unused   │                      │
│  └────────────────────────────────────┘                      │
│                                                              │
│  Spring Bean:                                                │
│  ┌────────────────────────────────────┐                      │
│  │  @Component                        │                      │
│  │  public class BookValidator { }    │                      │
│  │  • Created by SPRING CONTAINER     │                      │
│  │  • Managed by SPRING CONTAINER     │                      │
│  │  • Has DI, lifecycle, AOP, etc.    │                      │
│  │  • Destroyed by container          │                      │
│  └────────────────────────────────────┘                      │
└──────────────────────────────────────────────────────────────┘
```

```java
// This class becomes a Spring Bean because of @Component
@Component
public class BookValidator {
    public boolean isValid(Book book) {
        return book.getTitle() != null && !book.getTitle().isEmpty();
    }
}
```

### Bean Lifecycle

```
┌──────────────────────────────────────────────────────────────────┐
│                    SPRING BEAN LIFECYCLE                          │
│                                                                  │
│  ┌─────────────────┐                                             │
│  │  1. Instantiate  │  Spring creates the object (new)           │
│  └────────┬────────┘                                             │
│           ▼                                                      │
│  ┌─────────────────┐                                             │
│  │  2. Populate     │  Inject dependencies (DI)                  │
│  │   Properties     │                                            │
│  └────────┬────────┘                                             │
│           ▼                                                      │
│  ┌─────────────────┐                                             │
│  │  3. setBeanName  │  BeanNameAware interface                   │
│  │  setBeanFactory  │  BeanFactoryAware interface                │
│  └────────┬────────┘                                             │
│           ▼                                                      │
│  ┌─────────────────────┐                                         │
│  │  4. @PostConstruct   │  Initialization callback               │
│  │  or afterProperties  │  (runs after all properties are set)   │
│  │       Set()          │                                        │
│  └────────┬─────────────┘                                        │
│           ▼                                                      │
│  ┌─────────────────┐                                             │
│  │  5. BEAN READY   │  ← Bean is now fully initialized           │
│  │  (In Use)        │     and available for use                   │
│  └────────┬────────┘                                             │
│           ▼                                                      │
│  ┌─────────────────────┐                                         │
│  │  6. @PreDestroy      │  Cleanup callback                      │
│  │  or destroy()        │  (runs before container shuts down)    │
│  └─────────────────────┘                                         │
└──────────────────────────────────────────────────────────────────┘
```

### Bean Lifecycle Code Example

```java
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConnectionManager {

    private String connectionUrl;

    public DatabaseConnectionManager() {
        System.out.println("1. Constructor called — Bean Instantiated");
    }

    @PostConstruct
    public void init() {
        System.out.println("2. @PostConstruct — Initializing DB connection...");
        this.connectionUrl = "jdbc:mysql://localhost:3306/library";
        // Open connection pool, load configs, etc.
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("3. @PreDestroy — Closing DB connection...");
        // Close connection pool, release resources
    }
}
```

**Output when the application starts and stops:**
```
1. Constructor called — Bean Instantiated
2. @PostConstruct — Initializing DB connection...
   ... application runs ...
3. @PreDestroy — Closing DB connection...
```

### Advanced: Implementing Aware Interfaces

If a bean needs to know about its own name, or access the container, it can implement **Aware** interfaces:

```java
@Component
public class SmartBean implements BeanNameAware, ApplicationContextAware {

    private String beanName;
    private ApplicationContext applicationContext;

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
        System.out.println("My bean name is: " + name);
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        this.applicationContext = ctx;
        System.out.println("I now have access to the ApplicationContext!");
        System.out.println("Total beans in container: " + ctx.getBeanDefinitionCount());
    }
}
```

### Advanced: BeanPostProcessor — Hook Into EVERY Bean's Creation

A `BeanPostProcessor` lets you run custom logic **before** and **after** every bean is initialized:

```java
@Component
public class LoggingBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        // Runs BEFORE @PostConstruct
        System.out.println(">> Before init: " + beanName);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // Runs AFTER @PostConstruct
        // This is where AOP proxies are created!
        System.out.println("<< After init: " + beanName);
        return bean;
    }
}
```

```
┌──────────────────────────────────────────────────────────────────┐
│              COMPLETE BEAN LIFECYCLE (DETAILED)                   │
│                                                                  │
│  ┌─────────────────────┐                                         │
│  │  1. Instantiate      │  new MyBean()                          │
│  └──────────┬──────────┘                                         │
│             ▼                                                    │
│  ┌─────────────────────┐                                         │
│  │  2. Populate Props   │  @Autowired injection                  │
│  └──────────┬──────────┘                                         │
│             ▼                                                    │
│  ┌─────────────────────┐                                         │
│  │  3. Aware Interfaces │  setBeanName(), setApplicationContext()│
│  └──────────┬──────────┘                                         │
│             ▼                                                    │
│  ┌────────────────────────────────────┐                           │
│  │  4. BeanPostProcessor              │                           │
│  │     .postProcessBeforeInitialization│                           │
│  └──────────┬─────────────────────────┘                           │
│             ▼                                                    │
│  ┌─────────────────────┐                                         │
│  │  5. @PostConstruct   │  Your custom init logic                │
│  └──────────┬──────────┘                                         │
│             ▼                                                    │
│  ┌────────────────────────────────────┐                           │
│  │  6. BeanPostProcessor              │                           │
│  │     .postProcessAfterInitialization │  ← AOP proxies created  │
│  └──────────┬─────────────────────────┘                           │
│             ▼                                                    │
│  ┌─────────────────────┐                                         │
│  │  7. BEAN READY ✅    │  Bean is fully usable                   │
│  └──────────┬──────────┘                                         │
│             ▼                                                    │
│  ┌─────────────────────┐                                         │
│  │  8. @PreDestroy      │  Cleanup before shutdown               │
│  └─────────────────────┘                                         │
└──────────────────────────────────────────────────────────────────┘
```

### Three Ways to Define Init/Destroy Callbacks

| Method                           | Annotation-Based       | Interface-Based            | @Bean Attribute               |
| -------------------------------- | ---------------------- | -------------------------- | ----------------------------- |
| Init                             | `@PostConstruct`       | `InitializingBean.afterPropertiesSet()` | `@Bean(initMethod="init")`   |
| Destroy                          | `@PreDestroy`          | `DisposableBean.destroy()` | `@Bean(destroyMethod="cleanup")` |
| Recommended?                     | ✅ YES                 | ❌ Couples to Spring       | ✅ For third-party classes    |

---

## 4. Bean Scopes

### What are Bean Scopes?

**Bean Scope** defines **how many instances** of a bean Spring will create and **how long** that bean lives.

### All Bean Scopes

```
┌──────────────────────────────────────────────────────────────────┐
│                      SPRING BEAN SCOPES                          │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  GENERAL SCOPES (Available in ALL Spring applications)      │ │
│  │                                                             │ │
│  │  ┌───────────┐    ┌────────────┐                            │ │
│  │  │ Singleton │    │ Prototype  │                            │ │
│  │  │ (DEFAULT) │    │            │                            │ │
│  │  └───────────┘    └────────────┘                            │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  WEB SCOPES (Only in Web/Spring MVC applications)           │ │
│  │                                                             │ │
│  │  ┌───────────┐    ┌───────────┐    ┌─────────────┐         │ │
│  │  │  Request  │    │  Session  │    │ Application │         │ │
│  │  └───────────┘    └───────────┘    └─────────────┘         │ │
│  └─────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

---

### 4.1 Singleton Scope (DEFAULT)

**Only ONE instance** is created for the entire Spring Container. Every time you request this bean, you get the **same object**.

```java
@Component
@Scope("singleton")  // This is the DEFAULT — you don't need to write this
public class LibraryConfig {

    private String libraryName = "City Library";

    public String getLibraryName() {
        return libraryName;
    }
}
```

```
┌──────────────────────────────────────────────┐
│           SINGLETON SCOPE                     │
│                                              │
│   Spring Container                           │
│   ┌────────────────────────┐                 │
│   │   LibraryConfig Bean   │ ← Only ONE      │
│   │   (Single Instance)    │   instance       │
│   └───────────┬────────────┘   exists          │
│               │                              │
│       ┌───────┼───────┐                      │
│       ▼       ▼       ▼                      │
│    ClassA  ClassB  ClassC                    │
│                                              │
│    ALL classes get the SAME object            │
│    (same memory address)                     │
└──────────────────────────────────────────────┘
```

**Verification:**
```java
@SpringBootApplication
public class ScopeDemo implements CommandLineRunner {

    @Autowired private LibraryConfig config1;
    @Autowired private LibraryConfig config2;

    @Override
    public void run(String... args) {
        System.out.println(config1 == config2);  // true — SAME object
        System.out.println(config1.hashCode());   // Same hashCode
        System.out.println(config2.hashCode());   // Same hashCode
    }
}
```

---

### 4.2 Prototype Scope

A **NEW instance** is created **every time** the bean is requested.

```java
@Component
@Scope("prototype")
public class BookSearchRequest {

    private String query;
    private LocalDateTime timestamp;

    public BookSearchRequest() {
        this.timestamp = LocalDateTime.now();
    }

    // getters and setters
}
```

```
┌──────────────────────────────────────────────┐
│           PROTOTYPE SCOPE                     │
│                                              │
│   Spring Container                           │
│   ┌────────────────────────┐                 │
│   │ BookSearchRequest Bean │                 │
│   │    (TEMPLATE only)     │                 │
│   └───────────┬────────────┘                 │
│               │                              │
│       ┌───────┼───────┐                      │
│       ▼       ▼       ▼                      │
│  Instance1 Instance2 Instance3               │
│  (new obj) (new obj) (new obj)               │
│                                              │
│  Each request gets a DIFFERENT object         │
│  (different memory address)                  │
└──────────────────────────────────────────────┘
```

**Verification:**
```java
@SpringBootApplication
public class ScopeDemo implements CommandLineRunner {

    @Autowired private BookSearchRequest request1;
    @Autowired private BookSearchRequest request2;

    @Override
    public void run(String... args) {
        System.out.println(request1 == request2);  // false — DIFFERENT objects
        System.out.println(request1.hashCode());    // Different hashCode
        System.out.println(request2.hashCode());    // Different hashCode
    }
}
```

---

### 4.3 Request Scope (Web Only)

A **new instance** is created for **each HTTP request**. The bean is destroyed after the request completes.

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
// OR use the shortcut:
// @RequestScope
public class RequestLogger {

    private final String requestId = UUID.randomUUID().toString();
    private final List<String> logs = new ArrayList<>();

    public void log(String message) {
        logs.add("[" + requestId + "] " + message);
    }

    public List<String> getLogs() {
        return logs;
    }
}
```

```
┌──────────────────────────────────────────────────────────┐
│                  REQUEST SCOPE                            │
│                                                          │
│  HTTP Request 1 ──▶ ┌─────────────────┐                  │
│                     │ RequestLogger-1  │ ← Created         │
│                     └─────────────────┘ ← Destroyed       │
│                                           after response  │
│                                                          │
│  HTTP Request 2 ──▶ ┌─────────────────┐                  │
│                     │ RequestLogger-2  │ ← NEW instance    │
│                     └─────────────────┘ ← Destroyed       │
│                                           after response  │
│                                                          │
│  HTTP Request 3 ──▶ ┌─────────────────┐                  │
│                     │ RequestLogger-3  │ ← NEW instance    │
│                     └─────────────────┘ ← Destroyed       │
│                                           after response  │
└──────────────────────────────────────────────────────────┘
```

---

### 4.4 Session Scope (Web Only)

A **new instance** is created for **each HTTP session** (per user). The bean lives as long as the user's session is active.

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
// OR use the shortcut:
// @SessionScope
public class ShoppingCart {

    private final List<Book> items = new ArrayList<>();

    public void addBook(Book book) {
        items.add(book);
    }

    public List<Book> getItems() {
        return items;
    }

    public void clear() {
        items.clear();
    }
}
```

```
┌──────────────────────────────────────────────────────────┐
│                  SESSION SCOPE                            │
│                                                          │
│  User A (Session-A)                                      │
│  ┌───────────────────────────────────────┐                │
│  │  Request 1 ──▶ ┌──────────────┐      │                │
│  │  Request 2 ──▶ │ ShoppingCart │      │ SAME instance  │
│  │  Request 3 ──▶ │  (User A)    │      │ across all     │
│  │                └──────────────┘      │ requests       │
│  └───────────────────────────────────────┘                │
│                                                          │
│  User B (Session-B)                                      │
│  ┌───────────────────────────────────────┐                │
│  │  Request 1 ──▶ ┌──────────────┐      │                │
│  │  Request 2 ──▶ │ ShoppingCart │      │ DIFFERENT       │
│  │                │  (User B)    │      │ instance        │
│  │                └──────────────┘      │                │
│  └───────────────────────────────────────┘                │
└──────────────────────────────────────────────────────────┘
```

---

### Scope Summary Table

| Scope       | Instances Created      | Lifetime                    | Use Case                        |
| ----------- | ---------------------- | --------------------------- | ------------------------------- |
| `singleton` | ONE per container      | Entire application lifetime | Services, Repositories, Configs |
| `prototype` | NEW for each request   | Until garbage collected     | Stateful objects, builders      |
| `request`   | NEW per HTTP request   | Single HTTP request         | Request logging, validation     |
| `session`   | NEW per HTTP session   | User session duration       | Shopping cart, user preferences |

### ⚠️ Common Pitfall: Injecting Prototype Bean into Singleton Bean

This is one of the **most common mistakes** in Spring:

```java
@Component  // Singleton by default
public class BookService {

    @Autowired
    private BookSearchRequest searchRequest; // Prototype-scoped bean

    public void search(String query) {
        // PROBLEM: searchRequest is ALWAYS the SAME object!
        // Because BookService is Singleton, it was injected ONCE at startup.
        // The prototype scope is effectively BROKEN here.
        searchRequest.setQuery(query);
    }
}
```

```
┌──────────────────────────────────────────────────────────────────┐
│   ⚠️ THE PROBLEM: Prototype inside Singleton                    │
│                                                                  │
│   Startup:                                                       │
│   Container creates BookService (singleton)                      │
│      └──▶ injects BookSearchRequest (prototype instance #1)      │
│                                                                  │
│   Request 1: bookService.search("Java")                          │
│      └──▶ Uses instance #1  ← same object                       │
│                                                                  │
│   Request 2: bookService.search("Spring")                        │
│      └──▶ Uses instance #1  ← STILL the same object! 🐛          │
│                                                                  │
│   Expected: Each call gets a NEW prototype instance              │
│   Actual:   All calls share the SAME instance                    │
└──────────────────────────────────────────────────────────────────┘
```

**Solution 1: Use `ObjectFactory` or `Provider`:**

```java
@Component
public class BookService {

    @Autowired
    private ObjectProvider<BookSearchRequest> searchRequestProvider;

    public void search(String query) {
        // getObject() creates a NEW prototype instance every time!
        BookSearchRequest searchRequest = searchRequestProvider.getObject();
        searchRequest.setQuery(query);
    }
}
```

**Solution 2: Use `@Lookup` method injection:**

```java
@Component
public abstract class BookService {

    // Spring overrides this method to return a new prototype each time
    @Lookup
    protected abstract BookSearchRequest createSearchRequest();

    public void search(String query) {
        BookSearchRequest searchRequest = createSearchRequest(); // NEW each time
        searchRequest.setQuery(query);
    }
}
```

---

## 5. Stereotype Annotations — @Component, @Service, @Repository, @Controller

### What are Stereotype Annotations?

These are **special annotations** that tell Spring: "This class is a Spring Bean — please manage it." They all register the class as a bean, but each has a **specific semantic meaning**.

### 💡 Real-World Analogy

Think of employees at a **hospital**:
- **@Component** = Generic staff member (could be anyone)
- **@Service** = Doctor (performs the main medical logic/operations)
- **@Repository** = Pharmacist (handles medicine storage — data access)
- **@Controller** = Receptionist (interacts with patients — handles requests)

All are employees (Spring beans), but each has a **specific role**.

### Hierarchy Diagram

```
                      @Component
                     (Base/Generic)
                          │
            ┌─────────────┼─────────────┐
            │             │             │
        @Service     @Repository   @Controller
       (Business     (Data Access   (Web/HTTP
        Logic)        Layer)         Layer)
                                        │
                                   @RestController
                                  (REST API Layer
                                   = @Controller
                                   + @ResponseBody)
```

### Detailed Explanation of Each

---

### 5.1 @Component (Generic)

The **base annotation**. Use it when your class doesn't fit into Service, Repository, or Controller categories.

```java
@Component
public class EmailValidator {

    public boolean isValid(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
}
```

**Use for:** Utility classes, helpers, generic beans.

---

### 5.2 @Service (Business Logic Layer)

Marks a class that contains **business logic**. Functionally identical to `@Component`, but indicates **purpose**.

```java
@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Book findBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Book not found: " + id));
    }

    public List<Book> searchBooks(String title) {
        return bookRepository.findByTitleContaining(title);
    }

    public Book addBook(Book book) {
        // Business validation
        if (book.getIsbn() == null || book.getIsbn().length() != 13) {
            throw new InvalidBookException("ISBN must be 13 characters");
        }
        return bookRepository.save(book);
    }
}
```

**Use for:** Service classes with business rules, calculations, orchestration.

---

### 5.3 @Repository (Data Access Layer)

Marks a class as a **data access component**. Has a special feature: **automatic exception translation** — converts database-specific exceptions (like `SQLException`) into Spring's `DataAccessException`.

```java
@Repository
public class BookRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Book findById(Long id) {
        return entityManager.find(Book.class, id);
    }

    public List<Book> findByTitleContaining(String title) {
        return entityManager
                .createQuery("SELECT b FROM Book b WHERE b.title LIKE :title", Book.class)
                .setParameter("title", "%" + title + "%")
                .getResultList();
    }

    public Book save(Book book) {
        entityManager.persist(book);
        return book;
    }
}
```

With **Spring Data JPA**, you often just extend an interface:

```java
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByTitleContaining(String title);
    List<Book> findByAuthor(String author);
    Optional<Book> findByIsbn(String isbn);
}
```

**Use for:** Database operations, data access objects (DAO).

**@Repository Exception Translation (Special Feature):**

```
┌──────────────────────────────────────────────────────────────────┐
│            @Repository EXCEPTION TRANSLATION                     │
│                                                                  │
│  Database throws:                                                │
│  ┌────────────────────────────────────┐                           │
│  │  SQLException                      │ ← Database-specific      │
│  │  HibernateException                │   (varies by DB vendor)  │
│  │  JPA PersistenceException          │                           │
│  └──────────────┬─────────────────────┘                           │
│                 │                                                │
│                 │  @Repository enables                           │
│                 │  PersistenceExceptionTranslationPostProcessor   │
│                 ▼                                                │
│  Spring translates to:                                           │
│  ┌────────────────────────────────────┐                           │
│  │  DataAccessException (Spring)      │ ← Consistent, vendor-   │
│  │  ├── DuplicateKeyException         │   independent exceptions │
│  │  ├── DataIntegrityViolation...     │                           │
│  │  ├── EmptyResultDataAccessException│                           │
│  │  └── CannotAcquireLockException    │                           │
│  └────────────────────────────────────┘                           │
│                                                                  │
│  BENEFIT: Switch from MySQL to PostgreSQL without                │
│  changing exception handling code!                               │
└──────────────────────────────────────────────────────────────────┘
```

---

### 5.4 @Controller (Web/Presentation Layer)

Marks a class as a **web controller** that handles HTTP requests and returns **views** (HTML pages).

```java
@Controller
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/books")
    public String listBooks(Model model) {
        List<Book> books = bookService.getAllBooks();
        model.addAttribute("books", books);
        return "book-list";  // Returns VIEW name (HTML template)
    }
}
```

**@RestController** = `@Controller` + `@ResponseBody` (returns JSON/XML data instead of views):

```java
@RestController
@RequestMapping("/api/books")
public class BookRestController {

    private final BookService bookService;

    public BookRestController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();  // Returns JSON directly
    }

    @GetMapping("/{id}")
    public Book getBook(@PathVariable Long id) {
        return bookService.findBook(id);  // Returns JSON directly
    }
}
```

---

### Layered Architecture Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                   CLIENT (Browser / App)                      │
└──────────────────────────┬───────────────────────────────────┘
                           │ HTTP Request
                           ▼
┌──────────────────────────────────────────────────────────────┐
│  CONTROLLER LAYER  (@Controller / @RestController)           │
│                                                              │
│  • Handles HTTP requests (GET, POST, PUT, DELETE)            │
│  • Validates input                                           │
│  • Returns responses (JSON/HTML)                             │
│                                                              │
│  BookController, MemberController                            │
└──────────────────────────┬───────────────────────────────────┘
                           │ Method Call
                           ▼
┌──────────────────────────────────────────────────────────────┐
│  SERVICE LAYER  (@Service)                                   │
│                                                              │
│  • Business logic and rules                                  │
│  • Transaction management                                    │
│  • Orchestrates multiple repositories                        │
│                                                              │
│  BookService, MemberService, LendingService                  │
└──────────────────────────┬───────────────────────────────────┘
                           │ Method Call
                           ▼
┌──────────────────────────────────────────────────────────────┐
│  REPOSITORY LAYER  (@Repository)                             │
│                                                              │
│  • Database CRUD operations                                  │
│  • SQL / JPA queries                                         │
│  • Exception translation                                     │
│                                                              │
│  BookRepository, MemberRepository                            │
└──────────────────────────┬───────────────────────────────────┘
                           │ JDBC / JPA
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                      DATABASE                                │
│                   (MySQL, PostgreSQL)                         │
└──────────────────────────────────────────────────────────────┘
```

---

## 6. @Autowired

### What is @Autowired?

`@Autowired` tells Spring to **automatically inject the required dependency**. Spring looks in its container, finds a matching bean, and injects it.

### 💡 Real-World Analogy

`@Autowired` is like **autocomplete on your phone**. You type a few letters (declare the type), and the phone (Spring) **automatically fills in** the rest (finds and injects the matching bean).

### Important Rule: @Autowired is Optional on Constructors (Since Spring 4.3)

If a class has **only ONE constructor**, Spring automatically uses it for injection — no `@Autowired` needed:

```java
@Service
public class BookService {

    private final BookRepository bookRepository;

    // @Autowired is NOT needed here! Spring auto-detects the single constructor
    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }
}
```

But if there are **multiple constructors**, you MUST mark one with `@Autowired`:

```java
@Service
public class BookService {

    private final BookRepository bookRepository;
    private final CacheService cacheService;

    // Multiple constructors — MUST specify which one Spring should use
    @Autowired
    public BookService(BookRepository bookRepository, CacheService cacheService) {
        this.bookRepository = bookRepository;
        this.cacheService = cacheService;
    }

    public BookService(BookRepository bookRepository) {
        this(bookRepository, null);
    }
}
```

### How @Autowired Works Internally

```
┌──────────────────────────────────────────────────────────────────┐
│                HOW @Autowired RESOLVES BEANS                      │
│                                                                  │
│  Step 1: Spring sees @Autowired on a field/constructor/setter    │
│                          │                                       │
│                          ▼                                       │
│  Step 2: Spring looks at the TYPE of the dependency              │
│          (e.g., BookRepository)                                  │
│                          │                                       │
│                          ▼                                       │
│  Step 3: Spring searches its container for a bean                │
│          that matches that type                                  │
│                          │                                       │
│                ┌─────────┴─────────┐                             │
│                ▼                   ▼                              │
│          ONE match found    MULTIPLE matches found                │
│               │                    │                              │
│               ▼                    ▼                              │
│          Inject it!         Use @Qualifier or                    │
│                             @Primary to decide                   │
│                                    │                              │
│                             NO qualifier?                        │
│                                    │                              │
│                                    ▼                              │
│                          NoUniqueBeanDefinition                   │
│                              Exception!                          │
└──────────────────────────────────────────────────────────────────┘
```

### @Autowired with Multiple Implementations — @Qualifier & @Primary

When you have **multiple beans of the same type**, Spring doesn't know which one to inject. Use `@Qualifier` or `@Primary` to resolve.

```java
// Interface
public interface NotificationService {
    void sendNotification(String message);
}

// Implementation 1
@Service("emailNotification")
public class EmailNotificationService implements NotificationService {
    @Override
    public void sendNotification(String message) {
        System.out.println("EMAIL: " + message);
    }
}

// Implementation 2
@Service("smsNotification")
@Primary  // This will be used by DEFAULT when no @Qualifier is specified
public class SmsNotificationService implements NotificationService {
    @Override
    public void sendNotification(String message) {
        System.out.println("SMS: " + message);
    }
}
```

**Using @Qualifier:**

```java
@Service
public class AlertService {

    private final NotificationService notificationService;

    // Tell Spring EXACTLY which implementation to use
    @Autowired
    public AlertService(@Qualifier("emailNotification") NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void sendAlert(String message) {
        notificationService.sendNotification("ALERT: " + message);
    }
}
```

**Using @Primary (default choice):**

```java
@Service
public class ReminderService {

    private final NotificationService notificationService;

    @Autowired
    public ReminderService(NotificationService notificationService) {
        // SmsNotificationService will be injected (it has @Primary)
        this.notificationService = notificationService;
    }
}
```

### @Autowired(required = false)

Makes a dependency **optional**. If no matching bean is found, Spring injects `null` instead of throwing an error.

```java
@Service
public class ReportService {

    @Autowired(required = false)
    private PdfGenerator pdfGenerator;  // null if PdfGenerator bean doesn't exist

    public void generateReport() {
        if (pdfGenerator != null) {
            pdfGenerator.generate();
        } else {
            System.out.println("PDF generation not available");
        }
    }
}
```

### @Autowired with @Lazy — Delay Bean Initialization

If a bean is expensive to create and not always needed, use `@Lazy` to defer creation until first use:

```java
@Service
public class ReportGenerator {

    private final HeavyPdfEngine pdfEngine;

    @Autowired
    public ReportGenerator(@Lazy HeavyPdfEngine pdfEngine) {
        // HeavyPdfEngine is NOT created here!
        // A lightweight PROXY is injected instead.
        // The real object is created only when a method is called on it.
        this.pdfEngine = pdfEngine;
    }

    public void generateReport() {
        // HeavyPdfEngine is created NOW (first actual use)
        pdfEngine.render("Monthly Report");
    }
}
```

### ⚠️ Circular Dependency Problem

A **circular dependency** happens when Bean A depends on Bean B, and Bean B depends on Bean A:

```
┌──────────────────────────────────────────────────────────────────┐
│   ⚠️ CIRCULAR DEPENDENCY                                        │
│                                                                  │
│   ┌──────────┐  depends on   ┌──────────┐                       │
│   │ ServiceA │──────────────▶│ ServiceB │                       │
│   └──────────┘               └──────────┘                       │
│        ▲                          │                              │
│        │      depends on          │                              │
│        └──────────────────────────┘                              │
│                                                                  │
│   Spring says: "I can't create A without B,                      │
│                 but I can't create B without A!" 💥               │
│                                                                  │
│   Error: BeanCurrentlyInCreationException                        │
└──────────────────────────────────────────────────────────────────┘
```

**Solutions:**

```java
// Solution 1: Redesign — break the cycle (BEST approach)
// Extract shared logic into a third class

// Solution 2: Use @Lazy on one of the dependencies
@Service
public class ServiceA {
    private final ServiceB serviceB;

    public ServiceA(@Lazy ServiceB serviceB) {
        this.serviceB = serviceB; // Proxy injected, breaks the cycle
    }
}

// Solution 3: Use setter injection instead of constructor injection
@Service
public class ServiceA {
    private ServiceB serviceB;

    @Autowired
    public void setServiceB(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}
```

> **Note:** Since Spring Boot 2.6+, circular dependencies are **prohibited by default**. You must explicitly allow them with `spring.main.allow-circular-references=true` (not recommended).

---

## 7. Component Scanning

### What is Component Scanning?

**Component Scanning** is the process where Spring **automatically discovers** classes annotated with `@Component`, `@Service`, `@Repository`, `@Controller`, etc., and registers them as beans.

### 💡 Real-World Analogy

Component Scanning is like a **talent scout** visiting a school:
- The scout (Spring) walks through every classroom (package)
- If a student wears a **special badge** (@Component, @Service, etc.), the scout picks them up
- Students without badges are ignored
- The scout only checks classrooms in the assigned building (base package)

### How It Works

```
┌──────────────────────────────────────────────────────────────────┐
│                   COMPONENT SCANNING PROCESS                     │
│                                                                  │
│  1. Application starts                                           │
│     └──▶ @SpringBootApplication                                  │
│          (includes @ComponentScan)                               │
│                    │                                             │
│  2. Spring scans the BASE PACKAGE                                │
│     and all SUB-PACKAGES                                         │
│                    │                                             │
│                    ▼                                              │
│  com.library.library_management    ← Base package                │
│  ├── controller/                                                 │
│  │   └── BookController.java       @Controller  ✅ Found!        │
│  ├── service/                                                    │
│  │   └── BookService.java          @Service     ✅ Found!        │
│  ├── repository/                                                 │
│  │   └── BookRepository.java       @Repository  ✅ Found!        │
│  ├── model/                                                      │
│  │   └── Book.java                 (No annotation) ❌ Skipped    │
│  └── LibraryManagementApplication.java                           │
│                                                                  │
│  3. Register all discovered beans in the IoC Container           │
│                                                                  │
│  4. Resolve dependencies and inject them                         │
└──────────────────────────────────────────────────────────────────┘
```

### @SpringBootApplication Includes Component Scanning

```java
// @SpringBootApplication is a SHORTCUT for 3 annotations:
@SpringBootConfiguration    // Same as @Configuration
@EnableAutoConfiguration    // Auto-configures beans based on dependencies
@ComponentScan              // Scans current package + sub-packages
public class LibraryManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(LibraryManagementApplication.class, args);
    }
}
```

### Custom Component Scanning

You can customize which packages are scanned:

```java
// Scan specific packages
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.library.library_management",
    "com.library.common",
    "com.library.security"
})
public class LibraryManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(LibraryManagementApplication.class, args);
    }
}
```

```java
// Exclude specific components from scanning
@ComponentScan(
    basePackages = "com.library",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = TestService.class
    )
)
```

### Component Scan Filter Types

| FilterType          | Description                                      | Example                                            |
| ------------------- | ------------------------------------------------ | -------------------------------------------------- |
| `ANNOTATION`        | Filter by annotation type                        | Exclude all `@Deprecated` classes                  |
| `ASSIGNABLE_TYPE`   | Filter by specific class or interface            | Exclude `TestService.class`                        |
| `REGEX`             | Filter by regex pattern on class name            | Exclude `.*Test.*`                                 |
| `CUSTOM`            | Custom filter implementing `TypeFilter`          | Complex custom logic                               |

```java
// Include classes matching a custom annotation
@ComponentScan(
    basePackages = "com.library",
    includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        classes = MyCustomAnnotation.class
    )
)

// Exclude by regex pattern
@ComponentScan(
    basePackages = "com.library",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.library\\..*Test.*"
    )
)
```

### Common Mistake: Bean Not Found

```
┌──────────────────────────────────────────────────────────────────┐
│   COMMON MISTAKE — Bean outside scanned package                  │
│                                                                  │
│   com.library.library_management     ← @SpringBootApplication    │
│   ├── service/                                                   │
│   │   └── BookService.java           ✅ Scanned (sub-package)    │
│   │                                                              │
│   com.external.utils                 ← DIFFERENT root package    │
│   └── StringHelper.java              ❌ NOT Scanned!             │
│                                                                  │
│   FIX: Add @ComponentScan(basePackages = {"com.library",         │
│                                           "com.external"})       │
└──────────────────────────────────────────────────────────────────┘
```

---

## 8. ApplicationContext vs BeanFactory

### What are They?

Both are **Spring IoC Containers** — they manage beans. But `ApplicationContext` is more powerful.

### 💡 Real-World Analogy

- **BeanFactory** = A basic **vending machine** — you ask for an item, it gives it to you. Nothing else.
- **ApplicationContext** = A **smart restaurant** — serves food, plays music, adjusts lighting, handles events, speaks multiple languages, and manages everything automatically.

### Types of ApplicationContext

| Type                                      | Used For                                                  |
| ----------------------------------------- | --------------------------------------------------------- |
| `AnnotationConfigApplicationContext`      | Java-based @Configuration classes (modern, most common)   |
| `ClassPathXmlApplicationContext`          | XML config files from classpath (legacy)                  |
| `FileSystemXmlApplicationContext`         | XML config files from file system (legacy)                |
| `GenericWebApplicationContext`            | Web applications                                          |
| `SpringApplication.run()` returns         | `ConfigurableApplicationContext` (Spring Boot)            |

### Comparison Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│   BeanFactory                                                    │
│   ┌────────────────────────────────────┐                         │
│   │  • Basic bean creation             │                         │
│   │  • Dependency Injection            │                         │
│   │  • LAZY loading (creates beans     │                         │
│   │    only when requested)            │                         │
│   └────────────────────────────────────┘                         │
│                    ▲                                             │
│                    │ extends                                     │
│                    │                                             │
│   ApplicationContext                                             │
│   ┌────────────────────────────────────┐                         │
│   │  Everything in BeanFactory PLUS:   │                         │
│   │                                    │                         │
│   │  • EAGER loading (creates all      │                         │
│   │    singleton beans at startup)     │                         │
│   │  • Event publishing                │                         │
│   │  • Internationalization (i18n)     │                         │
│   │  • Environment & profiles          │                         │
│   │  • AOP integration                 │                         │
│   │  • Annotation-based config         │                         │
│   │  • Automatic BeanPostProcessor     │                         │
│   │    registration                    │                         │
│   └────────────────────────────────────┘                         │
│                                                                  │
│   ApplicationContext IS the standard. Use it always.             │
└──────────────────────────────────────────────────────────────────┘
```

### Feature Comparison Table

| Feature                           | BeanFactory | ApplicationContext |
| --------------------------------- | ----------- | ------------------ |
| Bean creation & DI                | ✅          | ✅                 |
| Lazy initialization               | ✅ Default  | Can be enabled     |
| Eager initialization              | ❌          | ✅ Default         |
| Automatic BeanPostProcessor       | ❌          | ✅                 |
| Event handling                    | ❌          | ✅                 |
| Internationalization (i18n)       | ❌          | ✅                 |
| Environment abstraction           | ❌          | ✅                 |
| AOP support                      | ❌          | ✅                 |
| Resource loading                  | ❌          | ✅                 |
| Annotation-based config           | ❌          | ✅                 |

### Code: BeanFactory vs ApplicationContext

```java
// ============ BeanFactory (OLD, rarely used) ============
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

public class BeanFactoryExample {
    public static void main(String[] args) {
        // BeanFactory — LAZY: bean is created only when getBean() is called
        BeanFactory factory = new XmlBeanFactory(
            new ClassPathResource("beans.xml")
        );

        // Bean is created HERE (not at startup)
        BookService bookService = factory.getBean(BookService.class);
        bookService.listBooks();
    }
}
```

```java
// ============ ApplicationContext (MODERN, always use this) ============
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ApplicationContextExample {
    public static void main(String[] args) {
        // ApplicationContext — EAGER: ALL singleton beans created at startup
        ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        // Bean already exists — just retrieving it
        BookService bookService = context.getBean(BookService.class);
        bookService.listBooks();
    }
}
```

### Spring Boot Uses ApplicationContext

In Spring Boot, you rarely interact with the container directly. `SpringApplication.run()` creates the `ApplicationContext` for you:

```java
@SpringBootApplication
public class LibraryManagementApplication {
    public static void main(String[] args) {
        // This returns an ApplicationContext
        ApplicationContext context = SpringApplication.run(
            LibraryManagementApplication.class, args
        );

        // You can retrieve beans from it
        BookService bookService = context.getBean(BookService.class);
        System.out.println("Total beans: " + context.getBeanDefinitionCount());

        // Print all registered bean names
        for (String name : context.getBeanDefinitionNames()) {
            System.out.println("  Bean: " + name);
        }
    }
}
```

### ApplicationContext Event Publishing (Unique Feature)

One powerful feature only `ApplicationContext` provides is **event publishing**:

```java
// Step 1: Define a custom event
public class BookAddedEvent extends ApplicationEvent {
    private final String bookTitle;

    public BookAddedEvent(Object source, String bookTitle) {
        super(source);
        this.bookTitle = bookTitle;
    }

    public String getBookTitle() { return bookTitle; }
}

// Step 2: Publish the event
@Service
public class BookService {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public void addBook(Book book) {
        // save book...
        // Publish event — any listener will be notified
        eventPublisher.publishEvent(new BookAddedEvent(this, book.getTitle()));
    }
}

// Step 3: Listen for the event
@Component
public class NotificationListener {

    @EventListener
    public void handleBookAdded(BookAddedEvent event) {
        System.out.println("New book added: " + event.getBookTitle());
        // Send email, update cache, log, etc.
    }
}
```

```
┌──────────────────────────────────────────────────────────────────┐
│               EVENT PUBLISHING FLOW                              │
│                                                                  │
│  BookService                    ApplicationContext                │
│  ┌──────────────┐               ┌─────────────────┐             │
│  │ addBook()    │──publishEvent─▶│  Event Bus      │             │
│  └──────────────┘               └────────┬────────┘             │
│                                          │                       │
│                                 ┌────────┴────────┐             │
│                                 ▼                 ▼             │
│                          ┌────────────┐    ┌────────────┐       │
│                          │ Listener 1 │    │ Listener 2 │       │
│                          │ (Email)    │    │ (Cache)    │       │
│                          └────────────┘    └────────────┘       │
│                                                                  │
│  Decoupled! BookService doesn't know about listeners.           │
└──────────────────────────────────────────────────────────────────┘
```

---

## 9. @Configuration & @Bean

### What is @Configuration?

`@Configuration` marks a class as a **source of bean definitions**. It replaces the old XML configuration files with **Java-based configuration**.

### What is @Bean?

`@Bean` is a **method-level annotation** used inside `@Configuration` classes. The method's **return value** becomes a Spring Bean managed by the container.

### ⚡ Key Concept: @Configuration uses CGLIB Proxy

`@Configuration` classes are **special** — Spring creates a CGLIB **subclass proxy** of them. This ensures that calling `@Bean` methods within the same class returns the **same singleton instance**, not a new object:

```java
@Configuration
public class AppConfig {

    @Bean
    public DataSource dataSource() {
        return new HikariDataSource();  // Called only ONCE
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        // This does NOT create a new DataSource!
        // Spring intercepts this call and returns the EXISTING singleton bean.
        return new JdbcTemplate(dataSource());
    }

    @Bean
    public TransactionManager txManager() {
        // Same DataSource singleton is reused
        return new DataSourceTransactionManager(dataSource());
    }
}
```

```
┌──────────────────────────────────────────────────────────────────┐
│   @Configuration CGLIB PROXY — How it works                      │
│                                                                  │
│   Your code:  AppConfig                                          │
│   Spring creates: AppConfig$$EnhancerByCGLIB (subclass proxy)    │
│                                                                  │
│   When jdbcTemplate() calls dataSource():                        │
│   ┌──────────────────────────────────────────────────────┐       │
│   │  1. CGLIB proxy intercepts the call                  │       │
│   │  2. Checks: "Does a DataSource bean already exist?"  │       │
│   │  3. YES → return existing singleton                  │       │
│   │     NO  → call real dataSource() method, cache it    │       │
│   └──────────────────────────────────────────────────────┘       │
│                                                                  │
│   If you used @Component instead of @Configuration:              │
│   dataSource() would be called 3 times = 3 DIFFERENT objects! 🐛 │
└──────────────────────────────────────────────────────────────────┘
```

### @Configuration(proxyBeanMethods = false) — Lite Mode

For performance, you can disable the CGLIB proxy if you don't call `@Bean` methods from within the class:

```java
@Configuration(proxyBeanMethods = false)  // Lite mode — no proxy
public class LiteConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // DON'T call other @Bean methods in lite mode!
    // They would create new objects instead of returning the singleton.
}
```

### When to Use @Bean vs @Component?

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│   Use @Component / @Service / @Repository / @Controller          │
│   ─────────────────────────────────────────────────────          │
│   When YOU write the class and can annotate it directly.         │
│                                                                  │
│   Use @Bean inside @Configuration                                │
│   ──────────────────────────────────                             │
│   When you need to create beans from:                            │
│   • Third-party library classes (you can't add @Component)       │
│   • Classes that need complex initialization logic               │
│   • Multiple beans of the same type with different configs       │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Basic @Configuration & @Bean Example

```java
@Configuration
public class AppConfig {

    // This method creates and returns a bean
    // The method name "objectMapper" becomes the bean name
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    // Create a RestTemplate bean (third-party class)
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // Create a PasswordEncoder bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

### @Bean with Dependencies

Beans can depend on other beans. Spring **auto-injects** them via method parameters:

```java
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/library");
        ds.setUsername("root");
        ds.setPassword("password");
        ds.setMaximumPoolSize(10);
        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        // Spring automatically injects the dataSource bean created above
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public TransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

```
┌──────────────────────────────────────────────────────────────────┐
│           @Configuration Bean Dependency Graph                   │
│                                                                  │
│   @Configuration DataSourceConfig                                │
│   ┌──────────────────────────────────────────────────────────┐   │
│   │                                                          │   │
│   │  ┌────────────┐                                          │   │
│   │  │ DataSource │ ← Created first                          │   │
│   │  └─────┬──────┘                                          │   │
│   │        │                                                 │   │
│   │    ┌───┴─────────────────┐                               │   │
│   │    ▼                     ▼                               │   │
│   │  ┌──────────────┐  ┌─────────────────────┐               │   │
│   │  │ JdbcTemplate │  │ TransactionManager  │               │   │
│   │  │ (uses DS)    │  │ (uses DS)           │               │   │
│   │  └──────────────┘  └─────────────────────┘               │   │
│   │                                                          │   │
│   └──────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
```

### @Bean with Custom Name and Scope

```java
@Configuration
public class LibraryBeanConfig {

    // Custom bean name
    @Bean(name = "libraryBookService")
    public BookService bookService(BookRepository bookRepository) {
        return new BookService(bookRepository);
    }

    // Bean with prototype scope
    @Bean
    @Scope("prototype")
    public SearchFilter searchFilter() {
        return new SearchFilter();
    }

    // Bean with init and destroy methods
    @Bean(initMethod = "connect", destroyMethod = "disconnect")
    public DatabaseConnection databaseConnection() {
        DatabaseConnection conn = new DatabaseConnection();
        conn.setUrl("jdbc:mysql://localhost:3306/library");
        return conn;
    }
}
```

### @Bean with @Conditional (Advanced)

Create beans **only under certain conditions**:

```java
@Configuration
public class ConditionalConfig {

    // Only create this bean if "email.enabled=true" in application.properties
    @Bean
    @ConditionalOnProperty(name = "email.enabled", havingValue = "true")
    public EmailService emailService() {
        return new EmailService();
    }

    // Only create this bean if the class exists in classpath
    @Bean
    @ConditionalOnClass(name = "com.redis.RedisClient")
    public CacheService redisCacheService() {
        return new RedisCacheService();
    }
}
```

### Complete Real-World @Configuration Example

```java
@Configuration
public class LibraryAppConfig {

    // ============ CORS Configuration ============
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:3000")
                        .allowedMethods("GET", "POST", "PUT", "DELETE");
            }
        };
    }

    // ============ ModelMapper for DTO conversion ============
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
              .setMatchingStrategy(MatchingStrategies.STRICT);
        return mapper;
    }

    // ============ Custom Audit Bean ============
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("SYSTEM");
    }
}
```

---

## Quick Reference Cheat Sheet

```
┌────────────────────────────────────────────────────────────────┐
│              SPRING CORE — CHEAT SHEET                          │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  IoC          = Spring manages objects, not you                │
│  DI           = Spring injects dependencies into objects       │
│  Bean         = An object managed by Spring container          │
│                                                                │
│  @Component   = Generic Spring bean                            │
│  @Service     = Business logic bean                            │
│  @Repository  = Data access bean (+ exception translation)     │
│  @Controller  = Web MVC controller (returns views)             │
│  @RestController = REST API controller (returns JSON)          │
│                                                                │
│  @Autowired   = "Spring, please inject this dependency"        │
│  @Qualifier   = "Inject THIS specific bean"                    │
│  @Primary     = "Use this bean as default"                     │
│                                                                │
│  @Configuration = "This class defines beans"                   │
│  @Bean           = "This method returns a bean"                │
│                                                                │
│  Singleton    = One instance for entire app (DEFAULT)          │
│  Prototype    = New instance every time                        │
│  Request      = One instance per HTTP request                  │
│  Session      = One instance per HTTP session                  │
│                                                                │
│  ApplicationContext > BeanFactory (always use AppContext)       │
│                                                                │
│  Constructor Injection > Setter Injection > Field Injection    │
│                                                                │
│  @ComponentScan = Auto-discovers beans in packages             │
│  @SpringBootApplication = @Configuration + @ComponentScan      │
│                           + @EnableAutoConfiguration           │
└────────────────────────────────────────────────────────────────┘
```

---

## 10. Common Mistakes & Troubleshooting

### ❌ Mistake 1: "No qualifying bean of type... found"

```
Error: NoSuchBeanDefinitionException:
No qualifying bean of type 'com.example.MyService' available
```

**Common Causes & Fixes:**

| Cause                                       | Fix                                                      |
| ------------------------------------------- | -------------------------------------------------------- |
| Missing stereotype annotation               | Add `@Component`, `@Service`, etc. to the class          |
| Class is outside the scanned package         | Add `@ComponentScan(basePackages = "...")`               |
| Interface without implementation             | Ensure a concrete `@Service` class implements it         |
| Bean defined in test scope only              | Move to main scope or add `@TestConfiguration`           |

---

### ❌ Mistake 2: "Expected single matching bean but found 2"

```
Error: NoUniqueBeanDefinitionException:
expected single matching bean but found 2: emailService, smsService
```

**Fix:** Use `@Primary` on the default bean or `@Qualifier` at the injection point.

---

### ❌ Mistake 3: NullPointerException on Injected Field

```java
// WRONG — creating the object manually bypasses Spring DI!
BookService bookService = new BookService();  // @Autowired fields are NULL!
bookService.findBook(1L);  // NullPointerException!

// RIGHT — let Spring create and inject
@Autowired
private BookService bookService;  // Spring injects everything properly
```

**Rule:** Never use `new` to create a Spring-managed bean. Always let the container manage it.

---

### ❌ Mistake 4: @Autowired in Non-Bean Class

```java
// This class is NOT a Spring bean (no @Component)
public class BookHelper {

    @Autowired  // THIS WILL NOT WORK — Spring doesn't manage this class!
    private BookRepository bookRepository;  // Always null!
}
```

**Fix:** Add `@Component` to the class, or create it as a `@Bean` in a `@Configuration` class.

---

### ❌ Mistake 5: Field Injection with `final`

```java
@Service
public class BookService {
    @Autowired
    private final BookRepository bookRepository;  // COMPILE ERROR!
    // final fields MUST be set in constructor — use constructor injection instead
}
```

---

### ❌ Mistake 6: Using @Bean Outside @Configuration

```java
@Component  // NOT @Configuration — @Bean method will NOT have CGLIB proxy!
public class MyConfig {

    @Bean
    public DataSource dataSource() { return new HikariDataSource(); }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        // This creates a SECOND DataSource instead of reusing the singleton!
        return new JdbcTemplate(dataSource());  // BUG!
    }
}
```

**Fix:** Use `@Configuration` instead of `@Component` for classes containing `@Bean` methods.

---

## 11. Interview Questions & Answers

### Q1: What is the difference between IoC and DI?

**Answer:**
- **IoC (Inversion of Control)** is a **design principle** where the control of object creation is transferred from the programmer to a container/framework.
- **DI (Dependency Injection)** is a **design pattern** that implements IoC by injecting dependencies into objects.
- IoC is the **"what"** (concept), DI is the **"how"** (implementation technique).
- Other forms of IoC include: Service Locator pattern, Template Method pattern. DI is the most popular form used in Spring.

---

### Q2: What are the differences between @Component, @Service, @Repository, and @Controller?

**Answer:**

| Annotation     | Purpose                  | Special Behavior                                            |
| -------------- | ------------------------ | ----------------------------------------------------------- |
| `@Component`   | Generic bean             | None — just registers as a bean                            |
| `@Service`     | Business logic           | None — purely semantic (readability/clarity)                |
| `@Repository`  | Data access              | **Exception translation** — converts DB exceptions to Spring's `DataAccessException` |
| `@Controller`  | Web MVC controller       | Enables `@RequestMapping` and returns **views**             |
| `@RestController` | REST API controller   | `@Controller` + `@ResponseBody` — returns **JSON/XML**     |

> All four are specializations of `@Component`. Under the hood, `@Service`, `@Repository`, and `@Controller` are annotated with `@Component`.

---

### Q3: Why is Constructor Injection preferred over Field Injection?

**Answer:**

| Aspect              | Constructor Injection          | Field Injection                      |
| ------------------- | ------------------------------ | ------------------------------------ |
| Immutability        | ✅ Fields can be `final`       | ❌ Fields cannot be `final`          |
| Required deps       | ✅ Enforced at compile time    | ❌ NPE at runtime if missing         |
| Testability         | ✅ Easy to pass mocks          | ❌ Needs reflection/Spring context   |
| Framework coupling  | ✅ Works as plain Java         | ❌ Requires Spring/reflection        |
| Visibility          | ✅ Dependencies visible in API | ❌ Hidden inside class               |

**One-liner:** Constructor injection guarantees immutability, enforces required dependencies, and makes unit testing trivial.

---

### Q4: What is the default scope of a Spring Bean? How many scopes are there?

**Answer:**
- Default scope: **Singleton** (one instance per container)
- Total scopes: **5** — Singleton, Prototype, Request, Session, Application
- Singleton and Prototype work everywhere. Request, Session, and Application only work in web-aware containers.

---

### Q5: What happens when you inject a Prototype bean into a Singleton bean?

**Answer:**
- The prototype bean is created **once** when the singleton is initialized and the **same instance** is reused for every subsequent call — effectively making it behave like a singleton.
- **Fix:** Use `ObjectProvider<T>`, `Provider<T>`, or `@Lookup` method injection to get a new prototype instance each time.

---

### Q6: What is the difference between @Configuration and @Component for defining @Bean methods?

**Answer:**
- `@Configuration` creates a **CGLIB proxy** of the class. Calling a `@Bean` method from another `@Bean` method returns the **same singleton**.
- `@Component` (or any non-@Configuration class) does **not** create a proxy. Calling a `@Bean` method creates a **new object each time** (breaking singleton guarantee).
- Always use `@Configuration` for classes containing inter-dependent `@Bean` methods.

---

### Q7: What is the difference between BeanFactory and ApplicationContext?

**Answer:**
- `BeanFactory` provides basic DI functionality with **lazy** bean initialization.
- `ApplicationContext` extends `BeanFactory` with **eager** initialization, event publishing, i18n, AOP support, environment abstraction, and annotation-based configuration.
- In practice, always use `ApplicationContext` (or just use Spring Boot, which uses it automatically).

---

### Q8: How does @Autowired resolve beans when there are multiple implementations?

**Answer:** Spring follows this resolution order:
1. **Match by Type** — Look for a bean matching the declared type
2. **Match by @Qualifier** — If specified, use the named bean
3. **Match by @Primary** — If one bean is marked `@Primary`, use it
4. **Match by Field Name** — If the field name matches a bean name, use it
5. **Fail** — Throw `NoUniqueBeanDefinitionException`

```java
// Field name matching example:
@Autowired
private NotificationService emailNotification;
// Spring matches field name "emailNotification" to @Service("emailNotification")
```

---

### Q9: What is the complete lifecycle of a Spring Bean?

**Answer:**
1. **Instantiation** — Container creates the object using constructor
2. **Dependency Injection** — `@Autowired` fields/setters/constructor params are injected
3. **Aware callbacks** — `setBeanName()`, `setBeanFactory()`, `setApplicationContext()`
4. **BeanPostProcessor.postProcessBeforeInitialization()** — Pre-init processing
5. **@PostConstruct / InitializingBean.afterPropertiesSet()** — Custom init logic
6. **BeanPostProcessor.postProcessAfterInitialization()** — Post-init (AOP proxies created here)
7. **Bean is READY** — Available for use
8. **@PreDestroy / DisposableBean.destroy()** — Cleanup before container shutdown

---

### Q10: Can you use @Autowired on a static field?

**Answer:** **No.** `@Autowired` does not work on static fields because Spring DI is instance-based. Static fields belong to the class, not to any instance. Workaround: inject via a setter and assign to the static field (but this is generally a code smell).

---

> **Study Tip:** The best way to truly understand Spring Core is to:
> 1. Create a small project and add `@Component`, `@Service`, `@Repository`, `@Controller`
> 2. Use all three DI types and observe behavior
> 3. Print bean names using `ApplicationContext.getBeanDefinitionNames()`
> 4. Experiment with Prototype + Singleton injection pitfall
> 5. Add a `BeanPostProcessor` and observe the lifecycle order

---

> **Author:** Spring Core Study Notes  
> **Covers:** Spring Framework 6.x / Spring Boot 3.x
