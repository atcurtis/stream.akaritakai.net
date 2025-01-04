package net.akaritakai.stream.handler.quartz;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.AbstractBlockingHandler;
import net.akaritakai.stream.models.quartz.KeyEntry;
import net.akaritakai.stream.models.quartz.TriggerEntry;
import net.akaritakai.stream.models.quartz.request.ListTriggersRequest;
import net.akaritakai.stream.models.quartz.response.ListTriggersResponse;
import net.akaritakai.stream.scheduling.ScheduleManagerMBean;
import net.akaritakai.stream.scheduling.Utils;
import org.apache.commons.lang3.Validate;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.ArrayList;
import java.util.List;

import static net.akaritakai.stream.config.GlobalNames.scheduleManagerName;

public class TriggersHandler extends AbstractBlockingHandler<ListTriggersRequest> {

    public TriggersHandler(Vertx vertx, CheckAuth checkAuth) {
        super(ListTriggersRequest.class, vertx, checkAuth);
    }

    @Override
    protected void validateRequest(ListTriggersRequest request) {
        Validate.notNull(request, "request cannot be null");
        Validate.notEmpty(request.getKey(), "key cannot be null/empty");
    }

    @Override
    protected void handleAuthorized(HttpServerRequest httpRequest, ListTriggersRequest listTriggersRequest, HttpServerResponse response) {
        String groupPrefix = listTriggersRequest.getGroupPrefix();
        executeBlocking(() -> {
            ScheduleManagerMBean manager = Utils.beanProxy(scheduleManagerName, ScheduleManagerMBean.class);
            List<TriggerEntry> triggers = new ArrayList<>();
            for (TriggerKey triggerKey : manager.getTriggerKeys(groupPrefix == null
                    ? GroupMatcher.anyTriggerGroup()
                    : GroupMatcher.triggerGroupStartsWith(groupPrefix))) {
                Trigger trigger = manager.getTrigger(triggerKey);
                triggers.add(TriggerEntry.builder()
                        .key(KeyEntry.builder()
                                .name(trigger.getKey().getName())
                                .group(trigger.getKey().getGroup())
                                .build())
                        .job(KeyEntry.builder()
                                .name(trigger.getJobKey().getName())
                                .group(trigger.getJobKey().getName())
                                .build())
                        .jobDataMap(QuartzUtils.mapOf(trigger.getJobDataMap()))
                        .description(trigger.getDescription())
                        .calendar(trigger.getCalendarName())
                        .priority(trigger.getPriority())
                        .mayFireAgain(trigger.mayFireAgain())
                        .startTime(QuartzUtils.instantOf(trigger.getStartTime()))
                        .endTime(QuartzUtils.instantOf(trigger.getEndTime()))
                        .nextFireTime(QuartzUtils.instantOf(trigger.getNextFireTime()))
                        .previousFireTime(QuartzUtils.instantOf(trigger.getPreviousFireTime()))
                        .finalFireTime(QuartzUtils.instantOf(trigger.getFinalFireTime()))
                        .misfireInstruction(trigger.getMisfireInstruction())
                        .build());
            }
            return ListTriggersResponse.builder().triggers(triggers).build();
        })
                .onSuccess(jobsResponse -> handleSuccess(writeValue(jobsResponse), "application/json", response))
                .onFailure(ex -> handleFailure("Error enumerating jobs", response, ex));
    }
}
