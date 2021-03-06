CREATE TABLE laboratory.workbooks (
  workbookId int,
  container entityid NOT NULL,

  CONSTRAINT PK_workbooks PRIMARY KEY (container)
);

ALTER TABLE laboratory.samples add makePublic bit default 0;
ALTER TABLE laboratory.samples add initials varchar(200);

--NOTE: due to timing issues, this has been disabled.  It is not strictly necessary and there is an option in the admin console to manually run this
--EXEC core.executeJavaUpgradeCode 'initWorkbookTable';
