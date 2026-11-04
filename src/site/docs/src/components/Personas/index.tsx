import React, { type ReactNode } from "react";
import Link from "@docusaurus/Link";
import styles from "./styles.module.css";

type PersonaProps = {
  role: string;
  title: string;
  to: string;
  uses: string[];
  children: ReactNode;
};

export const Persona = ({ role, title, to, uses, children }: PersonaProps): ReactNode => (
  <Link to={to} className={styles.persona}>
    <span className={styles.role}>{role}</span>
    <h3 className={styles.title}>{title}</h3>
    <div className={styles.description}>{children}</div>
    <ul className={styles.uses}>
      {uses.map((use) => (
        <li key={use}>{use}</li>
      ))}
    </ul>
    <span className={styles.start}>Start here →</span>
  </Link>
);

export const Personas = ({ children }: { children: ReactNode }): ReactNode => (
  <div className={styles.personas}>{children}</div>
);
