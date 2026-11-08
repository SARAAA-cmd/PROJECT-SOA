package com.universite.stages.soap.offres;

import com.universite.stages.common.Database;
import com.universite.stages.common.Database.DataAccessException;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accès aux données de la table offre. */
public class OffreDao {

    public Optional<Offre> trouver(long id) {
        List<Offre> l = requete("SELECT * FROM offre WHERE id = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public List<Offre> lister(String statut, Long entrepriseId) {
        StringBuilder sql = new StringBuilder("SELECT * FROM offre WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (statut != null && !statut.isBlank()) {
            sql.append(" AND statut = ?");
            params.add(statut);
        }
        if (entrepriseId != null) {
            sql.append(" AND entreprise_id = ?");
            params.add(entrepriseId);
        }
        sql.append(" ORDER BY date_limite");
        return requete(sql.toString(), params.toArray());
    }

    public List<Offre> disponibles() {
        return requete("SELECT * FROM offre WHERE statut = 'ACTIVE' AND places_disponibles > 0 AND date_limite >= ? "
                + "ORDER BY date_limite", Date.valueOf(LocalDate.now()));
    }

    public List<Offre> expirantAvant(LocalDate limite) {
        return requete("SELECT * FROM offre WHERE statut = 'ACTIVE' AND places_disponibles > 0 "
                + "AND date_limite >= ? AND date_limite <= ? ORDER BY date_limite",
                Date.valueOf(LocalDate.now()), Date.valueOf(limite));
    }

    public Offre inserer(Offre o) {
        String sql = "INSERT INTO offre (entreprise_id, titre, description, competences_requises, duree_mois, date_limite, "
                + "nombre_places, places_disponibles, filieres_ciblees, niveau_minimum, lieu, statut, date_publication) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            remplir(ps, o);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                o.setId(keys.getLong(1));
            }
            return o;
        } catch (SQLException e) {
            throw new DataAccessException("Création de l'offre impossible", e);
        }
    }

    public void mettreAJour(Offre o) {
        String sql = "UPDATE offre SET entreprise_id = ?, titre = ?, description = ?, competences_requises = ?, "
                + "duree_mois = ?, date_limite = ?, nombre_places = ?, places_disponibles = ?, filieres_ciblees = ?, "
                + "niveau_minimum = ?, lieu = ?, statut = ?, date_publication = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            remplir(ps, o);
            ps.setLong(14, o.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Mise à jour de l'offre impossible", e);
        }
    }

    /**
     * Décrément atomique d'une place : la condition places_disponibles &gt; 0
     * dans la clause WHERE empêche toute sur-réservation concurrente.
     */
    public boolean decrementerPlace(long id) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE offre SET places_disponibles = places_disponibles - 1, "
                             + "statut = CASE WHEN places_disponibles - 1 = 0 THEN 'POURVUE' ELSE statut END "
                             + "WHERE id = ? AND places_disponibles > 0")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Réservation de place impossible", e);
        }
    }

    public boolean supprimer(long id) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM offre WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Suppression de l'offre impossible", e);
        }
    }

    private List<Offre> requete(String sql, Object... params) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            List<Offre> res = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(map(rs));
                }
            }
            return res;
        } catch (SQLException e) {
            throw new DataAccessException("Lecture des offres impossible", e);
        }
    }

    private static void remplir(PreparedStatement ps, Offre o) throws SQLException {
        ps.setLong(1, o.getEntrepriseId());
        ps.setString(2, o.getTitre());
        ps.setString(3, o.getDescription());
        ps.setString(4, o.getCompetencesRequises());
        ps.setInt(5, o.getDureeMois());
        ps.setDate(6, Date.valueOf(o.getDateLimite()));
        ps.setInt(7, o.getNombrePlaces());
        ps.setInt(8, o.getPlacesDisponibles());
        ps.setString(9, o.getFilieresCiblees());
        ps.setString(10, o.getNiveauMinimum());
        ps.setString(11, o.getLieu());
        ps.setString(12, o.getStatut());
        ps.setDate(13, Date.valueOf(o.getDatePublication()));
    }

    private static Offre map(ResultSet rs) throws SQLException {
        Offre o = new Offre();
        o.setId(rs.getLong("id"));
        o.setEntrepriseId(rs.getLong("entreprise_id"));
        o.setTitre(rs.getString("titre"));
        o.setDescription(rs.getString("description"));
        o.setCompetencesRequises(rs.getString("competences_requises"));
        o.setDureeMois(rs.getInt("duree_mois"));
        o.setDateLimite(rs.getDate("date_limite").toLocalDate());
        o.setNombrePlaces(rs.getInt("nombre_places"));
        o.setPlacesDisponibles(rs.getInt("places_disponibles"));
        o.setFilieresCiblees(rs.getString("filieres_ciblees"));
        o.setNiveauMinimum(rs.getString("niveau_minimum"));
        o.setLieu(rs.getString("lieu"));
        o.setStatut(rs.getString("statut"));
        o.setDatePublication(rs.getDate("date_publication").toLocalDate());
        return o;
    }
}
