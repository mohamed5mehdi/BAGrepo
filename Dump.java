import java.sql.*;
public class Dump {
    public static void main(String[] args) throws Exception {
        Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/procurement_db", "mehdi", "0660438081");
        ResultSet rs = c.createStatement().executeQuery("SELECT id, designation, statut, categorie, budget_famille_id FROM demande_achat_interne ORDER BY id ASC");
        while(rs.next()) {
            System.out.println("ID: " + rs.getInt(1) + " | Desig: " + rs.getString(2) + " | Statut: " + rs.getString(3) + " | Cat: " + rs.getString(4) + " | FamID: " + rs.getInt(5));
        }
    }
}
