package uz.hidoyat.app.feature.zikr

data class ZikrItem(
    val id: String,
    val arabic: String,
    val transliteration: String,
    val meaning: String,
    val recommendedCount: Int
)

object ZikrCatalog {
    val items = listOf(
        ZikrItem(
            id = "subhanallah",
            arabic = "سُبْحَانَ اللَّهِ",
            transliteration = "Subhanalloh",
            meaning = "Alloh barcha nuqsonlardan pokdir",
            recommendedCount = 33
        ),
        ZikrItem(
            id = "alhamdulillah",
            arabic = "الْحَمْدُ لِلَّهِ",
            transliteration = "Alhamdulillah",
            meaning = "Barcha hamd va maqtov Allohga xosdir",
            recommendedCount = 33
        ),
        ZikrItem(
            id = "allahuakbar",
            arabic = "اللَّهُ أَكْبَرُ",
            transliteration = "Allohu akbar",
            meaning = "Alloh eng buyukdir",
            recommendedCount = 33
        ),
        ZikrItem(
            id = "astaghfirullah",
            arabic = "أَسْتَغْفِرُ اللَّهَ",
            transliteration = "Astag'firulloh",
            meaning = "Allohdan mag'firat so'rayman",
            recommendedCount = 100
        ),
        ZikrItem(
            id = "lailahaillallah",
            arabic = "لَا إِلَٰهَ إِلَّا اللَّهُ",
            transliteration = "La ilaha illalloh",
            meaning = "Allohdan o'zga iloh yo'q",
            recommendedCount = 100
        ),
        ZikrItem(
            id = "salavat",
            arabic = "اللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ",
            transliteration = "Allohumma solli 'ala Muhammad",
            meaning = "Allohim, Muhammadga salovot yo'llagin",
            recommendedCount = 100
        )
    )

    fun find(id: String): ZikrItem =
        items.firstOrNull { it.id == id } ?: items.first()
}
