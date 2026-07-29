package com.whitelistchecker.domain.publicservice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import com.whitelistchecker.domain.model.AreaDetectionState
import com.whitelistchecker.domain.model.AreaSource
import com.whitelistchecker.domain.model.PublicServiceCatalog
import com.whitelistchecker.domain.model.UserArea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

class PublicServiceAreaDetector(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    suspend fun detect(nowMillis: Long = System.currentTimeMillis()): AreaDetectionResult {
        if (appContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return AreaDetectionResult.Error(AreaDetectionState.PERMISSION_DENIED, "Нет разрешения на приблизительное местоположение")
        }
        if (!Geocoder.isPresent()) {
            return AreaDetectionResult.Error(AreaDetectionState.GEOCODER_UNAVAILABLE, "Geocoder недоступен на устройстве")
        }
        val provider = selectProvider()
            ?: return AreaDetectionResult.Error(AreaDetectionState.LOCATION_DISABLED, "Геолокация отключена")
        val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            currentLocation(provider)
        } ?: return AreaDetectionResult.Error(AreaDetectionState.TIMEOUT, "Не удалось получить местоположение за отведённое время")
        val address = withTimeoutOrNull(GEOCODER_TIMEOUT_MS) {
            reverseGeocode(location)
        } ?: return AreaDetectionResult.Error(AreaDetectionState.TIMEOUT, "Geocoder не ответил вовремя")
        val area = normalizeAddress(address, nowMillis)
            ?: return AreaDetectionResult.Error(AreaDetectionState.NOT_FOUND, "Регион не найден в справочнике")
        return AreaDetectionResult.Success(area)
    }

    private fun selectProvider(): String? {
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.GPS_PROVIDER,
        )
        return providers.firstOrNull { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(provider: String): Location? {
        val cached = runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        if (cached != null && System.currentTimeMillis() - cached.time <= MAX_CACHED_LOCATION_AGE_MS) {
            return cached
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                locationManager.getCurrentLocation(
                    provider,
                    signal,
                    appContext.mainExecutor,
                ) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            }
        } else {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationManager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(location)
                    }

                    @Deprecated("Deprecated by Android platform")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                    override fun onProviderEnabled(provider: String) = Unit

                    override fun onProviderDisabled(provider: String) {
                        locationManager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
                continuation.invokeOnCancellation { locationManager.removeUpdates(listener) }
                runCatching {
                    locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                }.onFailure {
                    locationManager.removeUpdates(listener)
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
    }

    private suspend fun reverseGeocode(location: Location): Address? {
        val geocoder = Geocoder(appContext, Locale("ru", "RU"))
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    },
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
                }.getOrNull()
            }
        }
    }

    private fun normalizeAddress(address: Address?, nowMillis: Long): UserArea? {
        if (address == null) return null
        val region = PublicServiceCatalog.normalizeRegion(address.adminArea)
            ?: PublicServiceCatalog.normalizeRegion(address.subAdminArea)
            ?: return null
        val city = PublicServiceCatalog.normalizeCity(
            regionCode = region.code,
            value = address.locality ?: address.subAdminArea ?: address.subLocality,
        )
        return UserArea(
            countryCode = address.countryCode ?: "RU",
            regionCode = region.code,
            regionName = region.label,
            cityCode = city?.code,
            cityName = city?.label,
            customCityName = null,
            source = AreaSource.AUTOMATIC_LOCATION,
            confirmedByUser = false,
            updatedAtMillis = nowMillis,
        )
    }

    sealed interface AreaDetectionResult {
        data class Success(val area: UserArea) : AreaDetectionResult
        data class Error(
            val state: AreaDetectionState,
            val message: String,
        ) : AreaDetectionResult
    }

    private companion object {
        const val LOCATION_TIMEOUT_MS = 15_000L
        const val GEOCODER_TIMEOUT_MS = 10_000L
        const val MAX_CACHED_LOCATION_AGE_MS = 15 * 60_000L
    }
}
