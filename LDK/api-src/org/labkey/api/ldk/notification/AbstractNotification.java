/*
 * Copyright (c) 2014 LabKey Corporation
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
package org.labkey.api.ldk.notification;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.module.Module;
import org.labkey.api.query.QueryUrls;
import org.labkey.api.settings.LookAndFeelProperties;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.api.view.ActionURL;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * User: bimber
 * Date: 1/7/14
 * Time: 5:30 PM
 */
abstract public class AbstractNotification implements Notification
{
    private final Module _owner;

    protected final static Logger log = LogHelper.getLogger(AbstractNotification.class, "LDK notification errors");
    protected final static SimpleDateFormat _timeFormat = new SimpleDateFormat("kk:mm");
    protected final static QueryUrls _queryUrls = PageFlowUtil.urlProvider(QueryUrls.class);

    public AbstractNotification(Module owner)
    {
        _owner = owner;

        // AbstractNotifications must be constructed after QueryUrls has been registered, typically in startup() or
        // doStartupAfterSpringConfig()
        assert _queryUrls != null;
    }

    @Override
    public boolean isAvailable(Container c)
    {
        return c.getActiveModules().contains(_owner);
    }

    protected String getExecuteQueryUrl(Container c, String schemaName, String queryName, @Nullable String viewName)
    {
        return getExecuteQueryUrl(c, schemaName, queryName, viewName, null);
    }

    /**
     * Returned URL always contains a couple parameters and therefore always has a "?". It should return an ActionURL,
     * but there is a lot of legacy URL string manipulation migrated into java, and it's not worth changing all of it
     * at this point.
     */
    protected String getExecuteQueryUrl(Container c, String schemaName, String queryName, @Nullable String viewName, @Nullable SimpleFilter filter)
    {
        ActionURL url = _queryUrls.urlExecuteQuery(c, schemaName, queryName);

        if (viewName != null)
            url.addParameter("query.viewName", viewName);

       if (filter != null)
            filter.applyToURL(url, "query");

        return url.getURIString(); // Return an absolute URL since links are sent via email
    }

    public DateFormat getDateFormat(Container c)
    {
        return new SimpleDateFormat(LookAndFeelProperties.getInstance(c).getDefaultDateFormat());
    }

    public DateFormat getDateTimeFormat(Container c)
    {
        return new SimpleDateFormat(LookAndFeelProperties.getInstance(c).getDefaultDateTimeFormat());
    }

    public Module getOwnerModule()
    {
        return _owner;
    }
}
