# V1 Tracker — Secret Key Phone Finder (Option A)

This repository now contains a starter system for finding **your own enrolled Android phone** from the laptop dashboard. Each phone gets a different random secret key.

## Current files
- `index.html` — laptop dashboard; enter the Supabase function URL, public/anon key, and that phone's key.
- `android/` — Android Studio starter app with account sign-in, device enrollment, and an explicitly enabled foreground location service with a persistent notification and Stop action.
- `supabase/schema.sql` — database table.
- `supabase/functions/v1-tracker/index.ts` — server-side key validation, enrollment, location update, lookup and revoke actions.
- `supabase/config.toml` — lets the function validate requests itself; device keys and owner login are checked inside the function.

## Setup — required before it works
1. Create a Supabase project and confirm email/account settings in Authentication.
2. Run `supabase/schema.sql` in Supabase SQL Editor.
3. In Supabase Edge Function secrets, set `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `SUPABASE_SERVICE_ROLE_KEY`. **Never publish the service-role key.**
4. Deploy the `supabase/functions/v1-tracker` function with `supabase/config.toml` settings. If using the CLI, deploy from this repository root and check the function's `verify_jwt = false` setting is applied; the handler validates owner tokens and per-device keys itself.
5. In Android Studio, open the `android/` folder. Install on your own Android phone, enter the Supabase project URL and anon/public key, create/sign in to an account, and enroll the phone. Copy the unique key when shown.
6. Tap **Start visible location sharing** on the phone and grant location permission. Android shows an ongoing notification while sharing; use Stop to end it.
7. Open the GitHub Pages site on your laptop. Enter `https://YOUR-PROJECT.supabase.co/functions/v1/v1-tracker`, the Supabase anon/public key, and the phone's secret key. Click **Find My Phone** to see its latest uploaded position.

## Important limitations and safety
- GitHub Pages alone cannot obtain a phone's GPS; the backend and Android app must be configured first.
- The phone needs location permission, power, network access, and the sharing service running to send fresh positions. Android may limit background work depending on battery settings. If offline, the dashboard can only show the last uploaded location.
- The device key is a password. Anyone with it may request that device's latest location. Keep it private and revoke it if exposed.
- The starter's `revoke` action accepts the device key; after revocation, enroll the device again to receive a replacement key.
- Use only on devices you own or whose owner explicitly agrees to location sharing.
- This code has not been built/tested on a physical device in this environment. Test before relying on it for theft recovery.
