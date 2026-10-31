const stages = [
  {
    number: "01",
    name: "Discover",
    detail: "Locate Maven and Gradle project boundaries.",
  },
  {
    number: "02",
    name: "Inventory",
    detail: "Record Java versions, dependencies, plugins, and framework signals.",
  },
  {
    number: "03",
    name: "Assess",
    detail: "Prepare an evidence-led modernization assessment.",
  },
  {
    number: "04",
    name: "Review",
    detail: "Inspect proposed changes before anything is applied.",
  },
  {
    number: "05",
    name: "Validate",
    detail: "Run builds and tests in an isolated workspace.",
  },
];

export function MigrationTrace() {
  return (
    <figure className="trace-panel" aria-labelledby="trace-title">
      <figcaption className="trace-heading">
        <div>
          <p className="trace-kicker">Migration Trace / 001</p>
          <h2 id="trace-title">The intended route</h2>
        </div>
        <span className="trace-index">JAVA</span>
      </figcaption>

      <ol className="trace-list">
        {stages.map((stage) => (
          <li className="trace-step" key={stage.number}>
            <span className="trace-number">{stage.number}</span>
            <div className="trace-copy">
              <h3>{stage.name}</h3>
              <p>{stage.detail}</p>
            </div>
            <span className="trace-mark" aria-hidden="true" />
          </li>
        ))}
      </ol>

      <p className="trace-footnote">
        Proposed product workflow. This is not a live repository scan.
      </p>
    </figure>
  );
}
