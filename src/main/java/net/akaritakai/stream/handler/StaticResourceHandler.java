package net.akaritakai.stream.handler;

import io.vertx.core.Context;
import io.vertx.core.MultiMap;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.*;
import io.vertx.core.http.impl.HttpUtils;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.impl.URIDecoder;
import io.vertx.ext.web.Http2PushMapping;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.impl.Utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpResponseStatus.*;

public class StaticResourceHandler implements StaticHandler {
    private static final Logger LOG = LoggerFactory.getLogger(StaticResourceHandler.class);

    private final Context _context;

    private long _maxAgeSeconds = DEFAULT_MAX_AGE_SECONDS; // One day
    private boolean _includeHidden = DEFAULT_INCLUDE_HIDDEN;
    private static final ConcurrentMap<String, URL> RESOURCE_MAP = new ConcurrentHashMap<>();
    private Map<String, FileProps> _propsMap = new HashMap<>();
    private String _webRoot = DEFAULT_WEB_ROOT;
    private boolean _sendVaryHeader = DEFAULT_SEND_VARY_HEADER;
    private boolean _rangeSupport = DEFAULT_RANGE_SUPPORT;
    private String _defaultContentEncoding = Charset.defaultCharset().name();
    private Set<String> _compressedMediaTypes = Collections.emptySet();
    private Set<String> _compressedFileSuffixes = Collections.emptySet();

    public StaticResourceHandler(Context context) {
        _context = context;
    }

    @Override
    public StaticHandler setAllowRootFileSystemAccess(boolean allowRootFileSystemAccess) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StaticHandler setWebRoot(String webRoot) {
        _webRoot = webRoot;
        return this;
    }

    @Override
    public StaticHandler setFilesReadOnly(boolean readOnly) {
        if (!readOnly) {
            throw new UnsupportedOperationException();
        }
        return this;
    }

    @Override
    public StaticHandler setMaxAgeSeconds(long maxAgeSeconds) {
        if (maxAgeSeconds < 0) {
            throw new IllegalArgumentException("timeout must be >= 0");
        }
        _maxAgeSeconds = maxAgeSeconds;
        return this;
    }

    @Override
    public StaticHandler setCachingEnabled(boolean enabled) {
        return this;
    }

    @Override
    public StaticHandler setDirectoryListing(boolean directoryListing) {
        if (directoryListing) {
            throw new UnsupportedOperationException();
        }
        return this;
    }

    @Override
    public StaticHandler setIncludeHidden(boolean includeHidden) {
        _includeHidden = includeHidden;
        return this;
    }

    @Override
    public StaticHandler setCacheEntryTimeout(long timeout) {
        return this;
    }

    @Override
    public StaticHandler setIndexPage(String indexPage) {
        return this;
    }

    @Override
    public StaticHandler setMaxCacheSize(int maxCacheSize) {
        return this;
    }

    @Override
    public StaticHandler setHttp2PushMapping(List<Http2PushMapping> http2PushMappings) {
        return this;
    }

    @Override
    public StaticHandler skipCompressionForMediaTypes(Set<String> mediaTypes) {
        if (mediaTypes != null) {
            _compressedMediaTypes = new HashSet<>(mediaTypes);
        }
        return this;
    }

    @Override
    public StaticHandler skipCompressionForSuffixes(Set<String> fileSuffixes) {
        if (fileSuffixes != null) {
            _compressedFileSuffixes = new HashSet<>(fileSuffixes);
        }
        return this;
    }

    @Override
    public StaticHandler setAlwaysAsyncFS(boolean alwaysAsyncFS) {
        return this;
    }

    @Override
    public StaticHandler setEnableFSTuning(boolean enableFSTuning) {
        return this;
    }

    @Override
    public StaticHandler setMaxAvgServeTimeNs(long maxAvgServeTimeNanoSeconds) {
        return this;
    }

    @Override
    public StaticHandler setDirectoryTemplate(String directoryTemplate) {
        return this;
    }

    @Override
    public StaticHandler setEnableRangeSupport(boolean enableRangeSupport) {
        _rangeSupport = enableRangeSupport;
        return this;
    }

    @Override
    public StaticHandler setSendVaryHeader(boolean varyHeader) {
        _sendVaryHeader = varyHeader;
        return this;
    }

    @Override
    public StaticHandler setDefaultContentEncoding(String contentEncoding) {
        _defaultContentEncoding = contentEncoding;
        return this;
    }


    /**
     * Create all required header so content can be cache by Caching servers or
     * Browsers
     *
     * @param request base HttpServerRequest
     * @param props   file properties
     */
    private void writeCacheHeaders(HttpServerRequest request, FileProps props) {

        MultiMap headers = request.response().headers();

        if (true) {
            // We use cache-control and last-modified
            // We *do not use* etags and expires (since they do the same thing - redundant)
            Utils.addToMapIfAbsent(headers, HttpHeaders.CACHE_CONTROL, "public, immutable, max-age=" + _maxAgeSeconds);
            Utils.addToMapIfAbsent(headers, HttpHeaders.LAST_MODIFIED, Utils.formatRFC1123DateTime(props.lastModifiedTime()));
            // We send the vary header (for intermediate caches)
            // (assumes that most will turn on compression when using static handler)
            if (_sendVaryHeader && request.headers().contains(HttpHeaders.ACCEPT_ENCODING)) {
                Utils.addToMapIfAbsent(headers, HttpHeaders.VARY, "accept-encoding");
            }
        }
        // date header is mandatory
        headers.set("date", Utils.formatRFC1123DateTime(System.currentTimeMillis()));
    }

    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();

        if (request.method() != HttpMethod.GET && request.method() != HttpMethod.HEAD) {
            if (LOG.isTraceEnabled())
                LOG.trace("Not GET or HEAD so ignoring request");
            context.next();
        } else {
            if (!request.isEnded()) {
                request.pause();
            }
            // decode URL path
            String uriDecodedPath = URIDecoder.decodeURIComponent(context.normalizedPath(), false);
            // if the normalized path is null it cannot be resolved
            if (uriDecodedPath == null) {
                LOG.warn("Invalid path: " + context.request().path());
                context.next();
                return;
            }
            // will normalize and handle all paths as UNIX paths
            String path = HttpUtils.removeDots(uriDecodedPath.replace('\\', '/'));

            // Access fileSystem once here to be safe
            FileSystem fs = context.vertx().fileSystem();

            sendStatic(
                    context,
                    fs,
                    path);
        }

    }

    /**
     * Can be called recursive for index pages
     */
    private void sendStatic(RoutingContext context, FileSystem fileSystem, String path) {

        String file = null;

        if (!_includeHidden) {
            file = getFile(path, context);
            int idx = file.lastIndexOf('/');
            String name = file.substring(idx + 1);
            if (name.length() > 0 && name.charAt(0) == '.') {
                // skip
                if (!context.request().isEnded()) {
                    context.request().resume();
                }
                context.next();
                return;
            }
        }

        final String localFile;

        if (file == null) {
            String ctxFile = getFile(path, context);
            localFile = ctxFile;
        } else {
            localFile = file;
        }

        URL url = RESOURCE_MAP.computeIfAbsent(localFile, getClass()::getResource);
        if (url == null) {
            context.next();
            return;
        }
        try {
            URLConnection connection = url.openConnection();
            FileProps fprops = new UrlConnectionProps(connection);

            _propsMap.put(path, fprops);

            if (Utils.fresh(context, Utils.secondsFactor(fprops.lastModifiedTime()))) {
                context.response().setStatusCode(NOT_MODIFIED.code()).end();
                return;
            }

            sendFile(context, connection, localFile, fprops);

        } catch (IOException ex) {
            context.fail(ex);
        }
    }


    private String getFile(String path, RoutingContext context) {
        String file = _webRoot + Utils.pathOffset(path, context);
        if (LOG.isTraceEnabled()) {
            LOG.trace("File to serve is " + file);
        }
        return file;
    }

    private static final Pattern RANGE = Pattern.compile("^bytes=(\\d+)-(\\d*)$");

    private void sendFile(RoutingContext context, URLConnection connection, String file, FileProps fileProps) {
        final HttpServerRequest request = context.request();
        final HttpServerResponse response = context.response();

        Long offset = null;
        Long end = null;
        MultiMap headers = null;

        if (response.closed())
            return;

        if (_rangeSupport) {
            // check if the client is making a range request
            String range = request.getHeader("Range");
            // end byte is length - 1
            end = fileProps.size() - 1;

            if (range != null) {
                Matcher m = RANGE.matcher(range);
                if (m.matches()) {
                    try {
                        String part = m.group(1);
                        // offset cannot be empty
                        offset = Long.parseLong(part);
                        // offset must fall inside the limits of the file
                        if (offset < 0 || offset >= fileProps.size()) {
                            throw new IndexOutOfBoundsException();
                        }
                        // length can be empty
                        part = m.group(2);
                        if (part != null && part.length() > 0) {
                            // ranges are inclusive
                            end = Math.min(end, Long.parseLong(part));
                            // end offset must not be smaller than start offset
                            if (end < offset) {
                                throw new IndexOutOfBoundsException();
                            }
                        }
                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                        context.response().putHeader(HttpHeaders.CONTENT_RANGE, "bytes */" + fileProps.size());
                        if (!context.request().isEnded()) {
                            context.request().resume();
                        }
                        context.fail(REQUESTED_RANGE_NOT_SATISFIABLE.code());
                        return;
                    }
                }
            }

            // notify client we support range requests
            headers = response.headers();
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            // send the content length even for HEAD requests
            headers.set(HttpHeaders.CONTENT_LENGTH, Long.toString(end + 1 - (offset == null ? 0 : offset)));
        }

        writeCacheHeaders(request, fileProps);

        if (request.method() == HttpMethod.HEAD) {
            response.end();
        } else {
            if (_rangeSupport && offset != null) {
                // must return content range
                headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + offset + "-" + end + "/" + fileProps.size());
                // return a partial response
                response.setStatusCode(PARTIAL_CONTENT.code());

                final long finalOffset = offset;
                final long finalLength = end + 1 - offset;
                // guess content type
                String contentType = MimeMapping.getMimeTypeForFilename(file);
                if (contentType != null) {
                    if (contentType.startsWith("text")) {
                        response.putHeader(HttpHeaders.CONTENT_TYPE, contentType + ";charset=" + _defaultContentEncoding);
                    } else {
                        response.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
                    }
                }

                _context.runOnContext(v -> {
                    InputStream is;
                    try {
                        is = connection.getInputStream();
                    } catch (IOException ex) {
                        if (!context.request().isEnded()) {
                            context.request().resume();
                        }
                        context.fail(ex);
                        return;
                    }
                    response.send(new AsyncInputStream(context.vertx(), _context, new Range(is, finalOffset, finalLength)), res2 -> {
                        if (res2.failed()) {
                            if (!context.request().isEnded()) {
                                context.request().resume();
                            }
                            context.fail(res2.cause());
                        }
                    });
                });
            } else {
                // guess content type
                String extension = getFileExtension(file);
                String contentType = MimeMapping.getMimeTypeForExtension(extension);
                if (_compressedMediaTypes.contains(contentType) || _compressedFileSuffixes.contains(extension)) {
                    response.putHeader(HttpHeaders.CONTENT_ENCODING, HttpHeaders.IDENTITY);
                }
                if (contentType != null) {
                    if (contentType.startsWith("text")) {
                        response.putHeader(HttpHeaders.CONTENT_TYPE, contentType + ";charset=" + _defaultContentEncoding);
                    } else {
                        response.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
                    }
                }

                _context.runOnContext(v -> {
                    InputStream is;
                    try {
                        is = connection.getInputStream();
                    } catch (IOException ex) {
                        if (!context.request().isEnded()) {
                            context.request().resume();
                        }
                        context.fail(ex);
                        return;
                    }
                    response.send(new AsyncInputStream(context.vertx(), _context, is), res2 -> {
                        if (res2.failed()) {
                            if (!context.request().isEnded()) {
                                context.request().resume();
                            }
                            context.fail(res2.cause());
                        }
                    });
                });
            }
        }
    }

    private String getFileExtension(String file) {
        int li = file.lastIndexOf(46);
        if (li != -1 && li != file.length() - 1) {
            return file.substring(li + 1);
        } else {
            return null;
        }
    }

    private static RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

    private static class UrlConnectionProps implements FileProps {
        private final long creationTime;
        private final long lastModifiedTime;
        private final long size;

        private UrlConnectionProps(URLConnection connection) {
            size = connection.getContentLengthLong();
            lastModifiedTime = connection.getLastModified();
            creationTime = Math.min(lastModifiedTime, runtimeMXBean.getStartTime());
        }

        @Override
        public long creationTime() {
            return creationTime;
        }

        @Override
        public long lastAccessTime() {
            return System.currentTimeMillis();
        }

        @Override
        public long lastModifiedTime() {
            return lastModifiedTime;
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public boolean isOther() {
            return false;
        }

        @Override
        public boolean isRegularFile() {
            return true;
        }

        @Override
        public boolean isSymbolicLink() {
            return false;
        }

        @Override
        public long size() {
            return size;
        }
    }

    private static class Range extends FilterInputStream {
        private long _offset;
        private long _length;


        Range(InputStream inputStream, long offset, long length) {
            super(inputStream);
            _offset = offset;
            _length = length;
        }

        @Override
        public int read() throws IOException {
            skip(0L);
            while (_offset > 0L) {
                int ch = super.read();
                if (ch == -1) {
                    return -1;
                }
                _offset--;
            }
            if (_length > 0) {
                int ch = super.read();
                if (ch != -1) {
                    _length--;
                }
                return ch;
            }
            return -1;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            skip(0L);
            while (_offset > 0L) {
                int ch = super.read();
                if (ch == -1) {
                    return 0;
                }
                _offset--;
            }
            if (len > _length) {
                len = Math.toIntExact(_length);
            }
            int read = super.read(b, off, len);
            if (read > 0) {
                _length -= read;
            }
            return read;
        }

        @Override
        public long skip(long n) throws IOException {
            if (_offset > 0) {
                long skipped = super.skip(_offset);
                _offset -= Math.min(_offset, skipped);
            }
            if (_offset == 0 && n > 0) {
                long skipped = super.skip(Math.min(_length, n));
                _length -= skipped;
                return skipped;
            }
            return 0L;
        }

        @Override
        public int available() throws IOException {
            skip(0);
            return Math.toIntExact(Math.min(_length, super.available()));
        }

        @Override
        public synchronized void mark(int readlimit) {
        }

        @Override
        public synchronized void reset() throws IOException {
            throw new IOException("mark/reset not supported");
        }

        @Override
        public boolean markSupported() {
            return false;
        }
    }
}
