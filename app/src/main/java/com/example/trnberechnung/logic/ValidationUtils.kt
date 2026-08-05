package com.example.trnberechnung.logic

import com.example.trnberechnung.model.CrewMember

object ValidationUtils {

    private val NAME_REGEX = Regex("[^a-zA-ZÀ-ÿ\\s]")
    private val SKIPPER_ID_REGEX = Regex("[^a-zA-Z0-9]")
    private val PHONE_REGEX = Regex("[^0-9\\+]")
    private val MEDICAL_NOTES_REGEX = Regex("[^a-zA-Z0-9À-ÿ\\s]")

    fun sanitizeName(name: String): String = name.replace(NAME_REGEX, "")

    fun sanitizeSkipperId(id: String): String = id.replace(SKIPPER_ID_REGEX, "")

    fun sanitizePhone(phone: String): String = phone.replace(PHONE_REGEX, "")

    fun sanitizeMedicalNotes(notes: String): String = notes.replace(MEDICAL_NOTES_REGEX, "")

    /**
     * Sanitize all relevant fields of a [CrewMember].
     */
    fun sanitizeCrewMember(member: CrewMember): CrewMember {
        return member.copy(
            name = sanitizeName(member.name),
            skipperId = sanitizeSkipperId(member.skipperId),
            phone = sanitizePhone(member.phone),
            emergencyPhone = sanitizePhone(member.emergencyPhone),
            emergencyContact = sanitizeName(member.emergencyContact),
            medicalNote = sanitizeMedicalNotes(member.medicalNote),
            medicalNotes = sanitizeMedicalNotes(member.medicalNotes)
        )
    }
}
