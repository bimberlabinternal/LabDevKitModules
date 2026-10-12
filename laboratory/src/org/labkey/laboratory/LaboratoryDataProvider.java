/*
 * Copyright (c) 2013-2026 LabKey Corporation
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

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.labkey.api.cache.Cache;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.laboratory.AbstractDataProvider;
import org.labkey.api.laboratory.DataProvider;
import org.labkey.api.laboratory.DetailsUrlWithoutLabelNavItem;
import org.labkey.api.laboratory.JSTabbedReportItem;
import org.labkey.api.laboratory.LaboratoryService;
import org.labkey.api.laboratory.NavItem;
import org.labkey.api.laboratory.QueryCountNavItem;
import org.labkey.api.laboratory.QueryImportNavItem;
import org.labkey.api.laboratory.QueryTabbedReportItem;
import org.labkey.api.laboratory.ReportItem;
import org.labkey.api.laboratory.SimpleSettingsItem;
import org.labkey.api.laboratory.SummaryNavItem;
import org.labkey.api.laboratory.TabbedReportItem;
import org.labkey.api.ldk.table.QueryCache;
import org.labkey.api.module.Module;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryAction;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.study.Study;
import org.labkey.api.study.StudyService;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.template.ClientDependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class LaboratoryDataProvider extends AbstractDataProvider
{
    public static final String NAME = "Laboratory";
    private final Module _module;

    private static final Logger _log = LogHelper.getLogger(LaboratoryDataProvider.class, "Messages from LaboratoryDataProvider");

    private static Cache<String, List<TabbedReportModel>> _cache = null;

    public LaboratoryDataProvider(Module m)
    {
        _module = m;
    }

    public static void clearCache()
    {
        if (_cache != null)
        {
            _cache.clear();
        }
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public ActionURL getInstructionsUrl(Container c, User u)
    {
        return null;
    }

    @Override
    public List<NavItem> getDataNavItems(Container c, User u)
    {
        return Collections.emptyList();
    }

    @Override
    public List<NavItem> getSampleNavItems(Container c, User u)
    {
        List<NavItem> items = new ArrayList<>();
        QueryCache cache = new QueryCache();
        if (c.getActiveModules().contains(getOwningModule()))
        {
            items.add(new QueryImportNavItem(this, LaboratoryModule.SCHEMA_NAME, "Samples", LaboratoryService.NavItemCategory.samples, "Samples", cache));
            items.add(new QueryImportNavItem(this, LaboratoryModule.SCHEMA_NAME, "DNA_Oligos", LaboratoryService.NavItemCategory.samples, "Samples", cache));
            items.add(new QueryImportNavItem(this, LaboratoryModule.SCHEMA_NAME, "Peptides", LaboratoryService.NavItemCategory.samples, "Samples", cache));
            items.add(new QueryImportNavItem(this, LaboratoryModule.SCHEMA_NAME, "Antibodies", LaboratoryService.NavItemCategory.samples, "Samples", cache));
        }

        return Collections.unmodifiableList(items);
    }

    @Override
    public List<NavItem> getReportItems(Container c, User u)
    {
        List<NavItem> items = new ArrayList<>();
        QueryCache cache = new QueryCache();
        if (c.getActiveModules().contains(getOwningModule()))
        {
            String reportCategory = "Samples"; //note, this is how they appear in the reports panel
            QueryImportNavItem owner1 = new QueryImportNavItem(this, LaboratoryModule.SCHEMA_NAME, "Samples", LaboratoryService.NavItemCategory.samples, "Samples", cache);
            ReportItem item1 = new ReportItem(this, null, LaboratoryModule.SCHEMA_NAME, "Samples", reportCategory, "View All Samples", cache);
            item1.setOwnerKey(owner1.getPropertyManagerKey());
            items.add(item1);

            DetailsUrlWithoutLabelNavItem item2 = new DetailsUrlWithoutLabelNavItem(this, "Freezer Summary", DetailsURL.fromString("/laboratory/freezerSummary.view", c), LaboratoryService.NavItemCategory.reports, reportCategory);
            item2.setOwnerKey(owner1.getPropertyManagerKey());
            items.add(item2);

            items.add(new ReportItem(this, null, LaboratoryModule.SCHEMA_NAME, "samples_by_subjectid_and_type", reportCategory, "Samples By Subject Id And Type", cache));
            items.add(new ReportItem(this, null, LaboratoryModule.SCHEMA_NAME, "samples_by_subjectid_date_and_type", reportCategory, "Samples By Subject Id, Sample Date And Type", cache));

            QueryImportNavItem owner2 = new QueryImportNavItem(this, LaboratoryModule.SCHEMA_NAME, "DNA_Oligos", LaboratoryService.NavItemCategory.samples, reportCategory, cache);
            ReportItem item3 = new ReportItem(this, null, LaboratoryModule.SCHEMA_NAME, "DNA_Oligos", reportCategory, "View All DNA Oligos", cache);
            item3.setOwnerKey(owner2.getPropertyManagerKey());
            items.add(item3);

            QueryImportNavItem owner3 = new QueryImportNavItem(this, LaboratoryModule.SCHEMA_NAME, "Peptides", LaboratoryService.NavItemCategory.samples, reportCategory, cache);
            ReportItem item4 = new ReportItem(this, null, LaboratoryModule.SCHEMA_NAME, "Peptides", reportCategory, "View All Peptides", cache);
            item4.setOwnerKey(owner3.getPropertyManagerKey());
            items.add(item4);

            for (Study study : StudyService.get().getAllStudies(c, u))
            {
                items.add(new DetailsUrlWithoutLabelNavItem(this, study.getLabel(), DetailsURL.fromString("/study-begin.view", study.getContainer()), LaboratoryService.NavItemCategory.reports, "Studies"));
            }

//            FileContentService service = FileContentService.get();
//            for (AttachmentDirectory dir : service.getRegisteredDirectories(c))
//            {
//                items.add(new DetailsUrlWithoutLabelNavItem(this, DetailsURL.fromString("/filecontent/begin.view?fileSetName=" + dir.getName(), c), dir.getLabel(), "Filesets"));
//            }

            DetailsUrlWithoutLabelNavItem item5 = new DetailsUrlWithoutLabelNavItem(this, "Data Browser", DetailsURL.fromString("/laboratory/dataBrowser.view", c), LaboratoryService.NavItemCategory.reports, "General");
            items.add(item5);

            QueryImportNavItem owner4 = new QueryImportNavItem(this, LaboratoryModule.SCHEMA_NAME, "Antibodies", LaboratoryService.NavItemCategory.samples, reportCategory, cache);
            ReportItem item6 = new ReportItem(this, null, LaboratoryModule.SCHEMA_NAME, "Antibodies", reportCategory, "View All Antibodies", cache);
            item6.setOwnerKey(owner4.getPropertyManagerKey());
            items.add(item6);

            DetailsUrlWithoutLabelNavItem owner5 = new DetailsUrlWithoutLabelNavItem(this, "Major Events", new DetailsURL(QueryService.get().urlFor(u, c, QueryAction.executeQuery, LaboratoryModule.SCHEMA_NAME, LaboratorySchema.TABLE_MAJOR_EVENTS)), LaboratoryService.NavItemCategory.misc, "Misc");
            ReportItem item7 = new ReportItem(this, null, LaboratoryModule.SCHEMA_NAME, LaboratorySchema.TABLE_MAJOR_EVENTS, reportCategory, "View All Major Events", cache);
            item7.setOwnerKey(owner5.getPropertyManagerKey());
            items.add(item7);
        }

        return Collections.unmodifiableList(items);
    }

    @Override
    public List<NavItem> getSettingsItems(Container c, User u)
    {
        List<NavItem> items = new ArrayList<>();
        String categoryName = "Samples";
        String general = "General Settings";
        String adminSettings = "Site Administration";

        if (ContainerManager.getSharedContainer().equals(c))
        {
            items.add(new SimpleSettingsItem(this, LaboratoryModule.SCHEMA_NAME, "Cell_Type", categoryName, "Allowable Cell Types"));
            items.add(new SimpleSettingsItem(this, LaboratoryModule.SCHEMA_NAME, "DNA_Mol_Type", categoryName, "Allowable DNA Molecule Types"));
            items.add(new SimpleSettingsItem(this, LaboratoryModule.SCHEMA_NAME, "Reference_Peptides", categoryName, "Reference Peptides"));
            items.add(new SimpleSettingsItem(this, LaboratoryModule.SCHEMA_NAME, "Genders", categoryName, "Allowable Genders"));
            items.add(new SimpleSettingsItem(this, LaboratoryModule.SCHEMA_NAME, "Geographic_Origins", categoryName, "Allowable Geographic Origins"));
            items.add(new SimpleSettingsItem(this, LaboratoryModule.SCHEMA_NAME, "Sample_Additive", categoryName, "Allowable Sample Additives"));
            items.add(new SimpleSettingsItem(this, LaboratoryModule.SCHEMA_NAME, "Species", categoryName, "Allowable Species"));

            if (u.hasSiteAdminPermission())
            {
                items.add(new DetailsUrlWithoutLabelNavItem(this, "Synchronize Assay Fields", DetailsURL.fromString("/laboratory/synchronizeAssayFields.view", ContainerManager.getRoot()), LaboratoryService.NavItemCategory.settings, adminSettings));
                items.add(new DetailsUrlWithoutLabelNavItem(this, "Reset Tabs and Webparts", DetailsURL.fromString("/laboratory/resetLaboratoryFolders.view", ContainerManager.getRoot()), LaboratoryService.NavItemCategory.settings, adminSettings));
                items.add(new DetailsUrlWithoutLabelNavItem(this, "Initialize Workbooks", DetailsURL.fromString("/laboratory/initWorkbooks.view", ContainerManager.getRoot()), LaboratoryService.NavItemCategory.settings, adminSettings));
                items.add(new DetailsUrlWithoutLabelNavItem(this, "Initialize Autoincrementing Tables", DetailsURL.fromString("/laboratory/initContainerIncrementingTable.view", ContainerManager.getRoot()), LaboratoryService.NavItemCategory.settings, adminSettings));
                items.add(new DetailsUrlWithoutLabelNavItem(this, "Ensure Indexes Exist", DetailsURL.fromString("/laboratory/ensureIndexes.view", ContainerManager.getRoot()), LaboratoryService.NavItemCategory.settings, adminSettings));
            }
        }
        else
        {
            if (c.getActiveModules().contains(getOwningModule()))
            {
                items.add(new SimpleSettingsItem(this, LaboratoryModule.SCHEMA_NAME, "Freezers", categoryName, "Manage Freezers"));

                items.add(new DetailsUrlWithoutLabelNavItem(this, "Control Item Visibility", DetailsURL.fromString("/laboratory/itemVisibility.view", c), LaboratoryService.NavItemCategory.settings, general));
                items.add(new DetailsUrlWithoutLabelNavItem(this, "Control Item Default Views", DetailsURL.fromString("/laboratory/itemDefaultViews.view", c), LaboratoryService.NavItemCategory.settings, general));
                items.add(new DetailsUrlWithoutLabelNavItem(this, "Customize Data Browser", DetailsURL.fromString("/laboratory/customizeDataBrowser.view", c), LaboratoryService.NavItemCategory.settings, general));
                items.add(new DetailsUrlWithoutLabelNavItem(this, "Manage Notifications", DetailsURL.fromString("/ldk/notificationAdmin.view", c), LaboratoryService.NavItemCategory.settings, general));
                items.add(new DetailsUrlWithoutLabelNavItem(this, "Set Assay Defaults", DetailsURL.fromString("/laboratory/assayDefaults.view", c), LaboratoryService.NavItemCategory.settings, general));
                items.add(new DetailsUrlWithoutLabelNavItem(this, "Populate Default Values", DetailsURL.fromString("/laboratory/populateInitialValues.view", c), LaboratoryService.NavItemCategory.settings, general));

                items.add(new SimpleSettingsItem(this, LaboratoryModule.SCHEMA_NAME, "Sample_Type", "Samples", "Allowable Sample Types"));

                if (u.hasSiteAdminPermission())
                {
                    items.add(new DetailsUrlWithoutLabelNavItem(this, "Synchronize Assay Fields", DetailsURL.fromString("/laboratory/synchronizeAssayFields.view", c), LaboratoryService.NavItemCategory.settings, adminSettings));
                    items.add(new DetailsUrlWithoutLabelNavItem(this, "Reset Tabs and Webparts", DetailsURL.fromString("/laboratory/resetLaboratoryFolders.view", c), LaboratoryService.NavItemCategory.settings, adminSettings));
                    items.add(new DetailsUrlWithoutLabelNavItem(this, "Initialize Workbooks", DetailsURL.fromString("/laboratory/initWorkbooks.view", c), LaboratoryService.NavItemCategory.settings, adminSettings));
                }
            }
        }

        //these are always available
        if (c.getActiveModules().contains(getOwningModule()))
        {
            String name = "Manage " + (ContainerManager.getSharedContainer().equals(c) ? "Default " : "") + "Data and Demographics Sources";
            items.add(new DetailsUrlWithoutLabelNavItem(this, name, DetailsURL.fromString("/laboratory/manageDataSources.view", c), LaboratoryService.NavItemCategory.settings, general));
        }

        return Collections.unmodifiableList(items);
    }

    @Override
    public List<NavItem> getMiscItems(Container c, User u)
    {
        List<NavItem> items = new ArrayList<>();
        if (c.getActiveModules().contains(getOwningModule()))
        {
            items.add(new DetailsUrlWithoutLabelNavItem(this, "Manage Subjects and Groups", DetailsURL.fromString("/laboratory/manageSubjects.view", c), LaboratoryService.NavItemCategory.misc, "Subjects and Projects"));
            items.add(new DetailsUrlWithoutLabelNavItem(this, "Major Events", new DetailsURL(QueryService.get().urlFor(u, c, QueryAction.executeQuery, LaboratoryModule.SCHEMA_NAME, LaboratorySchema.TABLE_MAJOR_EVENTS)), LaboratoryService.NavItemCategory.misc, LaboratoryService.NavItemCategory.misc.name()));
        }

        return Collections.unmodifiableList(items);
    }

    @Override
    public JSONObject getTemplateMetadata(ViewContext ctx)
    {
        return new JSONObject();
    }

    @Override @NotNull
    public Set<ClientDependency> getClientDependencies()
    {
        return Collections.emptySet();
    }

    @Override
    public Module getOwningModule()
    {
        return _module;
    }

    @Override
    public List<SummaryNavItem> getSummary(Container c, User u)
    {
        List<SummaryNavItem> items = new ArrayList<>();

        for (NavItem nav : getSampleNavItems(c, u))
        {
            if (nav.isVisible(c, u))
            {
                if (nav.getName().equalsIgnoreCase("Samples"))
                {
                    QueryImportNavItem item = ((QueryImportNavItem)nav);
                    items.add(new SamplesCountNavItem(this, item.getSchema(), item.getQuery(), item.getItemType(), item.getReportCategory(), item.getLabel()));
                }
                else
                {
                    QueryImportNavItem item = ((QueryImportNavItem)nav);
                    items.add(new QueryCountNavItem(this, item.getSchema(), item.getQuery(), item.getItemType(), item.getReportCategory(), item.getLabel()));
                }
            }
        }

        return Collections.unmodifiableList(items);
    }

    @Override
    public List<NavItem> getSubjectIdSummary(Container c, User u, String subjectId)
    {
        List<NavItem> items = new ArrayList<>();

        for (NavItem nav : getSampleNavItems(c, u))
        {
            if (nav.isVisible(c, u) && nav instanceof QueryImportNavItem item)
            {
                UserSchema us = item.getQueryCache().getUserSchema(c, u, item.getSchema());
                if (us != null)
                {
                    TableInfo ti = item.getQueryCache().getTableInfo(c, u, item.getSchema(), item.getQuery());
                    if (ti != null)
                    {
                        ColumnInfo ci = getSubjectColumn(ti);
                        if (ci != null)
                        {
                            QueryCountNavItem qc = new QueryCountNavItem(this, item.getSchema(), item.getQuery(), item.getItemType(), item.getReportCategory(), item.getLabel());
                            qc.setQueryCache(item.getQueryCache());
                            qc.setFilter(new SimpleFilter(ci.getFieldKey(), subjectId));
                            items.add(qc);
                        }
                    }
                }
            }
        }

        return Collections.unmodifiableList(items);
    }

    @Override
    public List<TabbedReportItem> getTabbedReportItems(Container c, User u)
    {
        if (!c.getActiveModules().contains(getOwningModule()))
        {
            return Collections.emptyList();
        }

        List<TabbedReportItem> items = new ArrayList<>();
        QueryCache cache = new QueryCache();

        NavItem nav = getReportItems(c, u).get(0);
        TabbedReportItem item = new QueryTabbedReportItem(cache, this, LaboratoryModule.SCHEMA_NAME, "Samples", "Samples", "Samples");
        item.setQueryCache(cache);
        item.setVisible(nav.isVisible(c, u));
        item.setOwnerKey(nav.getPropertyManagerKey());
        items.add(item);


        TabbedReportItem subSummary = new JSTabbedReportItem(this, "subjectSummary", "Subject Summary", "General", "subjectSummary");
        subSummary.setQueryCache(cache);
        items.add(subSummary);

        DetailsUrlWithoutLabelNavItem owner = new DetailsUrlWithoutLabelNavItem(this, "Major Events", new DetailsURL(QueryService.get().urlFor(u, c, QueryAction.executeQuery, LaboratoryModule.SCHEMA_NAME, LaboratorySchema.TABLE_MAJOR_EVENTS)), LaboratoryService.NavItemCategory.misc, LaboratoryService.NavItemCategory.misc.name());
        TabbedReportItem item2 = new QueryTabbedReportItem(cache, this, LaboratoryModule.SCHEMA_NAME, LaboratorySchema.TABLE_MAJOR_EVENTS, "Major Events", "General");
        item2.setQueryCache(cache);
        item2.setVisible(owner.isVisible(c, u));
        item2.setOwnerKey(owner.getPropertyManagerKey());
        items.add(item2);

        List<TabbedReportModel> models = getTabbedReports(c, u);
        if (!models.isEmpty())
        {
            items.addAll(models.stream().map(m -> {
                if (!m.isValid())
                {
                    return null;
                }

                return(m.toNavItem(c, u, cache, owner, this));
            }).filter(Objects::nonNull).toList());
        }

        return Collections.unmodifiableList(items);
    }

    private synchronized Cache<String, List<TabbedReportModel>> getCache()
    {
        if (_cache == null)
        {
            _cache = CacheManager.getCache(String.class, 1000, CacheManager.UNLIMITED, "LaboratoryDataProviderCache");
        }

        return _cache;
    }

    private synchronized List<TabbedReportModel> getTabbedReports(Container c, User u)
    {
        c = c.isWorkbookOrTab() ? c.getParent() : c;

        List<TabbedReportModel> models = getCache().get(c.getId());
        if (models != null)
            return models;

        UserSchema us = QueryService.get().getUserSchema(u, c, LaboratoryModule.SCHEMA_NAME);
        if (us == null)
        {
            return Collections.emptyList();
        }

        models = new ArrayList<>(new TableSelector(us.getTable(LaboratorySchema.TABLE_REPORTS)).getArrayList(TabbedReportModel.class));
        models = Collections.unmodifiableList(models);
        getCache().put(c.getId(), models);

        return models;
    }

    public static class TabbedReportModel
    {
        private int _rowId;
        private String _category;
        private String _reportType;
        private String _reportTitle;
        private String _sortOrder;
        private String _containerPath;
        private String _schemaName;
        private String _queryName;
        private String _viewName;
        private String _reportPath;
        private String _subjectFieldName;
        private String _description;

        public boolean isValid()
        {
            if (getCategory() == null || getSchemaName() == null || getQueryName() == null)
            {
                _log.error("Missing either category, schema or query for laboratory.reports item: " + getContainerPath() + ", rowId: " + getRowId());
                return false;
            }

            if (getContainerPath() != null)
            {
                Container target = ContainerManager.getForPath(getContainerPath());
                if (target == null)
                {
                    _log.error("Unknown container for saved TabbedReportItem: " + getContainerPath() + ", rowId: " + getRowId());
                    return false;
                }
            }

            return true;
        }

        public TabbedReportItem toNavItem(Container c, User u, QueryCache cache, NavItem owner, DataProvider provider)
        {
            QueryTabbedReportItem report = new QueryTabbedReportItem(cache, provider, getSchemaName(), getQueryName(), getReportTitle() == null ? getQueryName() : getReportTitle(), getCategory());
            report.setQueryCache(cache);
            if (getSubjectFieldName() != null)
            {
                report.setSubjectIdFieldKey(FieldKey.fromString(getSubjectFieldName()));
            }

            if (getContainerPath() != null)
            {
                Container target = ContainerManager.getForPath(getContainerPath());
                if (target != null)
                {
                    report.setTargetContainer(target);
                }
            }

            if (getViewName() != null)
            {
                report.setViewName(getViewName());
            }

            report.setVisible(owner.isVisible(c, u));
            report.setOwnerKey(owner.getPropertyManagerKey());

            return report;
        }

        public int getRowId()
        {
            return _rowId;
        }

        public void setRowId(int rowId)
        {
            _rowId = rowId;
        }

        public String getCategory()
        {
            return _category;
        }

        public void setCategory(String category)
        {
            _category = category;
        }

        public String getReportType()
        {
            return _reportType;
        }

        public void setReportType(String reportType)
        {
            _reportType = reportType;
        }

        public String getReportTitle()
        {
            return _reportTitle;
        }

        public void setReportTitle(String reportTitle)
        {
            _reportTitle = reportTitle;
        }

        public String getSortOrder()
        {
            return _sortOrder;
        }

        public void setSortOrder(String sortOrder)
        {
            _sortOrder = sortOrder;
        }

        public String getContainerPath()
        {
            return _containerPath;
        }

        public void setContainerPath(String containerPath)
        {
            _containerPath = containerPath;
        }

        public String getSchemaName()
        {
            return _schemaName;
        }

        public void setSchemaName(String schemaName)
        {
            _schemaName = schemaName;
        }

        public String getQueryName()
        {
            return _queryName;
        }

        public void setQueryName(String queryName)
        {
            _queryName = queryName;
        }

        public String getViewName()
        {
            return _viewName;
        }

        public void setViewName(String viewName)
        {
            _viewName = viewName;
        }

        public String getReportPath()
        {
            return _reportPath;
        }

        public void setReportPath(String reportPath)
        {
            _reportPath = reportPath;
        }

        public String getSubjectFieldName()
        {
            return _subjectFieldName;
        }

        public void setSubjectFieldName(String subjectFieldName)
        {
            _subjectFieldName = subjectFieldName;
        }

        public String getDescription()
        {
            return _description;
        }

        public void setDescription(String description)
        {
            _description = description;
        }
    }
}
