import React from "react";
import Layout from "@theme/Layout";
import BlogCard from "@site/src/components/BlogCard";
import styles from "./blogs.module.css";
import Seo from "@site/src/components/Seo";

export default function BlogListPage({ metadata, items }) {
  return (
    <Layout
      title="Wirespec Blogs"
      description="Simplify your API development workflows, accelerate implementation, and guarantee strict adherence to defined contract specifications"
      image="/img/code-snippet.jpg"
    >
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
