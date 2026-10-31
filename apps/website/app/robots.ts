import type { MetadataRoute } from "next";
import { getPublicSiteUrl, isProductionDeployment } from "@/lib/site-url";

export default function robots(): MetadataRoute.Robots {
  const siteUrl = isProductionDeployment() ? getPublicSiteUrl() : undefined;

  if (!siteUrl) {
    return {
      rules: {
        userAgent: "*",
        disallow: "/",
      },
    };
  }

  return {
    rules: {
      userAgent: "*",
      allow: "/",
    },
    sitemap: new URL("/sitemap.xml", siteUrl).toString(),
  };
}
