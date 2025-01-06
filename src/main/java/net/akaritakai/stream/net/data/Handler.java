package net.akaritakai.stream.net.data;

import com.amazonaws.util.Base16;
import com.amazonaws.util.Base32;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conform to RFC2397 Data URL Scheme
 *
 *        dataurl    := "data:" [ mediatype ] [ ";base64" ] "," data
 *        mediatype  := [ type "/" subtype ] *( ";" parameter )
 *        data       := *urlchar
 *        parameter  := attribute "=" value
 */
public class Handler extends URLStreamHandler {

    private static final Pattern FILE_REGEXP = Pattern.compile(
            "^([a-zA-Z_0-9\\-]+/[a-zA-Z_0-9\\-]+)?" +
                    "(;[a-zA-Z_0-9\\-]+=[a-zA-Z_0-9\\-]+)*" +
                    "(;[a-zA-Z_0-9\\-]+)?" +
                    ",(.*)", Pattern.DOTALL);
    private static final Pattern PARAM_REGEXP = Pattern.compile("^;([a-zA-Z_0-9\\-]+)=([a-zA-Z_0-9\\-]+)");

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        if ("data".equals(u.getProtocol())) {
            return new DataURLConnection(u);
        }
        throw new IOException("unsupported protocol");
    }

    private static class DataURLConnection extends URLConnection {

        private transient byte[] data;
        private transient String contentType;

        protected DataURLConnection(URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
            if (data == null) {
                Matcher m = FILE_REGEXP.matcher(url.getFile());
                if (!m.matches()) {
                    throw new IOException("Unable to decode URI");
                }
                Charset charset = StandardCharsets.ISO_8859_1;
                contentType = m.group(1);

                if (m.group(2) != null) {
                    Matcher p = PARAM_REGEXP.matcher(m.group(2));
                    int pos = 0;
                    String attribute;
                    String value;
                    while (p.find(pos) && m.groupCount() == 2 && (attribute = p.group(1)) != null && (value = p.group(2)) != null) {
                        switch (attribute) {
                            case "charset" -> charset = Charset.forName(value);
                            default -> {}
                        }
                        pos = p.end();
                    }
                    contentType += contentType + m.group(2);
                }

                switch (String.valueOf(m.group(3))) {
                    case ";base64" -> data = Base64.getDecoder().decode(m.group(4));
                    case ";base32" -> data = Base32.decode(m.group(4));
                    case ";base16" -> data = Base16.decode(m.group(4));
                    default -> data = URLDecoder.decode(m.group(4), charset).getBytes(charset);
                }
            }
        }

        @Override
        public String getHeaderField(String name) {
            try {
                connect();
            } catch (IOException e) {
                return null;
            }

            return switch (name.toLowerCase()) {
                case "content-type" -> contentType;
                case "content-length" -> String.valueOf(data.length);
                default -> null;
            };
        }

        @Override
        public InputStream getInputStream() throws IOException {
            connect();
            return new ByteArrayInputStream(data);
        }
    }
}