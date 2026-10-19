# Best-effort free deployment on Oracle Cloud

This runbook is a deployment scaffold for the College Portal on an Oracle Cloud Infrastructure (OCI) Always Free VM with an OCI MySQL HeatWave Always Free database. It is **not a 24/7 uptime guarantee**: OCI may reclaim qualifying idle Always Free compute instances, and the Always Free HeatWave database has no SLA. Treat it as a free public demo, not a production service for critical college operations. Official limits: <https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm> and <https://docs.oracle.com/en-us/iaas/mysql-database/doc/features-mysql-heatwave-service.html>.

## Before deployment

- Create and control your own OCI account. Never send cloud passwords, API keys, database passwords, or SSH private keys in chat.
- The OCI free services must be available in the tenancy's home region. Create the app VM and MySQL DB system in the same VCN, with MySQL accessible only from the app VM's private subnet/security rules.
- Use a domain name that you control and point its DNS A record to the VM public IP before enabling public login. Use HTTPS; do not expose the login page over plain HTTP. A free DNS name is acceptable if it supports DNS and certificate issuance.
- `database.sql` contains five user-approved public student directory records (names and IDs) without login accounts. They will be visible to anyone who can access the public portal. Do not add any other personal records without approval.
- Do **not** run the `preview` Spring profile in the public deployment. Preview enables iframe embedding, uses ephemeral H2, and has demo-account behavior.

## OCI resources

1. Create an OCI VCN with a public subnet for the VM and a private subnet for MySQL. Keep the database endpoint private.
2. Create a VM in the free tier (Ubuntu 22.04/24.04 ARM64 is suitable for the Java 11 JAR). Add an inbound security rule for SSH only from your own IP while administering. Later add public TCP 80 and 443 for Nginx/HTTPS. Do not open TCP 3306 to the internet.
3. Create an OCI MySQL HeatWave DB system and select the **Always Free** shape. Put it in the private subnet and allow TCP 3306 from the VM's subnet/security group only. Record its private endpoint, schema/user, and CA/TLS instructions from OCI.
4. Assign a public IP to the VM. For a free hostname, create a subdomain at [DuckDNS](https://www.duckdns.org/) and point its IPv4 address to the VM's public IP. DuckDNS documents its HTTPS update API at <https://www.duckdns.org/spec.jsp>; keep its account token private and configure any updater directly on the VM. After the DNS name resolves to the VM, issue a certificate for that hostname with Certbot/Let's Encrypt. If the VM public IP changes, update DuckDNS before the hostname becomes unreachable.

OCI resource names, free-tier eligibility, availability and console labels can change. Verify that the VM and database show as Always Free before creating them; avoid trial/paid shapes.

## Build the application locally

From the repository root:

```bash
mvn -q clean package
```

The resulting application JAR is `target/find-my-faculty-backend-0.1.0.jar`. This build uses the existing Maven tests; it does not deploy anything.

## VM application setup (Ubuntu)

SSH to the VM using your own private key. Install Java 11, Nginx, and a MySQL client using the package manager for the selected OS. Create a dedicated service account and directories:

```bash
sudo useradd --system --home /opt/college-portal --shell /usr/sbin/nologin portal || true
sudo install -d -o portal -g portal /opt/college-portal
sudo install -d -o root -g portal -m 0750 /etc/college-portal
```

From your development machine, copy the JAR (replace the SSH host with the VM's public IP/DNS):

```bash
scp target/find-my-faculty-backend-0.1.0.jar deploy/oracle-free/college-portal.service ubuntu@VM_PUBLIC_IP:/tmp/
```

On the VM, install the JAR and service file:

```bash
sudo install -o portal -g portal -m 0640 /tmp/find-my-faculty-backend-0.1.0.jar /opt/college-portal/find-my-faculty-backend.jar
sudo install -o root -g root -m 0644 /tmp/college-portal.service /etc/systemd/system/college-portal.service
```

The `portal.service` file expects its secrets/configuration in `/etc/college-portal/portal.env`. Create that file locally on the VM with `sudoedit` (do not put secrets in Git or a shared document):

```dotenv
DB_URL=jdbc:mysql://MYSQL_PRIVATE_ENDPOINT:3306/findmyfaculty?sslMode=REQUIRED&serverTimezone=Asia%2FKolkata
DB_USER=portal_app
DB_PASSWORD=REPLACE_WITH_UNIQUE_DB_PASSWORD
ADMIN_USERNAME=portal-admin
ADMIN_PASSWORD=REPLACE_WITH_UNIQUE_RANDOM_PASSWORD_AT_LEAST_12_CHARS
SESSION_COOKIE_SECURE=true
CORS_ALLOWED_ORIGINS=https://YOUR_HOSTNAME
```

Use the OCI-supported TLS/CA configuration for the MySQL endpoint; if the service requires CA verification rather than `sslMode=REQUIRED`, configure the Connector/J truststore/CA as specified by OCI. Do not make the database publicly reachable.

Restrict the environment file, import schema/data into a **new, empty** database as the MySQL database administrator, then create the app user (restrict its network access at the VCN layer to the VM):

```bash
sudo chown root:portal /etc/college-portal/portal.env
sudo chmod 0640 /etc/college-portal/portal.env
mysql --host=MYSQL_PRIVATE_ENDPOINT --user=DB_ADMIN_USER --password --ssl-mode=REQUIRED < database.sql
mysql --host=MYSQL_PRIVATE_ENDPOINT --user=DB_ADMIN_USER --password --ssl-mode=REQUIRED
```

At the MySQL prompt, create a unique app credential and grant access only to this schema (quote/escape the placeholder safely):

```sql
CREATE USER 'portal_app'@'%' IDENTIFIED BY 'UNIQUE_RANDOM_PASSWORD' REQUIRE SSL;
GRANT ALL PRIVILEGES ON findmyfaculty.* TO 'portal_app'@'%';
```

Put the same app username/password in `portal.env`. The grant is schema-scoped; VCN rules must still block all untrusted network sources. The SQL import seeds source-derived schedules, the ADND label, and the five approved directory-only student rows. It does not seed any login accounts. At first startup, Spring creates the administrator account from the environment and stores a BCrypt hash. Keep the admin password private and change it if ever disclosed.

Enable and inspect the service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now college-portal
sudo systemctl status college-portal --no-pager
sudo journalctl -u college-portal -n 100 --no-pager
```

## HTTPS reverse proxy

Install Nginx and configure a virtual host for your real DNS name, proxying to `http://127.0.0.1:8080`. Preserve the `Host`, `X-Forwarded-For`, `X-Forwarded-Proto`, and `X-Forwarded-Host` headers. Enable Spring's forwarded-header handling if TLS terminates at Nginx, and request a valid certificate with Certbot only after DNS points to the VM and TCP 80 is reachable. Configure Nginx to redirect HTTP to HTTPS. Do not expose application port 8080 in the OCI security list.

Before sharing the HTTPS URL, verify:

- HTTPS certificate is valid and login forms never load over HTTP.
- `GET /api/health` returns `200`.
- Admin, Student and Faculty login/logout work from a separate browser/device.
- Anonymous and cross-role requests remain blocked.
- The five approved directory records show without accounts; no preview credentials or H2 database are present.
- Restarting the VM does not lose persistent database data.

## Limitations

- Free-tier resources have limits, capacity constraints, and no uptime SLA for the Always Free MySQL system. OCI documents possible reclamation of idle compute instances. The service may need manual recovery and the deployment may be unavailable at times.
- Always Free MySQL automatic backups have a fixed one-day retention; it is not a substitute for an independent backup/export routine.
- No deployment has been performed by this runbook. It becomes a live service only after the account owner provisions the resources, applies network rules, configures DNS/HTTPS/secrets, imports the database and starts the service.
