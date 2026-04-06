package com.simats.goiretrieval

import com.simats.goiretrieval.api.ApiDocument

object DocumentRepository {
    private val savedDocs = mutableListOf<ApiDocument>()

    fun setSavedDocs(docs: List<ApiDocument>) {
        savedDocs.clear()
        savedDocs.addAll(docs)
    }

    fun isSaved(docId: String?): Boolean {
        if (docId == null) return false
        val normalizedId = docId.trim().uppercase()
        return savedDocs.any { it.id?.trim()?.uppercase() == normalizedId }
    }

    fun addSavedDoc(doc: ApiDocument) {
        if (!isSaved(doc.id)) {
            savedDocs.add(doc)
        }
    }

    fun removeSavedDoc(docId: String?) {
        if (docId == null) return
        val normalizedId = docId.trim().uppercase()
        savedDocs.removeAll { it.id?.trim()?.uppercase() == normalizedId }
    }

    fun getSavedDocs(): List<ApiDocument> {
        return savedDocs.toList()
    }
}
