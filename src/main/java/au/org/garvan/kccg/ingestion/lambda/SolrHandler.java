package au.org.garvan.kccg.ingestion.lambda;

import com.google.common.collect.Lists;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by ahmed on 18/7/17.
 */
public class SolrHandler {

    private static String documentPoolURL = getDocumentPoolURL();
    private static String commitQuery = "/update/json/docs?commit=true";
    private static String selectQuery = "/select";



    public static List<String> deduplicateArticles(List<String> articleIDs) throws IOException {
        List<String> cleanIDs = new ArrayList<>();
        OkHttpClient CLIENT = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(300L, TimeUnit.SECONDS)
                .writeTimeout(300L, TimeUnit.SECONDS)
                .readTimeout(300L, TimeUnit.SECONDS)
                .build();


        HttpUrl.Builder httpBuider = HttpUrl.parse(documentPoolURL + selectQuery).newBuilder();
        httpBuider.addQueryParameter("wt", "json");
        httpBuider.addQueryParameter("q", "PMID:*");
        for (String id : articleIDs) {
            httpBuider.setQueryParameter("q", "PMID:" + id);

            Request request = new Request.Builder()
                    .get()
                    .url(httpBuider.build().url())
                    .build();

            Response response = CLIENT.newCall(request).execute();


            if (response.code() == 200){
                JSONObject jsonObject = new JSONObject(response.body().string().trim());
                if ((int) jsonObject.getJSONObject("response").get("numFound") == 0) {
                    cleanIDs.add(id);
                }
            }
            else
            {
                System.out.println(String.format("Solr Call bad Response  %s.", response.toString()));

            }
        }

        return cleanIDs;

    }

    public static void postArticles(List<Article> finalArticles) throws IOException {
        List<List<Article>> batchSplits = Lists.partition(finalArticles, 100);
        int batchID = 1;
        for (List<Article> anArticleList : batchSplits) {
            System.out.println(String.format("Posting to SOLR. Solr-Batch Id:%d", batchID));
            // Create new batch client to avoid connection lost from Solr
            OkHttpClient batchClient = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .connectTimeout(300L, TimeUnit.SECONDS)
                    .writeTimeout(300L, TimeUnit.SECONDS)
                    .readTimeout(300L, TimeUnit.SECONDS)
                    .build();

            try {
                JSONArray jsonArrayArticle = new JSONArray();

                anArticleList.stream().forEach(x -> jsonArrayArticle.put(x.constructJsonObject()));
                String jsonArrayString = jsonArrayArticle.toString();

                HttpUrl.Builder httpBuilder = HttpUrl.parse(documentPoolURL + commitQuery).newBuilder();
                RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonArrayString);
                Request request = new Request.Builder()
                        .post(body)
                        .url(httpBuilder.build().url())
                        .build();

                Response response = batchClient.newCall(request).execute();
            } catch (SocketException e) {
                System.out.println(String.format("Socket exception in Article ID:%s\n Exception: %s", batchID, e.toString()));
            } catch (IOException ex) {
                System.out.println(String.format("IO exception in Article ID:%s\n Exception: %s", batchID, ex.toString()));
            }
            batchID++;


        }

    }

    private static String getDocumentPoolURL() {
        System.out.println("Getting SOLR URL");
        String URL = "http://localhost:8983/solr/Articles";
        String env = ConfigLoader.getENV();
        if (env !=null && env.equals("AWS"))
            URL = "http://solr-pubmed.phenomics.awsinternal:8983/solr/Articles";
        System.out.println(URL);
        return URL;

    }
    public static void main (String [] args) throws IOException {

    }



}
