package dev.foxikle.mainproxyplugin.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.foxikle.mainproxyplugin.MainProxy;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Database {
    private final MainProxy plugin;

    private final String host;
    private final String port;
    private final String database;
    private final String username;
    private final String password;
    private Connection connection;

    public Database(MainProxy plugin) {
        this.plugin = plugin;
        this.host = plugin.getConfig().getString("sqlHost");
        this.port = plugin.getConfig().getString("sqlPort");
        this.database = plugin.getConfig().getString("sqlName");
        this.username = plugin.getConfig().getString("sqlUsername");
        this.password = plugin.getConfig().getString("sqlPassword");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch(ClassNotFoundException e) {
            plugin.getLogger().error("Failed to load database driver");
            e.printStackTrace();
        }
    }


    public boolean isConnected() {
        return (connection != null);
    }

    public void connect() throws ClassNotFoundException, SQLException {
        if (!isConnected())
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false", username, password);
    }

    public void disconnect() {
        if (isConnected()) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().error("An error occoured whilst disconnecting from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
            }
        }
    }

    private Connection getConnection() {
        return connection;
    }

    public void createFriendsTable() {
        PreparedStatement ps;
        try {
            ps = getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS webnetfriends (uuid VARCHAR(36), friends TEXT, PRIMARY KEY(uuid))");
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
    }

    public void createNameTable() {
        PreparedStatement ps;
        try {
            ps = getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS webnetnames (uuid VARCHAR(36), name VARCHAR(16), PRIMARY KEY(uuid))");
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error("An error occoured whilst fetching data from the database. Please report the following stacktrace to Foxikle: \n" + Arrays.toString(e.getStackTrace()));
        }
    }

    public List<UUID> getFriends(UUID uuid) {
        List<UUID> returnme = new ArrayList<>();
        PreparedStatement ps;
        try {
            ps = getConnection().prepareStatement("SELECT friends FROM webnetfriends WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet set = ps.executeQuery();
            if(set.next()){
                returnme = new Gson().fromJson(set.getString("friends"), new TypeToken<List<UUID>>(){}.getType());
            }
            return returnme;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setFriends(UUID uuid, List<UUID> friends) {
        plugin.getLogger().error(new Gson().toJson(friends));
        PreparedStatement ps;
        try {
            ps = getConnection().prepareStatement("UPDATE webnetfriends SET friends = ? WHERE uuid = ?");
            ps.setString(1, new Gson().toJson(friends));
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addName(UUID uuid, String name) {
        PreparedStatement ps;
        try {
            ps = connection.prepareStatement("INSERT IGNORE INTO webnetnames (uuid, name) VALUES (?,?)");
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        PreparedStatement ps1;
        try {
            ps1 = connection.prepareStatement("INSERT IGNORE INTO webnetfriends (uuid, friends) VALUES (?,?)");
            ps1.setString(1, uuid.toString());
            ps1.setString(2, new Gson().toJson(new ArrayList<>()));
            ps1.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getName(UUID uuid){
        PreparedStatement ps;
        try {
            ps = connection.prepareStatement("SELECT name FROM webnetnames WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return "ERROR!";
    }

    public UUID getUUID(String name){
        PreparedStatement ps;
        try {
            ps = connection.prepareStatement("SELECT uuid FROM webnetnames WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                try {
                    return UUID.fromString(rs.getString("uuid"));
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }
}
