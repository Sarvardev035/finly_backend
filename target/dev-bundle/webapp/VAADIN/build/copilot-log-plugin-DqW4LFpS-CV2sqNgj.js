import { L as Ll, g as gt, z as zr, a as zt, M as Ml, s as ss, T as Ts, x, e as er, v as vs } from "./indexhtml-BZBCy2ek.js";
import { g as g$1 } from "./state-DOkKKpWv-DVftGsab.js";
import { o } from "./base-panel-BWfqXAMS-BrLfFEXa.js";
import { r as r$1 } from "./icons-DpnnuXfg-E8O83Wvc.js";
const S = "copilot-log-panel{padding:var(--space-100);font:var(--font-xsmall);display:flex;flex-direction:column;gap:var(--space-50);overflow-y:auto}copilot-log-panel .row{display:flex;align-items:flex-start;padding:var(--space-50) var(--space-100);border-radius:var(--radius-2);gap:var(--space-100)}copilot-log-panel .row.information{background-color:var(--blue-50)}copilot-log-panel .row.warning{background-color:var(--yellow-50)}copilot-log-panel .row.error{background-color:var(--red-50)}copilot-log-panel .type{margin-top:var(--space-25)}copilot-log-panel .type.error{color:var(--red)}copilot-log-panel .type.warning{color:var(--yellow)}copilot-log-panel .type.info{color:var(--color)}copilot-log-panel .message{display:flex;flex-direction:column;flex-grow:1;gap:var(--space-25);overflow:hidden}copilot-log-panel .message>*{white-space:nowrap}copilot-log-panel .firstrow{display:flex;align-items:baseline;gap:.5em;flex-direction:column}copilot-log-panel .firstrowmessage{width:100%}copilot-log-panel button{padding:0;border:0;background:transparent}copilot-log-panel svg{height:12px;width:12px}copilot-log-panel .secondrow,copilot-log-panel .timestamp{font-size:var(--font-size-0);line-height:var(--line-height-1)}copilot-log-panel .expand span{height:12px;width:12px}";
var I = Object.defineProperty, _ = Object.getOwnPropertyDescriptor, h = (e, t, a, o2) => {
  for (var s = o2 > 1 ? void 0 : o2 ? _(t, a) : t, p = e.length - 1, i; p >= 0; p--)
    (i = e[p]) && (s = (o2 ? i(t, a, s) : i(s)) || s);
  return o2 && s && I(t, a, s), s;
};
class b {
  constructor() {
    this.showTimestamps = false, er(this);
  }
  toggleShowTimestamps() {
    this.showTimestamps = !this.showTimestamps;
  }
}
const g = new b();
let r = class extends o {
  constructor() {
    super(), this.unreadErrors = false, this.messages = [], this.nextMessageId = 1, this.transitionDuration = 0, this.catchErrors();
  }
  connectedCallback() {
    super.connectedCallback(), this.onCommand("log", (e) => {
      this.handleLogEventData({ type: e.data.type, message: e.data.message });
    }), this.onEventBus("log", (e) => this.handleLogEvent(e)), this.onEventBus("update-log", (e) => this.updateLog(e.detail)), this.onEventBus("notification-shown", (e) => this.handleNotification(e)), this.onEventBus("clear-log", () => this.clear()), this.transitionDuration = parseInt(
      window.getComputedStyle(this).getPropertyValue("--dev-tools-transition-duration"),
      10
    );
  }
  clear() {
    this.messages = [];
  }
  handleNotification(e) {
    this.log(e.detail.type, e.detail.message, true, e.detail.details, e.detail.link, void 0);
  }
  handleLogEvent(e) {
    this.handleLogEventData(e.detail);
  }
  handleLogEventData(e) {
    this.log(
      e.type,
      e.message,
      !!e.internal,
      e.details,
      e.link,
      Ll(e.expandedMessage),
      Ll(e.expandedDetails),
      e.id
    );
  }
  activate() {
    this.unreadErrors = false, this.updateComplete.then(() => {
      const e = this.renderRoot.querySelector(".message:last-child");
      e && e.scrollIntoView();
    });
  }
  format(e) {
    return e.message ? e.message.toString() : e.toString();
  }
  catchErrors() {
    const e = window.Vaadin.ConsoleErrors;
    window.Vaadin.ConsoleErrors = {
      push: (t) => {
        gt.attentionRequiredPanelTag = y.tag, t[0].type !== void 0 && t[0].message !== void 0 ? this.log(t[0].type, t[0].message, !!t[0].internal, t[0].details, t[0].link) : this.log(zr.ERROR, t.map((a) => this.format(a)).join(" "), false), e.push(t);
      }
    };
  }
  render() {
    return zt`<style>
        ${S}
      </style>
      ${this.messages.map((e) => this.renderMessage(e))} `;
  }
  renderMessage(e) {
    let t, a, o2;
    return e.type === zr.ERROR ? (t = "error", o2 = r$1.exclamationMark, a = "Error") : e.type === zr.WARNING ? (t = "warning", o2 = r$1.warning, a = "Warning") : (t = "info", o2 = r$1.info, a = "Info"), e.internal && (t += " internal"), zt`
      <div class="row ${e.type} ${e.details || e.link ? "has-details" : ""}">
        <span class="type ${t}" title="${a}">${o2}</span>
        <div class="message" @click=${() => this.toggleExpanded(e)}>
          <span class="firstrow">
            <span class="timestamp" ?hidden=${!g.showTimestamps}>${q(e.timestamp)}</span>
            <span class="firstrowmessage"
              >${e.expanded && e.expandedMessage ? e.expandedMessage : e.message}
            </span>
          </span>
          ${e.expanded ? zt` <span class="secondrow">${e.expandedDetails}</span>` : zt`<span class="secondrow" ?hidden="${!e.details && !e.link}"
                >${Ll(e.details)}
                ${e.link ? zt`<a class="ahreflike" href="${e.link}" target="_blank">Learn more</a>` : ""}</span
              >`}
        </div>
        <button
          aria-label="Expand details"
          theme="icon tertiary"
          class="expand"
          @click=${() => this.toggleExpanded(e)}
          ?hidden=${!e.expandedDetails}>
          <span>${e.expanded ? r$1.chevronDown : r$1.chevronRight}</span>
        </button>
      </div>
    `;
  }
  log(e, t, a, o2, s, p, i, $) {
    const E = this.nextMessageId;
    this.nextMessageId += 1;
    const u = Ml(t, 200);
    u !== t && !i && (i = t);
    const m = {
      id: E,
      type: e,
      message: u,
      details: o2,
      link: s,
      dontShowAgain: false,
      deleted: false,
      expanded: false,
      expandedMessage: p,
      expandedDetails: i,
      timestamp: /* @__PURE__ */ new Date(),
      internal: a,
      userId: $
    };
    for (this.messages.push(m); this.messages.length > r.MAX_LOG_ROWS; )
      this.messages.shift();
    return this.requestUpdate(), this.updateComplete.then(() => {
      const f = this.renderRoot.querySelector(".message:last-child");
      f ? (setTimeout(() => f.scrollIntoView({ behavior: "smooth" }), this.transitionDuration), this.unreadErrors = false) : e === zr.ERROR && (this.unreadErrors = true);
    }), m;
  }
  updateLog(e) {
    let t = this.messages.find((a) => a.userId === e.id);
    t || (t = this.log(zr.INFORMATION, "<Log message to update was not found>", false)), Object.assign(t, e), ss(t.expandedDetails) && (t.expandedDetails = Ll(t.expandedDetails)), this.requestUpdate();
  }
  toggleExpanded(e) {
    e.expandedDetails && (e.expanded = !e.expanded, this.requestUpdate());
  }
};
r.MAX_LOG_ROWS = 1e3;
h([
  g$1()
], r.prototype, "unreadErrors", 2);
h([
  g$1()
], r.prototype, "messages", 2);
r = h([
  vs("copilot-log-panel")
], r);
let w = class extends Ts {
  createRenderRoot() {
    return this;
  }
  connectedCallback() {
    super.connectedCallback(), this.style.display = "flex";
  }
  render() {
    return zt`
      <button title="Clear log" aria-label="Clear log" theme="icon tertiary">
        <span
          @click=${() => {
      x.emit("clear-log", {});
    }}
          >${r$1.trash}</span
        >
      </button>
      <button title="Toggle timestamps" aria-label="Toggle timestamps" theme="icon tertiary">
        <span
          class="${g.showTimestamps ? "on" : "off"}"
          @click=${() => {
      g.toggleShowTimestamps();
    }}
          >${r$1.clock}</span
        >
      </button>
    `;
  }
};
w = h([
  vs("copilot-log-panel-actions")
], w);
const y = {
  header: "Log",
  expanded: true,
  panelOrder: 0,
  panel: "bottom",
  floating: false,
  tag: "copilot-log-panel",
  actionsTag: "copilot-log-panel-actions"
}, P = {
  init(e) {
    e.addPanel(y);
  }
};
window.Vaadin.copilot.plugins.push(P);
const B = { hour: "numeric", minute: "numeric", second: "numeric", fractionalSecondDigits: 3 }, A = new Intl.DateTimeFormat(navigator.language, B);
function q(e) {
  return A.format(e);
}
export {
  w as Actions,
  r as CopilotLogPanel
};
