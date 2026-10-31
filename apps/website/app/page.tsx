import { MigrationTrace } from "@/components/migration-trace";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";

const principles = [
  {
    number: "A",
    title: "Review before mutation",
    description:
      "Proposed changes should be visible and reviewable before they touch a source tree.",
  },
  {
    number: "B",
    title: "Deterministic first",
    description:
      "Prefer explicit, repeatable recipes. Use AI assistance only where it adds measurable value.",
  },
  {
    number: "C",
    title: "Validation is part of the change",
    description:
      "A file diff is not proof of a successful migration. Builds and tests belong in the workflow.",
  },
  {
    number: "D",
    title: "Treat source as untrusted",
    description:
      "Repository inspection and later build execution need clear security boundaries.",
  },
];

export default function HomePage() {
  return (
    <>
      <a className="skip-link" href="#main-content">
        Skip to content
      </a>

      <div className="page-shell" id="top">
        <SiteHeader />

        <main id="main-content" tabIndex={-1}>
          <section className="hero-section" aria-labelledby="hero-title">
            <div className="hero-copy">
              <p className="eyebrow">Engineering Atlas / Field note 001</p>
              <h1 id="hero-title">
                Understand the system.
                <span> Then change it.</span>
              </h1>
              <p className="hero-lead">
                QIVRA is being built to inspect legacy software, map its
                structure, and make each proposed modernization step
                reviewable.
              </p>
              <div className="hero-actions">
                <a className="button-primary" href="#workflow">
                  Follow the workflow
                </a>
                <a className="text-link" href="#scope">
                  See the first scope
                </a>
              </div>
              <div className="hero-caption">
                <span>Initial vertical slice</span>
                <strong>Java 8 / 11 to Java 17</strong>
                <span>Maven and Gradle projects</span>
              </div>
            </div>

            <MigrationTrace />
          </section>

          <div className="status-ribbon" aria-label="Project status">
            <span className="status-marker" aria-hidden="true" />
            <p>
              <strong>Pre-alpha.</strong> Product foundation documented.
              Analyzer implementation is the next milestone.
            </p>
            <a href="#status">Current status <span aria-hidden="true">↗</span></a>
          </div>

          <section className="principles-section section-block" id="principles">
            <div className="section-rail">
              <p className="section-index">01 / Operating principles</p>
              <h2>Change needs a reason. And a way back.</h2>
              <p className="section-intro">
                Modernization affects more than syntax. QIVRA is being
                designed around traceable decisions and controlled change.
              </p>
            </div>

            <div className="principle-list">
              {principles.map((principle) => (
                <article className="principle-row" key={principle.number}>
                  <span className="principle-number">{principle.number}</span>
                  <h3>{principle.title}</h3>
                  <p>{principle.description}</p>
                </article>
              ))}
            </div>
          </section>

          <section className="workflow-section section-block" id="workflow">
            <div className="workflow-heading">
              <p className="section-index">02 / Migration Trace</p>
              <h2>From codebase discovery to validation.</h2>
            </div>
            <div className="workflow-content">
              <p>
                The planned workflow starts with static inspection. It builds
                an inventory before preparing an assessment, then places
                review between a proposed transformation and its validation.
              </p>
              <ol className="workflow-sequence" aria-label="Modernization sequence">
                <li>Discover</li>
                <li>Inventory</li>
                <li>Assess</li>
                <li>Review</li>
                <li>Validate</li>
              </ol>
              <p className="workflow-note">
                The analyzer and transformation workflow are not available
                yet. This sequence describes the intended product behavior.
              </p>
            </div>
          </section>

          <section className="scope-section section-block" id="scope">
            <div className="scope-heading">
              <p className="section-index">03 / First vertical slice</p>
              <h2>Start with the build system.</h2>
              <p>
                The first implementation target is Java modernization for
                projects that use Maven or Gradle. The initial path is Java
                8 or 11 toward Java 17.
              </p>
              <span className="scope-status">Planned scope / Not yet released</span>
            </div>

            <dl className="scope-ledger">
              <div>
                <dt>Build systems</dt>
                <dd>Maven <span>/</span> Gradle</dd>
              </div>
              <div>
                <dt>Starting versions</dt>
                <dd>Java 8 <span>/</span> Java 11</dd>
              </div>
              <div>
                <dt>Initial target</dt>
                <dd>Java 17</dd>
              </div>
              <div>
                <dt>Discovery constraint</dt>
                <dd>Static inspection. Do not execute build scripts during discovery.</dd>
              </div>
            </dl>
          </section>

          <section className="status-section section-block" id="status">
            <div className="status-heading">
              <p className="section-index">04 / Development record</p>
              <h2>Built in milestones. Reported as they are.</h2>
            </div>
            <div className="status-content">
              <p className="status-lead">
                QIVRA is in pre-alpha. The repository foundation and product
                direction are documented. The software analyzer has not been
                released.
              </p>
              <div className="milestone-table" role="table" aria-label="QIVRA development milestones">
                <div className="milestone-row milestone-head" role="row">
                  <span role="columnheader">Milestone</span>
                  <span role="columnheader">Work</span>
                  <span role="columnheader">Status</span>
                </div>
                <div className="milestone-row" role="row">
                  <span role="cell">P0.1</span>
                  <span role="cell">Product and engineering foundation</span>
                  <span className="milestone-state complete" role="cell">Complete</span>
                </div>
                <div className="milestone-row" role="row">
                  <span role="cell">P0.2</span>
                  <span role="cell">Repository and build-system analyzer</span>
                  <span className="milestone-state next" role="cell">Next</span>
                </div>
                <div className="milestone-row" role="row">
                  <span role="cell">P0.3 onward</span>
                  <span role="cell">Assessment, transformation, validation, and reporting</span>
                  <span className="milestone-state planned" role="cell">Planned</span>
                </div>
              </div>
              <a
                className="status-link"
                href="https://github.com/Rafli161102/QIVRA/blob/main/docs/ROADMAP.md"
                target="_blank"
                rel="noreferrer"
              >
                Read the milestone roadmap <span aria-hidden="true">↗</span>
              </a>
            </div>
          </section>
        </main>

        <SiteFooter />
      </div>
    </>
  );
}
