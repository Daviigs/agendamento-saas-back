package lash_salao_kc.agendamento_back.service;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class WhatsAppService {

    private WebDriver driver;

    private WebDriver getDriver() {

        if (driver == null) {

            // 🔑 CAMINHO DO CHROMEDRIVER
            System.setProperty(
                    "webdriver.chrome.driver",
                    "C:\\webdriver\\chromedriver.exe"
            );

            ChromeOptions options = new ChromeOptions();

            options.setBinary(
                    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe"
            );

            options.addArguments("--user-data-dir=C:\\whatsapp-session");
            options.addArguments("--profile-directory=Default");
            options.addArguments("--disable-notifications");
            options.addArguments("--disable-infobars");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--no-sandbox");
            options.addArguments("--remote-allow-origins=*");

            // ❌ NÃO usar headless para WhatsApp Web

            driver = new ChromeDriver(options);

            // Abre o WhatsApp Web para login
            driver.get("https://web.whatsapp.com");

            System.out.println("➡ Escaneie o QR Code no WhatsApp Web (somente na primeira vez)");
        }

        return driver;
    }

    public void sendMessage(String phone, String message) {
        try {
            WebDriver driver = getDriver();

            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);

            String url = "https://web.whatsapp.com/send?phone="
                    + phone
                    + "&text="
                    + encodedMessage;

            driver.get(url);

            // Aguarda carregar a conversa
            Thread.sleep(8000);

            WebElement sendButton = driver.findElement(
                    By.xpath("//button[@aria-label='Enviar']")
            );

            sendButton.click();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
