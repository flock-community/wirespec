import React from "react";
import Layout from "@theme/Layout";
import styles from "./vision.module.css";

export default function VisionPage() {
  return (
    <Layout
      title="Wirespec your APIs"
      description="Simplify your API development workflows, accelerate implementation, and guarantee strict adherence to defined contract specifications"
    >
      <main className={styles.visionMain}>
        <section className={styles.visionSection}>
          <div className={styles.visionContent}>
            <h1 className={styles.title}>
              Wirespec <br /> <span>vision</span>{" "}
            </h1>
            <p className={styles.paragraph}>
              Wirespec envisions a future where interface design is streamlined
              through a contract-first approach. Specifications, defined in a
              concise and human-readable language, serve as the single source of
              truth, independent of any specific implementation. This
              design-centric methodology empowers cross-functional teams to
              collaborate effectively, ensuring alignment.
            </p>
            <p className={styles.paragraph}>
              Central to the Wirespec vision is automated code generation. From
              the contract specification, typesafe, purely functional, and
              dependency-free code is generated. This generated code
              encapsulates all possible inputs and outputs of the defined
              interface, ensuring consistency and reducing the likelihood of
              errors. By automating this process, Wirespec simplifies
              development workflows, accelerates implementation, and guarantees
              strict adherence to the defined contract specifications.
            </p>
            <p className={styles.paragraph}>
              validating and testing the implementation against the agreed-upon
              interface. The contract acts as a blueprint, allowing for
              automated testing to ensure that the implementation adheres to the
              defined behaviors and data structures. Test cases can be generated
              directly from the contract, verifying that the API responses match
              the specified schemas, status codes, and headers. This rigorous
              testing regime, driven by the contract, helps identify
              discrepancies early, guaranteeing a reliable and compliant API
              implementation.
            </p>
          </div>
        </section>
      </main>
    </Layout>
  );
}
