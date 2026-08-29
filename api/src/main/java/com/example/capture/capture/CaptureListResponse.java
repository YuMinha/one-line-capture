package com.example.capture.capture;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CaptureListResponse(List<CaptureResponse> items, Long nextCursor, boolean hasNext) {
}
