ALTER TABLE notification_preferences
ADD COLUMN IF NOT EXISTS email_event_reminders BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE notification_preferences
ADD COLUMN IF NOT EXISTS sms_event_reminders BOOLEAN NOT NULL DEFAULT FALSE;
