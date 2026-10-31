# Security Policy

## Status

QIVRA is pre-alpha. The repository does not currently execute customer code or provide a hosted service. The controls below define requirements for implementation; they are not a claim that a production isolation system already exists.

## Reporting a vulnerability

Do not disclose suspected vulnerabilities in public issues. Use GitHub's private vulnerability reporting feature for this repository when available. If private reporting is unavailable, contact the repository maintainer through a private channel listed on the maintainer's GitHub profile. Include steps to reproduce, impact, affected commit or version, and any suggested mitigation. Do not attach customer data or credentials.

## Security requirements for future execution features

Source repositories and build scripts must be treated as hostile input.

- Run builds and transformations in short-lived, isolated workers, never inside the API or dashboard process.
- Do not run privileged containers, mount the host Docker socket, or expose host credentials to a job.
- Apply CPU, memory, process-count, disk, and wall-clock limits.
- Use a read-only base filesystem and a disposable, job-scoped workspace.
- Deny outbound network access by default. If dependency resolution is required, use controlled egress with explicit policy and auditing.
- Validate Git URLs and redirects; block loopback, link-local, private-network, and cloud metadata destinations to reduce SSRF risk.
- Treat Maven/Gradle plugins, annotation processors, test code, shell scripts, Git hooks, submodules, and symlinks as executable or security-sensitive inputs.
- Do not pass secrets into untrusted build steps. Redact tokens and credentials from logs and diagnostics.
- Separate tenant data and authorization boundaries. Verify ownership on every project, job, artifact, and report access.
- Encrypt network traffic and sensitive persisted data. Define retention and deletion behavior before accepting customer source code.
- Record job identity, engine version, recipe set, policy, changed files, and validation outcome for auditability.

## Dependency security

Pin dependency versions, commit lockfiles, scan direct and transitive dependencies, review advisories, and record licenses. A dependency update that changes licensing or transitive artifacts requires review.

## Safe disclosure

Please provide a reasonable opportunity to investigate and remediate a report before public disclosure. Do not exploit a vulnerability beyond what is necessary to demonstrate it.
