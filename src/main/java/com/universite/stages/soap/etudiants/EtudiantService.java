package com.universite.stages.soap.etudiants;

import com.universite.stages.soap.commun.Namespaces;
import com.universite.stages.soap.commun.ServiceException;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

import java.util.List;

/**
 * Service 1 — Gestion des étudiants (contrat SOAP, style document/literal).
 * WSDL : {@code /ws/etudiants?wsdl}
 */
@WebService(name = "EtudiantService", targetNamespace = Namespaces.ETUDIANTS)
public interface EtudiantService {

    @WebMethod(operationName = "ajouterEtudiant")
    @WebResult(name = "etudiant")
    Etudiant ajouterEtudiant(@WebParam(name = "etudiant") Etudiant etudiant) throws ServiceException;

    @WebMethod(operationName = "consulterEtudiant")
    @WebResult(name = "etudiant")
    Etudiant consulterEtudiant(@WebParam(name = "matricule") String matricule) throws ServiceException;

    @WebMethod(operationName = "modifierEtudiant")
    @WebResult(name = "etudiant")
    Etudiant modifierEtudiant(@WebParam(name = "etudiant") Etudiant etudiant) throws ServiceException;

    @WebMethod(operationName = "supprimerEtudiant")
    @WebResult(name = "supprime")
    boolean supprimerEtudiant(@WebParam(name = "matricule") String matricule) throws ServiceException;

    @WebMethod(operationName = "listerEtudiants")
    @WebResult(name = "etudiant")
    List<Etudiant> listerEtudiants(@WebParam(name = "filiere") String filiere,
                                   @WebParam(name = "statut") String statut) throws ServiceException;

    @WebMethod(operationName = "verifierEtudiant")
    @WebResult(name = "verification")
    VerificationEtudiant verifierEtudiant(@WebParam(name = "matricule") String matricule) throws ServiceException;

    @WebMethod(operationName = "changerStatutEtudiant")
    @WebResult(name = "etudiant")
    Etudiant changerStatutEtudiant(@WebParam(name = "matricule") String matricule,
                                   @WebParam(name = "statut") String statut) throws ServiceException;
}
