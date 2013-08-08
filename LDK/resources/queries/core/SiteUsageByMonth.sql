/*
 * Copyright (c) 2010-2011 LabKey Corporation
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
SELECT

year(a.date) as year,
month(a.date) as month,
cast(cast(year(a.date) as varchar) || '-' || cast(month(a.date) as varchar) || '-01' as DATE) as date,
count(*) AS Logins

FROM "/".auditlog.audit a
--FROM audit_pg.auditLog a

WHERE a.EventType = 'UserAuditEvent'
AND a.Comment LIKE '%logged in%'

GROUP BY year(a.date), month(a.date)