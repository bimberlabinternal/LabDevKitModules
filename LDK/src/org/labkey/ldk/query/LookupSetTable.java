/*
 * Copyright (c) 2013-2015 LabKey Corporation
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
package org.labkey.ldk.query;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.SchemaTableInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.ldk.LDKService;
import org.labkey.api.ldk.table.AbstractDataDefinedTable;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.query.ValidationException;
import org.labkey.ldk.LDKModule;
import org.labkey.ldk.LDKSchema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class LookupSetTable extends AbstractDataDefinedTable
{
    private static final String CACHE_KEY = LookupSetTable.class.getName() + "||values";

    private static final String FILTER_COL = "set_name";
    private static final String VALUE_COL = "value";

    private String _keyField;

    public static String getCacheKey(Container c)
    {
        return CACHE_KEY + "||" + c.getId();
    }

    public LookupSetTable(UserSchema schema, SchemaTableInfo table, ContainerFilter cf, String setName, Map<String, Object> map)
    {
        super(schema, table, cf, FILTER_COL, VALUE_COL, setName, setName);

        setTitleColumn(VALUE_COL);

        if (map.containsKey("label"))
            setTitle((String)map.get("label"));

        if (map.containsKey("description"))
            setDescription((String) map.get("description"));

        if (map.containsKey("keyField") && map.get("keyField") != null)
            _keyField = (String)map.get("keyField");

        if (map.containsKey("titleColumn") && map.get("titleColumn") != null)
            _titleColumn = (String)map.get("titleColumn");
        else
            _titleColumn = VALUE_COL;
    }

    @Override
    public LookupSetTable init()
    {
        super.init();

        if (_keyField != null)
        {
            var keyCol = getMutableColumn(_keyField);
            if (keyCol != null)
            {
                keyCol.setKeyField(true);
                getMutableColumn("rowid").setKeyField(false);
            }
        }
        else
        {
            getMutableColumn(VALUE_COL).setKeyField(false);
            getMutableColumn("rowid").setKeyField(true);
        }

        if (_titleColumn != null)
        {
            ColumnInfo titleCol = getColumn(_titleColumn);
            if (titleCol != null)
            {
                setTitleColumn(titleCol.getName());
            }
        }
        LDKService.get().getDefaultTableCustomizer().customize(this);

        return this;
    }

    public static class TestCase extends AbstractIntegrationTest
    {
        public static final String PROJECT_NAME = "LookupSetTableTestProject";

        @BeforeClass
        public static void setup() throws Exception
        {
            doInitialSetUp(PROJECT_NAME);

            Container project = ContainerManager.getForPath(PROJECT_NAME);
            Set<Module> active = new HashSet<>(project.getActiveModules());
            active.add(ModuleLoader.getInstance().getModule(LDKModule.NAME));

            project.setActiveModules(active);

            populateTables();
        }

        private static final String TABLE1 = "TestLookupSet";
        private static final String TABLE2 = "TestLookupSet2";

        private static void populateTables() throws Exception
        {
            Container project = ContainerManager.getForPath(PROJECT_NAME);

            UserSchema us = QueryService.get().getUserSchema(getUser(), project, LookupsUserSchema.NAME);
            TableInfo lookupSets = us.getTable(LDKSchema.TABLE_LOOKUP_SETS);

            assertEquals("Extra tables present", 0, us.getTableNames().size());

            List<Map<String, Object>> rows1 = new ArrayList<>();
            Map<String, Object> row1 = new CaseInsensitiveHashMap<>();
            row1.put("setname", TABLE1);
            row1.put("label", "Test Lookup Set");
            row1.put("description", "This is the description");
            row1.put("keyField", "value");
            row1.put("titleColumn", "rowId");
            rows1.add(row1);

            Map<String, Object> row2 = new CaseInsensitiveHashMap<>();
            row2.put("setname", TABLE2);
            row2.put("label", "Test Lookup Set2");
            row2.put("description", "This is the description");
            row2.put("keyField", "value");
            row2.put("titleColumn", "rowId");
            rows1.add(row2);

            BatchValidationException bve = new BatchValidationException();
            lookupSets.getUpdateService().insertRows(getUser(), project, rows1, bve, null, null);
            if (bve.hasErrors())
            {
                throw bve;
            }

            LookupsUserSchema.clearCache();

            assertEquals("Tables not present", 2, us.getTableNames().size());
        }

        @AfterClass
        public static void cleanup()
        {
            doCleanup(PROJECT_NAME);
        }

        @Test
        public void basicTest() throws Exception
        {
            Container project = ContainerManager.getForPath(PROJECT_NAME);
            UserSchema us = QueryService.get().getUserSchema(getUser(), project, LookupsUserSchema.NAME);
            assertNotNull("Lookup table not found", us.getTable(TABLE1));
            assertNotNull("Lookup table not found", us.getTable(TABLE2));

            assertEquals("Extra tables present", 2, us.getTableNames().size());

            insertDataAndValidate(TABLE1, us, "Test Lookup Set");
            insertDataAndValidate(TABLE2, us, "Test Lookup Set2");
        }

        private void insertDataAndValidate(String tableName, UserSchema us, String tableTitle) throws Exception
        {
            Container project = ContainerManager.getForPath(PROJECT_NAME);

            TableInfo ti = us.getTable(tableName);
            assertEquals("Incorrect description", "This is the description", ti.getDescription());
            assertEquals("Incorrect keyField", 1, ti.getPkColumnNames().size());
            assertEquals("Incorrect keyField", "value", ti.getPkColumnNames().get(0));
            assertEquals("Incorrect titleColumn", "rowid", ti.getTitleColumn());
            assertEquals("Incorrect tableTitle", tableTitle, ti.getTitle());

            List<Map<String, Object>> toInsert = new ArrayList<>();
            Map<String, Object> row1 = new CaseInsensitiveHashMap<>();
            row1.put("vaLuE", "ABC");  //This uses non-canonical case, and also should pass the Table1 RegEx
            row1.put("displayValue", "DisplayValue1");
            row1.put("category", "MyCategory");
            toInsert.add(row1);

            Map<String, Object> row2 = new CaseInsensitiveHashMap<>();
            row2.put("vaLuE", "AB");
            row2.put("displayValue", "DisplayValue2");
            row2.put("category", "MyCategory");
            toInsert.add(row2);

            //expect success
            BatchValidationException errors1 = new BatchValidationException();
            ti.getUpdateService().insertRows(getUser(), project, toInsert, errors1, null, null);
            if (errors1.hasErrors())
            {
                throw errors1;
            }

            //Test duplicate keys, expect failure
            ti.getUpdateService().insertRows(getUser(), project, List.of(row1), errors1, null, null);
            if (errors1.hasErrors())
            {
                String msg = errors1.getRowErrors().get(0).getMessage();
                assertEquals("Duplicate key insert should be blocked", "There is already a record in the table " + tableName + " where value equals ABC", msg);
            }
            else
            {
                throw new Exception("Expected duplicate key insert to fail");
            }
        }

        @Test
        public void insertFailedValidationTest() throws Exception
        {
            Container project = ContainerManager.getForPath(PROJECT_NAME);
            UserSchema us = QueryService.get().getUserSchema(getUser(), project, LookupsUserSchema.NAME);

            Map<String, Object> row1 = new CaseInsensitiveHashMap<>();
            row1.put("vaLuE", "1234"); //this will fail the validation regex set up in TestLookupSet.query.xml
            row1.put("displayValue", "DisplayValue1");
            row1.put("category", "Category");

            // Expect failure
            BatchValidationException errors1 = new BatchValidationException();
            us.getTable(TABLE1).getUpdateService().insertRows(getUser(), project, List.of(row1), errors1, null, null);
            if (errors1.hasErrors())
            {
                String msg = errors1.getRowErrors().get(0).getMessage();
                assertEquals("Insert should be blocked by validator", "value: Value '1234' for field 'value' is invalid. Improper Value", msg);
            }
            else
            {
                throw new Exception("Expected insert to fail");
            }

            //this will pass
            row1.put("vaLuE", "CBA");
            errors1 = new BatchValidationException();
            us.getTable(TABLE1).getUpdateService().insertRows(getUser(), project, List.of(row1), errors1, null, null);
            if (errors1.hasErrors())
            {
                throw errors1;
            }

            //now try update:
            List<Map<String, Object>> oldKeys = new ArrayList<>();
            Map<String, Object> oldKey = new CaseInsensitiveHashMap<>();
            oldKey.put("VaLUE", row1.get("value"));
            oldKeys.add(oldKey);

            row1.put("vaLuE", "12345");  //back to failure

            try
            {
                us.getTable(TABLE1).getUpdateService().updateRows(getUser(), project, List.of(row1), oldKeys, null, null);

                //Ben's change
                throw new ValidationException("Expected update to fail because of row validators");
            }
            catch (BatchValidationException e)
            {
                assertEquals("Update should fail", "lookups:TestLookupSet: value: Value '12345' for field 'value' is invalid. Improper Value", e.getMessage());
            }
        }
    }
}


