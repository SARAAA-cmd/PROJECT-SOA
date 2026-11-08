// Rend le diagramme BPMN en PNG (et SVG) avec bpmn-js dans Chromium headless.
// Usage : NODE_PATH=/opt/node22/lib/node_modules node tools/rendre_bpmn.js
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

(async () => {
  const racine = path.resolve(__dirname, '..');
  const xml = fs.readFileSync(path.join(racine, 'src/main/resources/processes/candidature-stage.bpmn20.xml'), 'utf8');
  const vendor = path.join(racine, 'src/main/resources/public/vendor/bpmn-js');
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 3300, height: 1020 }, deviceScaleFactor: 2 });
  await page.setContent(`<html><head>
    <style>${fs.readFileSync(path.join(vendor, 'assets/diagram-js.css'), 'utf8')}
    ${fs.readFileSync(path.join(vendor, 'assets/bpmn-js.css'), 'utf8')}
    html,body,#c{margin:0;width:100%;height:100%;background:#fff} .bjs-powered-by{display:none}</style></head>
    <body><div id="c"></div></body></html>`);
  await page.addScriptTag({ path: path.join(vendor, 'bpmn-navigated-viewer.production.min.js') });
  const res = await page.evaluate(async (x) => {
    const v = new BpmnJS({ container: '#c' });
    const r = await v.importXML(x);
    v.get('canvas').zoom('fit-viewport');
    const { svg } = await v.saveSVG();
    return { warnings: r.warnings.map((w) => w.message), svg };
  }, xml);
  console.log('Avertissements :', res.warnings.length ? res.warnings : 'aucun');
  fs.writeFileSync(path.join(racine, 'rapport/figures/processus-bpmn.svg'), res.svg);
  await page.screenshot({ path: path.join(racine, 'rapport/figures/processus-bpmn.png') });
  await browser.close();
})();
