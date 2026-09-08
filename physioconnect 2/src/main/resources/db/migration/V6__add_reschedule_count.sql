ALTER TABLE appointments
ADD COLUMN reschedule_count INT NOT NULL DEFAULT 0;
