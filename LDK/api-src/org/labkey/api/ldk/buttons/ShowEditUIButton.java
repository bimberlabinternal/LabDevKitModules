/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
package org.labkey.api.ldk.buttons;

import org.labkey.api.data.TableInfo;
import org.labkey.api.ldk.LDKService;
import org.labkey.api.ldk.table.SimpleButtonConfigFactory;
import org.labkey.api.module.Module;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.util.PageFlowUtil;

import java.util.Map;

/**
 * User: bimber
 * Date: 7/14/13
 * Time: 4:05 PM
 */
public class ShowEditUIButton extends SimpleButtonConfigFactory
{
    protected String _schemaName;
    protected String _queryName;
    private Map<String, String> _urlParamMap = null;
    private boolean _copyFilters = true;

    protected Class<? extends Permission>[] _perms;

    @SafeVarargs
    public ShowEditUIButton(Module owner, String schemaName, String queryName, Class<? extends Permission>... perms)
    {
        this(owner, schemaName, queryName, "Edit Records", perms);
    }

    @SafeVarargs
    public ShowEditUIButton(Module owner, String schemaName, String queryName, String label, Class<? extends Permission>... perms)
    {
        super(owner, label, "");

        _schemaName = schemaName;
        _queryName = queryName;
        _perms = perms;
    }

    @Override
    public boolean isAvailable(TableInfo ti)
    {
        if (!super.isAvailable(ti))
            return false;

        for (Class<? extends Permission> perm : _perms)
        {
            if (!ti.getUserSchema().getContainer().hasPermission(ti.getUserSchema().getUser(), perm))
                return false;
        }

        return true;
    }

    public void setCopyFilters(boolean copyFilters)
    {
        _copyFilters = copyFilters;
    }

    public void setUrlParamMap(Map<String, String> urlParamMap)
    {
        _urlParamMap = urlParamMap;
    }

    protected String getHandlerName()
    {
        return "LDK.Utils.editUIButtonHandler";
    }

    @Override
    protected String getJsHandler(TableInfo ti)
    {
        String schema = _schemaName == null || LDKService.ALL_SCHEMAS.equals(_schemaName) ? ti.getPublicSchemaName() : _schemaName;
        String query = _queryName == null || LDKService.ALL_TABLES.equalsIgnoreCase(_queryName) ? ti.getPublicName() : _queryName;
        StringBuilder ret = new StringBuilder(getHandlerName() + "(" + PageFlowUtil.jsString(schema) + "," + PageFlowUtil.jsString(query) + ",dataRegionName, {");

        String delim = "";
        if (_urlParamMap != null)
        {
            for (String key : _urlParamMap.keySet())
            {
                ret.append(delim).append(PageFlowUtil.jsString(key)).append(":").append(PageFlowUtil.jsString(_urlParamMap.get(key)));
                delim = ",";
            }
        }

        ret.append("}, ").append(_copyFilters).append(");");

        return ret.toString();
    }
}
