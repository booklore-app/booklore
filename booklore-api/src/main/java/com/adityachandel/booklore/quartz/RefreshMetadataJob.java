package com.adityachandel.booklore.quartz;

import com.adityachandel.booklore.model.dto.request.MetadataRefreshRequest;
import com.adityachandel.booklore.service.metadata.BookMetadataService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
@DisallowConcurrentExecution
public class RefreshMetadataJob implements Job {

    private BookMetadataService bookMetadataService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            MetadataRefreshRequest request = (MetadataRefreshRequest) context.getMergedJobDataMap().get("request");
            bookMetadataService.refreshMetadata(request);
        } catch (Exception e) {
            throw new JobExecutionException("Error occurred while executing metadata refresh job", e);
        }
    }
}
