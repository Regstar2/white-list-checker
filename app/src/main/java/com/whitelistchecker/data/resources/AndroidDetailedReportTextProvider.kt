package com.whitelistchecker.data.resources

import android.content.Context
import com.whitelistchecker.R
import com.whitelistchecker.domain.telegram.DetailedReportTextKey
import com.whitelistchecker.domain.telegram.DetailedReportTextProvider

class AndroidDetailedReportTextProvider(
    private val context: Context,
) : DetailedReportTextProvider {

    override fun text(
        key: DetailedReportTextKey,
        vararg args: Any,
    ): String {
        return context.getString(resourceId(key), *args)
    }

    private fun resourceId(key: DetailedReportTextKey): Int = when (key) {
        DetailedReportTextKey.TITLE -> R.string.report_title
        DetailedReportTextKey.FINAL_STATE -> R.string.report_final_state
        DetailedReportTextKey.SITE_SIGNAL -> R.string.report_site_signal
        DetailedReportTextKey.DNS_SIGNAL -> R.string.report_dns_signal
        DetailedReportTextKey.CHECKED_NETWORK -> R.string.report_checked_network
        DetailedReportTextKey.ACTIVE_NETWORK -> R.string.report_active_network
        DetailedReportTextKey.PRIVATE_DNS -> R.string.report_private_dns
        DetailedReportTextKey.PRIVATE_DNS_SERVER -> R.string.report_private_dns_server
        DetailedReportTextKey.CUSTOM_DNS -> R.string.report_custom_dns
        DetailedReportTextKey.FOREIGN_SITES_SUMMARY -> R.string.report_foreign_sites_summary
        DetailedReportTextKey.LOCAL_SITES_SUMMARY -> R.string.report_local_sites_summary
        DetailedReportTextKey.FOREIGN_DNS_SUMMARY -> R.string.report_foreign_dns_summary
        DetailedReportTextKey.LOCAL_DNS_SUMMARY -> R.string.report_local_dns_summary
        DetailedReportTextKey.CHECK_TIME -> R.string.report_check_time
        DetailedReportTextKey.TCP_DIAGNOSTICS -> R.string.report_tcp_diagnostics
        DetailedReportTextKey.ERROR -> R.string.report_error
        DetailedReportTextKey.DNS_NOT_RUN -> R.string.report_dns_not_run
        DetailedReportTextKey.DNS_HEADER -> R.string.report_dns_header
        DetailedReportTextKey.FOREIGN_DNS_GROUP -> R.string.report_foreign_dns_group
        DetailedReportTextKey.LOCAL_DNS_GROUP -> R.string.report_local_dns_group
        DetailedReportTextKey.DNS_SERVER -> R.string.report_dns_server
        DetailedReportTextKey.PROTOCOL -> R.string.report_protocol
        DetailedReportTextKey.STATUS -> R.string.report_status
        DetailedReportTextKey.RESPONSE_TIME -> R.string.report_response_time
        DetailedReportTextKey.RESOLVED_ADDRESS_COUNT -> R.string.report_resolved_address_count
        DetailedReportTextKey.ERROR_TYPE -> R.string.report_error_type
        DetailedReportTextKey.SITES_NOT_RUN -> R.string.report_sites_not_run
        DetailedReportTextKey.SITES_HEADER -> R.string.report_sites_header
        DetailedReportTextKey.FOREIGN_SITES_GROUP -> R.string.report_foreign_sites_group
        DetailedReportTextKey.LOCAL_SITES_GROUP -> R.string.report_local_sites_group
        DetailedReportTextKey.SITE -> R.string.report_site
        DetailedReportTextKey.HTTP -> R.string.report_http
        DetailedReportTextKey.UNAVAILABLE_HEADER -> R.string.report_unavailable_header
        DetailedReportTextKey.UNAVAILABLE_SITE -> R.string.report_unavailable_site
        DetailedReportTextKey.EVENT -> R.string.report_event
        DetailedReportTextKey.OLD_STATE -> R.string.report_old_state
        DetailedReportTextKey.NEW_STATE -> R.string.report_new_state
        DetailedReportTextKey.EVENT_TIME -> R.string.report_event_time
        DetailedReportTextKey.ACTIVE -> R.string.report_active
        DetailedReportTextKey.INACTIVE -> R.string.report_inactive
        DetailedReportTextKey.USED -> R.string.report_used
        DetailedReportTextKey.NOT_USED -> R.string.report_not_used
        DetailedReportTextKey.AVAILABLE -> R.string.report_available
        DetailedReportTextKey.UNAVAILABLE -> R.string.report_unavailable
        DetailedReportTextKey.NOT_AVAILABLE -> R.string.report_not_available
    }
}
