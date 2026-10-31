import type { MetadataRoute } from "next";
import { getPublicSiteUrl, isProductionDeployment } from "@/lib/site-url";

export default function sitemap(): MetadataRoute.Sitemap {
  const siteUrl = isProductionDeployment() ? getPublicSiteUrl() : undefined;

  if (!siteUrl) {
    return [];
  }

  return [
    {
      url: siteUrl.toString(),
      changeFrequency: "monthly",
      priority: 1,
    },
  ];
}
