package com.example.TestAPI.Mapper;

import com.example.TestAPI.DTO.Job.JobResponse;
import com.example.TestAPI.Model.JobOffer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

public interface JobOfferMapper {

    @Mapping(source = "creator.username", target = "creatorUsername")
    @Mapping(source = "assignee.username", target = "assigneeUsername")
    JobResponse toDTO(JobOffer job);

}
