package net.akaritakai.stream.scheduling.jobs;

import net.akaritakai.stream.streamer.StreamerMBean;
import org.quartz.JobExecutionContext;

public class StreamStopJob extends AbstractStreamJob {
    @Override
    protected void execute1(JobExecutionContext context, StreamerMBean streamer) throws Exception {
        streamer.stopStream();
    }
}
