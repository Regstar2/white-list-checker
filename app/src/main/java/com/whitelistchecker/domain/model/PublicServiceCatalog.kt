package com.whitelistchecker.domain.model

enum class AreaSource {
    AUTOMATIC_LOCATION,
    MANUAL_SELECTION,
}

enum class OperatorSelectionMode {
    AUTO,
    MANUAL,
}

enum class OperatorDetectionSource {
    NETWORK_OPERATOR,
    SIM_OPERATOR,
    MANUAL,
    UNKNOWN,
}

enum class OperatorDetectionConfidence {
    EXACT_MCC_MNC,
    NORMALIZED_NAME,
    UNKNOWN,
}

enum class AreaDetectionState {
    IDLE,
    REQUESTING_PERMISSION,
    DETECTING,
    SUCCESS,
    PERMISSION_DENIED,
    LOCATION_DISABLED,
    TIMEOUT,
    GEOCODER_UNAVAILABLE,
    NOT_FOUND,
    ERROR,
}

data class PublicServiceRegion(
    val code: String,
    val label: String,
    val aliases: List<String> = emptyList(),
)

data class PublicServiceCity(
    val code: String,
    val regionCode: String,
    val label: String,
    val aliases: List<String> = emptyList(),
)

data class PublicServiceOperator(
    val code: String,
    val label: String,
    val mccMncs: Set<String> = emptySet(),
    val aliases: List<String> = emptyList(),
)

data class UserArea(
    val countryCode: String = "RU",
    val regionCode: String = PublicServiceSettings.DEFAULT_REGION_CODE,
    val regionName: String = "",
    val cityCode: String? = null,
    val cityName: String? = null,
    val customCityName: String? = null,
    val source: AreaSource = AreaSource.MANUAL_SELECTION,
    val confirmedByUser: Boolean = false,
    val updatedAtMillis: Long = 0L,
)

data class DetectedOperator(
    val operatorCode: String? = null,
    val displayName: String? = null,
    val mccMnc: String? = null,
    val source: OperatorDetectionSource = OperatorDetectionSource.UNKNOWN,
    val confidence: OperatorDetectionConfidence = OperatorDetectionConfidence.UNKNOWN,
)

object PublicServiceCatalog {
    val regions = listOf(
        PublicServiceRegion("UNKNOWN", "Регион не выбран"),
        PublicServiceRegion("RU-AD", "Республика Адыгея", listOf("Адыгея", "Adygea")),
        PublicServiceRegion("RU-AL", "Республика Алтай", listOf("Алтай республика", "Altai Republic")),
        PublicServiceRegion("RU-ALT", "Алтайский край", listOf("Altai Krai")),
        PublicServiceRegion("RU-AMU", "Амурская область", listOf("Amur Oblast")),
        PublicServiceRegion("RU-ARK", "Архангельская область", listOf("Arkhangelsk Oblast")),
        PublicServiceRegion("RU-AST", "Астраханская область", listOf("Astrakhan Oblast")),
        PublicServiceRegion("RU-BA", "Республика Башкортостан", listOf("Башкортостан", "Башкирия", "Bashkortostan")),
        PublicServiceRegion("RU-BEL", "Белгородская область", listOf("Belgorod Oblast")),
        PublicServiceRegion("RU-BRY", "Брянская область", listOf("Bryansk Oblast")),
        PublicServiceRegion("RU-BU", "Республика Бурятия", listOf("Бурятия", "Buryatia")),
        PublicServiceRegion("RU-CE", "Чеченская Республика", listOf("Чечня", "Chechnya")),
        PublicServiceRegion("RU-CHE", "Челябинская область", listOf("Chelyabinsk Oblast")),
        PublicServiceRegion("RU-CHU", "Чукотский автономный округ", listOf("Чукотка", "Chukotka")),
        PublicServiceRegion("RU-CU", "Чувашская Республика", listOf("Чувашия", "Chuvashia")),
        PublicServiceRegion("RU-DA", "Республика Дагестан", listOf("Дагестан", "Dagestan")),
        PublicServiceRegion("RU-IN", "Республика Ингушетия", listOf("Ингушетия", "Ingushetia")),
        PublicServiceRegion("RU-IRK", "Иркутская область", listOf("Irkutsk Oblast")),
        PublicServiceRegion("RU-IVA", "Ивановская область", listOf("Ivanovo Oblast")),
        PublicServiceRegion("RU-KAM", "Камчатский край", listOf("Камчатка", "Kamchatka Krai")),
        PublicServiceRegion("RU-KB", "Кабардино-Балкарская Республика", listOf("Кабардино-Балкария", "Kabardino-Balkaria")),
        PublicServiceRegion("RU-KC", "Карачаево-Черкесская Республика", listOf("Карачаево-Черкесия", "Karachay-Cherkessia")),
        PublicServiceRegion("RU-KDA", "Краснодарский край", listOf("Krasnodar Krai")),
        PublicServiceRegion("RU-KEM", "Кемеровская область", listOf("Кузбасс", "Kemerovo Oblast")),
        PublicServiceRegion("RU-KGD", "Калининградская область", listOf("Kaliningrad Oblast")),
        PublicServiceRegion("RU-KGN", "Курганская область", listOf("Kurgan Oblast")),
        PublicServiceRegion("RU-KHA", "Хабаровский край", listOf("Khabarovsk Krai")),
        PublicServiceRegion("RU-KHM", "Ханты-Мансийский автономный округ", listOf("ХМАО", "Югра", "Khanty-Mansi")),
        PublicServiceRegion("RU-KIR", "Кировская область", listOf("Kirov Oblast")),
        PublicServiceRegion("RU-KK", "Республика Хакасия", listOf("Хакасия", "Khakassia")),
        PublicServiceRegion("RU-KL", "Республика Калмыкия", listOf("Калмыкия", "Kalmykia")),
        PublicServiceRegion("RU-KLU", "Калужская область", listOf("Kaluga Oblast")),
        PublicServiceRegion("RU-KO", "Республика Коми", listOf("Коми", "Komi")),
        PublicServiceRegion("RU-KOS", "Костромская область", listOf("Kostroma Oblast")),
        PublicServiceRegion("RU-KR", "Республика Карелия", listOf("Карелия", "Karelia")),
        PublicServiceRegion("RU-KRS", "Курская область", listOf("Kursk Oblast")),
        PublicServiceRegion("RU-KYA", "Красноярский край", listOf("Krasnoyarsk Krai")),
        PublicServiceRegion("RU-LEN", "Ленинградская область", listOf("Leningrad Oblast")),
        PublicServiceRegion("RU-LIP", "Липецкая область", listOf("Lipetsk Oblast")),
        PublicServiceRegion("RU-MAG", "Магаданская область", listOf("Magadan Oblast")),
        PublicServiceRegion("RU-ME", "Республика Марий Эл", listOf("Марий Эл", "Mari El")),
        PublicServiceRegion("RU-MO", "Республика Мордовия", listOf("Мордовия", "Mordovia")),
        PublicServiceRegion("RU-MOS", "Московская область", listOf("Подмосковье", "Moscow Oblast")),
        PublicServiceRegion("RU-MOW", "Москва", listOf("Moscow")),
        PublicServiceRegion("RU-MUR", "Мурманская область", listOf("Murmansk Oblast")),
        PublicServiceRegion("RU-NEN", "Ненецкий автономный округ", listOf("НАО", "Nenets")),
        PublicServiceRegion("RU-NGR", "Новгородская область", listOf("Novgorod Oblast")),
        PublicServiceRegion("RU-NIZ", "Нижегородская область", listOf("Nizhny Novgorod Oblast")),
        PublicServiceRegion("RU-NVS", "Новосибирская область", listOf("Novosibirsk Oblast")),
        PublicServiceRegion("RU-OMS", "Омская область", listOf("Omsk Oblast")),
        PublicServiceRegion("RU-ORE", "Оренбургская область", listOf("Orenburg Oblast")),
        PublicServiceRegion("RU-ORL", "Орловская область", listOf("Oryol Oblast")),
        PublicServiceRegion("RU-PER", "Пермский край", listOf("Perm Krai")),
        PublicServiceRegion("RU-PNZ", "Пензенская область", listOf("Penza Oblast")),
        PublicServiceRegion("RU-PRI", "Приморский край", listOf("Приморье", "Primorsky Krai")),
        PublicServiceRegion("RU-PSK", "Псковская область", listOf("Pskov Oblast")),
        PublicServiceRegion("RU-ROS", "Ростовская область", listOf("Rostov Oblast")),
        PublicServiceRegion("RU-RYA", "Рязанская область", listOf("Рязанская обл.", "Ryazan Oblast")),
        PublicServiceRegion("RU-SA", "Республика Саха (Якутия)", listOf("Якутия", "Sakha", "Yakutia")),
        PublicServiceRegion("RU-SAK", "Сахалинская область", listOf("Sakhalin Oblast")),
        PublicServiceRegion("RU-SAM", "Самарская область", listOf("Samara Oblast")),
        PublicServiceRegion("RU-SAR", "Саратовская область", listOf("Saratov Oblast")),
        PublicServiceRegion("RU-SE", "Республика Северная Осетия — Алания", listOf("Северная Осетия", "North Ossetia")),
        PublicServiceRegion("RU-SMO", "Смоленская область", listOf("Smolensk Oblast")),
        PublicServiceRegion("RU-SPE", "Санкт-Петербург", listOf("СПб", "Saint Petersburg", "St Petersburg")),
        PublicServiceRegion("RU-STA", "Ставропольский край", listOf("Stavropol Krai")),
        PublicServiceRegion("RU-SVE", "Свердловская область", listOf("Sverdlovsk Oblast")),
        PublicServiceRegion("RU-TA", "Республика Татарстан", listOf("Татарстан", "Tatarstan")),
        PublicServiceRegion("RU-TAM", "Тамбовская область", listOf("Tambov Oblast")),
        PublicServiceRegion("RU-TOM", "Томская область", listOf("Tomsk Oblast")),
        PublicServiceRegion("RU-TUL", "Тульская область", listOf("Tula Oblast")),
        PublicServiceRegion("RU-TVE", "Тверская область", listOf("Tver Oblast")),
        PublicServiceRegion("RU-TY", "Республика Тыва", listOf("Тыва", "Тува", "Tuva")),
        PublicServiceRegion("RU-TYU", "Тюменская область", listOf("Tyumen Oblast")),
        PublicServiceRegion("RU-UD", "Удмуртская Республика", listOf("Удмуртия", "Udmurtia")),
        PublicServiceRegion("RU-ULY", "Ульяновская область", listOf("Ulyanovsk Oblast")),
        PublicServiceRegion("RU-VGG", "Волгоградская область", listOf("Volgograd Oblast")),
        PublicServiceRegion("RU-VLA", "Владимирская область", listOf("Vladimir Oblast")),
        PublicServiceRegion("RU-VLG", "Вологодская область", listOf("Vologda Oblast")),
        PublicServiceRegion("RU-VOR", "Воронежская область", listOf("Voronezh Oblast")),
        PublicServiceRegion("RU-YAN", "Ямало-Ненецкий автономный округ", listOf("ЯНАО", "Yamalo-Nenets")),
        PublicServiceRegion("RU-YAR", "Ярославская область", listOf("Yaroslavl Oblast")),
        PublicServiceRegion("RU-YEV", "Еврейская автономная область", listOf("ЕАО", "Jewish Autonomous Oblast")),
        PublicServiceRegion("RU-ZAB", "Забайкальский край", listOf("Zabaykalsky Krai")),
        PublicServiceRegion("RU-OTHER", "Другой регион"),
    )

    val cities = listOf(
        city("RU-MOW-MOSCOW", "RU-MOW", "Москва", "Moscow"),
        city("RU-SPE-SAINT-PETERSBURG", "RU-SPE", "Санкт-Петербург", "СПб", "Saint Petersburg"),
        city("RU-RYA-RYAZAN", "RU-RYA", "Рязань", "Ryazan"),
        city("RU-MOS-BALASHIKHA", "RU-MOS", "Балашиха"),
        city("RU-MOS-KHIMKI", "RU-MOS", "Химки"),
        city("RU-NIZ-NIZHNY-NOVGOROD", "RU-NIZ", "Нижний Новгород", "Nizhny Novgorod"),
        city("RU-KDA-KRASNODAR", "RU-KDA", "Краснодар", "Krasnodar"),
        city("RU-SVE-YEKATERINBURG", "RU-SVE", "Екатеринбург", "Yekaterinburg", "Ekaterinburg"),
        city("RU-TA-KAZAN", "RU-TA", "Казань", "Kazan"),
        city("RU-NVS-NOVOSIBIRSK", "RU-NVS", "Новосибирск", "Novosibirsk"),
        city("RU-SAM-SAMARA", "RU-SAM", "Самара", "Samara"),
        city("RU-ROS-ROSTOV-ON-DON", "RU-ROS", "Ростов-на-Дону", "Rostov-on-Don"),
        city("RU-BA-UFA", "RU-BA", "Уфа", "Ufa"),
        city("RU-CHE-CHELYABINSK", "RU-CHE", "Челябинск", "Chelyabinsk"),
        city("RU-PER-PERM", "RU-PER", "Пермь", "Perm"),
        city("RU-VOR-VORONEZH", "RU-VOR", "Воронеж", "Voronezh"),
        city("RU-VGG-VOLGOGRAD", "RU-VGG", "Волгоград", "Volgograd"),
        city("RU-KYA-KRASNOYARSK", "RU-KYA", "Красноярск", "Krasnoyarsk"),
        city("RU-ORE-ORENBURG", "RU-ORE", "Оренбург", "Orenburg"),
        city("RU-PRI-VLADIVOSTOK", "RU-PRI", "Владивосток", "Vladivostok"),
        city("RU-KHA-KHABAROVSK", "RU-KHA", "Хабаровск", "Khabarovsk"),
        city("RU-IRK-IRKUTSK", "RU-IRK", "Иркутск", "Irkutsk"),
        city("RU-TYU-TYUMEN", "RU-TYU", "Тюмень", "Tyumen"),
        city("RU-OMS-OMSK", "RU-OMS", "Омск", "Omsk"),
    )

    val operators = listOf(
        PublicServiceOperator("UNKNOWN", "Оператор не выбран"),
        PublicServiceOperator("MEGAFON", "МегаФон", setOf("25002", "25011", "25030"), listOf("megafon", "мегафон")),
        PublicServiceOperator("MTS", "МТС", setOf("25001", "25023"), listOf("mts", "мтс")),
        PublicServiceOperator("BEELINE", "Билайн", setOf("25028", "25099"), listOf("beeline", "билайн", "вымпелком")),
        PublicServiceOperator("T2", "T2", setOf("25020"), listOf("tele2", "t2", "теле2", "t-mobile", "т-мобайл")),
        PublicServiceOperator("YOTA", "Yota", emptySet(), listOf("yota", "йота")),
        PublicServiceOperator("ROSTELECOM", "Ростелеком", emptySet(), listOf("rostelecom", "ростелеком")),
        PublicServiceOperator("SBERMOBILE", "СберМобайл", emptySet(), listOf("sbermobile", "сбермобайл", "сбер мобайл")),
        PublicServiceOperator("TMOBILE", "Т-Мобайл", emptySet(), listOf("t-mobile", "т-мобайл", "тинькофф", "tinkoff")),
        PublicServiceOperator("GAZPROMBANK_MOBILE", "Газпромбанк Мобайл", emptySet(), listOf("газпромбанк", "gpb")),
        PublicServiceOperator("OTHER", "Другой оператор"),
    )

    fun sortedRegions(): List<PublicServiceRegion> =
        regions.filterNot { it.code == "UNKNOWN" }.sortedBy { it.label }

    fun citiesForRegion(regionCode: String): List<PublicServiceCity> =
        cities.filter { it.regionCode == regionCode }.sortedBy { it.label }

    fun regionByCode(code: String): PublicServiceRegion? =
        regions.firstOrNull { it.code == code }

    fun cityByCode(code: String?): PublicServiceCity? =
        code?.let { cityCode -> cities.firstOrNull { it.code == cityCode } }

    fun operatorByCode(code: String): PublicServiceOperator? =
        operators.firstOrNull { it.code == code }

    fun normalizeRegion(value: String?): PublicServiceRegion? {
        val normalized = normalizeText(value)
        if (normalized.isBlank()) return null
        return regions.firstOrNull { region ->
            normalizeText(region.label) == normalized ||
                region.aliases.any { normalizeText(it) == normalized }
        }
    }

    fun normalizeCity(regionCode: String, value: String?): PublicServiceCity? {
        val normalized = normalizeText(value)
        if (normalized.isBlank()) return null
        return citiesForRegion(regionCode).firstOrNull { city ->
            normalizeText(city.label) == normalized ||
                city.aliases.any { normalizeText(it) == normalized }
        }
    }

    fun detectOperatorByMccMnc(mccMnc: String?): PublicServiceOperator? {
        val normalized = mccMnc?.filter { it.isDigit() }.orEmpty()
        if (normalized.length < 5) return null
        return operators.firstOrNull { normalized in it.mccMncs }
    }

    fun detectOperatorByName(name: String?): PublicServiceOperator? {
        val normalized = normalizeText(name)
        if (normalized.isBlank()) return null
        return operators.firstOrNull { operator ->
            normalizeText(operator.label) == normalized ||
                operator.aliases.any { alias -> normalized.contains(normalizeText(alias)) }
        }
    }

    fun sanitizeCustomCityName(value: String): String {
        return value
            .filterNot { it.isISOControl() }
            .trim()
            .replace(Regex("\\s+"), " ")
            .take(64)
    }

    fun normalizeText(value: String?): String {
        return value
            ?.lowercase()
            ?.replace("ё", "е")
            ?.replace("обл.", "область")
            ?.replace("респ.", "республика")
            ?.replace(Regex("[^a-zа-я0-9]+"), " ")
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            .orEmpty()
    }

    private fun city(code: String, regionCode: String, label: String, vararg aliases: String): PublicServiceCity =
        PublicServiceCity(code = code, regionCode = regionCode, label = label, aliases = aliases.toList())
}
