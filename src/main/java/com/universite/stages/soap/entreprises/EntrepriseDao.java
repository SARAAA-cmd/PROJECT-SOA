package com.universite.stages.soap.entreprises;

import com.universite.stages.common.Database;
import com.universite.stages.common.Database.DataAccessException;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accès aux données de la table entreprise. */
public class EntrepriseDao {

    private static final String COLONNES = "raison_sociale, secteur_activite, adresse, ville, email, telephone, "
            + "site_web, contact_nom, contact_fonction, contact_email, contact_telephone, referencee, date_referencement";

    public Optional<Entreprise> trouver(long id) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM entreprise WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lecture de l'entreprise impossible", e);
        }
    }

    public boolean raisonSocialeUtilisee(String raisonSociale, Long saufId) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM entreprise WHERE LOWER(raison_sociale) = LOWER(?) AND id <> ?")) {
            ps.setString(1, raisonSociale);
            ps.setLong(2, saufId == null ? -1 : saufId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lecture de l'entreprise impossible", e);
        }
    }

    public List<Entreprise> lister(String secteur) {
        boolean filtre = secteur != null && !secteur.isBlank();
        String sql = "SELECT * FROM entreprise" + (filtre ? " WHERE secteur_activite = ?" : "") + " ORDER BY raison_sociale";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (filtre) {
                ps.setString(1, secteur);
            }
            List<Entreprise> res = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(map(rs));
                }
            }
            return res;
        } catch (SQLException e) {
            throw new DataAccessException("Lecture des entreprises impossible", e);
        }
    }

    public Entreprise inserer(Entreprise e) {
        String sql = "INSERT INTO entreprise (" + COLONNES + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            remplir(ps, e);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                e.setId(keys.getLong(1));
            }
            return e;
        } catch (SQLException ex) {
            throw new DataAccessException("Création de l'entreprise impossible", ex);
        }
    }

    public void mettreAJour(Entreprise e) {
        String sql = "UPDATE entreprise SET raison_sociale = ?, secteur_activite = ?, adresse = ?, ville = ?, email = ?, "
                + "telephone = ?, site_web = ?, contact_nom = ?, contact_fonction = ?, contact_email = ?, "
                + "contact_telephone = ?, referencee = ?, date_referencement = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            remplir(ps, e);
            ps.setLong(14, e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Mise à jour de l'entreprise impossible", ex);
        }
    }

    public boolean supprimer(long id) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM entreprise WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Suppression de l'entreprise impossible", e);
        }
    }

    private static void remplir(PreparedStatement ps, Entreprise e) throws SQLException {
        ps.setString(1, e.getRaisonSociale());
        ps.setString(2, e.getSecteurActivite());
        ps.setString(3, e.getAdresse());
        ps.setString(4, e.getVille());
        ps.setString(5, e.getEmail());
        ps.setString(6, e.getTelephone());
        ps.setString(7, e.getSiteWeb());
        ps.setString(8, e.getContactNom());
        ps.setString(9, e.getContactFonction());
        ps.setString(10, e.getContactEmail());
        ps.setString(11, e.getContactTelephone());
        ps.setBoolean(12, e.isReferencee());
        if (e.getDateReferencement() != null) {
            ps.setDate(13, Date.valueOf(e.getDateReferencement()));
        } else {
            ps.setNull(13, Types.DATE);
        }
    }

    private static Entreprise map(ResultSet rs) throws SQLException {
        Entreprise e = new Entreprise();
        e.setId(rs.getLong("id"));
        e.setRaisonSociale(rs.getString("raison_sociale"));
        e.setSecteurActivite(rs.getString("secteur_activite"));
        e.setAdresse(rs.getString("adresse"));
        e.setVille(rs.getString("ville"));
        e.setEmail(rs.getString("email"));
        e.setTelephone(rs.getString("telephone"));
        e.setSiteWeb(rs.getString("site_web"));
        e.setContactNom(rs.getString("contact_nom"));
        e.setContactFonction(rs.getString("contact_fonction"));
        e.setContactEmail(rs.getString("contact_email"));
        e.setContactTelephone(rs.getString("contact_telephone"));
        e.setReferencee(rs.getBoolean("referencee"));
        Date d = rs.getDate("date_referencement");
        e.setDateReferencement(d == null ? null : d.toLocalDate());
        return e;
    }
}
