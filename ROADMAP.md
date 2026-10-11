# V1 Tracker — Combined Roadmap

The project can support three separate tools:

## A. Vehicle GPS tracker
- Keep the current vehicle dashboard and demo map.
- Connect only GPS/VLTD devices and backend data the user is authorized to access.
- Add authenticated API access before using real vehicle locations.

## B. Phone location sharing
- Support only the phone currently opening the website and explicitly granting browser location permission.
- Require HTTPS (or localhost for development).
- Make the sharing state visible and provide a Stop button.
- Do not attempt to locate a phone remotely using only its number.

## C. Phone number information
- Check basic number formatting and show a limited country calling-code hint.
- Do not claim to identify the subscriber, exact location, or live location from a phone number.

## Recommended implementation order
1. Polish the existing vehicle tracker UI and keep demo data clearly labeled.
2. Add permission-based geolocation for the current device, with visible start/stop controls.
3. Add basic number-format and calling-code hints.
4. Build a secure backend only after choosing the GPS hardware/provider; require authentication and authorization.

## Privacy and security
Never track a person or vehicle without permission. A number alone does not reveal live GPS location. Do not store private API keys in public frontend files.