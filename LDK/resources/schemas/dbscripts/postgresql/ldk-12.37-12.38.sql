CREATE TABLE ldk.lookup_sets (
  rowid serial,
  setname varchar(100),
  label varchar(500),
  keyField varchar(100),
  titleColumn varchar(100),
  description varchar(4000),

  container entityid,
  created timestamp,
  createdby int,
  modified timestamp,
  modifiedby int,

  constraint PK_lookup_sets PRIMARY KEY (rowid)
);

CREATE TABLE ldk.lookup_data (
  rowid serial,
  set_name varchar(100),
  value varchar(200),
  displayValue varchar(200),
  category varchar(200),
  description varchar(4000),
  sort_order integer,
  date_disabled timestamp,

  objectid char(36),
  container entityid,
  createdby userid,
  created timestamp,
  modifiedby userid,
  modified timestamp,

  CONSTRAINT pk_lookup_data PRIMARY KEY (rowid)
);