import { R as Re } from "./indexhtml-BZBCy2ek.js";
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
const i = (t, e, a) => (a.configurable = true, a.enumerable = true, Reflect.decorate && typeof e != "object" && Object.defineProperty(t, e, a), a);
/**
 * @license
 * Copyright 2017 Google LLC
 * SPDX-License-Identifier: BSD-3-Clause
 */
function p(t, e) {
  return (a, r, d) => {
    const l = (o) => o.renderRoot?.querySelector(t) ?? null;
    return i(a, r, { get() {
      return l(this);
    } });
  };
}
function b(t) {
  t.querySelectorAll(
    "vaadin-context-menu, vaadin-menu-bar, vaadin-menu-bar-submenu, vaadin-select, vaadin-combo-box, vaadin-tooltip, vaadin-dialog, vaadin-multi-select-combo-box"
  ).forEach((e) => {
    e?.$?.comboBox && (e = e.$.comboBox);
    let a = e.shadowRoot?.querySelector(
      `${e.localName}-overlay, ${e.localName}-submenu, vaadin-menu-bar-overlay`
    );
    a?.localName === "vaadin-menu-bar-submenu" && (a = a.shadowRoot.querySelector("vaadin-menu-bar-overlay")), a ? a._attachOverlay = c.bind(a) : e.$?.overlay && (e.$.overlay._attachOverlay = c.bind(e.$.overlay));
  });
}
function u() {
  return document.querySelector(`${Re}main`).shadowRoot;
}
const v = () => Array.from(u().children).filter((e) => e._hasOverlayStackMixin && !e.hasAttribute("closing")).sort((e, a) => e.__zIndex - a.__zIndex || 0), y = (t) => t === v().pop();
function c() {
  const t = this;
  t._placeholder = document.createComment("vaadin-overlay-placeholder"), t.parentNode.insertBefore(t._placeholder, t), u().appendChild(t), t.hasOwnProperty("_last") || Object.defineProperty(t, "_last", {
    // Only returns odd die sides
    get() {
      return y(this);
    }
  }), t.bringToFront(), requestAnimationFrame(() => b(t));
}
export {
  b,
  p
};
