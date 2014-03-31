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

package org.labkey.ldk;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.audit.AuditLogService;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.ldk.ExtendedSimpleModule;
import org.labkey.api.ldk.LDKService;
import org.labkey.api.ldk.notification.NotificationService;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.settings.AdminConsole;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.WebPartFactory;
import org.labkey.ldk.ldap.LdapScheduler;
import org.labkey.ldk.ldap.LdapSyncAuditProvider;
import org.labkey.ldk.ldap.LdapSyncAuditViewFactory;
import org.labkey.ldk.notification.NotificationServiceImpl;
import org.labkey.ldk.notification.SiteSummaryNotification;
import org.labkey.ldk.query.MssqlUtilsUserSchema;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class LDKModule extends ExtendedSimpleModule
{
    public static final String NAME = "LDK";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public double getVersion()
    {
        return 12.37;
    }

    @Override
    public boolean hasScripts()
    {
        return true;
    }

    @NotNull
    @Override
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return Collections.emptyList();
    }

    @Override
    protected void init()
    {
        addController("ldk", LDKController.class);
        LDKService.setInstance(new LDKServiceImpl());
        NotificationService.setInstance(new NotificationServiceImpl());
    }

    @Override
    protected void doStartupAfterSpringConfig(ModuleContext moduleContext)
    {
        AuditLogService.get().addAuditViewFactory(LdapSyncAuditViewFactory.getInstance());
        AuditLogService.registerAuditType(new LdapSyncAuditProvider());

        AdminConsole.addLink(AdminConsole.SettingsLinkType.Management, "notification service admin", DetailsURL.fromString("/ldk/notificationSiteAdmin.view").getActionURL());
        AdminConsole.addLink(AdminConsole.SettingsLinkType.Management, "ldap sync admin", DetailsURL.fromString("/ldk/ldapSettings.view").getActionURL());
        AdminConsole.addLink(AdminConsole.SettingsLinkType.Management, "file root usage summary", DetailsURL.fromString("/ldk/folderSizeSummary.view").getActionURL());

        if (DbScope.getLabkeyScope().getSqlDialect().isSqlServer())
        {
            AdminConsole.addLink(AdminConsole.SettingsLinkType.Management, "sql server DB index usage", DetailsURL.fromString("/query/executeQuery.view?schemaName=mssqlutils&query.queryName=index_stats").getActionURL());
        }

        LdapScheduler.get().schedule();

        NotificationService.get().registerNotification(new SiteSummaryNotification());
    }

    @NotNull
    @Override
    public Collection<String> getSummary(Container c)
    {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public Set<String> getSchemaNames()
    {
        return PageFlowUtil.set(LDKSchema.getInstance().getSchemaName());
    }

    @Override
    public UpgradeCode getUpgradeCode()
    {
        return new LDKUpgradeCode();
    }

    @Override
    protected void registerSchemas()
    {
        super.registerSchemas();

        if (DbScope.getLabkeyScope().getSqlDialect().isSqlServer())
        {
            MssqlUtilsUserSchema.register(this);
        }
    }
}
