/*
 * Interface web de la plateforme de gestion des stages.
 * - Données SOAP (étudiants, entreprises, offres) : via Soap.appeler (soap.js)
 * - Données REST (candidatures, notifications, stages, processus) : via Api
 */

// ======================================================================
// Authentification
// ======================================================================
const Auth = (() => {
  let session = null;
  try { session = JSON.parse(sessionStorage.getItem('session')); } catch (e) { session = null; }
  return {
    identifiants: () => session,
    utilisateur: () => session && session.profil,
    async connecter(username, password) {
      session = { username, password };
      try {
        session.profil = await Api.get('/moi');
      } catch (e) {
        session = null;
        throw e;
      }
      try { sessionStorage.setItem('session', JSON.stringify(session)); } catch (e) { /* stockage indisponible */ }
    },
    deconnecter() {
      session = null;
      try { sessionStorage.removeItem('session'); } catch (e) { /* ignoré */ }
    },
  };
})();

// ======================================================================
// Client REST
// ======================================================================
const Api = (() => {
  async function appel(methode, chemin, corps) {
    const s = Auth.identifiants();
    const entetes = { Accept: 'application/json', 'X-Requested-With': 'fetch' };
    if (s) entetes.Authorization = 'Basic ' + btoa(unescape(encodeURIComponent(`${s.username}:${s.password}`)));
    if (corps !== undefined) entetes['Content-Type'] = 'application/json';
    const r = await fetch('/api' + chemin, { method: methode, headers: entetes, body: corps === undefined ? undefined : JSON.stringify(corps) });
    const texte = await r.text();
    let json = null;
    try { json = texte ? JSON.parse(texte) : null; } catch (e) { json = texte; }
    if (!r.ok) {
      const msg = json && json.message ? json.message + (json.details ? ' : ' + json.details.join(' ; ') : '') : `Erreur HTTP ${r.status}`;
      const err = new Error(msg);
      err.status = r.status;
      throw err;
    }
    return json;
  }
  return {
    get: (c) => appel('GET', c),
    post: (c, b) => appel('POST', c, b ?? {}),
    patch: (c, b) => appel('PATCH', c, b ?? {}),
    del: (c) => appel('DELETE', c),
    async xml(c) {
      const s = Auth.identifiants();
      const r = await fetch('/api' + c, { headers: { Accept: 'application/xml', Authorization: 'Basic ' + btoa(`${s.username}:${s.password}`) } });
      return r.text();
    },
  };
})();

// ======================================================================
// Utilitaires d'interface
// ======================================================================
const $ = (sel, racine = document) => racine.querySelector(sel);
const $$ = (sel, racine = document) => Array.from(racine.querySelectorAll(sel));
const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

const LIBELLES = {
  DEPOSEE: 'Déposée', EN_COURS_EVALUATION: "En cours d'évaluation", ACCEPTEE: 'Acceptée', REFUSEE: 'Refusée', ANNULEE: 'Annulée',
  EN_ATTENTE_DECISION: 'En attente de décision', REJETEE: 'Rejetée', INCIDENT_TECHNIQUE: 'Incident technique', ABANDONNEE: 'Abandonnée',
  INTERROMPU: 'Interrompu', EN_COURS: 'En cours', PLANIFIE: 'Planifié', TERMINE: 'Terminé', ACTIF: 'Actif', SUSPENDU: 'Suspendu',
  EN_STAGE: 'En stage', DIPLOME: 'Diplômé', ACTIVE: 'Active', SUSPENDUE: 'Suspendue', POURVUE: 'Pourvue', CLOTUREE: 'Clôturée',
  JOURNALISEE: 'Journalisée', ECHEC_ENVOI: 'Échec d\'envoi', ARCHIVEE: 'Archivée',
  RESPONSABLE: 'Responsable des stages', ETUDIANT: 'Étudiant', ENTREPRISE: 'Entreprise', SYSTEME: 'Système',
};
const badge = (s) => `<span class="badge b-${esc(s)}">${esc(LIBELLES[s] || s)}</span>`;
const date = (d) => (d ? new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' }) : '—');
const dateHeure = (d) => (d ? new Date(d).toLocaleString('fr-FR', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '—');
const liste = (v) => (v === undefined || v === null || v === '' ? [] : [].concat(v));
const competences = (s) => (s ? String(s).split(',').map((x) => x.trim()).filter(Boolean) : []);
const norm = (s) => s.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();

function toast(message, type = '') {
  const t = document.createElement('div');
  t.className = 'toast ' + type;
  t.textContent = message;
  $('#toasts').append(t);
  setTimeout(() => t.remove(), 4500);
}

const Modale = {
  ouvrir(html, { large = false } = {}) {
    $('#modale-contenu').innerHTML = html;
    $('.modale-boite').classList.toggle('large', large);
    $('#modale').hidden = false;
    return $('#modale-contenu');
  },
  fermer() { $('#modale').hidden = true; $('#modale-contenu').innerHTML = ''; },
};

function tableau(colonnes, lignes, { vide = 'Aucun élément', clic } = {}) {
  if (!lignes.length) return `<div class="vide">${vide}</div>`;
  return `<div class="tableau-conteneur"><table><thead><tr>${colonnes.map((c) => `<th>${c[0]}</th>`).join('')}</tr></thead><tbody>
    ${lignes.map((l, i) => `<tr ${clic ? `class="cliquable" data-index="${i}"` : ''}>${colonnes.map((c) => `<td>${c[1](l)}</td>`).join('')}</tr>`).join('')}
  </tbody></table></div>`;
}

function lierClics(racine, lignes, fn) {
  $$('tr.cliquable', racine).forEach((tr) => tr.addEventListener('click', (e) => {
    if (e.target.closest('button, select, a')) return;
    fn(lignes[Number(tr.dataset.index)]);
  }));
}

function formulaire(form) {
  const o = {};
  new FormData(form).forEach((v, k) => { o[k] = typeof v === 'string' ? v.trim() : v; });
  return o;
}

async function action(bouton, fn) {
  if (bouton) bouton.disabled = true;
  try { return await fn(); } catch (e) { toast(e.message, 'ko'); return undefined; } finally { if (bouton) bouton.disabled = false; }
}

// Cache des entreprises (noms) — service SOAP Entreprises
let cacheEntreprises = null;
async function entreprises() {
  if (!cacheEntreprises) cacheEntreprises = await Soap.appeler('entreprises', 'listerEntreprises', {}, { liste: true });
  return cacheEntreprises;
}
async function nomEntreprise(id) {
  const e = (await entreprises()).find((x) => Number(x.id) === Number(id));
  return e ? e.raisonSociale : `Entreprise ${id}`;
}

// ======================================================================
// Navigation
// ======================================================================
const MENUS = {
  ETUDIANT: [['offres', 'Offres de stage'], ['candidatures', 'Mes candidatures'], ['stage', 'Mon stage'], ['notifications', 'Notifications']],
  ENTREPRISE: [['evaluations', 'Candidatures à évaluer'], ['offres-ent', 'Mes offres'], ['candidatures-ent', 'Candidatures reçues'], ['stagiaires', 'Stagiaires'], ['notifications', 'Notifications']],
  RESPONSABLE: [['tableau', 'Tableau de bord'], ['processus', 'Processus BPMN'], ['incidents', 'Incidents'], ['etudiants', 'Étudiants'], ['entreprises', 'Entreprises'], ['offres-admin', 'Offres'], ['candidatures-admin', 'Candidatures'], ['stages-admin', 'Stages'], ['supervision', 'Supervision']],
};

let vueCourante = null;

async function afficher(vue) {
  vueCourante = vue;
  $('#profil')?.removeAttribute('open');
  try { sessionStorage.setItem('vue', vue); } catch (e) { /* ignoré */ }
  const u = Auth.utilisateur();
  $$('#menu button').forEach((b) => b.classList.toggle('actif', b.dataset.vue === vue));
  $('#menu button.actif')?.scrollIntoView({ block: 'nearest', inline: 'nearest' });
  const libelle = MENUS[u.role].find((m) => m[0] === vue)?.[1] || '';
  $('#titre-vue').textContent = libelle;
  $('#actions-vue').innerHTML = '';
  const racine = $('#vue');
  racine.innerHTML = '<div class="vide">Chargement…</div>';
  delete racine.dataset.charge;
  try {
    await VUES[vue](racine, u);
    racine.dataset.charge = vue;
  } catch (e) {
    racine.innerHTML = `<div class="encart ko">${esc(e.message)}</div>`;
    racine.dataset.charge = vue;
  }
  rafraichirPastilles();
}

async function rafraichirPastilles() {
  const u = Auth.utilisateur();
  if (!u) return;
  const maj = (vue, n) => {
    const b = $(`#menu button[data-vue="${vue}"]`);
    if (!b) return;
    b.querySelector('.pastille')?.remove();
    if (n > 0) b.insertAdjacentHTML('beforeend', `<span class="pastille">${n}</span>`);
  };
  try {
    if (u.role === 'RESPONSABLE') {
      const t = await Api.get('/processus/taches');
      maj('incidents', t.filter((x) => x.type === 'traiterIncident').length);
    } else if (u.role === 'ENTREPRISE') {
      maj('evaluations', (await Api.get('/processus/taches')).length);
      maj('notifications', (await Api.get('/notifications')).filter((n) => !n.lue).length);
    } else {
      maj('notifications', (await Api.get('/notifications')).filter((n) => !n.lue).length);
    }
  } catch (e) { /* service peut-être en panne */ }
}

async function demarrerApplication() {
  const u = Auth.utilisateur();
  $('#ecran-connexion').hidden = true;
  $('#app').hidden = false;
  $('#menu').innerHTML = MENUS[u.role].map(([v, l]) => `<button data-vue="${v}">${l}</button>`).join('');
  $$('#menu button').forEach((b) => b.addEventListener('click', () => afficher(b.dataset.vue)));
  $('#app').classList.toggle('menu-long', MENUS[u.role].length > 5);
  let nom = u.username;
  try {
    if (u.role === 'ETUDIANT') { const e = await Soap.appeler('etudiants', 'consulterEtudiant', { matricule: u.refId }); nom = `${e.prenom} ${e.nom}`; }
    if (u.role === 'ENTREPRISE') nom = await nomEntreprise(u.refId);
    if (u.role === 'RESPONSABLE') nom = 'Bureau des stages';
  } catch (e) { /* nom par défaut */ }
  $('#profil-nom').textContent = nom;
  $('#profil-role').textContent = LIBELLES[u.role];
  $('#profil-initiales').textContent = nom.split(/\s+/).filter(Boolean).slice(0, 2).map((m) => m[0].toUpperCase()).join('');
  let vue = null;
  try { vue = sessionStorage.getItem('vue'); } catch (e) { vue = null; }
  afficher(MENUS[u.role].some((m) => m[0] === vue) ? vue : MENUS[u.role][0][0]);
}

// ======================================================================
// Vues
// ======================================================================
const VUES = {};

// ---------------------------------------------------------------- Étudiant
VUES.offres = async (racine, u) => {
  const [offres, moi] = await Promise.all([
    Soap.appeler('offres', 'listerOffresDisponibles', {}, { liste: true }),
    Soap.appeler('etudiants', 'consulterEtudiant', { matricule: u.refId }),
  ]);
  // dégradation gracieuse : les offres restent consultables si le service Candidatures est indisponible
  const mesCandidatures = await Api.get('/candidatures').catch(() => []);
  const dejaPostule = new Set(mesCandidatures.filter((c) => c.statut !== 'ANNULEE').map((c) => c.offreId));
  const compat = await Promise.all(offres.map((o) => Soap.appeler('offres', 'verifierCompatibilite',
    { offreId: o.id, filiere: moi.filiere, niveau: moi.niveau, competences: moi.competences || '' }).catch(() => null)));
  const noms = await Promise.all(offres.map((o) => nomEntreprise(o.entrepriseId)));
  const mesComp = new Set(competences(moi.competences).map(norm));

  racine.innerHTML = `
    <p class="muted">Profil : <strong>${esc(moi.filiere)} — ${esc(moi.niveau)}</strong> · statut ${badge(moi.statut)}
      · compétences : ${competences(moi.competences).map((c) => `<span class="tag">${esc(c)}</span>`).join(' ') || '—'}</p>
    ${moi.statut !== 'ACTIF' ? `<div class="encart attention">Votre statut (${esc(LIBELLES[moi.statut])}) ne vous permet pas de déposer de nouvelles candidatures.</div>` : ''}
    <div class="grille">${offres.map((o, i) => {
      const c = compat[i];
      return `<div class="carte">
        <h3>${esc(o.titre)}</h3>
        <div class="meta"><span>🏢 ${esc(noms[i])}</span><span>📍 ${esc(o.lieu || '—')}</span><span>⏱ ${o.dureeMois} mois</span>
          <span>📅 avant le ${date(o.dateLimite)}</span><span>👥 ${o.placesDisponibles} place(s)</span></div>
        <p>${esc(o.description)}</p>
        <div class="tags">${competences(o.competencesRequises).map((k) => `<span class="tag ${mesComp.has(norm(k)) ? 'ok' : ''}">${esc(k)}</span>`).join('')}</div>
        ${c ? `<div class="score" style="margin-top:12px">Adéquation compétences <div class="jauge"><div style="width:${c.scoreCompetences}%"></div></div> ${c.scoreCompetences}%</div>
          ${c.compatible ? '' : `<div class="encart ko">${liste(c.motif).map(esc).join('<br>')}</div>`}` : ''}
        <div class="pied"><span class="muted">Niveau min. ${esc(o.niveauMinimum || '—')} · ${esc(o.filieresCiblees || 'toutes filières')}</span>
          ${dejaPostule.has(o.id) ? '<span class="badge b-DEPOSEE">Candidature envoyée</span>'
            : `<button class="btn primaire" data-postuler="${o.id}">Postuler</button>`}</div>
      </div>`;
    }).join('') || '<div class="vide">Aucune offre disponible</div>'}</div>`;

  $$('[data-postuler]', racine).forEach((b) => b.addEventListener('click', () => {
    const o = offres.find((x) => String(x.id) === b.dataset.postuler);
    const m = Modale.ouvrir(`
      <h2>Postuler : ${esc(o.titre)}</h2>
      <p class="muted">Votre candidature déclenche le processus BPMN de traitement : vérifications (SOAP), enregistrement (REST), puis transmission à l'entreprise.</p>
      <form id="f-postuler">
        <label>Lettre de motivation<textarea name="lettreMotivation" required maxlength="4000">Madame, Monsieur,\n\nVotre offre « ${esc(o.titre)} » correspond pleinement à mon projet professionnel…</textarea></label>
        <label>Lien vers le CV<input name="cvUrl" type="url" placeholder="https://…" value="https://cv.example/${esc(u.refId)}.pdf"></label>
        <button class="btn primaire" type="submit">Envoyer la candidature</button>
      </form><div id="resultat"></div>`);
    $('#f-postuler', m).addEventListener('submit', (e) => {
      e.preventDefault();
      action(e.submitter, async () => {
        const r = await Api.post('/processus/candidatures', { offreId: o.id, ...formulaire(e.target) });
        const v = r.variables;
        const res = $('#resultat', m);
        if (r.statut === 'REJETEE') {
          res.innerHTML = `<div class="encart ko"><strong>Candidature non recevable.</strong><br>${esc(v.motifRejet)}</div>`;
        } else if (r.statut === 'INCIDENT_TECHNIQUE') {
          res.innerHTML = '<div class="encart attention"><strong>Incident technique.</strong> Un service est momentanément indisponible ; le responsable des stages a été alerté et relancera le traitement.</div>';
        } else {
          res.innerHTML = `<div class="encart ok"><strong>Candidature n°${v.candidatureId} enregistrée</strong> et transmise à ${esc(v.entreprise)}. Vous serez notifié(e) de la décision.</div>`;
        }
        e.target.remove();
        afficher('offres');
      });
    });
  }));
};

VUES.candidatures = async (racine) => {
  const cands = await Api.get('/candidatures');
  racine.innerHTML = tableau([
    ['N°', (c) => c.id],
    ['Offre', (c) => `<strong>${esc(c.titreOffre)}</strong>`],
    ['Déposée le', (c) => date(c.dateDepot)],
    ['Adéquation', (c) => (c.scoreCompatibilite ?? '—') + ' %'],
    ['Statut', (c) => badge(c.statut)],
    ['Motif / commentaire', (c) => esc(c.motif || '')],
  ], cands, { vide: "Vous n'avez encore déposé aucune candidature.", clic: true });
  lierClics(racine, cands, detailCandidature);
};

async function detailCandidature(c) {
  const u = Auth.utilisateur();
  const hist = await Api.get(`/candidatures/${c.id}/historique`);
  const modifiable = ['DEPOSEE', 'EN_COURS_EVALUATION'].includes(c.statut);
  const m = Modale.ouvrir(`
    <h2>Candidature n°${c.id} ${badge(c.statut)}</h2>
    <p><strong>${esc(c.titreOffre)}</strong> — ${esc(c.nomEtudiant)} (${esc(c.matricule)})</p>
    <p class="muted">CV : ${c.cvUrl ? `<a href="${esc(c.cvUrl)}" target="_blank" rel="noopener">${esc(c.cvUrl)}</a>` : '—'}</p>
    <h3>Historique</h3>
    <ul class="chronologie">${hist.map((h) => `<li><strong>${esc(LIBELLES[h.nouveauStatut] || h.nouveauStatut)}</strong>
      ${h.commentaire ? `— ${esc(h.commentaire)}` : ''}<div class="quand">${dateHeure(h.date)} · ${esc(h.auteur)}</div></li>`).join('')}</ul>
    ${u.role === 'ETUDIANT' && modifiable ? `
      <h3>Compléter mon dossier</h3>
      <form id="f-completer">
        <label>Nouveau lien de CV / pièce complémentaire<input name="cvUrl" type="url" value="${esc(c.cvUrl || '')}"></label>
        <label>Lettre de motivation<textarea name="lettreMotivation">${esc(c.lettreMotivation || '')}</textarea></label>
        <div class="btn-groupe"><button class="btn primaire" type="submit">Enregistrer</button>
        ${c.statut === 'DEPOSEE' ? '<button class="btn danger" type="button" id="b-annuler">Annuler la candidature</button>' : ''}</div>
        ${c.statut === 'DEPOSEE' ? '' : '<p class="muted">L\'entreprise examine votre dossier : la candidature ne peut plus être annulée.</p>'}
      </form>` : ''}`);
  $('#f-completer', m)?.addEventListener('submit', (e) => {
    e.preventDefault();
    action(e.submitter, async () => { await Api.patch(`/candidatures/${c.id}`, formulaire(e.target)); toast('Dossier complété', 'ok'); Modale.fermer(); afficher(vueCourante); });
  });
  $('#b-annuler', m)?.addEventListener('click', (e) => {
    if (!confirm('Annuler définitivement cette candidature ?')) return;
    action(e.target, async () => { await Api.del(`/candidatures/${c.id}`); toast('Candidature annulée', 'ok'); Modale.fermer(); afficher(vueCourante); });
  });
}

VUES.stage = async (racine, u) => {
  const hist = await Api.get(`/stages/etudiant/${u.refId}/historique`);
  if (!hist.length) { racine.innerHTML = '<div class="vide">Aucun stage pour le moment. Les stages apparaissent ici dès qu\'une candidature est acceptée.</div>'; return; }
  const statuts = await Promise.all(hist.map((h) => Api.get(`/stages/${h.stage.id}/statut`)));
  racine.innerHTML = hist.map((h, i) => carteStage(h.stage, statuts[i], h)).join('');
  lierCartesStage(racine, u);
};

function carteStage(s, st, h) {
  return `<div class="carte" style="margin-bottom:16px">
    <div class="pied" style="margin-top:0"><h2>${esc(s.titreOffre)}</h2>${badge(s.statut)}</div>
    <div class="meta"><span>🏢 ${esc(s.nomEntreprise)}</span><span>👤 ${esc(s.nomEtudiant)}</span><span>📅 ${date(s.dateDebut)} → ${date(s.dateFin)}</span>
      <span>Tuteur : ${esc(s.tuteurEntreprise || '—')}</span></div>
    ${st ? `<div class="score">Progression <div class="jauge"><div style="width:${st.progression}%"></div></div> ${st.progression}% · ${st.joursRestants} j restants</div>` : ''}
    ${h && h.moyenne ? `<p>Moyenne des évaluations : <strong>${h.moyenne}/20</strong></p>` : ''}
    <div class="pied"><span class="muted">${h ? `${h.nombreComptesRendus} compte(s) rendu(s)` : ''}</span>
    <button class="btn" data-suivi="${s.id}">Comptes rendus & évaluations</button></div></div>`;
}

function lierCartesStage(racine, u) {
  $$('[data-suivi]', racine).forEach((b) => b.addEventListener('click', () => suiviStage(Number(b.dataset.suivi), u)));
}

async function suiviStage(id, u) {
  const [s, crs, evals] = await Promise.all([Api.get(`/stages/${id}`), Api.get(`/stages/${id}/comptes-rendus`), Api.get(`/stages/${id}/evaluations`)]);
  const ouvert = ['PLANIFIE', 'EN_COURS'].includes(s.statut);
  const m = Modale.ouvrir(`
    <h2>${esc(s.titreOffre)} ${badge(s.statut)}</h2>
    <p class="muted">${esc(s.nomEtudiant)} · ${esc(s.nomEntreprise)} · ${date(s.dateDebut)} → ${date(s.dateFin)}</p>
    ${u.role !== 'ETUDIANT' && s.statut === 'PLANIFIE' ? '<button class="btn" id="b-demarrer">Marquer le stage comme démarré</button>' : ''}
    <h3 style="margin-top:16px">Comptes rendus périodiques</h3>
    <ul class="chronologie">${crs.map((c) => `<li><strong>${esc(c.periode)}</strong><div>${esc(c.contenu)}</div><div class="quand">${dateHeure(c.dateDepot)} · ${esc(c.auteur)}</div></li>`).join('') || '<li class="muted">Aucun compte rendu</li>'}</ul>
    ${ouvert ? `<form id="f-cr" class="formulaire-grille">
      <label>Période<input name="periode" required placeholder="Semaine 1, Mois 2…"></label>
      <label style="grid-column:1/-1">Contenu<textarea name="contenu" required></textarea></label>
      <div><button class="btn primaire" type="submit">Déposer le compte rendu</button></div></form>` : ''}
    <h3 style="margin-top:16px">Évaluations</h3>
    ${tableau([['Type', (e) => esc(e.type)], ['Évaluateur', (e) => esc(e.evaluateur)], ['Note', (e) => `<strong>${e.note}/20</strong>`], ['Appréciation', (e) => esc(e.appreciation || '')]], evals, { vide: 'Aucune évaluation' })}
    ${u.role !== 'ETUDIANT' && ouvert ? `<form id="f-eval" class="formulaire-grille" style="margin-top:12px">
      <label>Type<select name="type"><option value="MI_PARCOURS">Mi-parcours</option><option value="FINALE">Finale (clôture le stage)</option></select></label>
      <label>Évaluateur<input name="evaluateur" required value="${esc(s.tuteurEntreprise || '')}"></label>
      <label>Note /20<input name="note" type="number" min="0" max="20" step="0.25" required></label>
      <label style="grid-column:1/-1">Appréciation<textarea name="appreciation"></textarea></label>
      <div><button class="btn primaire" type="submit">Enregistrer l'évaluation</button></div></form>` : ''}`, { large: true });
  $('#b-demarrer', m)?.addEventListener('click', (e) => action(e.target, async () => { await Api.patch(`/stages/${id}/statut`, { statut: 'EN_COURS' }); toast('Stage démarré', 'ok'); suiviStage(id, u); afficher(vueCourante); }));
  $('#f-cr', m)?.addEventListener('submit', (e) => { e.preventDefault(); action(e.submitter, async () => { await Api.post(`/stages/${id}/comptes-rendus`, formulaire(e.target)); toast('Compte rendu enregistré', 'ok'); suiviStage(id, u); }); });
  $('#f-eval', m)?.addEventListener('submit', (e) => {
    e.preventDefault();
    const f = formulaire(e.target);
    action(e.submitter, async () => { await Api.post(`/stages/${id}/evaluations`, { ...f, note: Number(f.note) }); toast('Évaluation enregistrée', 'ok'); suiviStage(id, u); afficher(vueCourante); });
  });
}

VUES.notifications = async (racine) => {
  const ns = await Api.get('/notifications');
  if (!ns.length) { racine.innerHTML = '<div class="vide">Aucune notification</div>'; return; }
  racine.innerHTML = `<div class="carte" style="padding:0">${ns.map((n) => `
    <div class="notif ${n.lue ? '' : 'non-lue'}" data-id="${n.id}">
      <div class="icone">${n.type.startsWith('REFUS') || n.type.startsWith('REJET') || n.type.startsWith('INCIDENT') ? '!' : '✉'}</div>
      <div><strong>${esc(n.sujet)}</strong> <span class="muted">· ${dateHeure(n.dateEnvoi)} · ${esc(n.email || '')}</span>
      <pre>${esc(n.message)}</pre></div></div>`).join('')}</div>`;
  $$('.notif.non-lue', racine).forEach((d) => d.addEventListener('click', async () => {
    await Api.patch(`/notifications/${d.dataset.id}/lue`);
    d.classList.remove('non-lue');
    rafraichirPastilles();
  }));
};

// ---------------------------------------------------------------- Entreprise
VUES.evaluations = async (racine, u) => {
  const taches = (await Api.get('/processus/taches')).filter((t) => t.type === 'evaluer');
  if (!taches.length) { racine.innerHTML = '<div class="vide">Aucune candidature en attente de décision.</div>'; return; }
  const profils = await Promise.all(taches.map((t) => Soap.appeler('etudiants', 'consulterEtudiant', { matricule: t.contexte.matricule }).catch(() => null)));
  racine.innerHTML = `<div class="grille">${taches.map((t, i) => {
    const c = t.contexte; const p = profils[i];
    return `<div class="carte">
      <div class="pied" style="margin-top:0"><h3>${esc(c.nomEtudiant)}</h3><span class="muted">Candidature n°${c.candidatureId}</span></div>
      <div class="meta"><span>🎯 ${esc(c.titreOffre)}</span>${p ? `<span>🎓 ${esc(p.filiere)} ${esc(p.niveau)}</span>` : ''}<span>reçue ${dateHeure(t.creeLe)}</span></div>
      ${p ? `<div class="tags">${competences(p.competences).map((k) => `<span class="tag">${esc(k)}</span>`).join('')}</div>` : ''}
      <div class="score" style="margin-top:10px">Adéquation <div class="jauge"><div style="width:${c.scoreCompatibilite || 0}%"></div></div> ${c.scoreCompatibilite ?? 0}%</div>
      <p style="white-space:pre-wrap">${esc((c.lettreMotivation || '').slice(0, 280))}${(c.lettreMotivation || '').length > 280 ? '…' : ''}</p>
      ${c.nombreDemandesComplement ? `<div class="encart info">${c.nombreDemandesComplement} demande(s) de complément envoyée(s) : « ${esc(c.commentaireDecision)} »</div>` : ''}
      <div class="pied">${c.cvUrl ? `<a href="${esc(c.cvUrl)}" target="_blank" rel="noopener">Voir le CV</a>` : '<span></span>'}
        ${t.priseEnChargePar ? `<span class="badge b-EN_COURS_EVALUATION">En examen (${esc(t.priseEnChargePar)})</span>` : '<span class="badge b-DEPOSEE">Nouvelle</span>'}
        <button class="btn primaire" data-decider="${t.id}">${t.priseEnChargePar ? 'Rendre une décision' : 'Examiner'}</button></div></div>`;
  }).join('')}</div>`;
  $$('[data-decider]', racine).forEach((b) => b.addEventListener('click', async () => {
    const t = taches.find((x) => x.id === b.dataset.decider);
    // la prise en charge fait passer la candidature « en cours d'évaluation »
    if (!t.priseEnChargePar && !(await action(b, () => Api.post(`/processus/taches/${t.id}/prise-en-charge`)))) return;
    const debut = new Date(Date.now() + 30 * 864e5).toISOString().slice(0, 10);
    const fin = new Date(Date.now() + 210 * 864e5).toISOString().slice(0, 10);
    const m = Modale.ouvrir(`
      <h2>Décision — ${esc(t.contexte.nomEtudiant)}</h2><p class="muted">${esc(t.contexte.titreOffre)}</p>
      <form id="f-decision">
        <label>Décision<select name="decision" id="s-decision">
          <option value="ACCEPTEE">Accepter la candidature</option><option value="REFUSEE">Refuser la candidature</option>
          <option value="COMPLEMENT">Demander un complément d'information</option></select></label>
        <label>Commentaire (transmis à l'étudiant)<textarea name="commentaire"></textarea></label>
        <div class="formulaire-grille" id="champs-acceptation">
          <label>Début du stage<input type="date" name="dateDebut" value="${debut}"></label>
          <label>Fin du stage<input type="date" name="dateFin" value="${fin}"></label>
          <label>Tuteur entreprise<input name="tuteur"></label>
        </div>
        <button class="btn primaire" type="submit">Valider la décision</button>
      </form>`);
    $('#s-decision', m).addEventListener('change', (e) => { $('#champs-acceptation', m).hidden = e.target.value !== 'ACCEPTEE'; });
    $('#f-decision', m).addEventListener('submit', (e) => {
      e.preventDefault();
      action(e.submitter, async () => {
        const r = await Api.post(`/processus/taches/${t.id}/decision`, formulaire(e.target));
        Modale.fermer();
        toast(r.statut === 'ACCEPTEE' ? `Candidature acceptée — dossier de stage n°${r.variables.stageId} créé`
          : r.statut === 'REFUSEE' ? 'Candidature refusée, étudiant notifié'
            : r.statut === 'INCIDENT_TECHNIQUE' ? 'Incident technique : le responsable a été alerté' : 'Demande de complément envoyée', r.statut === 'INCIDENT_TECHNIQUE' ? 'ko' : 'ok');
        afficher('evaluations');
      });
    });
  }));
};

VUES['offres-ent'] = async (racine, u) => {
  const offres = await Soap.appeler('entreprises', 'consulterOffresEntreprise', { entrepriseId: u.refId }, { liste: true });
  $('#actions-vue').innerHTML = '<button class="btn primaire" id="b-nouvelle">+ Nouvelle offre</button>';
  $('#b-nouvelle').addEventListener('click', () => formulaireOffre(Number(u.refId)));
  racine.innerHTML = tableauOffres(offres, true);
  lierActionsOffres(racine, offres);
};

function tableauOffres(offres, actions, noms) {
  return tableau([
    ['Offre', (o) => `<strong>${esc(o.titre)}</strong><div class="muted">${esc(o.lieu || '')} · ${o.dureeMois} mois</div>`],
    ...(noms ? [['Entreprise', (o) => esc(noms[o.entrepriseId] || o.entrepriseId)]] : []),
    ['Date limite', (o) => date(o.dateLimite)],
    ['Places', (o) => `${o.placesDisponibles} / ${o.nombrePlaces}`],
    ['Cible', (o) => `${esc(o.filieresCiblees || 'Toutes')}<div class="muted">niveau ≥ ${esc(o.niveauMinimum || '—')}</div>`],
    ['Statut', (o) => badge(o.statut)],
    ...(actions ? [['', (o) => (o.statut === 'ACTIVE' ? `<button class="btn" data-cloturer="${o.id}">Clôturer</button>` : '')]] : []),
  ], offres, { vide: 'Aucune offre publiée' });
}

function lierActionsOffres(racine, offres) {
  $$('[data-cloturer]', racine).forEach((b) => b.addEventListener('click', () => action(b, async () => {
    const o = { ...offres.find((x) => String(x.id) === b.dataset.cloturer), statut: 'CLOTUREE' };
    await Soap.appeler('offres', 'modifierOffre', { offre: o });
    toast('Offre clôturée', 'ok');
    afficher(vueCourante);
  })));
}

async function formulaireOffre(entrepriseId) {
  const ents = entrepriseId ? null : (await entreprises()).filter((e) => e.referencee);
  const limite = new Date(Date.now() + 30 * 864e5).toISOString().slice(0, 10);
  const m = Modale.ouvrir(`
    <h2>Publier une offre de stage</h2>
    <form id="f-offre" class="formulaire-grille">
      ${ents ? `<label style="grid-column:1/-1">Entreprise<select name="entrepriseId">${ents.map((e) => `<option value="${e.id}">${esc(e.raisonSociale)}</option>`).join('')}</select></label>` : ''}
      <label style="grid-column:1/-1">Titre<input name="titre" required maxlength="150"></label>
      <label style="grid-column:1/-1">Description<textarea name="description" required></textarea></label>
      <label style="grid-column:1/-1">Compétences requises (séparées par des virgules)<input name="competencesRequises" placeholder="Java, SQL, BPMN"></label>
      <label>Durée (mois)<input name="dureeMois" type="number" min="1" max="12" value="6" required></label>
      <label>Nombre de places<input name="nombrePlaces" type="number" min="1" value="1" required></label>
      <label>Date limite<input name="dateLimite" type="date" value="${limite}" required></label>
      <label>Lieu<input name="lieu"></label>
      <label>Filières ciblées<input name="filieresCiblees" placeholder="AMOA, Génie Logiciel"></label>
      <label>Niveau minimum<select name="niveauMinimum"><option value="">—</option>${['INE1', 'INE2', 'INE3'].map((n) => `<option>${n}</option>`).join('')}</select></label>
      <div><button class="btn primaire" type="submit">Publier (SOAP creerOffre)</button></div>
    </form>`);
  $('#f-offre', m).addEventListener('submit', (e) => {
    e.preventDefault();
    const f = formulaire(e.target);
    action(e.submitter, async () => {
      await Soap.appeler('offres', 'creerOffre', { offre: { entrepriseId: entrepriseId || f.entrepriseId, ...f } });
      Modale.fermer();
      toast('Offre publiée', 'ok');
      afficher(vueCourante);
    });
  });
}

VUES['candidatures-ent'] = async (racine) => {
  const cands = await Api.get('/candidatures');
  racine.innerHTML = tableauCandidatures(cands);
  lierClics(racine, cands, detailCandidature);
};

function tableauCandidatures(cands, avecEntreprise, noms) {
  return tableau([
    ['N°', (c) => c.id],
    ['Étudiant', (c) => `<strong>${esc(c.nomEtudiant)}</strong><div class="muted">${esc(c.matricule)}</div>`],
    ['Offre', (c) => esc(c.titreOffre)],
    ...(avecEntreprise ? [['Entreprise', (c) => esc(noms[c.entrepriseId] || '')]] : []),
    ['Déposée', (c) => date(c.dateDepot)],
    ['Adéquation', (c) => (c.scoreCompatibilite ?? '—') + ' %'],
    ['Statut', (c) => badge(c.statut)],
  ], cands, { vide: 'Aucune candidature', clic: true });
}

VUES.stagiaires = async (racine, u) => {
  const stages = await Api.get('/stages');
  racine.innerHTML = tableauStages(stages);
  lierClics(racine, stages, (s) => suiviStage(s.id, u));
};

function tableauStages(stages, avecEntreprise) {
  return tableau([
    ['Stagiaire', (s) => `<strong>${esc(s.nomEtudiant)}</strong><div class="muted">${esc(s.matricule)}</div>`],
    ['Sujet', (s) => esc(s.titreOffre)],
    ...(avecEntreprise ? [['Entreprise', (s) => esc(s.nomEntreprise)]] : []),
    ['Période', (s) => `${date(s.dateDebut)} → ${date(s.dateFin)}`],
    ['Tuteur', (s) => esc(s.tuteurEntreprise || '—')],
    ['Statut', (s) => badge(s.statut)],
  ], stages, { vide: 'Aucun stage', clic: true });
}

// ---------------------------------------------------------------- Responsable
VUES.tableau = async (racine) => {
  const [etus, offres, cands, stages, taches, instances] = await Promise.all([
    Soap.appeler('etudiants', 'listerEtudiants', {}, { liste: true }),
    Soap.appeler('offres', 'listerOffres', {}, { liste: true }),
    Api.get('/candidatures'), Api.get('/stages'), Api.get('/processus/taches'), Api.get('/processus/instances?limite=8'),
  ]);
  const compte = (l, f) => l.filter(f).length;
  racine.innerHTML = `
    <div class="kpis">
      <div class="kpi"><div class="valeur">${etus.length}</div><div class="libelle">Étudiants (${compte(etus, (e) => e.statut === 'EN_STAGE')} en stage)</div></div>
      <div class="kpi"><div class="valeur">${compte(offres, (o) => o.statut === 'ACTIVE')}</div><div class="libelle">Offres actives / ${offres.length}</div></div>
      <div class="kpi"><div class="valeur">${cands.length}</div><div class="libelle">Candidatures enregistrées</div></div>
      <div class="kpi"><div class="valeur">${compte(cands, (c) => c.statut === 'EN_COURS_EVALUATION')}</div><div class="libelle">En cours d'évaluation</div></div>
      <div class="kpi"><div class="valeur">${stages.length}</div><div class="libelle">Stages suivis</div></div>
      <div class="kpi"><div class="valeur" style="color:var(--danger)">${compte(taches, (t) => t.type === 'traiterIncident')}</div><div class="libelle">Incidents à traiter</div></div>
    </div>
    <h2>Dernières exécutions du processus</h2>
    ${tableauInstances(instances)}`;
  lierClics(racine, instances, (i) => detailInstance(i.processInstanceId));
};

function tableauInstances(instances) {
  return tableau([
    ['Instance', (i) => `#${esc(i.processInstanceId)}`],
    ['Étudiant', (i) => esc(i.variables.nomEtudiant || i.variables.matricule)],
    ['Offre', (i) => esc(i.variables.titreOffre || `n°${i.variables.offreId}`)],
    ['Démarrée', (i) => dateHeure(i.debut)],
    ['Activité(s) en cours', (i) => esc((i.activitesActives || []).filter((a) => !a.startsWith('timer')).join(', ') || '—')],
    ['Statut', (i) => badge(i.statut)],
  ], instances, { vide: 'Aucune exécution', clic: true });
}

VUES.processus = async (racine) => {
  const instances = await Api.get('/processus/instances?limite=100');
  racine.innerHTML = `<div class="carte" style="margin-bottom:16px"><h2>Modèle « Traitement d'une candidature »</h2>
    <p class="muted">Processus BPMN 2.0 exécuté par le moteur Flowable embarqué. Cliquez sur une instance pour suivre son exécution sur le diagramme.</p>
    <div class="bpmn" id="bpmn-modele"></div></div>
    <h2>Instances</h2>${tableauInstances(instances)}`;
  lierClics(racine, instances, (i) => detailInstance(i.processInstanceId));
  await dessinerBpmn($('#bpmn-modele'));
};

async function dessinerBpmn(conteneur, { actifs = [], passes = [] } = {}) {
  if (!window.BpmnJS) { conteneur.innerHTML = '<div class="vide">Visualiseur BPMN indisponible.</div>'; return; }
  const xml = await Api.xml('/processus/definition');
  const viewer = new window.BpmnJS({ container: conteneur });
  await viewer.importXML(xml);
  const canvas = viewer.get('canvas');
  const registre = viewer.get('elementRegistry');
  passes.forEach((id) => registre.get(id) && canvas.addMarker(id, 'passe'));
  actifs.forEach((id) => registre.get(id) && canvas.addMarker(id, 'actif'));
  canvas.zoom('fit-viewport');
}

async function detailInstance(pid) {
  const i = await Api.get(`/processus/instances/${pid}`);
  const v = i.variables;
  const m = Modale.ouvrir(`
    <h2>Instance #${esc(pid)} ${badge(i.statut)}</h2>
    <p class="muted">${esc(v.nomEtudiant || v.matricule)} → ${esc(v.titreOffre || '')} ${v.entreprise ? '(' + esc(v.entreprise) + ')' : ''}</p>
    ${i.raisonArret ? `<div class="encart attention">Processus interrompu : ${esc(i.raisonArret)}</div>` : ''}
    ${v.motifRejet && i.statut === 'REJETEE' ? `<div class="encart ko">Motif du rejet : ${esc(v.motifRejet)}</div>` : ''}
    ${v.messageErreur && i.statut === 'INCIDENT_TECHNIQUE' ? `<div class="encart ko">${esc(v.messageErreur)}</div>` : ''}
    ${v.incidents ? `<div class="encart attention"><strong>Incidents non bloquants :</strong><br>${esc(v.incidents).replace(/\n/g, '<br>')}</div>` : ''}
    <div class="bpmn" id="bpmn-instance"></div>
    <div class="deux-colonnes">
      <div><h3>Trace d'exécution</h3><ul class="chronologie">${i.trace.filter((t) => t.nom).map((t) => `
        <li class="${t.activite.startsWith('err') ? 'erreur-etape' : ''}"><strong>${esc(t.nom)}</strong> <span class="muted">${esc(t.type)}</span>
        <div class="quand">${dateHeure(t.debut)}${t.dureeMs !== undefined ? ` · ${t.dureeMs} ms` : ' · en cours'}</div></li>`).join('')}</ul></div>
      <div><h3>Variables</h3><table>${Object.entries(v).map(([k, val]) => `<tr><td class="muted">${esc(k)}</td><td>${esc(val)}</td></tr>`).join('')}</table></div>
    </div>`, { large: true });
  await dessinerBpmn($('#bpmn-instance', m), { actifs: i.activitesActives || [], passes: i.trace.map((t) => t.activite) });
}

VUES.incidents = async (racine) => {
  const taches = (await Api.get('/processus/taches')).filter((t) => t.type === 'traiterIncident');
  if (!taches.length) { racine.innerHTML = '<div class="vide">Aucun incident en attente. 🎉</div>'; return; }
  racine.innerHTML = `<div class="grille">${taches.map((t) => `<div class="carte">
    <h3>Incident — ${esc(t.contexte.nomEtudiant || t.contexte.matricule)}</h3>
    <div class="meta"><span>Instance #${esc(t.processInstanceId)}</span><span>Étape : <strong>${esc(t.contexte.etapeCourante)}</strong></span><span>${dateHeure(t.creeLe)}</span></div>
    <div class="encart ko">${esc(t.contexte.messageErreur)}</div>
    <div class="pied"><button class="btn" data-voir="${t.processInstanceId}">Voir l'instance</button>
      <div class="btn-groupe"><button class="btn danger" data-incident="${t.id}" data-action="ABANDONNER">Abandonner</button>
      <button class="btn succes" data-incident="${t.id}" data-action="RELANCER">Relancer l'étape</button></div></div></div>`).join('')}</div>`;
  $$('[data-voir]', racine).forEach((b) => b.addEventListener('click', () => detailInstance(b.dataset.voir)));
  $$('[data-incident]', racine).forEach((b) => b.addEventListener('click', () => action(b, async () => {
    const r = await Api.post(`/processus/taches/${b.dataset.incident}/incident`, { action: b.dataset.action });
    toast(`Instance #${r.processInstanceId} : ${LIBELLES[r.statut] || r.statut}`, r.statut === 'INCIDENT_TECHNIQUE' ? 'ko' : 'ok');
    afficher('incidents');
  })));
};

VUES.etudiants = async (racine) => {
  const etus = await Soap.appeler('etudiants', 'listerEtudiants', {}, { liste: true });
  $('#actions-vue').innerHTML = '<button class="btn primaire" id="b-ajout">+ Ajouter un étudiant</button>';
  $('#b-ajout').addEventListener('click', () => formulaireEtudiant());
  racine.innerHTML = tableau([
    ['Matricule', (e) => `<strong>${esc(e.matricule)}</strong>`],
    ['Nom', (e) => `${esc(e.prenom)} ${esc(e.nom)}<div class="muted">${esc(e.email)}</div>`],
    ['Filière', (e) => `${esc(e.filiere)} — ${esc(e.niveau)}`],
    ['Compétences', (e) => `<div class="tags">${competences(e.competences).map((k) => `<span class="tag">${esc(k)}</span>`).join('')}</div>`],
    ['Statut', (e) => `<select data-statut="${esc(e.matricule)}">${['ACTIF', 'SUSPENDU', 'EN_STAGE', 'DIPLOME'].map((s) => `<option value="${s}" ${s === e.statut ? 'selected' : ''}>${LIBELLES[s]}</option>`).join('')}</select>`],
  ], etus, { clic: true });
  lierClics(racine, etus, (e) => formulaireEtudiant(e));
  $$('[data-statut]', racine).forEach((s) => s.addEventListener('change', () => action(s, async () => {
    await Soap.appeler('etudiants', 'changerStatutEtudiant', { matricule: s.dataset.statut, statut: s.value });
    toast('Statut mis à jour', 'ok');
  })));
};

function formulaireEtudiant(e) {
  const m = Modale.ouvrir(`
    <h2>${e ? 'Modifier' : 'Ajouter'} un étudiant</h2>
    <form id="f-etu" class="formulaire-grille">
      <label>Matricule<input name="matricule" required value="${esc(e?.matricule || '')}" ${e ? 'readonly' : ''} placeholder="E2026010"></label>
      <label>E-mail<input name="email" type="email" required value="${esc(e?.email || '')}"></label>
      <label>Prénom<input name="prenom" required value="${esc(e?.prenom || '')}"></label>
      <label>Nom<input name="nom" required value="${esc(e?.nom || '')}"></label>
      <label>Filière<input name="filiere" required value="${esc(e?.filiere || '')}"></label>
      <label>Niveau<select name="niveau">${['INE1', 'INE2', 'INE3'].map((n) => `<option ${n === e?.niveau ? 'selected' : ''}>${n}</option>`).join('')}</select></label>
      <label>Téléphone<input name="telephone" value="${esc(e?.telephone || '')}"></label>
      <label>Adresse<input name="adresse" value="${esc(e?.adresse || '')}"></label>
      <label style="grid-column:1/-1">Compétences<input name="competences" value="${esc(e?.competences || '')}"></label>
      <div class="btn-groupe"><button class="btn primaire" type="submit">Enregistrer (SOAP)</button>
      ${e ? '<button class="btn danger" type="button" id="b-suppr">Supprimer</button>' : ''}</div>
    </form>`);
  $('#f-etu', m).addEventListener('submit', (ev) => {
    ev.preventDefault();
    const f = formulaire(ev.target);
    action(ev.submitter, async () => {
      await Soap.appeler('etudiants', e ? 'modifierEtudiant' : 'ajouterEtudiant', { etudiant: { ...f, statut: e?.statut, dateInscription: e?.dateInscription } });
      Modale.fermer(); toast('Étudiant enregistré', 'ok'); afficher('etudiants');
    });
  });
  $('#b-suppr', m)?.addEventListener('click', (ev) => {
    if (!confirm(`Supprimer l'étudiant ${e.matricule} ?`)) return;
    action(ev.target, async () => { await Soap.appeler('etudiants', 'supprimerEtudiant', { matricule: e.matricule }); Modale.fermer(); toast('Étudiant supprimé', 'ok'); afficher('etudiants'); });
  });
}

VUES.entreprises = async (racine) => {
  cacheEntreprises = null;
  const ents = await entreprises();
  $('#actions-vue').innerHTML = '<button class="btn primaire" id="b-ajout">+ Ajouter une entreprise</button>';
  $('#b-ajout').addEventListener('click', () => formulaireEntreprise());
  racine.innerHTML = tableau([
    ['Entreprise', (e) => `<strong>${esc(e.raisonSociale)}</strong><div class="muted">${esc(e.ville || '')}</div>`],
    ['Secteur', (e) => esc(e.secteurActivite)],
    ['Contact', (e) => `${esc(e.contactNom)}<div class="muted">${esc(e.contactEmail)}</div>`],
    ['Référencement', (e) => (e.referencee ? `<span class="tag ok">Référencée depuis ${date(e.dateReferencement)}</span>` : '<span class="tag ko">Non référencée</span>')],
  ], ents, { clic: true });
  lierClics(racine, ents, (e) => formulaireEntreprise(e));
};

async function formulaireEntreprise(e) {
  const offres = e ? await Soap.appeler('entreprises', 'consulterOffresEntreprise', { entrepriseId: e.id }, { liste: true }) : [];
  const champ = (n, l, req) => `<label>${l}<input name="${n}" ${req ? 'required' : ''} value="${esc(e?.[n] || '')}"></label>`;
  const m = Modale.ouvrir(`
    <h2>${e ? esc(e.raisonSociale) : 'Nouvelle entreprise partenaire'}</h2>
    <form id="f-ent" class="formulaire-grille">
      ${champ('raisonSociale', 'Raison sociale', true)}${champ('secteurActivite', "Secteur d'activité", true)}
      ${champ('email', 'E-mail', true)}${champ('telephone', 'Téléphone')}${champ('adresse', 'Adresse')}${champ('ville', 'Ville')}
      ${champ('contactNom', 'Responsable de contact', true)}${champ('contactFonction', 'Fonction')}
      ${champ('contactEmail', 'E-mail du contact', true)}${champ('contactTelephone', 'Téléphone du contact')}
      <label style="display:flex;gap:8px;align-items:center"><input type="checkbox" name="referencee" style="width:auto" ${e?.referencee ? 'checked' : ''}> Entreprise référencée</label>
      <div class="btn-groupe"><button class="btn primaire" type="submit">Enregistrer (SOAP)</button>
      ${e ? '<button class="btn danger" type="button" id="b-suppr">Supprimer</button>' : ''}</div>
    </form>
    ${e ? `<h3 style="margin-top:16px">Offres proposées (consulterOffresEntreprise)</h3>${tableauOffres(offres)}` : ''}`, { large: !!e });
  $('#f-ent', m).addEventListener('submit', (ev) => {
    ev.preventDefault();
    const f = formulaire(ev.target);
    f.referencee = ev.target.referencee.checked;
    action(ev.submitter, async () => {
      await Soap.appeler('entreprises', e ? 'modifierEntreprise' : 'ajouterEntreprise', { entreprise: { id: e?.id, ...f, dateReferencement: e?.dateReferencement } });
      Modale.fermer(); toast('Entreprise enregistrée', 'ok'); afficher('entreprises');
    });
  });
  $('#b-suppr', m)?.addEventListener('click', (ev) => {
    if (!confirm(`Supprimer ${e.raisonSociale} ?`)) return;
    action(ev.target, async () => { await Soap.appeler('entreprises', 'supprimerEntreprise', { entrepriseId: e.id }); Modale.fermer(); toast('Entreprise supprimée', 'ok'); afficher('entreprises'); });
  });
}

VUES['offres-admin'] = async (racine) => {
  const offres = await Soap.appeler('offres', 'listerOffres', {}, { liste: true });
  const noms = Object.fromEntries((await entreprises()).map((e) => [e.id, e.raisonSociale]));
  $('#actions-vue').innerHTML = '<button class="btn primaire" id="b-nouvelle">+ Nouvelle offre</button>';
  $('#b-nouvelle').addEventListener('click', () => formulaireOffre(null));
  racine.innerHTML = tableauOffres(offres, true, noms);
  lierActionsOffres(racine, offres);
};

VUES['candidatures-admin'] = async (racine) => {
  const cands = await Api.get('/candidatures');
  const noms = Object.fromEntries((await entreprises()).map((e) => [e.id, e.raisonSociale]));
  racine.innerHTML = tableauCandidatures(cands, true, noms);
  lierClics(racine, cands, detailCandidature);
};

VUES['stages-admin'] = async (racine, u) => {
  const stages = await Api.get('/stages');
  racine.innerHTML = tableauStages(stages, true);
  lierClics(racine, stages, (s) => suiviStage(s.id, u));
};

VUES.supervision = async (racine) => {
  const p = await Api.get('/admin/pannes');
  const types = { etudiants: 'SOAP', entreprises: 'SOAP', offres: 'SOAP', candidatures: 'REST', notifications: 'REST', stages: 'REST' };
  racine.innerHTML = `<div class="deux-colonnes">
    <div class="carte"><h2>Disponibilité des services</h2>
      <p class="muted">Simulez la panne d'un service (réponse HTTP 503) pour démontrer la politique de reprise et la gestion des erreurs du processus BPMN.</p>
      ${p.services.map((s) => `<div class="interrupteur"><div><strong>${s}</strong> <span class="tag">${types[s]}</span>
        <div class="muted">${types[s] === 'SOAP' ? `<a href="/ws/${s}?wsdl" target="_blank">/ws/${s}?wsdl</a>` : `/api/${s}`}</div></div>
        <div class="btn-groupe">${p.servicesEnPanne.includes(s) ? '<span class="badge b-REFUSEE">En panne</span>' : '<span class="badge b-ACCEPTEE">Opérationnel</span>'}
        <button class="btn" data-panne="${s}" data-etat="${!p.servicesEnPanne.includes(s)}">${p.servicesEnPanne.includes(s) ? 'Rétablir' : 'Simuler une panne'}</button></div></div>`).join('')}
    </div>
    <div class="carte"><h2>Boîte e-mail de test</h2>
      <p class="muted">Les notifications sont envoyées par SMTP à <strong>Mailpit</strong>, une boîte de réception locale :
      aucun e-mail ne sort de la machine.</p>
      <a class="btn" href="http://localhost:8025" target="_blank" rel="noopener">Ouvrir Mailpit (localhost:8025)</a>
      <h2 style="margin-top:22px">Rappels des dates limites</h2>
      <p class="muted">Le service Notifications interroge le service SOAP Offres (offres clôturant bientôt) et le service Étudiants (profils actifs compatibles), puis envoie les rappels. Une campagne automatique s'exécute toutes les 24 h.</p>
      <label>Jours avant la date limite<input type="number" id="jours" value="3" min="0" max="60"></label>
      <button class="btn primaire" id="b-rappels">Lancer la campagne</button><div id="res-rappels"></div>
    </div></div>`;
  $$('[data-panne]', racine).forEach((b) => b.addEventListener('click', () => action(b, async () => {
    await Api.post('/admin/pannes', { service: b.dataset.panne, panne: b.dataset.etat === 'true' });
    afficher('supervision');
  })));
  $('#b-rappels').addEventListener('click', (e) => action(e.target, async () => {
    const r = await Api.post('/notifications/rappels', { joursAvant: Number($('#jours').value) });
    $('#res-rappels').innerHTML = `<div class="encart ok">${r.offresConcernees} offre(s) concernée(s), ${r.rappelsEnvoyes} rappel(s) envoyé(s).</div>`;
  }));
};

// ======================================================================
// Démarrage
// ======================================================================
document.addEventListener('DOMContentLoaded', () => {
  $('#form-connexion').addEventListener('submit', async (e) => {
    e.preventDefault();
    const f = formulaire(e.target);
    $('#erreur-connexion').textContent = '';
    try {
      await Auth.connecter(f.username, f.password);
      demarrerApplication();
    } catch (err) {
      $('#erreur-connexion').textContent = err.status === 401 ? 'Identifiant ou mot de passe incorrect' : err.message;
    }
  });
  $$('.comptes-demo button').forEach((b) => b.addEventListener('click', () => {
    $('#form-connexion').username.value = b.dataset.u;
    $('#form-connexion').password.value = b.dataset.p;
    $('#form-connexion').requestSubmit();
  }));
  document.addEventListener('click', (e) => { if (!e.target.closest('#profil')) $('#profil').removeAttribute('open'); });
  $('#btn-cle').addEventListener('click', async () => {
    $('#profil').removeAttribute('open');
    if (!confirm('Générer une nouvelle clé d\'API ? L\'ancienne sera révoquée.')) return;
    try {
      const r = await Api.post('/moi/cle-api');
      Modale.ouvrir(`<h2>Votre clé d'API</h2>
        <p class="muted">À transmettre dans l'en-tête <code>X-API-Key</code> des appels REST (Postman, scripts).
        Elle ne sera plus affichée : conservez-la.</p>
        <pre style="background:#f4f6fb;padding:12px;border-radius:8px;user-select:all;white-space:pre-wrap;word-break:break-all">${esc(r.cleApi)}</pre>`);
    } catch (e) { toast(e.message, 'ko'); }
  });
  $('#btn-deconnexion').addEventListener('click', () => {
    Auth.deconnecter();
    try { sessionStorage.removeItem('vue'); } catch (e) { /* ignoré */ }
    location.reload();
  });
  $('#modale-fermer').addEventListener('click', Modale.fermer);
  $('#modale').addEventListener('click', (e) => { if (e.target.id === 'modale') Modale.fermer(); });
  if (Auth.utilisateur()) demarrerApplication();
});
