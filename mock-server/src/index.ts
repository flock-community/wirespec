import * as fs from "fs";
import { WsToTypeScript } from "wire-spec-lib";

const RandExp = require("randexp");
RandExp.prototype.randInt = (from, to) => {
  console.log(from, to);
  return from;
};

const express = require("express");

const app = express();

const wsToTs = new WsToTypeScript();
const file = fs.readFileSync("./wirespec/todo.ws");

// console.log(file.toString());
const ast = wsToTs.parse(file.toString());

const refined = ast.i_1
  .filter((it) => it.bd_1)
  .map((it) => ({
    name: it.bd_1.zc_1,
    regex: it.cd_1.ad_1,
  }))
  .reduce((acc, cur) => ({ ...acc, [cur.name]: cur.regex.slice(1, -1) }), {});

// console.log(JSON.stringify(refined, null, 4));

const types = ast.i_1
  .filter((it) => it.bb_1)
  .map((it) => {
    const name = it.bb_1.gb_1;
    const fields = it.cb_1.ib_1.i_1.map((it) => {
      const name = it.jb_1.ob_1;
      const type = it?.kb_1?.qb_1?.n9_1 || it?.kb_1?.sb_1;
      return { name, type };
    });
    return { name, fields };
  })
  .reduce((acc, cur) => ({ ...acc, [cur.name]: cur.fields }), {});

// console.log(JSON.stringify(types, null, 4));

ast.i_1.forEach((it) => {
  if (it.uc_1) {
    const type = it.yc_1.sc_1.mc_1;
    switch (it.vc_1.qc_1) {
      case "GET":
        app.get(path(it), (req, res) => {
          res.send(transform(type));
        });
    }
  }
});

function transform(it: string) {
  if (it === "String") {
    return new RandExp("^.{1,50}$").gen();
  }
  if (it === "Integer") {
    return parseInt(new RandExp("^[1-9][0-9]?$|^1000$").gen());
  }
  if (it === "Boolean") {
    return new RandExp("true|false").gen() === "true";
  }
  const type = types[it] || refined[it];
  if (typeof type === "string") {
    return new RandExp(type).gen();
  }
  return type?.reduce((acc, cur) => ({ ...acc, [cur.name]: transform(cur.type) }), {});
}

function path(it: any) {
  const path = it.wc_1.i_1
    .map((it) => {
      if (it.rc_1) {
        return it.rc_1;
      }
      if (it.ib_1) {
        return `:${it.ib_1.i_1[0].jb_1.ob_1}`;
      }
    })
    .join("/");
  return "/" + path;
}

function handler(it: any) {
  const path = it.wc_1.i_1
    .map((it) => {
      if (it.rc_1) {
        return it.rc_1;
      }
      if (it.ib_1) {
        return `:${it.ib_1.i_1[0].jb_1.ob_1}`;
      }
    })
    .join("/");
  return "/" + path;
}

const port = 3000;

app.listen(port, () => {
  console.log(`Timezones by location application is running on port ${port}.`);
});
