/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.ldk;

import org.labkey.api.data.DbSchema;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.dialect.SqlDialect;

/**
 * User: bbimber
 * Date: 7/14/12
 */
public class LDKSchema
{
    private static final LDKSchema _instance = new LDKSchema();
    public static final String SCHEMA_NAME = "ldk";

    public static final String TABLE_METRICS = "metrics";
    public static final String TABLE_NOTIFICATION_RECIPIENTS = "notificationrecipients";
    public static final String TABLE_LDAP_SYNC_MAP = "ldapSyncMap";

    public static final String TABLE_LOOKUPS = "lookup_data";
    public static final String TABLE_LOOKUP_SETS = "lookup_sets";

    public static LDKSchema getInstance()
    {
        return _instance;
    }

    public static TableInfo getTable(String name)
    {
        return _instance.getSchema().getTable(name);
    }

    private LDKSchema()
    {
        // private constructor to prevent instantiation from
    }

    public String getSchemaName()
    {
        return SCHEMA_NAME;
    }

    public DbSchema getSchema()
    {
        return DbSchema.get(SCHEMA_NAME);
    }

    public SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }
}
