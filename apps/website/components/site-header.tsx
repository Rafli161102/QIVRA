const navigation = [
  { href: "#principles", label: "Principles" },
  { href: "#workflow", label: "Workflow" },
  { href: "#scope", label: "Java scope" },
  { href: "#status", label: "Project status" },
];

export function SiteHeader() {
  return (
    <header className="site-header">
      <a className="wordmark" href="#top" aria-label="QIVRA home">
        <span className="wordmark-name">QIVRA</span>
        <span className="wordmark-descriptor">Software modernization</span>
      </a>

      <nav className="primary-nav" aria-label="Main navigation">
        {navigation.map((item) => (
          <a className="nav-link" href={item.href} key={item.href}>
            {item.label}
          </a>
        ))}
      </nav>

      <a
        className="header-link"
        href="https://github.com/Rafli161102/QIVRA/blob/main/docs/ROADMAP.md"
        target="_blank"
        rel="noreferrer"
      >
        Engineering plan <span aria-hidden="true">↗</span>
      </a>
    </header>
  );
}
