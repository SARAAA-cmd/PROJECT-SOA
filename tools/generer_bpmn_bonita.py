#!/usr/bin/env python3
"""Génère une variante du processus importable dans Bonita Studio (Fichier > Importer > BPMN 2.0).

Différences avec le fichier exécuté par Flowable :
- pas de sous-processus embarqués (non gérés par Bonita) : le flux est mis à plat ;
- un événement d'erreur de bordure sur chaque tâche de service critique ;
- aucune extension propriétaire (flowable:*) ; conditions écrites en Groovy.
Usage : python3 tools/generer_bpmn_bonita.py > docs/bonita/traitement-candidature-bonita.bpmn
"""
from xml.sax.saxutils import escape

TASK, EV, GW = (150, 75), (36, 36), (50, 50)
SX = 1.18                    # facteur d'espacement horizontal
POOL_X, POOL_Y, HDR, POOL_W = 30, 30, 30, 3450
LANES = [("laneEtudiant", "Étudiant", 130), ("laneOrchestrateur", "Orchestrateur SOA (services SOAP / REST)", 360),
         ("laneEntreprise", "Entreprise partenaire", 290), ("laneResponsable", "Responsable des stages", 190)]
ly = {}
y = POOL_Y
for lid, _, h in LANES:
    ly[lid] = (y, h)
    y += h
POOL_H = y - POOL_Y
S, O, E, R = (l[0] for l in LANES)
R1 = ly[O][0] + 110          # ligne principale de l'orchestrateur (centre)
R2 = ly[O][0] + 270          # ligne secondaire (rejet, refus)
RE = ly[E][0] + 90           # ligne de l'entreprise
RE2 = ly[E][0] + 195
RR = ly[R][0] + 100
BUS = ly[R][0] - 14          # axe horizontal des flux d'erreur

nodes, order, flows = {}, [], []


def node(i, kind, name, cx, cy, size, lane, scale=True, **ex):
    w, h = size
    if scale:
        cx = cx * SX
    nodes[i] = dict(kind=kind, name=name, x=cx - w / 2, y=cy - h / 2, w=w, h=h, lane=lane, ex=ex)
    order.append(i)


def svc(i, name, cx, cy, lane, doc, err=True):
    node(i, "serviceTask", name, cx, cy, TASK, lane, doc=doc)
    if err:
        n = nodes[i]
        node("err_" + i, "boundaryError", "", n["x"] + n["w"] - 25, n["y"] + n["h"], EV, lane, scale=False, attached=i)
        flows.append(("fErr_" + i, "err_" + i, "journaliser", None, None, "bus"))


def flow(i, s, t, name=None, cond=None, route=None):
    flows.append((i, s, t, name, cond, route))


# ------------------------------------------------------------------ éléments
node("debut", "startEvent", "Candidature soumise", 200, ly[S][0] + 55, EV, S)
svc("receptionner", "Recevoir dossier", 200, R1, O,
    "Contrôle de forme des données saisies (matricule, offre, lettre, CV).", err=False)
svc("verifierEtudiant", "Vérifier étudiant", 370, R1, O,
    "SOAP /ws/etudiants : verifierEtudiant(matricule) puis consulterEtudiant(matricule).")
node("gwEligible", "exclusiveGateway", "Étudiant éligible ?", 520, R1, GW, O, default="fNonEligible")
svc("verifierOffre", "Vérifier offre", 670, R1, O,
    "SOAP /ws/offres : consulterOffre, verifierCompatibilite ; SOAP /ws/entreprises : verifierEntreprise, consulterEntreprise.")
svc("controler", "Contrôle dossier", 840, R1, O,
    "SOAP verifierDisponibilite(offreId) ; REST GET /api/candidatures?matricule=..&offreId=.. (doublon).")
node("gwRecevable", "exclusiveGateway", "5. Recevable ?", 990, R1, GW, O, default="fNonRecevable")
svc("notifierRejet", "Notifier rejet", 990, R2, O,
    "REST POST /api/notifications, type REJET_CANDIDATURE (connecteur : ignorer l'erreur).", err=False)
node("finRejet", "endEvent", "Candidature rejetée", 1110, R2, EV, O)
svc("enregistrer", "Inscrire demande", 1140, R1, O,
    "REST POST /api/candidatures → candidatureId.")
svc("notifierConfirmation", "Confirme dépôt", 1310, R1, O,
    "REST POST /api/notifications, type CONFIRMATION_DEPOT (connecteur : ignorer l'erreur).", err=False)
svc("notifierEntreprise", "Prévenir société", 1480, R1, O,
    "REST POST /api/notifications, type RECEPTION_CANDIDATURE (connecteur : ignorer l'erreur).", err=False)
node("evaluer", "userTask", "Évaluer", 1800, RE, TASK, E,
     doc="Prise en charge par l'entreprise (REST PATCH statut EN_COURS_EVALUATION), puis formulaire : "
         "decision (ACCEPTEE / REFUSEE / COMPLEMENT), commentaireDecision, dateDebut, dateFin, tuteur.")
ev = nodes["evaluer"]
node("timerRelance", "boundaryTimer", "Délai dépassé", ev["x"] + 20, ev["y"] + ev["h"], EV, E, scale=False,
     attached="evaluer", duration="P3D")
svc("relancerEntreprise", "Relancer société", 1560, RE2, E,
    "REST POST /api/notifications, type RELANCE_DECISION (connecteur : ignorer l'erreur).", err=False)
node("finRelance", "endEvent", "Relance envoyée", 1430, RE2, EV, E)
node("gwDecision", "exclusiveGateway", "Décision ?", 1960, RE, GW, E, default="fDecisionFinale")
svc("notifierComplement", "Demander pièces", 1960, RE2, E,
    "REST POST /api/notifications, type DEMANDE_COMPLEMENT (connecteur : ignorer l'erreur).", err=False)
svc("majDecision", "Valider décision", 2090, R1, O,
    "REST PATCH /api/candidatures/{id}/statut {statut: decision, motif: commentaireDecision}.")
svc("notifierResultat", "Notifier résultat", 2260, R1, O,
    "REST POST /api/notifications, type ACCEPTATION ou REFUS (connecteur : ignorer l'erreur).", err=False)
node("gwAcceptee", "exclusiveGateway", "Acceptée ?", 2410, R1, GW, O, default="fRefusee")
node("finRefus", "endEvent", "Refus notifié", 2410, R2, EV, O)
svc("creerStage", "Créer stage", 2560, R1, O,
    "REST POST /api/stages → stageId.")
svc("majEtudiant", "MAJ étudiant", 2730, R1, O,
    "SOAP changerStatutEtudiant(matricule, EN_STAGE) ; SOAP reserverPlace(offreId).")
node("finAcceptee", "endEvent", "Stage créé", 2850, R1, EV, O)
svc("journaliser", "Tracer incident", 1000, RR, R,
    "Enregistre serviceEnErreur, messageErreur et etapeCourante.", err=False)
node("traiterIncident", "userTask", "Traiter incident", 1170, RR, TASK, R,
     doc="Formulaire : actionIncident (RELANCER / ABANDONNER).")
node("gwIncident", "exclusiveGateway", "Action ?", 1330, RR, GW, R, default="fAbandon")
svc("notifierIncident", "Notifier abandon", 1520, RR, R,
    "REST POST /api/notifications, type INCIDENT_TRAITEMENT (connecteur : ignorer l'erreur).", err=False)
node("finAbandon", "endEvent", "Traitement abandonné", 1680, RR, EV, R)

# ------------------------------------------------------------------ flux
flow("f1", "debut", "receptionner", route="vertical")
flow("f2", "receptionner", "verifierEtudiant")
flow("f3", "verifierEtudiant", "gwEligible")
flow("fEligible", "gwEligible", "verifierOffre", "oui", "etudiantEligible")
flow("fNonEligible", "gwEligible", "notifierRejet", "non", route="downright")
flow("f4", "verifierOffre", "controler")
flow("f5", "controler", "gwRecevable")
flow("fRecevable", "gwRecevable", "enregistrer", "oui", "recevable")
flow("fNonRecevable", "gwRecevable", "notifierRejet", "non", route="down")
flow("fRejet", "notifierRejet", "finRejet")
flow("f6", "enregistrer", "notifierConfirmation")
flow("f7", "notifierConfirmation", "notifierEntreprise")
flow("f8", "notifierEntreprise", "evaluer", route="rightdown")
flow("f10", "evaluer", "gwDecision")
flow("fComplement", "gwDecision", "notifierComplement", "complément", "decision == \"COMPLEMENT\"", route="down")
flow("fRetour", "notifierComplement", "evaluer", route="backup")
flow("fDecisionFinale", "gwDecision", "majDecision", "acceptée / refusée", route="rightup")
flow("fTimer", "timerRelance", "relancerEntreprise", route="downleft")
flow("fRelance", "relancerEntreprise", "finRelance", route="left")
flow("f11", "majDecision", "notifierResultat")
flow("f12", "notifierResultat", "gwAcceptee")
flow("fAcceptee", "gwAcceptee", "creerStage", "oui", "decision == \"ACCEPTEE\"")
flow("fRefusee", "gwAcceptee", "finRefus", "non", route="down")
flow("f13", "creerStage", "majEtudiant")
flow("f14", "majEtudiant", "finAcceptee")
flow("f15", "journaliser", "traiterIncident")
flow("f16", "traiterIncident", "gwIncident")
flow("fRelancerA", "gwIncident", "verifierEtudiant", "relancer (vérifications)",
     "actionIncident == \"RELANCER\" && etapeCourante == \"VERIFICATIONS\"", route="loop")
flow("fRelancerB", "gwIncident", "enregistrer", "relancer (enregistrement)",
     "actionIncident == \"RELANCER\" && etapeCourante == \"ENREGISTREMENT\"", route="loop")
flow("fRelancerC", "gwIncident", "majDecision", "relancer (finalisation)",
     "actionIncident == \"RELANCER\" && etapeCourante == \"FINALISATION\"", route="loop")
flow("fAbandon", "gwIncident", "notifierIncident", "abandonner")
flow("f17", "notifierIncident", "finAbandon")

loop_x = {"verifierEtudiant": -30, "enregistrer": -30, "majDecision": -30}
loop_y = {"fRelancerA": 30, "fRelancerB": 18, "fRelancerC": 6}


# ------------------------------------------------------------------ géométrie
def cxy(n):
    return n["x"] + n["w"] / 2, n["y"] + n["h"] / 2


def pts(fid, s, t, route):
    a, b = nodes[s], nodes[t]
    (ax, ay), (bx, by) = cxy(a), cxy(b)
    ar, at, ab = a["x"] + a["w"], a["y"], a["y"] + a["h"]
    bl, bt, bb = b["x"], b["y"], b["y"] + b["h"]
    if route is None:
        if abs(ay - by) < 1:
            return [(ar, ay), (bl, by)]
        m = (ar + bl) / 2
        return [(ar, ay), (m, ay), (m, by), (bl, by)]
    if route == "down":
        return [(ax, ab), (ax, by), (bl, by)] if bl > ax + 5 else [(ax, ab), (bx, bt)]
    if route == "downright":
        return [(ax, ab), (ax, by), (bl, by)]
    if route == "rightdown":
        return [(ar, ay), (bx, ay), (bx, bt)]
    if route == "rightup":
        return [(ar, ay), (bx, ay), (bx, bb)]
    if route == "backup":
        return [(a["x"], ay), (b["x"] + b["w"] - 20, ay), (b["x"] + b["w"] - 20, bb)]
    if route == "downleft":
        return [(ax, ab), (ax, by), (b["x"] + b["w"], by)]
    if route == "vertical":
        return [(ax, ab), (bx, bt)]
    if route == "left":
        return [(a["x"], ay), (b["x"] + b["w"], by)]
    if route == "bus":
        return [(ax, ab), (ax, BUS), (bx, BUS), (bx, bt)]
    if route == "loop":
        yy = ly[R][0] + ly[R][1] - loop_y[fid]
        tx = b["x"] + 25 if t != "enregistrer" else b["x"] + 25
        return [(ax, ab), (ax, yy), (POOL_X + 60 + loop_y[fid], yy), (POOL_X + 60 + loop_y[fid], R1 + 60),
                (tx, R1 + 60), (tx, bb)] if False else [(ax, ab), (ax, yy), (tx, yy), (tx, bb)]
    raise ValueError(route)


# ------------------------------------------------------------------ XML
out = []
w = out.append
a = lambda s: escape(s, {'"': "&quot;"})
w('<?xml version="1.0" encoding="UTF-8"?>')
w('<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"')
w('             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"')
w('             xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"')
w('             xmlns:di="http://www.omg.org/spec/DD/20100524/DI"')
w('             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"')
w('             id="Definitions_GestionStages" targetNamespace="http://stages.universite.com/processus"')
w('             exporter="generer_bpmn_bonita.py" exporterVersion="1.0">')
w('  <error id="ErreurTechnique" name="Service indisponible" errorCode="ERREUR_TECHNIQUE"/>')
w('  <collaboration id="Collaboration">')
w('    <participant id="PoolPlateforme" name="Traitement candidature stage" processRef="TraitementCandidature"/>')
w('  </collaboration>')
w('  <process id="TraitementCandidature" name="Traitement candidature stage" isExecutable="true">')
w('    <documentation>Traitement d\'une candidature de stage, du dépôt à la décision finale. '
  'Variante pour Bonita : flux à plat, une bordure d\'erreur par tâche critique.</documentation>')
w('    <laneSet id="LaneSet">')
for lid, nm, _ in LANES:
    w(f'      <lane id="{lid}" name="{a(nm)}">')
    for i in order:
        if nodes[i]["lane"] == lid:
            w(f'        <flowNodeRef>{i}</flowNodeRef>')
    w('      </lane>')
w('    </laneSet>')
for i in order:
    n = nodes[i]
    k, nm, ex = n["kind"], a(n["name"]), n["ex"]
    if k == "startEvent":
        w(f'    <startEvent id="{i}" name="{nm}"/>')
    elif k == "endEvent":
        w(f'    <endEvent id="{i}" name="{nm}"/>')
    elif k == "exclusiveGateway":
        w(f'    <exclusiveGateway id="{i}" name="{nm}" default="{ex["default"]}"/>')
    elif k in ("serviceTask", "userTask"):
        w(f'    <{k} id="{i}" name="{nm}">')
        w(f'      <documentation>{a(ex["doc"])}</documentation>')
        w(f'    </{k}>')
    elif k == "boundaryError":
        w(f'    <boundaryEvent id="{i}" name="Erreur" attachedToRef="{ex["attached"]}" cancelActivity="true">')
        w('      <errorEventDefinition errorRef="ErreurTechnique"/>')
        w('    </boundaryEvent>')
    elif k == "boundaryTimer":
        w(f'    <boundaryEvent id="{i}" name="{nm}" attachedToRef="{ex["attached"]}" cancelActivity="false">')
        w(f'      <timerEventDefinition><timeDuration xsi:type="tFormalExpression">{ex["duration"]}</timeDuration></timerEventDefinition>')
        w('    </boundaryEvent>')
for fid, s, t, nm, cond, _ in flows:
    nat = f' name="{a(nm)}"' if nm else ""
    if cond:
        w(f'    <sequenceFlow id="{fid}"{nat} sourceRef="{s}" targetRef="{t}">')
        w(f'      <conditionExpression xsi:type="tFormalExpression" language="http://groovy.codehaus.org/">{a(cond)}</conditionExpression>')
        w('    </sequenceFlow>')
    else:
        w(f'    <sequenceFlow id="{fid}"{nat} sourceRef="{s}" targetRef="{t}"/>')
w('  </process>')
w('  <bpmndi:BPMNDiagram id="Diagramme">')
w('    <bpmndi:BPMNPlane id="Plan" bpmnElement="Collaboration">')
w(f'      <bpmndi:BPMNShape id="PoolPlateforme_di" bpmnElement="PoolPlateforme" isHorizontal="true">'
  f'<dc:Bounds x="{POOL_X}" y="{POOL_Y}" width="{POOL_W}" height="{POOL_H}"/></bpmndi:BPMNShape>')
for lid, _, h in LANES:
    yy, hh = ly[lid]
    w(f'      <bpmndi:BPMNShape id="{lid}_di" bpmnElement="{lid}" isHorizontal="true">'
      f'<dc:Bounds x="{POOL_X + HDR}" y="{yy}" width="{POOL_W - HDR}" height="{hh}"/></bpmndi:BPMNShape>')
for i in order:
    n = nodes[i]
    mk = ' isMarkerVisible="true"' if n["kind"] == "exclusiveGateway" else ""
    lab = ""
    if n["kind"] in ("startEvent", "endEvent", "exclusiveGateway", "boundaryTimer") and n["name"]:
        lx, lyv = n["x"] + n["w"] / 2 - 55, n["y"] + n["h"] + 4
        if n["kind"] == "exclusiveGateway":
            lyv = n["y"] - 30
        if n["kind"] == "boundaryTimer":
            lx, lyv = n["x"] - 112, n["y"] + 22
        lab = f'<bpmndi:BPMNLabel><dc:Bounds x="{lx:.0f}" y="{lyv:.0f}" width="110" height="27"/></bpmndi:BPMNLabel>'
    w(f'      <bpmndi:BPMNShape id="{i}_di" bpmnElement="{i}"{mk}><dc:Bounds x="{n["x"]:.0f}" y="{n["y"]:.0f}" '
      f'width="{n["w"]}" height="{n["h"]}"/>{lab}</bpmndi:BPMNShape>')
for fid, s, t, nm, cond, route in flows:
    p = pts(fid, s, t, route)
    wp = "".join(f'<di:waypoint x="{x:.0f}" y="{yv:.0f}"/>' for x, yv in p)
    lab = ""
    if nm:
        (x1, y1), (x2, y2) = p[0], p[1]
        lx, lyv = ((x1 + x2) / 2 + 4, y1 - 18) if y1 == y2 else (x1 + 5, (y1 + y2) / 2 - 7)
        if route == "loop":
            lx, lyv = p[2][0] + 4, p[2][1] - 15
        lab = f'<bpmndi:BPMNLabel><dc:Bounds x="{lx:.0f}" y="{lyv:.0f}" width="{len(nm) * 6 + 8}" height="14"/></bpmndi:BPMNLabel>'
    w(f'      <bpmndi:BPMNEdge id="{fid}_di" bpmnElement="{fid}">{wp}{lab}</bpmndi:BPMNEdge>')
w('    </bpmndi:BPMNPlane>')
w('  </bpmndi:BPMNDiagram>')
w('</definitions>')
print("\n".join(out))
