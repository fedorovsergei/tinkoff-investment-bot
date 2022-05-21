CREATE SEQUENCE GLOBAL_SEQ AS INTEGER START WITH 100000;

CREATE TABLE CANDLE_HISTORY
(
    ID   INTEGER GENERATED BY DEFAULT AS SEQUENCE GLOBAL_SEQ PRIMARY KEY,
    DATE TIMESTAMP    NOT NULL,
    FIGI VARCHAR(255) NOT NULL,
    UNIT NUMERIC      NOT NULL,
    NANO INTEGER      NOT NULL
);

CREATE TABLE TRADE_TEST_RESULT
(
    ID     INTEGER GENERATED BY DEFAULT AS SEQUENCE GLOBAL_SEQ PRIMARY KEY,
    IS_BUY BOOLEAN      NOT NULL,
    DATE   TIMESTAMP    NOT NULL,
    FIGI   VARCHAR(255) NOT NULL,
    UNIT   NUMERIC      NOT NULL,
    NANO   INTEGER      NOT NULL
)