package com.github.brunopacheco1.realstate.bots.wortimmo;

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
public class WortimmoCrawler {
    
    @Inject
    @Channel(PubSubConstants.INCOMING_PROPERTY)
    Emitter<PropertyDto> incomingPropertyEmitter;

    @Scheduled(cron = "{scheduler.wortimmo}")
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
                    String transaction = transactionType == TransactionType.BUY ? "vente" : "location";
                    String url = "https://www.wortimmo.lu/en/search?property_search_engine%5BtransactionType%5D=" + 
                        transaction + "&property_search_engine%5Blocation%5D=country-1&property_search_engine%5BpurchasePriceMax%5D=&property_search_engine%5BbedroomMin%5D=&property_search_engine%5Bsubmit%5D=&page=" +
                        page;
                    HttpGet get = new HttpGet(url);

                    CloseableHttpResponse response = httpClient.execute(get);
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        Document doc = Jsoup.parse(EntityUtils.toString(entity));
                        if (existingPages == null) {
                            int total = getPages(doc.select("div.c-nb-results").text());
                            existingPages = total % 15 > 0 ? (total / 15) + 1 : (total / 15);
                        }

                        Elements elements = doc.select("div.property-informations");
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
            String urlSuffix = el.select("a").attr("href");
            String url = "https://www.wortimmo.lu/en" + urlSuffix;
            PropertyType propertyType = getPropertyType(urlSuffix);
            String location = el.select("h2.c-title").text().split("(to sell in)|(to rent in)")[1].toUpperCase().trim();
            BigDecimal value = getPrice(el.select("div.c-price").text());
            Source source = Source.WORTIMMO;
            PropertyDto property = new PropertyDto(location, value, propertyType, transactionType, url, source);
            incomingPropertyEmitter.send(property);
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private Integer getPages(String text) {
        return Integer.parseInt(text.replaceAll("\\D", ""));
    }

    private BigDecimal getPrice(String value) {
        if(value.equalsIgnoreCase("price on request")) {
            return BigDecimal.ZERO;
        }
        String cleanedValue = value.toUpperCase().trim();
        if(cleanedValue.contains("FROM")) {
            cleanedValue = cleanedValue.split("TO")[0].trim();
        }
        return new BigDecimal(cleanedValue.replaceAll("\\D", ""));
    }

    private PropertyType getPropertyType(String value) {
        String cleanedValue = value.split("\\/")[3];
        if (cleanedValue.contains("apartment") || cleanedValue.contains("loft") || cleanedValue.contains("penthouse") || cleanedValue.contains("studio") || cleanedValue.contains("duplex") || cleanedValue.contains("triplex")) {
            return PropertyType.APPARTMENT;
        }
        if (cleanedValue.contains("office")) {
            return PropertyType.OFFICE;
        }
        if (cleanedValue.contains("house-semi-detached") || cleanedValue.contains("joint-house") || cleanedValue.contains("investment-house") || cleanedValue.contains("house") || cleanedValue.contains("detached-house") || cleanedValue.contains("villa")) {
            return PropertyType.HOUSE;
        }
        if (cleanedValue.contains("parking") || cleanedValue.contains("garage")) {
            return PropertyType.PARKING;
        }
        if (cleanedValue.contains("ground")) {
            return PropertyType.LAND;
        }
        if (cleanedValue.contains("investment-property")) {
            return PropertyType.PROPERTY;
        }
        if (cleanedValue.contains("allotment") || cleanedValue.contains("building-residence") || cleanedValue.contains("premises-for-liberal-occupations")) {
            return PropertyType.NEW_PROPERTY;
        }
        if(cleanedValue.contains("shop") || cleanedValue.contains("business-takeover") || cleanedValue.contains("commercial-premises") || cleanedValue.contains("catering-hospitality")) {
            return PropertyType.COMMERCIAL_PREMISES;
        }
        log.info(cleanedValue);
        throw new RuntimeException("PropertyType not found");
    }
}
