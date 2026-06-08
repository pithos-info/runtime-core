# pithos-runtime-core

Foundation types for every Pithos service: application context, module lifecycle, executor pools, configuration, logging, and error codes.

---

## Packages

```
context/   ApplicationContext, ApplicationContextImpl, ServiceModule, ServiceLifeCycle,
           SystemContext, SystemContextImpl, ServiceConfigs, ContextCreator,
           AsyncTaskQueue, ErrorCode, ServiceException, Initializable
```

---

## Application lifecycle

Every Pithos service goes through three phases in order. Skipping or reordering them causes `IllegalStateException` from clients that guard against use before `start()`.

```
Phase 1 — init()      construct objects, register Guice bindings   (no I/O, no blocking)
Phase 2 — start()     open connections, establish pools, run migrations
Phase 3 — serve       bind ports, accept traffic
```

Shutdown reverses the serving phases:

```
Phase 4 — stop servers    drain in-flight requests
Phase 5 — shutdown()      close connections, drain pools
Phase 6 — executors       tear down ForkJoinPool + ScheduledExecutor
```

---

## `ApplicationContext`

The root object for a running service. Constructed once; passed into every module.

```java
public interface ApplicationContext {
    SystemContext           getSystemContext();
    Injector                getInjector();
    CompletableFuture<Void> start(long timeout, TimeUnit unit);
    CompletableFuture<Void> shutdown(long timeout, TimeUnit unit);
}
```

**`start(timeout, unit)`** — fans out `ServiceModule.start()` to all registered modules **in parallel**. Returns a `CompletableFuture<Void>` that resolves when every module has started. Callers `.join()` this before binding ports.

**`shutdown(timeout, unit)`** — fans out `ServiceModule.shutdown()` to all modules in parallel, then calls `SystemContext.shutdown()` to tear down executor pools. The system-context shutdown runs in `whenComplete` so it fires even if a module shutdown fails.

---

## `ApplicationContextImpl`

Drives phases 1 and 2. Holds the module list, injector, and system context.

```java
public ApplicationContextImpl(ContextCreator creator) {
    // Phase 1: construct all modules, call init(), create Guice injector
    modules.forEach(ServiceModule::init);
    injector = Guice.createInjector(modules);
}

// Phase 2: called externally (typically from the main class)
public CompletableFuture<Void> start(long timeout, TimeUnit unit) {
    CompletableFuture<?>[] futures = modules.stream()
        .map(m -> m.start(timeout, unit))
        .toArray(CompletableFuture[]::new);
    return CompletableFuture.allOf(futures);
}

public CompletableFuture<Void> shutdown(long timeout, TimeUnit unit) {
    CompletableFuture<?>[] futures = modules.stream()
        .map(m -> m.shutdown(timeout, unit))
        .toArray(CompletableFuture[]::new);
    return CompletableFuture.allOf(futures)
        .whenComplete((v, ex) -> systemContext.shutdown(unit.toMillis(timeout)));
}
```

---

## `ContextCreator`

Implemented once per deployable service. Supplies the `ConfigMap` and the ordered list of modules.

```java
public interface ContextCreator {
    ConfigMap                   getConfigMap();
    Iterable<ServiceModule>     getInjectionModules(ApplicationContext context);
}
```

`getInjectionModules` is called once inside the `ApplicationContextImpl` constructor. Return modules in dependency order where order matters within `init()` (though `start()` is always parallel across modules).

---

## `ServiceModule`

Base class for every Guice module that participates in the lifecycle.

```java
public abstract class ServiceModule extends AbstractModule {

    protected final AtomicBoolean initialized = new AtomicBoolean(false);

    // Phase 1 — must be implemented
    protected abstract boolean init();

    // Phase 2 — must be implemented by every concrete module
    public abstract CompletableFuture<Boolean> start(long timeout, TimeUnit unit);

    public abstract CompletableFuture<Boolean> shutdown(long timeout, TimeUnit unit);
}
```

### Rules for `init()`

- Construct clients (`new XxxClient(context)`) and service objects.
- Register all Guice bindings.
- Guard with `initialized.compareAndSet(false, true)` — `init()` is called exactly once.
- **Never** call `client.start()`, open sockets, or block on I/O.

### Rules for `start()`

- Open connections owned by this module.
- Chain intra-module ordered dependencies with `thenCompose` (e.g. DB must be up before Liquibase).
- Return `CompletableFuture<Boolean>`. Complete exceptionally on failure — the future propagates to the caller's `.join()` as an unchecked exception.
- Cross-module parallelism is handled by `ApplicationContext.start()`, not here.

### Rules for `shutdown()`

- Close connections in reverse start order.
- Mirror `start()`: if start opens A then B, shutdown closes B then A.

### Module with a single lifecycle client

```java
@Override
public CompletableFuture<Boolean> start(long timeout, TimeUnit unit) {
    return client.start(timeout, unit);
}

@Override
public CompletableFuture<Boolean> shutdown(long timeout, TimeUnit unit) {
    return client.shutdown(timeout, unit);
}
```

### Module with ordered start (DB pool → migration → cache pool)

```java
@Override
public CompletableFuture<Boolean> start(long timeout, TimeUnit unit) {
    return relationalClient.start(timeout, unit)
        .thenCompose(ok -> relationalClient.transaction(conn -> runLiquibase(conn)))
        .thenCompose(v  -> cacheClient.start(timeout, unit));
}

@Override
public CompletableFuture<Boolean> shutdown(long timeout, TimeUnit unit) {
    return cacheClient.shutdown(timeout, unit)
        .thenCompose(ok -> relationalClient.shutdown(timeout, unit));
}
```

---

## `ServiceLifeCycle`

Implemented by every client that manages an external connection.

```java
public interface ServiceLifeCycle {
    CompletableFuture<Boolean> start(long timeout, TimeUnit unit);
    CompletableFuture<Boolean> shutdown(long timeout, TimeUnit unit);
}
```

All Pithos infrastructure clients implement this interface:

| Client | Interface |
|---|---|
| `PostgresClient`, `CloudSqlClient` | `RelationalClient extends ServiceLifeCycle` |
| `RedisCacheClient`, `MemoryStoreCacheClient` | `DistributedCacheClient extends ServiceLifeCycle` |
| `MinioBlobStorageClient`, `GcsBlobStorageClient` | `BlobStorageClient extends ServiceLifeCycle` |
| `HashiCorpVaultClient`, `GcpSecretManagerClient` | `VaultClient extends ServiceLifeCycle` |
| `KeycloakOAuthClient`, `GcpIdentityOAuthClient` | `OAuthClient extends ServiceLifeCycle` |

Clients are only started through their owning module's `start()`. Service classes and handlers never call `client.start()` directly.

---

## Typical main-class pattern

```java
// Phase 1: construct (no I/O)
ApplicationContext ctx = new ApplicationContextImpl(new MyContextCreator(config));

// Phase 2: open all connections (blocks)
ctx.start(30, TimeUnit.SECONDS).join();

// Phase 3: accept traffic
grpcServer.start();
httpServer.start();

// Shutdown hook
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    httpServer.stop();
    grpcServer.stop();
    ctx.shutdown(30, TimeUnit.SECONDS).join();   // closes connections + tears down executors
}));
```

---

## `SystemContext`

Holds the shared thread pools and config map. Available via `applicationContext.getSystemContext()`.

```java
public interface SystemContext {
    String                       getServiceName();
    ForkJoinPool                 getForkJoinExecutor();    // used by all async client calls
    ScheduledThreadPoolExecutor  getScheduledExecutor();
    ServiceLogger                getLogger();
    ConfigMap                    getConfigMap();
    ServiceConfigs               getServiceConfigs();
    AsyncTaskQueue               getTaskQueue();           // background write-back tasks
    boolean                      shutdown(long ms);
}
```

Pool size = `availableProcessors × bootstrapConfigs.multiplier`. Set `multiplier: 2` for most services.

---

## `ConfigMap`

Single protobuf message carrying all infrastructure config for a service instance. Sections map directly to client configuration structs: `postgresConfigs`, `redisConfigs`, `keycloakOAuthConfigs`, `hashiCorpVaultConfigs`, `minioBlobStorageConfigs`, etc.

Populated at startup from a YAML file via `ProtoBufSerde.fromYaml()` in the `serde` module. YAML keys are exact camelCase proto field names. Unknown keys (e.g. `server.httpPort`) are silently ignored by `JsonFormat.parser().ignoringUnknownFields()`, allowing the YAML to carry app-level config alongside proto-mapped infrastructure config.
