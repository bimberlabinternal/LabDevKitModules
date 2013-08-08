CREATE TABLE ldk.notificationrecipients (
  rowid serial,
  notificationtype varchar(200),
  recipient integer,

  container entityid NOT NULL,
  createdby userid NOT NULL,
  created timestamp,
  modifiedby userid NOT NULL,
  modified timestamp,

  CONSTRAINT pk_notificationrecipients PRIMARY KEY (rowid)
);

--note: field lengths altered
CREATE TABLE ldk.notificationtypes (
  notificationtype varchar(200) NOT NULL,
  description text,
  CONSTRAINT pk_notificationtypes PRIMARY KEY (notificationtype)
);

