// Parcours de démonstration de l'interface + captures d'écran (rapport/figures/ui-*.png).
// Prérequis : plateforme démarrée sur http://localhost:8090 avec une base neuve.
const { chromium } = require('playwright');
const path = require('path');
const OUT = path.resolve(__dirname, '../rapport/figures');
const BASE = process.env.BASE || 'http://localhost:8090';
const MAILPIT = process.env.MAILPIT || 'http://localhost:8025';
const erreurs = [];

(async () => {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1.5 });
  const page = await ctx.newPage();
  page.on('pageerror', (e) => erreurs.push('pageerror: ' + e.message));
  page.on('console', (m) => { if (m.type() === 'error') erreurs.push('console: ' + m.text()); });
  page.on('dialog', (d) => d.accept());
  const capture = async (nom) => { await page.waitForTimeout(400); await page.screenshot({ path: `${OUT}/ui-${nom}.png` }); console.log('capture', nom); };
  const connexion = async (u) => {
    await page.goto(BASE + '/');
    await page.evaluate(() => sessionStorage.clear());
    await page.goto(BASE + '/');
    await page.click(`.comptes-demo button[data-u="${u}"]`);
    await page.waitForSelector('#app:not([hidden])');
    await page.waitForSelector('#vue[data-charge]', { state: 'attached' });
  };
  const menu = async (vue) => {
    await page.click(`#menu button[data-vue="${vue}"]`);
    await page.waitForSelector(`#vue[data-charge="${vue}"]`, { state: 'attached' });
  };
  const fermer = async () => { await page.click('#modale-fermer'); };

  // 1. Connexion
  await page.goto(BASE + '/');
  await capture('connexion');

  // 2. Étudiante : offres + candidature
  await connexion('E2026001');
  await capture('etudiant-offres');
  await page.click('[data-postuler="1"]');
  await page.click('#f-postuler button[type=submit]');
  await page.waitForSelector('#resultat .encart');
  await capture('etudiant-postuler');
  await fermer();
  await page.click('[data-postuler="4"]'); // Data analyst : filière non ciblée -> rejet
  await page.click('#f-postuler button[type=submit]');
  await page.waitForSelector('#resultat .encart');
  await capture('etudiant-rejet');
  await fermer();
  await menu('candidatures');
  await capture('etudiant-candidatures');

  // 3. Entreprise : évaluation et décision
  await connexion('atlas');
  await capture('entreprise-evaluations');
  await page.click('[data-decider]');
  await page.fill('#f-decision [name=commentaire]', 'Profil très pertinent, bienvenue chez Atlas Digital !');
  await page.fill('#f-decision [name=tuteur]', 'Nadia Berrada');
  await capture('entreprise-decision');
  await page.click('#f-decision button[type=submit]');
  await page.waitForTimeout(2500);
  await menu('offres-ent');
  await capture('entreprise-offres');

  // 3b. Boîte e-mail de test (Mailpit) : vrais e-mails envoyés en SMTP
  if (process.env.SANS_MAILPIT) {
    console.log('Mailpit ignoré (SANS_MAILPIT)');
  } else {
    await page.goto(MAILPIT + '/');
    await page.waitForTimeout(1500);
    await capture('mailpit-boite');
    await page.goto(MAILPIT + '/view/latest');
    await page.waitForTimeout(2000);
    await capture('mailpit-email');
  }

  // 4. Étudiante : notifications et stage
  await connexion('E2026001');
  await menu('notifications');
  await capture('etudiant-notifications');
  await menu('stage');
  await capture('etudiant-stage');

  // 5. Incident : panne du service candidatures pendant une candidature
  await connexion('admin');
  await menu('supervision');
  await page.click('[data-panne="candidatures"]');
  await page.waitForTimeout(500);
  await capture('admin-supervision');
  await connexion('E2026002');
  await page.click('[data-postuler="2"]');
  await page.click('#f-postuler button[type=submit]');
  await page.waitForSelector('#resultat .encart');
  await capture('etudiant-incident');
  await connexion('admin');
  await menu('supervision');
  await page.click('[data-panne="candidatures"]');
  await page.waitForTimeout(500);
  await menu('incidents');
  await capture('admin-incidents');
  await page.click('[data-voir]');
  await page.waitForSelector('#bpmn-instance svg');
  await page.waitForTimeout(800);
  await capture('admin-instance-incident');
  await fermer();
  await page.click('[data-action="RELANCER"]');
  await page.waitForTimeout(2500);

  // 6. Responsable : tableau de bord, processus, étudiants
  await menu('tableau');
  await capture('admin-tableau');
  await menu('processus');
  await page.waitForSelector('#bpmn-modele svg');
  await page.waitForTimeout(800);
  await capture('admin-processus');
  await page.click('tr.cliquable >> nth=-1');
  await page.waitForSelector('#bpmn-instance svg');
  await page.waitForTimeout(800);
  await capture('admin-instance');
  await fermer();
  await menu('etudiants');
  await capture('admin-etudiants');
  await menu('entreprises');
  await capture('admin-entreprises');

  await browser.close();
  console.log(erreurs.length ? 'ERREURS:\n' + erreurs.join('\n') : 'Aucune erreur JavaScript');
})().catch((e) => { console.error(e); console.log(erreurs.join('\n')); process.exit(1); });
