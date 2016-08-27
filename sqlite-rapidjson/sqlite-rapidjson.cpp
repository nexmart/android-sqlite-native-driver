//
// Created by Fabian Muecke on 26.08.16.
//

#include "sqlite-rapidjson.h"
#include "../rapidjson/include/rapidjson/document.h"
#include <android/log.h>

using namespace rapidjson;

int process_table(sqlite3 *connection, const Value &tableObj, primaryKeysFn getPrimaryKeysFn);
void finish_statements(sqlite3_stmt *updateStatement, sqlite3_stmt *insertStatement);
int bind_statement(sqlite3_stmt *statement, const Value &columns, const Value &values);
int create_temp_table(sqlite3 *connection, const char *table, const char **keys, int length);
int create_update_statement(sqlite3 *connection, const char *table, const Value &columns, const char **primaryKeys, int pkLength, sqlite3_stmt **statement);
int create_insert_statement(sqlite3 *connection, const char *table, const Value &columns, int ignoreExisting, sqlite3_stmt **statement);
int contains(const char **array, int length, const char *string);

int sqlite_import_json(sqlite3 *connection, const char *json, primaryKeysFn getPrimaryKeysFn) {
    Document document;
    document.Parse(json);
    const Value &current = document["current"];

    assert(current.IsArray());
    for (SizeType i = 0; i < current.Size(); i++) {
        const Value &tableObj = current[i];
        assert(tableObj.IsObject());
        int tableResult = process_table(connection, tableObj, getPrimaryKeysFn);
        if (tableResult != SQLITE_OK) {
            return tableResult;
        }
    }

    return SQLITE_OK;
}

int process_table(sqlite3 *connection, const Value &tableObj, primaryKeysFn getPrimaryKeysFn) {
    const char *table = tableObj["table"].GetString();
    __android_log_print(ANDROID_LOG_VERBOSE, "JSON_NDK", "processing table: %s\n", table);

    const Value &columns = tableObj["columns"];
    assert(columns.IsArray());

    const Value &values = tableObj["values"];
    assert(values.IsArray());

    int pkLength;
    const char **primaryKeys = getPrimaryKeysFn(table, &pkLength);

    if (pkLength > 1) {
        int tempTableResult = create_temp_table(connection, table, primaryKeys, pkLength);
        if (tempTableResult != SQLITE_OK) {
            return tempTableResult;
        }
    }

    sqlite3_stmt *updateStatement;
    int updateStatementResult = create_update_statement(connection, table, columns, primaryKeys, pkLength, &updateStatement);

    if (updateStatementResult != SQLITE_OK) {
        return updateStatementResult;
    }

    int tryUpdate = pkLength != columns.Size() && updateStatement != NULL;

    sqlite3_stmt *insertStatement;
    int insertStatementResult = create_insert_statement(connection, table, columns, 1, &insertStatement);
    if (insertStatementResult != SQLITE_OK) {
        finish_statements(updateStatement, NULL);
        return insertStatementResult;
    }

    // TODO:
    //SQLiteStatement insertTempKeysStatement = null;
    //if (keys.size() > 1) {
    //insertTempKeysStatement = insertStatementForTempTable(table, keys);
    //}

    // TODO:
    //List<Integer> primaryKeyIndexes = new ArrayList<>(keys.size());
    //for (String key : keys) {
    //    primaryKeyIndexes.add(JSONArrayUtils.indexOfObject(key, columns));
    //}

    for (int i = 0; i < values.Size(); i++) {
        const Value &valuesArray = values[i];
        assert(valuesArray.IsArray());

        // TODO: pk stuff

        if (tryUpdate) {
            sqlite3_reset(updateStatement);
            sqlite3_clear_bindings(updateStatement);
            int bindResult = bind_statement(updateStatement, columns, valuesArray);
            if (bindResult != SQLITE_OK) {
                finish_statements(updateStatement, insertStatement);
                return bindResult;
            }

            int updateResult = sqlite3_step(updateStatement);
            if (updateResult != SQLITE_DONE) {
                finish_statements(updateStatement, insertStatement);
                return updateResult;
            }
        }

        sqlite3_reset(insertStatement);
        sqlite3_clear_bindings(insertStatement);
        int bindResult = bind_statement(insertStatement, columns, valuesArray);
        if (bindResult != SQLITE_OK) {
            finish_statements(updateStatement, insertStatement);
            return bindResult;
        }

        int insertResult = sqlite3_step(insertStatement);
        if (insertResult != SQLITE_DONE) {
            const char *message = sqlite3_errmsg(connection);
            if (strcmp("FOREIGN KEY constraint failed", message) == 0) {
                __android_log_print(ANDROID_LOG_VERBOSE, "JSON_NDK", "foreign key constraint failed\n");
            } else {
                finish_statements(updateStatement, insertStatement);
                return insertResult;
            }
        }
    }

    finish_statements(updateStatement, insertStatement);

    return SQLITE_OK;
}

void finish_statements(sqlite3_stmt *updateStatement, sqlite3_stmt *insertStatement) {
    if (updateStatement) {
        sqlite3_finalize(updateStatement);
    }

    if (insertStatement) {
        sqlite3_finalize(insertStatement);
    }
}

int bind_statement(sqlite3_stmt *statement, const Value &columns, const Value &values) {
    for (int i = 0; i < values.Size(); i++) {
        std::string parameter = ":";
        parameter += columns[i].GetString();

        const Value &value = values[i];
        int index = sqlite3_bind_parameter_index(statement, parameter.c_str());
        int bindResult;
        if (value.IsNull()) {
            bindResult = sqlite3_bind_null(statement, index);
        } else if (value.IsBool()) {
            bindResult = sqlite3_bind_int(statement, index, value.GetBool());
        } else if (value.IsNumber()) {
            bindResult = sqlite3_bind_int64(statement, index, value.GetInt64());
        } else {
            bindResult = sqlite3_bind_text(statement, index, value.GetString(), -1, SQLITE_TRANSIENT);
        }

        if (bindResult != SQLITE_OK) {
            return bindResult;
        }
    }
    return SQLITE_OK;
}

int create_temp_table(sqlite3 *connection, const char *table, const char **keys, int length) {
    return SQLITE_OK;
}

int create_update_statement(sqlite3 *connection, const char *table, const Value &columns, const char **primaryKeys, int pkLength, sqlite3_stmt **statement) {
    if (pkLength == columns.Size()) {
        statement = NULL;
        return SQLITE_OK;
    }

    std::string sql = std::string("UPDATE \"") + table + "\" SET ";
    for (int i = 0; i < columns.Size(); i++) {
        const char *column = columns[i].GetString();
        if (contains(primaryKeys, pkLength, column)) {
            continue;
        }

        std::string columnStr = std::string(column);
        sql += "\"" + columnStr + "\" = :" + columnStr;
        if (i < columns.Size() - 1) {
            sql += ", ";
        }
    }

    sql += " WHERE ";
    for (int i = 0; i < pkLength; i++) {
        const char *pkColumn = primaryKeys[i];
        std::string pkColumnStr = std::string(pkColumn);
        sql += "\"" + pkColumnStr + "\" = :" + pkColumnStr;
        if (i < pkLength - 1) {
            sql += " AND ";
        }
    }

    __android_log_print(ANDROID_LOG_VERBOSE, "JSON_NDK", "update table statement: %s\n", sql.c_str());
    return sqlite3_prepare_v2(connection, sql.c_str(), -1, statement, NULL);
}

int create_insert_statement(sqlite3 *connection, const char *table, const Value &columns, int ignoreExisting, sqlite3_stmt **statement) {
    std::string sql = "INSERT ";
    if (ignoreExisting) {
        sql += "OR IGNORE ";
    }
    sql += "INTO \"" + std::string(table) + "\" (";
    for (int i = 0; i < columns.Size(); i++) {
        const char *column = columns[i].GetString();
        std::string columnStr = std::string(column);
        sql += "\"" + columnStr + "\"";
        if (i < columns.Size() - 1) {
            sql += ", ";
        }
    }

    sql += ") VALUES ( ";
    for (int i = 0; i < columns.Size(); i++) {
        const char *column = columns[i].GetString();
        std::string columnStr = std::string(column);
        sql += ":" + columnStr;
        if (i < columns.Size() - 1) {
            sql += ", ";
        }
    }
    sql += ")";

    __android_log_print(ANDROID_LOG_VERBOSE, "JSON_NDK", "update table statement: %s\n", sql.c_str());
    return sqlite3_prepare_v2(connection, sql.c_str(), -1, statement, NULL);
}

int contains(const char **array, int length, const char *string) {
    for (int i = 0; i < length; i++) {
        const char* str = array[i];
        if (strcmp(str, string) == 0) {
            return 1;
        }
    }
    return 0;
}
