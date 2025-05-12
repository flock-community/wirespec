import React from "react";
import DocPage from "@theme-original/DocPage";
import type { WrapperProps } from "@docusaurus/types";
import SEO from "@site/src/components/SEO";

type Props = WrapperProps<typeof DocPage>;

export default function DocPageWrapper(props: Props): React.ReactElement {
  const { metadata } = props;
  const { title, description } = metadata;

  return (
    <>
      <SEO
        title={`${title} | Wirespec Docs`}
        description={
          description ||
          "Find everything you need to get started with Wirespec: intro to the language, plugins, integrations, IDE setup, emitters, and converters—all in one place."
        }
        type="website"
      />
      <DocPage {...props} />
    </>
  );
}
