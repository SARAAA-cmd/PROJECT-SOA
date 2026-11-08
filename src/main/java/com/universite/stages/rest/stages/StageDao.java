package com.universite.stages.rest.stages;

import com.universite.stages.common.Database;
import com.universite.stages.common.Database.DataAccessException;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accès aux tables stage, compte_rendu et evaluation. */
public class StageDao {

    public Stage inserer(Stage s) {
        String sql = "INSERT INTO stage (candidature_id, matricule, nom_etudiant, offre_id, titre_offre, entreprise_id, "
                + "nom_entreprise, sujet, date_debut, date_fin, tuteur_entreprise, encadrant_academique, statut, "
                + "date_creation) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, s.getCandidatureId());
            ps.setString(2, s.getMatricule());
            ps.setString(3, s.getNomEtudiant());
            ps.setLong(4, s.getOffreId());
            ps.setString(5, s.getTitreOffre());
            ps.setLong(6, s.getEntrepriseId());
            ps.setString(7, s.getNomEntreprise());
            ps.setString(8, s.getSujet());
            ps.setDate(9, Date.valueOf(s.getDateDebut()));
            ps.setDate(10, Date.valueOf(s.getDateFin()));
            ps.setString(11, s.getTuteurEntreprise());
            ps.setString(12, s.getEncadrantAcademique());
            ps.setString(13, s.getStatut());
            ps.setTimestamp(14, Timestamp.valueOf(s.getDateCreation()));
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                s.setId(k.getLong(1));
            }
            return s;
        } catch (SQLException e) {
            throw new DataAccessException("Création du stage impossible", e);
        }
    }

    public Optional<Stage> trouver(long id) {
        List<Stage> l = requete("SELECT * FROM stage WHERE id = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public Optional<Stage> parCandidature(long candidatureId) {
        List<Stage> l = requete("SELECT * FROM stage WHERE candidature_id = ?", candidatureId);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public List<Stage> rechercher(String matricule, Long entrepriseId, String statut) {
        StringBuilder sql = new StringBuilder("SELECT * FROM stage WHERE 1=1");
        List<Object> p = new ArrayList<>();
        if (matricule != null) {
            sql.append(" AND matricule = ?");
            p.add(matricule);
        }
        if (entrepriseId != null) {
            sql.append(" AND entreprise_id = ?");
            p.add(entrepriseId);
        }
        if (statut != null) {
            sql.append(" AND statut = ?");
            p.add(statut);
        }
        sql.append(" ORDER BY date_debut DESC");
        return requete(sql.toString(), p.toArray());
    }

    public void majStatut(long id, String statut) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE stage SET statut = ? WHERE id = ?")) {
            ps.setString(1, statut);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Mise à jour du stage impossible", e);
        }
    }

    public CompteRendu ajouterCompteRendu(CompteRendu cr) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO compte_rendu (stage_id, periode, contenu, auteur, date_depot) VALUES (?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, cr.stageId());
            ps.setString(2, cr.periode());
            ps.setString(3, cr.contenu());
            ps.setString(4, cr.auteur());
            ps.setTimestamp(5, Timestamp.valueOf(cr.dateDepot()));
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return new CompteRendu(k.getLong(1), cr.stageId(), cr.periode(), cr.contenu(), cr.auteur(), cr.dateDepot());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Enregistrement du compte rendu impossible", e);
        }
    }

    public List<CompteRendu> comptesRendus(long stageId) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM compte_rendu WHERE stage_id = ? ORDER BY id")) {
            ps.setLong(1, stageId);
            List<CompteRendu> res = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(new CompteRendu(rs.getLong("id"), rs.getLong("stage_id"), rs.getString("periode"),
                            rs.getString("contenu"), rs.getString("auteur"), rs.getTimestamp("date_depot").toLocalDateTime()));
                }
            }
            return res;
        } catch (SQLException e) {
            throw new DataAccessException("Lecture des comptes rendus impossible", e);
        }
    }

    public Evaluation ajouterEvaluation(Evaluation ev) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO evaluation (stage_id, type, evaluateur, note, appreciation, date_evaluation) "
                             + "VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, ev.stageId());
            ps.setString(2, ev.type());
            ps.setString(3, ev.evaluateur());
            ps.setBigDecimal(4, ev.note());
            ps.setString(5, ev.appreciation());
            ps.setTimestamp(6, Timestamp.valueOf(ev.dateEvaluation()));
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                return new Evaluation(k.getLong(1), ev.stageId(), ev.type(), ev.evaluateur(), ev.note(),
                        ev.appreciation(), ev.dateEvaluation());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Enregistrement de l'évaluation impossible", e);
        }
    }

    public List<Evaluation> evaluations(long stageId) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM evaluation WHERE stage_id = ? ORDER BY id")) {
            ps.setLong(1, stageId);
            List<Evaluation> res = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(new Evaluation(rs.getLong("id"), rs.getLong("stage_id"), rs.getString("type"),
                            rs.getString("evaluateur"), rs.getBigDecimal("note"), rs.getString("appreciation"),
                            rs.getTimestamp("date_evaluation").toLocalDateTime()));
                }
            }
            return res;
        } catch (SQLException e) {
            throw new DataAccessException("Lecture des évaluations impossible", e);
        }
    }

    private List<Stage> requete(String sql, Object... params) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            List<Stage> res = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Stage s = new Stage();
                    s.setId(rs.getLong("id"));
                    s.setCandidatureId(rs.getLong("candidature_id"));
                    s.setMatricule(rs.getString("matricule"));
                    s.setNomEtudiant(rs.getString("nom_etudiant"));
                    s.setOffreId(rs.getLong("offre_id"));
                    s.setTitreOffre(rs.getString("titre_offre"));
                    s.setEntrepriseId(rs.getLong("entreprise_id"));
                    s.setNomEntreprise(rs.getString("nom_entreprise"));
                    s.setSujet(rs.getString("sujet"));
                    s.setDateDebut(rs.getDate("date_debut").toLocalDate());
                    s.setDateFin(rs.getDate("date_fin").toLocalDate());
                    s.setTuteurEntreprise(rs.getString("tuteur_entreprise"));
                    s.setEncadrantAcademique(rs.getString("encadrant_academique"));
                    s.setStatut(rs.getString("statut"));
                    s.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
                    res.add(s);
                }
            }
            return res;
        } catch (SQLException e) {
            throw new DataAccessException("Lecture des stages impossible", e);
        }
    }
}
