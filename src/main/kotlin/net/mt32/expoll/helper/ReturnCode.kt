package net.mt32.expoll.helper

import io.ktor.http.*

object ReturnCode {
    val OK = HttpStatusCode.OK // 200
    val CREATED = HttpStatusCode.Created // 200
    val USER_CREATED = CREATED // 202
    val BAD_REQUEST = HttpStatusCode.BadRequest // 400
    val MISSING_PARAMS = HttpStatusCode.BadRequest // 400
    val INVALID_PARAMS = HttpStatusCode.BadRequest // 400
    val UNAUTHORIZED = HttpStatusCode.Unauthorized // 401
    val INVALID_LOGIN_KEY = HttpStatusCode.Unauthorized // 401
    val INVALID_CHALLENGE_RESPONSE = HttpStatusCode.Unauthorized // 401
    val CAPTCHA_INVALID = HttpStatusCode.Unauthorized // 401
    val FORBIDDEN = HttpStatusCode.Forbidden // 403
    val CHANGE_NOT_ALLOWED = HttpStatusCode.Forbidden // 403
    val NOT_ACCEPTABLE = HttpStatusCode.NotAcceptable // 406
    val USER_EXISTS = HttpStatusCode.NotAcceptable // 406
    val CONFLICT = HttpStatusCode.Conflict // 409
    val INVALID_TYPE = HttpStatusCode.Conflict // 409
    val PAYLOAD_TOO_LARGE = HttpStatusCode.PayloadTooLarge // 413
    val TOO_MANY_POLLS = HttpStatusCode.PayloadTooLarge // 413
    val UNPROCESSABLE_ENTITY = HttpStatusCode.UnprocessableEntity // 422
    val INTERNAL_SERVER_ERROR = HttpStatusCode.InternalServerError // 500
    val NOT_IMPLEMENTED = HttpStatusCode.NotImplemented //501
}/*
enum ReturnCode {
    OK = 200,
    BAD_REQUEST = 400,
    MISSING_PARAMS = 400,
    INVALID_PARAMS = 400,
    UNAUTHORIZED = 401,
    INVALID_LOGIN_KEY = 401,
    INVALID_CHALLENGE_RESPONSE = 401,
    CAPTCHA_INVALID = 401,
    FORBIDDEN = 403,
    CHANGE_NOT_ALLOWED = 403,
    NOT_ACCEPTABLE = 406,
    USER_EXISTS = 406,
    CONFLICT = 409,
    INVALID_TYPE = 409,
    PAYLOAD_TOO_LARGE = 413,
    TOO_MANY_POLLS = 413,
    UNPROCESSABLE_ENTITY = 422,
    INTERNAL_SERVER_ERROR = 500,
    NOT_IMPLEMENTED = 501
}*/