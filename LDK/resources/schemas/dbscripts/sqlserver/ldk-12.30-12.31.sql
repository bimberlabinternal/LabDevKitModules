CREATE TABLE ldk.ldapSyncMap (
  rowid int identity(1,1),
  provider varchar(1000),
  sourceId varchar(1000),
  labkeyId int,
  type char(1),

  created datetime
);