package com.cainsgl.common.dto.response

enum class ResultCode(val code: Int, val message: String, val codeScopeDesc: String, val defaultResult: Result = Result(code, message))
{
    // ====================== 成功码段（200xx）======================
    SUCCESS(20000, "操作成功", "通用成功"),

    // ====================== 业务异常码段（400xx）======================
    BUSINESS_ERROR(40000, "业务逻辑异常", "通用业务异常"),
    MISSING_PARAM(40001, "请求参数缺失", "参数异常"),
    PARAM_INVALID(40002, "请求参数格式非法", "参数异常"),
    RESOURCE_NOT_FOUND(40003, "请求资源不存在", "资源异常"),
    PERMISSION_DENIED(40004, "权限不足", "权限异常"),
    DATA_DUPLICATE(40005, "数据重复", "数据异常"),

    // ====================== 系统异常码段（500xx）======================
    SYSTEM_ERROR(50000, "系统内部异常", "通用系统异常"),
    DB_ERROR(50001, "数据库操作异常", "系统异常-数据库"),
    CACHE_ERROR(50002, "缓存操作异常", "系统异常-缓存"),
    THIRD_PARTY_ERROR(50003, "第三方接口调用异常", "系统异常-第三方依赖"),
    NETWORK_ERROR(50004, "网络通信异常", "系统异常-网络");
}