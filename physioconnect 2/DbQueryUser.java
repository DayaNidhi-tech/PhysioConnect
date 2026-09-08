import java.sql.*;
public class DbQueryUser {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/Physiology?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        String user = "root";
        String pass = "root123";
        String email = args.length > 0 ? args[0] : "testpatient_auth@example.com";
        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            String sql = "SELECT user_id, full_name, email, role, is_active, is_email_verified, created_at FROM users WHERE email = ?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("FOUND: true");
                        System.out.println("user_id:"+rs.getLong("user_id"));
                        System.out.println("full_name:"+rs.getString("full_name"));
                        System.out.println("email:"+rs.getString("email"));
                        System.out.println("role:"+rs.getString("role"));
                        System.out.println("is_active:"+rs.getBoolean("is_active"));
                        System.out.println("is_email_verified:"+rs.getBoolean("is_email_verified"));
                        System.out.println("created_at:"+rs.getTimestamp("created_at"));
                    } else {
                        System.out.println("FOUND: false");
                    }
                }
            }
        }
    }
}
