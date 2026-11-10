/*
 * Gestion centralisée des erreurs de l'interface.
 *
 * - ErreurApplication : erreur structurée produite par les clients REST (app.js) et SOAP (soap.js) :
 *   statut HTTP, code applicatif (DONNEES_INVALIDES, REGLE_METIER…), service appelé, détails, message d'origine.
 * - getMessageUtilisateur(erreur, role, contexte) : SEUL endroit qui décide du texte montré à l'utilisateur.
 *   L'étudiant et l'entreprise ne voient jamais de détail technique (service, endpoint, code HTTP, faute SOAP,
 *   nombre de tentatives) : ce détail part dans console.error et, pour les incidents du processus, dans la vue
 *   « Incidents » du responsable des stages.
 */
class ErreurApplication extends Error {
  constructor({ statut = 0, code = null, service = null, details = [], message = '' } = {}) {
    super(message || code || `HTTP ${statut}`);
    this.name = 'ErreurApplication';
    this.statut = statut;
    this.code = code;
    this.service = service;
    this.details = [].concat(details || []);
  }
}

/** Codes des fautes SOAP (ServiceFault) ramenés à l'équivalent HTTP, pour un traitement unique. */
const STATUT_DES_CODES_SOAP = {
  RESSOURCE_INTROUVABLE: 404, DONNEES_INVALIDES: 400, CONFLIT: 409, ACCES_REFUSE: 403,
  REGLE_METIER: 422, ERREUR_TECHNIQUE: 500, FailedAuthentication: 401,
};

/** Règles de gestion expliquées en clair (reconnues dans le message renvoyé par les services). */
const REGLES_METIER = [
  { motif: /date limite/i, message: 'La date limite de candidature pour cette offre est dépassée.' },
  { motif: /déjà candidaté|déjà été déposée|déjà déposé/i, message: 'Vous avez déjà déposé une candidature pour cette offre.' },
  { motif: /aucune place|plus de place|pourvue/i, message: "Il n'y a plus de place disponible sur cette offre." },
  { motif: /nombre maximal|candidatures en cours/i,
    message: 'Vous avez atteint la limite de 5 candidatures en cours. Attendez une réponse ou annulez une candidature encore « déposée ».' },
  { motif: /réservée aux étudiants/i, message: "Ce type de stage n'est pas ouvert à votre année d'études." },
  { motif: /fili[eè]re|niveau|non cibl|incompatible/i, message: 'Votre profil (filière ou niveau) ne correspond pas aux critères de cette offre.' },
  { motif: /annulation impossible/i, message: "Cette candidature ne peut plus être annulée : l'entreprise a déjà commencé à l'examiner." },
  { motif: /ne peut plus être modifiée/i, message: 'Cette candidature ne peut plus être modifiée.' },
  { motif: /déjà traitée|déjà été enregistrée|existe déjà/i, message: 'Cette opération a déjà été effectuée. Actualisez la page.' },
  { motif: /suspendu|ne permet pas/i, message: 'Votre dossier ne permet pas cette action pour le moment. Contactez le bureau des stages.' },
];

/** Noms des champs des formulaires, en français simple. */
const LIBELLES_CHAMPS = {
  matricule: 'Matricule', nom: 'Nom', prenom: 'Prénom', email: 'E-mail', telephone: 'Téléphone', adresse: 'Adresse',
  filiere: 'Filière', niveau: 'Niveau', competences: 'Compétences', lettreMotivation: 'Lettre de motivation',
  cvUrl: 'Lien vers le CV', titre: 'Titre', description: 'Description', competencesRequises: 'Compétences requises',
  dureeMois: 'Durée', nombrePlaces: 'Nombre de places', dateLimite: 'Date limite', lieu: 'Lieu',
  filieresCiblees: 'Filières ciblées', typeStage: 'Type de stage', dateDebut: 'Début du stage', dateFin: 'Fin du stage',
  tuteur: 'Tuteur', commentaire: 'Commentaire', decision: 'Décision', note: 'Note', evaluateur: 'Évaluateur',
  appreciation: 'Appréciation', periode: 'Période', contenu: 'Contenu', raisonSociale: 'Raison sociale',
  secteurActivite: "Secteur d'activité", contactNom: 'Responsable de contact', contactEmail: 'E-mail du contact',
  contactTelephone: 'Téléphone du contact', contactFonction: 'Fonction du contact', ville: 'Ville', offreId: 'Offre', statut: 'Statut',
};

const RAISONS_SAISIE = [
  [/obligatoire/i, 'à renseigner'], [/dépasse/i, 'texte trop long'], [/invalide|format/i, 'format incorrect'],
  [/compris entre/i, 'valeur hors limites'], [/doit valoir|attendu/i, 'valeur non autorisée'],
  [/date future/i, 'doit être une date à venir'], [/positif/i, 'doit être supérieur à zéro'],
  [/postérieure/i, 'doit être après la date de début'], [/numérique/i, 'doit être un nombre'],
];

function champsAcorriger(details) {
  return details.map((d) => {
    const texte = String(d);
    let champ = (texte.match(/'([^']+)'/) || texte.match(/^(\w+) doit/) || [])[1];
    if (!champ) champ = Object.keys(LIBELLES_CHAMPS).find((k) => new RegExp(`\\b${LIBELLES_CHAMPS[k]}\\b|\\b${k}\\b`, 'i').test(texte));
    if (!champ && /date de fin/i.test(texte)) champ = 'dateFin';
    const libelle = LIBELLES_CHAMPS[champ];
    if (!libelle) return null;
    const raison = (RAISONS_SAISIE.find(([re]) => re.test(texte)) || [])[1];
    return raison ? `${libelle} (${raison})` : libelle;
  }).filter(Boolean);
}

/** Classe une erreur : session, acces, saisie, metier, panne ou inconnue. */
function typeErreur(erreur) {
  const statut = erreur && erreur.statut;
  const code = erreur && erreur.code;
  if (statut === 401) return 'session';
  if (statut === 403 || code === 'ACCES_REFUSE') return 'acces';
  if (statut === 0 || statut === 502 || statut === 503 || statut === 504 || code === 'SERVICE_INDISPONIBLE' || code === 'RESEAU') return 'panne';
  if (statut === 400 || code === 'DONNEES_INVALIDES') return 'saisie';
  if ([404, 409, 422].includes(statut) || ['REGLE_METIER', 'CONFLIT', 'RESSOURCE_INTROUVABLE'].includes(code)) return 'metier';
  return 'inconnue';
}

/** Détail technique complet, pour le débogage uniquement (jamais affiché à l'étudiant ni à l'entreprise). */
function journaliserErreur(erreur) {
  if (!erreur || erreur.journalisee) return;
  erreur.journalisee = true;
  console.error('[Plateforme des stages] détail technique', {
    statut: erreur.statut, code: erreur.code, service: erreur.service, details: erreur.details, message: erreur.message, erreur,
  });
}

/**
 * Traduit une erreur en message compréhensible, adapté au rôle.
 * @param erreur   ErreurApplication (ou toute erreur)
 * @param role     'ETUDIANT' | 'ENTREPRISE' | 'RESPONSABLE' | undefined (avant connexion)
 * @param contexte 'action' (défaut) | 'chargement' | 'candidature' | 'connexion'
 */
function getMessageUtilisateur(erreur, role, contexte = 'action') {
  journaliserErreur(erreur);
  const type = typeErreur(erreur);

  if (type === 'session') {
    return contexte === 'connexion' ? 'Identifiant ou mot de passe incorrect.' : 'Votre session a expiré. Veuillez vous reconnecter.';
  }
  if (type === 'acces') return "Vous n'avez pas accès à cette page.";

  if (type === 'panne') {
    if (!role || contexte === 'connexion') return 'La plateforme est momentanément indisponible. Veuillez réessayer dans quelques minutes.';
    if (role === 'RESPONSABLE') return 'Un service de la plateforme est momentanément indisponible. Consultez les vues Supervision et Incidents.';
    if (role === 'ETUDIANT' && contexte === 'candidature') {
      return 'Votre candidature a bien été reçue. Son traitement prend un peu plus de temps que prévu, vous serez notifié(e) par e-mail.';
    }
    if (contexte === 'chargement') return 'Ces informations sont momentanément indisponibles. Veuillez réessayer dans quelques minutes.';
    return 'Action impossible pour le moment, veuillez réessayer dans quelques minutes.';
  }

  if (type === 'saisie') {
    const champs = champsAcorriger(erreur.details || []);
    return champs.length ? `Certaines informations sont à corriger : ${champs.join(', ')}.`
      : 'Certaines informations saisies sont incorrectes. Vérifiez le formulaire.';
  }

  if (type === 'metier') {
    const texte = [erreur.message, ...(erreur.details || [])].join(' ');
    const regle = REGLES_METIER.find((r) => r.motif.test(texte));
    if (regle) return regle.message;
    if (erreur.statut === 404 || erreur.code === 'RESSOURCE_INTROUVABLE') return "L'élément demandé n'existe plus. Actualisez la page.";
    return "Cette action n'est pas possible dans l'état actuel du dossier.";
  }

  return 'Une erreur inattendue est survenue. Veuillez réessayer ; si le problème persiste, contactez le bureau des stages.';
}
