package net.akaritakai.stream.chat;

import io.vertx.core.json.JsonObject;
import org.quartz.utils.DBConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.Function;
import org.sqlite.SQLiteConnection;

import java.io.Writer;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class EmojiStore {

    private static final Logger LOG = LoggerFactory.getLogger(EmojiStore.class);

    private final SoftHashMap<String, String> _emojiCache;
    private final SoftHashMap<String, Boolean>_missCache;

    public EmojiStore() {
        _emojiCache = new SoftHashMap<>();
        _missCache = new SoftHashMap<>();
    }


    public static boolean isCustomEmoji(String token) {
        return token.length() > 2 && token.startsWith(":") && token.endsWith(":");
    }

    private Connection getConnection() throws SQLException {
        return DBConnectionManager.getInstance().getConnection("myDS");
    }

    public synchronized Emoji findCustomEmoji(final String emoji) {
        if (!isCustomEmoji(emoji)) {
            return null;
        }
        ArrayList<String> ref = new ArrayList<>();
        for (boolean redo = true;;) {
            String token = emoji;
            String url;
            //noinspection LoopStatementThatDoesntLoop
            for (; ; ) {
                if ((url = _emojiCache.get(token)) == null) {
                    int sep = token.indexOf("::");
                    if (sep < 1) {
                        if (_missCache.containsKey(token)) {
                            return null;
                        }
                        // fetch from db
                        break;
                    }
                    token = token.substring(0, sep + 1);
                    if ((url = _emojiCache.get(token)) == null) {
                        if (_missCache.containsKey(token)) {
                            return null;
                        }
                        // try fetch from db;
                        break;
                    }
                }
                return new Emoji(token, url);
            }
            if (!redo || emoji.contains("%") || emoji.contains("'") || emoji.contains("\"")) {
                LOG.info("no hit for {}", emoji);
                return null;
            }
            redo = false;
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT EMOJI_NAME,EMOJI_URL FROM EMOJIS WHERE EMOJI_NAME LIKE ?"
                 )) {
                stmt.setString(1, token + "%");
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        LOG.info("Record cache miss for {}", token);
                        _missCache.put(token, true);
                        return null;
                    }
                    do {
                        String key = rs.getString(1);
                        String value = rs.getString(2);
                        LOG.info("Found emoji for {}", key);

                        _emojiCache.put(key, value);

                        ref.add(key);
                    } while (rs.next());
                }
            } catch (SQLException ex) {
                LOG.warn("findCustomEmoji {}", ref.size() , ex);
            }
        }
    }

    public List<Emoji> listEmojis(String regexp, int limit) {
        ArrayList<Emoji> list = new ArrayList<>();
        try (Connection conn = getConnection()) {
            for (;;) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT EMOJI_NAME,EMOJI_URL FROM EMOJIS WHERE EMOJI_NAME REGEXP ? LIMIT " + limit
                )) {
                    stmt.setString(1, regexp.trim());
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            list.add(new Emoji(rs.getString(1), rs.getString(2)));
                        }
                        return list;
                    }
                } catch (SQLException ex) {
                    if (ex.getCause() != null && ex.getCause().getMessage().contains("no such function: REGEXP")) {
                        Function.create(conn.unwrap(SQLiteConnection.class), "REGEXP", new Function() {

                            @Override
                            protected void xFunc() throws SQLException {
                                String expression = value_text(0);
                                String value = value_text(1);
                                if (value == null)
                                    value = "";

                                Pattern pattern = Pattern.compile(expression);
                                result(pattern.matcher(value).find() ? 1 : 0);
                            }
                        });
                    } else {
                        throw ex;
                    }
                }
            }
        } catch (Exception ex) {
            LOG.warn("listEmojis", ex);
            return Collections.emptyList();
        }
    }

    public synchronized void setCustomEmoji(String key, String url) {
        key = key.trim();
        url = url.trim();
        if (isCustomEmoji(key) && !url.isBlank()) {
            String oldValue = _emojiCache.put(key, url);
            if (oldValue == null) {
                _missCache.remove(key);
            } else if (oldValue.equals(url)) {
                // no change
                return;
            }
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement("REPLACE INTO EMOJIS VALUES (?,?) ")) {
                stmt.setString(1, key);
                stmt.setString(2, url);
                stmt.executeUpdate();
            } catch (SQLException ex) {
                LOG.warn("setCustomEmoji", ex);
            }
        } else {
            throw new IllegalArgumentException("emoji starts and ends with a colon");
        }
    }

    public void exportTo(Writer out) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT EMOJI_NAME,EMOJI_URL FROM EMOJIS")) {
            out.write("{\n");
            while (rs.next()) {
                String key = rs.getString(1);
                key = key.substring(1, key.length() - 1); // trim off the start and end colons
                String value = JsonObject.of(key, rs.getString(2)).toString();
                value = value.substring(1, value.length() - 1);
                out.write(value);out.write("\n");
            }
            out.write("}\n");
        } catch (Exception ex) {
            LOG.warn("setCustomEmoji", ex);
        }
    }
}
