# GPS/VLTD Backend

This small Node.js API receives telemetry from an authorized GPS/VLTD device and exposes the latest location to the Vehicle Tracker frontend.

## Run

1. Install Node.js.
2. In this folder run `npm install`.
3. Set a strong `GPS_API_KEY` environment variable.
4. Run `npm start`.

## Send authorized telemetry

POST `/telemetry` with header `x-gps-api-key: YOUR_KEY`.

Example JSON:

```json
{
  "deviceId":"GPS-001",
  "vehicleNo":"HR26AB1234",
  "lat":28.6139,
  "lng":77.2090,
  "accuracy":"±8 m"
}
```

## Read latest location

GET `/vehicle/HR26AB1234` with the same API key.

The frontend currently expects the GET response without exposing the API key in the public website. For production, put authentication behind a secure user backend/proxy rather than placing a secret key in frontend JavaScript.

## Device integration

A real GPS/VLTD tracker must be configured to send telemetry to this backend using its documented protocol/API. Do not attempt to bypass device authentication or obtain location data for vehicles you are not authorized to track.