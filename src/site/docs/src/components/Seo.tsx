import React from 'react';
import Head from '@docusaurus/Head';

type Props = {
  title: string;
  description: string;
  image?: string;
};

const Seo = ({ title, description, image = '/img/default.png' }: Props) => (
  <Head>
    <title>{title}</title>
    <meta name="description" content={description} />
    <meta property="og:title" content={title} />
    <meta property="og:description" content={description} />
    <meta property="og:image" content={image} />
    <meta name="twitter:card" content="summary_large_image" />
    <meta name="twitter:title" content={title} />
    <meta name="twitter:description" content={description} />
    <meta name="twitter:image" content={image} />
  </Head>
);

export default Seo;
