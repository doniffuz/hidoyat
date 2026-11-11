package uz.hidoyat.app.feature.dua

enum class DuaCategory(val title: String) {
    MORNING("Ertalabki duolar"),
    EVENING("Kechki duolar"),
    PRAYER("Namozdan keyin"),
    SLEEP("Uyqu duolari"),
    FOOD("Ovqatlanish duolari"),
    DAILY("Kundalik duolar")
}

data class DuaItem(
    val id: String,
    val category: DuaCategory,
    val title: String,
    val arabic: String,
    val transliteration: String,
    val meaning: String,
    val source: String
)
