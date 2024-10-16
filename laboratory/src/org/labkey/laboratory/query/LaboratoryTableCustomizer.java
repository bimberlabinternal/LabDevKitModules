package org.labkey.laboratory.query;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.AbstractTableInfo;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.MutableColumnInfo;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableCustomizer;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.WrappedColumn;
import org.labkey.api.gwt.client.FacetingBehaviorType;
import org.labkey.api.laboratory.LaboratoryService;
import org.labkey.api.ldk.LDKService;
import org.labkey.api.ldk.table.ButtonConfigFactory;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.query.QueryException;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.view.ActionURL;
import org.labkey.laboratory.DemographicsSource;
import org.labkey.laboratory.LaboratoryModule;
import org.labkey.laboratory.LaboratorySchema;
import org.labkey.laboratory.LaboratoryServiceImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * User: bimber
 * Date: 1/3/13
 * Time: 9:23 PM
 */
public class LaboratoryTableCustomizer implements TableCustomizer
{
    private static final Logger _log = LogManager.getLogger(LaboratoryTableCustomizer.class);
    private final MultiValuedMap _props;

    public LaboratoryTableCustomizer()
    {
        this(new ArrayListValuedHashMap());
    }

    public LaboratoryTableCustomizer(MultiValuedMap props)
    {
        _props = props;
    }

    @Override
    public void customize(TableInfo ti)
    {
        if (ti.isLocked())
        {
            _log.debug("LaboratoryTableCustomizer called on a locked table: " + ti.getPublicSchemaName() + " / " + ti.getName(), new Exception());
            return;
        }

        //apply defaults
        TableCustomizer tc = LDKService.get().getBuiltInColumnsCustomizer(true);
        tc.customize(ti);

        if (ti instanceof AbstractTableInfo)
        {
            customizeColumns((AbstractTableInfo) ti);
            appendCalculatedCols((AbstractTableInfo) ti);
            customizeURLs((AbstractTableInfo) ti);

            LDKService.get().getColumnsOrderCustomizer().customize(ti);

            ensureWorkbookCol((AbstractTableInfo) ti);

            customizeButtonBar((AbstractTableInfo) ti);

            if (LaboratorySchema.TABLE_SAMPLES.equalsIgnoreCase(ti.getName()) && LaboratoryModule.SCHEMA_NAME.equalsIgnoreCase(ti.getPublicSchemaName()))
            {
                customzieSamplesTable((AbstractTableInfo) ti);
            }

            //this should execute after any default code
            if (ti.getUserSchema() != null)
            {
                Container c = ti.getUserSchema().getContainer();

                List<TableCustomizer> customizers = LaboratoryServiceImpl.get().getCustomizers(c, ti.getSchema().getName(), ti.getName());
                for (TableCustomizer customizer : customizers)
                {
                    customizer.customize(ti);
                }
            }
        }
    }

    public void ensureWorkbookCol(AbstractTableInfo ti)
    {
        ColumnInfo wrappedContainer = ti.getColumn("workbook");
        if (wrappedContainer != null)
        {
            List<FieldKey> cols = new ArrayList<FieldKey>();
            cols.addAll(ti.getDefaultVisibleColumns());
            if (!cols.contains(wrappedContainer.getFieldKey()))
            {
                cols.add(wrappedContainer.getFieldKey());
                ti.setDefaultVisibleColumns(cols);
            }
        }
    }

    public void customizeURLs(AbstractTableInfo ti)
    {
        if (!ti.hasPermission(ti.getUserSchema().getUser(), InsertPermission.class))
        {
            return;
        }

        String schemaName = ti.getUserSchema().getSchemaName();
        assert schemaName != null;

        String queryName = ti.getPublicName();
        assert queryName != null;

        List<String> keyFields = ti.getPkColumnNames();
        if (keyFields.size() == 0)
        {
            _log.error("Table: " + schemaName + "." + queryName + " has no key fields: " + StringUtils.join(keyFields, ";"));
            return;
        }
        else if (keyFields.size() != 1)
        {
            _log.warn("Table: " + schemaName + "." + queryName + " has more than 1 PK: " + StringUtils.join(keyFields, ";"));
            return;
        }

        String keyField = keyFields.get(0);
        //NOTE: this is a proxy for a table using the default insert UI
        ActionURL insertUrl = ti.getInsertURL(ti.getUserSchema().getContainer());
        if (insertUrl == null || insertUrl.getAction().equalsIgnoreCase("insertQueryRow"))
        {
            if (!AbstractTableInfo.LINK_DISABLER_ACTION_URL.equals(ti.getInsertURL(ti.getUserSchema().getContainer())))
                ti.setInsertURL(DetailsURL.fromString("/query/importData.view?schemaName=" + schemaName + "&query.queryName=" + queryName + "&keyField=" + keyField + "&bulkImport=false"));

            if (!AbstractTableInfo.LINK_DISABLER_ACTION_URL.equals(ti.getImportDataURL(ti.getUserSchema().getContainer())))
                ti.setImportURL(DetailsURL.fromString("/query/importData.view?schemaName=" + schemaName + "&query.queryName=" + queryName + "&keyField=" + keyField + "&bulkImport=true"));

            if (!AbstractTableInfo.LINK_DISABLER_ACTION_URL.equals(ti.getUpdateURL(null, ti.getUserSchema().getContainer())))
                ti.setUpdateURL(DetailsURL.fromString("/ldk/manageRecord.view?schemaName=" + schemaName + "&query.queryName=" + queryName + "&keyField=" + keyField + "&key=${" + keyField + "}"));
        }
    }

    public void customizeColumns(AbstractTableInfo  ti)
    {
        MutableColumnInfo container = ti.getMutableColumn("container");
        if (container == null)
        {
            container = ti.getMutableColumn("folder");
        }

        if (container != null && ti.getColumn("workbook") == null)
        {
            UserSchema us = getUserSchema(ti, "laboratory");
            if (us != null)
            {
                container.setHidden(true);

                WrappedColumn wrappedContainer = new WrappedColumn(container, "workbook");
                wrappedContainer.setLabel("Workbook");
                wrappedContainer.setFk(QueryForeignKey
                        .from(ti.getUserSchema(), ti.getContainerFilter())
                        .schema(us)
                        .to("workbooks", LaboratoryWorkbooksTable.WORKBOOK_COL, "workbookId"));
                wrappedContainer.setURL(DetailsURL.fromString("/project/start.view"));
                wrappedContainer.setShownInDetailsView(true);
                wrappedContainer.setFacetingBehaviorType(FacetingBehaviorType.ALWAYS_OFF);
                wrappedContainer.setDisplayColumnFactory(new DisplayColumnFactory()
                {
                    @Override
                    public DisplayColumn createRenderer(ColumnInfo colInfo)
                    {
                        return new WorkbookIdDisplayColumn(colInfo);
                    }
                });

                ti.addColumn(wrappedContainer);
            }

            if (ti.getColumn("samplename") != null)
            {
                LDKService.get().applyNaturalSort(ti, "samplename");
            }

            if (ti.getColumn("subjectId") != null)
            {
                LDKService.get().applyNaturalSort(ti, "subjectId");
            }

            if (ti.getColumn("well") != null)
            {
                LDKService.get().applyNaturalSort(ti, "well");
            }
        }
    }

    public void appendCalculatedCols(AbstractTableInfo ti)
    {
        UserSchema us = ti.getUserSchema();
        if (us == null)
        {
            _log.error("Table does have has a UserSchema: " + ti.getName());
            return;
        }

        Container c = us.getContainer();
        if (!c.getActiveModules(true).contains(ModuleLoader.getInstance().getModule(LaboratoryModule.class)))
        {
            _log.warn("Laboratory module not enabled in container: " + c.getPath() + ", so LaboratoryTableCustomizer is aborting for table: " + ti.getPublicName());
            return;
        }

        ColumnInfo dateCol = null;
        ColumnInfo subjectCol = null;
        for (ColumnInfo ci : ti.getColumns())
        {
            if (LaboratoryService.PARTICIPANT_CONCEPT_URI.equals(ci.getConceptURI()))
            {
                subjectCol = ci;
            }
            else if (LaboratoryService.SAMPLEDATE_CONCEPT_URI.equals(ci.getConceptURI()))
            {
                dateCol = ci;
            }
        }

        if (dateCol != null && subjectCol != null)
        {
            appendOverlapingProjectsCol(us, ti, dateCol.getName(), subjectCol.getName());
            appendRelativeDatesCol(us, ti, dateCol.getName(), subjectCol.getName());
            appendMajorEventsCol(us, ti, dateCol.getName(), subjectCol.getName());
        }

        if (subjectCol != null)
        {
            appendDemographicsCols(us, ti, subjectCol);
            appendProjectsCol(us, ti, subjectCol.getName());
        }
    }

    private void appendDemographicsCols(final UserSchema us, AbstractTableInfo ti, ColumnInfo subjectCol)
    {
       LaboratoryServiceImpl service = LaboratoryServiceImpl.get();
        Set<DemographicsSource> qds = service.getDemographicsSources(us.getContainer(), us.getUser());
        if (qds != null)
        {
            for (final DemographicsSource qd : qds)
            {
                Container targetContainer = us.getContainer().isWorkbookOrTab() ? us.getContainer().getParent() : us.getContainer();
                TableInfo target = qd.getTableInfo(targetContainer, us.getUser());

                //TODO: push this into LaboratoryService and also cache them?
                if (target != null)
                {
                    if (target.getUserSchema().getName().equalsIgnoreCase(ti.getUserSchema().getName()) && (target.getName().equalsIgnoreCase(ti.getName())))
                    {
                        //dont link to self
                        continue;
                    }

                    String name = ColumnInfo.legalNameFromName(qd.getLabel());

                    if (ti.getColumn(name) != null)
                        continue;

                    WrappedColumn col = new WrappedColumn(subjectCol, name);
                    col.setLabel(qd.getLabel());
                    col.setReadOnly(true);
                    col.setIsUnselectable(true);
                    col.setUserEditable(false);

                    UserSchema targetSchema = qd.getTableInfo(targetContainer, us.getUser()).getUserSchema();
                    col.setFk(new QueryForeignKey(us, ti.getContainerFilter(), targetSchema, null, qd.getQueryName(), qd.getTargetColumn(), qd.getTargetColumn())
                    {
                        @Override
                        public TableInfo getLookupTableInfo()
                        {
                            if (null == _table)
                            {
                                // get forWrite==true because we modify this table
                                TableInfo ti = targetSchema.getTable(_tableName, getLookupContainerFilter(), true, true);

                                ((MutableColumnInfo)ti.getColumn(qd.getTargetColumn())).setKeyField(true);

                                Set<ColumnInfo> birthCols = new HashSet<>();
                                Set<ColumnInfo> deathCols = new HashSet<>();
                                for (ColumnInfo ci : ti.getColumns())
                                {
                                    if (LaboratoryService.BIRTHDATE_CONCEPT_URI.equalsIgnoreCase(ci.getConceptURI()))
                                    {
                                        birthCols.add(ci);
                                    }
                                    else if (LaboratoryService.DEATHDATE_CONCEPT_URI.equalsIgnoreCase(ci.getConceptURI()))
                                    {
                                        deathCols.add(ci);
                                    }
                                }

                                if (!birthCols.isEmpty())
                                {
                                    ColumnInfo deathCol = deathCols.isEmpty() ? null : deathCols.iterator().next();
                                    addAgeCols((AbstractTableInfo)ti, birthCols.iterator().next(), deathCol);
                                }
                                _table = ti;
                            }

                            return _table;
                        }

//                        @Override
//                        public Set<FieldKey> getSuggestedColumns()
//                        {
//                            Set<FieldKey> keys = super.getSuggestedColumns();
//                            keys = keys == null ? new HashSet<>() : new HashSet<>(keys);
//                            keys.add(getRemappedField(subjectCol.getFieldKey()));
//
//                            return keys;
//                        }
                    });

                    ti.addColumn(col);
                }
            }
        }
    }

    private void appendMajorEventsCol(final UserSchema us, AbstractTableInfo ds, final String dateColName, final String subjectColName)
    {
        String name = "majorEvents";
        if (ds.getColumn(name) != null)
            return;

        List<ColumnInfo> pks = ds.getPkColumns();
        if (pks.size() != 1){
            _log.warn("Table does not have a single PK column: " + ds.getName());
            return;
        }
        ColumnInfo pk = pks.get(0);
        final String pkColSelectName = pk.getFieldKey().toSQLString();
        final String pkColRawName = pk.getName();

        MutableColumnInfo col = new WrappedColumn(pk, name);
        col.setLabel("Major Events");
        col.setDescription("This column shows all major events recorded in this subject's history and will calculate the time elapsed between the current sample and these dates.");
        col.setReadOnly(true);
        col.setIsUnselectable(true);
        col.setUserEditable(false);

        final String schemaName = ds.getUserSchema().getSchemaPath().toSQLString();
        final String subjectSelectName = ds.getSqlDialect().makeLegalIdentifier(subjectColName);
        final String dateSelectName = dateColName == null ? null : ds.getSqlDialect().makeLegalIdentifier(dateColName);
        final String colName = ds.getName() + "_majorEvents";
        final String publicTableName = ds.getPublicName();
        final String querySelectName = ds.getSqlDialect().makeLegalIdentifier(ds.getPublicName());

        col.setFk(new LookupForeignKey(){
            @Override
            public TableInfo getLookupTableInfo()
            {
                Container target = us.getContainer().isWorkbookOrTab() ? us.getContainer().getParent() : us.getContainer();
                UserSchema effectiveUs = us.getContainer().isWorkbookOrTab() ? QueryService.get().getUserSchema(us.getUser(), target, us.getSchemaPath()) : us;
                QueryDefinition qd = QueryService.get().createQueryDef(us.getUser(), target, effectiveUs, colName);

                qd.setSql(getMajorEventsSql(target, schemaName, querySelectName, pkColSelectName, subjectSelectName, dateSelectName));
                qd.setIsTemporary(true);

                List<QueryException> errors = new ArrayList<>();
                TableInfo ti = qd.getTable(errors, true);

                if (!errors.isEmpty()){
                    _log.error("Problem with table customizer: " + publicTableName);
                    for (QueryException e : errors)
                    {
                        _log.error(e.getMessage());
                    }
                }

                if (ti != null)
                {
                    MutableColumnInfo col = (MutableColumnInfo)ti.getColumn(pkColRawName);
                    col.setKeyField(true);
                    col.setHidden(true);
                }

                return ti;
            }
        });

        ds.addColumn(col);
    }

    private void appendOverlapingProjectsCol(final UserSchema us, AbstractTableInfo ds, final String dateColName, final String subjectColName)
    {
        String name = "overlappingProjects";
        if (ds.getColumn(name) != null)
            return;

        if (dateColName == null)
        {
            _log.info("Unable to add overlapping groups column for table " + ds.getPublicSchemaName() + "." + ds.getName() + ", since dateCol is null");
            return;
        }

        List<ColumnInfo> pks = ds.getPkColumns();
        if (pks.size() != 1){
            _log.warn("Table does not have a single PK column: " + ds.getName());
            return;
        }
        ColumnInfo pk = pks.get(0);
        final String pkColSelectName = pk.getFieldKey().toSQLString();
        final String pkColRawName = pk.getName();
        final String publicTableName = ds.getPublicName();
        final String colName = ds.getName() + "_overlappingProjects";

        final String schemaName = ds.getUserSchema().getSchemaPath().toSQLString();
        final String querySelectName = ds.getSqlDialect().makeLegalIdentifier(ds.getPublicName());
        final String subjectSelectName = ds.getSqlDialect().makeLegalIdentifier(subjectColName);
        final String dateSelectName = dateColName == null ? null : ds.getSqlDialect().makeLegalIdentifier(dateColName);

        WrappedColumn col = new WrappedColumn(pk, name);
        col.setLabel("Overlapping Groups");
        col.setDescription("This column shows all groups to which this subject belonged at the time of this sample.");
        col.setReadOnly(true);
        col.setIsUnselectable(true);
        col.setUserEditable(false);
        col.setFk(new LookupForeignKey(){
            @Override
            public TableInfo getLookupTableInfo()
            {
                Container target = us.getContainer().isWorkbookOrTab() ? us.getContainer().getParent() : us.getContainer();
                UserSchema effectiveUs = us.getContainer().isWorkbookOrTab() ? QueryService.get().getUserSchema(us.getUser(), target, us.getSchemaPath()) : us;
                QueryDefinition qd = QueryService.get().createQueryDef(us.getUser(), target, effectiveUs, colName);

                qd.setSql(getOverlapSql(target, schemaName, querySelectName, pkColSelectName, subjectSelectName, dateSelectName));
                qd.setIsTemporary(true);

                List<QueryException> errors = new ArrayList<>();
                TableInfo ti = qd.getTable(errors, true);

                if (!errors.isEmpty()){
                    _log.error("Problem with table customizer: " + publicTableName);
                    for (QueryException e : errors)
                    {
                        _log.error(e.getMessage());
                    }
                }

                if (ti != null)
                {
                    MutableColumnInfo col = (MutableColumnInfo)ti.getColumn(pkColRawName);
                    col.setKeyField(true);
                    col.setHidden(true);

                    ((MutableColumnInfo)ti.getColumn("projects")).setLabel("Overlapping Groups");
                    ((MutableColumnInfo)ti.getColumn("groups")).setLabel("Overlapping Sub-groups");
                }

                return ti;
            }
        });

        ds.addColumn(col);

        //add pivot column
        String pivotColName = "overlappingProjectsPivot";
        WrappedColumn col2 = new WrappedColumn(pk, pivotColName);
        col2.setLabel("Overlapping Group List");
        col2.setDescription("Shows groups to which this subject belonged at the time of this sample.");
        col2.setHidden(true);
        col2.setReadOnly(true);
        col2.setIsUnselectable(true);
        col2.setUserEditable(false);
        final String lookupColName = ds.getName() + "_overlappingProjectsPivot";
        col2.setFk(new LookupForeignKey(){
            @Override
            public TableInfo getLookupTableInfo()
            {
                Container target = us.getContainer().isWorkbookOrTab() ? us.getContainer().getParent() : us.getContainer();
                QueryDefinition qd = QueryService.get().createQueryDef(us.getUser(), target, us, lookupColName);

                qd.setSql(getOverlapPivotSql(target, schemaName, querySelectName, pkColSelectName, subjectColName, dateColName));
                qd.setIsTemporary(true);

                List<QueryException> errors = new ArrayList<>();
                TableInfo ti = qd.getTable(errors, true);

                if (!errors.isEmpty()){
                    _log.error("Problem with table customizer: " + publicTableName);
                    for (QueryException e : errors)
                    {
                        _log.error(e.getMessage());
                    }
                }

                if (ti != null)
                {
                    MutableColumnInfo col = (MutableColumnInfo)ti.getColumn(pkColRawName);
                    col.setKeyField(true);
                    col.setHidden(true);

                    ((MutableColumnInfo)ti.getColumn("lastStartDate")).setLabel("Most Recent Start Date");
                }

                return ti;
            }
        });

        ds.addColumn(col2);
    }

    public void appendProjectsCol(final UserSchema us, AbstractTableInfo ds, final String subjectColName)
    {
        String name = "allProjects";
        if (ds.getColumn(name) != null)
            return;

        List<ColumnInfo> pks = ds.getPkColumns();
        if (pks.size() != 1){
            _log.warn("Table does not have a single PK column: " + ds.getName());
            return;
        }
        ColumnInfo pk = pks.get(0);
        final String pkColSelectName = pk.getFieldKey().toSQLString();
        final String pkColRawName = pk.getName();
        final String schemaName = ds.getUserSchema().getSchemaPath().toSQLString();
        final String querySelectName = ds.getSqlDialect().makeLegalIdentifier(ds.getPublicName());
        final String subjectSelectName = ds.getSqlDialect().makeLegalIdentifier(subjectColName);
        final String publicTableName = ds.getPublicName();

        final String colName = ds.getName() + "_allProjects";
        WrappedColumn col = new WrappedColumn(pk, name);
        col.setLabel("Groups");
        col.setDescription("This column shows all groups to which this subject has ever been a member, regardless of whether that assignment overlaps with the current data point");
        col.setReadOnly(true);
        col.setIsUnselectable(true);
        col.setUserEditable(false);
        col.setFk(new LookupForeignKey(){
            @Override
            public TableInfo getLookupTableInfo()
            {
                Container target = us.getContainer().isWorkbookOrTab() ? us.getContainer().getParent() : us.getContainer();
                UserSchema effectiveUs = us.getContainer().isWorkbookOrTab() ? QueryService.get().getUserSchema(us.getUser(), target, us.getSchemaPath()) : us;
                QueryDefinition qd = QueryService.get().createQueryDef(us.getUser(), target, effectiveUs, colName);

                qd.setSql(getOverlapSql(target, schemaName, querySelectName, pkColSelectName, subjectSelectName, null));
                qd.setIsTemporary(true);

                List<QueryException> errors = new ArrayList<>();
                TableInfo ti = qd.getTable(errors, true);

                if (!errors.isEmpty()){
                    _log.error("Problem with table customizer: " + publicTableName);
                    for (QueryException e : errors)
                    {
                        _log.error(e.getMessage());
                    }
                }

                if (ti != null)
                {
                    MutableColumnInfo col = (MutableColumnInfo) ti.getColumn(pkColRawName);
                    col.setKeyField(true);
                    col.setHidden(true);

                    ((MutableColumnInfo)ti.getColumn("projects")).setLabel("All Groups/Projects");
                    ((MutableColumnInfo)ti.getColumn("groups")).setLabel("All Sub-Groups");
                }

                return ti;
            }
        });

        ds.addColumn(col);

        //add pivot column
        String pivotColName = "allProjectsPivot";
        final String lookupName = ds.getName() + "_allProjectsPivot";
        WrappedColumn col2 = new WrappedColumn(pk, pivotColName);
        col2.setLabel("Group Summary List");
        col2.setDescription("Shows groups to which this subject belonged at any point in time.");
        col2.setHidden(true);
        col2.setReadOnly(true);
        col2.setIsUnselectable(true);
        col2.setUserEditable(false);
        col2.setFk(new LookupForeignKey(){
            @Override
            public TableInfo getLookupTableInfo()
            {
                Container target = us.getContainer().isWorkbookOrTab() ? us.getContainer().getParent() : us.getContainer();
                UserSchema effectiveUs = us.getContainer().isWorkbookOrTab() ? QueryService.get().getUserSchema(us.getUser(), target, us.getSchemaPath()) : us;
                QueryDefinition qd = QueryService.get().createQueryDef(us.getUser(), target, effectiveUs, lookupName);

                qd.setSql(getOverlapPivotSql(target, schemaName, querySelectName, pkColSelectName, subjectSelectName, null));
                qd.setIsTemporary(true);

                List<QueryException> errors = new ArrayList<>();
                TableInfo ti = qd.getTable(errors, true);

                if (!errors.isEmpty()){
                    _log.error("Problem with table customizer: " + publicTableName);
                    for (QueryException e : errors)
                    {
                        _log.error(e.getMessage());
                    }
                }

                if (ti != null)
                {
                    MutableColumnInfo col = (MutableColumnInfo) ti.getColumn(pkColRawName);
                    col.setKeyField(true);
                    col.setHidden(true);

                    ((MutableColumnInfo)ti.getColumn("lastStartDate")).setLabel("Most Recent Start Date");
                }

                return ti;
            }
        });

        ds.addColumn(col2);
    }

    private String getOverlapSql(Container source, String schemaName, String querySelectName, String pkColSelectName, String subjectSelectName, @Nullable String dateSelectName)
    {
        return "SELECT\n" +
                "s." + pkColSelectName + ",\n" +
                "group_concat(DISTINCT s.project, chr(10)) as projects,\n" +
                "group_concat(DISTINCT s.projectGroup, chr(10)) as groups,\n" +
                "\n" +
                "FROM (\n" +
                "\n" +
                "SELECT\n" +
                "s." + pkColSelectName + ",\n" +
                "p.project,\n" +
                "CASE\n" +
                "  WHEN p.groupname IS NULL then NULL\n" +
                "  ELSE (p.project || ' (' || p.groupname || ')')\n" +
                "END as projectGroup,\n" +
                "\n" +
                "FROM " + schemaName + "." + querySelectName + " s\n" +
                "JOIN \"" + source.getPath() + "\".laboratory.project_usage p\n" +
                "ON (s." + subjectSelectName + " = p.subjectId" +
                (dateSelectName != null ? " AND p.startdate <= s." + dateSelectName + " AND s." + dateSelectName + " <= COALESCE(p.enddate, {fn curdate()})" : "") +
                ")\n" +
                "WHERE s." + subjectSelectName + " IS NOT NULL\n" +
                (dateSelectName != null ? " AND s." + dateSelectName + " IS NOT NULL " : "") +
                "\n" +
                ") s\n" +
                "\n" +
                "GROUP BY s." + pkColSelectName;
    }

    private String getOverlapPivotSql(Container source, String schemaName, String querySelectName, String pkColSelectName, String subjectSelectName, @Nullable String dateSelectName)
    {
        return "SELECT\n" +
                "s." + pkColSelectName + ",\n" +
                "p.project,\n" +
                "max(p.startdate) as lastStartDate\n" +
                "\n" +
                "FROM " + schemaName + "." + querySelectName + " s\n" +
                "JOIN \"" + source.getPath() + "\".laboratory.project_usage p\n" +
                "ON (s." + subjectSelectName + " = p.subjectId" +
                (dateSelectName != null ? " AND p.startdate <= s." + dateSelectName + " AND s." + dateSelectName + " <= COALESCE(p.enddate, {fn curdate()})" : "") +
                ")\n" +
                "WHERE s." + subjectSelectName + " IS NOT NULL\n" +
                (dateSelectName != null ? " AND s." + dateSelectName + " IS NOT NULL " : "") +
                "\n" +
                "GROUP BY s." + pkColSelectName + ", p.project\n" +
                "PIVOT lastStartDate by project IN (select distinct project from laboratory.project_usage)";
    }

    private String getMajorEventsSql(Container sourceContainer, String schemaName, String querySelectName, String pkColSelectName, String subjectSelectName, @Nullable String dateSelectName)
    {
        return "SELECT\n" +
            "t." + pkColSelectName + ",\n" +
            "t.event,\n" +
            "max(date) as eventDate,\n" +
            "max(DaysPostEvent) as DaysPostEvent,\n" +
            "max(WeeksPostEvent) as WeeksPostEvent,\n" +
            "max(WeeksPostEventDecimal) as WeeksPostEventDecimal,\n" +
            "max(MonthsPostEvent) as MonthsPostEvent,\n" +
            "max(YearsPostEvent) as YearsPostEvent,\n" +
            "max(YearsPostEventDecimal) as YearsPostEventDecimal,\n" +
            "\n" +
            "FROM (\n" +
            "\n" +
            "SELECT\n" +
            "s." + pkColSelectName + ",\n" +
            "p.event,\n" +
            "p.date,\n" +
            "\n" +
            "TIMESTAMPDIFF('SQL_TSI_DAY', p.date, s." + dateSelectName + ") as DaysPostEvent,\n" +
            "TIMESTAMPDIFF('SQL_TSI_DAY', p.date, s." + dateSelectName + ") / 7 as WeeksPostEvent,\n" +
            "ROUND(CONVERT(TIMESTAMPDIFF('SQL_TSI_DAY', p.date, s." + dateSelectName + "), DOUBLE) / 7.0, 1) as WeeksPostEventDecimal,\n" +
            "ROUND(CONVERT(age_in_months(p.date, s." + dateSelectName + "), DOUBLE), 1) AS MonthsPostEvent,\n" +
            "floor(age(p.date, s." + dateSelectName + ")) AS YearsPostEvent,\n" +
                "ROUND(CONVERT(age_in_months(p.date, s." + dateSelectName + "), DOUBLE) / 12, 1) AS YearsPostEventDecimal,\n" +
            "\n" +
            "FROM " + schemaName + "." + querySelectName + " s\n" +
            "JOIN \"" + sourceContainer.getPath() + "\".laboratory.major_events p\n" +
            "ON (s." + subjectSelectName + " = p.subjectId)\n" +
            "WHERE s." + subjectSelectName + " IS NOT NULL\n" +
            "\n" +
            ") t\n" +
            "\n" +
            "GROUP BY t." + pkColSelectName + ", t.event\n" +
            "PIVOT DaysPostEvent, WeeksPostEvent, WeeksPostEventDecimal, MonthsPostEvent, YearsPostEvent, YearsPostEventDecimal by event";
    }

    private void appendRelativeDatesCol(final UserSchema us, AbstractTableInfo ds, final String dateColName, final String subjectColName)
    {
        String name = "relativeDates";
        if (ds.getColumn(name) != null)
            return;

        List<ColumnInfo> pks = ds.getPkColumns();
        if (pks.size() != 1){
            _log.warn("Table does not have a single PK column: " + ds.getName());
            return;
        }
        ColumnInfo pk = pks.get(0);
        final String pkColSelectName = pk.getFieldKey().toSQLString();
        final String pkColRawName = pk.getName();

        WrappedColumn col = new WrappedColumn(pk, name);
        col.setLabel("Relative Dates");
        col.setReadOnly(true);
        col.setIsUnselectable(true);
        col.setUserEditable(false);

        final String colName = ds.getName() + "_relativeDates";
        final String schemaName = ds.getUserSchema().getSchemaPath().toSQLString();
        final String subjectSelectName = ds.getSqlDialect().makeLegalIdentifier(subjectColName);
        final String dateSelectName = dateColName == null ? null : ds.getSqlDialect().makeLegalIdentifier(dateColName);
        final String publicTableName = ds.getSqlDialect().makeLegalIdentifier(ds.getPublicName());

        col.setFk(new LookupForeignKey(){
            @Override
            public TableInfo getLookupTableInfo()
            {
                Container target = us.getContainer().isWorkbookOrTab() ? us.getContainer().getParent() : us.getContainer();
                UserSchema effectiveUs = us.getContainer().isWorkbookOrTab() ? QueryService.get().getUserSchema(us.getUser(), target, us.getSchemaPath()) : us;
                QueryDefinition qd = QueryService.get().createQueryDef(us.getUser(), target, effectiveUs, colName);

                qd.setSql("SELECT\n" +
                "t." + pkColSelectName + ",\n" +
                "t.project,\n" +
                "max(startdate) as startDate,\n" +
                "max(DaysPostStart) as DaysPostStart,\n" +
                "max(WeeksPostStart) as WeeksPostStart,\n" +
                "max(WeeksPostStartDecimal) as WeeksPostStartDecimal,\n" +
                "max(MonthsPostStart) as MonthsPostStart,\n" +
                "max(YearsPostStart) as YearsPostStart,\n" +
                "max(YearsPostStartDecimal) as YearsPostStartDecimal,\n" +
                "\n" +
                "FROM (\n" +
                "\n" +
                "SELECT\n" +
                "s." + pkColSelectName + ",\n" +
                "p.project,\n" +
                "p.startdate,\n" +
                "\n" +
                "TIMESTAMPDIFF('SQL_TSI_DAY', p.startdate, s." + dateSelectName + ") as DaysPostStart,\n" +
                "TIMESTAMPDIFF('SQL_TSI_DAY', p.startdate, s." + dateSelectName + ") / 7 as WeeksPostStart,\n" +
                "ROUND(CONVERT(TIMESTAMPDIFF('SQL_TSI_DAY', p.startdate, s." + dateSelectName + "), DOUBLE) / 7.0, 1) as WeeksPostStartDecimal,\n" +
                "ROUND(CONVERT(age_in_months(p.startdate, s." + dateSelectName + "), DOUBLE), 1) AS MonthsPostStart,\n" +
                "floor(age(p.startdate, s." + dateSelectName + ")) AS YearsPostStart,\n" +
                "ROUND(CONVERT(age_in_months(p.startdate, s." + dateSelectName + "), DOUBLE) / 12, 1) AS YearsPostStartDecimal,\n" +
                "\n" +
                "FROM " + schemaName + "." + publicTableName + " s\n" +
                "JOIN \"" + target.getPath() + "\".laboratory.project_usage p\n" +
                "ON (s." + subjectSelectName + " = p.subjectId AND CONVERT(p.startdate, DATE) <= CONVERT(s." + dateSelectName + ", DATE) AND CONVERT(s." + dateSelectName + ", DATE) <= CONVERT(COALESCE(p.enddate, {fn curdate()}), DATE))\n" +
                "WHERE s." + dateSelectName + " IS NOT NULL and s." + subjectSelectName + " IS NOT NULL\n" +
                "\n" +
                ") t\n" +
                "\n" +
                "GROUP BY t." + pkColSelectName + ", t.project\n" +
                "PIVOT DaysPostStart, WeeksPostStart, WeeksPostStartDecimal, MonthsPostStart, YearsPostStart, YearsPostStartDecimal by project");

                qd.setIsTemporary(true);

                List<QueryException> errors = new ArrayList<>();
                TableInfo ti = qd.getTable(errors, true);
                if (!errors.isEmpty()){
                    _log.error("Problem with table customizer: " + publicTableName);
                    for (QueryException e : errors)
                    {
                        _log.error(e.getMessage());
                    }
                }

                if (ti != null)
                {
                    MutableColumnInfo col = (MutableColumnInfo)ti.getColumn(pkColRawName);
                    col.setKeyField(true);
                    col.setHidden(true);
                }

                return ti;
            }
        });

        ds.addColumn(col);
    }

    public UserSchema getUserSchema(AbstractTableInfo ds, String name)
    {
        UserSchema us = ds.getUserSchema();
        if (us != null)
        {
            if (name.equalsIgnoreCase(us.getName()))
                return us;

            return QueryService.get().getUserSchema(us.getUser(), us.getContainer(), name);
        }

        return null;
    }

    public void addAgeCols(AbstractTableInfo ti, ColumnInfo birthCol, ColumnInfo deathCol)
    {
        //NOTE: years will round to the nearest integer, meaning if you're 31.6 years old it reports it as 32.  not what you typically want.
        appendTimeDiffCol(ti, birthCol, deathCol, "ageInYears", "Age In Years", Calendar.DATE, 365.0, true);
        appendTimeDiffCol(ti, birthCol, deathCol, "ageInYearsDecimal", "Age In Years, Decimal", Calendar.DATE, 365.0);
        appendAgeInMonthsCol(ti, birthCol, deathCol, "ageInMonths", "Age In Months");
        appendTimeDiffCol(ti, birthCol, deathCol, "ageInDays", "Age In Days", Calendar.DATE);
    }

    private void appendAgeInMonthsCol(AbstractTableInfo ti, ColumnInfo birthCol, ColumnInfo deathCol, String name, String label)
    {
        if (ti.getColumn(name) == null)
        {
            SQLFragment sql = getAgeInMonthsSQL(ti.getSchema(), birthCol, deathCol);
            JdbcType type = JdbcType.INTEGER;

            ExprColumn col = new ExprColumn(ti, name, sql, type, getDependentColumns(birthCol, deathCol));
            col.setLabel(label);
            col.setShownInDetailsView(false);
            col.setFormat("0.##");
            ti.addColumn(col);
        }
    }

    //NOTE: patterned off of AgeInMonthsMethodInfo
    public SQLFragment getAgeInMonthsSQL(DbSchema schema, ColumnInfo column, @Nullable ColumnInfo deathCol)
    {
        SQLFragment yearA = schema.getSqlDialect().getDatePart(Calendar.YEAR, column.getValueSql(ExprColumn.STR_TABLE_ALIAS));
        SQLFragment monthA = schema.getSqlDialect().getDatePart(Calendar.MONTH, column.getValueSql(ExprColumn.STR_TABLE_ALIAS));
        SQLFragment dayA = schema.getSqlDialect().getDatePart(Calendar.DATE, column.getValueSql(ExprColumn.STR_TABLE_ALIAS));

        String curDateExpr = getCurDateExpr(deathCol);
        SQLFragment yearB = new SQLFragment(schema.getSqlDialect().getDatePart(Calendar.YEAR, curDateExpr));
        SQLFragment monthB = new SQLFragment(schema.getSqlDialect().getDatePart(Calendar.MONTH, curDateExpr));
        SQLFragment dayB = new SQLFragment(schema.getSqlDialect().getDatePart(Calendar.DATE, curDateExpr));

        SQLFragment ret = new SQLFragment();
        ret.append("(CASE WHEN (")
                .append(dayA).append(">").append(dayB)
                .append(") THEN (")
                .append("12*(").append(yearB).append("-").append(yearA).append(")")
                .append("+")
                .append(monthB).append("-").append(monthA).append("-1")
                .append(") ELSE (")
                .append("12*(").append(yearB).append("-").append(yearA).append(")")
                .append("+")
                .append(monthB).append("-").append(monthA)
                .append(") END)");
        return ret;
    }

    private void appendTimeDiffCol(AbstractTableInfo ti, ColumnInfo parent, ColumnInfo deathCol, String name, String label, int part)
    {
        appendTimeDiffCol(ti, parent, deathCol, name, label, part, null);
    }

    private void appendTimeDiffCol(AbstractTableInfo ti, ColumnInfo parent, ColumnInfo deathCol, String name, String label, int part, Double divisor)
    {
        appendTimeDiffCol(ti, parent, deathCol, name, label, part, divisor, false);
    }

    private String getCurDateExpr(@Nullable ColumnInfo deathCol)
    {
        String curDateExpr = "{fn curdate()}";
        if (deathCol != null)
        {
            curDateExpr = "COALESCE(" + ExprColumn.STR_TABLE_ALIAS + "." + deathCol.getName() + ", " + curDateExpr + ")";
        }

        return curDateExpr;
    }

    private void appendTimeDiffCol(AbstractTableInfo ti, ColumnInfo parent, ColumnInfo deathCol, String name, String label, int part, Double divisor, boolean floored)
    {
        if (ti.getColumn(name) == null)
        {
            JdbcType type;
            SQLFragment sql;
            String curDateExpr = getCurDateExpr(deathCol);

            if (divisor == null)
            {
                sql = new SQLFragment(ti.getSqlDialect().getDateDiff(part, curDateExpr, ExprColumn.STR_TABLE_ALIAS + "." + parent.getName()));
                type = JdbcType.INTEGER;
            }
            else
            {
                String fragment = "((" + ti.getSqlDialect().getDateDiff(part, curDateExpr, ExprColumn.STR_TABLE_ALIAS + "." + parent.getName()) + ") / ? )";
                if (floored)
                {
                    fragment = "floor(" + fragment + ")";
                    type = JdbcType.INTEGER;
                }
                else
                {
                    type = JdbcType.DOUBLE;
                }

                sql = new SQLFragment(fragment, divisor);
            }

            ExprColumn col = new ExprColumn(ti, name, sql, type, getDependentColumns(parent, deathCol));
            col.setLabel(label);
            col.setShownInDetailsView(false);
            col.setFormat("0.##");
            ti.addColumn(col);
        }
    }

    private ColumnInfo[] getDependentColumns(ColumnInfo col1, ColumnInfo col2)
    {
        ColumnInfo[] dcs;
        if (col2 != null)
        {
            dcs = new ColumnInfo[]{col1, col2};
        }
        else
        {
            dcs = new ColumnInfo[]{col1};
        }

        return dcs;
    }

    public void customizeButtonBar(AbstractTableInfo ti)
    {
        List<ButtonConfigFactory> buttons = LDKService.get().getQueryButtons(ti);
        LDKService.get().customizeButtonBar(ti, buttons);
        if (ti.getButtonBarConfig() != null)
        {
            String[] includes = ti.getButtonBarConfig().getScriptIncludes();
            LinkedHashSet<String> newIncludes = new LinkedHashSet<>();
            newIncludes.add("laboratory.context");
            if (includes != null)
            {
                newIncludes.addAll(Arrays.asList(includes));
            }

            ti.getButtonBarConfig().setScriptIncludes(newIncludes.toArray(new String[newIncludes.size()]));
        }
    }

    private void customzieSamplesTable(AbstractTableInfo ti)
    {
        String name = "sampleCount";
        if (ti.getColumn(name) == null)
        {
            Container c = ti.getUserSchema().getContainer();
            c = c.isWorkbook() ? c.getParent() : c;
            SQLFragment containerSql = ContainerFilter.current(c).getSQLFragment(LaboratorySchema.getInstance().getSchema(), new SQLFragment(ti.getContainerFieldKey().toString()));

            SQLFragment sql = new SQLFragment("(SELECT count(*) as _expr FROM laboratory.samples s WHERE " +
                    " (s.").append(containerSql).append(")" + " AND ").
                    append(getNullSafeEqual("s.subjectid", ExprColumn.STR_TABLE_ALIAS + ".subjectid")).
                    append(" AND ").append(getNullSafeEqual("CAST(s.sampledate as DATE)", "CAST(" + ExprColumn.STR_TABLE_ALIAS + ".sampledate as DATE)")).
                    append(" AND ").append(getNullSafeEqual("s.sampletype", ExprColumn.STR_TABLE_ALIAS + ".sampletype")).
                    append(" AND ").append(getNullSafeEqual("s.samplesubtype", ExprColumn.STR_TABLE_ALIAS + ".samplesubtype")).
                    append(" AND ").append(ExprColumn.STR_TABLE_ALIAS).append(".dateremoved IS NULL").
                    append(" AND s.rowid != ").append(ExprColumn.STR_TABLE_ALIAS).append(".rowid").
                    append(")");

            ExprColumn col = new ExprColumn(ti, name, sql, JdbcType.INTEGER, ti.getColumn("subjectid"), ti.getColumn("sampledate"), ti.getColumn("sampletype"), ti.getColumn("samplesubtype"));
            col.setLabel("Matching Samples");
            col.setFacetingBehaviorType(FacetingBehaviorType.ALWAYS_OFF);
            col.setDescription("This column show the total number of active freezer samples with the same subjectId, sample date, sample type and sample subtype as the current row");
            col.setURL(DetailsURL.fromString("/query/executeQuery.view?schemaName=laboratory&query.queryName=samples&query.subjectId~eq=${subjectId}&query.sampledate~eq=${sampledate}&query.sampletype~eq=${sampletype}&query.samplesubtype~eq=${samplesubtype}", c));
            ti.addColumn(col);
        }

        LDKService.get().applyNaturalSort(ti, "freezer");
        LDKService.get().applyNaturalSort(ti, "cane");
        LDKService.get().applyNaturalSort(ti, "box");
        LDKService.get().applyNaturalSort(ti, "box_row");
        LDKService.get().applyNaturalSort(ti, "box_column");

        LDKService.get().applyNaturalSort(ti, "subjectId");
    }

    private String getNullSafeEqual(String col1, String col2)
    {
        return "(" + col1 + " = " + col2 + " OR (" + col1 + " IS NULL AND " + col2 + " IS NULL))";
    }
}
