# Supabase setup

1. Run `schema.sql` in Supabase SQL Editor.
2. Deploy `functions/v1-tracker/index.ts` as `v1-tracker`.
3. Configure server-side Edge Function secrets `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY`. Never expose the service role key in website code, Android code, or GitHub.
4. Configure Auth and allow only your own account to enroll devices.

The dashboard expects POST JSON `{ "action": "locate", "tracking_key": "..." }`. The function returns `{ "device": { "device_name", "latitude", "longitude", "accuracy_m", "updated_at" } }` after validating the key.
