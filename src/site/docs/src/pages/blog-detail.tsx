import React from "react";
import Layout from "@theme/Layout";
import styles from "./blogDetail.module.css";
import Heading from "@theme/Heading";
import Link from "@docusaurus/Link";
import clsx from "clsx";

export default function BlogDetail() {
  return (
    <Layout title="Blog Detail" description="Case study: LSEG and API design">
      <main className={styles.blogMain}>
        <section className={styles.hero}>
          <h1>Enhancing API design at the London Stock Exchange Group</h1>
        </section>

        <section className={styles.imageSection}>
          <img
            src="/img/code-snippet.png"
            alt="Code snippet"
            className={styles.heroImage}
          />
        </section>

        <section className={styles.content}>
          <p>
            In the fast-paced world of financial services, having consistent and
            efficient APIs and SDKs is crucial. At Microsoft, we've been working
            closely with LSEG (London Stock Exchange Group) to streamline their
            development process using TypeSpec. This collaboration has been very
            beneficial and educational for both sides, and we’re excited to
            share our insights with you.
          </p>

          <h2>Background</h2>
          <p>
            LSEG faced the significant challenge of integrating multiple APIs
            derived from different design schemas. There are many reasons why an
            organization could find itself in a situation like this. For
            example, APIs may have been created at different times, by different
            teams, for different purposes like developing a user interface or
            delivering data as files, or they may have come from acquisitions.
          </p>
          <p>
            This inconsistency made it tough for LSEG to provide APIs with a
            unified customer experience, and limited how customers could
            integrate them into their workflows. That’s when LSEG turned to
            TypeSpec, a new API design language created by Microsoft, with the
            intention of using TypeSpec to bring consistency and efficiency to
            their API and SDK development.
          </p>
          <p>
            Our collaboration with LSEG began with initial discussions involving
            key individuals on both sides. LSEG’s goal was to release a version
            of their APIs defined using TypeSpec, which would be used to
            generate OpenAPI specifications and SDKs in TypeScript and Python
            for use by internal and external customers.
          </p>
        </section>
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
