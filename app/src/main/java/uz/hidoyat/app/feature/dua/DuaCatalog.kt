package uz.hidoyat.app.feature.dua

object DuaCatalog {
    val items = listOf(
        DuaItem(
            id = "wake_up",
            category = DuaCategory.MORNING,
            title = "Uyqudan uyg‘onganda",
            arabic = "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            transliteration = "Alhamdu lillahil-laziy ahyana ba’da ma amatana va ilayhin-nushur.",
            meaning = "Bizni o‘limdan keyin tiriltirgan Allohga hamd bo‘lsin. Qaytish ham Uning huzurigadir.",
            source = "Sahih al-Buxoriy, 6312"
        ),
        DuaItem(
            id = "sleep",
            category = DuaCategory.SLEEP,
            title = "Uyqudan oldin",
            arabic = "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا",
            transliteration = "Bismika Allohumma amutu va ahya.",
            meaning = "Allohim, Sening noming bilan o‘laman va tirilaman.",
            source = "Sahih al-Buxoriy, 6324"
        ),
        DuaItem(
            id = "before_food",
            category = DuaCategory.FOOD,
            title = "Ovqatdan oldin",
            arabic = "بِسْمِ اللَّهِ",
            transliteration = "Bismillah.",
            meaning = "Allohning nomi bilan.",
            source = "Sunan Abu Dovud, 3767"
        ),
        DuaItem(
            id = "after_food",
            category = DuaCategory.FOOD,
            title = "Ovqatdan keyin",
            arabic = "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنِي هَذَا وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ",
            transliteration = "Alhamdu lillahil-laziy at’amani haza va razaqanihi min g‘ayri havlin minni va la quvvah.",
            meaning = "Menga bu taomni yedirgan va uni mening kuch-quvvatimsiz rizq qilib bergan Allohga hamd bo‘lsin.",
            source = "Sunan Abu Dovud, 4023"
        ),
        DuaItem(
            id = "forgiveness",
            category = DuaCategory.DAILY,
            title = "Istig‘for",
            arabic = "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ",
            transliteration = "Astaghfirulloha va atubu ilayh.",
            meaning = "Allohdan mag‘firat so‘rayman va Unga tavba qilaman.",
            source = "Sahih al-Buxoriy, 6307"
        ),
        DuaItem(
            id = "after_prayer",
            category = DuaCategory.PRAYER,
            title = "Namozdan keyin istig‘for",
            arabic = "أَسْتَغْفِرُ اللَّهَ",
            transliteration = "Astaghfirulloh.",
            meaning = "Allohdan mag‘firat so‘rayman.",
            source = "Sahih Muslim, 591"
        ),
        DuaItem(
            id = "morning_protection",
            category = DuaCategory.MORNING,
            title = "Himoya so‘rash duosi",
            arabic = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
            transliteration = "Bismillahil-laziy la yadurru ma’asmihi shay’un fil-ardi va la fis-sama’i va huvas-sami’ul-‘alim.",
            meaning = "Allohning nomi bilan. Uning nomi bilan yeru osmonda hech narsa zarar yetkaza olmaydi. U Eshituvchi va Biluvchidir.",
            source = "Sunan Abu Dovud, 5088; Jome’ at-Termiziy, 3388"
        )
    )

    fun byCategory(category: DuaCategory): List<DuaItem> =
        items.filter { it.category == category }

    fun find(id: String): DuaItem? =
        items.firstOrNull { it.id == id }

    fun search(query: String): List<DuaItem> {
        val value = query.trim()
        if (value.isEmpty()) return items
        return items.filter {
            it.title.contains(value, ignoreCase = true) ||
                it.transliteration.contains(value, ignoreCase = true) ||
                it.meaning.contains(value, ignoreCase = true)
        }
    }
}
