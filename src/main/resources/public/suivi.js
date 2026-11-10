/*
 * Suivi d'une candidature en langage simple, pour l'étudiant et l'entreprise.
 * Le diagramme BPMN (avec l'étape en cours surlignée) est réservé au responsable des stages ;
 * les autres rôles voient une chronologie en 5 étapes, calculée à partir de la trace du processus.
 *
 * SUIVI_CANDIDATURE est l'unique table de correspondance entre le modèle BPMN
 * (candidature-stage.bpmn20.xml) et les étapes affichées : à mettre à jour si le modèle évolue.
 */
const SUIVI_CANDIDATURE = {
  etapes: [
    { cle: 'DEPOT', libelle: 'Candidature déposée' },
    { cle: 'VERIFICATION', libelle: 'Vérification du dossier' },
    { cle: 'TRANSMISSION', libelle: "Transmise à l'entreprise" },
    { cle: 'DECISION', libelle: "Décision de l'entreprise" },
    { cle: 'STAGE', libelle: 'Stage confirmé' },
  ],

  // identifiant d'activité BPMN -> étape simplifiée ; une activité absente (journalisation, incident,
  // événements d'erreur…) est purement technique et n'apparaît pas dans la chronologie
  activites: {
    debut: 'DEPOT', receptionner: 'DEPOT',
    spVerifications: 'VERIFICATION', verifierEtudiant: 'VERIFICATION', gwEligible: 'VERIFICATION',
    verifierOffre: 'VERIFICATION', controler: 'VERIFICATION', gwRecevable: 'VERIFICATION', notifierRejet: 'VERIFICATION',
    spEnregistrement: 'TRANSMISSION', enregistrer: 'TRANSMISSION', notifierConfirmation: 'TRANSMISSION', notifierEntreprise: 'TRANSMISSION',
    evaluer: 'DECISION', relancerEntreprise: 'DECISION', gwDecision: 'DECISION', notifierComplement: 'DECISION',
    spFinalisation: 'DECISION', majDecision: 'DECISION', notifierResultat: 'DECISION', gwAcceptee: 'DECISION',
    creerStage: 'STAGE', majEtudiant: 'STAGE', finAcceptee: 'STAGE',
  },

  // statut de la candidature (service REST) -> étape atteinte ; utilisé quand la trace du processus
  // n'est pas consultable (ancienne candidature, service momentanément indisponible)
  statuts: { DEPOSEE: 'DECISION', EN_COURS_EVALUATION: 'DECISION', ACCEPTEE: 'STAGE', REFUSEE: 'DECISION', ANNULEE: 'TRANSMISSION' },

  // issues particulières : libellé de l'étape finale qui remplace la suite du parcours
  issues: {
    REFUSEE: { libelle: 'Candidature non retenue', apres: 'DECISION' },
    ANNULEE: { libelle: 'Candidature annulée', apres: 'TRANSMISSION' },
    REJETEE: { libelle: 'Dossier non recevable', apres: 'VERIFICATION' },
    ABANDONNEE: { libelle: 'Traitement interrompu', apres: null },
  },
  complement: "Complément d'information demandé",
};

const ETATS_ETAPE = { terminee: 'Terminée', 'en-cours': 'En cours', 'a-venir': 'À venir', issue: '' };

/**
 * Calcule les étapes à afficher.
 * @param c        candidature (service REST Candidatures)
 * @param hist     historique des changements de statut de la candidature
 * @param instance état du processus (GET /processus/instances/{id}) ou null
 * @param role     'ETUDIANT' | 'ENTREPRISE'
 */
function calculerSuivi(c, hist, instance, role) {
  const S = SUIVI_CANDIDATURE;
  const ordre = S.etapes.map((e) => e.cle);
  const rang = (cle) => ordre.indexOf(cle);
  const dateStatut = (statut) => (hist.filter((h) => h.nouveauStatut === statut).pop() || {}).date;

  // 1. dates de début / fin de chaque étape d'après la trace du processus
  const dates = {};
  let atteinte = rang(S.statuts[c.statut] || 'DEPOT');
  if (instance) {
    atteinte = 0;
    for (const t of instance.trace || []) {
      const cle = S.activites[t.activite];
      if (!cle) continue;
      const d = (dates[cle] = dates[cle] || {});
      if (t.debut && (!d.debut || t.debut < d.debut)) d.debut = t.debut;
      if (t.fin && (!d.fin || t.fin > d.fin)) d.fin = t.fin;
      atteinte = Math.max(atteinte, rang(cle));
    }
  }
  const processusTermine = !instance || ['ACCEPTEE', 'REFUSEE', 'REJETEE', 'ABANDONNEE', 'INTERROMPU'].includes(instance.statut);

  // 2. issue particulière (refus, annulation, rejet, abandon) ou parcours normal
  let issue = null;
  if (c.statut === 'ANNULEE') issue = 'ANNULEE';
  else if (c.statut === 'REFUSEE') issue = 'REFUSEE';
  else if (instance && instance.statut === 'REJETEE') issue = 'REJETEE';
  else if (instance && instance.statut === 'ABANDONNEE') issue = 'ABANDONNEE';
  if (c.statut === 'ACCEPTEE' && processusTermine) atteinte = ordre.length; // tout est terminé

  const vars = (instance && instance.variables) || {};
  const etapes = S.etapes.map((e, i) => {
    let etat = i < atteinte ? 'terminee' : i === atteinte ? 'en-cours' : 'a-venir';
    if (i === atteinte && processusTermine && issue) etat = 'terminee';
    const d = dates[e.cle] || {};
    return { ...e, etat, date: etat === 'terminee' ? (d.fin || d.debut) : etat === 'en-cours' ? d.debut : null, note: null };
  });

  // dates connues par le service Candidatures (plus fiables pour le dépôt et la décision)
  etapes[0].date = c.dateDepot || etapes[0].date;
  if (!instance) etapes.slice(1, 3).forEach((e) => { if (e.etat === 'terminee') e.date = c.dateDepot; });
  const decision = etapes[rang('DECISION')];
  if (c.statut === 'ACCEPTEE' || c.statut === 'REFUSEE') decision.date = dateStatut(c.statut) || decision.date;
  if (decision.etat === 'en-cours') {
    decision.date = dateStatut('EN_COURS_EVALUATION') || decision.date;
    decision.note = c.statut === 'EN_COURS_EVALUATION' ? "Le dossier est en cours d'examen par l'entreprise." : "Le dossier attend d'être examiné par l'entreprise.";
    if (Number(vars.nombreDemandesComplement) > 0) {
      decision.libelle = S.complement;
      decision.note = role === 'ETUDIANT'
        ? `L'entreprise souhaite des informations complémentaires${vars.commentaireDecision ? ` : « ${vars.commentaireDecision} »` : ''}. Complétez votre dossier ci-dessous.`
        : `Complément demandé à l'étudiant${vars.commentaireDecision ? ` : « ${vars.commentaireDecision} »` : ''}.`;
    }
  }
  if (c.statut === 'ACCEPTEE') etapes[rang('STAGE')].date = etapes[rang('STAGE')].date || dateStatut('ACCEPTEE');

  // 3. retard dû à un incident technique : message neutre, jamais le détail
  if (instance && instance.statut === 'INCIDENT_TECHNIQUE') {
    const courante = etapes.find((e) => e.etat === 'en-cours');
    if (courante) {
      courante.note = role === 'ETUDIANT'
        ? 'Son traitement prend un peu plus de temps que prévu, vous serez notifié(e) par e-mail.'
        : 'Le traitement prend un peu plus de temps que prévu.';
    }
  }

  // 4. remplacement de la fin du parcours par l'étape finale adaptée
  if (issue) {
    const def = S.issues[issue];
    const dernier = def.apres ? rang(def.apres) : Math.min(atteinte, ordre.length - 1) - 1;
    const final = {
      cle: issue, libelle: def.libelle, etat: 'issue',
      date: issue === 'ANNULEE' ? (dateStatut('ANNULEE') || c.dateMiseAJour) : issue === 'REFUSEE' ? dateStatut('REFUSEE') : (instance && instance.fin),
      note: issue === 'REFUSEE' ? (c.motif ? `Commentaire de l'entreprise : « ${c.motif} »` : null)
        : issue === 'REJETEE' ? (vars.motifRejet || null)
          : issue === 'ABANDONNEE' ? (role === 'ETUDIANT' ? 'Le traitement n\'a pas pu aboutir. Vous pouvez déposer une nouvelle candidature.' : 'Le traitement de ce dossier a été interrompu.')
            : null,
    };
    return [...etapes.slice(0, dernier + 1).map((e) => ({ ...e, etat: 'terminee', note: e.cle === 'DECISION' ? null : e.note })), final];
  }
  return etapes;
}

/** Rendu HTML de la chronologie (stepper vertical). */
function rendreSuivi(etapes) {
  return `<ol class="etapes">${etapes.map((e, i) => `
    <li class="etape ${e.etat}">
      <span class="numero-etape">${i + 1}</span>
      <div>
        <div class="titre-etape">${esc(e.libelle)}</div>
        <div class="etat-etape">${[ETATS_ETAPE[e.etat], e.date ? (e.etat === 'en-cours' ? `depuis le ${date(e.date)}` : date(e.date)) : ''].filter(Boolean).join(' · ')}</div>
        ${e.note ? `<div class="note-etape">${esc(e.note)}</div>` : ''}
      </div>
    </li>`).join('')}</ol>`;
}
