package main.java;

import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j
public class downloader {

    private static int BATCH_SIZE = 500;
    private static int FETCH_SIZE = 50000;

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(30L,TimeUnit.SECONDS)
            .writeTimeout(30L,TimeUnit.SECONDS)
            .readTimeout(30L, TimeUnit.SECONDS)
            .build();

    private static String searchURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";
    private static String fetchURL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";
    private static String documentPoolURL = "http://localhost:8983/solr/Articles";
    private static String updateQuery = "/update/json/docs";
    private static String commitQuery = "/update/json/docs?commit=true";
    private static String selectQuery = "/select";


    public static void main (String [] args) throws IOException {
        Map<String, Object> id = new HashMap();
        ingestArticles(id);
    }

    public static String ingestArticles(Map<String,Object> event) throws IOException {

        HttpUrl.Builder httpBuider = HttpUrl.parse(searchURL).newBuilder();
        HashMap<String,String> params = getSearchParams();
        if (params != null) {
            for(Map.Entry<String, String> param : params.entrySet()) {
                httpBuider.addQueryParameter(param.getKey(),param.getValue());
            }
        }
        Request request = new Request.Builder()
                .get()
                .url(httpBuider.build().url())
                .build();

        Response response = CLIENT.newCall(request).execute();
        JSONObject jsonObject = new JSONObject(response.body().string().trim());

        String totalCount = jsonObject.getJSONObject("esearchresult").get("count").toString();
        //TODO: Check count and limit and make a call again
        List<String> articleIDs= new ArrayList<String>();
        jsonObject.getJSONObject("esearchresult").getJSONArray("idlist").forEach(x-> articleIDs.add(x.toString()));


        List<String> dedupedArticleIDs = deduplicateArticles(articleIDs);
        processArticles(dedupedArticleIDs);

        return "OK";
    }

    private static List<String> deduplicateArticles(List<String> articleIDs) throws IOException {
        List<String> cleanIDs = new ArrayList<>();

        HttpUrl.Builder httpBuider = HttpUrl.parse(documentPoolURL + selectQuery).newBuilder();
        httpBuider.addQueryParameter("wt","json");
        httpBuider.addQueryParameter("q","PMID:*");
        for (String id : articleIDs)
        {
            httpBuider.setQueryParameter("q","PMID:" + id);
            Request request = new Request.Builder()
                    .get()
                    .url(httpBuider.build().url())
                    .build();

            Response response = CLIENT.newCall(request).execute();
            JSONObject jsonObject = new JSONObject(response.body().string().trim());
            if ((int)jsonObject.getJSONObject("response").get("numFound") ==0)
            {
                cleanIDs.add(id);
            }

        }

        return cleanIDs;

    }

    private static void processArticles(List<String> articleIDs) throws IOException {

        List<Article> collectedArticles = new ArrayList<>();

        List<List<String>> batchSplits = Lists.partition(articleIDs, BATCH_SIZE);

        for (List<String> batch : batchSplits)
        {
            HttpUrl.Builder httpBuider = HttpUrl.parse(fetchURL).newBuilder();
            HashMap<String,String> params = getFetchParams(batch);
            if (params != null) {
                for(Map.Entry<String, String> param : params.entrySet()) {
                    httpBuider.addQueryParameter(param.getKey(),param.getValue());
                }
            }
            //Post call is required for more than 200 IDs
            Request request = new Request.Builder()
                    .get()
                    .method("POST", RequestBody.create(null, new byte[0]))
                    .url(httpBuider.build().url())
                    .build();

            Response response = CLIENT.newCall(request).execute();

            if (response.code()==200) {
                JSONObject jsonObject = XML.toJSONObject(response.body().string().trim());
                JSONArray articles = jsonObject.getJSONObject("PubmedArticleSet").getJSONArray("PubmedArticle");


                collectedArticles.addAll(constructArticles(articles));
            }
        }


        List<Article> cleanedArticles = collectedArticles.stream().filter(Article::getIsComplete).collect(Collectors.toList());
        postArticles(cleanedArticles);


    }

    private static void postArticles(List<Article> finalArticles) throws IOException {


        List<List<Article>> batchSplits = Lists.partition(finalArticles, 100);
        int batchID = 0;
        for (List<Article> anArticleList : batchSplits) {
            // Create new batch client to avoid connection lost from Solr
            OkHttpClient batchClient = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .connectTimeout(30L,TimeUnit.SECONDS)
                    .writeTimeout(30L,TimeUnit.SECONDS)
                    .readTimeout(30L, TimeUnit.SECONDS)
                    .build();

            try {
                JSONArray jsonArrayArticle = new JSONArray();

                anArticleList.stream().forEach(x-> jsonArrayArticle.put(x.constructJsonObject()));
                String jsonArrayString = jsonArrayArticle.toString();

                HttpUrl.Builder httpBuider = HttpUrl.parse(documentPoolURL + commitQuery).newBuilder();
                RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonArrayString );
                Request request = new Request.Builder()
                        .post(body)
                        .url(httpBuider.build().url())
                        .build();

                Response response = batchClient.newCall(request).execute();
            }
            catch (SocketException e)
            {
                System.out.print(String.format("Socket exception in Article ID:%s\n Exception: %s", batchID ,e.toString()));
            }
            catch (IOException ex) {
                System.out.print(String.format("IO exception in Article ID:%s\n Exception: %s", batchID, ex.toString()));
            }
            batchID ++;


        }

    }


    private static List<Article> constructArticles(JSONArray jsonArticles){
        List<Article> constructedArticles = new ArrayList<>();
        jsonArticles.forEach(x-> constructedArticles.add(new Article((JSONObject)x)));
        return constructedArticles;

    }


    private static void jsonDump(JSONObject jObj, String fileName) throws IOException {
        FileWriter fileWriter = new FileWriter(fileName);
        fileWriter.write(jObj.toString());
        fileWriter.close();
    }

    private static HashMap getSearchParams(){
        HashMap<String,String> params = new HashMap();
        params.put("db","pubmed");
        params.put("reldate","10");
        params.put("datetype","pdat");
        params.put("usehistory","y");
        params.put("retmax", String.valueOf(FETCH_SIZE));
        params.put("retmode","json");
        return params;
    }
    private static HashMap getFetchParams(List<String> lstIds){
        HashMap<String,String> params = new HashMap();
        params.put("db","pubmed");
        params.put("usehistory","y");
        params.put("retmode","xml");
        params.put("rettype","abstract");
        params.put("ID", org.apache.commons.lang3.StringUtils.join(lstIds,','));

        return params;
    }

}
