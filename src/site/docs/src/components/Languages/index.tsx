import React, { type ReactNode } from "react";
import styles from "./styles.module.css";

type Language = "Java" | "Kotlin" | "Scala" | "TypeScript" | "Python" | "Rust";

const classOf: Record<Language, string> = {
  Java: styles.java,
  Kotlin: styles.kotlin,
  Scala: styles.scala,
  TypeScript: styles.typescript,
  Python: styles.python,
  Rust: styles.rust,
};

/** The languages an integration applies to, as one badge each. */
export const Languages = ({ languages }: { languages: Language[] }): ReactNode => (
  <span className={styles.languages} aria-label={`Languages: ${languages.join(", ")}`}>
    {languages.map((language) => (
      <span key={language} className={`${styles.badge} ${classOf[language]}`}>
        {language}
      </span>
    ))}
  </span>
);
