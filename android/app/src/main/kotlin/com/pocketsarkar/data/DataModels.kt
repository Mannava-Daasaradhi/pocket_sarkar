package com.pocketsarkar.data

data class SchemeAlert(
    val title: String,
    val description: String,
    val date: String,
    val type: AlertType
)

enum class AlertType {
    DEADLINE, NEW_SCHEME, UPDATE
}

data class ApplicationStatus(
    val schemeName: String,
    val status: String, // "Submitted", "Processing", "Approved"
    val lastUpdate: String,
    val progress: Float // 0.0 to 1.0
)

data class OfficeLocation(
    val name: String,
    val address: String,
    val distance: String,
    val phone: String
)

object MockData {
    fun getAlerts(lang: String): List<SchemeAlert> {
        return listOf(
            SchemeAlert("Ration Card Update", "Last date to link Aadhaar is June 30th.", "Today", AlertType.DEADLINE),
            SchemeAlert("New Farmer Subsidy", "Apply for tractor subsidy by May 15th.", "Yesterday", AlertType.NEW_SCHEME),
            SchemeAlert("Ayushman Card News", "Free health checkup camp starting Monday.", "2 days ago", AlertType.UPDATE)
        )
    }

    fun getApplications(lang: String): List<ApplicationStatus> {
        return listOf(
            ApplicationStatus("PM Kisan Nidhi", "Approved", "Apr 20", 1.0f),
            ApplicationStatus("PM Awas Yojana", "Processing", "Apr 24", 0.6f),
            ApplicationStatus("Ujjwala Yojana", "Submitted", "Apr 25", 0.2f)
        )
    }

    fun getOffices(): List<OfficeLocation> {
        return listOf(
            OfficeLocation("MeeSeva Center", "Main Road, Ward 4", "0.8 km", "040-2345678"),
            OfficeLocation("Panchayat Office", "Village Square", "1.2 km", "040-9876543"),
            OfficeLocation("Jan Seva Kendra", "Opposite Bus Stand", "2.5 km", "040-1112223")
        )
    }
}
