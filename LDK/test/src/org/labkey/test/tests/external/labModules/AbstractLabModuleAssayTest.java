/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
package org.labkey.test.tests.external.labModules;

import org.labkey.test.TestProperties;
import org.labkey.test.tests.external.labModules.LabModulesTest;

import java.text.SimpleDateFormat;

abstract public class AbstractLabModuleAssayTest extends LabModulesTest
{
    protected final static SimpleDateFormat _dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm");
    protected final static SimpleDateFormat _dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void checkLinks()
    {
        if ( TestProperties.isLinkCheckEnabled() )
            log("LabModulesTest uses essentially the same UI and will check links, so link checking is skipped");
    }
}
