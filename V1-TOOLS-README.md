# V1 Tracker tools A, B and C

Open `v1-tools.html` through GitHub Pages/HTTPS to use the combined page.

## A — Vehicle GPS
- Demo vehicles: HR26AB1234 and DL01XY5678.
- Register a vehicle and device ID in this browser.
- For real data, set an authorized API base URL. The page calls `GET {base}/vehicle/{vehicleNumber}` and expects JSON with numeric `lat` and `lng`, optionally `accuracy` and `updatedAt`.
- Real tracking requires a compatible GPS/VLTD device and a backend that authenticates requests and restricts users to vehicles they may access.

## B — This phone's location
- Open this page on the phone whose location is being shared.
- Tap “Allow and start location” and grant the browser's location permission.
- The page displays coordinates in that browser only and does not upload them. Stop sharing with the Stop button.
- Browser geolocation requires HTTPS (or localhost during development).

## C — Phone number information
- Performs a basic format/length check and gives a limited calling-code hint.
- It does not look up a subscriber, carrier, exact region, or live location.

## Privacy/security
A phone number alone cannot reveal live GPS location. Do not track people or vehicles without clear authorization. The API URL is stored in browser local storage. Do not place API secrets in frontend code. Add server-side authentication, authorization, validation, and rate limiting before production use.