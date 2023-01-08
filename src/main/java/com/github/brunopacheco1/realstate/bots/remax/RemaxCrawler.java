package com.github.brunopacheco1.realstate.bots.remax;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.logging.Level;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.brunopacheco1.realstate.PubSubConstants;
import com.github.brunopacheco1.realstate.api.PropertyDto;
import com.github.brunopacheco1.realstate.api.PropertyType;
import com.github.brunopacheco1.realstate.api.Source;
import com.github.brunopacheco1.realstate.api.TransactionType;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import io.quarkus.scheduler.Scheduled;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class RemaxCrawler {

    @Inject
    @Channel(PubSubConstants.INCOMING_PROPERTY)
    Emitter<PropertyDto> incomingPropertyEmitter;

    @Scheduled(cron = "{scheduler.remax}")
    public void produces() {
        log.info("Starting crawling.");

        try {
            WebDriver driver = initPhantomJS();
            for (TransactionType transactionType : TransactionType.values()) {
                int page = 1;
                Integer maxPages = 5;
                while (true) {
                    if (page > maxPages) {
                        break;
                    }
                    String entity = getWebsite(driver, transactionType, page);
                    if (entity != null) {
                        Document doc = Jsoup.parse(entity);

                        Elements elements = doc.select("div.gallery-item");
                        elements.forEach(el -> {
                            getProperty(el, transactionType);
                        });
                        page++;
                        Thread.sleep(1000);
                        continue;
                    }
                    break;
                }
            }
            driver.quit();
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
        log.info("Finished crawling.");
    }

    private String getWebsite(WebDriver driver, TransactionType transactionType, Integer page) throws Exception {
        String transaction = transactionType == TransactionType.BUY ? "261" : "260";
        String url = "https://www.remax.lu/PublicListingList.aspx?Lang=en-LU&PreviousLang=fr-LU#mode=gallery&tt="
                + transaction + "&cur=EUR&sb=MostRecent&page=" + page + "&sc=28&rl=174&lsgeo=174,0,0,0";

        driver.get(url);
        Thread.sleep(2000);
        return driver.getPageSource();
    }

    public WebDriver initPhantomJS() {
        String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.193 Safari/537.36";

        DesiredCapabilities desiredCaps = new DesiredCapabilities();
        desiredCaps.setCapability("takesScreenshot", false);
        desiredCaps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
                "/usr/local/bin/phantomjs");
        desiredCaps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_CUSTOMHEADERS_PREFIX + "User-Agent",
                USER_AGENT);

        ArrayList<String> cliArgsCap = new ArrayList<>();
        cliArgsCap.add("--web-security=false");
        cliArgsCap.add("--ssl-protocol=any");
        cliArgsCap.add("--ignore-ssl-errors=true");
        cliArgsCap.add("--webdriver-loglevel=ERROR");

        desiredCaps.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, cliArgsCap);
        WebDriver driver = new PhantomJSDriver(desiredCaps);
        driver.manage().window().setSize(new Dimension(1920, 1080));
        return driver;
    }

    private void getProperty(Element el, TransactionType transactionType) {
        try {
            String propertyUrlSuffix = el.select("a.proplist_price").attr("href");
            String propertyUrl = "https://www.remax.lu" + propertyUrlSuffix;
            PropertyType propertyType = getPropertyType(el.select("div.gallery-transtype").text());
            String location = getLocation(el.select("div.gallery-title > a").text());
            BigDecimal value = getPrice(el.select("a.proplist_price").text());
            Source source = Source.REMAX;
            Integer numberOfBedrooms = getNumberOfBedrooms(el.select("img[data-original-title^='Num. of Bedrooms']").attr("data-original-title"));
            Boolean hasGarage = null;
            PropertyDto property = new PropertyDto(location, value, propertyType, transactionType, propertyUrl, source,
                    numberOfBedrooms, hasGarage);
            incomingPropertyEmitter.send(property);
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private String getLocation(String value) {
        String cleanedValue = value.toUpperCase().trim();
        if (cleanedValue.contains(",")) {
            String[] parts = cleanedValue.split(",");
            cleanedValue = parts[1].trim() + "-" + parts[0].trim();
        }
        return cleanedValue;
    }

    private Integer getNumberOfBedrooms(String value) {
        String cleanedValue = value.replaceAll("\\D", "");
        if(cleanedValue.isEmpty()) {
            return null;
        }
        return Integer.parseInt(cleanedValue);
    }

    private BigDecimal getPrice(String value) {
        if (value.equalsIgnoreCase("Upon Request")) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.replaceAll("\\D", ""));
    }

    private PropertyType getPropertyType(String value) {
        String cleanedValue = value.toUpperCase().trim();
        if (cleanedValue.contains("HOUSE") || cleanedValue.contains("VILLA") || cleanedValue.contains("CHALET")) {
            return PropertyType.HOUSE;
        }
        if (cleanedValue.contains("GARAGE")) {
            return PropertyType.PARKING;
        }
        if (cleanedValue.contains("LAND")) {
            return PropertyType.LAND;
        }
        if (cleanedValue.contains("BUILDING")) {
            return PropertyType.PROPERTY;
        }
        if (cleanedValue.contains("ROOM")) {
            return PropertyType.ROOM;
        }
        if (cleanedValue.contains("APARTMENT") || cleanedValue.contains("STUDIO") || cleanedValue.contains("APPARTMENT")
                || cleanedValue.contains("DUPLEX") || cleanedValue.contains("PENTHOUSE")) {
            return PropertyType.APPARTMENT;
        }
        throw new RuntimeException("PropertyType not found - " + value);
    }
}
