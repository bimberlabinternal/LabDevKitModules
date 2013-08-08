/*
 * Copyright (c) 2012 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * This is a hack which can be used as a requiredScript on a buttonBar XML config, which causes Ext4 to load.  It should
 * only be used when no better option exists.  When QueryView / ButtonBars feeds directly into ClientDependencies this should be removed
 */
LABKEY.requiresExt4ClientAPI();