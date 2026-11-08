package com.universite.stages.rest.candidatures;

import com.universite.stages.common.Database;
import com.universite.stages.common.Database.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accès aux tables candidature et candidature_historique. */
public class CandidatureDao {

    public Optional<Candidature> trouver(long id) {
        List<Candidature> l = requete("SELECT * FROM candidature WHERE id = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public List<Candidature> rechercher(String matricule, Long offreId, Long entrepriseId, String statut) {
        StringBuilder sql = new StringBuilder("SELECT * FROM candidature WHERE 1=1");
        List<Object> p = new ArrayList<>();
        if (matricule != null) {
            sql.append(" AND matricule = ?");
            p.add(matricule);
        }
        if (offreId != null) {
            sql.append(" AND offre_id = ?");
            p.add(offreId);
        }
        if (entrepriseId != null) {
            sql.append(" AND entreprise_id = ?");
            p.add(entrepriseId);
        }
        if (statut != null) {
            sql.append(" AND statut = ?");
            p.add(statut);
        }
        sql.append(" ORDER BY date_depot DESC");
        return requete(sql.toString(), p.toArray());
    }

    /** Nombre de candidatures en cours (déposées ou en évaluation) d'un étudiant. */
    public int nombreActives(String matricule) {
        return requete("SELECT * FROM candidature WHERE matricule = ? AND statut IN ('DEPOSEE', 'EN_COURS_EVALUATION')",
                matricule).size();
    }

    /** Candidature non annulée existante pour le même couple étudiant / offre. */
    public Optional<Candidature> doublon(String matricule, long offreId) {
        List<Candidature> l = requete("SELECT * FROM candidature WHERE matricule = ? AND offre_id = ? AND statut <> 'ANNULEE'",
                matricule, offreId);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public Candidature inserer(Candidature c) {
        String sql = "INSERT INTO candidature (matricule, nom_etudiant, offre_id, titre_offre, entreprise_id, "
                + "lettre_motivation, cv_url, score_compatibilite, statut, motif, process_instance_id, date_depot, "
                + "date_mise_a_jour) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getMatricule());
            ps.setString(2, c.getNomEtudiant());
            ps.setLong(3, c.getOffreId());
            ps.setString(4, c.getTitreOffre());
            ps.setObject(5, c.getEntrepriseId());
            ps.setString(6, c.getLettreMotivation());
            ps.setString(7, c.getCvUrl());
            ps.setObject(8, c.getScoreCompatibilite());
            ps.setString(9, c.getStatut());
            ps.setString(10, c.getMotif());
            ps.setString(11, c.getProcessInstanceId());
            ps.setTimestamp(12, Timestamp.valueOf(c.getDateDepot()));
            ps.setTimestamp(13, Timestamp.valueOf(c.getDateMiseAJour()));
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                c.setId(k.getLong(1));
            }
            return c;
        } catch (SQLException e) {
            throw new DataAccessException("Création de la candidature impossible", e);
        }
    }

    public void mettreAJour(Candidature c) {
        String sql = "UPDATE candidature SET lettre_motivation = ?, cv_url = ?, statut = ?, motif = ?, "
                + "process_instance_id = ?, date_mise_a_jour = ? WHERE id = ?";
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, c.getLettreMotivation());
            ps.setString(2, c.getCvUrl());
            ps.setString(3, c.getStatut());
            ps.setString(4, c.getMotif());
            ps.setString(5, c.getProcessInstanceId());
            ps.setTimestamp(6, Timestamp.valueOf(c.getDateMiseAJour()));
            ps.setLong(7, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Mise à jour de la candidature impossible", e);
        }
    }

    public void historiser(long candidatureId, String ancien, String nouveau, String commentaire, String auteur) {
        String sql = "INSERT INTO candidature_historique (candidature_id, ancien_statut, nouveau_statut, commentaire, "
                + "auteur, date_changement) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, candidatureId);
            ps.setString(2, ancien);
            ps.setString(3, nouveau);
            ps.setString(4, commentaire);
            ps.setString(5, auteur);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Historisation impossible", e);
        }
    }

    public List<ChangementStatut> historique(long candidatureId) {
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     "SELECT * FROM candidature_historique WHERE candidature_id = ? ORDER BY id")) {
            ps.setLong(1, candidatureId);
            List<ChangementStatut> res = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(new ChangementStatut(rs.getString("ancien_statut"), rs.getString("nouveau_statut"),
                            rs.getString("commentaire"), rs.getString("auteur"),
                            rs.getTimestamp("date_changement").toLocalDateTime()));
                }
            }
            return res;
        } catch (SQLException e) {
            throw new DataAccessException("Lecture de l'historique impossible", e);
        }
    }

    private List<Candidature> requete(String sql, Object... params) {
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            List<Candidature> res = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(map(rs));
                }
            }
            return res;
        } catch (SQLException e) {
            throw new DataAccessException("Lecture des candidatures impossible", e);
        }
    }

    private static Candidature map(ResultSet rs) throws SQLException {
        Candidature c = new Candidature();
        c.setId(rs.getLong("id"));
        c.setMatricule(rs.getString("matricule"));
        c.setNomEtudiant(rs.getString("nom_etudiant"));
        c.setOffreId(rs.getLong("offre_id"));
        c.setTitreOffre(rs.getString("titre_offre"));
        long ent = rs.getLong("entreprise_id");
        c.setEntrepriseId(rs.wasNull() ? null : ent);
        c.setLettreMotivation(rs.getString("lettre_motivation"));
        c.setCvUrl(rs.getString("cv_url"));
        int score = rs.getInt("score_compatibilite");
        c.setScoreCompatibilite(rs.wasNull() ? null : score);
        c.setStatut(rs.getString("statut"));
        c.setMotif(rs.getString("motif"));
        c.setProcessInstanceId(rs.getString("process_instance_id"));
        c.setDateDepot(rs.getTimestamp("date_depot").toLocalDateTime());
        c.setDateMiseAJour(rs.getTimestamp("date_mise_a_jour").toLocalDateTime());
        return c;
    }
}
