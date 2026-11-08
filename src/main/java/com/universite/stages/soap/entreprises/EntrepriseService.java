package com.universite.stages.soap.entreprises;

import com.universite.stages.soap.commun.Namespaces;
import com.universite.stages.soap.commun.ServiceException;
import com.universite.stages.soap.offres.Offre;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

import java.util.List;

/**
 * Service 2 — Gestion des entreprises partenaires (contrat SOAP).
 * WSDL : {@code /ws/entreprises?wsdl}
 */
@WebService(name = "EntrepriseService", targetNamespace = Namespaces.ENTREPRISES)
public interface EntrepriseService {

    @WebMethod(operationName = "ajouterEntreprise")
    @WebResult(name = "entreprise")
    Entreprise ajouterEntreprise(@WebParam(name = "entreprise") Entreprise entreprise) throws ServiceException;

    @WebMethod(operationName = "consulterEntreprise")
    @WebResult(name = "entreprise")
    Entreprise consulterEntreprise(@WebParam(name = "entrepriseId") long entrepriseId) throws ServiceException;

    @WebMethod(operationName = "modifierEntreprise")
    @WebResult(name = "entreprise")
    Entreprise modifierEntreprise(@WebParam(name = "entreprise") Entreprise entreprise) throws ServiceException;

    @WebMethod(operationName = "supprimerEntreprise")
    @WebResult(name = "supprime")
    boolean supprimerEntreprise(@WebParam(name = "entrepriseId") long entrepriseId) throws ServiceException;

    @WebMethod(operationName = "listerEntreprises")
    @WebResult(name = "entreprise")
    List<Entreprise> listerEntreprises(@WebParam(name = "secteurActivite") String secteurActivite) throws ServiceException;

    @WebMethod(operationName = "verifierEntreprise")
    @WebResult(name = "verification")
    VerificationEntreprise verifierEntreprise(@WebParam(name = "entrepriseId") long entrepriseId) throws ServiceException;

    @WebMethod(operationName = "consulterOffresEntreprise")
    @WebResult(name = "offre")
    List<Offre> consulterOffresEntreprise(@WebParam(name = "entrepriseId") long entrepriseId) throws ServiceException;
}
