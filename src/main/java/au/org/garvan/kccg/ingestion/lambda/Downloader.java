package au.org.garvan.kccg.ingestion.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.common.collect.Lists;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class Downloader implements RequestHandler<Map<String,Object>, String> {
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(300L, TimeUnit.SECONDS)
            .writeTimeout(300L, TimeUnit.SECONDS)
            .readTimeout(300L, TimeUnit.SECONDS)
            .build();
//    private static int BATCH_SIZE = 500;
    private static int FETCH_SIZE = 50000;
    private static String searchURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";
    private static String fetchURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";

    public static void main (String [] args) throws IOException {

    }


    @Override
    public String handleRequest(Map<String,Object> input, Context context) {
        System.out.println("Lambda Initialized. ");

        HttpUrl.Builder httpBuider = HttpUrl.parse(searchURL).newBuilder();
        HashMap<String, String> params = getSearchParams();
        if (params != null) {
            for (Map.Entry<String, String> param : params.entrySet()) {
                httpBuider.addQueryParameter(param.getKey(), param.getValue());
            }
        }
        Request request = new Request.Builder()
                .get()
                .url(httpBuider.build().url())
                .build();

        Response response;
        try {
            System.out.println( String.format("Calling pubmed for ID List. Request config is:%s", request.url().toString()));
            response = CLIENT.newCall(request).execute();
            System.out.println(String.format("Got response from Pubmed, response code: %d.", response.code()));

            if(response.code()==200) {

                JSONObject jsonObject = new JSONObject(response.body().string().trim());

                String totalCount = jsonObject.getJSONObject("esearchresult").get("count").toString();
                System.out.println(String.format("Total items count from Pubmed: %s.", totalCount.toString()));

                //TODO: Check count and limit and make a call again
                List<String> articleIDs = new ArrayList<>();
                jsonObject.getJSONObject("esearchresult").getJSONArray("idlist").forEach(x -> articleIDs.add(x.toString()));
                System.out.println(String.format("Total fetched IDs: %d.", articleIDs.size()));

                if (articleIDs.size()>0)
//                    processArticles(Arrays.asList("29906225"));
                    processArticles(articleIDs.subList(0,10));
            }


        } catch (IOException e) {
            System.out.println(String.format("Exception in lambda  %s.", e.toString()));
        }


        return "OK";
    }



    private static void processArticles(List<String> articleIDs) throws IOException {
        System.out.println(String.format("Processing articles. Received total for today:%d", articleIDs.size()));

        // Split articles in batch to optimize processing
        List<List<String>> batchSplits = Lists.partition(articleIDs, ConfigLoader.getBATCHSIZE());
        System.out.println(String.format("Processing articles in batches. Batch size: %d and Total Batches %d", ConfigLoader.getBATCHSIZE(), batchSplits.size()));

        int batchId = 1;
        for (List<String> batch : batchSplits) {

            System.out.println(String.format("Processing started for batch: %d", batchId));

            List<Article> collectedArticles = new ArrayList<>();
            HttpUrl.Builder httpBuider = HttpUrl.parse(fetchURL).newBuilder();
            HashMap<String, String> params = getFetchParams(batch);
            if (params != null) {
                for (Map.Entry<String, String> param : params.entrySet()) {
                    httpBuider.addQueryParameter(param.getKey(), param.getValue());
                }
            }
            //Post call is required for more than 200 IDs
            Request request = new Request.Builder()
                    .method("POST", RequestBody.create(null, new byte[0]))
                    .url(httpBuider.build().url())
                    .build();

            Response response = CLIENT.newCall(request).execute();

            if (response.code() == 200) {
                System.out.println(String.format("Successful response for batch: %d", batchId));
                JSONObject jsonObject = XML.toJSONObject(response.body().string().trim());
                JSONArray articles;

                if (batch.size()>1)
                    articles = jsonObject.getJSONObject("PubmedArticleSet").getJSONArray("PubmedArticle");
                else {
                    articles = new JSONArray();
                    articles.put(jsonObject.getJSONObject("PubmedArticleSet").getJSONObject("PubmedArticle"));
                }

                collectedArticles.addAll(constructArticles(articles));
                //Find articles which are complete in nature and can ber persisted
                List<Article> cleanedArticles = collectedArticles.stream().filter(ar-> ar.getIsComplete()).collect(Collectors.toList());
                System.out.println(String.format("Total complete items for batch:%d are: %d.",batchId, cleanedArticles.size()));

                if(ConfigLoader.shouldSendToPipeline()) {
                    System.out.println(String.format("Calling Pipeline for batch:%d.",batchId));
                    PipelineHandler.postArticles(cleanedArticles);

                }


            }
            else
            {
                System.out.println(String.format("Un-Successful response for batch: %d - Response code %d and message:%s", batchId, response.code(), response.message()));

            }
            batchId++;

        }// Batch loop


    }


    public static void callWithID(List<String> articleIDs){
        try {
            processArticles(articleIDs);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static List<Article> constructArticles(JSONArray jsonArticles) {
        List<Article> constructedArticles = new ArrayList<>();
        jsonArticles.forEach(x -> constructedArticles.add(new Article((JSONObject) x)));
        return constructedArticles;

    }

    private static HashMap getSearchParams() {
        HashMap<String, String> params = new HashMap();
        params.put("db", "pubmed");
        params.put("reldate", ConfigLoader.getDAYS());
        params.put("datetype", "pdat");
        params.put("usehistory", "y");
        params.put("retmax", String.valueOf(FETCH_SIZE));
        params.put("retmode", "json");
        return params;
    }

    private static HashMap getFetchParams(List<String> lstIds) {
        HashMap<String, String> params = new HashMap();
        params.put("db", "pubmed");
        params.put("usehistory", "y");
        params.put("retmode", "xml");
        params.put("rettype", "abstract");
        params.put("ID", org.apache.commons.lang3.StringUtils.join(lstIds, ','));

        return params;
    }


}
