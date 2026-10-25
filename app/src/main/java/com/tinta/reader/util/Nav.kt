package com.tinta.reader.util

/** Tampilan chapter: "Bab 12" bila judul sudah bernomor, jika tidak pakai judul apa adanya. */
fun displayLatest(latest: String?): String? =
    latest?.trim()?.takeIf { it.isNotEmpty() }?.let { if (it.length > 12) it else "Ch. $it" }

fun List<String>.joinGenres(max: Int = 3): String? =
    take(max).joinToString(", ").takeIf { it.isNotEmpty() }

/** "2025-03-01T10:20:00Z" -> "2025-03-01"; teks lain dibiarkan apa adanya. */
fun shortDate(raw: String?): String? {
    val s = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return if (s.length >= 10 && s[4] == '-' && s[7] == '-') s.take(10) else s
}
