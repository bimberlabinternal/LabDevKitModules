-- Cleanup legacy orphan rows
delete from laboratory.workbooks WHERE (SELECT c.entityid from core.containers c where c.entityid = container) is null;
