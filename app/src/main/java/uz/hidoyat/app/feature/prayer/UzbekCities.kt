package uz.hidoyat.app.feature.prayer

data class UzbekCity(
    val name: String,
    val regionSlug: String
)

/** Eski qo'lda yozilgan ro'yxat. Asosiy tanlov emas: UzbekRegionsRepository + namoz_region_map.json ishlatiladi. */
@Deprecated("UzbekRegionsRepository va namoz_region_map.json dan foydalaning")
object UzbekCities {

    val all = listOf(
        UzbekCity("Toshkent", "toshkent"),
        UzbekCity("Samarqand", "samarqand"),
        UzbekCity("Buxoro", "buxoro"),
        UzbekCity("Andijon", "andijon"),
        UzbekCity("Farg'ona", "fargona"),
        UzbekCity("Namangan", "namangan"),
        UzbekCity("Qarshi", "qarshi"),
        UzbekCity("Termiz", "termiz"),
        UzbekCity("Navoiy", "navoiy"),
        UzbekCity("Jizzax", "jizzax"),
        UzbekCity("Guliston", "sirdaryo"),
        UzbekCity("Nukus", "nukus"),
        UzbekCity("Urganch", "xorazm")
    )

    fun find(name: String): UzbekCity? =
        all.firstOrNull { it.name.equals(name, ignoreCase = true) }
}
