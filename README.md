# V1 Tracker — Installable laptop dashboard

This repository contains a starter system for locating your own enrolled Android phone from an installable laptop dashboard.

## Laptop PWA
- `laptop-dashboard/` contains the installable web app, manifest, service worker, and app icon.
- The repository root page is only an information page; it does not show phone locations.
- A PWA must be served from HTTPS or localhost to install. On GitHub Pages, open the dashboard at `https://rudar6511-dot.github.io/-V1-Tracker/laptop-dashboard/` and use the browser's Install app option (or the in-page install button when available).
- A PWA is not physically locked to one laptop. Other devices may also install its shell if they can access the public URL. Protect location data through account authentication and server-side ownership checks; keep your login private.
- The shell can load offline after caching, but sign-in and location lookups need internet.

## Required setup before location lookup works
1. Create/configure your Supabase project and Authentication account.
2. Run `supabase/schema.sql` in Supabase SQL Editor.
3. Configure Edge Function secrets `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `SUPABASE_SERVICE_ROLE_KEY`. Never put the service-role key in website code, Android code, or GitHub.
4. Deploy `supabase/functions/v1-tracker/index.ts` as `v1-tracker` with `verify_jwt = false` applied from `supabase/config.toml`. The function verifies each user's Supabase access token internally. Each user can enroll their own phone; location lookup and revocation are restricted to that signed-in user's account ID. No `V1_OWNER_EMAIL` allow-list is needed for this multi-user setup. Redeploy after code changes.
5. Install/run the Android project in `android/` on the phone. Each user should create/sign in to their own Supabase account, enroll that phone, and keep the unique Secret Key private. On a laptop, sign in with the same account used on that phone.
6. Start visible location sharing on the phone and grant location permission. The phone must be powered on, online, and sharing to upload a fresh position.
7. On the laptop PWA, enter the function URL and public/anon key, then sign in with the same account and enter the phone's Secret Key.

## Security and limitations
- GitHub Pages is public hosting; do not put secrets in the repository. The service-role key must remain server-side.
- Location lookup requires both the enrolled account and its device key after the updated Edge Function is deployed. Before redeploying, do not rely on the updated dashboard for privacy.
- This starter has not been built or tested on a physical Android device. Test before relying on it for theft recovery.
- Only track your own device or a device whose owner explicitly agrees to location sharing.


## Recovery email and lost-phone alert
1. Run the latest `supabase/schema.sql` in Supabase SQL Editor. This adds the private `recovery_profiles` table.
2. Configure a Resend account and verify the sender domain/address. Add these Edge Function secrets in Supabase:
   - `RESEND_API_KEY`: Resend API key (server-side secret only).
   - `V1_ALERT_EMAIL`: administrator alert inbox.
   - `V1_ALERT_FROM`: verified sender address, e.g. `V1 Tracker <alerts@your-verified-domain.example>`.
3. Redeploy the `v1-tracker` Edge Function after updating `index.ts`.
4. Sign in to the dashboard, save a recovery email, and explicitly accept the disclosure. Use **Report my phone lost** to send the alert to the configured administrator inbox and the entered recovery email. The email contains account email, device name, and timestamp only—never the Secret Key or coordinates.
5. The recovery email is currently saved as entered; this version does not independently verify control of that recovery mailbox. Use an email address you control and add a verification flow before public production use.

## Important Android limitation
This web dashboard and ordinary Android app do **not** detect how many times someone enters a wrong lock-screen PIN/password. Android does not expose those failed unlock attempts to ordinary apps. The alert is therefore user-triggered with **Report my phone lost**; it is not automatically triggered after 6–7 failed attempts. Do not rely on this starter as a theft alarm until the Android management approach and physical-device tests are completed. Location is only available if the phone is online and its own visible location-sharing service is running.
