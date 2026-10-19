# Render Free demo deployment

This configuration deploys the **preview/demo profile**, not the persistent production portal. Render Free is useful for sharing a public demo, but it is **not 24/7**: free web services spin down after 15 minutes without inbound traffic and can take about a minute to wake. This service uses in-memory H2, so its demo data resets whenever the service restarts. See Render's current limitations: <https://render.com/docs/free>.

## Included configuration

- `Dockerfile` builds the Java 11 Spring Boot JAR and runs it as a web service.
- `render.yaml` defines one Render Free web service, `/api/health` health check, and the preview profile.
- H2 preview mode means no MySQL database or database secret is needed for this demo. It is ephemeral.
- The preview profile seeds the source timetable data, synthetic demo accounts, and the five student directory records the user approved for public display. Those student records have no login accounts.

## Deploy from a Git repository

Render's Blueprint deployment expects the project in a Git repository. This workspace is not connected to a GitHub repository, and no Render account is connected here, so deployment cannot be completed directly from this workspace.

1. Create a private GitHub repository under your account. Upload the project files, including `Dockerfile` and `render.yaml`. Do not include `.env`, passwords, API keys, or cloud tokens.
2. In Render, choose **New → Blueprint**, connect that repository, and select its `render.yaml`.
3. Set the six secret values Render prompts for. Use unique demo-only credentials; all passwords must be 12–72 characters. Keep the values in Render's environment settings—never commit them to the repository.
4. Deploy and wait for `/api/health` to pass. Render will provide an HTTPS `onrender.com` URL. Share that URL with friends.
5. Test login, role protections, and logout in a separate browser/device. Remember that this free demo may sleep and its database resets after a restart.

The preview role selector can autofill the configured synthetic demo credentials. Keep this public deployment limited to demo data; do not use it for college operations or confidential records.

## For an always-on portal later

Render Free does not satisfy a 24/7 requirement. Render's free Postgres also expires after 30 days and is not a MySQL drop-in for this application. A persistent Render deployment would need a paid web service and a persistent MySQL database (for example, a separately configured MySQL service/disk or an external managed MySQL provider), plus backups. The five user-approved student directory records are included for the planned public demo; do not add other personal records without authorization.
