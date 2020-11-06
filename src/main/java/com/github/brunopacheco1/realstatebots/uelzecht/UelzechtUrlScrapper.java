package com.github.brunopacheco1.realstatebots.uelzecht;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.enterprise.context.ApplicationScoped;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import io.quarkus.scheduler.Scheduled;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class UelzechtUrlScrapper {

    @Scheduled(cron = "{scheduler.uelzecht}")
    public void cronJobWithExpressionInConfig() {
        List<String> urls = new ArrayList<>();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            int page = 1;
            Integer pages = null;
            while (true) {
                log.info(String.valueOf(page));
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

                    Elements properties = doc.select("div.content_annonce_bien > div.image > a");
                    properties.forEach(property -> urls.add("https://www.uelzecht.lu/" + property.attr("href")));
                    page++;
                    continue;
                }
                break;
            }
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
        log.info(String.valueOf(urls.size()));
    }
}
