import React from 'react';
import Layout from '@theme/Layout';
import type { Props } from '@theme/BlogPostPage';
import styles from './blogDetail.module.css'; // Create this CSS module

export default function BlogPostPage(props: Props): JSX.Element {
  const { content: BlogPostContent } = props;
  const { frontMatter, metadata } = BlogPostContent;
  const { title, description, date } = metadata;

  return (
    <Layout title={title} description={description}>
      <main className={styles.blogMain}>
        <section className={styles.hero}>
          <h1>{title}</h1>
        </section>

        {frontMatter.image && (
          <section className={styles.imageSection}>
            <img
              src={frontMatter.image}
              alt="Blog banner"
              className={styles.heroImage}
            />
          </section>
        )}

        <section className={styles.content}>
          <BlogPostContent />
        </section>
      </main>
    </Layout>
  );
}
