-- Copyright 2004-2020 H2 Group. Multiple-Licensed under the MPL 2.0,
-- and the EPL 1.0 (https://h2database.com/html/license.html).
-- Initial Developer: H2 Group
--

CREATE TABLE TEST(C1 CLOB, C2 CHARACTER LARGE OBJECT, C3 TINYTEXT, C4 TEXT, C5 MEDIUMTEXT, C6 LONGTEXT, C7 NTEXT,
    C8 NCLOB, C9 CHAR LARGE OBJECT, C10 NCHAR LARGE OBJECT, C11 NATIONAL CHARACTER LARGE OBJECT);
> ok

SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'TEST' ORDER BY ORDINAL_POSITION;
> COLUMN_NAME DATA_TYPE
> ----------- ----------------------
> C1          CHARACTER LARGE OBJECT
> C2          CHARACTER LARGE OBJECT
> C3          CHARACTER LARGE OBJECT
> C4          CHARACTER LARGE OBJECT
> C5          CHARACTER LARGE OBJECT
> C6          CHARACTER LARGE OBJECT
> C7          CHARACTER LARGE OBJECT
> C8          CHARACTER LARGE OBJECT
> C9          CHARACTER LARGE OBJECT
> C10         CHARACTER LARGE OBJECT
> C11         CHARACTER LARGE OBJECT
> rows (ordered): 11

DROP TABLE TEST;
> ok

CREATE TABLE TEST(C0 CLOB(10), C1 CLOB(10K), C2 CLOB(10M CHARACTERS), C3 CLOB(10G OCTETS), C4 CLOB(10T), C5 CLOB(10P));
> ok

SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'TEST' ORDER BY ORDINAL_POSITION;
> COLUMN_NAME DATA_TYPE              CHARACTER_MAXIMUM_LENGTH
> ----------- ---------------------- ------------------------
> C0          CHARACTER LARGE OBJECT 10
> C1          CHARACTER LARGE OBJECT 10240
> C2          CHARACTER LARGE OBJECT 10485760
> C3          CHARACTER LARGE OBJECT 10737418240
> C4          CHARACTER LARGE OBJECT 10995116277760
> C5          CHARACTER LARGE OBJECT 11258999068426240
> rows (ordered): 6

INSERT INTO TEST(C0) VALUES ('12345678901');
> exception VALUE_TOO_LONG_2

INSERT INTO TEST(C0) VALUES ('1234567890');
> update count: 1

SELECT C0 FROM TEST;
>> 1234567890

DROP TABLE TEST;
> ok

CREATE TABLE TEST(C CLOB(8192P));
> exception INVALID_VALUE_2

EXPLAIN VALUES CAST(' ' AS CLOB(1));
>> VALUES (CAST(' ' AS CHARACTER LARGE OBJECT(1)))

CREATE TABLE T(C CLOB(0));
> exception INVALID_VALUE_2

CREATE TABLE TEST(C1 CLOB(1K CHARACTERS), C2 CLOB(1K OCTETS));
> ok

DROP TABLE TEST;
> ok
