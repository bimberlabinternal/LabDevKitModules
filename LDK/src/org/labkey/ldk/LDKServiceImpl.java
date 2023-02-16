package org.labkey.ldk;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.AbstractTableInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Selector;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;
import org.labkey.api.files.FileContentService;
import org.labkey.api.ldk.LDKService;
import org.labkey.api.ldk.notification.NotificationSection;
import org.labkey.api.ldk.table.ButtonConfigFactory;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.ValidEmail;
import org.labkey.ldk.query.BuiltInColumnsCustomizer;
import org.labkey.ldk.query.ColumnOrderCustomizer;
import org.labkey.ldk.query.DefaultTableCustomizer;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: bimber
 * Date: 11/4/12
 * Time: 3:47 PM
 */
public class LDKServiceImpl extends LDKService
{
    private static final Logger _log = LogManager.getLogger(LDKServiceImpl.class);
    private final Set<NotificationSection> _summaryNotificationSections = new HashSet<>();
    private final List<List<String>> _containerScopedTables = new ArrayList<>();
    private Boolean _isNaturalizeInstalled = null;
    private final Map<String, Map<String, List<ButtonConfigFactory>>> _queryButtons = new CaseInsensitiveHashMap<Map<String, List<ButtonConfigFactory>>>();
    private static final String BACKGROUND_USER_PROPNAME = "BackgroundAdminUser";

    public LDKServiceImpl()
    {

    }

    @Override
    public TableCustomizer getDefaultTableCustomizer()
    {
        return new DefaultTableCustomizer();
    }

    @Override
    public TableCustomizer getDefaultTableCustomizer(MultiValuedMap<String, String> props)
    {
        return new DefaultTableCustomizer(props);
    }

    @Override
    public TableCustomizer getBuiltInColumnsCustomizer(boolean disableFacetingForNumericCols)
    {
        BuiltInColumnsCustomizer ret = new BuiltInColumnsCustomizer();
        ret.setDisableFacetingForNumericCols(disableFacetingForNumericCols);

        return ret;
    }

    @Override
    public TableCustomizer getColumnsOrderCustomizer()
    {
        return new ColumnOrderCustomizer();
    }

    @Override
    public Map<String, Object> getContainerSizeJson(Container c, User u, boolean includeAllRootTypes, boolean includeFileCount)
    {
        FileContentService svc = FileContentService.get();
        Map<String, Object> json = c.toJSON(u);

        JSONArray fileRoots = new JSONArray();

        //primary root
        File root = svc.getFileRoot(c);
        if (root != null && root.exists())
        {
            fileRoots.put(getJSONForRoot(root, "root", includeFileCount));
        }

        //append children
        if (includeAllRootTypes)
        {
            Set<File> paths = new HashSet<File>();
            for (FileContentService.ContentType type : FileContentService.ContentType.values())
            {
                File fileRoot = svc.getFileRoot(c, type);
                if (fileRoot != null && fileRoot.exists())
                {
                    if (paths.contains(fileRoot))
                        continue;

                    paths.add(fileRoot);
                    fileRoots.put(getJSONForRoot(fileRoot, type.name(), includeFileCount));
                }
            }
        }

        json.put("fileRoots", fileRoots);

        return json;
    }

    private JSONObject getJSONForRoot(File fileRoot, String name, boolean includeFileCount)
    {
        try
        {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("rootPath", fileRoot.getPath());
            long size = FileUtils.sizeOfDirectory(fileRoot);
            obj.put("rootSizeInt", size);
            obj.put("rootSize", FileUtils.byteCountToDisplaySize(size));
            if (includeFileCount)
                obj.put("totalFiles", getFileCount(fileRoot));

            return obj;
        }
        catch (IllegalArgumentException e)
        {
            //NOTE: this has been hit when there are bad symlinks under a file root
            _log.error(e.getMessage(), e);
        }

        return new JSONObject();
    }

    private int getFileCount(File file)
    {
        if (!file.isDirectory())
        {
            return 1;
        }

        int count = 0;
        File[] files = file.listFiles();
        if (files != null)
        {
            for (File f: files)
            {
                count += getFileCount(f);
            }
        }
        return count;
    }

    @Override
    public void applyNaturalSort(AbstractTableInfo ti, String colName)
    {
        DefaultTableCustomizer.applyNaturalSort(ti, colName);
    }

    @Override
    public void appendCalculatedDateColumns(AbstractTableInfo ti, String dateColName, String enddateColName)
    {
        DefaultTableCustomizer.appendCalculatedDateColumns(ti, dateColName, enddateColName);
    }

    @Override
    public void registerSiteSummaryNotification(NotificationSection ns)
    {
        _summaryNotificationSections.add(ns);
    }

    public Set<NotificationSection> getSiteSummaryNotificationSections()
    {
        return Collections.unmodifiableSet(_summaryNotificationSections);
    }

    @Override
    public boolean isNaturalizeInstalled()
    {
        if (_isNaturalizeInstalled != null)
        {
            return _isNaturalizeInstalled;
        }
        else
        {
            try
            {
                // Attempt to use the ldk.naturalize function. If this succeeds, we'll skip the install step.
                SqlExecutor executor = new SqlExecutor(LDKSchema.getInstance().getSchema());
                executor.setLogLevel(Level.OFF);
                executor.execute("SELECT ldk.naturalize('Foo') FROM (SELECT 1 AS G) x");
                _isNaturalizeInstalled = true;
            }
            catch (Exception e)
            {
                _isNaturalizeInstalled = false;
            }

            return _isNaturalizeInstalled;
        }
    }

    @Override
    public void registerContainerScopedTable(String dbSchemaName, String tableName, String pseudoPk)
    {
        _containerScopedTables.add(Arrays.asList(dbSchemaName, tableName, pseudoPk));
    }

    public List<String> validateContainerScopedTables(boolean onlyReportErrors)
    {
        final List<String> messages = new ArrayList<>();

        for (List<String> values : _containerScopedTables)
        {
            DbSchema schema = DbSchema.get(values.get(0));
            if (schema == null)
            {
                messages.add("Unknown schema: " + values.get(0));
                continue;
            }

            TableInfo ti = schema.getTable(values.get(1));
            if (ti == null)
            {
                messages.add("Unknown table: " + values.get(0) + "." + values.get(1));
                continue;
            }

            final ColumnInfo pseudoPk = ti.getColumn(values.get(2));
            if (pseudoPk == null)
            {
                messages.add("Unable to find column " + values.get(2) + " in table " + values.get(0) + "." + values.get(1));
                continue;
            }

            // group data based on pseudoPK and effective container (ie. workbooks go with parent) and return duplicates
            SQLFragment sql = new SQLFragment("SELECT ").append(pseudoPk.getValueSql("t")).append(" as keyField, count(*) as total FROM ").append(ti.getFromSQL("t")).append(
            " LEFT JOIN core.containers c ON t.container = c.entityid " +
            " GROUP BY ").append(pseudoPk.getValueSql("t")).append(", CASE WHEN c.type = 'workbook' THEN c.parent ELSE c.entityid END " +
            " HAVING count(*) > 1");

            SqlSelector ss = new SqlSelector(schema.getScope(), sql);
            if (ss.exists())
            {
                messages.add("ERROR: duplicates found in: " + values.get(0) + "." + values.get(1));
                ss.forEach(new Selector.ForEachBlock<ResultSet>()
                {
                    @Override
                    public void exec(ResultSet rs) throws SQLException
                    {
                        messages.add(pseudoPk.getName() + ": " + rs.getString("keyField") + ", total: " + rs.getInt("total"));
                    }
                });
            }
            else
            {
                if (!onlyReportErrors)
                    messages.add("No duplicates: " + values.get(0) + "." + values.get(1));
            }
        }

        return messages;
    }

    @Override
    public void logPerfMetric(Container c, User u, String metricName, String comment, Double value)
    {
        PerfMetricModel model = new PerfMetricModel();
        model.setMetricName(metricName);
        model.setStringValue1(comment);
        model.setNumericValue1(value);

        logPerfMetric(c, u, model);
    }

    public void logPerfMetric(Container c, User u, PerfMetricModel model)
    {
        Map<String, Object> result = new HashMap<>();
        TableInfo t = LDKSchema.getInstance().getSchema().getTable(LDKSchema.TABLE_METRICS);

        if(model.getMetricName() == null)
        {
            throw new IllegalArgumentException("No metric name provided");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("container", c.getId());
        map.put("created", new Date());
        map.put("createdby", u.getUserId());

        map.put("category", model.getCategory());
        map.put("metric_name", model.getMetricName());
        map.put("floatvalue1", model.getNumericValue1());
        map.put("floatvalue2", model.getNumericValue2());
        map.put("floatvalue3", model.getNumericValue3());
        map.put("stringvalue1", model.getStringValue1());
        map.put("stringvalue2", model.getStringValue2());
        map.put("stringvalue3", model.getStringValue3());

        map.put("referrerURL", model.getReferrerURL());
        map.put("browser", model.getBrowser());
        map.put("platform", model.getPlatform());

        Table.insert(u, t, map);
    }

    @Override
    public void registerQueryButton(ButtonConfigFactory btn, String schema, String query)
    {
        Map<String, List<ButtonConfigFactory>> schemaMap = _queryButtons.get(schema);
        if (schemaMap == null)
            schemaMap = new CaseInsensitiveHashMap<List<ButtonConfigFactory>>();

        List<ButtonConfigFactory> list = schemaMap.get(query);
        if (list == null)
            list = new ArrayList<ButtonConfigFactory>();

        list.add(btn);

        schemaMap.put(query, list);
        _queryButtons.put(schema, schemaMap);
    }

    @Override
    public List<ButtonConfigFactory> getQueryButtons(TableInfo ti)
    {
        List<ButtonConfigFactory> buttons = new ArrayList<ButtonConfigFactory>();

        Map<String, List<ButtonConfigFactory>> factories = _queryButtons.get(ti.getPublicSchemaName());
        if (factories == null)
            return buttons;

        List<ButtonConfigFactory> list = factories.get(ti.getPublicName());
        if (list == null)
            return  buttons;

        for (ButtonConfigFactory fact : list)
        {
            if (fact.isAvailable(ti))
                buttons.add(fact);
        }

        return Collections.unmodifiableList(buttons);
    }

    @Override
    public void customizeButtonBar(AbstractTableInfo ti, List<ButtonConfigFactory> buttons)
    {
        DefaultTableCustomizer.customizeButtonBar(ti, buttons);
    }

    public static class PerfMetricModel
    {
        String _category;
        String _metricName;
        String _stringValue1;
        String _stringValue2;
        String _stringValue3;
        Double _numericValue1;
        Double _numericValue2;
        Double _numericValue3;
        String _referrerURL;
        String _platform;
        String _browser;

        public PerfMetricModel()
        {

        }

        public String getCategory()
        {
            return _category;
        }

        public void setCategory(String category)
        {
            _category = category;
        }

        public String getMetricName()
        {
            return _metricName;
        }

        public void setMetricName(String metricName)
        {
            _metricName = metricName;
        }

        public String getStringValue1()
        {
            return _stringValue1;
        }

        public void setStringValue1(String stringValue1)
        {
            _stringValue1 = stringValue1;
        }

        public String getStringValue2()
        {
            return _stringValue2;
        }

        public void setStringValue2(String stringValue2)
        {
            _stringValue2 = stringValue2;
        }

        public String getStringValue3()
        {
            return _stringValue3;
        }

        public void setStringValue3(String stringValue3)
        {
            _stringValue3 = stringValue3;
        }

        public Double getNumericValue1()
        {
            return _numericValue1;
        }

        public void setNumericValue1(Double numericValue1)
        {
            _numericValue1 = numericValue1;
        }

        public Double getNumericValue2()
        {
            return _numericValue2;
        }

        public void setNumericValue2(Double numericValue2)
        {
            _numericValue2 = numericValue2;
        }

        public Double getNumericValue3()
        {
            return _numericValue3;
        }

        public void setNumericValue3(Double numericValue3)
        {
            _numericValue3 = numericValue3;
        }

        public String getReferrerURL()
        {
            return _referrerURL;
        }

        public void setReferrerURL(String referrerURL)
        {
            _referrerURL = referrerURL;
        }

        public String getPlatform()
        {
            return _platform;
        }

        public void setPlatform(String platform)
        {
            _platform = platform;
        }

        public String getBrowser()
        {
            return _browser;
        }

        public void setBrowser(String browser)
        {
            _browser = browser;
        }
    }

    @Override
    public User getBackgroundAdminUser()
    {
        Module m = ModuleLoader.getInstance().getModule(LDKModule.NAME);
        ModuleProperty mp = m.getModuleProperties().get(BACKGROUND_USER_PROPNAME);
        String username = StringUtils.trimToNull(mp.getValueContainerSpecific(ContainerManager.getRoot()));
        if (username != null)
        {
            User u = UserManager.getUserByDisplayName(username);
            if (u == null)
            {
                try
                {
                    u = UserManager.getUser(new ValidEmail(username));
                }
                catch (ValidEmail.InvalidEmailException e)
                {
                    //ignore

                }
            }

            return u;
        }

        return null;
    }
}
