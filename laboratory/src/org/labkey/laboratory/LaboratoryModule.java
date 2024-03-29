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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.laboratory.LaboratoryService;
import org.labkey.api.laboratory.security.LaboratoryAdminPermission;
import org.labkey.api.ldk.ExtendedSimpleModule;
import org.labkey.api.ldk.LDKService;
import org.labkey.api.ldk.buttons.ShowBulkEditButton;
import org.labkey.api.ldk.notification.NotificationService;
import org.labkey.api.ldk.table.SimpleButtonConfigFactory;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.security.permissions.AdminOperationsPermission;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.security.roles.RoleManager;
import org.labkey.api.settings.AdminConsole;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.BaseWebPartFactory;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.Portal;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.WebPartView;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.api.writer.ContainerUser;
import org.labkey.laboratory.notification.LabSummaryNotification;
import org.labkey.laboratory.query.LaboratoryDemographicsProvider;
import org.labkey.laboratory.query.WorkbookModel;
import org.labkey.laboratory.security.LaboratoryAdminRole;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * User: bimber
 * Date: 9/1/12
 * Time: 8:14 PM
 */
public class LaboratoryModule extends ExtendedSimpleModule
{
    public static final String NAME = "Laboratory";
    public static final String CONTROLLER_NAME = "laboratory";
    public static final String SCHEMA_NAME = "laboratory";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public @Nullable Double getSchemaVersion()
    {
        return 12.305;
    }

    @Override
    @NotNull
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return Arrays.asList(
            new BaseWebPartFactory("Workbook Header")
            {
                @Override
                public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    if (!portalCtx.getContainer().isWorkbook())
                    {
                        return new HtmlView(HtmlString.of("This container is not a workbook"));
                    }

                    WorkbookModel model = LaboratoryManager.get().getWorkbookModel(portalCtx.getContainer(), true);
                    if (model == null)
                    {
                        model = WorkbookModel.createNew(portalCtx.getContainer());
                    }

                    JspView<WorkbookModel> view = new JspView<>("/org/labkey/laboratory/view/workbookHeader.jsp", model);
                    view.setTitle("Workbook Header");
                    view.setFrame(WebPartView.FrameType.NONE);
                    return view;
                }

                @Override
                public boolean isAvailable(Container c, String scope, String location)
                {
                    return false;
                }
            },
            new BaseWebPartFactory("Laboratory Data Browser")
            {
                @Override
                public WebPartView getWebPartView(@NotNull ViewContext portalCtx, @NotNull Portal.WebPart webPart)
                {
                    JspView<Object> view = new JspView<>("/org/labkey/laboratory/view/dataBrowser.jsp", new Object());
                    view.setTitle("Laboratory Data Browser");
                    //view.setFrame(WebPartView.FrameType.NONE);

                    if (portalCtx.getContainer().hasPermission(portalCtx.getUser(), LaboratoryAdminPermission.class) || portalCtx.getContainer().hasPermission(portalCtx.getUser(), AdminPermission.class))
                    {
                        NavTree customize = new NavTree("");
                        //customize.setScript(getWrappedOnClick(webPart, _webPartDef.getCustomizeHandler()));
                        customize.setHref(DetailsURL.fromString("/laboratory/customizeDataBrowser.view", portalCtx.getContainer()).getActionURL().toString());
                        view.setCustomize(customize);
                    }

                    return view;
                }

                @Override
                public String getDisplayName(Container container, String location)
                {
                    return "Laboratory Data Browser";
                }

                @Override
                public boolean isAvailable(Container c, String scope, String location)
                {
                    return WebPartFactory.LOCATION_BODY.equals(location);
                }
            }
        );
    }

    @Override
    protected void init()
    {
        addController(CONTROLLER_NAME, LaboratoryController.class);

        LaboratoryService.setInstance(LaboratoryServiceImpl.get());
    }

    @Override
    protected void doStartupAfterSpringConfig(ModuleContext moduleContext)
    {
        RoleManager.registerRole(new LaboratoryAdminRole());

        LaboratoryService.get().registerDataProvider(new LaboratoryDataProvider(this));
        LaboratoryService.get().registerDataProvider(new SampleTypeDataProvider());
        LaboratoryService.get().registerDataProvider(new ExtraDataSourcesDataProvider(this));
        LaboratoryService.get().registerDemographicsProvider(new LaboratoryDemographicsProvider());

        DetailsURL details = DetailsURL.fromString("/laboratory/siteLabSettings.view");
        details.setContainerContext(ContainerManager.getSharedContainer());
        AdminConsole.addLink(AdminConsole.SettingsLinkType.Management, "discvr admin", details.getActionURL(), AdminOperationsPermission.class);
        NotificationService.get().registerNotification(new LabSummaryNotification(this));

        LaboratoryService.get().registerTableIndex("core", "containers", Arrays.asList("RowId", "Parent", "EntityId", "Type"));
        LaboratoryService.get().registerTableIndex("exp", "data", Arrays.asList("RowId", "RunId", "Container"));

        LDKService.get().registerContainerScopedTable(SCHEMA_NAME, LaboratorySchema.TABLE_SAMPLE_TYPE, "type");
        LDKService.get().registerContainerScopedTable(SCHEMA_NAME, LaboratorySchema.TABLE_FREEZERS, "name");
        LDKService.get().registerContainerScopedTable(SCHEMA_NAME, LaboratorySchema.TABLE_SUBJECTS, "subjectname");

        SimpleButtonConfigFactory btn1 = new SimpleButtonConfigFactory(this, "Mark Removed", "Laboratory.buttonHandlers.markSamplesRemoved(dataRegionName, arguments[0])");
        btn1.setClientDependencies(ClientDependency.supplierFromModuleName("laboratory"));
        btn1.setPermission(UpdatePermission.class);
        LDKService.get().registerQueryButton(btn1, LaboratoryModule.SCHEMA_NAME, LaboratorySchema.TABLE_SAMPLES);

        SimpleButtonConfigFactory btn2 = new SimpleButtonConfigFactory(this, "Duplicate/Derive Samples", "Laboratory.buttonHandlers.deriveSamples(dataRegionName, arguments[0])");
        btn2.setClientDependencies(ClientDependency.supplierFromModuleName("laboratory"));
        btn2.setPermission(UpdatePermission.class);
        LDKService.get().registerQueryButton(btn2, LaboratoryModule.SCHEMA_NAME, LaboratorySchema.TABLE_SAMPLES);

        SimpleButtonConfigFactory btn4 = new SimpleButtonConfigFactory(this, "Append Comment", "Laboratory.buttonHandlers.appendCommentToSamples(dataRegionName, arguments[0])");
        btn4.setClientDependencies(ClientDependency.supplierFromModuleName("laboratory"));
        btn4.setPermission(UpdatePermission.class);
        LDKService.get().registerQueryButton(btn4, LaboratoryModule.SCHEMA_NAME, LaboratorySchema.TABLE_SAMPLES);

        LDKService.get().registerQueryButton(new ShowBulkEditButton(this, LaboratoryModule.SCHEMA_NAME, LaboratorySchema.TABLE_SAMPLES), LaboratoryModule.SCHEMA_NAME, LaboratorySchema.TABLE_SAMPLES);
    }

    @Override
    protected void registerContainerListeners()
    {
        ContainerManager.addContainerListener(new LaboratoryContainerListener(this));
    }

    @Override
    protected void registerSchemas()
    {
        LaboratoryUserSchema.register(this);
        ExtraDataSourcesUserSchema.register(this);
    }

    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return PageFlowUtil.set(SCHEMA_NAME);
    }

    @Override
    public JSONObject getPageContextJson(ContainerUser context)
    {
        JSONObject ret = super.getPageContextJson(context);
        ret.put("isLaboratoryAdmin", context.getContainer().hasPermission(context.getUser(), LaboratoryAdminPermission.class));

        return ret;
    }

    @Override
    public UpgradeCode getUpgradeCode()
    {
        return new LaboratoryUpgradeCode();
    }

    @Override
    public @NotNull Set<Class> getIntegrationTests()
    {
        return PageFlowUtil.set(WorkbookTestCase.class);
    }
}
