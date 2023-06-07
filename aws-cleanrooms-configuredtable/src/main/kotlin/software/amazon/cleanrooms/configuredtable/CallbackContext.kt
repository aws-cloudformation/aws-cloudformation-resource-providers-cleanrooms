package software.amazon.cleanrooms.configuredtable

import software.amazon.cloudformation.proxy.StdCallbackContext

data class CallbackContext(val stabilizationRetriesRemaining: Int = 0, val pendingStabilization: Boolean = false): StdCallbackContext()
