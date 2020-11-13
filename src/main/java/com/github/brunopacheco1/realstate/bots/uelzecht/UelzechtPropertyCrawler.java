package com.github.brunopacheco1.realstate.bots.uelzecht;

import java.math.BigDecimal;
import java.util.logging.Level;
import javax.enterprise.context.ApplicationScoped;

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
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class UelzechtPropertyCrawler {

    @Incoming(PubSubConstants.UELZECHT_CRAWLER)
    @Outgoing(PubSubConstants.INCOMING_PROPERTY)
    public PropertyDto crawl(Message<String> message) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = message.getPayload();

            HttpGet get = new HttpGet(url);

            CloseableHttpResponse response = httpClient.execute(get);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                Document doc = Jsoup.parse(EntityUtils.toString(entity));
                String location = doc.select("span#ville_details").text().toUpperCase();
                BigDecimal value = getPrice(doc.select("span#prix_details").text());
                PropertyType propertyType = getPropertyType(doc.select("span#type_details").text());
                TransactionType transactionType = url.endsWith("vente") ? TransactionType.BUY : TransactionType.RENT;
                Source source = Source.UELZECHT;
                return new PropertyDto(location, value, propertyType, transactionType, url, source);
            }
            throw new Exception("Empty body");
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e);
            throw new RuntimeException(e);
        }
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
        throw new RuntimeException("PropertyType not found");
    }
}
