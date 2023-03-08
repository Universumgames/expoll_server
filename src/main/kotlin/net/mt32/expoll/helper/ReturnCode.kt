package net.mt32.expoll.helper

import io.ktor.http.*

object ReturnCode {
    /** HTTP Code 200 */
    val OK = HttpStatusCode.OK
    /** HTTP Code 202*/
    val CREATED = HttpStatusCode.Created
    /** HTTP Code 202*/
    val USER_CREATED = CREATED
    /** HTTP Code 400*/
    val BAD_REQUEST = HttpStatusCode.BadRequest
    /** HTTP Code 400*/
    val MISSING_PARAMS = HttpStatusCode.BadRequest
    /** HTTP Code 400*/
    val INVALID_PARAMS = HttpStatusCode.BadRequest
    /** HTTP Code 401*/
    val UNAUTHORIZED = HttpStatusCode.Unauthorized
    /** HTTP Code 401*/
    val INVALID_LOGIN_KEY = HttpStatusCode.Unauthorized
    /** HTTP Code 401*/
    val INVALID_CHALLENGE_RESPONSE = HttpStatusCode.Unauthorized
    /** HTTP Code 401*/
    val CAPTCHA_INVALID = HttpStatusCode.Unauthorized
    /** HTTP Code 403*/
    val FORBIDDEN = HttpStatusCode.Forbidden
    /** HTTP Code 403*/
    val CHANGE_NOT_ALLOWED = HttpStatusCode.Forbidden
    /** HTTP Code 406*/
    val NOT_ACCEPTABLE = HttpStatusCode.NotAcceptable
    /** HTTP Code 406*/
    val USER_EXISTS = HttpStatusCode.NotAcceptable
    /** HTTP Code 409*/
    val CONFLICT = HttpStatusCode.Conflict
    /** HTTP Code 409*/
    val INVALID_TYPE = HttpStatusCode.Conflict
    /** HTTP Code 413*/
    val PAYLOAD_TOO_LARGE = HttpStatusCode.PayloadTooLarge
    /** HTTP Code 413*/
    val TOO_MANY_POLLS = HttpStatusCode.PayloadTooLarge
    /** HTTP Code 422*/
    val UNPROCESSABLE_ENTITY = HttpStatusCode.UnprocessableEntity
    /** HTTP Code 500*/
    val INTERNAL_SERVER_ERROR = HttpStatusCode.InternalServerError
    /** HTTP Code 501*/
    val NOT_IMPLEMENTED = HttpStatusCode.NotImplemented
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