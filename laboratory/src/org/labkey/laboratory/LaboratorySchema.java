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
package org.labkey.laboratory;

import org.labkey.api.data.DbSchema;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.dialect.SqlDialect;

/**
 * User: bimber
 * Date: 9/25/12
 * Time: 11:32 AM
 */
public class LaboratorySchema
{
    private static final LaboratorySchema _instance = new LaboratorySchema();
    public static final String TABLE_SAMPLES = "samples";
    public static final String TABLE_DNA_OLIGOS = "dna_oligos";
    public static final String TABLE_PEPTIDES = "peptides";
    public static final String TABLE_ANTIBODIES = "antibodies";
    public static final String TABLE_SUBJECTS = "subjects";
    public static final String TABLE_FREEZERS = "freezers";
    public static final String TABLE_MAJOR_EVENTS = "major_events";
    public static final String TABLE_SAMPLE_TYPE = "sample_type";
    public static final String TABLE_WORKBOOKS = "workbooks";
    public static final String TABLE_WORKBOOK_TAGS = "workbook_tags";
    public static final String TABLE_ASSAY_RUN_TEMPLATES = "assay_run_templates";

    public static LaboratorySchema getInstance()
    {
        return _instance;
    }

    private LaboratorySchema()
    {
        // private constructor to prevent instantiation from
        // outside this class
    }

    public DbSchema getSchema()
    {
        return DbSchema.get(LaboratoryModule.SCHEMA_NAME);
    }

    public TableInfo getTable(String name)
    {
        return getSchema().getTable(name);
    }

    public SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }
}
