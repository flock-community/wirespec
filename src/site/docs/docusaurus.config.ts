import { themes as prismThemes } from "prism-react-renderer";
import type { Config } from "@docusaurus/types";
import type * as Preset from "@docusaurus/preset-classic";

const config: Config = {
  title: "Wirespec",
  tagline: "Dinosaurs are cool",
  favicon: "img/wirespec-favicon.png",
  // Set the production url of your site here
  url: "https://docs.wirespec.io",
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: "/",
  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: "flock-community/wirespec", // Usually your GitHub org/user name.
  projectName: "wirespec", // Usually your repo name.
  onBrokenLinks: "throw",
  onBrokenMarkdownLinks: "warn",
  // Even if you don't use internationalization, you can use this field to set
  // useful metadata like html lang. For example, if your site is Chinese, you
  // may want to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: "en",
    locales: ["en"],
  },
  customFields: {
    version: "0.0.0-SNAPSHOT",
  },
  plugins: [["drawio", {}]],
  presets: [
    [
      "classic",
      {
        docs: {
          sidebarPath: "./sidebars.ts",
          editUrl:
            "https://github.com/flock-community/wirespec/tree/master/src/site/docs/",
        },
        blog: {
          path: "./blog",
          routeBasePath: "/blog",
          showReadingTime: true,
        },
        theme: {
          customCss: [
            "./node_modules/@flock/black-sun/build/infima/custom.css",
            "./custom.css",
          ],
        },
      } satisfies Preset.Options,
    ],
  ],
  themeConfig: {
    colorMode: {
      defaultMode: "dark",
    },
    stylesheets: [
      {
        href: "https://fonts.googleapis.com/css2?family=Funnel+Display:wght@300..800&display=swap",
        type: "text/css",
      },
    ],
    // Replace with your project's social card
    image: "img/docusaurus-social-card.jpg",
    navbar: {
      title: "",
      logo: {
        alt: "Wirespec Logo",
        src: "img/wirespec-logo.svg",
      },
      items: [
        {
          to: "/vision",
          label: "Vision",
          position: "left",
        },
        {
          label: "How",
          position: "left",
          items: [
            {
              label: "Contract",
              //   to: '/docs/how-design',
              to: "/how#contract",
            },
            {
              label: "Generate",
              to: "/how#generate",
            },
            {
              label: "Validate",
              //   href: '/docs/how-validate',
              to: "/how#validate",
            },
          ],
        },
        {
          to: "https://playground.wirespec.io",
          label: "Playground",
          position: "left",
        },
        {
          type: "docSidebar",
          sidebarId: "docsSidebar",
          position: "left",
          label: "Docs",
        },
        { to: "/blog", label: "Blog", position: "left" },
        {
          href: "https://github.com/flock-community/wirespec",
          position: "right",
          className: "header-github-link",
          html: '<svg width="22" height="22" viewBox="0 0 22 22" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><g clip-path="url(#clip0_1_50)"><path fill-rule="evenodd" clip-rule="evenodd" d="M10.9672 0C4.90263 0 0 5.04167 0 11.2789C0 16.2646 3.14129 20.485 7.49908 21.9787C8.04392 22.091 8.24349 21.736 8.24349 21.4374C8.24349 21.1759 8.22553 20.2796 8.22553 19.3458C5.17471 20.0182 4.53941 18.0013 4.53941 18.0013C4.04912 16.6941 3.32267 16.3581 3.32267 16.3581C2.32414 15.6672 3.39541 15.6672 3.39541 15.6672C4.50304 15.7419 5.08424 16.825 5.08424 16.825C6.06459 18.5428 7.64433 18.0574 8.27986 17.7586C8.37055 17.0303 8.66127 16.5261 8.96994 16.2461C6.53669 15.9846 3.97661 15.0136 3.97661 10.6812C3.97661 9.44877 4.41212 8.44044 5.1022 7.65623C4.99333 7.37619 4.61192 6.21821 5.21131 4.66835C5.21131 4.66835 6.13733 4.36952 8.22531 5.8261C9.11924 5.57921 10.0411 5.45362 10.9672 5.45256C11.8932 5.45256 12.8372 5.58342 13.7089 5.8261C15.7971 4.36952 16.7231 4.66835 16.7231 4.66835C17.3225 6.21821 16.9409 7.37619 16.832 7.65623C17.5403 8.44044 17.9578 9.44877 17.9578 10.6812C17.9578 15.0136 15.3978 15.9658 12.9463 16.2461C13.3459 16.6008 13.6907 17.273 13.6907 18.3375C13.6907 19.85 13.6728 21.0639 13.6728 21.4372C13.6728 21.736 13.8726 22.091 14.4172 21.9789C18.775 20.4847 21.9163 16.2646 21.9163 11.2789C21.9342 5.04167 17.0136 0 10.9672 0Z"/></g></svg>',
        },
      ],
    },
    footer: {
      style: "dark",
      logo: {
        href: "/docs/intro",
        alt: "Wirespec Logo",
        src: "img/wirespec-logo.svg",
      },
      links: [
        {
          items: [
            {
              label: "Vision",
              to: "/docs/intro",
            },
            {
              label: "Use cases",
              to: "/docs/plugins",
            },
            {
              label: "Playground",
              to: "/docs/plugins",
            },
          ],
        },
        {
          items: [
            {
              label: "Docs",
              to: "/docs/intro",
            },
            {
              label: "Blog",
              to: "/blog",
            },
          ],
        },
      ],
      copyright: `© ${new Date().getFullYear()} Wirespec. All rights reserved.`,
    },
    prism: {
      theme: prismThemes.vsLight,
      darkTheme: prismThemes.vsDark,
    },
    // Algolia configuration inside themeConfig
    algolia: {
      // The application ID provided by Algolia
      appId: "9QKUCBFICR",
      // Public API key: it is safe to commit it
      apiKey: "b4a1301e4e3b1f1a240e3eee170e216e",
      indexName: "wirespec-docs",
      // Optional: see doc section below
      contextualSearch: true,
      // Optional: Specify domains where the navigation should occur through window.location instead on history.push. Useful when our Algolia config crawls multiple documentation sites and we want to navigate with window.location.href to them.
      externalUrlRegex: "external\\.com|domain\\.com",
      // Optional: Replace parts of the item URLs from Algolia. Useful when using the same search index for multiple deployments using a different baseUrl. You can use regexp or string in the `from` param. For example: localhost:3000 vs myCompany.com/docs
      replaceSearchResultPathname: {
        from: "/docs/", // or as RegExp: /\/docs\//
        to: "/",
      },
      // Optional: Algolia search parameters
      searchParameters: {},
      // Optional: path for search page that enabled by default (`false` to disable it)
      searchPagePath: "search",
      // Optional: whether the insights feature is enabled or not on Docsearch (`false` by default)
      insights: false,
    },
  } satisfies Preset.ThemeConfig,
  markdown: {
    format: "mdx",
    preprocessor: ({ fileContent }) => {
      const variables = {
        WIRESPEC_VERSION: "0.0.0-SNAPSHOT",
      };
      return fileContent.replaceAll(
        "{{WIRESPEC_VERSION}}",
        variables.WIRESPEC_VERSION
      );
    },
  },
};

export default config;
