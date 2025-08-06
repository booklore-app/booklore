ALTER TABLE user_book_progress
    ADD COLUMN IF NOT EXISTS koreader_progress VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS koreader_progress_percent FLOAT,
    ADD COLUMN IF NOT EXISTS koreader_device VARCHAR(100),
    ADD COLUMN IF NOT EXISTS koreader_device_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS koreader_last_sync_time TIMESTAMP;