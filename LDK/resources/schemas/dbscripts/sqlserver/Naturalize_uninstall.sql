/*
 * Copyright (c) 2017 LabKey Corporation. All rights reserved. No portion of this work may be reproduced in
 * any form or by any electronic or mechanical means without written permission from LabKey Corporation.
 */
IF object_id(N'ldk.Naturalize', N'FS') IS NOT NULL
DROP FUNCTION ldk.Naturalize
GO

IF  EXISTS (SELECT * FROM sys.assemblies asms WHERE asms.name = N'Naturalize' and is_user_defined = 1)
DROP ASSEMBLY [Naturalize]
GO