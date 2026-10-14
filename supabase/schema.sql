create extension if not exists pgcrypto;
create table if not exists public.tracked_devices (
 id uuid primary key default gen_random_uuid(),
 owner_id uuid not null references auth.users(id) on delete cascade,
 device_name text not null default 'My Android phone',
 key_hash text not null unique,
 latitude double precision,
 longitude double precision,
 accuracy_m double precision,
 updated_at timestamptz,
 created_at timestamptz not null default now(),
 revoked_at timestamptz
);
alter table public.tracked_devices enable row level security;
revoke all on public.tracked_devices from anon, authenticated;
-- All reads/writes must go through the server-side Edge Function.


-- Recovery settings and explicit consent for lost-device alerts.
create table if not exists public.recovery_profiles (
 user_id uuid primary key references auth.users(id) on delete cascade,
 recovery_email text not null,
 location_consent boolean not null default false,
 updated_at timestamptz not null default now()
);
alter table public.recovery_profiles enable row level security;
revoke all on public.recovery_profiles from anon, authenticated;
-- Read/write only through the authenticated Edge Function.
