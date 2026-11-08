package com.universite.stages.soap.etudiants;

import com.universite.stages.common.Database;
import com.universite.stages.common.Database.DataAccessException;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accès aux données de la table etudiant. */
public class EtudiantDao {

    public Optional<Etudiant> trouver(String matricule) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM etudiant WHERE matricule = ?")) {
            ps.setString(1, matricule);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lecture de l'étudiant impossible", e);
        }
    }

    public boolean emailUtilise(String email, String saufMatricule) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM etudiant WHERE LOWER(email) = LOWER(?) AND matricule <> ?")) {
            ps.setString(1, email);
            ps.setString(2, saufMatricule == null ? "" : saufMatricule);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lecture de l'étudiant impossible", e);
        }
    }

    public List<Etudiant> lister(String filiere, String statut) {
        StringBuilder sql = new StringBuilder("SELECT * FROM etudiant WHERE 1=1");
        List<String> params = new ArrayList<>();
        if (filiere != null && !filiere.isBlank()) {
            sql.append(" AND filiere = ?");
            params.add(filiere);
        }
        if (statut != null && !statut.isBlank()) {
            sql.append(" AND statut = ?");
            params.add(statut);
        }
        sql.append(" ORDER BY nom, prenom");
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            List<Etudiant> res = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(map(rs));
                }
            }
            return res;
        } catch (SQLException e) {
            throw new DataAccessException("Lecture des étudiants impossible", e);
        }
    }

    public void inserer(Etudiant e) {
        String sql = "INSERT INTO etudiant (nom, prenom, email, telephone, adresse, filiere, niveau, competences, "
                + "statut, date_inscription, matricule) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        ecrire(sql, e);
    }

    public void mettreAJour(Etudiant e) {
        String sql = "UPDATE etudiant SET nom = ?, prenom = ?, email = ?, telephone = ?, adresse = ?, filiere = ?, "
                + "niveau = ?, competences = ?, statut = ?, date_inscription = ? WHERE matricule = ?";
        ecrire(sql, e);
    }

    public boolean supprimer(String matricule) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM etudiant WHERE matricule = ?")) {
            ps.setString(1, matricule);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Suppression de l'étudiant impossible", e);
        }
    }

    private void ecrire(String sql, Etudiant e) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, e.getNom());
            ps.setString(2, e.getPrenom());
            ps.setString(3, e.getEmail());
            ps.setString(4, e.getTelephone());
            ps.setString(5, e.getAdresse());
            ps.setString(6, e.getFiliere());
            ps.setString(7, e.getNiveau());
            ps.setString(8, e.getCompetences());
            ps.setString(9, e.getStatut());
            ps.setDate(10, Date.valueOf(e.getDateInscription()));
            ps.setString(11, e.getMatricule());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Écriture de l'étudiant impossible", ex);
        }
    }

    private static Etudiant map(ResultSet rs) throws SQLException {
        Etudiant e = new Etudiant();
        e.setMatricule(rs.getString("matricule"));
        e.setNom(rs.getString("nom"));
        e.setPrenom(rs.getString("prenom"));
        e.setEmail(rs.getString("email"));
        e.setTelephone(rs.getString("telephone"));
        e.setAdresse(rs.getString("adresse"));
        e.setFiliere(rs.getString("filiere"));
        e.setNiveau(rs.getString("niveau"));
        e.setCompetences(rs.getString("competences"));
        e.setStatut(rs.getString("statut"));
        e.setDateInscription(rs.getDate("date_inscription").toLocalDate());
        return e;
    }
}
