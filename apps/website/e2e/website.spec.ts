import { expect, test } from "@playwright/test";

const navigationTargets = [
  { label: "Principles", id: "principles" },
  { label: "Workflow", id: "workflow" },
  { label: "Java scope", id: "scope" },
  { label: "Project status", id: "status" },
];

function luminance(color: string): number {
  const channels = color.match(/\d+(?:\.\d+)?/g);
  if (!channels || channels.length < 3) {
    throw new Error(`Could not parse computed color: ${color}`);
  }

  const [red, green, blue] = channels.slice(0, 3).map((value) => {
    const channel = Number(value) / 255;
    return channel <= 0.04045
      ? channel / 12.92
      : ((channel + 0.055) / 1.055) ** 2.4;
  });

  return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
}

function contrastRatio(foreground: string, background: string): number {
  const first = luminance(foreground);
  const second = luminance(background);
  const lighter = Math.max(first, second);
  const darker = Math.min(first, second);
  return (lighter + 0.05) / (darker + 0.05);
}

test("renders the factual pre-alpha product overview", async ({ page }) => {
  const response = await page.goto("/");

  expect(response?.status()).toBe(200);
  await expect(page).toHaveTitle("QIVRA | Software Modernization");
  await expect(page.locator("html")).toHaveAttribute("lang", "en");
  await expect(page.getByRole("heading", { level: 1 })).toHaveCount(1);
  await expect(
    page.getByRole("heading", { name: /Understand the system\.\s*Then change it\./ }),
  ).toBeVisible();
  await expect(page.getByText("Proposed product workflow. This is not a live repository scan.")).toBeVisible();
  await expect(page.getByText("Planned scope / Not yet released")).toBeVisible();
  await expect(page.locator("img")).toHaveCount(0);
});

test("every primary navigation item reaches an existing section", async ({ page }) => {
  await page.goto("/");
  const navigation = page.getByRole("navigation", { name: "Main navigation" });

  for (const target of navigationTargets) {
    await navigation.getByRole("link", { name: target.label }).click();
    await expect(page).toHaveURL(new RegExp(`#${target.id}$`));
    await expect(page.locator(`#${target.id}`)).toBeInViewport();
  }
});

test("hero and status calls to action reach their intended sections", async ({ page }) => {
  await page.goto("/");
  const actions = [
    { label: "Follow the workflow", id: "workflow" },
    { label: "See the first scope", id: "scope" },
    { label: /Current status/, id: "status" },
  ];

  for (const action of actions) {
    await page.getByRole("link", { name: action.label }).click();
    await expect(page).toHaveURL(new RegExp(`#${action.id}$`));
    await expect(page.locator(`#${action.id}`)).toBeInViewport();
  }
});

test("header and footer wordmarks return to the page top", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("link", { name: "QIVRA home" }).click();
  await expect(page).toHaveURL(/#top$/);
  await expect(page.locator("#top")).toBeInViewport();

  await page.locator(".footer-wordmark").click();
  await expect(page).toHaveURL(/#top$/);
  await expect(page.locator("#top")).toBeInViewport();
});

test("the page emits no browser runtime errors", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  page.on("console", (message) => {
    if (message.type() === "error") errors.push(message.text());
  });

  const response = await page.goto("/");
  expect(response?.status()).toBe(200);
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  expect(errors).toEqual([]);
});

test("all links have a real destination", async ({ page }) => {
  await page.goto("/");
  const links = await page.locator("a").evaluateAll((anchors) =>
    anchors.map((anchor) => ({
      label: anchor.textContent?.trim() ?? "",
      href: anchor.getAttribute("href") ?? "",
    })),
  );

  expect(links.length).toBeGreaterThan(0);

  for (const link of links) {
    expect(link.href, `Empty href on link: ${link.label}`).not.toBe("");

    const destination = new URL(link.href, "http://127.0.0.1:3000");
    if (destination.origin === "http://127.0.0.1:3000" && destination.hash) {
      await expect(
        page.locator(destination.hash),
        `Missing local target for link: ${link.label}`,
      ).toHaveCount(1);
    } else {
      expect(
        ["http:", "https:"],
        `Unsupported protocol on link: ${link.label}`,
      ).toContain(destination.protocol);
    }
  }
});

test("skip link moves keyboard focus to main content", async ({ page }) => {
  await page.goto("/");
  await page.keyboard.press("Tab");

  const skipLink = page.getByRole("link", { name: "Skip to content" });
  await expect(skipLink).toBeFocused();
  await expect(skipLink).toHaveCSS("outline-style", "solid");
  await page.keyboard.press("Enter");

  await expect(page).toHaveURL(/#main-content$/);
  await expect(page.locator("#main-content")).toBeFocused();
  await expect(page.locator("#main-content")).toHaveCSS("outline-style", "solid");
});

test("all visible links meet the 44px minimum touch target height", async ({ page }) => {
  await page.goto("/");
  const targetHeights = await page.locator("a:visible").evaluateAll((anchors) =>
    anchors.map((anchor) => ({
      label: anchor.textContent?.trim() ?? "Unnamed link",
      height: anchor.getBoundingClientRect().height,
    })),
  );

  expect(targetHeights.length).toBeGreaterThan(0);
  const undersized = targetHeights.filter((target) => target.height < 44);
  expect(undersized).toEqual([]);
});

test("implemented text and primary action color pairs meet WCAG AA", async ({ page }) => {
  await page.goto("/");
  const pairs = await page.evaluate(() => {
    const readPair = (selector: string) => {
      const element = document.querySelector<HTMLElement>(selector);
      if (!element) throw new Error(`Missing contrast test element: ${selector}`);

      const foreground = getComputedStyle(element).color;
      let current: HTMLElement | null = element;
      let background = "rgb(0, 0, 0)";

      while (current) {
        const candidate = getComputedStyle(current).backgroundColor;
        if (candidate !== "rgba(0, 0, 0, 0)" && candidate !== "transparent") {
          background = candidate;
          break;
        }
        current = current.parentElement;
      }

      return { foreground, background };
    };

    return {
      hero: readPair(".hero-lead"),
      workflow: readPair(".workflow-content > p:first-child"),
      action: readPair(".button-primary"),
    };
  });

  expect(contrastRatio(pairs.hero.foreground, pairs.hero.background)).toBeGreaterThanOrEqual(4.5);
  expect(contrastRatio(pairs.workflow.foreground, pairs.workflow.background)).toBeGreaterThanOrEqual(4.5);
  expect(contrastRatio(pairs.action.foreground, pairs.action.background)).toBeGreaterThanOrEqual(4.5);
});

for (const width of [320, 360, 390, 700, 768, 980, 1024, 1440]) {
  test(`layout has no horizontal overflow at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await page.goto("/");

    const dimensions = await page.evaluate(() => ({
      viewport: document.documentElement.clientWidth,
      document: document.documentElement.scrollWidth,
      body: document.body.scrollWidth,
    }));

    expect(dimensions.document).toBeLessThanOrEqual(dimensions.viewport);
    expect(dimensions.body).toBeLessThanOrEqual(dimensions.viewport);
  });
}


test("SEO metadata is complete and preview deployments are not indexable", async ({ page }) => {
  await page.goto("/");

  await expect(page.locator('meta[name="description"]')).toHaveAttribute(
    "content",
    /inspecting legacy software.*Java, Maven, and Gradle/i,
  );
  await expect(page.locator('meta[property="og:type"]')).toHaveAttribute("content", "website");
  await expect(page.locator('meta[property="og:site_name"]')).toHaveAttribute("content", "QIVRA");
  await expect(page.locator('meta[name="twitter:card"]')).toHaveAttribute("content", "summary");

  const configuredSiteUrl = process.env.NEXT_PUBLIC_SITE_URL;
  const isConfiguredProduction = Boolean(
    process.env.VERCEL_ENV === "production" && configuredSiteUrl,
  );
  const robots = await page.locator('meta[name="robots"]').getAttribute("content");

  if (isConfiguredProduction && configuredSiteUrl) {
    expect(robots).toMatch(/index/);
    await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
      "href",
      new URL("/", configuredSiteUrl).toString(),
    );
  } else {
    expect(robots).toContain("noindex");
    await expect(page.locator('link[rel="canonical"]')).toHaveCount(0);
  }
});

test("robots and sitemap never expose a preview URL to crawlers", async ({ page }) => {
  const robotsResponse = await page.request.get("/robots.txt");
  const sitemapResponse = await page.request.get("/sitemap.xml");

  expect(robotsResponse.status()).toBe(200);
  expect(sitemapResponse.status()).toBe(200);

  const robotsText = await robotsResponse.text();
  const sitemapText = await sitemapResponse.text();
  const configuredSiteUrl = process.env.NEXT_PUBLIC_SITE_URL;
  const isConfiguredProduction = Boolean(
    process.env.VERCEL_ENV === "production" && configuredSiteUrl,
  );

  if (isConfiguredProduction && configuredSiteUrl) {
    expect(robotsText).toContain("Allow: /");
    expect(robotsText).toContain(new URL("/sitemap.xml", configuredSiteUrl).toString());
    expect(sitemapText).toContain(new URL("/", configuredSiteUrl).toString());
  } else {
    expect(robotsText).toContain("Disallow: /");
    expect(sitemapText).not.toContain("<loc>http");
  }
});

test("baseline security response headers are present", async ({ page }) => {
  const response = await page.goto("/");
  expect(response).not.toBeNull();

  const headers = response!.headers();
  expect(headers["x-content-type-options"]).toBe("nosniff");
  expect(headers["x-frame-options"]).toBe("DENY");
  expect(headers["referrer-policy"]).toBe("strict-origin-when-cross-origin");
  expect(headers["permissions-policy"]).toBe("camera=(), microphone=(), geolocation=()");
});

test("reduced-motion preference disables smooth scrolling", async ({ page }) => {
  await page.emulateMedia({ reducedMotion: "reduce" });
  await page.goto("/");

  const scrollBehavior = await page.locator("html").evaluate((element) =>
    getComputedStyle(element).scrollBehavior,
  );
  expect(scrollBehavior).toBe("auto");
});
