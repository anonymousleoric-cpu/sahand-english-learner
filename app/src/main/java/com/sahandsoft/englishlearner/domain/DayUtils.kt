package com.sahandsoft.englishlearner.domain

import java.util.Calendar

object DayUtils {

    /** روز محلی به‌صورت شمارهٔ ترتیبی (برای محاسبهٔ زنجیره و آمار) */
    fun localEpochDay(): Long {
        val c = Calendar.getInstance()
        return (c.timeInMillis + c.get(Calendar.ZONE_OFFSET) + c.get(Calendar.DST_OFFSET)) / 86_400_000L
    }

    /** برچسب کوتاه تاریخ مثل 12/3 */
    fun dateLabel(epochDay: Long): String {
        val c = Calendar.getInstance()
        c.timeInMillis = epochDay * 86_400_000L
        return "${c.get(Calendar.MONTH) + 1}/${c.get(Calendar.DAY_OF_MONTH)}"
    }

    /** تبدیل ارقام لاتین به فارسی برای نمایش */
    fun fa(number: Int): String = fa(number.toString())

    fun fa(text: String): String {
        val faDigits = listOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (ch in text) {
            if (ch.isDigit()) sb.append(faDigits[ch - '0']) else sb.append(ch)
        }
        return sb.toString()
    }

    /** محاسبهٔ زنجیرهٔ روزهای فعال */
    fun streak(activeDays: Set<Long>, today: Long): Int {
        var d = today
        if (d !in activeDays) d -= 1
        var s = 0
        while (activeDays.contains(d)) {
            s++
            d -= 1
        }
        return s
    }
}
