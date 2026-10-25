package com.tinta.reader

import com.tinta.reader.util.displayLatest
import com.tinta.reader.util.joinGenres
import com.tinta.reader.util.shortDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UtilTest {
    @Test fun latestChapterLabel() {
        assertEquals("Ch. 200", displayLatest("200"))
        assertNull(displayLatest("  "))
        assertNull(displayLatest(null))
    }

    @Test fun shortDateKeepsOnlyIsoDay() {
        assertEquals("2025-03-01", shortDate("2025-03-01T10:20:00Z"))
        assertEquals("kemarin", shortDate("kemarin"))
        assertNull(shortDate(null))
    }

    @Test fun genresAreCapped() {
        assertEquals("A, B", listOf("A", "B", "C").joinGenres(2))
        assertNull(emptyList<String>().joinGenres())
    }
}
