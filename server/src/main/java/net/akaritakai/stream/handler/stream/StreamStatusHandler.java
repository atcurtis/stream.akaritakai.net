package net.akaritakai.stream.handler.stream;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;
import net.akaritakai.stream.models.stream.StreamState;
import net.akaritakai.stream.streamer.Streamer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.AttributeChangeNotification;


/**
 * Handles websocket requests to "/stream/status"
 */
public class StreamStatusHandler implements Handler<RoutingContext> {
  private static final Logger LOG = LoggerFactory.getLogger(StreamStatusHandler.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final Vertx _vertx;
  private final Streamer _stream;
  private final Set<ServerWebSocket> _sockets = ConcurrentHashMap.newKeySet();

  public StreamStatusHandler(Vertx vertx, Streamer stream) {
    _vertx = vertx;
    _stream = stream;
    _stream.addNotificationListener((notification, handback) -> {
      assert this == handback;
      StreamState streamState;
      switch (notification.getMessage()) {
        case "StreamState":
          streamState = (StreamState) ((AttributeChangeNotification) notification).getNewValue();
          onStateUpdate(streamState);
          break;
      }
    }, null, this);
  }

  @Override
  public void handle(RoutingContext event) {
    event.request().toWebSocket().onFailure(event::fail).onSuccess(socket -> handle(event, socket));
  }

  private void handle(RoutingContext event, ServerWebSocket socket) {
    event.request().resume();
    _sockets.add(socket);
    socket.endHandler(endEvent -> _sockets.remove(socket));
    socket.closeHandler(closeEvent -> _sockets.remove(socket));
    _vertx.runOnContext(upgradeEvent -> {
      try {
        StreamState state = _stream.getState();
        String newState = OBJECT_MAPPER.writeValueAsString(state);
        socket.writeTextMessage(newState);
      } catch (Exception e) {
        LOG.warn("Unable to send state to client. Reason: {}: {}", e.getClass().getCanonicalName(), e.getMessage());
      }
    });
  }

  public void onStateUpdate(StreamState state) {
    try {
      String newState = OBJECT_MAPPER.writeValueAsString(state);
      _sockets.forEach(socket -> _vertx.runOnContext(event -> socket.writeTextMessage(newState)));
    } catch (Exception e) {
      LOG.error("Unable to send status update to clients", e);
    }
  }
}
