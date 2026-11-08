#!/usr/bin/env python3
"""Génère le processus BPMN 2.0 (sémantique + diagramme BPMN-DI).

Usage : python3 tools/generer_bpmn.py > src/main/resources/processes/candidature-stage.bpmn20.xml
"""
from xml.sax.saxutils import escape

D = "com.universite.stages.bpmn.delegates."

# ---------------------------------------------------------------- dimensions
TASK = (130, 80)
EVENT = (36, 36)
GATEWAY = (50, 50)

# ---------------------------------------------------------------- couloirs
POOL_X, POOL_Y = 40, 40
LANE_HDR = 30
LANES = [  # (id, nom, hauteur)
    ("laneEtudiant", "Étudiant", 140),
    ("laneOrchestrateur", "Orchestrateur SOA (services SOAP / REST)", 400),
    ("laneEntreprise", "Entreprise partenaire", 230),
    ("laneResponsable", "Responsable des stages", 200),
]
POOL_W = 3260

lane_y = {}
y = POOL_Y
for lid, _, h in LANES:
    lane_y[lid] = (y, h)
    y += h
POOL_H = y - POOL_Y


def ly(lane, off):
    return lane_y[lane][0] + off


nodes = {}       # id -> dict(kind, name, x, y, w, h, extra, parent, lane)
order = []       # ordre de déclaration
flows = []       # (id, src, tgt, name, cond, route, parent)


def node(nid, kind, name, x, y, size, lane=None, parent=None, **extra):
    w, h = size
    nodes[nid] = dict(kind=kind, name=name, x=x, y=y, w=w, h=h, lane=lane, parent=parent, extra=extra)
    order.append(nid)


def task(nid, name, cls, x, y, lane=None, parent=None, fields=None, doc=None):
    node(nid, "serviceTask", name, x, y, TASK, lane, parent, cls=cls, fields=fields or [], doc=doc)


def flow(fid, s, t, name=None, cond=None, route=None, parent=None):
    flows.append((fid, s, t, name, cond, route, parent))


# ================================================================ modèle
O = "laneOrchestrateur"
E = "laneEntreprise"
R = "laneResponsable"
S = "laneEtudiant"
mid_o = ly(O, 200)  # axe principal de l'orchestrateur

node("debut", "startEvent", "Candidature soumise", 110, ly(S, 70) - 18, EVENT, S)
task("receptionner", "1. Réceptionner la candidature", D + "ReceptionnerCandidatureDelegate", 170, mid_o - 40, O,
     doc="Contrôle de forme des données saisies et initialisation du contexte du processus.")

# --- Sous-processus A : vérifications
A = dict(x=350, y=ly(O, 40), w=870, h=300)
node("spVerifications", "subProcess", "Vérifications de recevabilité", A["x"], A["y"], (A["w"], A["h"]), O,
     etape="VERIFICATIONS")
node("debutA", "startEvent", "", 380, mid_o - 18, EVENT, parent="spVerifications")
task("verifierEtudiant", "2. Vérifier l'étudiant [SOAP Étudiants]", D + "VerifierEtudiantDelegate", 450, mid_o - 40,
     parent="spVerifications", doc="verifierEtudiant + consulterEtudiant")
node("gwEligible", "exclusiveGateway", "Étudiant éligible ?", 630, mid_o - 25, GATEWAY, parent="spVerifications",
     default="fNonEligible")
task("verifierOffre", "3. Vérifier l'offre et l'entreprise [SOAP Offres / Entreprises]", D + "VerifierOffreDelegate",
     730, mid_o - 40, parent="spVerifications",
     doc="consulterOffre, verifierEntreprise, consulterEntreprise, verifierCompatibilite")
task("controler", "4. Contrôler la candidature [SOAP Offres / REST Candidatures]", D + "ControlerCandidatureDelegate",
     910, mid_o - 40, parent="spVerifications", doc="Date limite, places disponibles, absence de doublon")
node("finA", "endEvent", "Vérifications terminées", 1120, mid_o - 18, EVENT, parent="spVerifications")
node("finNonEligible", "endEvent", "Étudiant non éligible", 637, ly(O, 280) - 18, EVENT, parent="spVerifications")

flow("fA1", "debutA", "verifierEtudiant", parent="spVerifications")
flow("fA2", "verifierEtudiant", "gwEligible", parent="spVerifications")
flow("fEligible", "gwEligible", "verifierOffre", "oui", "${etudiantEligible}", parent="spVerifications")
flow("fNonEligible", "gwEligible", "finNonEligible", "non", route="down", parent="spVerifications")
flow("fA3", "verifierOffre", "controler", parent="spVerifications")
flow("fA4", "controler", "finA", parent="spVerifications")

node("errA", "boundaryError", "Service indisponible", A["x"] + A["w"] - 60, A["y"] + A["h"] - 18, EVENT, O,
     attached="spVerifications")

# --- Passerelle de décision
node("gwRecevable", "exclusiveGateway", "5. Conditions satisfaites ?", 1270, mid_o - 25, GATEWAY, O,
     default="fNonRecevable")
task("notifierRejet", "Notifier le rejet à l'étudiant [REST Notifications]", D + "NotifierDelegate", 1230,
     ly(S, 30), S, fields=[("type", "REJET_CANDIDATURE"), ("destinataire", "ETUDIANT")])
node("finRejet", "endEvent", "Candidature rejetée", 1420, ly(S, 70) - 18, EVENT, S)

# --- Sous-processus B : enregistrement et transmission
B = dict(x=1360, y=ly(O, 40), w=640, h=300)
node("spEnregistrement", "subProcess", "Enregistrement et transmission", B["x"], B["y"], (B["w"], B["h"]), O,
     etape="ENREGISTREMENT")
node("debutB", "startEvent", "", 1385, mid_o - 18, EVENT, parent="spEnregistrement")
task("enregistrer", "Enregistrer la candidature [REST Candidatures]", D + "EnregistrerCandidatureDelegate",
     1450, mid_o - 40, parent="spEnregistrement", doc="POST /api/candidatures")
task("notifierConfirmation", "Confirmer le dépôt à l'étudiant [REST Notifications]", D + "NotifierDelegate",
     1620, mid_o - 40, parent="spEnregistrement",
     fields=[("type", "CONFIRMATION_DEPOT"), ("destinataire", "ETUDIANT")])
task("notifierEntreprise", "6. Transmettre à l'entreprise [REST Notifications]", D + "NotifierDelegate",
     1790, mid_o - 40, parent="spEnregistrement",
     fields=[("type", "RECEPTION_CANDIDATURE"), ("destinataire", "ENTREPRISE")])
node("finB", "endEvent", "", 1950, mid_o - 18, EVENT, parent="spEnregistrement")
flow("fB1", "debutB", "enregistrer", parent="spEnregistrement")
flow("fB2", "enregistrer", "notifierConfirmation", parent="spEnregistrement")
flow("fB3", "notifierConfirmation", "notifierEntreprise", parent="spEnregistrement")
flow("fB4", "notifierEntreprise", "finB", parent="spEnregistrement")
node("errB", "boundaryError", "Service indisponible", B["x"] + B["w"] - 60, B["y"] + B["h"] - 18, EVENT, O,
     attached="spEnregistrement")

# --- Décision de l'entreprise
mid_e = ly(E, 95)
node("evaluer", "userTask", "7. Examiner et évaluer la candidature", 2200, mid_e - 40, TASK, E, group="entreprise")
node("timerRelance", "boundaryTimer", "Délai de décision dépassé", 2250, mid_e + 22, EVENT, E,
     attached="evaluer", duration="${delaiRelance}")
task("relancerEntreprise", "Relancer l'entreprise [REST Notifications]", D + "NotifierDelegate", 2330, ly(E, 140), E,
     fields=[("type", "RELANCE_DECISION"), ("destinataire", "ENTREPRISE")])
node("finRelance", "endEvent", "Relance envoyée", 2520, ly(E, 180) - 18, EVENT, E)
node("gwDecision", "exclusiveGateway", "Décision ?", 2400, mid_e - 25, GATEWAY, E, default="fDecisionFinale")
task("notifierComplement", "Demander un complément [REST Notifications]", D + "NotifierDelegate", 2190,
     ly(S, 30), S, fields=[("type", "DEMANDE_COMPLEMENT"), ("destinataire", "ETUDIANT")])

# --- Sous-processus C : finalisation
C = dict(x=2500, y=ly(O, 40), w=700, h=300)
node("spFinalisation", "subProcess", "Finalisation de la décision", C["x"], C["y"], (C["w"], C["h"]), O,
     etape="FINALISATION")
node("debutC", "startEvent", "", 2520, mid_o - 18, EVENT, parent="spFinalisation")
task("majDecision", "Enregistrer la décision [REST Candidatures]", D + "MajStatutCandidatureDelegate", 2580,
     mid_o - 40, parent="spFinalisation", fields=[("statut", "${decision}")])
task("notifierResultat", "8. Notifier le résultat à l'étudiant [REST Notifications]", D + "NotifierDelegate",
     2740, mid_o - 40, parent="spFinalisation",
     fields=[("type", "${decision == 'ACCEPTEE' ? 'ACCEPTATION' : 'REFUS'}"), ("destinataire", "ETUDIANT")])
node("gwAcceptee", "exclusiveGateway", "Acceptée ?", 2905, mid_o - 25, GATEWAY, parent="spFinalisation",
     default="fRefusee")
task("creerStage", "9. Créer le dossier de stage [REST Suivi des stages]", D + "CreerDossierStageDelegate", 2990,
     ly(O, 60), parent="spFinalisation", doc="POST /api/stages")
task("majEtudiant", "Statut étudiant EN_STAGE + réservation de place [SOAP]", D + "MajEtudiantEtOffreDelegate",
     2990, ly(O, 200), parent="spFinalisation", doc="changerStatutEtudiant + reserverPlace")
node("finAcceptee", "endEvent", "Stage créé", 3150, ly(O, 240) - 18, EVENT, parent="spFinalisation")
node("finRefusee", "endEvent", "Refus notifié", 2912, ly(O, 300) - 18, EVENT, parent="spFinalisation")
flow("fC1", "debutC", "majDecision", parent="spFinalisation")
flow("fC2", "majDecision", "notifierResultat", parent="spFinalisation")
flow("fC3", "notifierResultat", "gwAcceptee", parent="spFinalisation")
flow("fAcceptee", "gwAcceptee", "creerStage", "oui", "${decision == 'ACCEPTEE'}", route="up",
     parent="spFinalisation")
flow("fRefusee", "gwAcceptee", "finRefusee", "non", route="down", parent="spFinalisation")
flow("fC4", "creerStage", "majEtudiant", route="vertical", parent="spFinalisation")
flow("fC5", "majEtudiant", "finAcceptee", parent="spFinalisation")
node("errC", "boundaryError", "Service indisponible", C["x"] + C["w"] - 60, C["y"] + C["h"] - 18, EVENT, O,
     attached="spFinalisation")
node("finTraitee", "endEvent", "Candidature traitée", 3080, ly(S, 70) - 18, EVENT, S)

# --- Gestion des incidents (responsable)
mid_r = ly(R, 110)
task("journaliser", "Journaliser l'incident technique", D + "JournaliserIncidentDelegate", 1150, mid_r - 40, R)
node("traiterIncident", "userTask", "Traiter l'incident (relancer / abandonner)", 1350, mid_r - 40, TASK, R,
     group="responsable")
node("gwIncident", "exclusiveGateway", "Action ?", 1550, mid_r - 25, GATEWAY, R, default="fAbandon")
task("notifierIncident", "Informer l'étudiant de l'abandon [REST Notifications]", D + "NotifierDelegate", 1740,
     mid_r - 40, R, fields=[("type", "INCIDENT_TRAITEMENT"), ("destinataire", "ETUDIANT")])
node("finAbandon", "endEvent", "Traitement abandonné", 1940, mid_r - 18, EVENT, R)

# --- flux de niveau processus
flow("f1", "debut", "receptionner", route="down")
flow("f2", "receptionner", "spVerifications")
flow("f3", "spVerifications", "gwRecevable")
flow("fRecevable", "gwRecevable", "spEnregistrement", "oui", "${recevable}")
flow("fNonRecevable", "gwRecevable", "notifierRejet", "non", route="upnode")
flow("fRejet", "notifierRejet", "finRejet")
flow("f4", "spEnregistrement", "evaluer", route="downto")
flow("f5", "evaluer", "gwDecision")
flow("fComplement", "gwDecision", "notifierComplement", "complément", "${decision == 'COMPLEMENT'}", route="upleft")
flow("fRetourEvaluation", "notifierComplement", "evaluer", route="downnode")
flow("fDecisionFinale", "gwDecision", "spFinalisation", "acceptée / refusée", route="rightup")
flow("fTimer", "timerRelance", "relancerEntreprise", route="down")
flow("fRelance", "relancerEntreprise", "finRelance")
flow("f6", "spFinalisation", "finTraitee", route="upnode")
flow("fErrA", "errA", "journaliser", route="down")
flow("fErrB", "errB", "journaliser", route="downleft")
flow("fErrC", "errC", "journaliser", route="downleft")
flow("f7", "journaliser", "traiterIncident")
flow("f8", "traiterIncident", "gwIncident")
flow("fRelancerA", "gwIncident", "spVerifications", "relancer (vérifications)",
     "${actionIncident == 'RELANCER' && etapeCourante == 'VERIFICATIONS'}", route="loopA")
flow("fRelancerB", "gwIncident", "spEnregistrement", "relancer (enregistrement)",
     "${actionIncident == 'RELANCER' && etapeCourante == 'ENREGISTREMENT'}", route="upnode")
flow("fRelancerC", "gwIncident", "spFinalisation", "relancer (finalisation)",
     "${actionIncident == 'RELANCER' && etapeCourante == 'FINALISATION'}", route="loopC")
flow("fAbandon", "gwIncident", "notifierIncident", "abandonner")
flow("f9", "notifierIncident", "finAbandon")


# ================================================================ géométrie
def c(n):
    return n["x"] + n["w"] / 2, n["y"] + n["h"] / 2


def waypoints(s, t, route):
    a, b = nodes[s], nodes[t]
    (ax, ay), (bx, by) = c(a), c(b)
    right_a, left_b = a["x"] + a["w"], b["x"]
    bottom_a, top_a = a["y"] + a["h"], a["y"]
    top_b, bottom_b = b["y"], b["y"] + b["h"]
    if route is None:
        if abs(ay - by) < 1:
            return [(right_a, ay), (left_b, by)]
        mx = (right_a + left_b) / 2
        return [(right_a, ay), (mx, ay), (mx, by), (left_b, by)]
    if route == "down":          # sortie bas, entrée gauche ou haut
        if left_b > ax + 5:
            return [(ax, bottom_a), (ax, by), (left_b, by)]
        return [(ax, bottom_a), (ax, top_b)]
    if route == "vertical":
        return [(ax, bottom_a), (bx, top_b)]
    if route == "up":            # sortie haut, entrée gauche
        return [(ax, top_a), (ax, by), (left_b, by)]
    if route == "upnode":        # sortie haut, entrée bas
        return [(ax, top_a), (ax, (top_a + bottom_b) / 2), (bx, (top_a + bottom_b) / 2), (bx, bottom_b)] \
            if abs(ax - bx) > 1 else [(ax, top_a), (bx, bottom_b)]
    if route == "downto":        # sortie droite, entrée haut
        return [(right_a, ay), (bx, ay), (bx, top_b)]
    if route == "downnode":      # sortie bas, entrée haut
        return [(ax, bottom_a), (ax, top_b - 20), (bx, top_b - 20), (bx, top_b)]
    if route == "upleft":        # passerelle -> tâche au-dessus à gauche
        return [(ax, top_a), (ax, by), (b["x"] + b["w"], by)]
    if route == "rightup":       # sortie droite, entrée bas du sous-processus
        return [(right_a, ay), (bx - 150, ay), (bx - 150, bottom_b - 60), (left_b, bottom_b - 60)]
    if route == "downleft":      # bordure -> tâche en bas à gauche
        return [(ax, bottom_a), (ax, by - 60), (bx + 20, by - 60), (bx + 20, top_b)]
    if route == "loopA":
        yy = bottom_b + 40
        return [(ax, top_a), (ax, yy), (b["x"] + 140, yy), (b["x"] + 140, bottom_b)]
    if route == "loopC":
        yy = a["y"] + a["h"] + 40
        return [(ax, a["y"] + a["h"]), (ax, yy), (b["x"] + 120, yy), (b["x"] + 120, bottom_b)]
    raise ValueError(route)


# ================================================================ XML
out = []
w = out.append


def a(s):
    return escape(s, {'"': "&quot;"})


w('<?xml version="1.0" encoding="UTF-8"?>')
w('<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"')
w('             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"')
w('             xmlns:flowable="http://flowable.org/bpmn"')
w('             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"')
w('             xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"')
w('             xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"')
w('             typeLanguage="http://www.w3.org/2001/XMLSchema"')
w('             expressionLanguage="http://www.w3.org/1999/XPath"')
w('             targetNamespace="http://stages.universite.com/processus">')
w('')
w('  <!-- Généré par tools/generer_bpmn.py : ne pas éditer à la main -->')
w('  <error id="ErreurTechnique" name="Service indisponible" errorCode="ERREUR_TECHNIQUE"/>')
w('')
w('  <collaboration id="Collaboration">')
w('    <participant id="Participant_Plateforme" name="Plateforme de gestion des stages" '
  'processRef="traitementCandidature"/>')
w('  </collaboration>')
w('')
w('  <process id="traitementCandidature" name="Traitement d\'une candidature de stage" isExecutable="true">')
w('    <documentation>Orchestration du traitement d\'une candidature, du dépôt à la décision finale.</documentation>')
w('    <laneSet id="LaneSet_1">')
for lid, name, _ in LANES:
    w(f'      <lane id="{lid}" name="{a(name)}">')
    for nid in order:
        if nodes[nid]["lane"] == lid and nodes[nid]["parent"] is None:
            w(f'        <flowNodeRef>{nid}</flowNodeRef>')
    w('      </lane>')
w('    </laneSet>')


def emit_node(nid, ind):
    n = nodes[nid]
    k, name, ex = n["kind"], a(n["name"]), n["extra"]
    p = " " * ind
    if k == "startEvent":
        w(f'{p}<startEvent id="{nid}" name="{name}"/>')
    elif k == "endEvent":
        w(f'{p}<endEvent id="{nid}" name="{name}"/>')
    elif k == "exclusiveGateway":
        w(f'{p}<exclusiveGateway id="{nid}" name="{name}" default="{ex["default"]}"/>')
    elif k == "serviceTask":
        w(f'{p}<serviceTask id="{nid}" name="{name}" flowable:class="{ex["cls"]}">')
        if ex.get("doc"):
            w(f'{p}  <documentation>{a(ex["doc"])}</documentation>')
        if ex["fields"]:
            w(f'{p}  <extensionElements>')
            for fname, fval in ex["fields"]:
                attr = "expression" if "${" in fval else "stringValue"
                w(f'{p}    <flowable:field name="{fname}" {attr}="{a(fval)}"/>')
            w(f'{p}  </extensionElements>')
        w(f'{p}</serviceTask>')
    elif k == "userTask":
        w(f'{p}<userTask id="{nid}" name="{name}" flowable:candidateGroups="{ex["group"]}">')
        w(f'{p}  <extensionElements>')
        if nid == "evaluer":
            w(f'{p}    <flowable:formProperty id="decision" name="Décision" type="enum" required="true">')
            for vid, vname in [("ACCEPTEE", "Accepter"), ("REFUSEE", "Refuser"),
                               ("COMPLEMENT", "Demander un complément")]:
                w(f'{p}      <flowable:value id="{vid}" name="{vname}"/>')
            w(f'{p}    </flowable:formProperty>')
            w(f'{p}    <flowable:formProperty id="commentaireDecision" name="Commentaire" type="string"/>')
            w(f'{p}    <flowable:formProperty id="dateDebut" name="Date de début" type="string"/>')
            w(f'{p}    <flowable:formProperty id="dateFin" name="Date de fin" type="string"/>')
            w(f'{p}    <flowable:formProperty id="tuteur" name="Tuteur entreprise" type="string"/>')
        else:
            w(f'{p}    <flowable:formProperty id="actionIncident" name="Action" type="enum" required="true">')
            w(f'{p}      <flowable:value id="RELANCER" name="Relancer l\'étape"/>')
            w(f'{p}      <flowable:value id="ABANDONNER" name="Abandonner le traitement"/>')
            w(f'{p}    </flowable:formProperty>')
        w(f'{p}  </extensionElements>')
        w(f'{p}</userTask>')
    elif k == "boundaryError":
        w(f'{p}<boundaryEvent id="{nid}" name="{name}" attachedToRef="{ex["attached"]}">')
        w(f'{p}  <errorEventDefinition errorRef="ErreurTechnique"/>')
        w(f'{p}</boundaryEvent>')
    elif k == "boundaryTimer":
        w(f'{p}<boundaryEvent id="{nid}" name="{name}" attachedToRef="{ex["attached"]}" cancelActivity="false">')
        w(f'{p}  <timerEventDefinition><timeDuration>{ex["duration"]}</timeDuration></timerEventDefinition>')
        w(f'{p}</boundaryEvent>')
    elif k == "subProcess":
        w(f'{p}<subProcess id="{nid}" name="{name}">')
        w(f'{p}  <extensionElements>')
        w(f'{p}    <flowable:executionListener event="start" '
          f'expression="${{execution.setVariable(\'etapeCourante\', \'{ex["etape"]}\')}}"/>')
        w(f'{p}  </extensionElements>')
        for cid in order:
            if nodes[cid]["parent"] == nid:
                emit_node(cid, ind + 2)
        for fl in flows:
            if fl[6] == nid:
                emit_flow(fl, ind + 2)
        w(f'{p}</subProcess>')


def emit_flow(fl, ind):
    fid, s, t, name, cond, _, _ = fl
    p = " " * ind
    nm = f' name="{a(name)}"' if name else ""
    if cond:
        w(f'{p}<sequenceFlow id="{fid}"{nm} sourceRef="{s}" targetRef="{t}">')
        w(f'{p}  <conditionExpression xsi:type="tFormalExpression"><![CDATA[{cond}]]></conditionExpression>')
        w(f'{p}</sequenceFlow>')
    else:
        w(f'{p}<sequenceFlow id="{fid}"{nm} sourceRef="{s}" targetRef="{t}"/>')


for nid in order:
    if nodes[nid]["parent"] is None:
        emit_node(nid, 4)
for fl in flows:
    if fl[6] is None:
        emit_flow(fl, 4)
w('  </process>')
w('')

# ---------------------------------------------------------------- BPMN-DI
w('  <bpmndi:BPMNDiagram id="Diagramme">')
w('    <bpmndi:BPMNPlane id="Plan" bpmnElement="Collaboration">')
w(f'      <bpmndi:BPMNShape id="Participant_Plateforme_di" bpmnElement="Participant_Plateforme" isHorizontal="true">')
w(f'        <omgdc:Bounds x="{POOL_X}" y="{POOL_Y}" width="{POOL_W}" height="{POOL_H}"/>')
w('      </bpmndi:BPMNShape>')
for lid, _, h in LANES:
    yy, hh = lane_y[lid]
    w(f'      <bpmndi:BPMNShape id="{lid}_di" bpmnElement="{lid}" isHorizontal="true">')
    w(f'        <omgdc:Bounds x="{POOL_X + LANE_HDR}" y="{yy}" width="{POOL_W - LANE_HDR}" height="{hh}"/>')
    w('      </bpmndi:BPMNShape>')
for nid in order:
    n = nodes[nid]
    extra = ' isExpanded="true"' if n["kind"] == "subProcess" else ""
    extra += ' isMarkerVisible="true"' if n["kind"] == "exclusiveGateway" else ""
    w(f'      <bpmndi:BPMNShape id="{nid}_di" bpmnElement="{nid}"{extra}>')
    w(f'        <omgdc:Bounds x="{n["x"]:.0f}" y="{n["y"]:.0f}" width="{n["w"]}" height="{n["h"]}"/>')
    if n["kind"] in ("startEvent", "endEvent", "exclusiveGateway", "boundaryError", "boundaryTimer") and n["name"]:
        lw = 110
        lx = n["x"] + n["w"] / 2 - lw / 2
        lyy = n["y"] + n["h"] + 4
        if n["kind"] == "exclusiveGateway":
            lyy = n["y"] - 30
        if n["kind"] == "boundaryError":
            lx, lyy, lw = n["x"] + n["w"] + 4, n["y"] + 12, 80
        if n["kind"] == "boundaryTimer":
            lx, lyy, lw = n["x"] - 115, n["y"] + 14, 105
        w(f'        <bpmndi:BPMNLabel><omgdc:Bounds x="{lx:.0f}" y="{lyy:.0f}" width="{lw}" height="27"/></bpmndi:BPMNLabel>')
    w('      </bpmndi:BPMNShape>')
for fid, s, t, name, cond, route, _ in flows:
    pts = waypoints(s, t, route)
    w(f'      <bpmndi:BPMNEdge id="{fid}_di" bpmnElement="{fid}">')
    for x, yv in pts:
        w(f'        <omgdi:waypoint x="{x:.0f}" y="{yv:.0f}"/>')
    if name:
        (x1, y1), (x2, y2) = pts[0], pts[1]
        lx, lyv = (x1 + x2) / 2 + 4, (y1 + y2) / 2 - 18
        if x1 == x2:  # premier segment vertical : libellé à droite du départ
            lx, lyv = x1 + 6, min(y1, y2) + (abs(y2 - y1) / 2) - 7
        if route in ("loopA", "loopC"):
            lx, lyv = pts[1][0] + 8, pts[1][1] - 16
        w(f'        <bpmndi:BPMNLabel><omgdc:Bounds x="{lx:.0f}" y="{lyv:.0f}" '
          f'width="{len(name) * 6 + 10}" height="14"/></bpmndi:BPMNLabel>')
    w('      </bpmndi:BPMNEdge>')
w('    </bpmndi:BPMNPlane>')
w('  </bpmndi:BPMNDiagram>')
w('</definitions>')
print("\n".join(out))
