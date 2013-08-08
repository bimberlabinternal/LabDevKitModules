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

cast(a.date as date) as date,
CASE
  WHEN dayofweek(cast(a.date as date)) = 1 THEN 'Sunday'
  WHEN dayofweek(cast(a.date as date)) = 2 THEN 'Monday'
  WHEN dayofweek(cast(a.date as date)) = 3 THEN 'Tuesday'
  WHEN dayofweek(cast(a.date as date)) = 4 THEN 'Wednesday'
  WHEN dayofweek(cast(a.date as date)) = 5 THEN 'Thursday'
  WHEN dayofweek(cast(a.date as date)) = 6 THEN 'Friday'
  WHEN dayofweek(cast(a.date as date)) = 7 THEN 'Saturday'
END as dayOfWeek,
count(*) AS Logins

FROM "/".auditlog.audit a

WHERE a.EventType = 'UserAuditEvent'
AND a.Comment LIKE '%logged in%'
GROUP BY cast(a.date as date)