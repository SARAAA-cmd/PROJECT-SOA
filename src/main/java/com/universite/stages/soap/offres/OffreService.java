package com.universite.stages.soap.offres;

import com.universite.stages.soap.commun.Namespaces;
import com.universite.stages.soap.commun.ServiceException;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;

import java.util.List;

/**
 * Service 3 — Gestion des offres de stage (contrat SOAP).
 * WSDL : {@code /ws/offres?wsdl}
 */
@WebService(name = "OffreService", targetNamespace = Namespaces.OFFRES)
public interface OffreService {

    @WebMethod(operationName = "creerOffre")
    @WebResult(name = "offre")
    Offre creerOffre(@WebParam(name = "offre") Offre offre) throws ServiceException;

    @WebMethod(operationName = "consulterOffre")
    @WebResult(name = "offre")
    Offre consulterOffre(@WebParam(name = "offreId") long offreId) throws ServiceException;

    @WebMethod(operationName = "modifierOffre")
    @WebResult(name = "offre")
    Offre modifierOffre(@WebParam(name = "offre") Offre offre) throws ServiceException;

    @WebMethod(operationName = "supprimerOffre")
    @WebResult(name = "supprime")
    boolean supprimerOffre(@WebParam(name = "offreId") long offreId) throws ServiceException;

    @WebMethod(operationName = "listerOffres")
    @WebResult(name = "offre")
    List<Offre> listerOffres(@WebParam(name = "statut") String statut,
                             @WebParam(name = "entrepriseId") Long entrepriseId) throws ServiceException;

    @WebMethod(operationName = "listerOffresDisponibles")
    @WebResult(name = "offre")
    List<Offre> listerOffresDisponibles() throws ServiceException;

    @WebMethod(operationName = "verifierDisponibilite")
    @WebResult(name = "disponibilite")
    DisponibiliteOffre verifierDisponibilite(@WebParam(name = "offreId") long offreId) throws ServiceException;

    @WebMethod(operationName = "verifierCompatibilite")
    @WebResult(name = "compatibilite")
    Compatibilite verifierCompatibilite(@WebParam(name = "offreId") long offreId,
                                        @WebParam(name = "filiere") String filiere,
                                        @WebParam(name = "niveau") String niveau,
                                        @WebParam(name = "competences") String competences) throws ServiceException;

    @WebMethod(operationName = "reserverPlace")
    @WebResult(name = "offre")
    Offre reserverPlace(@WebParam(name = "offreId") long offreId) throws ServiceException;

    @WebMethod(operationName = "listerOffresExpirantBientot")
    @WebResult(name = "offre")
    List<Offre> listerOffresExpirantBientot(@WebParam(name = "jours") int jours) throws ServiceException;
}
