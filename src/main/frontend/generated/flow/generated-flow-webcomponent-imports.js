import { injectGlobalWebcomponentCss } from 'Frontend/generated/jar-resources/theme-util.js';

import '@vaadin/vertical-layout/src/vaadin-vertical-layout.js';
import '@vaadin/app-layout/src/vaadin-app-layout.js';
import '@vaadin/app-layout/src/vaadin-drawer-toggle.js';
import '@vaadin/button/src/vaadin-button.js';
import '@vaadin/tooltip/src/vaadin-tooltip.js';
import 'Frontend/generated/jar-resources/disableOnClickFunctions.js';
import '@vaadin/popover/src/vaadin-popover.js';
import 'Frontend/generated/jar-resources/vaadin-popover/popover.ts';
import '@vaadin/icons/vaadin-iconset.js';
import '@vaadin/icon/src/vaadin-icon.js';
import '@vaadin/list-box/src/vaadin-list-box.js';
import 'Frontend/generated/jar-resources/flow-component-renderer.js';
import 'Frontend/generated/jar-resources/flow-component-directive.js';
import 'Frontend/generated/jar-resources/lit-renderer.ts';
import '@vaadin/item/src/vaadin-item.js';
import '@vaadin/horizontal-layout/src/vaadin-horizontal-layout.js';
import '@vaadin/progress-bar/src/vaadin-progress-bar.js';
import '@vaadin/grid/src/vaadin-grid-tree-toggle.js';
import 'Frontend/generated/jar-resources/treeGridConnector.ts';
import 'Frontend/generated/jar-resources/gridConnector.ts';
import '@vaadin/grid/src/vaadin-grid.js';
import '@vaadin/grid/src/vaadin-grid-column.js';
import '@vaadin/grid/src/vaadin-grid-sorter.js';
import '@vaadin/checkbox/src/vaadin-checkbox.js';
import 'Frontend/generated/jar-resources/vaadin-grid-flow-selection-column.js';
import '@vaadin/grid/src/vaadin-grid-column-group.js';
import '@vaadin/context-menu/src/vaadin-context-menu.js';
import 'Frontend/generated/jar-resources/contextMenuConnector.js';
import 'Frontend/generated/jar-resources/contextMenuTargetConnector.js';
import '@vaadin/upload/src/vaadin-upload.js';
import '@vaadin/combo-box/src/vaadin-combo-box.js';
import 'Frontend/generated/jar-resources/comboBoxConnector.js';
import '@vaadin/multi-select-combo-box/src/vaadin-multi-select-combo-box.js';
import '@vaadin/number-field/src/vaadin-number-field.js';
import '@vaadin/form-layout/src/vaadin-form-layout.js';
import '@vaadin/form-layout/src/vaadin-form-item.js';
import '@vaadin/form-layout/src/vaadin-form-row.js';
import '@vaadin/details/src/vaadin-details.js';
import '@vaadin/notification/src/vaadin-notification.js';
import '@vaadin/text-area/src/vaadin-text-area.js';
import '@vaadin/tabs/src/vaadin-tabs.js';
import '@vaadin/tabs/src/vaadin-tab.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import 'Frontend/generated/jar-resources/ReactRouterOutletElement.tsx';

const loadOnDemand = (key) => {
  const pending = [];
  if (key === '1265b06209c839fd679eb8b9466da22ccecb1069d46e0768556bcc8e59014779') {
    pending.push(import('./chunks/chunk-98c64a4fd40db554326cf598f78d9f1a991c2d71cae2af63c5a2d8cbddad93d9.js'));
  }
  if (key === '75fa9297ec12d0eaef79767cac5479e6746ac44aa2b2e4953208027bc05a23b1') {
    pending.push(import('./chunks/chunk-72c7c4fdbd217e4e9e9bfe418af801c659badd1b0a3e95d75f92ad69f3cd7ac5.js'));
  }
  if (key === '581235731a74cf8f17e838e4b12550fa5de1975c8c8c87aae1dd42c01defb9a4') {
    pending.push(import('./chunks/chunk-c79af0f8a55203463d3de9fe796021943cfbe571652d1d7b67bcda0081816cf3.js'));
  }
  if (key === '89e8b043961f424378d4833fa5ced8416e1057fc54fa477739ad987956b4e9da') {
    pending.push(import('./chunks/chunk-7e1f0d2ce12459468efbd537b026af90aa0c00abd62a3d3b7a9cd08fe64fcc12.js'));
  }
  if (key === 'a5afccb5c832f2b92e1234c0a79dcbdd5cc84d721770d5d567df8580171f1d16') {
    pending.push(import('./chunks/chunk-5c20a9fad99eaaf22c07fb7e5d2b57b8f7398e021bdce3a45df208c2abecb4ff.js'));
  }
  if (key === 'eb9080c4cdfce7883e08c8e1fc9833ad6aa9e542e7762e3768b86e1ba876cf10') {
    pending.push(import('./chunks/chunk-eedfe9fa92df0e614545176fe24e6df6e6b8641206bb22b0960481c637b11ac4.js'));
  }
  if (key === '6594740d6f7dd85df3e8e1a302d6edc137b461306f8e9abdf10bb36cc1053af2') {
    pending.push(import('./chunks/chunk-7e1f0d2ce12459468efbd537b026af90aa0c00abd62a3d3b7a9cd08fe64fcc12.js'));
  }
  if (key === 'e9cf265128c860a3b7bb81fc4598e431280261d128b3f6732734d04f1abada27') {
    pending.push(import('./chunks/chunk-eedfe9fa92df0e614545176fe24e6df6e6b8641206bb22b0960481c637b11ac4.js'));
  }
  if (key === '42076b8b565b334e6fc36af33076c36cf8205bd2098f72ef1d99895c1a4fa3bd') {
    pending.push(import('./chunks/chunk-eedfe9fa92df0e614545176fe24e6df6e6b8641206bb22b0960481c637b11ac4.js'));
  }
  return Promise.all(pending);
}
window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}