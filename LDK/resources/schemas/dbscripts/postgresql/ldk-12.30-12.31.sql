CREATE TABLE ldk.ldapSyncMap (
  rowid serial,
  provider varchar(1000),
  sourceId varchar(1000),
  labkeyId int,
  type char(1),

  created timestamp
);