package uz.hidoyat.app.feature.prayer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class UzbekRegion(
    val id: String,
    val name: String,
    val slug: String,
    val latitude: Double,
    val longitude: Double
)

data class UzbekDistrict(
    val id: String,
    val name: String,
    val slug: String,
    val parentId: String,
    val latitude: Double,
    val longitude: Double
)

/** namoz-vaqti.uz dagi haqiqiy hudud. */
data class NamozRegion(val slug: String, val name: String)

/** Open Admin Data district'ining namoz-vaqti.uz bilan moslik natijasi. */
sealed interface NamozMatch {
    data class Exact(val region: NamozRegion) : NamozMatch
    object Unresolved : NamozMatch
}

/**
 * Hududlar ro'yxati (assets/uzbekistan_regions.json) va ularning namoz-vaqti.uz sluglari
 * (assets/namoz_region_map.json). Hammasi offline.
 */
class UzbekRegionsRepository(context: Context) {

    private val regions: List<UzbekRegion>
    private val districts: List<UzbekDistrict>
    private val namozMap: Map<String, NamozRegion>
    private val namozRegions: List<NamozRegion>

    init {
        val assets = context.applicationContext.assets
        fun read(name: String): String =
            assets.open(name).bufferedReader().use { it.readText() }

        val root = JSONObject(read("uzbekistan_regions.json"))

        val provinceArray = root.getJSONArray("province")
        regions = List(provinceArray.length()) { index ->
            val item = provinceArray.getJSONObject(index)
            UzbekRegion(
                id = item.getString("id"),
                name = displayName(item.getString("name_local")),
                slug = item.getString("slug"),
                latitude = item.getDouble("lat"),
                longitude = item.getDouble("lon")
            )
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

        val districtArray = root.getJSONArray("district")
        districts = List(districtArray.length()) { index ->
            val item = districtArray.getJSONObject(index)
            val parentId = item.getString("parent_id")
            val raw = item.getString("name_local").trim()
            // "X ш." -> "X shahri"; oddiy nom tuman. Toshkent shahri ichidagi tumanlar o'z nomida.
            val name = when {
                raw.endsWith("ш.") -> displayName(raw)
                parentId == TASHKENT_CITY_ID -> raw
                else -> "$raw tumani"
            }
            UzbekDistrict(
                id = item.getString("id"),
                name = name,
                slug = item.getString("slug"),
                parentId = parentId,
                latitude = item.getDouble("lat"),
                longitude = item.getDouble("lon")
            )
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

        namozRegions = readNamozRegions(::read)
        namozMap = readNamozMap(::read)
    }

    fun getRegions(): List<UzbekRegion> = regions

    fun getDistricts(regionId: String): List<UzbekDistrict> =
        districts.filter { it.parentId == regionId }

    fun findRegion(id: String): UzbekRegion? = regions.firstOrNull { it.id == id }

    fun findDistrict(id: String): UzbekDistrict? =
        districts.firstOrNull { it.id == id }

    /** Aniq mapping bo'lsa [NamozMatch.Exact], aks holda [NamozMatch.Unresolved]. */
    fun matchFor(districtId: String): NamozMatch =
        namozMap[districtId]?.let { NamozMatch.Exact(it) } ?: NamozMatch.Unresolved

    /** Qo'lda tanlash uchun namoz-vaqti.uz ning barcha hududlari. */
    fun getNamozRegions(): List<NamozRegion> = namozRegions

    fun findNamozRegion(slug: String): NamozRegion? =
        namozRegions.firstOrNull { it.slug == slug }

    /**
     * Koordinataga eng yaqin district markazi. Markaz koordinatasi aniq chegara emas,
     * shuning uchun natija faqat taxminiy; [maxKm] dan uzoq bo'lsa null.
     */
    fun nearestDistrict(latitude: Double, longitude: Double, maxKm: Double = 150.0): UzbekDistrict? {
        val nearest = districts.minByOrNull {
            distanceKm(latitude, longitude, it.latitude, it.longitude)
        } ?: return null
        return nearest.takeIf {
            distanceKm(latitude, longitude, it.latitude, it.longitude) <= maxKm
        }
    }

    private fun readNamozRegions(read: (String) -> String): List<NamozRegion> = try {
        val array = JSONArray(read("namoz_regions.json"))
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            NamozRegion(item.getString("slug"), cleanName(item.getString("name")))
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    } catch (e: Exception) {
        emptyList()
    }

    private fun readNamozMap(read: (String) -> String): Map<String, NamozRegion> = try {
        val obj = JSONObject(read("namoz_region_map.json"))
        buildMap {
            obj.keys().forEach { id ->
                val item = obj.getJSONObject(id)
                val slug = item.optString("namozSlug")
                if (slug.isNotBlank()) {
                    put(id, NamozRegion(slug, cleanName(item.optString("namozName", slug))))
                }
            }
        }
    } catch (e: Exception) {
        emptyMap()
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * 6371.0 * asin(sqrt(a))
    }

    private companion object {
        const val TASHKENT_CITY_ID = "UZB010"

        fun displayName(raw: String): String = cleanName(raw.trim().replace(" ш.", " shahri"))

        fun cleanName(raw: String): String = raw.replace('‘', '\'').replace('’', '\'')
    }
}
