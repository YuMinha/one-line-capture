package com.example.capture.capture;

import com.example.capture.common.ApiTokenFilter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 완료·읽음은 분류와 무관한 상태 변화라 captures 아래가 아니라 각자의 경로를 쓴다 (stack.md §3.1)
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DetailController {

    private final CaptureService captureService;

    @PatchMapping("/todos/{captureId}")
    public CaptureResponse toggleDone(@RequestAttribute(ApiTokenFilter.USER_ID) Long userId,
                                      @PathVariable Long captureId,
                                      @Valid @RequestBody DetailToggleRequest request) {
        return captureService.changeDone(userId, captureId, request.value());
    }

    @PatchMapping("/links/{captureId}")
    public CaptureResponse toggleRead(@RequestAttribute(ApiTokenFilter.USER_ID) Long userId,
                                      @PathVariable Long captureId,
                                      @Valid @RequestBody DetailToggleRequest request) {
        return captureService.changeRead(userId, captureId, request.value());
    }
}
