package com.cainsgl.common.dto.response;

import com.cainsgl.common.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Result
{
    private int code;
    private String msg;
    private Object data;
    private static final int SUCCESS_CODE = 200;
    public static final int ERROR_CODE = 500;
    private static final String SUCCESS_MSG = "success";

    public static Result success(Object body)
    {
        return new Result(SUCCESS_CODE, SUCCESS_MSG, body);
    }
    public static Result success()
    {
        return new Result(SUCCESS_CODE, SUCCESS_MSG, null);
    }
    //非业务异常，系统异常返回500
    public static Result error(String msg)
    {
        return new Result(ERROR_CODE, msg, null);
    }
    //非业务异常，系统异常返回500
    public static Result error(String msg,String cause)
    {
        return new Result(ERROR_CODE, msg, cause);
    }
    //业务异常，返回自定义code
    public static Result error(BusinessException msg)
    {
        return new Result(msg.getErrorCode(), msg.getMessage(), null);
    }
}
