import React from "react";
import Layout from "@theme/Layout";
import styles from "./contract.module.css";
import Heading from "@theme/Heading";
import Link from "@docusaurus/Link";
import clsx from "clsx";
import CodeBlock from '@theme/CodeBlock';


export default function ContractPage() {
  return (
    <Layout
      title="Contract-First Design"
      description="Understanding Wirespec's contract-first approach"
    >
      <main className={styles.contractMain}>
        <div id="contract">
          <section className={styles.hero} style={{ marginTop: "10rem" }}>
            <h1>Contract</h1>
            <p>
              The contract–first approach for interface design, as envisioned by
              Wirespec, is a methodology that emphasizes the creation of a
              formal specification before any implementation occurs. This
              approach recognizes the importance of defining clear and detailed
              contracts (specifications) as the cornerstone of building robust
              interfaces. Here's a more detailed exploration of this concept:
            </p>
          </section>

          <section className={styles.section}>
            <h2>Key Principles of the Contract-First Approach:</h2>

            <ol className={styles.principles}>
              <li>
                <strong>Single Source of Truth:</strong>
                <ul>
                  <li>
                    <span className={styles.bullet}></span> At the heart of the
                    contract–first approach is the idea that a well-defined
                    contract serves as the single authoritative source of truth
                    for the interface.
                  </li>
                  <li>
                    <span className={styles.bullet}></span> These specifications
                    are written in a concise and human–readable language, making
                    them accessible and understandable across teams, regardless
                    of their technical expertise.
                  </li>
                </ul>
              </li>

              <li>
                <strong>Independence from Implementation:</strong>
                <ul>
                  <li>
                    <span className={styles.bullet}></span> The interface
                    contract is agnostic of any specific programming language,
                    framework, or architectural style.
                  </li>
                  <li>
                    <span className={styles.bullet}></span> By abstracting the
                    specification from implementation details, teams are free to
                    implement the interface in diverse tech stacks while
                    maintaining consistency and adherence to the defined
                    standards.
                  </li>
                </ul>
              </li>

              <li>
                <strong>Collaborative Design–Centric Methodology:</strong>
                <ul>
                  <li>
                    <span className={styles.bullet}></span> This approach
                    simplifies communication between cross–functional teams
                    (e.g., developers, designers, product managers, and quality
                    assurance teams).
                  </li>
                  <li>
                    <span className={styles.bullet}></span> Specifications serve
                    as a shared reference point, ensuring everyone is aligned on
                    expectations and requirements for the interface. This
                    reduces ambiguity and miscommunication during the
                    development lifecycle.
                  </li>
                </ul>
              </li>
            </ol>
          </section>
          <section className="card card-border-bottom card-nospace">
            <CodeBlock language="js" title="example.js">
              {`function hello() {
  return 'hi';
}`}
            </CodeBlock>
          </section>
        </div>

        <div id="generate">
          <section className={styles.generateSection}>
            <h2>Generate</h2>
            <p>
              The vision of code generation centers on enabling a contract–first
              approach where specifications act as the single source of truth.
              By prioritizing the specification over the implementation,
              development is streamlined, reducing ambiguity and fostering
              better collaboration among cross–functional teams.
            </p>
            <p>
              Interfaces, often mapped to the domain, play a pivotal role in
              this vision. They are designed to have a longer lifespan compared
              to implementation code and must remain isolated to ensure they are
              reusable, adaptable, and independent of specific technological
              constraints. This isolation allows interfaces to act as durable
              blueprints, transcending implementation changes and maintaining a
              stable foundation for the system’s evolution.
            </p>
            <p>
              Code generation helps bridge the gap between specification and
              implementation by automating the creation of typesafe, functional,
              and dependency–free code from the specification. This approach
              ensures that the generated code precisely reflects the defined
              interfaces, capturing all possible inputs, outputs, and
              interactions in a consistent and predictable manner.
            </p>
          </section>
          <section className="card card-border-bottom card-nospace">
           <CodeBlock language="js" title="example.js">
              {`function hello() {
  return 'hi';
}`}
            </CodeBlock>
          </section>
        </div>

        <div id="validate">
          <section className={styles.validateSection}>
            <h2>Validate</h2>
            <p>
              Specifications, such as those defined by Wirespec, play a critical
              role in validating the implementation of APIs by serving as the
              single source of truth. They ensure alignment between expected
              interfaces and actual code, facilitating robust development and
              testing practices.
            </p>

            <div className={styles.validateBlock}>
              <h3>Validation Through Specifications</h3>
              <p>
                Specifications provide a blueprint for the implementation. By
                describing the structure of data, endpoints, and channels in a
                concise, human–readable manner, they form the foundation for
                verifying that implementations align precisely with defined
                interfaces. This is especially helpful for cross–functional
                teams aiming to reduce ambiguity and miscommunication during
                development.
              </p>
            </div>

            <div className={styles.validateBlock}>
              <h3>Generating Random Test Data</h3>
              <p>
                Using specifications to generate randomized yet valid test data
                ensures that input data conforms to expected formats and
                constraints. Tools like Wirespec can leverage the detailed
                schema definitions to create test scenarios that systematically
                cover edge cases, identifying potential issues early in the
                development cycle. This automated data generation streamlines
                testing and improves reliability.
              </p>
            </div>

            <div className={styles.validateBlock}>
              <h3>Mock Servers for Testing</h3>
              <p>
                Mock servers can be generated based on the same specification,
                reducing dependency on live systems for testing. These servers
                simulate the behavior of APIs as defined in the contracts,
                allowing testing environments to mimic production without
                requiring complete backend implementations. This approach helps
                developers verify that their code interacts correctly with the
                API’s interface, ensuring input/output consistency and expected
                status codes are met.
              </p>
            </div>

            <div className={styles.validateBlock}>
              <h3>Detecting Discrepancies Early</h3>
              <p>
                By using the specification to drive both code generation and
                validation processes, inconsistencies can be detected before
                they reach production. Automated tests generated directly from
                the specification confirm that the code adheres to expected
                behaviors and data formats defined in the agreed–upon contract.
              </p>
            </div>

            <p>
              In summary, specifications like those enabled by Wirespec empower
              teams to validate implementations through code generation,
              automated testing, and mock servers, ensuring reliable and
              predictable API behavior.
            </p>
          </section>
        </div>
      </main>
    </Layout>
  );
}
