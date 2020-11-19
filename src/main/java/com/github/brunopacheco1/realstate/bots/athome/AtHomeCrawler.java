package com.github.brunopacheco1.realstate.bots.athome;

import java.math.BigDecimal;
import java.util.logging.Level;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.brunopacheco1.realstate.PubSubConstants;
import com.github.brunopacheco1.realstate.api.PropertyDto;
import com.github.brunopacheco1.realstate.api.PropertyType;
import com.github.brunopacheco1.realstate.api.Source;
import com.github.brunopacheco1.realstate.api.TransactionType;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import io.quarkus.scheduler.Scheduled;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class AtHomeCrawler {

    @Inject
    @Channel(PubSubConstants.INCOMING_PROPERTY)
    Emitter<PropertyDto> incomingPropertyEmitter;

    @Scheduled(cron = "{scheduler.athome}")
    public void produces() {
        log.info("Starting crawling.");
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            int page = 1;
            Integer existingPages = null;
            Integer maxPages = 20;
            for (TransactionType transactionType : TransactionType.values()) {
                while (true) {
                    if (existingPages != null && page > Math.min(maxPages, existingPages)) {
                        break;
                    }
                    String url = "https://www.athome.lu/en/srp/?tr=" + transactionType.name().toLowerCase()
                            + "&sort=date_desc&q=faee1a4a&loc=L2-luxembourg&page=" + page;
                    HttpGet get = new HttpGet(url);

                    CloseableHttpResponse response = httpClient.execute(get);
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        Document doc = Jsoup.parse(EntityUtils.toString(entity));
                        if (existingPages == null) {
                            int total = getPages(doc.select("div.paginator > p").text());
                            existingPages = total % 20 > 0 ? (total / 20) + 1 : (total / 20);
                        }

                        Elements elements = doc.select("article");
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
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
        log.info("Finished crawling.");
    }

    private void getProperty(Element el, TransactionType transactionType) {
        try {
            String urlSuffix = el.select("link").attr("href");
            String propertyUrl = "https://www.athome.lu" + urlSuffix;
            PropertyType propertyType = getPropertyType(urlSuffix);
            String location = el.select("span[itemprop=addressLocality]").text().toUpperCase();
            BigDecimal value = getPrice(el.select("ul.mainInfos > li.propertyPrice").text());
            Source source = Source.ATHOME;
            Integer numberOfBedrooms = getNumberOfBedrooms(el.select("ul.characterstics > li:has(i.icon-bed)").text());
            Boolean hasGarage = !el.select("ul.characterstics > li:has(i.icon-car)").isEmpty();
            PropertyDto property = new PropertyDto(location, value, propertyType, transactionType, propertyUrl, source,
                    numberOfBedrooms, hasGarage);
            incomingPropertyEmitter.send(property);
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private Integer getPages(String text) {
        return Integer.parseInt(text.split("of")[1].replaceAll("\\D", ""));
    }

    private Integer getNumberOfBedrooms(String value) {
        String cleanedValue = value.replaceAll("\\D", "");
        if (cleanedValue.isEmpty()) {
            return null;
        }
        return Integer.parseInt(cleanedValue);
    }

    private BigDecimal getPrice(String value) {
        return new BigDecimal(value.replaceAll("\\D", ""));
    }

    private PropertyType getPropertyType(String value) {
        String cleanedValue = value.toLowerCase().trim();
        if (cleanedValue.contains("apartment") || cleanedValue.contains("flat")) {
            return PropertyType.APPARTMENT;
        }
        if (cleanedValue.contains("bureau") || cleanedValue.contains("office")) {
            return PropertyType.OFFICE;
        }
        if (cleanedValue.contains("house") || cleanedValue.contains("townhouse")) {
            return PropertyType.HOUSE;
        }
        if (cleanedValue.contains("ground")) {
            return PropertyType.LAND;
        }
        if (cleanedValue.contains("garage-parking")) {
            return PropertyType.PARKING;
        }
        if (cleanedValue.contains("build")) {
            return PropertyType.PROPERTY;
        }
        if (cleanedValue.contains("commercial-property")) {
            return PropertyType.COMMERCIAL_PREMISES;
        }
        if (cleanedValue.contains("housing-project")) {
            return PropertyType.NEW_PROPERTY;
        }
        throw new RuntimeException("PropertyType not found - " + value);
    }
}
