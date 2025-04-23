import React from 'react';
import clsx from 'clsx';
import LinkItem from '@theme/Footer/LinkItem';
function Separator() {
  return <span className="footer__link-separator">·</span>;
}
function SimpleLinkItem({ item }) {
  return item.html ? (
    <span
      className={clsx('footer__link-item', item.className)}
      dangerouslySetInnerHTML={{ __html: item.html }}
    />
  ) : (
    <LinkItem item={item} />
  );
}
export default function FooterLinksSimple({ links }) {
  return (
    <div className="footer__links text--center">
      <div className="footer__links">
        {links.map((item, i) => (
          <React.Fragment key={i}>
            <SimpleLinkItem item={item} />
            {links.length !== i + 1 && <Separator />}
          </React.Fragment>
        ))}
      </div>
    </div>
  );
}
