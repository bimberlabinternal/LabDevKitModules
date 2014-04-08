/*
 * Copyright (c) 2013 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
--this query displays all animals co-housed with each housing record
--to be considered co-housed, they only need to overlap by any period of time

PARAMETERS(StartDate TIMESTAMP, EndDate TIMESTAMP)

SELECT
  e.Id,
  e.date,
  e.project,
  e.procedureId,
  e.chargetype,
  e.objectid as sourceRecord,
  e.taskid

FROM study.encounters e
LEFT JOIN onprc_billing.procedureFeeDefinition p ON (
  p.procedureId = e.procedureId and
  --we want to either have the chargeType match between tables, or allow NULL to act like a wildcard
  (e.chargetype = p.chargetype OR (p.chargetype IS NULL AND e.chargetype != 'Not Billable')) AND
  p.active = true
)

WHERE e.dateOnly >= CAST(StartDate as date) AND e.dateOnly <= CAST(EndDate as date)

--we want to capture surgeries that would not normally get billed, based on procedure/chargetype
and p.procedureid IS NULL
and e.procedureid IS NOT NULL
AND (e.procedureid.category = 'Surgery' OR e.procedureid.category = 'Pathology')
and (e.chargetype is null or e.chargetype != 'No Charge')
and (e.project IS NULL OR e.project.displayName != javaConstant('org.labkey.onprc_ehr.ONPRC_EHRManager.BASE_GRANT_PROJECT'))