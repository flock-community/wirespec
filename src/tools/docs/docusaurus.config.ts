import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const config: Config = {
    title: 'Wirespec',
    tagline: 'Dinosaurs are cool',
    favicon: 'img/favicon.png',

    // Set the production url of your site here
    url: 'https://docs.wirespec.io',
    // Set the /<baseUrl>/ pathname under which your site is served
    // For GitHub pages deployment, it is often '/<projectName>/'
    baseUrl: '/',

    // GitHub pages deployment config.
    // If you aren't using GitHub pages, you don't need these.
    organizationName: 'flock-community/wirespec', // Usually your GitHub org/user name.
    projectName: 'wirespec', // Usually your repo name.

    onBrokenLinks: 'throw',
    onBrokenMarkdownLinks: 'warn',

    // Even if you don't use internationalization, you can use this field to set
    // useful metadata like html lang. For example, if your site is Chinese, you
    // may want to replace "en" with "zh-Hans".
    i18n: {
        defaultLocale: 'en',
        locales: ['en'],
    },

    customFields: {
        version: '0.0.0-SNAPSHOT'
    },
    plugins: [
        ['drawio', {}],
    ],
    presets: [
        [
            'classic',
            {
                docs: {
                    sidebarPath: './sidebars.ts',
                    // Please change this to your repo.
                    // Remove this to remove the "edit this page" links.
                    editUrl:
                        'https://github.com/flock-community/wirespec/tree/master/src/tools/docs/',
                },
                theme: {
                    customCss: [
                        './node_modules/@flock/black-sun/build/infima/custom.css',
                        './custom.css'
                    ],
                },
            } satisfies Preset.Options,
        ],
    ],

    themeConfig: {
        colorMode: {
            defaultMode: 'dark',
        },
        // Replace with your project's social card
        image: 'img/docusaurus-social-card.jpg',
        navbar: {
            title: 'Wirespec',
            logo: {
                alt: 'Wirespec Logo',
                src: 'img/wirespec.svg',
            },
            items: [
                {
                    type: 'docSidebar',
                    sidebarId: 'docsSidebar',
                    position: 'left',
                    label: 'Docs',
                },
                {to: '/blog', label: 'Blog', position: 'left'},
                {to: 'https://playground.wirespec.io', label: 'Playground', position: 'left', },
                {
                    href: 'https://github.com/flock-community/wirespec',
                    label: 'GitHub',
                    position: 'right',
                },
            ],
        },
        footer: {
            style: 'dark',
            links: [
                {
                    title: 'Docs',
                    items: [
                        {
                            label: 'About',
                            to: '/docs/intro',
                        },
                        {
                            label: 'Plugins',
                            to: '/docs/plugins',
                        },
                    ],
                },
                {
                    title: 'Community',
                    items: [
                        {
                            label: 'Stack Overflow',
                            href: 'https://stackoverflow.com/questions/tagged/wirespec',
                        },
                        {
                            label: 'Signal',
                            href: 'https://signal.group/#CjQKIJonLCeiw-BY_wjD58JegEWXAXI79Ig24tjYTYwtyi9wEhDL67jUl5GFj140yszVCib3',
                        }
                    ],
                },
                {
                    title: 'More',
                    items: [
                        {
                            label: 'Blog',
                            to: '/blog',
                        },
                        {
                            label: 'GitHub',
                            href: 'https://github.com/flock-community/wirespec',
                        },
                    ],
                },
            ],
            copyright: `Copyright © ${new Date().getFullYear()} Wirespec. Built with Docusaurus.`,
        },
        prism: {
            theme: prismThemes.github,
            darkTheme: prismThemes.dracula,
        },

    } satisfies Preset.ThemeConfig,

    markdown: {
        format: "mdx",
        preprocessor: ({ fileContent }) => {
            const variables = {
                "WIRESPEC_VERSION": "0.0.0-SNAPSHOT"
            }
            return fileContent.replaceAll("{{WIRESPEC_VERSION}}", variables.WIRESPEC_VERSION)
        },
    },
};

export default config;
