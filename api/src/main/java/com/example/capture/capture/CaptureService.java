package com.example.capture.capture;

import com.example.capture.capture.domain.Capture;
import com.example.capture.capture.domain.CaptureSource;
import com.example.capture.capture.domain.CaptureType;
import com.example.capture.capture.domain.Todo;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CaptureService {

    private final CaptureRepository captureRepository;
    private final TodoRepository todoRepository;
    private final EntityManager entityManager;

    @Transactional
    public CaptureResponse create(String text) {
        String rawText = text.trim();

        // T2.6에서 파서가 여기 들어온다. 지금은 무조건 TODO
        Capture capture = captureRepository.saveAndFlush(
                new Capture(rawText, CaptureType.TODO, CaptureSource.AUTO));

        // @MapsId라 capture가 id를 받은 뒤에야 상세를 저장할 수 있다
        Todo todo = todoRepository.save(new Todo(capture, rawText, null));

        // created_at은 DB가 채우므로 다시 읽지 않으면 null이다
        entityManager.refresh(capture);

        return CaptureResponse.of(capture, todo);
    }
}
