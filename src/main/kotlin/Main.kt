package com.renegatemaster.airqualitybot

import com.github.kotlintelegrambot.*
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.location
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.TelegramFile
import io.github.cdimascio.dotenv.dotenv
import mu.KotlinLogging
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.WebDriverWait
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import java.time.Duration
import javax.imageio.ImageIO
import kotlin.concurrent.thread

const val START_PHRASE: String = "Hi. Send me your location and I'll send you an air quality report."

private val logger = KotlinLogging.logger {}

fun createDriver(): WebDriver {
    val seleniumUri = URI(dotenv()["SELENIUM_URL"]).toURL()
    val chromeOptions = ChromeOptions().apply {
        setExperimentalOption("mobileEmulation", mapOf("deviceName" to "iPad"))
        addArguments("--headless", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage")
    }
    return RemoteWebDriver(seleniumUri, chromeOptions)
}

fun captureAirQualityMap(lat: Float, lng: Float): File {
    val driver = createDriver() // Создаём WebDriver перед началом работы
    try {
        val mapUrl = "https://www.iqair.com/ru/air-quality-map?lat=$lat&lng=$lng&zoomLevel=12"
        driver.get(mapUrl)

        WebDriverWait(driver, Duration.ofSeconds(5)).until {
            (it as JavascriptExecutor).executeScript("return document.readyState") == "complete"
        }

        // Скрываем элементы
        listOf(
            "/html/body/div/div",
            "/html/body/app-root/app-air-quality-map/app-iqair-map-earth-container/div/div[2]/app-osm-fullmap/div[2]/map-dialog-container/div"
        ).forEach { xpath ->
            driver.findElements(By.xpath(xpath)).firstOrNull()?.let {
                (driver as JavascriptExecutor).executeScript("arguments[0].style.display = 'none';", it)
            }
        }

        Thread.sleep(500)

        val screenshot = (driver as TakesScreenshot).getScreenshotAs(OutputType.FILE)
        val image: BufferedImage = ImageIO.read(screenshot)

        val croppedImage = image.getSubimage(0, 640, 1536, 1100)

        val outputFile = File("air-quality-map.png")
        ImageIO.write(croppedImage, "png", outputFile)

        return outputFile
    } finally {
        driver.quit()
    }
}

fun main() {
    val botToken = dotenv()["BOT_TOKEN"] ?: error("BOT_TOKEN not found")
    val bot = bot {
        token = botToken
        dispatch {
            command("start") {
                bot.sendMessage(
                    chatId = ChatId.fromId(update.message!!.chat.id),
                    text = START_PHRASE
                )
            }

            location {
                logger.info("Request for air quality report")
                val chatId = ChatId.fromId(message.chat.id)

                thread {
                    val screenshot = captureAirQualityMap(location.latitude, location.longitude)
                    bot.sendPhoto(
                        chatId = chatId,
                        photo = TelegramFile.ByFile(screenshot),
                    )
                    logger.info("Air quality report lat=${location.latitude}, lng=${location.longitude} send")
                }

                bot.sendMessage(chatId, "Generating air quality report, please wait...")
            }
        }
    }

    bot.startPolling()
}

