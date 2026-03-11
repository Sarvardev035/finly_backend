import { v as vs, a as zt, d as Ls, X as Xl } from "./indexhtml-BZBCy2ek.js";
import { o } from "./base-panel-BWfqXAMS-BrLfFEXa.js";
import { r as r$1 } from "./icons-DpnnuXfg-E8O83Wvc.js";
const f = "copilot-shortcuts-panel{font:var(--font-xsmall);padding:var(--space-200);display:flex;flex-direction:column;gap:var(--space-50)}copilot-shortcuts-panel h3{font:var(--font-xsmall-strong);margin:0;padding:0}copilot-shortcuts-panel h3:not(:first-of-type){margin-top:var(--space-200)}copilot-shortcuts-panel ul{list-style:none;margin:0;padding:0 var(--space-50);display:flex;flex-direction:column}copilot-shortcuts-panel ul li{display:flex;align-items:center;gap:var(--space-150);padding:var(--space-75) 0}copilot-shortcuts-panel ul li:not(:last-of-type){border-bottom:1px dashed var(--border-color)}copilot-shortcuts-panel ul li svg{height:16px;width:16px}copilot-shortcuts-panel ul li .kbds{flex:1;text-align:right}copilot-shortcuts-panel kbd{display:inline-block;border-radius:var(--radius-1);border:1px solid var(--border-color);min-width:1em;min-height:1em;text-align:center;margin:0 .1em;padding:.25em;box-sizing:border-box;font-size:var(--font-size-1);font-family:var(--font-family);line-height:1}";
var b = (i, a, n, s) => {
  for (var o2 = a, r = i.length - 1, p; r >= 0; r--)
    (p = i[r]) && (o2 = p(o2) || o2);
  return o2;
};
let c = class extends o {
  render() {
    return zt`<style>
        ${f}
      </style>
      <h3>Global</h3>
      <ul>
        <li>${r$1.vaadinLogo} Copilot ${t(Xl.toggleCopilot)}</li>
        <li>${r$1.terminal} Command window ${t(Xl.toggleCommandWindow)}</li>
        <li>${r$1.undo} Undo ${t(Xl.undo)}</li>
        <li>${r$1.redo} Redo ${t(Xl.redo)}</li>
      </ul>
      <h3>Selected component</h3>
      <ul>
        <li>${r$1.code} Go to source ${t(Xl.goToSource)}</li>
        <li>${r$1.copy} Copy ${t(Xl.copy)}</li>
        <li>${r$1.paste} Paste ${t(Xl.paste)}</li>
        <li>${r$1.duplicate} Duplicate ${t(Xl.duplicate)}</li>
        <li>${r$1.userUp} Select parent ${t(Xl.selectParent)}</li>
        <li>${r$1.userLeft} Select previous sibling ${t(Xl.selectPreviousSibling)}</li>
        <li>${r$1.userRight} Select first child / next sibling ${t(Xl.selectNextSibling)}</li>
        <li>${r$1.trash} Delete ${t(Xl.delete)}</li>
      </ul>`;
  }
};
c = b([
  vs("copilot-shortcuts-panel")
], c);
function t(i) {
  return zt`<span class="kbds">${Ls(i)}</span>`;
}
const v = {
  header: "Keyboard Shortcuts",
  expanded: true,
  expandable: false,
  panelOrder: 0,
  floating: false,
  tag: "copilot-shortcuts-panel",
  width: 400,
  height: 475,
  floatingPosition: {
    top: 50,
    left: 50
  }
}, x = {
  init(i) {
    i.addPanel(v);
  }
};
window.Vaadin.copilot.plugins.push(x);
