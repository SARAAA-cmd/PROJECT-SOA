/*
 * Client SOAP minimal pour le navigateur : construit une enveloppe SOAP 1.1
 * avec l'en-tête WS-Security UsernameToken et convertit la réponse XML en objets JS.
 */
const Soap = (() => {
  const NS = {
    etudiants: 'http://stages.universite.com/etudiants',
    entreprises: 'http://stages.universite.com/entreprises',
    offres: 'http://stages.universite.com/offres',
  };
  const WSSE = 'http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd';

  const echapper = (v) => String(v).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

  function versXml(nom, valeur) {
    if (valeur === undefined || valeur === null || valeur === '') return '';
    if (Array.isArray(valeur)) return valeur.map((v) => versXml(nom, v)).join('');
    if (typeof valeur === 'object') {
      return `<${nom}>${Object.entries(valeur).map(([k, v]) => versXml(k, v)).join('')}</${nom}>`;
    }
    return `<${nom}>${echapper(valeur)}</${nom}>`;
  }

  function versObjet(el) {
    const enfants = Array.from(el.children);
    if (enfants.length === 0) {
      const t = el.textContent;
      if (t === 'true') return true;
      if (t === 'false') return false;
      if (/^-?\d+$/.test(t) && t.length < 12 && !/^0\d/.test(t)) return Number(t);
      return t;
    }
    const o = {};
    for (const c of enfants) {
      const v = versObjet(c);
      const k = c.localName;
      if (k in o) o[k] = [].concat(o[k], v);
      else o[k] = v;
    }
    return o;
  }

  /**
   * Appelle une opération. `liste` indique que la réponse est une séquence
   * (opérations lister*), afin de toujours obtenir un tableau.
   */
  async function appeler(service, operation, params = {}, { liste = false } = {}) {
    const { username, password } = Auth.identifiants();
    const corps = Object.entries(params).map(([k, v]) => versXml(k, v)).join('');
    const enveloppe = `<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:s="${NS[service]}" xmlns:wsse="${WSSE}">
  <soapenv:Header><wsse:Security><wsse:UsernameToken>
    <wsse:Username>${echapper(username)}</wsse:Username><wsse:Password>${echapper(password)}</wsse:Password>
  </wsse:UsernameToken></wsse:Security></soapenv:Header>
  <soapenv:Body><s:${operation}>${corps}</s:${operation}></soapenv:Body>
</soapenv:Envelope>`;
    let r;
    try {
      r = await fetch(`/ws/${service}`, {
        method: 'POST',
        headers: { 'Content-Type': 'text/xml; charset=utf-8', SOAPAction: '""' },
        body: enveloppe,
      });
    } catch (e) {
      throw new ErreurApplication({ statut: 0, code: 'RESEAU', service, message: `${service}.${operation} : ${e.message}` });
    }
    if (r.status === 503) {
      throw new ErreurApplication({ statut: 503, code: 'SERVICE_INDISPONIBLE', service, message: `Service SOAP « ${service} » indisponible (${operation})` });
    }
    const doc = new DOMParser().parseFromString(await r.text(), 'text/xml');
    const fault = doc.getElementsByTagNameNS('*', 'Fault')[0];
    if (fault) {
      const detail = fault.getElementsByTagNameNS('*', 'ServiceFault')[0];
      if (detail) {
        const f = versObjet(detail);
        throw new ErreurApplication({
          statut: STATUT_DES_CODES_SOAP[f.code] || 500, code: f.code, service, details: [].concat(f.detail || []), message: f.message,
        });
      }
      const code = fault.getElementsByTagName('faultcode')[0]?.textContent || '';
      const texte = fault.getElementsByTagName('faultstring')[0]?.textContent || 'Faute SOAP';
      throw new ErreurApplication({ statut: /FailedAuthentication/.test(code) ? 401 : 500, code: code || null, service, message: texte });
    }
    const reponse = doc.getElementsByTagNameNS(NS[service], `${operation}Response`)[0];
    const valeurs = Array.from(reponse.children).map(versObjet);
    return liste ? valeurs : valeurs[0];
  }

  return { appeler };
})();
