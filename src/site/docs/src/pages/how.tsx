import React from "react";
import Layout from "@theme/Layout";
import styles from "./contract.module.css";
import Heading from "@theme/Heading";
import Link from "@docusaurus/Link";
import clsx from "clsx";

export default function ContractPage() {
  return (
    <Layout
      title="Contract-First Design"
      description="Understanding Wirespec's contract-first approach"
    >
      <main className={styles.contractMain}>
        <div id="contract">
          <section className={styles.hero}>
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
          <section className={styles.contractImageSection}>
            <img
              src="/img/contractImage.png"
              alt="contractImage"
              className={styles.heroImage}
            />
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
          <section className={styles.generateImageSection}>
            <img
              src="/img/generateImage.png"
              alt="generateImage"
              className={styles.heroImage}
            />
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

      <section className="start-wirespec">
        <div className="container">
          <div className="row">
            <div className="col col--12">
              <div className="card card-border-bottom card-footer">
                <div className="card-footer-header">
                  <Heading as="h2" className={clsx(styles.heading2)}>
                    Start with <span>Wirespec</span>
                  </Heading>
                  <p>
                    By understanding your project's specific needs and
                    architecture, you can choose the most suitable specification
                    tool to streamline development and improve collaboration.
                  </p>
                </div>
                <div className="icon-item-group">
                  <div className="icon-item-box">
                    <div className="icon-box">
                      <svg
                        width="20"
                        height="20"
                        viewBox="0 0 20 20"
                        fill="none"
                        xmlns="http://www.w3.org/2000/svg"
                      >
                        <path
                          d="M13.5 15L18 10.5L13.5 6"
                          stroke="black"
                          stroke-width="1.5"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        />
                        <path
                          d="M6.5 6L2 10.5L6.5 15"
                          stroke="black"
                          stroke-width="1.5"
                          stroke-linecap="round"
                          stroke-linejoin="round"
                        />
                      </svg>
                    </div>
                    <p>Free source-code</p>
                  </div>
                  <div className="icon-item-box">
                    <div className="icon-box">
                      <svg
                        width="18"
                        height="18"
                        viewBox="0 0 18 18"
                        fill="none"
                        xmlns="http://www.w3.org/2000/svg"
                      >
                        <g clip-path="url(#clip0_1_488)">
                          <path
                            d="M15 5.25L12.75 5.25C12.3522 5.25 11.9706 5.09196 11.6893 4.81066C11.408 4.52936 11.25 4.14782 11.25 3.75L11.25 1.5"
                            stroke="black"
                            stroke-width="1.5"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          />
                          <path
                            d="M6.75 13.5C6.35218 13.5 5.97064 13.342 5.68934 13.0607C5.40804 12.7794 5.25 12.3978 5.25 12L5.25 3C5.25 2.60218 5.40804 2.22064 5.68934 1.93934C5.97064 1.65804 6.35218 1.5 6.75 1.5L12 1.5L15 4.5L15 12C15 12.3978 14.842 12.7794 14.5607 13.0607C14.2794 13.342 13.8978 13.5 13.5 13.5L6.75 13.5Z"
                            stroke="black"
                            stroke-width="1.5"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          />
                          <path
                            d="M2.25 5.69922L2.25 15.2992C2.25 15.6175 2.37643 15.9227 2.60147 16.1477C2.82652 16.3728 3.13174 16.4992 3.45 16.4992L10.8 16.4992"
                            stroke="black"
                            stroke-width="1.5"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          />
                        </g>
                      </svg>
                    </div>
                    <p>Extended documentation</p>
                  </div>
                  <div className="icon-item-box">
                    <div className="icon-box">
                      <svg
                        width="18"
                        height="18"
                        viewBox="0 0 18 18"
                        fill="none"
                        xmlns="http://www.w3.org/2000/svg"
                      >
                        <g clip-path="url(#clip0_1_498)">
                          <path
                            d="M12.0008 15.7519L12.0008 14.2519C12.0008 13.4563 11.6848 12.6932 11.1222 12.1306C10.5596 11.568 9.79651 11.252 9.00087 11.252L4.5009 11.252C3.70525 11.252 2.94219 11.568 2.37959 12.1306C1.81698 12.6932 1.50092 13.4563 1.50092 14.2519L1.50092 15.7519"
                            stroke="#101010"
                            stroke-width="1.5"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          />
                          <path
                            d="M6.7509 8.25191C8.40774 8.25191 9.75088 6.90878 9.75088 5.25193C9.75088 3.59509 8.40774 2.25195 6.7509 2.25195C5.09405 2.25195 3.75092 3.59509 3.75092 5.25193C3.75092 6.90878 5.09405 8.25191 6.7509 8.25191Z"
                            stroke="#101010"
                            stroke-width="1.5"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          />
                          <path
                            d="M16.501 15.754L16.501 14.254C16.5005 13.5893 16.2792 12.9436 15.872 12.4183C15.4647 11.893 14.8946 11.5177 14.251 11.3516"
                            stroke="#101010"
                            stroke-width="1.5"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          />
                          <path
                            d="M12.001 2.35156C12.6463 2.51679 13.2182 2.89208 13.6267 3.41829C14.0351 3.94449 14.2568 4.59167 14.2568 5.25779C14.2568 5.92392 14.0351 6.57109 13.6267 7.0973C13.2182 7.6235 12.6463 7.9988 12.001 8.16402"
                            stroke="#101010"
                            stroke-width="1.5"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          />
                        </g>
                      </svg>
                    </div>
                    <p>Free community</p>
                  </div>
                </div>
                <div className="footer-action">
                  <Link
                    className={clsx(styles.button, styles.buttonPrimary)}
                    to="/docs/getting-started"
                  >
                    Get started
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </Layout>
  );
}
