CREATE TABLE ldk.daysOfWeek (
  idx int,
  name varchar(100),

  CONSTRAINT PK_daysOfWeek PRIMARY KEY (idx)
);

INSERT INTO ldk.daysOfWeek (idx, name) VALUES (1, 'Sunday');
INSERT INTO ldk.daysOfWeek (idx, name) VALUES (2, 'Monday');
INSERT INTO ldk.daysOfWeek (idx, name) VALUES (3, 'Tuesday');
INSERT INTO ldk.daysOfWeek (idx, name) VALUES (4, 'Wednesday');
INSERT INTO ldk.daysOfWeek (idx, name) VALUES (5, 'Thursday');
INSERT INTO ldk.daysOfWeek (idx, name) VALUES (6, 'Friday');
INSERT INTO ldk.daysOfWeek (idx, name) VALUES (7, 'Saturday');