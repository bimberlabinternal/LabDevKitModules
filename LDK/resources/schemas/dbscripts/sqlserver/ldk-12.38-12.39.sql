--this ensures we preserve data prior to dropping this table
CREATE PROCEDURE ldk.handleUpgrade AS
BEGIN
IF EXISTS( SELECT name from sys.tables WHERE name = 'ldapSyncMap' AND schema_id = (SELECT schema_id FROM sys.schemas WHERE name = 'openldapsync' ) )
BEGIN
INSERT INTO openldapsync.ldapSyncMap (provider, sourceId, labkeyId, type, created, container)
  SELECT provider, sourceId, labkeyId, type, created, (select entityid from core.containers WHERE name IS NULL) as container
  FROM ldk.ldapSyncMap;
END
END;
GO

EXEC ldk.handleUpgrade
GO

DROP PROCEDURE ldk.handleUpgrade
DROP TABLE ldk.ldapSyncMap;