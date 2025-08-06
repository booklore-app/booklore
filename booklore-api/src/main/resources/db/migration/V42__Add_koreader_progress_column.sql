ALTER TABLE user_book_progress
    ADD koreader_progress         varchar(1000) null,
    ADD koreader_progress_percent float         null,
    ADD koreader_device           varchar(100)  null,
    ADD koreader_device_id        varchar(100)  null;