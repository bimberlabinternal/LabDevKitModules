--this ensures we preserve data prior to dropping this table
CREATE FUNCTION ldk.handleUpgrade() RETURNS VOID AS $$
DECLARE
BEGIN
  IF EXISTS ( SELECT * FROM information_schema.tables WHERE table_schema = 'openldapsync' AND table_name = 'ldapSyncMap' )
  THEN
    INSERT INTO openldapsync.ldapSyncMap (provider, sourceId, labkeyId, type, created, container)
      SELECT provider, sourceId, labkeyId, type, created, (select entityid from core.containers WHERE name IS NULL) as container
      FROM ldk.ldapSyncMap;
  END IF;
END;
$$ LANGUAGE plpgsql;

SELECT ldk.handleUpgrade();

DROP FUNCTION ldk.handleUpgrade();

DROP TABLE ldk.ldapSyncMap;