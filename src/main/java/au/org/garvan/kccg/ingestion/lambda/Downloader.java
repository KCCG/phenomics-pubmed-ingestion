package au.org.garvan.kccg.ingestion.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class Downloader implements RequestHandler<Map<String, Object>, String> {
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(300L, TimeUnit.SECONDS)
            .writeTimeout(300L, TimeUnit.SECONDS)
            .readTimeout(300L, TimeUnit.SECONDS)
            .build();
    private static int FETCH_SIZE = 50000;
    private static String searchURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";
    private static String fetchURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";

    public static void main(String[] args) throws IOException {

    }

    private static boolean processArticles(List<String> articleIDs, String workerID) throws IOException {
        System.out.println(String.format("WorkerID:%s. Processing articles. Received total for today:%d", workerID, articleIDs.size()));

        // Split articles in batch to optimize processing
        List<List<String>> batchSplits = Lists.partition(articleIDs, ConfigLoader.getBATCHSIZE());
        System.out.println(String.format("WorkerID:%s. Processing articles in batches. Batch size: %d and Total Batches %d", workerID, ConfigLoader.getBATCHSIZE(), batchSplits.size()));

        int batchId = 1;
        for (List<String> batch : batchSplits) {

            System.out.println(String.format("WorkerID:%s. Processing started for batch: %d", workerID, batchId));

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
                System.out.println(String.format("WorkerID:%s. Successful response for batch: %d", workerID, batchId));
                JSONObject jsonObject = XML.toJSONObject(response.body().string().trim());


                JSONArray articles;
                if (Strings.isNullOrEmpty(jsonObject.get("PubmedArticleSet").toString())) {
                    System.out.println(String.format("WorkerID:%s. No Article found in response: %d", workerID, batchId));
                    articles = new JSONArray();
                } else if (batch.size() > 1)
                    articles = jsonObject.getJSONObject("PubmedArticleSet").getJSONArray("PubmedArticle");
                else {
                    articles = new JSONArray();
                    articles.put(jsonObject.getJSONObject("PubmedArticleSet").getJSONObject("PubmedArticle"));
                }
                System.out.println(String.format("WorkerID:%s. Total received articles: %d", workerID, articles.length()));
                collectedArticles.addAll(constructArticles(articles));
                //Find articles which are complete in nature and can ber persisted
                List<Article> cleanedArticles = collectedArticles.stream().filter(ar -> ar.getIsComplete()).collect(Collectors.toList());
                System.out.println(String.format("WorkerID:%s. Total complete items for batch:%d are: %d.", workerID, batchId, cleanedArticles.size()));

                if (ConfigLoader.shouldSendToPipeline()) {
                    System.out.println(String.format("WorkerID:%s. Calling Pipeline for batch:%d.", workerID, batchId));
                    if (!PipelineHandler.postArticles(cleanedArticles, workerID))
                        return false;


                }


            } else {
                System.out.println(String.format("Un-Successful response for batch: %d - Response code %d and message:%s", batchId, response.code(), response.message()));

            }
            batchId++;

        }// Batch loop

        return true;
    }

    private static Pair<Integer, List<String>> makeIDListForThisRun(Integer startingId, boolean isIgnitionCall) {

        //Skip IDs as per total number of workers
        Integer skipStep = ConfigLoader.getNumberOfWorkers();
        Integer batchSize = ConfigLoader.getBATCHSIZE();

        Integer lastID = startingId;
        Integer tempID = startingId;
        List<Integer> generatedIDs = new ArrayList<>();

        if (!isIgnitionCall) {
            for (int x = 0; x < batchSize; x++) {
                tempID = tempID - skipStep;
                if (tempID > 0)
                    generatedIDs.add(tempID);
                else
                    break;
            }
            lastID = generatedIDs.get(generatedIDs.size() - 1);
        } else {
            //If ignition call is then prepare the last run IDS to make sure nothing is missed
            for (int x = 0; x < batchSize; x++) {
                generatedIDs.add(tempID);
                tempID = tempID + skipStep;
            }
        }

        Pair<Integer, List<String>> returnObject = new Pair<>(lastID, generatedIDs.stream().map(x -> x.toString()).collect(Collectors.toList()));
        return returnObject;
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

    public static long getEpoch() {
        LocalDateTime ldt = LocalDateTime.now();
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("Australia/Sydney"));
        return zdt.toEpochSecond();

    }

    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        String workerID = input.get(Constants.WORKER_ID).toString();
        Boolean isIgnitionCall = false;
        if (input.containsKey(Constants.IGNITION_CALL))
            isIgnitionCall = (Boolean) input.get(Constants.IGNITION_CALL);


        System.out.println(String.format("WorkerID:%s. Lambda initialized. Ignition call is:%s ", workerID, isIgnitionCall.toString()));

        try {

            System.out.println(String.format("WorkerID:%s. Getting configs for calling worker.", workerID));
            Map<String, Object> workerConfig = DynamoDBHandler.getSubscription(workerID);

            if (workerConfig.size() > 0 && workerConfig.get(Constants.WORKER_ID).equals(workerID) && (Boolean) workerConfig.get(Constants.IS_ACTIVE)) {
                PipelineHandler.updatePort((String) workerConfig.get(Constants.PIPELINE_PORT));
                List<String> articleIDs;
                BigDecimal lastPMID = (BigDecimal) workerConfig.get(Constants.LAST_PMID_LABEL);
                Integer startingPMID = lastPMID.intValue();
                System.out.println(String.format("WorkerID:%s. Last Article ID: %d.", workerID, startingPMID));

                Pair<Integer, List<String>> currentRunValues = makeIDListForThisRun(startingPMID, isIgnitionCall);
                articleIDs = currentRunValues.getSecond();
                System.out.println(String.format("WorkerID:%s. Total Article IDs: %d.", workerID, articleIDs.size()));
                if (articleIDs.size() > 0) {
                    Boolean result = processArticles(articleIDs, workerID);
                    if (result && !isIgnitionCall)
                        DynamoDBHandler.updateLastID(workerID, currentRunValues.getFirst(), getEpoch());
                }

            } else {
                System.out.println(String.format("WorkerID:%s. Config missing or disabled.", workerID));

            }
        } catch (IOException e) {
            System.out.println(String.format("WorkerID:%s. Exception in lambda  %s.", workerID, e.toString()));
        }


        return "OK";
    }


}
