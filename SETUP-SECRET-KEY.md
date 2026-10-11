# V1 Tracker — per-phone secret key

This project is being converted to an owner-authorized Android phone finder. Each enrolled phone must have its own random key. The phone owner must grant Android location permission and see clear tracking status.

## Important status
The current website alone cannot track a phone. A working installation requires all three pieces: Android app, secure server, and laptop dashboard. Never put a Supabase service-role key in GitHub Pages or Android source code.

## Safe setup plan
1. Create a Supabase project and configure Auth.
2. Apply the database schema in `supabase/schema.sql`.
3. Deploy the Edge Function in `supabase/functions/v1-tracker/index.ts` and set server-side secrets.
4. Build and install the Android project in `android/` on your own phone.
5. Sign in as the owner, approve location permission, and copy the device's unique key into the laptop dashboard.

A key should be random, shown once, stored server-side only as a SHA-256 hash, and revocable. Treat the key like a password. Offline phones cannot send new GPS data; the dashboard can only show the last uploaded location.

Track only your own devices or devices whose owner has explicitly agreed.