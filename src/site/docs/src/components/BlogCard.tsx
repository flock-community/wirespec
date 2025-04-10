import React from 'react';
import styles from './BlogCard.module.css';
import Link from '@docusaurus/Link';

interface BlogCardProps {
  image: string;
  title: string;
  subtitle: string;
  link: string;
}

const BlogCard: React.FC<BlogCardProps> = ({ image, title, subtitle, link }) => {
  return (
    <div className={styles.card}>
      <img src={image} alt={title} className={styles.cardImage} />
      <div className={styles.cardContent}>
        <h3>{title}</h3>
        <p>{subtitle}</p>
        <Link to={link} className={styles.readMore}>
          Read more
        </Link>
      </div>
    </div>
  );
};

export default BlogCard;
