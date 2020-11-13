package com.github.brunopacheco1.realstate.bots.uelzecht;

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
import org.apache.http.client.methods.HttpPost;
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
public class UelzechtCrawler {

    @Inject
    @Channel(PubSubConstants.INCOMING_PROPERTY)
    Emitter<PropertyDto> incomingPropertyEmitter;

    @Scheduled(cron = "{scheduler.uelzecht}")
    public void produces() {
        log.info("Starting crawling.");
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            int page = 1;
            Integer pages = null;
            while (true) {
                if (pages != null && page > pages) {
                    break;
                }
                String url = "https://www.uelzecht.lu/index.php?page=recherche&recherche&paging=" + page;
                HttpPost post = new HttpPost(url);

                CloseableHttpResponse response = httpClient.execute(post);
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    Document doc = Jsoup.parse(EntityUtils.toString(entity));
                    if (pages == null) {
                        int total = Integer.parseInt(doc.select("div#nb_resultats_text > b.pink_title").text());
                        pages = total % 15 > 0 ? (total / 15) + 1 : (total / 15);
                    }

                    Elements properties = doc.select("div.content_annonce_bien");
                    properties.forEach(el -> {
                        PropertyDto property = getProperty(el);
                        incomingPropertyEmitter.send(property);
                    });
                    page++;
                    continue;
                }
                break;
            }
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
        log.info("Finished crawling.");
    }

    private PropertyDto getProperty(Element el) {
        String urlSuffix = el.select("div.image > a").attr("href").split("_")[0];
        String propertyUrl = "https://www.uelzecht.lu/" + urlSuffix;
        TransactionType transactionType = urlSuffix.endsWith("vente") ? TransactionType.BUY : TransactionType.RENT;
        PropertyType propertyType = getPropertyType(el.select("span.nature_listing").text());
        String location = el.select("span.ville_listing").text().toUpperCase();
        BigDecimal value = getPrice(el.select("span.prix_listing").text());
        Source source = Source.UELZECHT;
        return new PropertyDto(location, value, propertyType, transactionType, propertyUrl, source);
    }

    private BigDecimal getPrice(String value) {
        return new BigDecimal(value.replaceAll("\\D", ""));
    }

    private PropertyType getPropertyType(String value) {
        String cleanedValue = value.toLowerCase().trim();
        if (cleanedValue.contains("appartement") || cleanedValue.contains("studio")) {
            return PropertyType.APPARTMENT;
        }
        if (cleanedValue.contains("maison")) {
            return PropertyType.HOUSE;
        }
        if (cleanedValue.contains("terrain")) {
            return PropertyType.LAND;
        }
        if (cleanedValue.contains("immeuble")) {
            return PropertyType.PROPERTY;
        }
        if (cleanedValue.contains("parking") || cleanedValue.contains("emplacement")) {
            return PropertyType.PARKING;
        }
        if (cleanedValue.contains("bureau")) {
            return PropertyType.OFFICE;
        }
        if (cleanedValue.contains("commercial") || cleanedValue.contains("restaurant") || cleanedValue.contains("commerce")) {
            return PropertyType.COMMERCIAL_PREMISES;
        }
        log.info(cleanedValue);
        throw new RuntimeException("PropertyType not found");
    }
}
