# Vehicle Tracker V3

Vehicle tracking dashboard for vehicles and GPS/VLTD data the user is authorized to access.

## V3 changes
- Removed manual latitude/longitude registration fields
- Register a vehicle with Vehicle Number + Vehicle Name + GPS Device ID
- Optional REST API connection for an authorized GPS/VLTD backend
- Dashboard polls the configured API every 10 seconds
- Interactive OpenStreetMap map using Leaflet
- Demo GPS vehicles remain available for testing
- No public number-plate location lookup

## GPS API contract

The website expects this endpoint:

GET `YOUR_API_BASE/vehicle/HR26AB1234`

Example JSON response:

```json
{
  "lat": 28.6139,
  "lng": 77.2090,
  "accuracy": "±8 m",
  "updatedAt": "2026-09-23T10:30:00Z"
}
```

The backend must identify the vehicle from its authorized device and return only data the authenticated user is allowed to see.

## Device-to-server flow

`GPS/VLTD device → cellular network → authorized backend → VehicleTracker website → map`

A generic device cannot be connected by registration number alone. The actual GPS/VLTD device must support a documented API/protocol, and the backend must authenticate its telemetry.

## Important

A vehicle registration/number plate does not reveal live location. This project is intended only for vehicles and location data you own or are authorized to track.

## Next step

Add a secure backend (for example Node.js + PostgreSQL/Supabase) and connect the exact GPS/VLTD device's documented telemetry protocol. Do not put device secrets or backend credentials in this public frontend repository.
