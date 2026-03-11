import { G as Gl, b as b$1, a as zt, z as zr, O as O$1, T as Ts, x, y as yl, C as Cl, c as Tl, U as Ur, v as vs } from "./indexhtml-BZBCy2ek.js";
import { g as g$1 } from "./state-DOkKKpWv-DVftGsab.js";
import { o } from "./base-panel-BWfqXAMS-BrLfFEXa.js";
import { showNotification as N$1 } from "./copilot-notification-CUWNmpXC-kpIRXL4B.js";
import { r as r$1 } from "./icons-DpnnuXfg-E8O83Wvc.js";
const O = "copilot-info-panel{--dev-tools-red-color: red;--dev-tools-grey-color: gray;--dev-tools-green-color: green;position:relative}copilot-info-panel div.info-tray{display:flex;flex-direction:column;gap:10px}copilot-info-panel dl{display:grid;grid-template-columns:auto auto;gap:0;margin:var(--space-100) var(--space-50);font:var(--font-xsmall)}copilot-info-panel dl>dt,copilot-info-panel dl>dd{padding:3px 10px;margin:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}copilot-info-panel dd.live-reload-status>span{overflow:hidden;text-overflow:ellipsis;display:block;color:var(--status-color)}copilot-info-panel dd span.hidden{display:none}copilot-info-panel dd span.true{color:var(--dev-tools-green-color);font-size:large}copilot-info-panel dd span.false{color:var(--dev-tools-red-color);font-size:large}copilot-info-panel code{white-space:nowrap;-webkit-user-select:all;user-select:all}copilot-info-panel .checks{display:inline-grid;grid-template-columns:auto 1fr;gap:var(--space-50)}copilot-info-panel span.hint{font-size:var(--font-size-0);background:var(--gray-50);padding:var(--space-75);border-radius:var(--radius-2)}";
var j = function() {
  var e = document.getSelection();
  if (!e.rangeCount)
    return function() {
    };
  for (var t = document.activeElement, a = [], l = 0; l < e.rangeCount; l++)
    a.push(e.getRangeAt(l));
  switch (t.tagName.toUpperCase()) {
    case "INPUT":
    case "TEXTAREA":
      t.blur();
      break;
    default:
      t = null;
      break;
  }
  return e.removeAllRanges(), function() {
    e.type === "Caret" && e.removeAllRanges(), e.rangeCount || a.forEach(function(i) {
      e.addRange(i);
    }), t && t.focus();
  };
}, U = j, v = {
  "text/plain": "Text",
  "text/html": "Url",
  default: "Text"
}, T = "Copy to clipboard: #{key}, Enter";
function L(e) {
  var t = (/mac os x/i.test(navigator.userAgent) ? "⌘" : "Ctrl") + "+C";
  return e.replace(/#{\s*key\s*}/g, t);
}
function N(e, t) {
  var a, l, i, o2, r, n, h = false;
  t || (t = {}), a = t.debug || false;
  try {
    i = U(), o2 = document.createRange(), r = document.getSelection(), n = document.createElement("span"), n.textContent = e, n.ariaHidden = "true", n.style.all = "unset", n.style.position = "fixed", n.style.top = 0, n.style.clip = "rect(0, 0, 0, 0)", n.style.whiteSpace = "pre", n.style.webkitUserSelect = "text", n.style.MozUserSelect = "text", n.style.msUserSelect = "text", n.style.userSelect = "text", n.addEventListener("copy", function(c) {
      if (c.stopPropagation(), t.format)
        if (c.preventDefault(), typeof c.clipboardData > "u") {
          a && console.warn("unable to use e.clipboardData"), a && console.warn("trying IE specific stuff"), window.clipboardData.clearData();
          var m = v[t.format] || v.default;
          window.clipboardData.setData(m, e);
        } else
          c.clipboardData.clearData(), c.clipboardData.setData(t.format, e);
      t.onCopy && (c.preventDefault(), t.onCopy(c.clipboardData));
    }), document.body.appendChild(n), o2.selectNodeContents(n), r.addRange(o2);
    var x2 = document.execCommand("copy");
    if (!x2)
      throw new Error("copy command was unsuccessful");
    h = true;
  } catch (c) {
    a && console.error("unable to copy using execCommand: ", c), a && console.warn("trying IE specific stuff");
    try {
      window.clipboardData.setData(t.format || "text", e), t.onCopy && t.onCopy(window.clipboardData), h = true;
    } catch (m) {
      a && console.error("unable to copy using clipboardData: ", m), a && console.error("falling back to prompt"), l = L("message" in t ? t.message : T), window.prompt(l, e);
    }
  } finally {
    r && (typeof r.removeRange == "function" ? r.removeRange(o2) : r.removeAllRanges()), n && document.body.removeChild(n), i();
  }
  return h;
}
var B = N;
const M = /* @__PURE__ */ yl(B);
var F = Object.defineProperty, W = Object.getOwnPropertyDescriptor, g = (e, t, a, l) => {
  for (var i = l > 1 ? void 0 : l ? W(t, a) : t, o2 = e.length - 1, r; o2 >= 0; o2--)
    (r = e[o2]) && (i = (l ? r(t, a, i) : r(i)) || i);
  return l && i && F(t, a, i), i;
};
const w = zt`<a
  href="${Cl}"
  target="_blank"
  @click="${() => k("idea")}"
  title="Get IntelliJ plugin"
  >Get IntelliJ plugin</a
>`, b = zt`<a
  href="${Tl}"
  target="_blank"
  @click="${() => k("vscode")}"
  title="Get VS Code plugin"
  >Get VS Code plugin</a
>`;
function k(e) {
  return Ur("get-plugin", e), false;
}
let f = class extends o {
  constructor() {
    super(...arguments), this.serverInfo = [], this.clientInfo = [{ name: "Browser", version: navigator.userAgent }], this.handleServerInfoEvent = (e) => {
      const t = JSON.parse(e.data.info);
      this.serverInfo = t.versions, this.updateJdkInfo(t.jdkInfo), this.updateIdePluginInfo(), Gl().then((a) => {
        a && (this.clientInfo.unshift({ name: "Vaadin Employee", version: "true", more: void 0 }), this.requestUpdate("clientInfo"));
      });
    };
  }
  connectedCallback() {
    super.connectedCallback(), this.onCommand("copilot-info", this.handleServerInfoEvent), this.onEventBus("system-info-with-callback", (e) => {
      e.detail.callback(this.getInfoForClipboard(e.detail.notify));
    }), this.reaction(
      () => b$1.idePluginState,
      () => {
        this.updateIdePluginInfo(), this.requestUpdate("serverInfo");
      }
    );
  }
  updateJdkInfo(e) {
    const t = e.extendedClassDefCapable && e.runningWithExtendClassDef && e.hotswapAgentFound && e.runningWitHotswap && e.hotswapVersionOk, a = e.jrebel;
    b$1.jdkInfo = {
      ...e,
      activeHotswap: a ? "jrebel" : t ? "hotswapagent" : void 0
    };
  }
  updateIdePluginInfo() {
    const e = this.getIndex("Copilot IDE Plugin");
    let t = "false", a;
    b$1.idePluginState?.active ? t = `${b$1.idePluginState.version}-${b$1.idePluginState.ide}` : b$1.idePluginState?.ide === "vscode" ? a = b : b$1.idePluginState?.ide === "idea" ? a = w : a = zt`${w} or ${b}`, this.serverInfo[e].version = t, this.serverInfo[e].more = a;
  }
  getIndex(e) {
    return this.serverInfo.findIndex((t) => t.name === e);
  }
  render() {
    return zt`<style>
        ${O}
      </style>
      <div class="info-tray">
        <dl>
          ${[...this.serverInfo, ...this.clientInfo].map(
      (e) => zt`
              <dt>${e.name}</dt>
              <dd title="${e.version}" style="${e.name === "Java Hotswap" ? "white-space: normal" : ""}">
                ${this.renderVersion(e)} ${e.more}
              </dd>
            `
    )}
        </dl>
      </div>`;
  }
  renderVersion(e) {
    return e.name === "Java Hotswap" ? this.renderJavaHotswap() : this.renderValue(e.version);
  }
  renderValue(e) {
    return e === "false" ? p(false) : e === "true" ? p(true) : e;
  }
  getInfoForClipboard(e) {
    const t = this.renderRoot.querySelectorAll(".info-tray dt"), i = Array.from(t).map((o2) => ({
      key: o2.textContent.trim(),
      value: o2.nextElementSibling.textContent.trim()
    })).filter((o2) => o2.key !== "Live reload").filter((o2) => !o2.key.startsWith("Vaadin Emplo")).map((o2) => {
      const { key: r } = o2;
      let { value: n } = o2;
      return r === "Copilot IDE Plugin" && !b$1.idePluginState?.active ? n = "false" : r === "Java Hotswap" && (n = String(n.includes("JRebel is in use") || n.includes("HotswapAgent is in use"))), `${r}: ${n}`;
    }).join(`
`);
    return e && N$1({
      type: zr.INFORMATION,
      message: "Environment information copied to clipboard",
      dismissId: "versionInfoCopied"
    }), i.trim();
  }
  renderJavaHotswap() {
    const e = b$1.jdkInfo;
    if (!e)
      return O$1;
    const t = e.activeHotswap === "jrebel";
    return !e.extendedClassDefCapable && !t ? zt`<details>
        <summary>${p(false)} No Hotswap solution in use</summary>
        <p>To enable hotswap for Java, you can either use HotswapAgent or JRebel.</p>
        <p>HotswapAgent is an open source project that utilizes the JetBrains Runtime (JDK).</p>
        <div class="checks">
          <span class="hint"
            >If you are running IntelliJ, edit the launch configuration to use the bundled JDK.<br />
            Otherwise, download it from
            <a target="_blank" href="https://github.com/JetBrains/JetBrainsRuntime/releases"
              >the JetBrains release page</a
            >
            to get started.
          </span>
        </div>
        <p>
          JRebel is a commercial solution available from
          <a target="_blank" href="https://www.jrebel.com/">jrebel.com</a>
        </p>
      </details>` : t ? zt`<div class="checks">
        ${p(true)}
        <span>JRebel is in use</span>
      </div>` : e.activeHotswap === "hotswapagent" ? zt`<div class="checks">${p(true)}<span>HotswapAgent is in use</span></div>` : zt`<details>
      <summary><div class="checks">${p(false)} HotswapAgent is partially enabled</div></summary>
      <div class="checks">
        ${p(e.extendedClassDefCapable)}
        <span>JDK supports hotswapping</span>
        ${p(e.runningWithExtendClassDef)}
        <span>JDK hotswapping enabled</span>
          ${e.runningWithExtendClassDef ? O$1 : zt`<span></span
                  ><span class="hint"
                    >Add the <code>-XX:+AllowEnhancedClassRedefinition</code> JVM argument when launching the
                    application</span
                  >`}
          ${p(e.hotswapAgentFound)}
          <span>HotswapAgent installed</span>
          ${e.hotswapAgentFound ? O$1 : zt`<span></span
                  ><span class="hint"
                    ><a target="_blank" href="https://github.com/HotswapProjects/HotswapAgent/releases"
                      >Download the latest HotswapAgent</a
                    >
                    and place it in <code>${e.hotswapAgentLocation}</code></span
                  >`}
          ${p(e.hotswapVersionOk)}
          <span>HotswapAgent is version 1.4.2 or newer</span>
          ${e.hotswapVersionOk ? O$1 : zt`<span></span
                  ><span class="hint"
                    >HotswapAgent version ${e.hotswapVersion} is in use<br />
                    <a target="_blank" href="https://github.com/HotswapProjects/HotswapAgent/releases"
                      >Download the latest HotswapAgent</a
                    >
                    and place it in <code>${e.hotswapAgentLocation}</code></span
                  >`}
          ${p(e.runningWitHotswap)}
          <span>HotswapAgent configured</span>
          ${e.runningWitHotswap ? O$1 : zt`<span></span
                  ><span class="hint"
                    >Add the <code>-XX:HotswapAgent=fatjar</code> JVM argument when launching the application</span
                  >`}
          ${p(e.runningInJavaDebugMode)}
        <span>Application running in Java debug mode</span>
          ${e.runningInJavaDebugMode ? O$1 : zt`<span></span><span class="hint">Start the application in debug mode in the IDE</span>`}
        <a href="https://vaadin.com/docs/latest/flow/configuration/live-reload/hotswap-agent" target="_blank" style="grid-column: 1 / -1">Read more about Hot Deploy & Live Reload</a>
    </details> `;
  }
};
g([
  g$1()
], f.prototype, "serverInfo", 2);
g([
  g$1()
], f.prototype, "clientInfo", 2);
f = g([
  vs("copilot-info-panel")
], f);
let y = class extends Ts {
  createRenderRoot() {
    return this;
  }
  connectedCallback() {
    super.connectedCallback(), this.style.display = "flex";
  }
  render() {
    return zt`<button title="Copy to clipboard" aria-label="Copy to clipboard" theme="icon tertiary">
      <span
        @click=${() => {
      x.emit("system-info-with-callback", {
        callback: M,
        notify: true
      });
    }}
        >${r$1.copy}</span
      >
    </button>`;
  }
};
y = g([
  vs("copilot-info-actions")
], y);
const z = {
  header: "Info",
  expanded: true,
  panelOrder: 15,
  panel: "right",
  floating: false,
  tag: "copilot-info-panel",
  actionsTag: "copilot-info-actions"
}, G = {
  init(e) {
    e.addPanel(z);
  }
};
window.Vaadin.copilot.plugins.push(G);
function p(e) {
  return e ? zt`<span class="true">☑</span>` : zt`<span class="false">☒</span>`;
}
export {
  y as Actions,
  f as CopilotInfoPanel
};
