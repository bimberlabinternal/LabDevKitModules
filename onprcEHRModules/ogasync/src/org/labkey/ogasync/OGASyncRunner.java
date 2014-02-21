package org.labkey.ogasync;

import org.apache.log4j.Logger;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.ExecutingSelector;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Selector;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: bimber
 * Date: 10/10/13
 * Time: 12:26 PM
 */
public class OGASyncRunner implements Job
{
    private static final Logger _log = Logger.getLogger(OGASyncRunner.class);
    private static final String INVESTIGATOR_EMPLOYYE_ID_NUMBER = "investigatorEmployeeNumber";
    private static final String FISCAL_AUTHORITY_EMPLOYYE_ID_NUMBER = "fiscalAuthorityNumber";
    private static final String AWARD_STATUS = "awardStatus";
    private static final String PROJECT_STATUS = "projectStatus";

    public OGASyncRunner()
    {

    }

    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        if (!OGASyncManager.get().isEnabled())
            return;

        run();
    }

    public void run()
    {
        _log.info("Starting OGA Sync");
        OGASyncManager.get().validateSettings();

        User u = OGASyncManager.get().getLabKeyUser();
        Container c = OGASyncManager.get().getLabKeyContainer();
        if (c == null)
        {
            _log.error("Invalid LabKey container");
            return;
        }

        TableInfo sourceTable = getOGAAliasesTable();
        if (sourceTable == null)
        {
            _log.error("Unable to find OGA aliases table");
            return;
        }

        DbSchema targetSchema = DbSchema.get("onprc_billing");
        if (targetSchema == null)
        {
            _log.error("Unable to find schema: onprc_billing");
            return;
        }

        try
        {
            doMergeInvestigators(u, c, sourceTable, targetSchema);
            doMergeGrants(u, c, sourceTable, targetSchema);
            doMergeGrantProjects(u, c, sourceTable, targetSchema);
            doMergeOGAAccounts(u, c, sourceTable, targetSchema);

            doMergeOtherAccounts(u, c, sourceTable, targetSchema);
        }
        catch (SQLException e)
        {
            _log.error(e.getMessage(), e);
        }

        OGASyncManager.get().setLastRun(new Date());
        _log.info("Finished OGA Sync");
    }

    private TableInfo getOGAAliasesTable()
    {
        DbScope scope = DbScope.getDbScope(OGASyncManager.get().getDataSourceName());
        if (scope == null)
            return null;

        DbSchema schema = scope.getSchema(OGASyncManager.get().getSchemaName());
        if (schema == null)
            return null;

        TableInfo ti = schema.getTable(OGASyncManager.get().getOgaQueryName());
        if (ti == null)
            return null;

        return ti;
    }

    private TableInfo getAllAliasesTable()
    {
        DbScope scope = DbScope.getDbScope(OGASyncManager.get().getDataSourceName());
        if (scope == null)
            return null;

        DbSchema schema = scope.getSchema(OGASyncManager.get().getSchemaName());
        if (schema == null)
            return null;

        TableInfo ti = schema.getTable(OGASyncManager.get().getAllQueryName());
        if (ti == null)
            return null;

        return ti;
    }

    public void doMergeGrantProjects(User u, Container c, TableInfo sourceTable, DbSchema targetSchema) throws SQLException
    {
        TableInfo targetTable = targetSchema.getTable("grantProjects");
        Map<String, String> fieldMap = new HashMap<>();
        fieldMap.put("projectNumber", "OGA_PROJECT_NUMBER");
        fieldMap.put("grantNumber", "OGA_AWARD_NUMBER");
        fieldMap.put("agencyAwardNumber", "AGENCY_AWARD_NUMBER");
        fieldMap.put(INVESTIGATOR_EMPLOYYE_ID_NUMBER, "PI_EMP_NUM");
        fieldMap.put(FISCAL_AUTHORITY_EMPLOYYE_ID_NUMBER, "PDFM_EMP_NUM");
        fieldMap.put("investigatorName", "PI");

        fieldMap.put("projectTitle", "PROJECT_TITLE");
        fieldMap.put("projectDescription", "PROJECT_DESCRIPTION");
        fieldMap.put("organization", "ORG");

        fieldMap.put("budgetStartDate", "CURRENT_BUDGET_START_DATE");
        fieldMap.put("budgetEndDate", "CURRENT_BUDGET_END_DATE");

        fieldMap.put("protocolNumber", "IACUC_NUMBER");
        fieldMap.put(PROJECT_STATUS, "PROJECT_STATUS");
        fieldMap.put("applicationType", "APPLICATION_TYPE");
        fieldMap.put("activityType", "ACTIVITY_TYPE");
        fieldMap.put("ogaProjectId", "PROJECT_ID");

        TableSelector ts = new TableSelector(sourceTable, new HashSet<>(fieldMap.values()));
        doMerge(u, c, targetTable, ts, "projectNumber", fieldMap, null, null);
    }

    public void doMergeOtherAccounts(User u, Container c, TableInfo sourceTable, DbSchema targetSchema) throws SQLException
    {
        TableInfo allAliases = getAllAliasesTable();

        TableInfo targetTable = targetSchema.getTable("aliases");
        Map<String, String> fieldMap = new HashMap<>();

        fieldMap.put("alias", "ALIAS_NAME");
        fieldMap.put("aliasEnabled", "ENABLED_FLAG");

        //fieldMap.put(FISCAL_AUTHORITY_EMPLOYYE_ID_NUMBER, "PDFM_EMP_NUM");
        fieldMap.put("category", null);

        fieldMap.put("budgetStartDate", "START_DATE_ACTIVE");
        fieldMap.put("budgetEndDate", "END_DATE_ACTIVE");

        //find existing aliases
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("category"), "Other", CompareType.NEQ_OR_NULL);
        filter.addCondition(FieldKey.fromString("container"), c.getId());
        TableSelector existingTs = new TableSelector(targetTable, PageFlowUtil.set("alias"), filter, null);
        Collection<String> existingKeys = existingTs.getCollection(String.class);

        TableSelector ts = new TableSelector(allAliases, new HashSet<>(fieldMap.values()));
        doMerge(u, c, targetTable, ts, "alias", fieldMap, "Other", existingKeys);
    }

    public void doMergeOGAAccounts(User u, Container c, TableInfo sourceTable, DbSchema targetSchema) throws SQLException
    {
        TableInfo targetTable = targetSchema.getTable("aliases");
        Map<String, String> fieldMap = new HashMap<>();

        fieldMap.put("alias", "ALIAS");
        fieldMap.put("aliasEnabled", "ALIAS_ENABLED_FLAG");

        fieldMap.put("projectNumber", "OGA_PROJECT_NUMBER");
        fieldMap.put("grantNumber", "OGA_AWARD_NUMBER");
        fieldMap.put("agencyAwardNumber", "AGENCY_AWARD_NUMBER");
        fieldMap.put(INVESTIGATOR_EMPLOYYE_ID_NUMBER, "PI_EMP_NUM");
        fieldMap.put(FISCAL_AUTHORITY_EMPLOYYE_ID_NUMBER, "PDFM_EMP_NUM");
        fieldMap.put("investigatorName", "PI");
        fieldMap.put("category", null);  //special case handling below

        fieldMap.put("budgetStartDate", "CURRENT_BUDGET_START_DATE");
        fieldMap.put("budgetEndDate", "CURRENT_BUDGET_END_DATE");

        fieldMap.put("faRate", "BURDEN_RATE");
        fieldMap.put("faSchedule", "BURDEN_SCHEDULE");

        //new
        fieldMap.put("projectTitle", "PROJECT_TITLE");
        fieldMap.put("projectDescription", "PROJECT_DESCRIPTION");
        fieldMap.put(PROJECT_STATUS, "PROJECT_STATUS");


        TableSelector ts = new TableSelector(sourceTable, new HashSet<>(fieldMap.values()));
        doMerge(u, c, targetTable, ts, "alias", fieldMap, "OGA", null);
    }

    public void doMergeGrants(User u, Container c, TableInfo sourceTable, DbSchema targetSchema) throws SQLException
    {
        TableInfo targetTable = targetSchema.getTable("grants");
        Map<String, String> fieldMap = new HashMap<>();

        fieldMap.put("grantNumber", "OGA_AWARD_NUMBER");
        //fieldMap.put("fundingAgency", "");
        //fieldMap.put("grantType", "");
        //fieldMap.put("", "AWARD_NUMBER");
        fieldMap.put("agencyAwardNumber", "AGENCY_AWARD_NUMBER");
        //fieldMap.put(INVESTIGATOR_EMPLOYYE_ID_NUMBER, "PI_EMP_NUM");
        fieldMap.put(FISCAL_AUTHORITY_EMPLOYYE_ID_NUMBER, "ADFM_EMP_NUM");
        //fieldMap.put("investigatorName", "PI");
        fieldMap.put("title", "PROJECT_TITLE");
        fieldMap.put(AWARD_STATUS, "AWARD_STATUS");
        fieldMap.put("projectDescription", "PROJECT_DESCRIPTION");
        fieldMap.put("applicationType", "APPLICATION_TYPE");
        fieldMap.put("activityType", "ACTIVITY_TYPE");
        fieldMap.put("budgetStartDate", "CURRENT_BUDGET_START_DATE");
        fieldMap.put("budgetEndDate", "CURRENT_BUDGET_END_DATE");
        fieldMap.put("awardSuffix", "AWARD_SUFFIX");
        fieldMap.put("organization", "ORG");
        fieldMap.put("protocolNum", "IACUC_NUMBER");
        fieldMap.put("ogaAwardId", "AWARD_ID");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT\n");

        String delim = "";
        for (String fieldName : fieldMap.values())
        {
            sql.append(delim);
            delim = ",\n";
            if (fieldName.equalsIgnoreCase("OGA_AWARD_NUMBER"))
            {
                sql.append(fieldName);
            }
            else
            {
                sql.append("max(").append(fieldName).append(") AS ").append(fieldName);
            }
        }

        sql.append("\nFROM  " + sourceTable.getSelectName()).append("\n");
        sql.append("GROUP BY OGA_AWARD_NUMBER");

        SqlSelector ss = new SqlSelector(sourceTable.getSchema(), new SQLFragment(sql.toString()));
        doMerge(u, c, targetTable, ss, "grantNumber", fieldMap, null, null);
    }

    public void doMergeInvestigators(User u, Container c, TableInfo sourceTable, DbSchema targetSchema)
    {


    }

    /**
     * This is not particularly efficient, but it runs in the background once per day,
     * and total rows should not be that large
     */
    public void doMerge(final User u, final Container c, final TableInfo targetTable, ExecutingSelector selector, final String selectionKey, final Map<String, String> fieldMap, final String category, final Collection<String> existingAliases) throws SQLException
    {
        _log.info("starting to merge table: " + targetTable.getName());
        ExperimentService.get().ensureTransaction();

        _log.info("truncating table");
        SqlExecutor ex = new SqlExecutor(targetTable.getSchema().getScope());
        ex.execute(new SQLFragment("DELETE FROM " + targetTable.getSelectName() + " WHERE container = ?" + (category != null ? " AND (category IS NULL OR category = '" + category + "')" : ""), c.getId()));

        try
        {
            final Map<String, Integer> totals = new HashMap<>();
            totals.put("insert", 0);
            totals.put("update", 0);
            totals.put("total", 0);
            final Date start = new Date();

            selector.forEach(new Selector.ForEachBlock<ResultSet>()
            {
                @Override
                public void exec(ResultSet rs) throws SQLException
                {
                    totals.put("total", totals.get("total") + 1);
                    if (totals.get("total") % 500 == 0)
                    {
                        _log.info("processed " + totals.get("total") + " rows in " + (((new Date()).getTime() - start.getTime()) / 1000) + " seconds");
                        start.setTime(new Date().getTime());
                    }

                    Set<String> columnNames = getColumnNames(rs);

                    //first build the row
                    Map<String, Object> row = new CaseInsensitiveHashMap<>();
                    row.put("container", c.getId());

                    for (String key : fieldMap.keySet())
                    {
                        if (fieldMap.get(key) != null && !columnNames.contains(fieldMap.get(key)))
                        {
                            _log.error("Unknown column: " + fieldMap.get(key));
                        }
                        else
                        {
                            if (key.equals(INVESTIGATOR_EMPLOYYE_ID_NUMBER))
                            {
                                row.put("investigatorId", resolveInvestigatorId(c, u, rs.getString(fieldMap.get(key))));
                            }
                            else if (targetTable.getName().equalsIgnoreCase("aliases") && "category".equals(key))
                            {
                                row.put("category", category);
                            }
                            else if (key.equals(FISCAL_AUTHORITY_EMPLOYYE_ID_NUMBER))
                            {
                                row.put("fiscalAuthority", resolveFinancialAnalystId(c, u, rs.getString(fieldMap.get(key))));
                            }
                            else if (key.equals(AWARD_STATUS) || key.equals(PROJECT_STATUS))
                            {
                                String val = (String)rs.getObject(fieldMap.get(key));
                                if ("1000".equals(val))
                                {
                                    val = "At Risk";
                                }
                                else if ("1002".equals(val))
                                {
                                    val = "On Hold";
                                }
                                else if ("1003".equals(val))
                                {
                                    val = "Preliminary";
                                }
                                else if ("1004".equals(val))
                                {
                                    val = "Uncloseable";
                                }
                                else if ("1005".equals(val))
                                {
                                    val = "Destroyed";
                                }
                                else if ("1006".equals(val))
                                {
                                    val = "Partial Setup";
                                }
                                else if ("1007".equals(val))
                                {
                                    val = "Preaward";
                                }
                                else if ("1008".equals(val))
                                {
                                    val = "No Cost Ext";
                                }

                                row.put(key, val);
                            }
                            else
                            {
                                row.put(key, rs.getObject(fieldMap.get(key)));
                            }
                        }
                    }

                    assert targetTable.getPkColumns().size() == 1 : "Incorrect number of PK columns for table: " + targetTable.getName();

                    if (existingAliases != null && existingAliases.contains(rs.getString(fieldMap.get("alias"))))
                    {
                        //TODO: consider update
                    }
                    else {
                        row.put("modifiedby", u.getUserId());
                        row.put("modified", new Date());
    
                        row.put("createdby", u.getUserId());
                        row.put("created", new Date());
                        Table.insert(u, targetTable, row);

                        totals.put("insert", totals.get("insert") + 1);
                    }
                }
            });

            ExperimentService.get().commitTransaction();
            _log.info("finished merging table: " + targetTable.getName() + ".  total inserts: " + totals.get("insert") + ".  total updates: " + totals.get("update"));
        }
        finally
        {
            ExperimentService.get().closeTransaction();
        }
    }

    private Set<String> getColumnNames(ResultSet rs) throws SQLException
    {
        Set<String> columnNames = new HashSet<>();

        ResultSetMetaData rsMetaData = rs.getMetaData();
        int numberOfColumns = rsMetaData.getColumnCount();

        for (int i = 1; i < numberOfColumns + 1; i++) {
            columnNames.add(rsMetaData.getColumnName(i));
        }

        return columnNames;
    }

    private Map<String, Integer> _investigatorMap = null;

    private Integer resolveInvestigatorId(Container c, User u, String employeeId)
    {
        if (_investigatorMap == null)
        {
            TableInfo ti = QueryService.get().getUserSchema(u, c, "onprc_ehr").getTable("investigators");
            TableSelector ts = new TableSelector(ti, PageFlowUtil.set("rowid", "employeeid"));
            _investigatorMap = new HashMap<>();
            Collection<Map<String, Object>> rows = ts.getMapCollection();
            for (Map<String, Object> row : rows)
            {
                if (row.containsKey("employeeid") && row.get("employeeid") != null)
                    _investigatorMap.put((String)row.get("employeeid"), (Integer)row.get("rowid"));
            }
        }

        return _investigatorMap.get(employeeId);
    }

    private Map<String, Integer> _financialAnalystMap = null;

    private Integer resolveFinancialAnalystId(Container c, User u, String employeeId)
    {
        if (_financialAnalystMap == null)
        {
            TableInfo ti = QueryService.get().getUserSchema(u, c, "onprc_billing").getTable("fiscalAuthorities");
            TableSelector ts = new TableSelector(ti, PageFlowUtil.set("rowid", "employeeid"));
            _financialAnalystMap = new HashMap<>();
            Collection<Map<String, Object>> rows = ts.getMapCollection();
            for (Map<String, Object> row : rows)
            {
                if (row.containsKey("employeeid") && row.get("employeeid") != null)
                    _financialAnalystMap.put((String)row.get("employeeid"), (Integer)row.get("rowid"));
            }
        }

        return _financialAnalystMap.get(employeeId);
    }
}