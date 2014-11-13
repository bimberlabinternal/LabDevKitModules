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
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.labkey.api.data.AbstractTableInfo;
import org.labkey.api.data.ButtonBarConfig;
import org.labkey.api.data.ButtonConfig;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SchemaTableInfo;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.UserDefinedButtonConfig;
import org.labkey.api.gwt.client.AuditBehaviorType;
import org.labkey.api.gwt.client.util.StringUtils;
import org.labkey.api.ldk.LDKService;
import org.labkey.api.ldk.table.ButtonConfigFactory;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.UserSchema;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.template.ClientDependency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created with IntelliJ IDEA.
 * User: bimber
 * Date: 9/27/12
 * Time: 3:09 PM
 */
public class DefaultTableCustomizer implements TableCustomizer
{
    private static final String MORE_ACTIONS = "More Actions";

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
            if (!AbstractTableInfo.LINK_DISABLER_ACTION_URL.equals(ti.getImportDataURL(ti.getUserSchema().getContainer())))
                ti.setImportURL(DetailsURL.fromString("/query/importData.view?schemaName=" + schemaName + "&query.queryName=" + queryName + "&keyField=" + keyField));

            ti.setInsertURL(AbstractTableInfo.LINK_DISABLER);

            if (!AbstractTableInfo.LINK_DISABLER.equals(ti.getUpdateURL(null, ti.getUserSchema().getContainer())))
                ti.setUpdateURL(DetailsURL.fromString("/ldk/manageRecord.view?schemaName=" + schemaName + "&query.queryName=" + queryName + "&keyField=" + keyField + "&key=${" + keyField + "}"));

            ti.setDetailsURL(DetailsURL.fromString("/query/recordDetails.view?schemaName=" + schemaName + "&query.queryName=" + queryName + "&keyField=" + keyField + "&key=${" + keyField + "}"));
        }

        ti.setAuditBehavior(_auditMode);

        List<ButtonConfigFactory> buttons = LDKService.get().getQueryButtons(ti);
        customizeButtonBar(ti, buttons);

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

        if (!LDKService.get().isNaturalizeInstalled())
        {
            _log.warn("Attempt to add natural sorting to a column when naturalize() has not been installed on this server");
            return;
        }

        //first add the sort col
        String name = colName + "_sortValue";
        ColumnInfo sortCol = ti.getColumn(name);
        if (sortCol != null)
        {
            // We need to swap out the placeholder version
            ti.removeColumn(sortCol);
        }

        if (!ti.getSqlDialect().isPostgreSQL() && !ti.getSqlDialect().isSqlServer())
        {
            throw new UnsupportedOperationException("naturalize() is only supported on Postgres and SqlServer");
        }

        SQLFragment sql = new SQLFragment("ldk.naturalize(" + col.getValueSql(ExprColumn.STR_TABLE_ALIAS) + ")");
        sortCol = new ExprColumn(ti, name, sql, JdbcType.VARCHAR, col);
        sortCol.setHidden(true);
        sortCol.setLabel(col.getLabel() + " - Sort Field");
        ti.addColumn(sortCol);

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

    public static void appendCalculatedDateColumns(AbstractTableInfo ti, @Nullable String dateColName, @Nullable String enddateColName)
    {
        if (enddateColName != null)
        {
            appendEnddate(ti, enddateColName);
        }

        if (dateColName != null)
        {
            appendDateOnly(ti, dateColName);
        }
    }

    private static void appendEnddate(AbstractTableInfo ti, String sourceColName)
    {
        ColumnInfo sourceCol = ti.getColumn(sourceColName);
        if (sourceCol == null)
        {
            _log.error("Unable to find column: " + sourceColName + " on table " + ti.getSelectName());
            return;
        }

        String name = sourceCol.getName();
        if (ti.getColumn(name + "Coalesced") == null)
        {
            SQLFragment sql = new SQLFragment("CAST(COALESCE(" + ExprColumn.STR_TABLE_ALIAS + "." + sourceCol.getSelectName() + ", {fn curdate()}) as date)");
            ExprColumn col = new ExprColumn(ti, name + "Coalesced", sql, JdbcType.DATE);
            col.setCalculated(true);
            col.setUserEditable(false);
            col.setHidden(true);
            col.setLabel(col.getLabel() + ", Coalesced");

            if (sourceCol.getFormat() != null)
                col.setFormat(sourceCol.getFormat());

            ti.addColumn(col);
        }

        if (ti.getColumn(name + "timeCoalesced") == null)
        {
            SQLFragment sql = new SQLFragment("COALESCE(" + ExprColumn.STR_TABLE_ALIAS + "." + sourceCol.getSelectName() + ", {fn now()})");
            ExprColumn col = new ExprColumn(ti, name + "timeCoalesced", sql, JdbcType.DATE);
            col.setCalculated(true);
            col.setUserEditable(false);
            col.setHidden(true);
            col.setLabel(col.getLabel() + " - DateTime, Coalesced");
            col.setFormat("yyyy-MM-dd HH:mm");

            ti.addColumn(col);
        }
    }

    private static void appendDateOnly(AbstractTableInfo ti, String sourceColName)
    {
        ColumnInfo sourceCol = ti.getColumn(sourceColName);
        if (sourceCol == null)
        {
            _log.error("Unable to find column: " + sourceColName + " on table " + ti.getName());
            return;
        }

        String name = sourceCol.getName().equals("date") ? "dateOnly" : sourceCol.getName() + "DatePart";
        if (ti.getColumn(name) == null)
        {
            SQLFragment sql = new SQLFragment(ti.getSqlDialect().getDateTimeToDateCast(ExprColumn.STR_TABLE_ALIAS + "." + sourceCol.getSelectName()));
            ExprColumn col = new ExprColumn(ti, name, sql, JdbcType.DATE);
            col.setCalculated(true);
            col.setUserEditable(false);
            col.setHidden(true);
            col.setLabel(col.getLabel() + " - Date Only");
            ti.addColumn(col);
        }
    }

    public static void customizeButtonBar(AbstractTableInfo ti, List<ButtonConfigFactory> buttons)
    {
        UserSchema us = ti.getUserSchema();
        if (us == null)
            return;

        ButtonBarConfig cfg = ti.getButtonBarConfig();
        if (cfg == null)
        {
            cfg = new ButtonBarConfig(new JSONObject());
            cfg.setIncludeStandardButtons(true);
        }

        //ensure client dependencies
        Set<String> scripts = new LinkedHashSet<String>();
        String[] existingScripts = cfg.getScriptIncludes();
        if (existingScripts != null)
        {
            for (String s : existingScripts)
            {
                scripts.add(s);
            }
        }

        configureMoreActionsBtn(ti, buttons, cfg, scripts);

        cfg.setScriptIncludes(scripts.toArray(new String[scripts.size()]));
        cfg.setAlwaysShowRecordSelectors(true);

        ti.setButtonBarConfig(cfg);
    }

    private static void configureMoreActionsBtn(TableInfo ti, List<ButtonConfigFactory> buttons, ButtonBarConfig cfg, Set<String> scripts)
    {
        if (buttons == null || buttons.isEmpty())
        {
            return;
        }

        List<ButtonConfig> existingBtns = cfg.getItems();
        UserDefinedButtonConfig moreActionsBtn = null;
        if (existingBtns != null)
        {
            for (ButtonConfig btn : existingBtns)
            {
                if (btn instanceof UserDefinedButtonConfig)
                {
                    UserDefinedButtonConfig ub = (UserDefinedButtonConfig)btn;
                    if (MORE_ACTIONS.equals(ub.getText()))
                    {
                        moreActionsBtn = ub;
                        break;
                    }
                }
            }
        }

        if (moreActionsBtn == null)
        {
            //abort if there are no custom buttons
            if (buttons.size() == 0)
                return;

            moreActionsBtn = new UserDefinedButtonConfig();
            moreActionsBtn.setText(MORE_ACTIONS);
            moreActionsBtn.setInsertPosition(-1);
            existingBtns.add(moreActionsBtn);
            cfg.setItems(existingBtns);
        }

        List<NavTree> menuItems = new ArrayList<NavTree>();
        if (moreActionsBtn.getMenuItems() != null)
            menuItems.addAll(moreActionsBtn.getMenuItems());

        //create map of existing item names
        Map<String, NavTree> btnNameMap = new HashMap<String, NavTree>();
        for (NavTree item : menuItems)
        {
            btnNameMap.put(item.getText(), item);
        }

        for (ButtonConfigFactory fact : buttons)
        {
            NavTree newButton = fact.create(ti);
            if (!btnNameMap.containsKey(newButton.getText()))
            {
                btnNameMap.put(newButton.getText(), newButton);
                menuItems.add(newButton);

                for (ClientDependency cd : fact.getClientDependencies(ti.getUserSchema().getContainer(), ti.getUserSchema().getUser()))
                {
                    scripts.add(cd.getScriptString());
                }
            }
        }

        moreActionsBtn.setMenuItems(menuItems);
    }
}
