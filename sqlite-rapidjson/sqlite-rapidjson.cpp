//
// Created by Fabian Muecke on 26.08.16.
//

#include "sqlite-rapidjson.h"
#include "../rapidjson/include/rapidjson/document.h"
#include <android/log.h>
#include <vector>
#include <sstream>

using namespace rapidjson;

int process_table(sqlite3 *connection, const Value &tableObj, primaryKeysFn getPrimaryKeysFn);
const char * print_error(sqlite3 *connection, const char *table, int code);
void drop_temp_table_if_necessary(sqlite3 *connection, const char *table, int primaryKeysLength);
int execute_statement(sqlite3 *connection, std::string sql);
int delete_from_table(sqlite3 *connection, const char *table, const char **keys, int length, std::vector<int64_t> usedPrimaryKeys);
void finish_statements(sqlite3_stmt *updateStatement, sqlite3_stmt *insertStatement, sqlite3_stmt *insertTempKeysStatement);
int bind_statement(sqlite3_stmt *statement, const Value &columns, const Value &values);
int create_temp_table(sqlite3 *connection, const char *table, const char **keys, int length);
int create_insert_statement_for_temp_table(sqlite3 *connection, const char *table, const char **keys, int length, sqlite3_stmt **statement);
int create_update_statement(sqlite3 *connection, const char *table, const Value &columns, const char **primaryKeys, int pkLength, sqlite3_stmt **statement);
int create_insert_statement(sqlite3 *connection, const char *table, const Value &columns, int ignoreExisting, sqlite3_stmt **statement);
int index_of(const char *string, const Value &columns);
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
        if (tempTableResult != SQLITE_DONE) {
            print_error(connection, table, tempTableResult);
            return tempTableResult;
        }
    }

    sqlite3_stmt *updateStatement;
    int updateStatementResult = create_update_statement(connection, table, columns, primaryKeys, pkLength, &updateStatement);

    if (updateStatementResult != SQLITE_OK) {
        print_error(connection, table, updateStatementResult);
        drop_temp_table_if_necessary(connection, table, pkLength);
        return updateStatementResult;
    }

    int tryUpdate = pkLength != columns.Size() && updateStatement != NULL;

    sqlite3_stmt *insertStatement;
    int insertStatementResult = create_insert_statement(connection, table, columns, 1, &insertStatement);
    if (insertStatementResult != SQLITE_OK) {
        print_error(connection, table, insertStatementResult);
        drop_temp_table_if_necessary(connection, table, pkLength);
        finish_statements(updateStatement, NULL, NULL);
        return insertStatementResult;
    }

    sqlite3_stmt *insertTempKeysStatement;
    if (pkLength > 1) {
        int insertTempKeysStatementResult = create_insert_statement_for_temp_table(connection, table, primaryKeys, pkLength, &insertTempKeysStatement);
        if (insertTempKeysStatementResult != SQLITE_OK) {
            print_error(connection, table, insertTempKeysStatementResult);
            drop_temp_table_if_necessary(connection, table, pkLength);
            finish_statements(updateStatement, NULL, NULL);
            return insertTempKeysStatementResult;
        }
    } else {
        insertTempKeysStatement = NULL;
    }

    std::vector<int> primaryKeyIndexes = std::vector<int>();
    for (int i = 0; i < pkLength; i++) {
        const char *key = primaryKeys[i];
        primaryKeyIndexes.push_back(index_of(key, columns));
    }

    std::vector<int64_t> usedPrimaryKeys = std::vector<int64_t>();
    for (int i = 0; i < values.Size(); i++) {
        const Value &valuesArray = values[i];
        assert(valuesArray.IsArray());

        if (pkLength == 1) {
            usedPrimaryKeys.push_back(valuesArray[primaryKeyIndexes.at(0)].GetInt64());
        } else {
            assert(insertTempKeysStatement != NULL);
            sqlite3_reset(insertTempKeysStatement);
            sqlite3_clear_bindings(insertTempKeysStatement);
            for (int keyIndex = 0; keyIndex < pkLength; keyIndex++) {
                const char *key = primaryKeys[keyIndex];
                int index = primaryKeyIndexes.at(keyIndex);
                const int64_t pkValue = valuesArray[index].GetInt64();
                std::string parameter = ":";
                parameter += key;

                int paramIndex = sqlite3_bind_parameter_index(insertTempKeysStatement, parameter.c_str());
                //__android_log_print(ANDROID_LOG_VERBOSE, "JSON_NDK", "temp keys of table: %s bind: %lld to: %s at index: %d\n", table, (long long)pkValue, parameter.c_str(), paramIndex);
                int bindResult = sqlite3_bind_int64(insertTempKeysStatement, paramIndex, pkValue);
                if (bindResult != SQLITE_OK) {
                    print_error(connection, table, bindResult);
                    drop_temp_table_if_necessary(connection, table, pkLength);
                    finish_statements(updateStatement, insertStatement, insertTempKeysStatement);
                    return bindResult;
                }
            }
            int stepResult = sqlite3_step(insertTempKeysStatement);
            if (stepResult != SQLITE_DONE) {
                print_error(connection, table, stepResult);
                drop_temp_table_if_necessary(connection, table, pkLength);
                finish_statements(updateStatement, insertStatement, insertTempKeysStatement);
                return stepResult;
            }
        }

        if (tryUpdate) {
            sqlite3_reset(updateStatement);
            sqlite3_clear_bindings(updateStatement);
            int bindResult = bind_statement(updateStatement, columns, valuesArray);
            if (bindResult != SQLITE_OK) {
                print_error(connection, table, bindResult);
                drop_temp_table_if_necessary(connection, table, pkLength);
                finish_statements(updateStatement, insertStatement, insertTempKeysStatement);
                return bindResult;
            }

            int updateResult = sqlite3_step(updateStatement);
            if (updateResult != SQLITE_DONE) {
                print_error(connection, table, updateResult);
                drop_temp_table_if_necessary(connection, table, pkLength);
                finish_statements(updateStatement, insertStatement, insertTempKeysStatement);
                return updateResult;
            }
        }

        sqlite3_reset(insertStatement);
        sqlite3_clear_bindings(insertStatement);
        int bindResult = bind_statement(insertStatement, columns, valuesArray);
        if (bindResult != SQLITE_OK) {
            print_error(connection, table, bindResult);
            drop_temp_table_if_necessary(connection, table, pkLength);
            finish_statements(updateStatement, insertStatement, insertTempKeysStatement);
            return bindResult;
        }

        int insertResult = sqlite3_step(insertStatement);
        if (insertResult != SQLITE_DONE) {
            const char *message = print_error(connection, table, insertResult);
            if (strcmp("FOREIGN KEY constraint failed", message) != 0) {
                drop_temp_table_if_necessary(connection, table, pkLength);
                finish_statements(updateStatement, insertStatement, insertTempKeysStatement);
                return insertResult;
            }
        }
    }

    int deleteResult = delete_from_table(connection, table, primaryKeys, pkLength, usedPrimaryKeys);
    int result = SQLITE_OK;
    if (deleteResult != SQLITE_DONE) {
        print_error(connection, table, deleteResult);
        result = deleteResult;
    }

    drop_temp_table_if_necessary(connection, table, pkLength);
    finish_statements(updateStatement, insertStatement, insertTempKeysStatement);

    return result;
}

const char * print_error(sqlite3 *connection, const char *table, int code) {
    const char * message = sqlite3_errmsg(connection);
    __android_log_print(ANDROID_LOG_WARN, "JSON_NDK", "table: %s code: %d error: %s\n", table, code, message);
    return message;
}

void drop_temp_table_if_necessary(sqlite3 *connection, const char *table, int primaryKeysLength) {
    if (primaryKeysLength > 1) {
        std::string sql = "DROP TABLE temp_";
        sql += table;
        execute_statement(connection, sql);
    }
}

template <typename T>
std::string to_string(T value)
{
    std::ostringstream os ;
    os << value ;
    return os.str() ;
}

int delete_from_table(sqlite3 *connection, const char *table, const char **keys, int length, std::vector<int64_t> usedPrimaryKeys) {
    std::string sql = "DELETE FROM \"";
    sql += table;
    sql += "\" ";

    if (length == 1) {
        if (usedPrimaryKeys.size() > 0) {
            sql += " WHERE \"";
            sql += keys[0];
            sql += "\" NOT IN (";
            for (int i = 0; i < usedPrimaryKeys.size(); i++) {
                int64_t value = usedPrimaryKeys.at(i);
                sql += to_string(value);
                if (i < usedPrimaryKeys.size() - 1) {
                    sql += ", ";
                }
            }
            sql += ")";
        }
    } else {
        sql += " WHERE NOT EXISTS (SELECT 1 FROM \"";
        sql += table;
        sql += "\" a JOIN temp_";
        sql += table;
        sql += " b ON ";
        for (int i = 0; i < length; i++) {
            const char *key = keys[i];
            sql += "a.";
            sql += key;
            sql += " = b.";
            sql += key;
            if (i < length - 1) {
                sql += " AND ";
            }
        }
        sql += ")";
    }

    //__android_log_print(ANDROID_LOG_VERBOSE, "JSON_NDK", "%s\n", sql.c_str());
    return execute_statement(connection, sql);
}

int execute_statement(sqlite3 *connection, std::string sql) {
    sqlite3_stmt *statement;
    int prepareResult = sqlite3_prepare_v2(connection, sql.c_str(), -1, &statement, NULL);
    if (prepareResult != SQLITE_OK) {
        return prepareResult;
    }

    int stepResult = sqlite3_step(statement);
    sqlite3_finalize(statement);
    return stepResult;
}

void finish_statements(sqlite3_stmt *updateStatement, sqlite3_stmt *insertStatement, sqlite3_stmt *insertTempKeysStatement) {
    if (updateStatement) {
        sqlite3_finalize(updateStatement);
    }

    if (insertStatement) {
        sqlite3_finalize(insertStatement);
    }

    if (insertTempKeysStatement) {
        sqlite3_finalize(insertTempKeysStatement);
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
    std::string sql = "CREATE TEMPORARY TABLE temp_";
    sql += table;
    sql += " (";
    for (int i = 0; i < length; i++) {
        const char *key = keys[i];
        sql += "\"";
        sql += key;
        sql += "\" INTEGER NOT NULL, ";
    }
    sql += "PRIMARY KEY (";
    for (int i = 0; i < length; i++) {
        const char *key = keys[i];
        sql += "\"";
        sql += key;
        sql += "\"";
        if (i < length - 1) {
            sql += ", ";
        }
    }
    sql += ") )";

    return execute_statement(connection, sql);
}

int create_insert_statement_for_temp_table(sqlite3 *connection, const char *table, const char **keys, int length, sqlite3_stmt **statement) {
    std::string sql = "INSERT INTO temp_";
    sql += table;
    sql += " (";
    for (int i = 0; i < length; i++) {
        const char *key = keys[i];
        sql += "\"";
        sql += key;
        sql += "\"";
        if (i < length - 1) {
            sql += ", ";
        }
    }
    sql += ") VALUES (";
    for (int i = 0; i < length; i++) {
        const char *key = keys[i];
        sql += ":";
        sql += key;
        if (i < length - 1) {
            sql += ", ";
        }
    }
    sql += ")";

    return sqlite3_prepare_v2(connection, sql.c_str(), -1, statement, NULL);
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

    return sqlite3_prepare_v2(connection, sql.c_str(), -1, statement, NULL);
}

int index_of(const char *string, const Value &columns) {
    for (int i = 0; i < columns.Size(); i++) {
        const char *column = columns[i].GetString();
        if (strcmp(column, string) == 0) {
            return i;
        }
    }
    return -1;
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
