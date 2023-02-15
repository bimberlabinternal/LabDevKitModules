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
import org.json.JSONObject;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpSampleType;
import org.labkey.api.exp.api.SampleTypeService;
import org.labkey.api.laboratory.AbstractDataProvider;
import org.labkey.api.laboratory.LaboratoryService;
import org.labkey.api.laboratory.QueryCountNavItem;
import org.labkey.api.laboratory.SummaryNavItem;
import org.labkey.api.laboratory.NavItem;
import org.labkey.api.module.Module;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.template.ClientDependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * User: bimber
 * Date: 10/8/12
 * Time: 9:06 AM
 */
public class SampleTypeDataProvider extends AbstractDataProvider
{
    public static final String NAME = "SampleTypeDataProvider";

    public SampleTypeDataProvider()
    {
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
        //also append all sample types in this container
        List<NavItem> navItems = new ArrayList<NavItem>();

        for (ExpSampleType st : SampleTypeService.get().getSampleTypes(c, u, true))
        {
            navItems.add(new SampleTypeNavItem(this, LaboratoryService.NavItemCategory.samples, st));
        }
        return navItems;
    }

    @Override
    public List<NavItem> getSettingsItems(Container c, User u)
    {
        return Collections.emptyList();
    }

    @Override
    public JSONObject getTemplateMetadata(ViewContext ctx)
    {
        return new JSONObject();
    }

    @Override
    @NotNull
    public Set<ClientDependency> getClientDependencies()
    {
        return Collections.emptySet();
    }

    @Override
    public Module getOwningModule()
    {
        return null;
    }

    @Override
    public List<SummaryNavItem> getSummary(Container c, User u)
    {
        List<SummaryNavItem> items = new ArrayList<>();
        for (ExpSampleType st : SampleTypeService.get().getSampleTypes(c, u, true))
        {
            SampleTypeNavItem nav = new SampleTypeNavItem(this, LaboratoryService.NavItemCategory.samples, st);
            if (nav.isVisible(c, u))
            {
                items.add(new QueryCountNavItem(this, "Samples", st.getName(), LaboratoryService.NavItemCategory.samples, "Samples", st.getName()));
            }
        }

        return items;
    }

    @Override
    public List<NavItem> getSubjectIdSummary(Container c, User u, String subjectId)
    {
        List<NavItem> items = new ArrayList<NavItem>();
        for (ExpSampleType st : SampleTypeService.get().getSampleTypes(c, u, true))
        {
            UserSchema us = QueryService.get().getUserSchema(u, c, "Samples");
            if (us != null)
            {
                TableInfo ti = us.getTable(st.getName());
                if (ti != null)
                {
                    ColumnInfo ci = getSubjectColumn(ti);
                    if (ci != null)
                    {
                        QueryCountNavItem qc = new QueryCountNavItem(this, "Samples", st.getName(), LaboratoryService.NavItemCategory.samples, "Samples", st.getName());
                        qc.setFilter(new SimpleFilter(FieldKey.fromString(ci.getName()), subjectId));
                        items.add(qc);
                    }
                }
            }
        }

        return items;
    }
}
