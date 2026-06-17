package com.company.roro.handler;

import com.company.roro.dto.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全局异常处理器单元测试
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("参数xxx不能为空");

        Result<Void> result = handler.handleIllegalArgument(ex);

        assertEquals(400, result.getCode());
        assertEquals("参数xxx不能为空", result.getMessage());
    }

    @Test
    void shouldHandleIllegalArgumentException_withResponseStatus() throws NoSuchMethodException {
        Method method = GlobalExceptionHandler.class.getMethod("handleIllegalArgument", IllegalArgumentException.class);
        ResponseStatus status = method.getAnnotation(ResponseStatus.class);
        assertNotNull(status);
        assertEquals(HttpStatus.BAD_REQUEST, status.value());
    }

    @Test
    void shouldHandleMethodArgumentNotValidException() throws NoSuchMethodException {
        // Simulate a binding result with field errors
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "target");
        bindingResult.addError(new FieldError("target", "name", "名称不能为空"));
        bindingResult.addError(new FieldError("target", "age", "年龄必须大于0"));

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        Result<Map<String, String>> result = handler.handleValidation(ex);

        assertEquals(400, result.getCode());
        assertEquals("参数校验失败", result.getMessage());
        assertNotNull(result.getData());
        assertEquals("名称不能为空", result.getData().get("name"));
        assertEquals("年龄必须大于0", result.getData().get("age"));
    }

    @Test
    void shouldHandleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("拒绝访问");

        Result<Void> result = handler.handleAccessDenied(ex);

        assertEquals(401, result.getCode());
        assertEquals("无权限访问", result.getMessage());
    }

    @Test
    void shouldHandleGenericException() {
        Exception ex = new RuntimeException("数据库连接失败");

        Result<Void> result = handler.handleGeneric(ex);

        assertEquals(500, result.getCode());
        assertEquals("服务器内部错误", result.getMessage());
    }

    @Test
    void shouldHandleGenericException_withResponseStatus() throws NoSuchMethodException {
        Method method = GlobalExceptionHandler.class.getMethod("handleGeneric", Exception.class);
        ResponseStatus status = method.getAnnotation(ResponseStatus.class);
        assertNotNull(status);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status.value());
    }
}
