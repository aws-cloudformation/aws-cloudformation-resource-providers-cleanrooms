package software.amazon.cleanrooms.configuredtable.typemapper

import software.amazon.awssdk.services.cleanrooms.model.AccessDeniedException
import software.amazon.awssdk.services.cleanrooms.model.ConflictException
import software.amazon.awssdk.services.cleanrooms.model.InternalServerException
import software.amazon.awssdk.services.cleanrooms.model.ResourceNotFoundException
import software.amazon.awssdk.services.cleanrooms.model.ServiceQuotaExceededException
import software.amazon.awssdk.services.cleanrooms.model.ThrottlingException
import software.amazon.awssdk.services.cleanrooms.model.ValidationException
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException
import software.amazon.cloudformation.exceptions.CfnNotFoundException
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException
import software.amazon.cloudformation.exceptions.CfnThrottlingException

fun Exception.toCfnException() = when (this) {
    is ConflictException -> CfnAlreadyExistsException(this)
    is ValidationException -> CfnInvalidRequestException(this)
    is ServiceQuotaExceededException -> CfnServiceLimitExceededException(this)
    is AccessDeniedException -> CfnAccessDeniedException(this)
    is ThrottlingException -> CfnThrottlingException(this)
    is ResourceNotFoundException -> CfnNotFoundException(this)
    is InternalServerException -> CfnServiceInternalErrorException(this)
    is CfnNotStabilizedException -> CfnNotStabilizedException(this)
    else -> CfnGeneralServiceException(this)
}
