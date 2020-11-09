package com.github.brunopacheco1.realstatebots.bots.athome;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.logging.Level;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.brunopacheco1.realstatebots.consumers.PubSubConstants;
import com.github.brunopacheco1.realstatebots.domain.Property;
import com.github.brunopacheco1.realstatebots.domain.PropertyType;
import com.github.brunopacheco1.realstatebots.domain.Source;
import com.github.brunopacheco1.realstatebots.domain.TransactionType;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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
public class AtHomeCrawler {

    @Inject
    @Channel(PubSubConstants.INCOMING_PROPERTY)
    Emitter<Property> incomingPropertyEmitter;

    @Scheduled(cron = "{scheduler.athome}")
    public void produces() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            int page = 1;
            Integer pages = null;
            for (TransactionType transactionType : TransactionType.values()) {
                while (true) {
                    if (pages != null && page > pages) {
                        break;
                    }
                    String url = "https://www.athome.lu/en/srp/?tr=" + transactionType.name().toLowerCase()
                            + "&sort=date_desc&q=faee1a4a&loc=L2-luxembourg&page=" + page;
                    HttpGet get = new HttpGet(url);

                    CloseableHttpResponse response = httpClient.execute(get);
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        Document doc = Jsoup.parse(EntityUtils.toString(entity));
                        if (pages == null) {
                            int total = getPages(doc.select("div.paginator > p").text());
                            pages = total % 20 > 0 ? (total / 20) + 1 : (total / 20);
                        }

                        Elements elements = doc.select("article");
                        elements.forEach(el -> {
                            Property property = getProperty(el, transactionType);
                            incomingPropertyEmitter.send(property);
                        });
                        page++;
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

    private Property getProperty(Element el, TransactionType transactionType) {
        String urlSuffix = el.select("link").attr("href");
        String url = "https://www.athome.lu" + urlSuffix;
        PropertyType propertyType = getPropertyType(urlSuffix);
        String location = el.select("span[itemprop=addressLocality]").text().toUpperCase();
        BigDecimal value = getPrice(el.select("ul.mainInfos > li.propertyPrice").text());
        Source source = Source.ATHOME;
        String id = DigestUtils.sha3_256Hex(url + source);
        return new Property(id, location, value, propertyType, transactionType, url, source, LocalDateTime.now());
    }

    private Integer getPages(String text) {
        return Integer.parseInt(text.split("of")[1].replaceAll("\\D", ""));
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
        if(cleanedValue.contains("commercial-property")) {
            return PropertyType.COMMERCIAL_PREMISES;
        }
        if(cleanedValue.contains("housing-project")) {
            return PropertyType.NEW_PROPERTY;
        }
        throw new RuntimeException("PropertyType not found");
    }
}
