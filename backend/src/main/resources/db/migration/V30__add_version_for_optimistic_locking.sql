-- Add version columns for JPA optimistic locking (@Version) on the three
-- entities involved in the purchase flow: tickets, listings, and orders.
-- Hibernate will include "WHERE version = ?" on every UPDATE and increment
-- the value, throwing OptimisticLockException if the row changed since read.

ALTER TABLE tickets ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE listings ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
