package uz.hidoyat.app.feature.quran

/**
 * Suralar katalogi: raqam, nomlar, oyatlar soni va nozil bo'lgan joy (Tanzil tasnifi).
 * Bu yerda Qur'on MATNI yo'q — faqat tuzilma. Oyatlar soni jami 6236 ga teng bo'lishi shart
 * (tools/quran_data.py va ilova ichidagi tekshiruv shuni nazorat qiladi).
 * O'zbek kirill nomi [SurahInfo.nameCyrillic] orqali lotindan avtomatik o'giriladi.
 */
object QuranCatalog {
    const val SURAH_COUNT = 114
    const val AYAH_COUNT = 6236

    val surahs: List<SurahInfo> = listOf(
        SurahInfo(1, "Al-Fotiha", "Аль-Фатиха", 7, Revelation.MECCAN),
        SurahInfo(2, "Al-Baqara", "Аль-Бакара", 286, Revelation.MEDINAN),
        SurahInfo(3, "Oli Imron", "Али Имран", 200, Revelation.MEDINAN),
        SurahInfo(4, "An-Niso", "Ан-Ниса", 176, Revelation.MEDINAN),
        SurahInfo(5, "Al-Moida", "Аль-Маида", 120, Revelation.MEDINAN),
        SurahInfo(6, "Al-An'om", "Аль-Анам", 165, Revelation.MECCAN),
        SurahInfo(7, "Al-A'rof", "Аль-Араф", 206, Revelation.MECCAN),
        SurahInfo(8, "Al-Anfol", "Аль-Анфаль", 75, Revelation.MEDINAN),
        SurahInfo(9, "At-Tavba", "Ат-Тауба", 129, Revelation.MEDINAN),
        SurahInfo(10, "Yunus", "Юнус", 109, Revelation.MECCAN),
        SurahInfo(11, "Hud", "Худ", 123, Revelation.MECCAN),
        SurahInfo(12, "Yusuf", "Юсуф", 111, Revelation.MECCAN),
        SurahInfo(13, "Ar-Ra'd", "Ар-Рад", 43, Revelation.MEDINAN),
        SurahInfo(14, "Ibrohim", "Ибрахим", 52, Revelation.MECCAN),
        SurahInfo(15, "Al-Hijr", "Аль-Хиджр", 99, Revelation.MECCAN),
        SurahInfo(16, "An-Nahl", "Ан-Нахль", 128, Revelation.MECCAN),
        SurahInfo(17, "Al-Isro", "Аль-Исра", 111, Revelation.MECCAN),
        SurahInfo(18, "Al-Kahf", "Аль-Кахф", 110, Revelation.MECCAN),
        SurahInfo(19, "Maryam", "Марьям", 98, Revelation.MECCAN),
        SurahInfo(20, "Toha", "Та Ха", 135, Revelation.MECCAN),
        SurahInfo(21, "Al-Anbiyo", "Аль-Анбийа", 112, Revelation.MECCAN),
        SurahInfo(22, "Al-Haj", "Аль-Хадж", 78, Revelation.MEDINAN),
        SurahInfo(23, "Al-Mu'minun", "Аль-Муминун", 118, Revelation.MECCAN),
        SurahInfo(24, "An-Nur", "Ан-Нур", 64, Revelation.MEDINAN),
        SurahInfo(25, "Al-Furqon", "Аль-Фуркан", 77, Revelation.MECCAN),
        SurahInfo(26, "Ash-Shuaro", "Аш-Шуара", 227, Revelation.MECCAN),
        SurahInfo(27, "An-Naml", "Ан-Намль", 93, Revelation.MECCAN),
        SurahInfo(28, "Al-Qasas", "Аль-Касас", 88, Revelation.MECCAN),
        SurahInfo(29, "Al-Ankabut", "Аль-Анкабут", 69, Revelation.MECCAN),
        SurahInfo(30, "Ar-Rum", "Ар-Рум", 60, Revelation.MECCAN),
        SurahInfo(31, "Luqmon", "Лукман", 34, Revelation.MECCAN),
        SurahInfo(32, "As-Sajda", "Ас-Саджда", 30, Revelation.MECCAN),
        SurahInfo(33, "Al-Ahzob", "Аль-Ахзаб", 73, Revelation.MEDINAN),
        SurahInfo(34, "Saba", "Саба", 54, Revelation.MECCAN),
        SurahInfo(35, "Fotir", "Фатыр", 45, Revelation.MECCAN),
        SurahInfo(36, "Yosin", "Йа Син", 83, Revelation.MECCAN),
        SurahInfo(37, "As-Soffot", "Ас-Саффат", 182, Revelation.MECCAN),
        SurahInfo(38, "Sod", "Сад", 88, Revelation.MECCAN),
        SurahInfo(39, "Az-Zumar", "Аз-Зумар", 75, Revelation.MECCAN),
        SurahInfo(40, "G'ofir", "Гафир", 85, Revelation.MECCAN),
        SurahInfo(41, "Fussilat", "Фуссылат", 54, Revelation.MECCAN),
        SurahInfo(42, "Ash-Shuro", "Аш-Шура", 53, Revelation.MECCAN),
        SurahInfo(43, "Az-Zuxruf", "Аз-Зухруф", 89, Revelation.MECCAN),
        SurahInfo(44, "Ad-Duxon", "Ад-Духан", 59, Revelation.MECCAN),
        SurahInfo(45, "Al-Josiya", "Аль-Джасия", 37, Revelation.MECCAN),
        SurahInfo(46, "Al-Ahqof", "Аль-Ахкаф", 35, Revelation.MECCAN),
        SurahInfo(47, "Muhammad", "Мухаммад", 38, Revelation.MEDINAN),
        SurahInfo(48, "Al-Fath", "Аль-Фатх", 29, Revelation.MEDINAN),
        SurahInfo(49, "Al-Hujurot", "Аль-Худжурат", 18, Revelation.MEDINAN),
        SurahInfo(50, "Qof", "Каф", 45, Revelation.MECCAN),
        SurahInfo(51, "Az-Zoriyot", "Аз-Зарият", 60, Revelation.MECCAN),
        SurahInfo(52, "At-Tur", "Ат-Тур", 49, Revelation.MECCAN),
        SurahInfo(53, "An-Najm", "Ан-Наджм", 62, Revelation.MECCAN),
        SurahInfo(54, "Al-Qamar", "Аль-Камар", 55, Revelation.MECCAN),
        SurahInfo(55, "Ar-Rahmon", "Ар-Рахман", 78, Revelation.MEDINAN),
        SurahInfo(56, "Al-Voqe'a", "Аль-Вакиа", 96, Revelation.MECCAN),
        SurahInfo(57, "Al-Hadid", "Аль-Хадид", 29, Revelation.MEDINAN),
        SurahInfo(58, "Al-Mujodala", "Аль-Муджадаля", 22, Revelation.MEDINAN),
        SurahInfo(59, "Al-Hashr", "Аль-Хашр", 24, Revelation.MEDINAN),
        SurahInfo(60, "Al-Mumtahana", "Аль-Мумтахана", 13, Revelation.MEDINAN),
        SurahInfo(61, "As-Saff", "Ас-Сафф", 14, Revelation.MEDINAN),
        SurahInfo(62, "Al-Jum'a", "Аль-Джумуа", 11, Revelation.MEDINAN),
        SurahInfo(63, "Al-Munofiqun", "Аль-Мунафикун", 11, Revelation.MEDINAN),
        SurahInfo(64, "At-Tag'obun", "Ат-Тагабун", 18, Revelation.MEDINAN),
        SurahInfo(65, "At-Taloq", "Ат-Талак", 12, Revelation.MEDINAN),
        SurahInfo(66, "At-Tahrim", "Ат-Тахрим", 12, Revelation.MEDINAN),
        SurahInfo(67, "Al-Mulk", "Аль-Мульк", 30, Revelation.MECCAN),
        SurahInfo(68, "Al-Qalam", "Аль-Калям", 52, Revelation.MECCAN),
        SurahInfo(69, "Al-Hoqqa", "Аль-Хакка", 52, Revelation.MECCAN),
        SurahInfo(70, "Al-Ma'orij", "Аль-Мааридж", 44, Revelation.MECCAN),
        SurahInfo(71, "Nuh", "Нух", 28, Revelation.MECCAN),
        SurahInfo(72, "Al-Jin", "Аль-Джинн", 28, Revelation.MECCAN),
        SurahInfo(73, "Al-Muzzammil", "Аль-Музаммиль", 20, Revelation.MECCAN),
        SurahInfo(74, "Al-Muddassir", "Аль-Муддассир", 56, Revelation.MECCAN),
        SurahInfo(75, "Al-Qiyoma", "Аль-Кыяма", 40, Revelation.MECCAN),
        SurahInfo(76, "Al-Inson", "Аль-Инсан", 31, Revelation.MEDINAN),
        SurahInfo(77, "Al-Mursalot", "Аль-Мурсалят", 50, Revelation.MECCAN),
        SurahInfo(78, "An-Naba", "Ан-Наба", 40, Revelation.MECCAN),
        SurahInfo(79, "An-Nozi'ot", "Ан-Назиат", 46, Revelation.MECCAN),
        SurahInfo(80, "Abasa", "Абаса", 42, Revelation.MECCAN),
        SurahInfo(81, "At-Takvir", "Ат-Таквир", 29, Revelation.MECCAN),
        SurahInfo(82, "Al-Infitor", "Аль-Инфитар", 19, Revelation.MECCAN),
        SurahInfo(83, "Al-Mutoffifin", "Аль-Мутаффифин", 36, Revelation.MECCAN),
        SurahInfo(84, "Al-Inshiqoq", "Аль-Иншикак", 25, Revelation.MECCAN),
        SurahInfo(85, "Al-Buruj", "Аль-Бурудж", 22, Revelation.MECCAN),
        SurahInfo(86, "At-Toriq", "Ат-Тарик", 17, Revelation.MECCAN),
        SurahInfo(87, "Al-A'lo", "Аль-Аля", 19, Revelation.MECCAN),
        SurahInfo(88, "Al-G'oshiya", "Аль-Гашия", 26, Revelation.MECCAN),
        SurahInfo(89, "Al-Fajr", "Аль-Фаджр", 30, Revelation.MECCAN),
        SurahInfo(90, "Al-Balad", "Аль-Баляд", 20, Revelation.MECCAN),
        SurahInfo(91, "Ash-Shams", "Аш-Шамс", 15, Revelation.MECCAN),
        SurahInfo(92, "Al-Layl", "Аль-Лайль", 21, Revelation.MECCAN),
        SurahInfo(93, "Ad-Duho", "Ад-Духа", 11, Revelation.MECCAN),
        SurahInfo(94, "Ash-Sharh", "Аш-Шарх", 8, Revelation.MECCAN),
        SurahInfo(95, "At-Tin", "Ат-Тин", 8, Revelation.MECCAN),
        SurahInfo(96, "Al-Alaq", "Аль-Алак", 19, Revelation.MECCAN),
        SurahInfo(97, "Al-Qadr", "Аль-Кадр", 5, Revelation.MECCAN),
        SurahInfo(98, "Al-Bayyina", "Аль-Баййина", 8, Revelation.MEDINAN),
        SurahInfo(99, "Az-Zalzala", "Аз-Зальзаля", 8, Revelation.MEDINAN),
        SurahInfo(100, "Al-Odiyot", "Аль-Адият", 11, Revelation.MECCAN),
        SurahInfo(101, "Al-Qori'a", "Аль-Кариа", 11, Revelation.MECCAN),
        SurahInfo(102, "At-Takosur", "Ат-Таказур", 8, Revelation.MECCAN),
        SurahInfo(103, "Al-Asr", "Аль-Аср", 3, Revelation.MECCAN),
        SurahInfo(104, "Al-Humaza", "Аль-Хумаза", 9, Revelation.MECCAN),
        SurahInfo(105, "Al-Fil", "Аль-Филь", 5, Revelation.MECCAN),
        SurahInfo(106, "Quraysh", "Курайш", 4, Revelation.MECCAN),
        SurahInfo(107, "Al-Mo'un", "Аль-Маун", 7, Revelation.MECCAN),
        SurahInfo(108, "Al-Kavsar", "Аль-Каусар", 3, Revelation.MECCAN),
        SurahInfo(109, "Al-Kofirun", "Аль-Кафирун", 6, Revelation.MECCAN),
        SurahInfo(110, "An-Nasr", "Ан-Наср", 3, Revelation.MEDINAN),
        SurahInfo(111, "Al-Masad", "Аль-Масад", 5, Revelation.MECCAN),
        SurahInfo(112, "Al-Ixlos", "Аль-Ихляс", 4, Revelation.MECCAN),
        SurahInfo(113, "Al-Falaq", "Аль-Фаляк", 5, Revelation.MECCAN),
        SurahInfo(114, "An-Nos", "Ан-Нас", 6, Revelation.MECCAN)
    )

    fun surah(number: Int): SurahInfo? = surahs.getOrNull(number - 1)

    fun ayahCount(surah: Int): Int = surah(surah)?.ayahCount ?: 0

    fun isValid(surah: Int, ayah: Int): Boolean {
        val info = surah(surah) ?: return false
        return ayah in 1..info.ayahCount
    }

    /** Noto'g'ri qiymatlarni eng yaqin to'g'ri oyatga keltiradi (crash bo'lmasligi uchun). */
    fun clamp(surah: Int, ayah: Int): AyahKey {
        val s = surah.coerceIn(1, SURAH_COUNT)
        return AyahKey(s, ayah.coerceIn(1, ayahCount(s)))
    }

    init {
        check(surahs.size == SURAH_COUNT) { "Katalog: 114 sura bo'lishi kerak" }
        check(surahs.sumOf { it.ayahCount } == AYAH_COUNT) { "Katalog: oyatlar yig'indisi 6236 bo'lishi kerak" }
        check(surahs.withIndex().all { (i, s) -> s.number == i + 1 }) { "Katalog: raqamlar ketma-ket emas" }
    }
}
