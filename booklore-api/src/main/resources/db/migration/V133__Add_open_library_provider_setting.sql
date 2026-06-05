UPDATE app_settings
SET val = JSON_SET(val, '$.openLibrary.enabled', true)
WHERE name = 'metadata_provider_settings'
  AND JSON_EXTRACT(val, '$.openLibrary') IS NULL;
