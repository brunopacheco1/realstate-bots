package com.github.brunopacheco1.realstate.bots.immotop;

import java.math.BigDecimal;
import java.util.logging.Level;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.brunopacheco1.realstate.PubSubConstants;
import com.github.brunopacheco1.realstate.api.PropertyDto;
import com.github.brunopacheco1.realstate.api.PropertyType;
import com.github.brunopacheco1.realstate.api.Source;
import com.github.brunopacheco1.realstate.api.TransactionType;
import com.google.common.base.Optional;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.quarkus.scheduler.Scheduled;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class ImmotopCrawler {

    @Inject
    @Channel(PubSubConstants.INCOMING_PROPERTY)
    Emitter<PropertyDto> incomingPropertyEmitter;

    @Scheduled(cron = "{scheduler.immotop}")
    public void produces() {
        log.info("Starting crawling.");

        try {
            var driver = initWebDriver();
            for (var transactionType : TransactionType.values()) {
                var page = 1;
                var maxPages = 20;
                while (true) {
                    if (page > maxPages) {
                        break;
                    }
                    var entity = getWebsite(driver, transactionType, page);
                    if (entity != null) {
                        var doc = Jsoup.parse(entity);
                        var elements = doc.select("div.in-reListCard > div.in-reListCard__content");
                        elements.forEach(el -> {
                            getProperty(el, transactionType);
                        });
                    }

                    page++;
                    Thread.sleep(1000);
                }
            }
            driver.quit();
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
    }

    public WebDriver initWebDriver() {
        var options = new ChromeOptions();
        options.addArguments("--headless");
        return new ChromeDriver(options);
    }

    private String getWebsite(WebDriver driver, TransactionType transactionType, Integer page) throws Exception {
        var pageString = page == 1 ? "" : "&pag=" + page;
        var transactionTypeInImmotop = transactionType == TransactionType.BUY ? "vente" : "location";
        var url = "https://www.immotop.lu/en/" + transactionTypeInImmotop
                + "-maisons-appartements/luxembourg-pays/?criterio=rilevanza&noAste=1" + pageString;

        driver.get(url);
        Thread.sleep(2000);
        return driver.getPageSource();
    }

    private void getProperty(Element el, TransactionType transactionType) {
        try {
            var link = el.select("a");
            var propertyUrl = link.attr("href");
            var location = getLocation(link.text());
            var propertyType = getPropertyType(el.select("li.in-feat__item[aria-label='floor']").text());
            var value = getPrice(el.select("div.in-reListCardPrice").text());
            var source = Source.IMMOTOP;
            var numberOfBedrooms = getNumberOfBedrooms(el.select("li.in-feat__item[aria-label='rooms']").text());
            var property = new PropertyDto(location, value, propertyType, transactionType, propertyUrl, source,
                    numberOfBedrooms, null);
            incomingPropertyEmitter.send(property);
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private String getLocation(String value) {
        var cleanedValue = value.toUpperCase().trim();
        if (cleanedValue.contains("IN")) {
            return cleanedValue.split("IN")[1].trim();
        }
        throw new RuntimeException("Location not found");
    }

    private Integer getNumberOfBedrooms(String value) {
        var cleanedValue = value.toUpperCase().trim();
        if (cleanedValue.contains(" - ")) {
            cleanedValue = cleanedValue.split(" - ")[0].trim();
        }
        cleanedValue = value.replaceAll("\\D", "");
        if (cleanedValue.isEmpty()) {
            return null;
        }
        return Integer.parseInt(cleanedValue);
    }

    private BigDecimal getPrice(String value) {
        String cleanedValue = value.toUpperCase().trim();
        if (cleanedValue.contains("FROM")) {
            cleanedValue = cleanedValue.split("TO")[0].trim();
        }
        if (cleanedValue.contains(".")) {
            cleanedValue = cleanedValue.split("\\.")[1];
        }
        return new BigDecimal(cleanedValue.replaceAll("\\D", ""));
    }

    private PropertyType getPropertyType(String value) {
        if (Optional.fromNullable(value).or("").isEmpty()) {
            return PropertyType.HOUSE;
        }
        return PropertyType.APPARTMENT;
    }
}
