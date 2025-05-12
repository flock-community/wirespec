import React from "react";
import Layout from "@theme/Layout";
import BlogCard from "@site/src/components/BlogCard";
import styles from "./blogs.module.css";
import SEO from "@site/src/components/SEO";

export default function BlogListPage({ metadata, items }) {
  return (
    <Layout>
      <SEO
        title="Blog | Wirespec"
        description="Explore in-depth articles on API design, contract-first development, code generation, and interface testing—insights and best practices from the team behind Wirespec."
        type="website"
      />
      <main className={styles.blogMain}>
        <h1 className={styles.heading}>Blog</h1>
        <div className={styles.grid}>
          {items.map(({ content: BlogPostContent }, index) => {
            const { frontMatter, metadata } = BlogPostContent;
            return (
              <BlogCard
                key={index}
                image={frontMatter.image || "/img/code-snippet.png"}
                title={metadata.title}
                subtitle={metadata.description || metadata.excerpt}
                link={metadata.permalink}
              />
            );
          })}
        </div>
      </main>
    </Layout>
  );
}
