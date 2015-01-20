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
package org.labkey.ldk.notification;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.audit.AuditLogService;
import org.labkey.api.audit.AuditTypeProvider;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerFilterable;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Selector;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.list.ListDefinition;
import org.labkey.api.exp.list.ListService;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.ldk.LDKService;
import org.labkey.api.ldk.notification.Notification;
import org.labkey.api.ldk.notification.NotificationSection;
import org.labkey.api.pipeline.PipelineService;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.study.Study;
import org.labkey.api.study.StudyService;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.ldk.LDKServiceImpl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: bbimber
 * Date: 8/4/12
 * Time: 4:02 PM
 */
public class SiteSummaryNotification implements Notification
{
    protected final static Logger log = Logger.getLogger(SiteSummaryNotification.class);
    protected final static SimpleDateFormat _dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    protected final static SimpleDateFormat _dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm");
    private static final String lastSave = "lastSave";
    private NumberFormat _pctFormat = null;

    private static final String PROP_CATEGORY = "ldk.SiteSummaryNotification";

    public String getName()
    {
        return "Site Summary Notification";
    }

    public String getCategory()
    {
        return "Admin";
    }

    public boolean isAvailable(Container c)
    {
        return c.isRoot();
    }

    public String getDescription()
    {
        return "This runs every day at 5AM and sends an email summarizing various events about the site, including usage";
    }

    public String getEmailSubject()
    {
        return "Daily Admin Alerts: " + _dateTimeFormat.format(new Date());
    }

    @Override
    public String getCronString()
    {
        return "0 0 5 * * ?";
    }

    public String getScheduleDescription()
    {
        return "daily at 5AM";
    }

    private Map<String, String> getSavedValues(Container c)
    {
        return PropertyManager.getProperties(c, PROP_CATEGORY);
    }

    private String getLastSaveString(Map<String, String> map)
    {
        Long lastSaveMills = map.containsKey(lastSave) ? Long.parseLong(map.get(lastSave)) : null;
        if (lastSaveMills == null)
            return "never";
        else
            return _dateTimeFormat.format(new Date(lastSaveMills));
    }

    private void saveValues(Container c, Map<String, String> saved, Map<String, String> newValues)
    {
        PropertyManager.PropertyMap map = PropertyManager.getWritableProperties(c, PROP_CATEGORY, true);

        Long lastSaveMills = map.containsKey(lastSave) ? Long.parseLong(map.get(lastSave)) : null;

        //if values have already been cached for this alert on this day, dont re-cache them.
        if (lastSaveMills != null)
        {
            if (DateUtils.isSameDay(new Date(), new Date(lastSaveMills)))
            {
                return;
            }
        }

        newValues.put(lastSave, String.valueOf(new Date().getTime()));
        map.putAll(newValues);

        map.save();
    }

    public String getMessage(Container c, User u)
    {
        _pctFormat = NumberFormat.getPercentInstance();
        _pctFormat.setMaximumFractionDigits(1);

        Map<String, String> saved = getSavedValues(c);
        Map<String, String> newValues = new HashMap<String, String>();

        StringBuilder msg = new StringBuilder();
        StringBuilder alerts = new StringBuilder();

        getSiteUsageStats(c, u, msg, alerts, saved, newValues);

        getTableSizeStats(c, u, msg, alerts, saved, newValues);

        getFileRootSizes(c, u, msg, alerts, saved, newValues);

        validateContainerScopedTables(c, u, msg, alerts);

        //allow registering of additional sections
        Set<NotificationSection> sections = ((LDKServiceImpl)LDKServiceImpl.get()).getSiteSummaryNotificationSections();
        for (NotificationSection ns : sections)
        {
            if (ns.isAvailable(c, u))
            {
                String m = ns.getMessage(c, u);
                if (m != null)
                    msg.append(m);
            }
        }

        if (alerts.length() > 0)
        {
            alerts.insert(0, "<b>The following alerts were generated:</b><p>");
            alerts.append("<hr>");
            msg.insert(0, alerts);
        }

        msg.insert(0, "This email contains a series of alerts designed for site admins.  It was run on: " + _dateTimeFormat.format(new Date()) + ".<p>");

        saveValues(c, saved, newValues);

        return msg.toString();
    }

    /**
     * summarize site usage in the past 7 days
     */
    private void siteUsage(Container c, User u, final StringBuilder msg, final StringBuilder alerts, Map<String, String> saved, Map<String, String> toSave)
    {
        //different behavior depending on whether audit data has migrated
        String tableName = "auditlog";
        if (AuditLogService.get().hasEventTypeMigrated("UserAuditEvent"))
        {
            AuditTypeProvider ap = AuditLogService.get().getAuditProvider("UserAuditEvent");
            tableName = ap.getDomain().getStorageTableName();
        }

        DbSchema auditSchema = DbSchema.get("audit");
        String sql = "SELECT\n" +
            (auditSchema.getSqlDialect().isSqlServer() ? "TOP 7\n" : "") +
            "cast(a.created as date) as date,\n" +
            "count(*) AS Logins,\n" +
            "count(distinct a.createdby) AS DistinctUsers\n" +
            "FROM audit." + tableName + " a\n" +
            "WHERE a.EventType = 'UserAuditEvent'\n" +
            "AND a.Comment LIKE '%logged in%'\n" +
            "AND a.created >= ?\n" +
            "GROUP BY cast(a.created as date)\n" +
            "ORDER BY cast(a.created as date)";

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, -7);
        SqlSelector ss = new SqlSelector(auditSchema, sql, cal.getTime());
        if (ss.getRowCount() > 0)
        {
            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            final SimpleDateFormat dayOfWeek = new SimpleDateFormat("E");

            msg.append("<b>Site Logins In The Past 7 Days:</b><br>\n");
            msg.append("<table border=1 style='border-collapse: collapse;'><tr style='font-weight: bold;'><td>Day of Week</td><td>Date</td><td>Logins</td><td>Distinct Users</td><td>Logins / User</td></tr>");
            ss.forEach(new Selector.ForEachBlock<ResultSet>()
            {
                @Override
                public void exec(ResultSet rs) throws SQLException
                {
                    Date date = rs.getDate("date");
                    double distinct = rs.getDouble("DistinctUsers");
                    double logins = rs.getDouble("Logins");
                    double loginsPerUser = distinct == 0 ? 0 : logins / distinct;

                    msg.append("<tr><td>" + dayOfWeek.format(date) + "</td><td>" + dateFormat.format(date) + "</td><td>" + NumberFormat.getInstance().format(logins) + "</td><td>" + NumberFormat.getInstance().format(distinct) + "</td><td>" + NumberFormat.getInstance().format(loginsPerUser) + "</td></tr>");
                }
            });

            msg.append("</table><p>\n");
        }
    }

    /**
     * we print some stats on data entry
     */
    private void dataEntryStatus(Container c, User u, final StringBuilder msg)
    {
        msg.append("<b>Data Entry Stats:</b><p>");

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, -1);
        SQLFragment sql = new SQLFragment("SELECT t.formtype, count(*) as total FROM ehr.tasks t WHERE cast(t.created as date) = '" + _dateFormat.format(cal.getTime()) + "' GROUP BY t.formtype ORDER BY t.formtype");

        UserSchema us = QueryService.get().getUserSchema(u, c, "core");
        SqlSelector ss = new SqlSelector(us.getDbSchema(), sql);

        msg.append("Number of Forms Created Yesterday: <br>\n");

        ss.forEach(new Selector.ForEachBlock<ResultSet>()
        {
            @Override
            public void exec(ResultSet rs) throws SQLException
            {
                msg.append(rs.getString("formtype") + ": " + rs.getInt("total") + "<br>\n");
            }
        });

        msg.append("<p>\n");
    }

    private void getStudySizeSummary(Container c, User u, final StringBuilder msg, final StringBuilder alerts, Map<String, String> saved, Map<String, String> toSave)
    {
        Set<? extends Study> studies = StudyService.get().getAllStudies(ContainerManager.getRoot(), u);
        String studySize = "studySize";
        if (!studies.isEmpty())
        {
            Map<String, String> newValueMap = new HashMap<String, String>();
            JSONObject oldValueMap = null;
            if (saved.containsKey(studySize))
            {
                oldValueMap = new JSONObject(saved.get(studySize));
            }

            msg.append("<br><b>Study Summary:</b><br><br>");
            msg.append("<table border=1 style='border-collapse: collapse;'><tr style='font-weight:bold;'><td>Study Name</td><td>Container Path</td><td>Number of records</td><td>Previous Value</td><td>% Change</td></tr>");

            for (Study s : studies)
            {
                UserSchema ss = QueryService.get().getUserSchema(u, s.getContainer(), "study");
                TableSelector ts = new TableSelector(ss.getTable("studydata"));
                Long count = ts.getRowCount();

                newValueMap.put(s.getEntityId(), count.toString());
                Long previousValue = null;
                if (oldValueMap != null && oldValueMap.containsKey(s.getEntityId()))
                {
                    previousValue = oldValueMap.getLong(s.getEntityId());
                }

                String pctChange = getPctChange(previousValue, count, 0.05, "The number of rows in the study " +  s.getLabel() + " has changed signficiantly since the last run on " + getLastSaveString(saved), alerts);
                msg.append("<tr><td>" + s.getLabel() + "</td><td>" + s.getContainer().getPath() + "</td><td>" + NumberFormat.getInstance().format(count) + "</td><td>" + (previousValue == null ? "" : NumberFormat.getInstance().format(previousValue)) + "</td>" + pctChange + "</tr>");
            }

            msg.append("</table>");

            if (newValueMap.size() > 0)
                toSave.put(studySize, new JSONObject(newValueMap).toString());
        }
    }

    private String getPctChange(Long oldVal, Long newVal, double threshold, String message, StringBuilder alerts)
    {
        if (oldVal == null || newVal == null || oldVal == 0)
        {
            return "<td></td>";
        }

        double pct = (newVal.doubleValue() / oldVal.doubleValue()) - 1.0;
        String style = "";
        if (Math.abs(pct) > threshold)
        {
            style = " style='background-color:yellow;'";
            alerts.append(message).append("<br>");
        }

        return "<td" + style + ">" + _pctFormat.format(pct) + "</td>";

    }

    private void getPipelineJobCount(Container c, User u, final StringBuilder msg, final StringBuilder alerts, Map<String, String> saved, Map<String, String> toSave)
    {
        TableInfo jobs = PipelineService.get().getJobsTable(u, c);
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("modified"), "-1d", CompareType.DATE_GTE);
        TableSelector ts = new TableSelector(jobs, filter, null);
        long count = ts.getRowCount();

        msg.append("Pipeline jobs created/modified in the past 24 hours: " + count + "<br>");
    }

    private void validateContainerScopedTables(Container c, User u, final StringBuilder msg, final StringBuilder alerts)
    {
        LDKServiceImpl service = (LDKServiceImpl)LDKServiceImpl.get();
        List<String> errors = service.validateContainerScopedTables(true);

        if (!errors.isEmpty())
        {
            msg.append("There were errors with container scoped tables.  This means there are inappropriate duplicate values in the underlying data<p>");
            msg.append(StringUtils.join(errors, "<br>"));
            msg.append("<br><hr>");

            alerts.append("There are errors with container scoped tables.  See below for more information<br>");
        }
    }

    private void getFileRootSizes(Container c, User u, final StringBuilder msg, final StringBuilder alerts, Map<String, String> saved, Map<String, String> toSave)
    {
        msg.append("<br><b>File Root Sizes:</b><br><br>");

        String fileRootSizes = "fileRootSizes";
        String fileRootCounts = "fileRootCounts";

        Map<String, String> newValueMap = new HashMap<String, String>();
        JSONObject oldValueMap = saved.containsKey(fileRootSizes) ? new JSONObject(saved.get(fileRootSizes)) : null;

        Map<String, String> newValueMapCounts = new HashMap<String, String>();
        JSONObject oldValueMapCounts = saved.containsKey(fileRootCounts) ? new JSONObject(saved.get(fileRootCounts)) : null;

        JSONArray ret = new JSONArray();
        ret.put(LDKService.get().getContainerSizeJson(c, u, false, true));
        for (Container child : c.getChildren())
        {
            if (!child.isWorkbook())
                ret.put(LDKService.get().getContainerSizeJson(child, u, false, true));
        }

        msg.append("<table border=1 style='border-collapse: collapse;'><tr style='font-weight:bold;'>");
        msg.append("<td>").append("Folder Path").append("</td><td>").append("File Path").append("</td><td>").append("Size").append("</td><td>").append("Previous Value").append("</td><td>").append("% Change").append("</td><td>").append("Total Files").append("</td><td>").append("Previous Value").append("</td><td>").append("% Change").append("</td></tr>");

        for (JSONObject json : ret.toJSONObjectArray())
        {
            if (json.has("fileRoots"))
            {
                JSONArray fileRoots = json.getJSONArray("fileRoots");
                for (JSONObject fr : fileRoots.toJSONObjectArray())
                {
                    //find previous value for filesize
                    String key = json.getString("path");
                    Long size = fr.containsKey("rootSizeInt") ? fr.getLong("rootSizeInt") : null;

                    newValueMap.put(key, size.toString());
                    Long previousSize = null;
                    if (oldValueMap != null && oldValueMap.containsKey(key))
                    {
                        previousSize = oldValueMap.getLong(key);
                    }

                    String formattedPreviousSize = previousSize == null ? "" : FileUtils.byteCountToDisplaySize(previousSize);
                    String pctChange = getPctChange(previousSize, size, 0.05, "The size of files under the root for the container: " +  key + " has changed signficiantly since the last run on " + getLastSaveString(saved), alerts);

                    //then do the same for file count
                    String fileCountKey = json.getString("path");
                    Long totalFiles = fr.containsKey("totalFiles") ? fr.getLong("totalFiles") : null;

                    newValueMapCounts.put(fileCountKey, totalFiles.toString());
                    Long previousCount = null;
                    if (oldValueMapCounts != null && oldValueMapCounts.containsKey(fileCountKey))
                    {
                        previousCount = oldValueMapCounts.getLong(fileCountKey);
                    }
                    String formattedPreviousCount = previousCount == null ? "" : NumberFormat.getInstance().format(previousCount);
                    String pctChange2 = getPctChange(previousCount, totalFiles, 0.05, "The total number of files under the root for the container: " +  key + " has changed signficiantly since the last run on " + getLastSaveString(saved), alerts);

                    msg.append("<tr><td>").append(json.getString("path")).append("</td><td>").append(fr.getString("rootPath")).append("</td><td>").append(fr.getString("rootSize")).append("</td><td>").append(formattedPreviousSize).append("</td>").append(pctChange).append("<td>").append(NumberFormat.getInstance().format(totalFiles)).append("</td><td>").append(formattedPreviousCount).append("</td>").append(pctChange2).append("</td></tr>");
                }
            }
        }

        msg.append("</table><br>");
        msg.append("<hr>");

        if (newValueMap.size() > 0)
            toSave.put(fileRootSizes, new JSONObject(newValueMap).toString());
        if (newValueMapCounts.size() > 0)
            toSave.put(fileRootCounts, new JSONObject(newValueMapCounts).toString());
    }

    private void getSiteUsageStats(Container c, User u, final StringBuilder msg, final StringBuilder alerts, Map<String, String> saved, Map<String, String> toSave)
    {
        msg.append("<br>The following items are designed to give a summary of recent site usage:<br><br>");

        siteUsage(c, u, msg, alerts, saved, toSave);

        msg.append("<b>Other Misc Statistics:</b><br><br>");

        getPipelineJobCount(c, u, msg, alerts, saved, toSave);

        msg.append("<hr>");
    }

    private void getTableSizeStats(Container c, User u, final StringBuilder msg, final StringBuilder alerts, final Map<String, String> saved, Map<String, String> toSave)
    {
        SQLFragment sql = null;
        if (DbScope.getLabkeyScope().getSqlDialect().isPostgreSQL())
        {
            sql = new SQLFragment("SELECT nspname as schemaName, relname as tableName, reltuples as rowcnt FROM pg_class C LEFT JOIN pg_namespace N ON (N.oid = C.relnamespace) WHERE nspname NOT IN ('pg_catalog', 'information_schema') AND relkind='r' ORDER BY reltuples DESC limit 20");
        }
        else if (DbScope.getLabkeyScope().getSqlDialect().isSqlServer())
        {
            sql = new SQLFragment("SELECT top 20 OBJECT_SCHEMA_NAME(o.id) as schemaName, o.name as tableName, i.rowcnt FROM sysindexes AS i INNER JOIN sysobjects AS o ON i.id = o.id WHERE i.indid < 2  AND OBJECTPROPERTY(o.id, 'IsMSShipped') = 0 ORDER BY i.rowcnt desc");
        }

        if (sql != null)
        {
            msg.append("<br>The following items are designed to give an overview of the amount of data stored in this site:<br><br>");
            String tableSizes = "tableSizes";

            msg.append("<br><b>The top 20 largest tables, by row count:</b><br><br>");
            msg.append("<table border=1 style='border-collapse: collapse;'><tr style='font-weight:bold;'><td>Schema</td><td>Table</td><td># of Rows</td><td>Previous Value</td><td>% Change</td></tr>");

            SqlSelector ss = new SqlSelector(DbScope.getLabkeyScope(), sql);

            final Map<String, String> newValueMap = new HashMap<>();
            final JSONObject oldValueMap = saved.containsKey(tableSizes) ? new JSONObject(saved.get(tableSizes)) : null;

            ss.forEach(new Selector.ForEachBlock<ResultSet>()
            {
                @Override
                public void exec(ResultSet object) throws SQLException
                {
                    Long total = object.getLong("rowcnt");
                    String schema = object.getString("schemaName");
                    String table = object.getString("tableName");
                    String key = schema + "." + table;

                    newValueMap.put(key, total.toString());
                    Long previousValue = null;
                    if (oldValueMap != null && oldValueMap.containsKey(key))
                    {
                        previousValue = oldValueMap.getLong(key);
                    }

                    String pctChange = getPctChange(previousValue, total, 0.05, "The number of rows in the table " +  key + " has changed signficiantly since the last run on " + getLastSaveString(saved), alerts);
                    msg.append("<tr><td>" + (schema == null ? "" : schema) + "</td><td>" + (table == null ? "" : table) + "</td><td>" + (total == null ? "" : NumberFormat.getInstance().format(total)) + "</td><td>" + (previousValue == null ? "" : NumberFormat.getInstance().format(previousValue)) + "</td>" + pctChange + "</tr>");
                }
            });

            msg.append("</table><br>");

            if (newValueMap.size() > 0)
                toSave.put(tableSizes, new JSONObject(newValueMap).toString());
        }

        getDBSize(c, u, msg, alerts, saved, toSave);
        getStudySizeSummary(c, u, msg, alerts, saved, toSave);
        getAssayRunSummary(c, u, msg, alerts, saved, toSave);
        getListSummary(c, u, msg, alerts, saved, toSave);

        msg.append("<hr>");
    }

    private void getDBSize(Container c, User u, final StringBuilder msg, final StringBuilder alerts, Map<String, String> saved, Map<String, String> toSave)
    {
        SqlSelector ss;

        String dbSizes = "dbSizes";
        final Map<String, String> newValueMap = new HashMap<String, String>();
        final JSONObject oldValueMap = saved.containsKey(dbSizes) ? new JSONObject(saved.get(dbSizes)) : null;

        if (DbScope.getLabkeyScope().getSqlDialect().isSqlServer())
        {
            ss = new SqlSelector(DbScope.getLabkeyScope(), new SQLFragment("SELECT " +
                "DB_NAME(database_id) AS DatabaseName, Name AS LogicalName, (size*8) as size\n" +  //this column holds size as 8KB pages
                "FROM sys.master_files\n" +
                "ORDER BY size desc"));

            Map<String, Object>[] maps = ss.getMapArray();
            if (maps.length > 0)
            {
                msg.append("<b>Database sizes:</b><br><br>");
                msg.append("<table border=1 style='border-collapse: collapse;'><tr style='font-weight:bold;'><td>Database</td><td>Logical Name</td><td>Size (MB)</td><td>Previous Size</td><td>% Change</td></tr>");
                for (Map<String, Object> row : maps)
                {
                    Long size = Long.parseLong(row.get("size").toString());
                    String key = row.get("LogicalName").toString();

                    newValueMap.put(key, size.toString());
                    Long previousValue = null;
                    if (oldValueMap != null && oldValueMap.containsKey(key))
                    {
                        previousValue = oldValueMap.getLong(key);
                    }

                    String pctChange = getPctChange(previousValue, size, 0.05, "The size of the database " +  key + " has changed signficiantly since the last run on " + getLastSaveString(saved), alerts);
                    msg.append("<tr><td>" + row.get("DatabaseName").toString() + "</td><td>" + row.get("LogicalName").toString() + "</td><td>" + FileUtils.byteCountToDisplaySize(size * 1000) + "</td><td>" + (previousValue == null ? "" : FileUtils.byteCountToDisplaySize(previousValue * 1000))+ "</td>" + pctChange + "</tr>");
                }
                msg.append("</table>");

                if (newValueMap.size() > 0)
                    toSave.put(dbSizes, new JSONObject(newValueMap).toString());
            }
        }
        else
        {
            ss = new SqlSelector(DbScope.getLabkeyScope(), new SQLFragment("SELECT pg_database_size(?) As size", DbScope.getLabkeyScope().getDatabaseName()));
            Map<String, Object>[] maps = ss.getMapArray();
            if (maps.length > 0)
            {
                Long size = Long.parseLong(maps[0].get("size").toString());
                msg.append("Size of LabKey DB: " + FileUtils.byteCountToDisplaySize(size) + "<br>");
            }
        }
    }

    private void getListSummary(Container c, User u, final StringBuilder msg, final StringBuilder alerts, Map<String, String> saved, Map<String, String> toSave)
    {
        msg.append("<br><b>Lists Summary:</b><br><br>");

        String listSizes = "listSizes";
        final Map<String, String> newValueMap = new HashMap<>();
        final JSONObject oldValueMap = saved.containsKey(listSizes) ? new JSONObject(saved.get(listSizes)) : null;

        DbSchema schema = DbSchema.get("list");
        int listCount = schema.getTableNames().size();
        msg.append("Total # of Lists: " + NumberFormat.getInstance().format(listCount) + "<br><br>");

        int maxLists = 20;
        msg.append("Top " + maxLists + " Lists By Size:<br>");

        Map<String, Long> listMap = new HashMap<>();
        for (String name : schema.getTableNames())
        {
            TableInfo ti = schema.getTable(name);
            if (ti == null)
            {
                //note sure why we would ever hit this.  a schema caching issue?
                log.error("Unable to find list table with table name: " + name);
                continue;
            }

            Long rowCount = new TableSelector(ti).getRowCount();
            listMap.put(name, rowCount);
        }


        List<Map.Entry<String, Long>> list = new ArrayList<>(listMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Long>>() {
            public int compare(Map.Entry<String, Long> e1, Map.Entry<String, Long> e2) {
                return e2.getValue().compareTo(e1.getValue());
            }
        });

        msg.append("<table border=1 style='border-collapse: collapse;'>");
        msg.append("<tr style='font-weight:bold;'><td>Table Name</td><td>Container Path</td><td># of Rows</td><td>Previous Value</td><td>% Change</td></tr>");

        for (Map.Entry<String, Long> entry : list.subList(0, Math.min(maxLists, list.size())))
        {
            Integer rowId = Integer.parseInt(entry.getKey().substring(1, entry.getKey().indexOf('d')));
            Container listContainer = ContainerManager.getForRowId(rowId);
            if (listContainer == null)
            {
                log.error("unable to find list container corresponding to: " + entry.getKey());
                continue;
            }

            String listName = entry.getKey().split("_")[1];
            ListDefinition ld = ListService.get().getList(listContainer, listName);

            String key = entry.getKey().toString();

            newValueMap.put(key, entry.getValue().toString());
            Long previousValue = null;
            if (oldValueMap != null && oldValueMap.containsKey(key))
            {
                previousValue = oldValueMap.getLong(key);
            }

            String pctChange = getPctChange(previousValue, entry.getValue(), 0.05, "The size of the list " +  listName + " has changed signficiantly since the last run on " + getLastSaveString(saved), alerts);

            if (ld != null)
                msg.append("<tr><td>" + ld.getName() + "</td><td>" + listContainer.getPath() + "</td><td>" + NumberFormat.getInstance().format(entry.getValue()) + "</td><td>" + (previousValue == null ? "" : NumberFormat.getInstance().format(previousValue)) + "</td>" + pctChange + "</tr>");

            if (newValueMap.size() > 0)
                toSave.put(listSizes, new JSONObject(newValueMap).toString());
        }

        msg.append("</table><br>");
    }

    private void getAssayRunSummary(Container c, User u, final StringBuilder msg, final StringBuilder alerts, Map<String, String> saved, Map<String, String> toSave)
    {
        Map<String, Map<ExpProtocol, Long[]>> providerMap = new TreeMap<>();
        String assayResultSize = "assayResultSize";

        for (ExpProtocol p : ExperimentService.get().getAllExpProtocols())
        {
            AssayProvider ap = AssayService.get().getProvider(p);
            if (ap == null)
                continue;

            Map<ExpProtocol, Long[]> protocolMap = providerMap.get(ap.getName());
            if (protocolMap == null)
                protocolMap = new TreeMap<>();

            ExpRunTable tiRun = AssayService.get().createRunTable(p, ap, u, p.getContainer());
            tiRun.setContainerFilter(ContainerFilter.EVERYTHING);
            TableSelector tsRun = new TableSelector(tiRun);

            AssayProtocolSchema schema = ap.createProtocolSchema(u, p.getContainer(), p, null);
            ContainerFilterable tiResult = schema.createDataTable();
            tiResult.setContainerFilter(ContainerFilter.EVERYTHING);
            TableSelector tsResult = new TableSelector(tiResult);

            protocolMap.put(p, new Long[]{tsRun.getRowCount(), tsResult.getRowCount()});

            providerMap.put(ap.getName(), protocolMap);
        }

        msg.append("<br><b>Assay Summary:</b><br><br>");
        msg.append("<table border=1 style='border-collapse: collapse;'><tr style='font-weight:bold;'><td>Provider</td><td>Protocol</td><td>Container Path</td><td># Runs</td><td># Results</td><td>Previous Value</td><td>% Change</td></tr>");

        Map<String, String> newValueMap = new HashMap<String, String>();
        JSONObject oldValueMap = saved.containsKey(assayResultSize) ? new JSONObject(saved.get(assayResultSize)) : null;

        for (String ap : providerMap.keySet())
        {
            Map<ExpProtocol, Long[]> protocolMap = providerMap.get(ap);
            for (ExpProtocol p : protocolMap.keySet())
            {
                Long[] totals = protocolMap.get(p);
                String key = String.valueOf(p.getRowId());

                newValueMap.put(key, totals[1].toString());
                Long previousValue = null;
                if (oldValueMap != null && oldValueMap.containsKey(key))
                {
                    previousValue = oldValueMap.getLong(key);
                }

                String pctChange = getPctChange(previousValue, totals[1], 0.05, "The number of assay results for " +  p.getName() + " has changed signficiantly since the last run on " + getLastSaveString(saved), alerts);
                msg.append("<tr><td>" + ap + "</td><td>" + p.getName() + "</td><td>" + p.getContainer().getPath() + "</td><td>" + NumberFormat.getInstance().format(totals[0]) + "</td><td>" + NumberFormat.getInstance().format(totals[1]) + "</td><td>" + (previousValue == null ? "" : NumberFormat.getInstance().format(previousValue)) + "</td>" + pctChange + "</tr>");
            }
        }
        msg.append("</table><br>");

        if (newValueMap.size() > 0)
            toSave.put(assayResultSize, new JSONObject(newValueMap).toString());
    }
}