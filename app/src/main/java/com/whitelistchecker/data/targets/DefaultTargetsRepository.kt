package com.whitelistchecker.data.targets

import com.whitelistchecker.domain.model.CheckTarget

class DefaultTargetsRepository {

    fun getTargets(): List<CheckTarget> = DefaultCheckTargets.defaults().mapNotNull { it.toCheckTarget() }
}
