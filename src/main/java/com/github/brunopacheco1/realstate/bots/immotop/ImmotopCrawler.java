package com.github.brunopacheco1.realstate.bots.immotop;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.brunopacheco1.realstate.PubSubConstants;
import com.github.brunopacheco1.realstate.api.PropertyDto;
import com.github.brunopacheco1.realstate.api.PropertyType;
import com.github.brunopacheco1.realstate.api.Source;
import com.github.brunopacheco1.realstate.api.TransactionType;

import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
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
public class ImmotopCrawler {

    @Inject
    @Channel(PubSubConstants.INCOMING_PROPERTY)
    Emitter<PropertyDto> incomingPropertyEmitter;

    @Scheduled(cron = "{scheduler.immotop}")
    public void produces() {
        log.info("Starting crawling.");
        for (TransactionType transactionType : TransactionType.values()) {
            try {
                int page = 1;
                Integer maxPages = 20;
                initialSearchPost(transactionType);
                while (true) {
                    if (page > maxPages) {
                        break;
                    }
                    String url = "https://www.immotop.lu/en/search/index" + page + ".html";

                    var response = Request.get(url)
                            .addHeader(":authority", "www.immotop.lu")
                            .addHeader(":method", "GET")
                            .addHeader(":path", "/en/search/index" + page + ".html")
                            .addHeader(":scheme", "https")
                            .addHeader("accept",
                                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                            .addHeader("accept-encoding", "gzip, deflate, br")
                            .addHeader("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,pl-PL;q=0.6,pl;q=0.5")
                            .addHeader("cache-control", "max-age=0")
                            .addHeader("referer",
                                    page == 1 ? "https://www.immotop.lu/en/"
                                            : "https://www.immotop.lu/en/search/index" + (page - 1) + ".html")
                            .addHeader("sec-fetch-dest", "document")
                            .addHeader("sec-fetch-mode", "navigate")
                            .addHeader("sec-fetch-site", "same-origin")
                            .addHeader("sec-fetch-user", "?1")
                            .addHeader("upgrade-insecure-requests", "1")
                            .addHeader("user-agent",
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.193 Safari/537.36")
                            .execute();

                    Document doc = Jsoup.parse(response.returnContent().asString());

                    Elements elements = doc.select("div.search-agency-item");
                    elements.forEach(el -> {
                        getProperty(el, transactionType);
                    });
                    page++;
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                log.log(Level.WARNING, e.getMessage(), e);
            }
        }
    }

    private void initialSearchPost(TransactionType transactionType) throws Exception {
        String url = "https://www.immotop.lu/en/search/";
        var post = Request.post(url)
        .addHeader("authority", "www.immotop.lu")
        .addHeader("cache-control", "max-age=0")
        .addHeader("upgrade-insecure-requests", "1")
        .addHeader("origin", "https://www.immotop.lu")
        .addHeader("content-type", "application/x-www-form-urlencoded")
        .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.193 Safari/537.36")
        .addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
        .addHeader("sec-fetch-site", "same-origin")
        .addHeader("sec-fetch-mode", "navigate")
        .addHeader("sec-fetch-user", "?1")
        .addHeader("sec-fetch-dest", "document")
        .addHeader("referer", "https://www.immotop.lu/en/search/index1.html")
        .addHeader("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,pl-PL;q=0.6,pl;q=0.5");

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("f[sort_field]", "date_modifyed"));
        params.add(new BasicNameValuePair("f[sort_type]", "desc"));
        String transaction = TransactionType.BUY == transactionType ? "sale" : "rent";
        params.add(new BasicNameValuePair("f[Type]", transaction));
        params.add(new BasicNameValuePair("form", "simple_search"));
        params.add(new BasicNameValuePair("sort_field", "ts"));
        params.add(new BasicNameValuePair("sort_type", "desc"));
        params.add(new BasicNameValuePair("f[text_search]", ""));
        params.add(new BasicNameValuePair("f[city]", "Luxembourg"));
        params.add(new BasicNameValuePair("f[rooms][from]", ""));
        params.add(new BasicNameValuePair("f[surface][from]", ""));
        params.add(new BasicNameValuePair("f[price][from]", ""));
        params.add(new BasicNameValuePair("f[price][to]", ""));
        params.add(new BasicNameValuePair("f[by_transport]", ""));
        params.add(new BasicNameValuePair("form", "main_search_form"));
        params.add(new BasicNameValuePair("f[geodata][0][country]", "luxembourg"));
        params.add(new BasicNameValuePair("f[geodata][0][name]", "Luxembourg"));
        params.add(new BasicNameValuePair("f[geodata][0][label]", "Country"));
        params.add(new BasicNameValuePair("f[geodata][0][c_label]", "Luxembourg"));
        params.add(new BasicNameValuePair("f[geodata][0][level]", "country"));

        post.body(new UrlEncodedFormEntity(params)).execute();
    }

    private void getProperty(Element el, TransactionType transactionType) {
        try {
            String propertyUrl = el.select("a").attr("href");
            PropertyType propertyType = getPropertyType(propertyUrl);
            String location = getLocation(el.select("a").text());
            BigDecimal value = getPrice(el.select("div.price").text());
            Source source = Source.IMMOTOP;
            Integer numberOfBedrooms = getNumberOfBedrooms(el.select("div[title='Rooms']:has(i.fa-bed)").text());
            Boolean hasGarage = !el.select("div[title='Garages']:has(i.fa-car)").isEmpty();
            PropertyDto property = new PropertyDto(location, value, propertyType, transactionType, propertyUrl, source,
                    numberOfBedrooms, hasGarage);
            incomingPropertyEmitter.send(property);
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private String getLocation(String value) {
        String cleanedValue = value.toUpperCase().trim();
        if (cleanedValue.contains("FOR SALE IN")) {
            return cleanedValue.split("FOR SALE IN")[1].trim();
        }
        if (cleanedValue.contains("FOR RENT IN")) {
            return cleanedValue.split("FOR RENT IN")[1].trim();
        }
        throw new RuntimeException("Location not found");
    }

    private Integer getNumberOfBedrooms(String value) {
        String cleanedValue = value.replaceAll("\\D", "");
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
        if (value.contains("/new/")) {
            return PropertyType.NEW_PROPERTY;
        }
        String cleanedValue = value.split("\\/")[6];
        if (cleanedValue.equals("house")) {
            return PropertyType.HOUSE;
        }
        if (cleanedValue.equals("apartment")) {
            return PropertyType.APPARTMENT;
        }
        if (cleanedValue.equals("building-of-flats") || cleanedValue.equals("residence")) {
            return PropertyType.PROPERTY;
        }
        if (cleanedValue.equals("garage")) {
            return PropertyType.PARKING;
        }
        if (cleanedValue.equals("industrial-property") || cleanedValue.equals("commercial-property")
                || cleanedValue.equals("business")) {
            return PropertyType.COMMERCIAL_PREMISES;
        }
        if (cleanedValue.equals("ground")) {
            return PropertyType.LAND;
        }
        throw new RuntimeException("PropertyType not found - " + value);
    }
}
