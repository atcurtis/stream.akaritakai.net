package net.akaritakai.stream.handler.quartz;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.akaritakai.stream.CheckAuth;
import net.akaritakai.stream.handler.AbstractBlockingHandler;
import net.akaritakai.stream.handler.AbstractHandler;
import net.akaritakai.stream.models.quartz.JobEntry;
import net.akaritakai.stream.models.quartz.KeyEntry;
import net.akaritakai.stream.models.quartz.request.ListJobsRequest;
import net.akaritakai.stream.models.quartz.response.ListJobsResponse;
import net.akaritakai.stream.scheduling.ScheduleManager;
import net.akaritakai.stream.scheduling.ScheduleManagerMBean;
import net.akaritakai.stream.scheduling.Utils;
import org.apache.commons.lang3.Validate;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.*;

import static net.akaritakai.stream.config.GlobalNames.scheduleManagerName;

public class JobsHandler extends AbstractBlockingHandler<ListJobsRequest> {

    public JobsHandler(Vertx vertx, CheckAuth checkAuth) {
        super(ListJobsRequest.class, vertx, checkAuth);
    }

    @Override
    protected void validateRequest(ListJobsRequest request) {
        Validate.notNull(request, "request cannot be null");
        Validate.notEmpty(request.getKey(), "key cannot be null/empty");
    }

    @Override
    protected void handleAuthorized(HttpServerRequest httpRequest, ListJobsRequest listJobsRequest, HttpServerResponse response) {
        String groupPrefix = listJobsRequest.getGroupPrefix();
        executeBlocking(() -> {
            ScheduleManagerMBean manager = Utils.beanProxy(scheduleManagerName, ScheduleManagerMBean.class);
            List<JobEntry> jobs = new ArrayList<>();
            for (JobKey jobKey : manager.getJobKeys(groupPrefix == null
                    ? GroupMatcher.anyJobGroup()
                    : GroupMatcher.jobGroupStartsWith(groupPrefix))) {
                JobDetail jobDetail = manager.getJobDetail(jobKey);
                jobs.add(JobEntry.builder()
                        .key(KeyEntry.builder()
                                .name(jobDetail.getKey().getName())
                                .group(jobDetail.getKey().getGroup())
                                .build())
                        .clazz(jobDetail.getJobClass().getName())
                        .jobDataMap(QuartzUtils.mapOf(jobDetail.getJobDataMap()))
                        .description(jobDetail.getDescription())
                        .durable(jobDetail.isDurable())
                        .persist(jobDetail.isPersistJobDataAfterExecution())
                        .concurrent(!jobDetail.isConcurrentExectionDisallowed())
                        .recoverable(jobDetail.requestsRecovery())
                        .build());
            }
            return ListJobsResponse.builder().jobs(jobs).build();
        })
                .onSuccess(jobsResponse -> handleSuccess(writeValue(jobsResponse), "application/json", response))
                .onFailure(ex -> handleFailure("Error enumerating jobs", response, ex));
    }
}
