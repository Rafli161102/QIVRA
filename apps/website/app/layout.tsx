import type { Metadata, Viewport } from "next";
import type { ReactNode } from "react";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "QIVRA | Software Modernization",
    template: "%s | QIVRA",
  },
  description:
    "QIVRA is being built to inspect legacy software, plan controlled modernization, and make validation part of every change.",
  applicationName: "QIVRA",
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
