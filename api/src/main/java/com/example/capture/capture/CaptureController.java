package com.example.capture.capture;

import com.example.capture.common.ApiTokenFilter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import com.example.capture.capture.domain.CaptureType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/captures")
@RequiredArgsConstructor
public class CaptureController {

    private final CaptureService captureService;

    @GetMapping
    public CaptureListResponse list(
            @RequestAttribute(ApiTokenFilter.USER_ID) Long userId,
            @RequestParam(required = false) CaptureType type,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean done) {
        return captureService.list(userId, type, cursor, size, done);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaptureResponse create(@RequestAttribute(ApiTokenFilter.USER_ID) Long userId, @Valid @RequestBody CaptureCreateRequest request) {
        return captureService.create(userId, request.text());
    }

    @GetMapping("/{id}")
    public CaptureResponse get(@RequestAttribute(ApiTokenFilter.USER_ID) Long userId, @PathVariable Long id) {
        return captureService.get(userId, id);
    }

    @PatchMapping("/{id}")
    public CaptureResponse update(@RequestAttribute(ApiTokenFilter.USER_ID) Long userId, @PathVariable Long id, @Valid @RequestBody CaptureUpdateRequest request) {
        return captureService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestAttribute(ApiTokenFilter.USER_ID) Long userId, @PathVariable Long id) {
        captureService.delete(userId, id);
    }

    // 아무것도 만들지 않으므로 201이 아니라 200이다
    @PostMapping("/preview")
    public CaptureResponse preview(@Valid @RequestBody CaptureCreateRequest request) {
        return captureService.preview(request.text());
    }
}
