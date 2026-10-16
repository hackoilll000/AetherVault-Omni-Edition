package com.example.core.telegram

import org.json.JSONArray
import org.json.JSONObject

data class VaultManifest(
    val magic: String = "AETHER_MANIFEST_V1",
    val manifestSeq: Long = 1L,
    val hlcTimestamp: String = System.currentTimeMillis().toString(),
    val kdfSaltHex: String = "",
    val keyVerifyTokenB64: String = "",
    val snapshotDbChunkMsgIds: List<Int> = emptyList(),
    val thumbPackMsgIds: List<Int> = emptyList(),
    val deltaEventMsgIds: List<Int> = emptyList()
) {
    fun toJsonString(): String {
        val json = JSONObject().apply {
            put("magic", magic)
            put("manifest_seq", manifestSeq)
            put("hlc_timestamp", hlcTimestamp)
            put("kdf_salt_hex", kdfSaltHex)
            put("key_verify_token_b64", keyVerifyTokenB64)
            put("snapshot_db_chunk_msg_ids", JSONArray(snapshotDbChunkMsgIds))
            put("thumb_pack_msg_ids", JSONArray(thumbPackMsgIds))
            put("delta_event_msg_ids", JSONArray(deltaEventMsgIds))
        }
        return json.toString()
    }

    companion object {
        fun fromJson(raw: String): VaultManifest? {
            return try {
                val json = JSONObject(raw)
                val snapshotArr = json.optJSONArray("snapshot_db_chunk_msg_ids") ?: JSONArray()
                val snapshotList = mutableListOf<Int>()
                for (i in 0 until snapshotArr.length()) snapshotList.add(snapshotArr.getInt(i))

                val thumbArr = json.optJSONArray("thumb_pack_msg_ids") ?: JSONArray()
                val thumbList = mutableListOf<Int>()
                for (i in 0 until thumbArr.length()) thumbList.add(thumbArr.getInt(i))

                val deltaArr = json.optJSONArray("delta_event_msg_ids") ?: JSONArray()
                val deltaList = mutableListOf<Int>()
                for (i in 0 until deltaArr.length()) deltaList.add(deltaArr.getInt(i))

                VaultManifest(
                    magic = json.optString("magic", "AETHER_MANIFEST_V1"),
                    manifestSeq = json.optLong("manifest_seq", 1L),
                    hlcTimestamp = json.optString("hlc_timestamp", ""),
                    kdfSaltHex = json.optString("kdf_salt_hex", ""),
                    keyVerifyTokenB64 = json.optString("key_verify_token_b64", ""),
                    snapshotDbChunkMsgIds = snapshotList,
                    thumbPackMsgIds = thumbList,
                    deltaEventMsgIds = deltaList
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
