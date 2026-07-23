package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.response.MediaFileResponse;
import com.petbuddy.petbuddystore.model.MediaFile;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MediaFileMapper {
    MediaFileResponse toMediaFileResponse(MediaFile mediaFile);
    List<MediaFileResponse> toMediaFileResponses(List<MediaFile> mediaFiles);
}
