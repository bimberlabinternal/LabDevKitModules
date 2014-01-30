/*
 * Copyright (c) 2010-2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
PARAMETERS(StartDate TIMESTAMP, NumMonths INTEGER)

SELECT
  convert((year(StartDate) + i.value), INTEGER) as year,
  m.monthName,
  m.monthNum,
  CAST((cast(m.monthNum as varchar(2)) || '/1/' || CAST((year(StartDate) + i.value) as varchar(10))) as date) as date,
  (i.value *12) + m.monthNum as monthsSinceStart

--build a base by year, with 1 row per month
FROM ldk.integers i
CROSS JOIN ldk.months m

WHERE ((i.value *12) + m.monthNum) <= NumMonths
