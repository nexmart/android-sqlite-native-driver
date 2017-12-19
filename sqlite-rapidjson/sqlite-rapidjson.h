//
// Created by Fabian Muecke on 26.08.16.
//

#ifndef SWISSCAMPUS_LERNAPP_SQLITE_RAPIDJSON_H
#define SWISSCAMPUS_LERNAPP_SQLITE_RAPIDJSON_H

#include "../sqlcipher/sqlite3.h"

typedef const char ** (*primaryKeysFn)(const char *, int *);

int sqlite_import_json(sqlite3 *connection, const char *json, primaryKeysFn getPrimaryKeysFn);

#endif //SWISSCAMPUS_LERNAPP_SQLITE_RAPIDJSON_H
