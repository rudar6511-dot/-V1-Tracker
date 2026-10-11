const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json"
};
const reply = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: cors });
const hex = (bytes: Uint8Array) => [...bytes].map(b => b.toString(16).padStart(2, "0")).join("");
async function digest(value: string) {
  return hex(new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value))));
}
function randomKey() {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  return "v1_" + btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}
Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "POST") return reply({ error: "POST required" }, 405);
  const url = Deno.env.get("SUPABASE_URL");
  const service = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const anon = Deno.env.get("SUPABASE_ANON_KEY");
  if (!url || !service || !anon) return reply({ error: "Server configuration is incomplete." }, 500);
  const headers = { apikey: service, Authorization: "Bearer " + service, "Content-Type": "application/json" };
  const rest = async (path: string, init: RequestInit = {}) =>
    fetch(url + "/rest/v1/" + path, { ...init, headers: { ...headers, ...(init.headers || {}) } });
  try {
    const body = await req.json();
    if (body.action === "register") {
      const auth = req.headers.get("Authorization") || "";
      if (!auth.startsWith("Bearer ")) return reply({ error: "Please sign in first." }, 401);
      const who = await fetch(url + "/auth/v1/user", { headers: { apikey: anon, Authorization: auth } });
      if (!who.ok) return reply({ error: "Sign-in token is invalid or expired." }, 401);
      const user = await who.json();
      const key = randomKey();
      const inserted = await rest("tracked_devices", {
        method: "POST",
        headers: { Prefer: "return=representation" },
        body: JSON.stringify({ owner_id: user.id, device_name: String(body.device_name || "My Android phone").slice(0, 80), key_hash: await digest(key) })
      });
      if (!inserted.ok) return reply({ error: "Could not register device. Check the database schema." }, 500);
      const rows = await inserted.json();
      return reply({ device_id: rows[0]?.id, tracking_key: key, message: "Copy this key now. It is shown only once." }, 201);
    }
    const key = String(body.tracking_key || "");
    if (!/^v1_[A-Za-z0-9_-]{30,60}$/.test(key)) return reply({ error: "Invalid tracking key." }, 401);
    const keyHash = await digest(key);
    if (body.action === "update_location") {
      const lat = Number(body.latitude), lng = Number(body.longitude), accuracy = body.accuracy_m == null ? null : Number(body.accuracy_m);
      if (!Number.isFinite(lat) || lat < -90 || lat > 90 || !Number.isFinite(lng) || lng < -180 || lng > 180) return reply({ error: "Invalid coordinates." }, 400);
      const updated = await rest("tracked_devices?key_hash=eq." + encodeURIComponent(keyHash) + "&revoked_at=is.null", {
        method: "PATCH", headers: { Prefer: "return=minimal" },
        body: JSON.stringify({ latitude: lat, longitude: lng, accuracy_m: Number.isFinite(accuracy) ? accuracy : null, updated_at: new Date().toISOString() })
      });
      if (!updated.ok) return reply({ error: "Could not update location." }, 500);
      return reply({ ok: true });
    }
    if (body.action === "locate") {
      const found = await rest("tracked_devices?select=device_name,latitude,longitude,accuracy_m,updated_at&key_hash=eq." + encodeURIComponent(keyHash) + "&revoked_at=is.null&limit=1");
      if (!found.ok) return reply({ error: "Could not query device." }, 500);
      const rows = await found.json();
      if (!rows.length) return reply({ error: "Key not found or revoked." }, 404);
      const d = rows[0];
      if (d.latitude == null || d.longitude == null) return reply({ error: "Key accepted, but this phone has not uploaded a location yet." }, 404);
      return reply({ device: d });
    }
    if (body.action === "revoke") {
      const revoked = await rest("tracked_devices?key_hash=eq." + encodeURIComponent(keyHash) + "&revoked_at=is.null", {
        method: "PATCH", body: JSON.stringify({ revoked_at: new Date().toISOString() })
      });
      if (!revoked.ok) return reply({ error: "Could not revoke this key." }, 500);
      return reply({ ok: true });
    }
    return reply({ error: "Unknown action." }, 400);
  } catch {
    return reply({ error: "Invalid request." }, 400);
  }
});