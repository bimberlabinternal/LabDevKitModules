/*
 * Copyright (c) 2013-2014 LabKey Corporation
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

import org.labkey.api.ldk.table.SimpleButtonConfigFactory;
import org.labkey.api.module.Module;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.security.permissions.AdminPermission;

/**
 * User: bimber
 * Date: 7/14/13
 * Time: 4:05 PM
 */
public class ShowBulkEditButton extends SimpleButtonConfigFactory
{
    protected String _schemaName;
    protected String _queryName;

    public ShowBulkEditButton(Module owner, String schemaName, String queryName)
    {
        super(owner, "Bulk Edit", DetailsURL.fromString("/ldk/apiBulkEdit.view?schemaName=" + schemaName + "&queryName=" + queryName));
        setPermission(AdminPermission.class);
    }
}
