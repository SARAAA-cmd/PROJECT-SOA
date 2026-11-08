package com.universite.stages.rest.notifications;

import com.universite.stages.common.Database;
import com.universite.stages.common.Database.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accès à la table notification (journal des envois). */
public class NotificationDao {

    public Notification inserer(Notification n) {
        String sql = "INSERT INTO notification (type, destinataire_type, destinataire_id, email, sujet, message, canal, "
                + "statut, candidature_id, offre_id, date_envoi, lue) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, n.getType());
            ps.setString(2, n.getDestinataireType());
            ps.setString(3, n.getDestinataireId());
            ps.setString(4, n.getEmail());
            ps.setString(5, n.getSujet());
            ps.setString(6, n.getMessage());
            ps.setString(7, n.getCanal());
            ps.setString(8, n.getStatut());
            ps.setObject(9, n.getCandidatureId());
            ps.setObject(10, n.getOffreId());
            ps.setTimestamp(11, Timestamp.valueOf(n.getDateEnvoi()));
            ps.setBoolean(12, n.isLue());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                k.next();
                n.setId(k.getLong(1));
            }
            return n;
        } catch (SQLException e) {
            throw new DataAccessException("Enregistrement de la notification impossible", e);
        }
    }

    public Optional<Notification> trouver(long id) {
        List<Notification> l = requete("SELECT * FROM notification WHERE id = ?", id);
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public List<Notification> rechercher(String destinataireType, String destinataireId, Long candidatureId, String type) {
        StringBuilder sql = new StringBuilder("SELECT * FROM notification WHERE 1=1");
        List<Object> p = new ArrayList<>();
        if (destinataireType != null) {
            sql.append(" AND destinataire_type = ?");
            p.add(destinataireType);
        }
        if (destinataireId != null) {
            sql.append(" AND destinataire_id = ?");
            p.add(destinataireId);
        }
        if (candidatureId != null) {
            sql.append(" AND candidature_id = ?");
            p.add(candidatureId);
        }
        if (type != null) {
            sql.append(" AND type = ?");
            p.add(type);
        }
        sql.append(" ORDER BY id DESC");
        return requete(sql.toString(), p.toArray());
    }

    public boolean rappelDejaEnvoye(String matricule, long offreId) {
        return !requete("SELECT * FROM notification WHERE type = 'RAPPEL_DATE_LIMITE' AND destinataire_id = ? "
                + "AND offre_id = ?", matricule, offreId).isEmpty();
    }

    public void marquerLue(long id) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE notification SET lue = TRUE WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Mise à jour de la notification impossible", e);
        }
    }

    private List<Notification> requete(String sql, Object... params) {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            List<Notification> res = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Notification n = new Notification();
                    n.setId(rs.getLong("id"));
                    n.setType(rs.getString("type"));
                    n.setDestinataireType(rs.getString("destinataire_type"));
                    n.setDestinataireId(rs.getString("destinataire_id"));
                    n.setEmail(rs.getString("email"));
                    n.setSujet(rs.getString("sujet"));
                    n.setMessage(rs.getString("message"));
                    n.setCanal(rs.getString("canal"));
                    n.setStatut(rs.getString("statut"));
                    long cid = rs.getLong("candidature_id");
                    n.setCandidatureId(rs.wasNull() ? null : cid);
                    long oid = rs.getLong("offre_id");
                    n.setOffreId(rs.wasNull() ? null : oid);
                    n.setDateEnvoi(rs.getTimestamp("date_envoi").toLocalDateTime());
                    n.setLue(rs.getBoolean("lue"));
                    res.add(n);
                }
            }
            return res;
        } catch (SQLException e) {
            throw new DataAccessException("Lecture des notifications impossible", e);
        }
    }
}
