package com.company.roro.dto;

import lombok.Data;

/**
 * 统一接口返回格式
 *
 * 作用：
 * 1. 规范所有接口的返回格式，便于前端统一处理
 * 2. 包含状态码、消息、数据三个核心字段
 *
 * 使用示例：
 * - 成功：Result.success(data)
 * - 失败：Result.error("用户名已存在")
 *
 * @param <T> 数据类型，可以是任意类型
 */
@Data
public class Result<T> {

    /** 状态码：200表示成功，其他表示失败 */
    private Integer code;

    /** 提示消息 */
    private String message;

    /** 返回的数据，可为空 */
    private T data;

    /**
     * 成功返回（无消息）
     *
     * @param data 返回的数据
     * @return Result 对象
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    /**
     * 成功返回（自定义消息）
     *
     * @param message 提示消息
     * @param data 返回的数据
     * @return Result 对象
     */
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    /**
     * 失败返回（默认状态码500）
     *
     * @param message 错误消息
     * @return Result 对象
     */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败返回（自定义状态码）
     *
     * @param code 状态码
     * @param message 错误消息
     * @return Result 对象
     */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}