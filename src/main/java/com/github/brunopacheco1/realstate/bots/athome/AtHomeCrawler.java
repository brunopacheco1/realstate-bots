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

import org.apache.hc.client5.http.fluent.Request;
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
        try {
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

                    var response = Request.get(url).execute();
                    var responseContent = response.returnContent().asString();
                    Document doc = Jsoup.parse(responseContent);
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
            String location = el.select("span.property-card-immotype-location-city").text().toUpperCase();
            BigDecimal value = getPrice(el.select("span.property-card-price").text());
            Source source = Source.ATHOME;
            Integer numberOfBedrooms = getNumberOfBedrooms(
                    el.select("ul.property-card-info-icons > li.item-rooms").text());
            Boolean hasGarage = !el.select("ul.property-card-info-icons > li.item-garages").isEmpty();
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
