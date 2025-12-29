package com.cainsgl.common.exception

class BSystemException : RuntimeException {
    private val errorCode: Int

    constructor(message: String, errorCode: Int) : super(message) {
        this.errorCode = errorCode
    }

    constructor(message: String) : this(message, DEFAULT_ERROR_CODE)
    constructor(ex:Exception) : super(ex) {
        this.errorCode = DEFAULT_ERROR_CODE
    }
    companion object {
        private const val DEFAULT_ERROR_CODE = 501
    }
}
