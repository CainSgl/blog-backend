package com.cainsgl.common.dto.response;

import com.cainsgl.common.exception.BSystemException;
import com.cainsgl.common.exception.BusinessException;
import com.cainsgl.common.util.TraceIdUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Result
{
    private int code;
    private String msg;
    private Object data;
    private String traceId;
    private static final int SUCCESS_CODE = 200;
    public static final int ERROR_CODE = 500;
    private static final String SUCCESS_MSG = "success";

    public static Result success(Object body)
    {
        return new Result(SUCCESS_CODE, SUCCESS_MSG, body,null);
    }
    public static Result success()
    {
        return new Result(SUCCESS_CODE, SUCCESS_MSG, null,null);
    }
    //非业务异常，系统异常返回500
    public static Result error(String msg)
    {
        return new Result(ERROR_CODE, msg, null, null);
    }
    //非业务异常，系统异常返回500
    public static Result error(String msg,String cause,String traceId)
    {
        return new Result(ERROR_CODE, msg, cause,traceId);
    }

    //业务异常
    public static Result error(BusinessException exception)
    {
        return new Result(500, exception.getMessage(), null,null);
    }
    //系统异常
    public static Result error(BSystemException exception)
    {
        return new Result(exception.getErrorCode(), exception.getMessage(), null, TraceIdUtils.getTraceId());
    }
}
