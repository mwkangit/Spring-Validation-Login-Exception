package hello.exception.exhandler;

import lombok.AllArgsConstructor;
import lombok.Data;

// @ExceptionHandler에서 반환할 json 객체
@Data
@AllArgsConstructor
public class ErrorResult {
    private String code;
    private String message;
}
