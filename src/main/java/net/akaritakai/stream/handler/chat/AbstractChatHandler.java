package net.akaritakai.stream.handler.chat;

import io.vertx.core.Vertx;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.chat.ChatManagerMBean;
import net.akaritakai.stream.handler.AbstractBlockingHandler;
import net.akaritakai.stream.scheduling.Utils;

import javax.management.ObjectName;

abstract class AbstractChatHandler<REQUEST> extends AbstractBlockingHandler<REQUEST> {

    private final ObjectName _chatObjectName;

    protected AbstractChatHandler(Class<REQUEST> requestClass, ObjectName chatObjectName, Vertx vertx, CheckAuth checkAuth) {
        super(requestClass, vertx, checkAuth);
        _chatObjectName = chatObjectName;
    }

    protected ChatManagerMBean chatManager() {
        return Utils.beanProxy(_chatObjectName, ChatManagerMBean.class);
    }
}
