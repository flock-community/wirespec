import React from "react";
export default function FooterCopyright({ copyright }) {
  return (
    <div
      className="footer__copyright"
      dangerouslySetInnerHTML={{ __html: copyright }}
    />
  );
}
