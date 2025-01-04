package net.akaritakai.stream.streamer;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.client.AwsS3Client;
import net.akaritakai.stream.client.FakeAwsS3Client;
import net.akaritakai.stream.config.ConfigData;
import net.akaritakai.stream.exception.StreamStateConflictException;
import net.akaritakai.stream.handler.RouterHelper;
import net.akaritakai.stream.handler.stream.*;
import net.akaritakai.stream.models.stream.StreamEntry;
import net.akaritakai.stream.models.stream.StreamMetadata;
import net.akaritakai.stream.models.stream.StreamState;
import net.akaritakai.stream.models.stream.StreamStateType;
import net.akaritakai.stream.models.stream.request.StreamResumeRequest;
import net.akaritakai.stream.models.stream.request.StreamStartRequest;
import net.akaritakai.stream.scheduling.ScheduleManagerMBean;
import net.akaritakai.stream.scheduling.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import static net.akaritakai.stream.config.GlobalNames.scheduleManagerName;


public class Streamer extends NotificationBroadcasterSupport implements StreamerMBean {
    private static final Logger LOG = LoggerFactory.getLogger(Streamer.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
  private final Vertx _vertx;
  private final AwsS3Client _client;
  private final String _livePlaylistUrl;
  private final AtomicReference<StreamState> _state = new AtomicReference<>(StreamState.OFFLINE);

  private final AtomicInteger _sequenceNumber = new AtomicInteger();

  public Streamer(Vertx vertx, ConfigData config) {
    _vertx = vertx;
    if (config.isDevelopment() || !config.isAwsDirectory()) {
      _client = new FakeAwsS3Client(vertx, config);
    } else {
      _client = new AwsS3Client(vertx, config);
    }
    _livePlaylistUrl = config.getLivePlaylistUrl();
  }

  public static void register(Vertx vertx, RouterHelper router, CheckAuth auth, ObjectName streamerName) {
    router.registerGetHandler("/stream/status", new StreamStatusHandler(vertx));
    router.registerPostApiHandler("/stream/start", new StartCommandHandler(auth, vertx));
    router.registerPostApiHandler("/stream/stop", new StopCommandHandler(auth, vertx));
    router.registerPostApiHandler("/stream/pause", new PauseCommandHandler(auth, vertx));
    router.registerPostApiHandler("/stream/resume", new ResumeCommandHandler(auth, vertx));
    router.registerPostApiHandler("/stream/dir", new DirCommandHandler(auth, vertx));
  }


  static StreamState.StreamStateBuilder builder(StreamState state) {
    return StreamState.builder()
            .status(state.getStatus())
            .live(state.isLive())
            .playlist(state.getPlaylist())
            .mediaName(state.getMediaName())
            .mediaDuration(state.getMediaDuration())
            .startTime(state.getStartTime())
            .endTime(state.getEndTime())
            .seekTime(state.getSeekTime());
  }

  @Override
  public void startStream(@Nonnull StreamStartRequest request) {
    LOG.info("Got request to start the stream: {}", request);
    // Ensure that the stream is not already running
    StreamState state = _state.get();
    if (state == null || state.getStatus() != StreamStateType.OFFLINE) {
      LOG.warn("Can't start the stream: the stream is already running");
      throw new StreamStateConflictException("The stream is already running");
    }

    // Use the request provided delay or none at all
    Duration delay = Optional.ofNullable(request.getDelay()).orElse(Duration.ZERO);
    LOG.info("delay={}", delay);
    // We use the provided start time + delay, or now + delay
    Instant startTime = Optional.ofNullable(request.getStartAt()).orElse(Instant.now()).plus(delay);

    StreamMetadata metadata = CompletableFuture
            .completedFuture(request.getName())
            .thenApply(_client::getMetadata)
            .thenApply(Future::toCompletionStage)
            .thenCompose(Function.identity())
            .join();
    // Create the new state
    StreamState.StreamStateBuilder builder = StreamState.builder()
            .status(StreamStateType.ONLINE)
            .playlist(metadata.getPlaylist())
            .mediaName(Optional.ofNullable(metadata.getName()).orElse("LIVE"))
            .startTime(startTime)
            .live(metadata.isLive());

    if (!metadata.isLive()) {
      // Use the request provided seek time or the default one
      Duration seekTime = Optional.ofNullable(request.getSeekTime()).orElse(Duration.ZERO);
      // We end at startTime + media duration - seek time
      Instant endTime = startTime.plus(metadata.getDuration()).minus(seekTime);

      builder.mediaDuration(metadata.getDuration())
              .seekTime(seekTime)
              .endTime(endTime);
    }

    StreamState newState = builder.build();

    setState(newState);
  }

  @Override
  public void pauseStream() {
    LOG.info("Got request to pause the stream");
    // Ensure that the stream is already running
    StreamState state = _state.get();
    if (state == null || state.getStatus() != StreamStateType.ONLINE) {
      LOG.warn("Can't stop the stream: the stream is not running");
      throw new StreamStateConflictException("The stream is not currently running");
    }

    StreamState.StreamStateBuilder builder = builder(state)
            .status(StreamStateType.PAUSE);

    if (!state.isLive()) {
      // Stream is pre-recorded

      // Determine the virtual start time (start time - seek time)
      Instant startTime = state.getStartTime().minus(state.getSeekTime());
      // Determine the seek time we should pause at (now - virtual start time)
      Duration seekTime = Duration.between(startTime, Instant.now());

      // Create the new state
      builder
              .mediaDuration(state.getMediaDuration())
              .seekTime(seekTime);
    }

    StreamState newState = builder.build();

    setState(newState);
  }

  @Override
  public void resumeStream(@Nonnull StreamResumeRequest request) {
    LOG.info("Got request to resume the stream: {}", request);
    // Ensure that the stream is paused
    StreamState state = _state.get();
    if (state == null || state.getStatus() != StreamStateType.PAUSE) {
      LOG.warn("Can't stop the stream: the stream is not paused");
      throw new StreamStateConflictException("The stream is not currently paused");
    }

    // Use the request provided delay or none at all
    Duration delay = Optional.ofNullable(request.getDelay()).orElse(Duration.ZERO);
    // We start at the provided start time + delay, or now + delay
    Instant startTime = Optional.ofNullable(request.getStartAt()).orElse(Instant.now()).plus(delay);

    StreamState.StreamStateBuilder builder = builder(state)
            .status(StreamStateType.ONLINE)
            .startTime(startTime);

    if (!state.isLive()) {
      // Determine our new seek time (use request seek time or the time we paused at)
      Duration seekTime = Optional.ofNullable(request.getSeekTime()).orElse(state.getSeekTime());
      // We end at startTime + media duration - seek time
      Instant endTime = startTime.plus(state.getMediaDuration()).minus(seekTime);

      builder.mediaDuration(state.getMediaDuration())
              .endTime(endTime)
              .seekTime(seekTime);
    }

    StreamState newState = builder.build();

    setState(newState);
  }

  @Override
  public void stopStream() {
    LOG.info("Got request to stop the stream");
    // Ensure that the stream is not already stopped
    StreamState state = _state.get();
    if (state == null || state.getStatus() == StreamStateType.OFFLINE) {
      LOG.warn("Can't stop the stream: the stream is already stopped");
      throw new StreamStateConflictException("The stream is already stopped");
    }

    // Set the new state
    setState(StreamState.OFFLINE);
  }

  @Override
  public String getState() {
    try {
      return objectMapper.writeValueAsString(getStreamState());
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public StreamState getStreamState() {
    return _state.get();
  }

  public void setState(StreamState state) {
    if (state.getStatus() == StreamStateType.ONLINE && state.getEndTime() != null) {
      // Take the stream offline at the end of the media
      _vertx.setTimer(Math.max(Duration.between(Instant.now(), state.getEndTime()).toMillis(), 1), eventEnd -> {
        // Only take the stream online if the currently running stream is the stream we're supposed to end
        if (Objects.equals(_state.get(), state)) {
          setState(StreamState.OFFLINE);
        }
      });
    }
    StreamState old = _state.getAndSet(Objects.requireNonNull(state));
    if (old != state && !state.equals(old)) {
      LOG.info("New state = {}", state);

      // Notify all the listeners that the stream changed
      Notification n = new AttributeChangeNotification(this, _sequenceNumber.getAndIncrement(), System.currentTimeMillis(), "StreamState", "StreamState", StreamState.class.getName(), old, state);
      sendNotification(n);

      Map<String, String> jobDataMap = new HashMap<>();
      jobDataMap.put("status", Optional.ofNullable(state.getStatus()).map(Objects::toString).orElse(null));
      jobDataMap.put("live", String.valueOf(state.isLive()));
      jobDataMap.put("playlist", state.getPlaylist());
      jobDataMap.put("mediaName", state.getMediaName());
      jobDataMap.put("mediaDuration", Optional.ofNullable(state.getMediaDuration()).map(Duration::toString).orElse(null));
      jobDataMap.put("startTime", Optional.ofNullable(state.getStartTime()).map(Instant::toString).orElse(null));
      jobDataMap.put("endTime", Optional.ofNullable(state.getEndTime()).map(Instant::toString).orElse(null));
      jobDataMap.put("seekTime", Optional.ofNullable(state.getSeekTime()).map(Duration::toString).orElse(null));
      Utils.beanProxy(scheduleManagerName, ScheduleManagerMBean.class)
              .triggerIfExists( "Stream", String.valueOf(state), jobDataMap);
    }
  }

  public List<String> listStreams(String filter) {
    try {
      Predicate<String> predicate = filter != null && !filter.isBlank() ? new Predicate<String>() {
        final Pattern pattern = Pattern.compile(filter.trim(), Pattern.CASE_INSENSITIVE);

        @Override
        public boolean test(String s) {
          return pattern.matcher(s).find();
        }
      } : any -> true;
      return listStreams0(predicate).stream().map(Utils::writeAsString).collect(Collectors.toList());
    } catch (java.util.regex.PatternSyntaxException ex) {
      return Collections.emptyList();
    }
  }

  @Override
  public String getStatus() {
    return Optional.ofNullable(getStreamState()).map(StreamState::getStatus).orElse(StreamStateType.OFFLINE).name();
  }

  @Override
  public boolean isLive() {
    return Optional.ofNullable(getStreamState()).map(StreamState::isLive).orElse(false);
  }

  @Override
  public String getCurrentPlaylist() {
    return Optional.ofNullable(getStreamState()).map(StreamState::getPlaylist).orElse(null);
  }

  @Override
  public String getCurrentMediaName() {
    return Optional.ofNullable(getStreamState()).map(StreamState::getMediaName).orElse(null);
  }

  @Override
  public Duration getCurrentDuration() {
    return Optional.ofNullable(getStreamState()).map(StreamState::getMediaDuration).orElse(null);
  }

  @Override
  public Instant getCurrentStartTime() {
    return Optional.ofNullable(getStreamState()).map(StreamState::getStartTime).orElse(null);
  }

  @Override
  public Instant getCurrentEndTime() {
    return Optional.ofNullable(getStreamState()).map(StreamState::getEndTime).orElse(null);
  }

  public List<StreamEntry> listStreams0(Predicate<String> predicate) {
    return CompletableFuture.completedFuture(predicate)
            .thenApplyAsync(_client::listMetadataNames)
            .thenCompose(Future::toCompletionStage)
            .join();
  }
}
