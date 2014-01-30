CREATE TABLE ldk.months (
    rowid serial,
    monthName varchar(100),
    monthNum int,

    CONSTRAINT PK_months PRIMARY KEY (rowid)
);

INSERT into ldk.months (monthName, monthNum) VALUES ('January', 1);
INSERT into ldk.months (monthName, monthNum) VALUES ('February', 2);
INSERT into ldk.months (monthName, monthNum) VALUES ('March', 3);
INSERT into ldk.months (monthName, monthNum) VALUES ('April', 4);
INSERT into ldk.months (monthName, monthNum) VALUES ('May', 5);
INSERT into ldk.months (monthName, monthNum) VALUES ('June', 6);
INSERT into ldk.months (monthName, monthNum) VALUES ('July', 7);
INSERT into ldk.months (monthName, monthNum) VALUES ('August', 8);
INSERT into ldk.months (monthName, monthNum) VALUES ('September', 9);
INSERT into ldk.months (monthName, monthNum) VALUES ('October', 10);
INSERT into ldk.months (monthName, monthNum) VALUES ('November', 11);
INSERT into ldk.months (monthName, monthNum) VALUES ('December', 12);
