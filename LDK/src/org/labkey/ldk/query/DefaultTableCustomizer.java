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
package org.labkey.ldk.query;

import org.apache.log4j.Logger;
import org.labkey.api.data.AbstractTableInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SchemaTableInfo;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;
import org.labkey.api.gwt.client.AuditBehaviorType;
import org.labkey.api.gwt.client.util.StringUtils;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;

import java.util.Arrays;
import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: bimber
 * Date: 9/27/12
 * Time: 3:09 PM
 */
public class DefaultTableCustomizer implements TableCustomizer
{
    private static final Logger _log = Logger.getLogger(TableCustomizer.class);
    private boolean _disableFacetingForNumericCols = true;
    private AuditBehaviorType _auditMode = AuditBehaviorType.DETAILED;

    public DefaultTableCustomizer()
    {

    }

    public void customize(TableInfo table)
    {
        if (table instanceof SchemaTableInfo)
            _log.error("Table customizer is being passed a SchemaTableInfo for: " + table.getPublicSchemaName() + "." + table.getPublicName());
        else if (table instanceof AbstractTableInfo)
            customizeAbstractTableInfo((AbstractTableInfo)table);
    }

    private void customizeAbstractTableInfo(AbstractTableInfo ti)
    {
        String schemaName = ti.getUserSchema().getSchemaName();
        assert schemaName != null;

        String queryName = ti.getPublicName();
        assert queryName != null;

        List<String> keyFields = ti.getPkColumnNames();
        assert keyFields.size() > 0 : "No key fields found for the table: " + ti.getPublicSchemaName() + "." + ti.getPublicName();
        if (keyFields.size() != 1)
        {
            _log.warn("Table: " + schemaName + "." + queryName + " has more than 1 PK: " + StringUtils.join(keyFields, ";"));
            return;
        }

        if (schemaName != null && queryName != null && keyFields.size() > 0)
        {
            String keyField = keyFields.get(0);
            ti.setImportURL(DetailsURL.fromString("/query/importData.view?schemaName=" + schemaName + "&query.queryName=" + queryName + "&keyField=" + keyField));
            ti.setInsertURL(AbstractTableInfo.LINK_DISABLER);
            ti.setUpdateURL(DetailsURL.fromString("/ldk/manageRecord.view?schemaName=" + schemaName + "&query.queryName=" + queryName + "&keyField=" + keyField + "&key=${" + keyField + "}"));
            ti.setDetailsURL(DetailsURL.fromString("/query/recordDetails.view?schemaName=" + schemaName + "&query.queryName=" + queryName + "&keyField=" + keyField + "&key=${" + keyField + "}"));
        }

        ti.setAuditBehavior(_auditMode);

        //customize builtin columns
        BuiltInColumnsCustomizer colCustomizer = new BuiltInColumnsCustomizer();
        colCustomizer.setDisableFacetingForNumericCols(_disableFacetingForNumericCols);
        colCustomizer.customize(ti);
    }

    public static void applyNaturalSort(AbstractTableInfo ti, String colName)
    {
        ColumnInfo col = ti.getColumn(FieldKey.fromString(colName));
        if (col == null)
            return;

        //only attempt to do this for strings
        if (!String.class.equals(col.getJavaClass()))
            throw new IllegalArgumentException("Natural sorting only supported on string columns");

        //first add the sort col
        String name = colName + "_sortValue";
        ColumnInfo sortCol = ti.getColumn(name);
        if (sortCol == null)
        {
            if (!ti.getSqlDialect().isPostgreSQL() && !ti.getSqlDialect().isSqlServer())
            {
                throw new UnsupportedOperationException("naturalize() is only supported on Postgres and SqlServer");
            }

            SQLFragment sql = new SQLFragment("ldk.naturalize(" + ExprColumn.STR_TABLE_ALIAS + "." + col.getSelectName() + ")");
            sortCol = new ExprColumn(ti, name, sql, JdbcType.VARCHAR, col);
            sortCol.setHidden(true);
            sortCol.setLabel(col.getLabel() + " - Sort Field");
            ti.addColumn(sortCol);
        }

        col.setSortFieldKeys(Arrays.asList(sortCol.getFieldKey()));
    }

    public void setDisableFacetingForNumericCols(boolean disableFacetingForNumericCols)
    {
        _disableFacetingForNumericCols = disableFacetingForNumericCols;
    }

    public AuditBehaviorType getAuditMode()
    {
        return _auditMode;
    }

    public void setAuditMode(AuditBehaviorType auditMode)
    {
        _auditMode = auditMode;
    }

    public static void appendEnddateColumns(AbstractTableInfo ti)
    {
        appendEnddate(ti);
        appendDateOnly(ti);
    }

    private static void appendEnddate(AbstractTableInfo ti)
    {
        ColumnInfo enddate = ti.getColumn("enddate");
        if (enddate != null && ti.getColumn("enddateCoalesced") == null)
        {
            SQLFragment sql = new SQLFragment("CAST(COALESCE(" + ExprColumn.STR_TABLE_ALIAS + "." + enddate.getSelectName() + ", {fn curdate()}) as date)");
            ExprColumn col = new ExprColumn(ti, "enddateCoalesced", sql, JdbcType.DATE);
            col.setCalculated(true);
            col.setUserEditable(false);
            col.setHidden(true);
            col.setLabel("Enddate, Coalesced");

            if (enddate.getFormat() != null)
                col.setFormat(enddate.getFormat());

            ti.addColumn(col);
        }

        if (enddate != null && ti.getColumn("enddatetimeCoalesced") == null)
        {
            SQLFragment sql = new SQLFragment("COALESCE(" + ExprColumn.STR_TABLE_ALIAS + "." + enddate.getSelectName() + ", {fn now()})");
            ExprColumn col = new ExprColumn(ti, "enddatetimeCoalesced", sql, JdbcType.DATE);
            col.setCalculated(true);
            col.setUserEditable(false);
            col.setHidden(true);
            col.setLabel("End Time, Coalesced");
            col.setFormat("yyyy-MM-dd HH:mm");

            ti.addColumn(col);
        }
    }

    private static void appendDateOnly(AbstractTableInfo ti)
    {
        ColumnInfo date = ti.getColumn("date");
        if (date != null && ti.getColumn("dateOnly") == null)
        {
            SQLFragment sql = new SQLFragment(ti.getSqlDialect().getDateTimeToDateCast(ExprColumn.STR_TABLE_ALIAS + "." + date.getSelectName()));
            ExprColumn col = new ExprColumn(ti, "dateOnly", sql, JdbcType.DATE);
            col.setCalculated(true);
            col.setUserEditable(false);
            col.setHidden(true);
            col.setLabel("Date Only");
            ti.addColumn(col);
        }
    }
}
