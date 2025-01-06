package net.akaritakai.stream.net;

import io.netty.util.internal.StringUtil;
import net.akaritakai.stream.net.data.Handler;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class DataUrlStreamHandlerFactory implements URLStreamHandlerFactory {
    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("data".equals(protocol)) {
            return new Handler();
        }
        return null;
    }

    private static final String PKGS = "java.protocol.handler.pkgs";
    private static final String PACKAGE = DataUrlStreamHandlerFactory.class.getPackageName();

    public static void register() {
        //URL.setURLStreamHandlerFactory(new DataUrlStreamHandlerFactory());
        String property = System.getProperty(PKGS);
        ArrayList<String> list = new ArrayList<>(property != null ? Arrays.asList(property.split("\\|")) : Collections.emptyList());
        if (!list.contains(PACKAGE)) {
            list.add(PACKAGE);
            System.setProperty(PKGS, StringUtil.join("|", list).toString());
        }
    }
}
