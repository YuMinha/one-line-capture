package com.example.capture.capture;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/captures")
@RequiredArgsConstructor
public class CaptureController {

    private final CaptureService captureService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaptureResponse create(@Valid @RequestBody CaptureCreateRequest request) {
        return captureService.create(request.text());
    }

    // 아무것도 만들지 않으므로 201이 아니라 200이다
    @PostMapping("/preview")
    public CaptureResponse preview(@Valid @RequestBody CaptureCreateRequest request) {
        return captureService.preview(request.text());
    }
}
