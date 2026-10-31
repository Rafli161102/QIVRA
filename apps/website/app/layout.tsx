import type { Metadata, Viewport } from "next";
import type { ReactNode } from "react";
import { getPublicSiteUrl, isProductionDeployment } from "@/lib/site-url";
import "./globals.css";

const siteUrl = isProductionDeployment() ? getPublicSiteUrl() : undefined;
const isIndexable = Boolean(siteUrl);

const siteTitle = "QIVRA | Software Modernization";
const siteDescription =
  "QIVRA is an engineering platform in development for inspecting legacy software, planning controlled modernization, and validating changes. Initial focus: Java, Maven, and Gradle.";

export const metadata: Metadata = {
  ...(siteUrl ? { metadataBase: siteUrl, alternates: { canonical: "/" } } : {}),
  title: {
    default: siteTitle,
    template: "%s | QIVRA",
  },
  description: siteDescription,
  applicationName: "QIVRA",
  openGraph: {
    type: "website",
    locale: "en_US",
    siteName: "QIVRA",
    title: siteTitle,
    description: siteDescription,
    ...(siteUrl ? { url: siteUrl.toString() } : {}),
  },
  twitter: {
    card: "summary",
    title: siteTitle,
    description: siteDescription,
  },
  robots: {
    index: isIndexable,
    follow: isIndexable,
    googleBot: {
      index: isIndexable,
      follow: isIndexable,
      "max-image-preview": "large",
      "max-snippet": -1,
      "max-video-preview": -1,
    },
  },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
