import clsx from "clsx";
import Link from "@docusaurus/Link";
import Layout from "@theme/Layout";
import CodeBlock from "@theme/CodeBlock";
import Heading from "@theme/Heading";
import "../css/custom.css";
import styles from "./index.module.css";
import Tabs from "@theme/Tabs";
import TabItem from "@theme/TabItem";
import { JSX } from "react";

function HomepageHeader() {
  return (
    <header className={clsx("hero hero--primary", styles.heroBanner)}>
      <div className="container">
        <Heading as="h1" className={clsx("hero__title hero-heading", styles.heroHeading)}>Wirespec <br /> your <span className="primary-text-color">APIs</span></Heading>
        <p className={clsx("hero__subtitle", styles.heroSubtitle)}>Simplify your API development, with human readable specifications and guarantee strict adherence</p>
        <div className={styles.buttons}>
          <Link className={clsx(styles.button, styles.buttonPrimary)} to="/docs/getting-started">Get started</Link>
          <Link className={clsx(styles.button, styles.buttonDefault)} to="https://playground.wirespec.io/">Playground</Link>
        </div>
      </div>
    </header>
  );
}

export default function Home(): JSX.Element {
  return (
    <Layout title="Wirespec your APIs" description="Simplify your API development workflows, accelerate implementation, and guarantee strict adherence to defined contract specifications" >
      <HomepageHeader />
      <main className={clsx(styles.page)}>
        <section className="designFirstContent designFirstContent-blur pt-0">
          <div className="container">
            <div className="row row--align-center">
              <div className="col col--5">
                <div className="designFirstContent-left">
                  <Heading as="h2" className={clsx(styles.heading2)}><span className="primary-text-color">Human</span> readable <br/>spec first </Heading>
                  <p>Designing, building and testing interfaces is time consuming and error prone. Automate and standardize your workflow to eliminate the following problems:</p>
                  <ul className={styles.listItemGroup}>
                    <li className={clsx(styles.listItem)}>Negotiating interfaces between teams is hard</li>
                    <li className={clsx(styles.listItem)}>Tool overload to cover all (REST, Queues, Models)</li>
                    <li className={clsx(styles.listItem)}>Tedious experience writing specifications</li>
                    <li className={clsx(styles.listItem)}>Dependency hell in generated code</li>
                    <li className={clsx(styles.listItem)}>Choice paralysis in libraries, tools and frameworks</li>
                    <li className={clsx(styles.listItem)}>Polyglot implementation struggles</li>
                  </ul>
                </div>
              </div>
              <div className="col col--7">
                <div className="card-nospace">
                  <img src="/img/design-first-human-readable.png" alt="design-first-human-readable"/>
                </div>
              </div>
            </div>
          </div>
        </section>
        <section className="designFirstContent">
          <div className="container">
            <div className="row row--align-center">
              <div className="col col--7">
                <div className="card card-border-bottom card-nospace">
                  <img src="/img/wirespec-double-px.jpg" alt="design-first-human-readable"/>
                </div>
              </div>
              <div className="col col--5">
                <div className="designFirstContent-right">
                  <Heading as="h2" className={clsx(styles.heading2)}>Why <span className="primary-text-color">Wirespec</span></Heading>
                  <p>Wirespec is a language that simplifies interface design. A requirement for truly contract-first specifications is readability, by humans, not machines.</p>
                  <p>By generating producer and consumer it acts as the single source of truth for your implementations, reducing errors, ensuring consistency, and enabling simultaneous frontend and backend development.</p>
                  <p>Additionally, Wirespec guarantees implementations follow specification by compile-time checks, automated testing, and validation. In short, Wirespec streamlines API development, reduces errors, and improves team alignment.</p>
                </div>
              </div>
            </div>
          </div>
        </section>
        <section className="steps">
          <div className="container">
            <div className="grid grid-cols-3">
              <div className="card card-border-left">
                <div className="icon-box">
                  <svg width="28" height="28" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg" >
                    <path d="M18.6673 24.5L18.6673 22.1667C18.6673 20.929 18.1757 19.742 17.3005 18.8668C16.4253 17.9917 15.2383 17.5 14.0007 17.5L7.00065 17.5C5.76297 17.5 4.57599 17.9917 3.70082 18.8668C2.82565 19.742 2.33398 20.929 2.33398 22.1667L2.33398 24.5" stroke="#101010" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                    <path d="M10.5007 12.8333C13.078 12.8333 15.1673 10.744 15.1673 8.16667C15.1673 5.58934 13.078 3.5 10.5007 3.5C7.92332 3.5 5.83398 5.58934 5.83398 8.16667C5.83398 10.744 7.92332 12.8333 10.5007 12.8333Z" stroke="#101010" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                    <path d="M25.668 24.5007L25.668 22.1673C25.6672 21.1334 25.3231 20.1289 24.6896 19.3117C24.0561 18.4945 23.1691 17.9108 22.168 17.6523" stroke="#101010" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                    <path d="M18.668 3.65234C19.6718 3.90936 20.5615 4.49316 21.1969 5.31171C21.8322 6.13025 22.1771 7.13698 22.1771 8.17318C22.1771 9.20938 21.8322 10.2161 21.1969 11.0346C20.5615 11.8532 19.6718 12.437 18.668 12.694" stroke="#101010" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                </div>
                <p>Create human-readable specifications automatically</p>
              </div>
              <div className="card">
                <div className="icon-box">
                  <svg width="28" height="28" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg" >
                    <path d="M10.5 3.5H5.83333C4.54467 3.5 3.5 4.54467 3.5 5.83333V10.5C3.5 11.7887 4.54467 12.8333 5.83333 12.8333H10.5C11.7887 12.8333 12.8333 11.7887 12.8333 10.5V5.83333C12.8333 4.54467 11.7887 3.5 10.5 3.5Z" stroke="black" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                    <path d="M8.16602 12.832V17.4987C8.16602 18.1175 8.41185 18.711 8.84943 19.1486C9.28702 19.5862 9.88051 19.832 10.4993 19.832H15.166" stroke="black" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                    <path d="M22.166 15.168H17.4993C16.2107 15.168 15.166 16.2126 15.166 17.5013V22.168C15.166 23.4566 16.2107 24.5013 17.4993 24.5013H22.166C23.4547 24.5013 24.4993 23.4566 24.4993 22.168V17.5013C24.4993 16.2126 23.4547 15.168 22.166 15.168Z" stroke="black" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                </div>
                <p>Accelerate development worfkflows</p>
              </div>
              <div className="card card-border-right">
                <div className="icon-box">
                  <svg width="28" height="28" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg" >
                    <path d="M11.667 2.33203H16.3337" stroke="black" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                    <path d="M14 16.332L17.5 12.832" stroke="black" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                    <path d="M14.0003 25.6667C19.155 25.6667 23.3337 21.488 23.3337 16.3333C23.3337 11.1787 19.155 7 14.0003 7C8.84567 7 4.66699 11.1787 4.66699 16.3333C4.66699 21.488 8.84567 25.6667 14.0003 25.6667Z" stroke="black" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                </div>
                <p>Reduce errors and increase efficieny</p>
              </div>
            </div>
          </div>
        </section>
        <section className="how">
          <div className="section-header">
            <div className="container">
              <Heading as="h2" className={clsx(styles.heading2)}><span className="primary-text-color">How</span></Heading>
            </div>
          </div>
          <div className="inner-section" id="how-design">
            <div className="container">
              <div className="row row--align-center">
                <div className="col col--6">
                  <div>
                    <Heading as="h2" className={clsx(styles.heading2)}>Contract</Heading>
                    <p>Wirespec’s contract-first approach empowers teams by establishing one clear, authoritative source of truth, independent of implementation or technology. It drives robust, consistent interface designs, boosts team alignment, eliminates ambiguity, and accelerates effective collaboration. </p>
                    <Link className={clsx("button-link", styles.button, styles.buttonPrimary)} to="/how#contract">Explore Key Principles</Link>
                  </div>
                </div>
                <div className="col col--6 code-block-col">
                  <div className="card card-border-bottom card-nospace">
                    <CodeBlock language="wirespec" title="todo.ws" className="custom-code-block">
                      {`type TodoDto {
    id: Integer?,
    name: String
}

endpoint GetTodos GET /api/todos -> {
    200 -> TodoDto[]
}

endpoint GetById GET /api/todos/{id: Integer} -> {
    200 -> TodoDto
    404 -> String
}

endpoint CreateTodo POST TodoDto /api/todos -> {
    201 -> TodoDto
}

endpoint DeleteTodo DELETE /api/todos/{id: Integer} -> {
    204 -> Unit
    404 -> String
}

endpoint UpdateTodo PUT TodoDto /api/todos/{id: Integer} -> {
    200 -> TodoDto
}`}
                    </CodeBlock>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div className="inner-section" id="how-generate">
            <div className="container">
              <div className="row row--align-center">
                <div className="col col--6 code-block-col">
                  <div>
                    <Tabs>
                      <TabItem value="TypeScript" label="TypeScript">
                        <CodeBlock language="bash">{`wirespec compile --input . --language typescript`}</CodeBlock>
                        <p>This will generate the following file:</p>
                        <CodeBlock>{`project/out/community/flock/wirespec/generated/
└─ Todo.ts`}</CodeBlock>
                      </TabItem>

                      <TabItem value="Kotlin" label="Kotlin">
                        <CodeBlock language="bash">{`wirespec compile --input . --language kotlin`}</CodeBlock>
                        <p>This will generate the following file:</p>
                        <CodeBlock>{`project/out/community/flock/wirespec/generated/
└─ Todo.kt`}</CodeBlock>
                      </TabItem>

                      <TabItem value="Java" label="Java">
                        <CodeBlock language="bash">{`wirespec compile --input . --language java`}</CodeBlock>
                        <p>This will generate the following files:</p>
                        <CodeBlock>{`project/out/community/flock/wirespec/generated/
└─ CreateTodoEndpoint.java
└─ DeleteTodoEndpoint.java
└─ GetByIdEndpoint.java
└─ GetTodosEndpoint.java
└─ TodoDto.java
└─ UpdateTodoEndpoint.java`}</CodeBlock>
                      </TabItem>
                    </Tabs>
                  </div>
                </div>
                <div className="col col--6">
                  <div>
                    <Heading as="h2" className={clsx(styles.heading2)}>Generate</Heading>
                    <p>Our code generation vision embraces a contract-first strategy, placing specifications as the definitive source of truth. This powerful approach ensures durable, reusable interfaces, isolates domain definitions from implementation details, and leverages automated, typesafe code generation-boosting clarity, collaboration, and efficiency across teams.</p>
                    <Link className={clsx("button-link", styles.button, styles.buttonPrimary)} to="/how#generate">Code Generation</Link>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div className="inner-section" id="how-validate">
            <div className="container">
              <div className="row row--align-center">
                <div className="col col--6">
                  <div className="how-content">
                    <Heading as="h2" className={clsx(styles.heading2)}>Validate</Heading>
                    <p>Wirespec specifications serve as the definitive source of truth, ensuring API implementations align seamlessly with defined interfaces. Leveraging automated test data generation and mock servers, they streamline robust validation, swiftly identify discrepancies, reduce ambiguity, and boost reliability - empowering teams to deliver consistent, predictable API behavior.</p>
                    <Link className={clsx("button-link", styles.button, styles.buttonPrimary )} to="/how#validate" >Ensure Compliance</Link>
                  </div>
                </div>
                <div className="col col--6 code-block-col">
                  <img src="/img/validate.png" alt="" />
                </div>
              </div>
            </div>
          </div>
        </section>
        <section className="other-capabilites other-capabilites-shadow ">
          <div className="container">
            <div className="row row--align-center">
              <div className="col col--6">
                <Heading as="h2" className={clsx(styles.heading2)}>Other <span>capabilities</span></Heading>
              </div>
              <div className="col col--6">
                <div className="card card-other card-border-bottom">
                  <Heading as="h4" className={clsx(styles.heading2)}>Plugins</Heading>
                  <p>Wirespec supports various plugins for integration into a variety of ecosystems. </p>
                  <p><span>Plugins:</span> Cli, Gradle, Maven, NPM </p>
                </div>
              </div>
              <div className="col col--6">
                <div className="card card-other card-border-bottom">
                  <Heading as="h4" className={clsx(styles.heading2)}>IDE's</Heading>
                  <p>Wirespec supports two IDEs: IntelliJ IDEA and VS Code.</p>
                  <p><span>Plugins:</span> IntelliJ IDEA and VS Code.</p>
                </div>
              </div>
              <div className="col col--6">
                <div className="card card-other card-border-bottom">
                  <Heading as="h4" className={clsx(styles.heading2)}>Emitters</Heading>
                  <p>Wirespec generates functional and dependency-free code.</p>
                  <p><span>Plugins:</span> Python, Java, Kotlin, Typescript, Javascript</p>
                </div>
              </div>
              <div className="col col--6">
                <div className="card card-other card-border-bottom">
                  <Heading as="h4" className={clsx(styles.heading2)}>Converters</Heading>
                  <p>Wirespec offers the capability to convert from existing specification formats.</p>
                  <p><span>Plugins:</span> OpenAPI, Avro</p>
                </div>
              </div>
              <div className="col col--6">
                <div className="card card-other card-border-bottom">
                  <Heading as="h4" className={clsx(styles.heading2)}>Integration</Heading>
                  <p>Wirespec has integrations with major frameworks.</p>
                  <p><span>Plugins:</span> Spring, Jackson, Avro</p>
                </div>
              </div>
            </div>
          </div>
        </section>
        <section className="other-capabilites">
          <div className="container">
            <div className="row">
              <div className="col col--6">
                <Heading as="h2" className={clsx(styles.heading2)}>Comparison with other <span>tools</span></Heading>
                <p>By understanding your project's specific needs and architecture, you can choose the most suitable specification tool to streamline development and improve collaboration.</p>
              </div>
            </div>
            <div className="table">
              <table>
                <thead>
                  <tr>
                    <th>Feature / Aspect</th>
                    <th>Wirespec</th>
                    <th>OpenAPI</th>
                    <th>AsyncAPI</th>
                    <th>Typespec</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>Primary focus</td>
                    <td>Streamlined API design</td>
                    <td>Restful API’s</td>
                    <td>Asynchronous API’s</td>
                    <td>Programmatic API design</td>
                  </tr>
                  <tr>
                    <td>Specification Format</td>
                    <td>Minimal, Wirespec syntax</td>
                    <td>YAML/JSON</td>
                    <td>YAML/JSON</td>
                    <td>TypeScript-like syntax</td>
                  </tr>
                  <tr>
                    <td>Ecosystem Support</td>
                    <td>Emerging</td>
                    <td>Mature and extensive</td>
                    <td>Growing rapidly</td>
                    <td>Emerging</td>
                  </tr>
                  <tr>
                    <td>Code Generation</td>
                    <td>Built-in, cross-language</td>
                    <td>Extensive via tools</td>
                    <td>Robust via tools</td>
                    <td>Flexible and customizable</td>
                  </tr>
                  <tr>
                    <td>Best for Microservices</td>
                    <td>Excellent</td>
                    <td>Good</td>
                    <td>Excellent</td>
                    <td>Good</td>
                  </tr>
                  <tr>
                    <td>Asynchronous Support</td>
                    <td>Limited</td>
                    <td>Limited</td>
                    <td>Excellent</td>
                    <td>Limited</td>
                  </tr>
                  <tr>
                    <td>Ease of Use</td>
                    <td>High (minimalist)</td>
                    <td>Moderate (can be verbose)</td>
                    <td>Moderate</td>
                    <td>Moderate (requires coding)</td>
                  </tr>
                  <tr>
                    <td>Technology</td>
                    <td>Multiplatform (JVM Node.js Binary)</td>
                    <td>JVM</td>
                    <td>Node.js</td>
                    <td>Node.js</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>
        <section className="other-capabilites">
          <div className="container">
            <div className="row row--align-center">
              <div className="col col--6">
                <Heading as="h2" className={clsx(styles.heading2)}>Why Wirespec from <span>Flock</span></Heading> 
                <p>Choosing Wirespec means selecting a product developed and supported by Flock., a community of passionate and driven professionals dedicated to continuous improvement.</p>
                <p>Our dedication to ongoing innovation and quality ensures that Wirespec not only meets current demands but also evolves with emerging technologies.</p>
                <p>By actively engaging in the open-source community and maintaining transparent development processes, we ensure that continuity and advancement are well-anchored. At Flock., we combine deep technical expertise with a strong focus on collaboration and knowledge sharing, providing you with a partner who elevates your projects to new heights.</p>
              </div>
              <div className="col col--1"></div>
              <div className="col col--5">
                <div className="card-flock-wrap">
                  <div className="card card-border-bottom card-nospace card-flock">
                    <iframe width="560" height="315" src="https://www.youtube.com/embed/LMA5ByWUhBo?si=4BK1w7GIIVfr2doG" title="YouTube video player" frameBorder="0" allow="accelerometer; autoplay; clipboard-write;  encrypted-media; gyroscope; picture-in-picture; web-share" referrerPolicy="strict-origin-when-cross-origin" allowFullScreen></iframe>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>
    </Layout>
  );
}
