package com.whitelistchecker.domain.model

data class PublicServiceSettings(
    val installationId: String = "",
    val shareReports: Boolean = false,
    val allowRemoteChecks: Boolean = false,
    val regionCode: String = DEFAULT_REGION_CODE,
    val regionName: String = "",
    val cityCode: String? = null,
    val cityName: String? = null,
    val customCityName: String? = null,
    val areaSource: AreaSource = AreaSource.MANUAL_SELECTION,
    val areaConfirmedByUser: Boolean = false,
    val areaUpdatedAtMillis: Long = 0L,
    val operatorCode: String = DEFAULT_OPERATOR_CODE,
    val operatorSelectionMode: OperatorSelectionMode = OperatorSelectionMode.AUTO,
    val operatorSource: OperatorDetectionSource = OperatorDetectionSource.UNKNOWN,
    val operatorDisplayName: String? = null,
    val operatorMccMnc: String? = null,
    val operatorUpdatedAtMillis: Long = 0L,
    val deviceAlias: String = DEFAULT_DEVICE_ALIAS,
    val registrationState: PublicServiceRegistrationState = PublicServiceRegistrationState.NOT_REGISTERED,
) {
    val isRegistered: Boolean
        get() = installationId.isNotBlank() &&
            registrationState == PublicServiceRegistrationState.REGISTERED

    val canUseRemoteChecks: Boolean
        get() = isRegistered && allowRemoteChecks

    val hasPublicReportContext: Boolean
        get() = shareReports &&
            regionCode != DEFAULT_REGION_CODE &&
            areaConfirmedByUser &&
            operatorCode != DEFAULT_OPERATOR_CODE

    val userArea: UserArea
        get() = UserArea(
            countryCode = "RU",
            regionCode = regionCode,
            regionName = regionName.ifBlank { PublicServiceCatalog.regionByCode(regionCode)?.label.orEmpty() },
            cityCode = cityCode,
            cityName = cityName,
            customCityName = customCityName,
            source = areaSource,
            confirmedByUser = areaConfirmedByUser,
            updatedAtMillis = areaUpdatedAtMillis,
        )

    val selectedOperatorLabel: String
        get() = PublicServiceCatalog.operatorByCode(operatorCode)?.label
            ?: operatorDisplayName
            ?: "Оператор не выбран"

    companion object {
        const val DEFAULT_REGION_CODE = "UNKNOWN"
        const val DEFAULT_OPERATOR_CODE = "UNKNOWN"
        const val DEFAULT_DEVICE_ALIAS = "Android-устройство"
        const val LEGACY_DEFAULT_DEVICE_ALIAS = "Мой телефон"
    }
}
