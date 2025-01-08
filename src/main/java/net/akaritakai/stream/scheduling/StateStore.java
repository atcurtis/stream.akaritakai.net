package net.akaritakai.stream.scheduling;

import org.quartz.utils.DBConnectionManager;

import javax.annotation.Nonnull;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class StateStore extends AbstractMap<String, Object> implements StateStoreMBean {

    private static final byte[] EMPTY = new byte[0];

    private transient Set<Entry<String, Object>> _entrySet;

    private Connection getConnection() throws SQLException {
        return DBConnectionManager.getInstance().getConnection("myDS");
    }

    @Override
    @Nonnull
    public Set<Entry<String, Object>> entrySet() {
        if (_entrySet == null) {
            _entrySet = new AbstractSet<Entry<String, Object>>() {
                @Override
                @Nonnull
                public Iterator<Entry<String, Object>> iterator() {
                    ArrayList<Entry<String, byte[]>> entres = new ArrayList<>();
                    try (Connection conn = getConnection();
                         PreparedStatement stmt = conn.prepareStatement("SELECT STATE_NAME,STATE_VALUE FROM STATE_STORE");
                         ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            entres.add(new SimpleImmutableEntry<>(rs.getString(1), rs.getBytes(2)));
                        }
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                    Iterator<Entry<String, byte[]>> it = entres.iterator();
                    return new Iterator<Entry<String, Object>>() {
                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public Entry<String, Object> next() {
                            Entry<String, byte[]> entry = it.next();
                            return new Entry<String, Object>() {
                                @Override
                                public String getKey() {
                                    return entry.getKey();
                                }

                                @Override
                                public Object getValue() {
                                    try {
                                        return deserialize(entry.getValue());
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                @Override
                                public Object setValue(Object value) {
                                    throw new UnsupportedOperationException();
                                }
                            };
                        }
                    };
                }

                @Override
                public int size() {
                    return StateStore.this.size();
                }
            };
        }
        return _entrySet;
    }

    private byte[] serialize(Object object) throws IOException {
        if (object == null) {
            return EMPTY;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
        }
        return baos.toByteArray();
    }

    private Object deserialize(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        } catch (ClassNotFoundException e) {
            return new IOException(e);
        }
    }


    @Override
    public int size() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM STATE_STORE");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        try {
            byte[] bytesValue = serialize(value);
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT STATE_NAME FROM STATE_STORE WHERE STATE_VALUE=? LIMIT 1")) {
                stmt.setBytes(1, bytesValue);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return true;
                    }
                }
            }
        } catch (SQLException | IOException ex) {
            throw new RuntimeException(ex);
        }
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT STATE_NAME FROM STATE_STORE WHERE STATE_NAME=? LIMIT 1")) {
            stmt.setString(1, key.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return true;
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return false;
    }

    @Override
    public Object get(Object key) {
        try {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT STATE_VALUE FROM STATE_STORE WHERE STATE_NAME=?")) {
                stmt.setString(1, key.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return deserialize(rs.getBytes(1));
                    }
                }
            }
        } catch (SQLException | IOException ex) {
            throw new RuntimeException(ex);
        }
        return null;
    }

    @Override
    public Object put(String key, Object value) {
        try {
            byte[] newValue = serialize(value);
            byte[] oldValue;
            try (Connection conn = getConnection();
                 PreparedStatement stmt1 = conn.prepareStatement("SELECT STATE_VALUE FROM STATE_STORE WHERE STATE_NAME=?");
                 PreparedStatement stmt2 = conn.prepareStatement("REPLACE INTO STATE_STORE VALUES (?,?)")) {
                stmt1.setString(1, key);
                stmt2.setString(1, key);
                stmt2.setBytes(2, newValue);
                try (ResultSet rs = stmt1.executeQuery()) {
                    if (rs.next()) {
                        oldValue = rs.getBytes(1);
                    } else {
                        oldValue = EMPTY;
                    }
                }
                if (stmt2.executeUpdate() > 0) {
                    return deserialize(oldValue);
                } else {
                    return value;
                }
            }
        } catch (SQLException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Object remove(Object key) {
        String stringKey = key.toString();
        Object rc;
        try (Connection conn = getConnection();
             PreparedStatement stmt1 = conn.prepareStatement("SELECT STATE_VALUE FROM STATE_STORE WHERE STATE_NAME=?");
             PreparedStatement stmt2 = conn.prepareStatement("DELETE FROM STATE_STORE WHERE STATE_NAME=?")) {
            stmt1.setString(1, stringKey);
            byte[] bytes;
            try (ResultSet rs = stmt1.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                bytes = rs.getBytes(1);
            }
            stmt2.setString(1, stringKey);
            if (stmt2.executeUpdate() == 0) {
                return null;
            } else {
                return deserialize(bytes);
            }
        } catch (SQLException | IOException ex) {
            return null;
        }
    }

    @Override
    public void clear() {
        super.clear();
    }
}
