package com.propertylisting.flows.utils

import net.corda.core.CordaRuntimeException

class AlreadyRegisteredException(message: String) : CordaRuntimeException(message)