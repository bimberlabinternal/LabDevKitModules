CREATE TABLE ldk.lookup_sets (
  rowid int identity(1,1),
  setname varchar(100),
  label varchar(500),
  keyField varchar(100),
  titleColumn varchar(100),
  description varchar(4000),

  container entityid,
  created datetime,
  createdby int,
  modified datetime,
  modifiedby int,

  constraint PK_lookup_sets PRIMARY KEY (rowid)
);

CREATE TABLE ldk.lookup_data (
  rowid int identity(1,1),
  set_name varchar(100),
  value varchar(200),
  displayValue varchar(200),
  category varchar(200),
  description varchar(4000),
  sort_order integer,
  date_disabled datetime,

  objectid char(36),
  container entityid,
  createdby userid,
  created datetime,
  modifiedby userid,
  modified datetime,

  CONSTRAINT pk_lookup_data PRIMARY KEY (rowid)
);