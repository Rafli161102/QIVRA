/**
 * Returns the configured canonical origin for production metadata.
 * The value must be an origin only, not a path or query string.
 */
export function getPublicSiteUrl(): URL | undefined {
  const configuredUrl = process.env.NEXT_PUBLIC_SITE_URL?.trim();

  if (!configuredUrl) {
    return undefined;
  }

  let siteUrl: URL;

  try {
    siteUrl = new URL(configuredUrl);
  } catch {
    throw new Error(
      "NEXT_PUBLIC_SITE_URL must be a valid absolute URL, such as https://qivra.example.",
    );
  }

  const isLocalhost = siteUrl.hostname === "localhost";
  if (siteUrl.protocol !== "https:" && !isLocalhost) {
    throw new Error("NEXT_PUBLIC_SITE_URL must use HTTPS outside localhost.");
  }

  if (siteUrl.pathname !== "/" || siteUrl.search || siteUrl.hash) {
    throw new Error("NEXT_PUBLIC_SITE_URL must contain an origin only, without a path, query, or hash.");
  }

  if (siteUrl.username || siteUrl.password) {
    throw new Error("NEXT_PUBLIC_SITE_URL must not contain credentials.");
  }

  return siteUrl;
}

export function isProductionDeployment(): boolean {
  return process.env.VERCEL_ENV === "production";
}
