package com.example.data.weather

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

data class CityCoordinates(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val latitude: Double,
    val longitude: Double,
    val regionTag: String
)

data class HourlyForecastItem(
    val hourLabel: String,
    val tempC: Int,
    val uvIndex: Double
)

data class WeatherInfo(
    val cityId: String,
    val cityNameAr: String,
    val cityNameEn: String,
    val temperatureC: Double,
    val feelsLikeC: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val uvIndex: Double,
    val conditionAr: String,
    val conditionEn: String,
    val heatRiskLevel: String, // NORMAL, CAUTION, EXTREME_CAUTION, DANGER
    val heatRiskAr: String,
    val heatRiskEn: String,
    val hourlyForecast: List<HourlyForecastItem>,
    val isLive: Boolean,
    val updatedAtEpochMs: Long
)

object WeatherService {

    private const val PREFS_NAME = "hydramind_weather_cache"

    val supportedCities = listOf(
        CityCoordinates("Baghdad", "بغداد", "Baghdad", 33.3152, 44.3661, "IRAQ"),
        CityCoordinates("Basra", "البصرة", "Basra", 30.5085, 47.7804, "IRAQ_SOUTH"),
        CityCoordinates("Najaf", "النجف الأشرف", "Najaf", 32.0000, 44.3333, "IRAQ"),
        CityCoordinates("Karbala", "كربلاء", "Karbala", 32.6160, 44.0249, "IRAQ"),
        CityCoordinates("Erbil", "أربيل", "Erbil", 36.1901, 44.0091, "IRAQ_NORTH"),
        CityCoordinates("Mosul", "الموصل", "Mosul", 36.3489, 43.1577, "IRAQ_NORTH"),
        CityCoordinates("Nasiriyah", "الناصرية (ذي قار)", "Nasiriyah", 31.0439, 46.2573, "IRAQ_SOUTH"),
        CityCoordinates("Amarah", "العمارة (ميسان)", "Amarah", 31.8356, 47.1448, "IRAQ_SOUTH"),
        CityCoordinates("Kirkuk", "كركوك", "Kirkuk", 35.4681, 44.3922, "IRAQ"),
        CityCoordinates("Sulaymaniyah", "السليمانية", "Sulaymaniyah", 35.5650, 45.4329, "IRAQ_NORTH"),
        CityCoordinates("Dubai", "دبي", "Dubai", 25.2048, 55.2708, "GULF"),
        CityCoordinates("Riyadh", "الرياض", "Riyadh", 24.7136, 46.6753, "GULF")
    )

    fun findCity(cityId: String): CityCoordinates {
        return supportedCities.firstOrNull { it.id.equals(cityId, ignoreCase = true) }
            ?: supportedCities.first()
    }

    suspend fun fetchWeather(context: Context, cityId: String): WeatherInfo = withContext(Dispatchers.IO) {
        val city = findCity(cityId)
        try {
            val urlString = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${city.latitude}&longitude=${city.longitude}" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m,weather_code" +
                "&hourly=temperature_2m,uv_index&forecast_days=1&timezone=auto"

            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val current = json.getJSONObject("current")
                val temp = current.optDouble("temperature_2m", 36.0)
                val apparent = current.optDouble("apparent_temperature", temp + 1.5)
                val humidity = current.optInt("relative_humidity_2m", 28)
                val wind = current.optDouble("wind_speed_10m", 14.0)
                val weatherCode = current.optInt("weather_code", 0)

                val hourlyObj = json.optJSONObject("hourly")
                val hourlyTimes = hourlyObj?.optJSONArray("time")
                val hourlyTemps = hourlyObj?.optJSONArray("temperature_2m")
                val hourlyUvs = hourlyObj?.optJSONArray("uv_index")

                val hourlyList = mutableListOf<HourlyForecastItem>()
                var maxUv = 7.0
                if (hourlyTimes != null && hourlyTemps != null) {
                    val indices = listOf(9, 12, 15, 18, 21)
                    for (idx in indices) {
                        if (idx < hourlyTimes.length() && idx < hourlyTemps.length()) {
                            val rawTime = hourlyTimes.optString(idx, "12:00")
                            val hourLabel = rawTime.substringAfter("T", "12:00")
                            val hTemp = hourlyTemps.optDouble(idx, temp).roundToInt()
                            val hUv = hourlyUvs?.optDouble(idx, 5.0) ?: 5.0
                            if (hUv > maxUv) maxUv = hUv
                            hourlyList.add(HourlyForecastItem(hourLabel, hTemp, hUv))
                        }
                    }
                }

                val (condAr, condEn) = mapWeatherCode(weatherCode, temp)
                val (riskKey, riskAr, riskEn) = evaluateHeatRisk(temp, apparent)

                val info = WeatherInfo(
                    cityId = city.id,
                    cityNameAr = city.nameAr,
                    cityNameEn = city.nameEn,
                    temperatureC = temp,
                    feelsLikeC = apparent,
                    humidityPercent = humidity,
                    windSpeedKmh = wind,
                    uvIndex = maxUv,
                    conditionAr = condAr,
                    conditionEn = condEn,
                    heatRiskLevel = riskKey,
                    heatRiskAr = riskAr,
                    heatRiskEn = riskEn,
                    hourlyForecast = hourlyList,
                    isLive = true,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
                saveCachedWeather(context, info)
                return@withContext info
            }
        } catch (_: Exception) {
            // Fall through to cached offline weather
        }
        return@withContext loadCachedOrFallbackWeather(context, city)
    }

    private fun saveCachedWeather(context: Context, info: WeatherInfo) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("cityId_${info.cityId}", info.cityId)
            .putFloat("temp_${info.cityId}", info.temperatureC.toFloat())
            .putFloat("feels_${info.cityId}", info.feelsLikeC.toFloat())
            .putInt("hum_${info.cityId}", info.humidityPercent)
            .putFloat("wind_${info.cityId}", info.windSpeedKmh.toFloat())
            .putFloat("uv_${info.cityId}", info.uvIndex.toFloat())
            .putLong("time_${info.cityId}", info.updatedAtEpochMs)
            .apply()
    }

    private fun loadCachedOrFallbackWeather(context: Context, city: CityCoordinates): WeatherInfo {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultTemp = when (city.regionTag) {
            "IRAQ_SOUTH" -> 44.0
            "IRAQ_NORTH" -> 33.0
            else -> 39.0
        }
        val temp = prefs.getFloat("temp_${city.id}", defaultTemp.toFloat()).toDouble()
        val feels = prefs.getFloat("feels_${city.id}", (defaultTemp + 2.0).toFloat()).toDouble()
        val hum = prefs.getInt("hum_${city.id}", 24)
        val wind = prefs.getFloat("wind_${city.id}", 15.0f).toDouble()
        val uv = prefs.getFloat("uv_${city.id}", 9.0f).toDouble()
        val savedTime = prefs.getLong("time_${city.id}", System.currentTimeMillis() - 1800_000L)

        val (condAr, condEn) = mapWeatherCode(0, temp)
        val (riskKey, riskAr, riskEn) = evaluateHeatRisk(temp, feels)

        val fallbackHourly = listOf(
            HourlyForecastItem("09:00", (temp - 5).roundToInt(), 5.0),
            HourlyForecastItem("12:00", (temp - 1).roundToInt(), 9.2),
            HourlyForecastItem("15:00", (temp + 1).roundToInt(), 8.5),
            HourlyForecastItem("18:00", (temp - 3).roundToInt(), 2.0),
            HourlyForecastItem("21:00", (temp - 6).roundToInt(), 0.0)
        )

        return WeatherInfo(
            cityId = city.id,
            cityNameAr = city.nameAr,
            cityNameEn = city.nameEn,
            temperatureC = temp,
            feelsLikeC = feels,
            humidityPercent = hum,
            windSpeedKmh = wind,
            uvIndex = uv,
            conditionAr = condAr,
            conditionEn = condEn,
            heatRiskLevel = riskKey,
            heatRiskAr = riskAr,
            heatRiskEn = riskEn,
            hourlyForecast = fallbackHourly,
            isLive = false,
            updatedAtEpochMs = savedTime
        )
    }

    private fun mapWeatherCode(code: Int, tempC: Double): Pair<String, String> {
        return when {
            tempC >= 43.0 -> "شديد الحرارة ومشمس" to "Extreme Heat & Sunny"
            code == 0 -> "صافي ومشمس" to "Clear Sky"
            code in 1..3 -> "غائم جزئياً" to "Partly Cloudy"
            code in 45..48 -> "غبار أو ضباب خفيف" to "Hazy / Dusty"
            code in 51..67 -> "زخات مطر" to "Light Rain"
            else -> "مشمس" to "Sunny"
        }
    }

    private fun evaluateHeatRisk(tempC: Double, feelsC: Double): Triple<String, String, String> {
        val peak = maxOf(tempC, feelsC)
        return when {
            peak >= 44.0 -> Triple(
                "DANGER",
                "خطر حراري شديد (وضع حرارة العراق مفعل)",
                "Severe Heat Danger (Iraq Heat Mode Active)"
            )
            peak >= 37.0 -> Triple(
                "EXTREME_CAUTION",
                "حذر شديد: إجهاد حراري مرتفع",
                "High Caution: Elevated Thermal Stress"
            )
            peak >= 30.0 -> Triple(
                "CAUTION",
                "تنبيه حراري متوسط: حافظ على شرب الماء",
                "Moderate Heat Caution: Stay Hydrated"
            )
            else -> Triple(
                "NORMAL",
                "أجواء مريحة ومعتدلة",
                "Comfortable Thermal Conditions"
            )
        }
    }
}
