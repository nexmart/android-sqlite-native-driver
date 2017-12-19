# Android SQLite native driver

Provides a native build of SQLite with an interface for Android libraries.

Based on [JavaCPP](https://github.com/bytedeco/javacpp) and [SQLCipher](https://github.com/sqlcipher/sqlcipher). Borrowed some build scripts from [Android SQLCipher](https://github.com/sqlcipher/android-database-sqlcipher).

License: UNLICENSE (public domain). Please note that SQLCipher is not public domain, but BSD 3-clause License. Find it here [here](https://github.com/sqlcipher/sqlcipher/blob/master/LICENSE).

## About

Android SQLite native driver provides:
- Semi-automatic AAR build, including build of a native SQLite library (currently version 3.15.2) for major Android targets (`armeabi`, `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`) - See setup steps below
- `SQLiteConnection` and `SQLiteStatement` interfaces for the most common use cases of accessing SQLite databases
- Registration of function callbacks in SQLite
- Safely making asynchronous database calls using `Database` and `DatabaseAccess` classes (see example below) by leveraging [Bolts](https://github.com/BoltsFramework/Bolts-Android) - This is optional. You're welcome to just use `NativeSQLiteConnection` directly, but keep in mind, that SQLite by itself is not thread-safe and you'll have to handle both thread-safety and opening/closing database connections by yourself then.
- Lightweight database migrations (just apply, no revert). Again this is optional. Feel free to roll your own migration mechanism.
- `DIACRITIC` collation for text columns that should be sorted, respecting diacritics. **NOTE**: This makes sorting considerably slower, as it uses a callback to Java internally. Just apply `COLLATE DIACRITIC` to your `TEXT` column, if you want this.
- Fast initialization/update of a database from a JSON String (using [RapidJSON](https://github.com/miloyip/rapidjson))
- Database encryption using [SQLCipher](https://github.com/sqlcipher/sqlcipher) (pass null key to skip encryption)

SQLite connection handles, function handles etc. are mostly wrapped for type-safety - single exception are callback function arguments (see example below).

# Setup

To init submodules, build Open SSL as well as Amalgamation and finally the native SQLite lib (libjniSQLiteNative.so) call:

$ `./initialize.sh`

Build the AAR with gradle:

$ `./gradlew assembleRelease`

# Usage examples

## Bolts API
### Limitation
Whatever happens in your database operations `run` method (see below) must run synchronously, because of the way Bolts threading on Android works. If you do something asynchronous in there, the next queued database operation might start before your operation finishes.

### Establishing a database connection

This example assumes the initial schema of your database is stored in a raw text file called `schema`. `MyDatabaseOperations` provides your actual high-level database functions. This is covered below.

**NOTE**: This sets up a thread-safe way to call your database. Because de-/encryption makes opening connections expensive, the connection is kept open until explicitly closed.

```java
public Task<Database<MyDatabaseOperations>> establishDatabase(File file, String encryptionKey) {
    if (file == null) {
        return Task.forResult(null);
    }

    Database<MyDatabaseOperations> database = new Database<>(new DatabaseAccess(file.getAbsolutePath(), encryptionKey), new DatabaseOperationsFactory() {
        @Override
        public MyDatabaseOperations createOperations(@Nullable SQLiteConnection connection) {
            return new MyDatabaseOperationsImpl(connection);
        }
    });

    return database.migrate(Arrays.asList(
            new CreateMigrationsTableMigration(1),
            new FileBasedMigration(201704041221L, "Created Initial Schema", this, R.raw.schema)
    ));
}
```

### Read

```java
public void displayMyObjectsAsync(Database<MyDatabaseOperations> database, final String name) {
    CancellationTokenSource tokenSource = new CancellationTokenSource();
    final CancellationToken token = tokenSource.getToken();

    database.performOperations(new Database.OperationsRunner<MyDatabaseOperations, List<MyObj>>() {
            @Override
            public List<MyObj> run(MyDatabaseOperations databaseOperations) throws SQLiteException {
                return databaseOperations.findObjectsWithName(name, token);
            }
        }, false).onSuccess(new Continuation<List<MyObj>, Void>() {
            @Override
            public Void then(Task<List<MyObj>> task) throws Exception {
                // Update your UI here.
                // It is important that you do your UI updates on the main thread, so you need to tell Bolts to execute your continuation there. We use the class UIThreadExecutor for that. Find our implementation below.
                return null;
            }
        }, new UIThreadExecutor());
}
```

```java
public class MyDatabaseOperationsImpl implements MyDatabaseOperations {

    // ... snip

    public List<MyObj> findObjectsWithName(String name, final CancellationToken token) throws SQLiteException {
        SQLiteStatement statement = connection.createStatement("SELECT id, name FROM my_objects WHERE name LIKE :name");
        try {
            statement.bindValue("%" + name + "%").to(":name");
            return statement.readList(new SQLiteStatement.CancellableRowValueCallback<ContentItem>() {
                @Override
                public boolean shouldCancel() {
                    return token.isCancellationRequested();
                }

                @Override
                public ContentItem readRow(SQLiteRow row) throws SQLiteException {
                    return new MyObj(row.getId(), row.getText("name"));
                }
            }, false);
        } finally {
            statement.finish();
        }
    }
}
```

### Insert

```java
public void insertMyObjectsAsync(Database<MyDatabaseOperations> database, final List<MyObj> objs) {
    database.performOperations(new Database.OperationsRunner<MyDatabaseOperations, Void>() {
            @Override
            public Void run(MyDatabaseOperations databaseOperations) throws SQLiteException {
                databaseOperations.insertObjects(objs);
                return null;
            }
        }, true).onSuccess(new Continuation<Void, Void>() {
            @Override
            public Void then(Task<Void> task) throws Exception {
                // Update your UI here.
                // It is important that you do your UI updates on the main thread, so you need to tell Bolts to execute your continuation there. We use the class UIThreadExecutor for that. Find our implementation below.
                return null;
            }
        }, new UIThreadExecutor());
}
```

```java
public class MyDatabaseOperationsImpl implements MyDatabaseOperations {

    // ... snip

    public void insertObjects(List<MyObj> objs) throws SQLiteException {
        SQLiteStatement statement = connection.createStatement("INSERT INTO my_objects (id, name) VALUES (:id, :name)");
        try
        {
            for (MyObj obj : objs) {
                statement.resetAndClearBindings();

                statement.bindNull().to(":id");
                statement.bindValue(obj.getName()).to(":name");
                statement.step();

                obj.setId(connection.getLastInsertRowId());
             }
        } finally {
            statement.finish();
        }
    }
}
```

### Prerequisites

```java
public interface MyDatabaseOperations {
    List<MyObj> findObjectsWithName(String name, final CancellationToken token) throws SQLiteException;
    List<MyObj> insertObjects(List<MyObj> objs) throws SQLiteException;
}

public class MyDatabaseOperationsImpl implements MyDatabaseOperations {

    private final SQLiteConnection connection;

    public MyDatabaseOperations(SQLiteConnection connection) {
        this.connection = connection;
    }

    // snipped functions - see above for examples
}
```

```java
public class UIThreadExecutor implements Executor {
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void execute(@NonNull Runnable runnable) {
        handler.post(runnable);
    }
}
```

## Non-Bolts API
### Establishing a database connection
```java
public NativeSQLiteConnection establishConnection(String path) {
    return new NativeSQLiteConnection(path, SQLiteNative.SQLITE_OPEN_CREATE_IF_NECESSARY | SQLiteNative.SQLITE_OPEN_READWRITE);
}
```

### Read
```java
public List<MyObj> findObjectsWithName(NativeSQLiteConnection connection, String name) throws SQLiteException {
    connection.open();
    try
    {
        SQLiteStatement statement = connection.createStatement("SELECT id, name FROM my_objects WHERE name LIKE :name");
        statement.bindValue("%" + name + "%").to(":name");
        statement.readList(new SQLiteStatement.RowValueCallback<T>() {
            @Override
            public T readRow(SQLiteRow row) throws SQLiteException {
                return new MyObj(row.getId("id"), row.getText("name"));
            }
        }, true);
    } finally {
        connection.close();
    }
}
```

### Insert
```java
public void insertObjects(NativeSQLiteConnection connection, List<MyObj> objs) throws SQLiteException {
    connection.open();
    try
    {
        connection.beginTransaction();
        SQLiteStatement statement = connection.createStatement("INSERT INTO my_objects (id, name) VALUES (:id, :name)");
        try
        {
            for (MyObj obj : objs) {
                statement.resetAndClearBindings();

                statement.bindNull().to(":id");
                statement.bindValue(obj.getName()).to(":name");
                statement.step();

                obj.setId(connection.getLastInsertRowId());
             }

            connection.commitTransaction();
        } catch (SQLiteException ex) {
            connection.rollbackTransaction();
            throw ex;
        } finally {
            statement.finish();
        }
    } finally {
        connection.close();
    }
}
```

## Registering functions
We'll assume the provided connection has already been opened and close also happens outside of this function - you can use this with both, the Bolts or non-Bolts API.

**NOTE**: A registered function only stays registered as long as the connection is open. If you need the same function again when re-opening a connection, you have to re-register it.

```java
public void createContainsFunction(SQLiteConnection connection) throws SQLiteException {
    connection.registerFunction("diacritic_contains", 2, new SQLiteFunction() {
        @Override
        public void call(SQLiteNative.ContextHandle context, int argc, PointerPointer<SQLiteNative.ValueHandle> argv) {
            SQLiteNative.ValueHandle leftHandle = argv.get(SQLiteNative.ValueHandle.class, 0);
            SQLiteNative.ValueHandle rightHandle = argv.get(SQLiteNative.ValueHandle.class, 1);
            String left = SQLiteNative.sqlite3_value_text(leftHandle);
            String right = SQLiteNative.sqlite3_value_text(rightHandle);
            if (left == null) {
                left = "";
            }
            if (right == null) {
                right = "";
            }
            SQLiteNative.sqlite3_result_int(context, left.toLowerCase().contains(right.toLowerCase()) ? 1 : 0);
        }
    });
}
```

## JSON import

The JSON file structure is assumed to be like this:

```javascript
{ "current": [
    {
        "table": "my_objects",
        "columns": [
            "id",
            "name"
        ],
        "count": 16567,
        "values": [
            [
                1,
                "Object name 1"
            ]
            // more rows ...
        ]
    }
    // more tables ...
  ]
}
```

This will either initialize an empty database or update the database to the state represented by the JSON string.

**NOTE**: Your tables should be in the `"current"` array in a meaningful order and you should setup your tables to CASCADE deletes for foreign keys, so you don't run into PK/FK errors.

**WARNING**: All rows not present in the JSON will be deleted!

```java
public void importJson(SQLiteConnection connection, String json) throws SQLiteException {
    connection.importJson(json, new PrimaryKeysCallbackFunction() {
        @Override
        public String[] call(String table) {
            List<String> primaryKeys = getPrimaryKeys(table);
            return primaryKeys.toArray(new String[primaryKeys.size()]);
        }
    });
}

private List<String> getPrimaryKeys(String table) {
    switch (table) {
        case "hierarchy":
            return Arrays.asList("parent_id", "child_id"); // The table hierarchy just connects parents and children, so it has a combined primary key
        default:
            return Collections.singletonList("id"); // All other tables just have a "id INTEGER NOT NULL PRIMARY KEY" primary key column. If your primary keys are named different, then just insert the appropriate switch cases.
    }
}
```
