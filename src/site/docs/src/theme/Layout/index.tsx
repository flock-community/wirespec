import React, { useEffect } from "react";
import Layout from "@theme-original/Layout";

export default function LayoutWrapper(props) {
  useEffect(() => {
    const navbar = document.querySelector(".navbar");

    const onScroll = () => {
      if (window.scrollY > 50) {
        navbar?.classList.add("scrolled");
      } else {
        navbar?.classList.remove("scrolled");
      }
    };

    window.addEventListener("scroll", onScroll);
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return <Layout {...props} />;
}
