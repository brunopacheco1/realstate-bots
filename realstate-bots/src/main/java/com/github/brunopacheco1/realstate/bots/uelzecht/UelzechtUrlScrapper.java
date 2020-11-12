package com.github.brunopacheco1.realstate.bots.uelzecht;

import java.util.logging.Level;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.brunopacheco1.realstate.bots.PubSubConstants;

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
import org.jsoup.select.Elements;
import io.quarkus.scheduler.Scheduled;
import lombok.extern.java.Log;

@ApplicationScoped
@Log
public class UelzechtUrlScrapper {

    @Inject
    @Channel(PubSubConstants.UELZECHT_CRAWLER)
    Emitter<String> urlEmitter;

    @Scheduled(cron = "{scheduler.uelzecht}")
    public void produces() {
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

                    Elements properties = doc.select("div.content_annonce_bien > div.image > a");
                    properties.forEach(property -> {
                        String propertyUrl = "https://www.uelzecht.lu/" + property.attr("href").split("_")[0];
                        urlEmitter.send(propertyUrl);
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
}
