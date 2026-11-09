import React, { type ReactNode } from "react";
import styles from "./styles.module.css";

/* Visual building blocks of the "GraphQL client" blog post. */

type Stat = { value: string; label: string };

export const Stats = ({ stats }: { stats: Stat[] }): ReactNode => (
  <div className={styles.stats}>
    {stats.map(({ value, label }) => (
      <div key={label} className={styles.stat}>
        <span className={styles.statValue}>{value}</span>
        <span className={styles.statLabel}>{label}</span>
      </div>
    ))}
  </div>
);

const Arrow = (): ReactNode => (
  <div className={styles.arrow} aria-hidden="true">
    <span className={styles.arrowLine} />
    <span className={styles.arrowHead} />
  </div>
);

export const Pipeline = (): ReactNode => (
  <figure className={styles.pipeline}>
    <div className={`${styles.stage} ${styles.stageGraphql}`}>
      <span className={styles.stageKicker}>1 · your schema</span>
      <span className={styles.stageFile}>schema.graphql</span>
      <code className={styles.stageCode}>todo(id: ID!): Todo</code>
    </div>
    <Arrow />
    <div className={`${styles.stage} ${styles.stageWirespec}`}>
      <span className={styles.stageKicker}>2 · converted</span>
      <span className={styles.stageFile}>todo.ws</span>
      <code className={styles.stageCode}>rpc QueryTodo {"{ id: ID }"} -&gt; Todo?</code>
    </div>
    <Arrow />
    <div className={`${styles.stage} ${styles.stageClient}`}>
      <span className={styles.stageKicker}>3 · generated</span>
      <span className={styles.stageFile}>GraphQLClient</span>
      <span className={styles.chips}>
        <span className={`${styles.chip} ${styles.kotlin}`}>Kotlin</span>
        <span className={`${styles.chip} ${styles.java}`}>Java</span>
        <span className={`${styles.chip} ${styles.scala}`}>Scala</span>
      </span>
    </div>
    <figcaption>One schema in, a typed client per language out, all at build time.</figcaption>
  </figure>
);

const icons = {
  typed: (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M4 7V4h16v3M9 20h6M12 4v16" />
    </svg>
  ),
  select: (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M3 5h18M6 12h12M10 19h4" />
    </svg>
  ),
  errors: (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12 3 2 20h20L12 3zM12 10v4M12 17v.5" />
    </svg>
  ),
  zero: (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M21 16V8l-9-5-9 5v8l9 5 9-5zM3.3 7.3 12 12l8.7-4.7M12 22V12" />
    </svg>
  ),
} as const;

export type IconName = keyof typeof icons;

export const Features = ({ children }: { children: ReactNode }): ReactNode => (
  <div className={styles.features}>{children}</div>
);

export const Feature = ({ icon, title, children }: { icon: IconName; title: string; children: ReactNode }): ReactNode => (
  <div className={styles.feature}>
    <span className={styles.featureIcon}>{icons[icon]}</span>
    <h3 className={styles.featureTitle}>{title}</h3>
    <div className={styles.featureBody}>{children}</div>
  </div>
);

export const Steps = ({ children }: { children: ReactNode }): ReactNode => (
  <ol className={styles.steps}>{children}</ol>
);

export const Step = ({ title, children }: { title: string; children: ReactNode }): ReactNode => (
  <li className={styles.step}>
    <span className={styles.stepTitle}>{title}</span>
    <div className={styles.stepBody}>{children}</div>
  </li>
);

export const Versus = ({ children }: { children: ReactNode }): ReactNode => (
  <div className={styles.versus}>{children}</div>
);

export const Side = ({ label, title, accent, children }: { label: string; title: string; accent?: boolean; children: ReactNode }): ReactNode => (
  <div className={`${styles.side} ${accent ? styles.sideAccent : ""}`}>
    <span className={styles.sideLabel}>{label}</span>
    <h3 className={styles.sideTitle}>{title}</h3>
    <div className={styles.sideBody}>{children}</div>
  </div>
);

type Entry = { name: string; note?: string; fresh?: boolean; children?: Entry[] };

const Tree = ({ entries }: { entries: Entry[] }): ReactNode => (
  <ul>
    {entries.map(({ name, note, fresh, children }) => (
      <li key={name} className={fresh ? styles.fresh : undefined}>
        <span className={children ? styles.dir : styles.file}>{name}</span>
        {note && <span className={styles.note}>{note}</span>}
        {children && children.length > 0 && <Tree entries={children} />}
      </li>
    ))}
  </ul>
);

export const FileTree = ({ entries }: { entries: Entry[] }): ReactNode => (
  <div className={styles.tree}>
    <Tree entries={entries} />
  </div>
);
