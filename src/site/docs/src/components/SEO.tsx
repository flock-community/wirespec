import React from "react";
import Head from "@docusaurus/Head";
import { useLocation } from "@docusaurus/router";

interface SEOProps {
  title: string;
  description: string;
  image?: string;
  type?: "website" | "article";
  url?: string;
  keywords?: string[];
}

export default function SEO({
  title,
  description,
  image = "/img/code-snippet.jpg", // Default image
  type = "website",
  url = "https://wirespec.io",
  keywords = [
    "API design",
    "contract-first",
    "code generation",
    "testing",
    "Wirespec",
  ],
}: SEOProps) {
  const location = useLocation();
  const siteUrl = "https://wirespec.io";
  const fullUrl = url.startsWith("http") ? url : `${siteUrl}${url}`;
  const fullImageUrl = image.startsWith("http") ? image : `${siteUrl}${image}`;

  // Determine the full title based on the current path
  let fullTitle = title;
  if (location.pathname === "/") {
    fullTitle = "Wirespec your APIs";
  } else if (location.pathname.startsWith("/docs/")) {
    fullTitle = `${title} | Docs | Wirespec`;
  } else if (location.pathname === "/blog") {
    fullTitle = "Blog | Wirespec";
  } else if (title !== "Wirespec your APIs") {
    fullTitle = `${title} | Wirespec`;
  }

  return (
    <Head>
      {/* Basic meta tags */}
      <title>{fullTitle}</title>
      <meta name="description" content={description} />
      <meta name="keywords" content={keywords.join(", ")} />
      <meta name="author" content="Wirespec Team" />

      {/* OpenGraph tags */}
      <meta property="og:type" content={type} />
      <meta property="og:title" content={fullTitle} />
      <meta property="og:description" content={description} />
      <meta property="og:image" content={fullImageUrl} />
      <meta property="og:url" content={fullUrl} />
      <meta property="og:site_name" content="Wirespec" />
      <meta property="og:locale" content="en_US" />

      {/* Twitter Card tags */}
      <meta name="twitter:card" content="summary_large_image" />
      <meta name="twitter:site" content="@wirespec" />
      <meta name="twitter:creator" content="@wirespec" />
      <meta name="twitter:title" content={fullTitle} />
      <meta name="twitter:description" content={description} />
      <meta name="twitter:image" content={fullImageUrl} />
    </Head>
  );
}
