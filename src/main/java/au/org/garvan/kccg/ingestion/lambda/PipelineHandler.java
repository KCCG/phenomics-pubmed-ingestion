package au.org.garvan.kccg.ingestion.lambda;

import com.google.common.base.Strings;
import okhttp3.*;
import org.json.JSONArray;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by ahmed on 08/12/17.
 */
public class PipelineHandler {

    private static String pipelineEndpoint = getPipelineURL();
    private static String submitQuery = "/articles";

    public static void updatePort(String workerPort){
        port = ":"+ workerPort;
    }
    public static boolean postArticles(List<Article> finalArticles, String workerID) throws IOException {

            OkHttpClient batchClient = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .connectTimeout(300L, TimeUnit.SECONDS)
                    .writeTimeout(300L, TimeUnit.SECONDS)
                    .readTimeout(300L, TimeUnit.SECONDS)
                    .build();

            try {
                JSONArray jsonArrayArticle = new JSONArray();
                finalArticles.stream().forEach(x -> jsonArrayArticle.put(x.constructJsonObject()));
                String jsonArrayString = jsonArrayArticle.toString();

                HttpUrl.Builder httpBuilder = HttpUrl.parse(pipelineEndpoint + submitQuery).newBuilder();
                RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonArrayString);
                Request request = new Request.Builder()
                        .post(body)
                        .url(httpBuilder.build().url())
                        .build();

                System.out.println(String.format("WorkerID:%s. Dispatching articles to pipeline.", workerID));
                Response response = batchClient.newCall(request).execute();
                System.out.println(String.format("WorkerID:%s. Batch submitted with status code: %d", workerID,  response.code()));

            } catch (SocketException e) {
                System.out.println(String.format("WorkerID:%s. Socket exception when posting articles to pipeline.\n Exception: %s", workerID, e.toString()));
                return false;
            } catch (IOException ex) {
                System.out.println(String.format("WorkerID:%s. IO exception when posting articles to pipeline.\n Exception: %s",workerID, ex.toString()));
                return false;
            }
            return true;

    }



    public static void postArticles(List<Article> finalArticles, Boolean oneByOne) throws IOException {

        OkHttpClient batchClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(300L, TimeUnit.SECONDS)
                .writeTimeout(300L, TimeUnit.SECONDS)
                .readTimeout(300L, TimeUnit.SECONDS)
                .build();

        HttpUrl.Builder httpBuilder = HttpUrl.parse(pipelineEndpoint + submitQuery).newBuilder();
        try {

            for (Article anArticle: finalArticles)
            {
                JSONArray jsonArrayArticle = new JSONArray();
                jsonArrayArticle.put(anArticle.constructJsonObject());
                String jsonArrayString = jsonArrayArticle.toString();
                RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonArrayString);
                Request request = new Request.Builder()
                        .post(body)
                        .url(httpBuilder.build().url())
                        .build();
                System.out.println(String.format("Dispatching article to pipeline. ID:%d",anArticle.getPMID()));
                Response response = batchClient.newCall(request).execute();
                System.out.println(String.format("Article %d submitted with status code: %d",anArticle.getPMID(),  response.code()));

            }


        } catch (SocketException e) {
            System.out.println(String.format("Socket exception when posting articles to pipeline.\n Exception: %s", e.toString()));
        } catch (IOException ex) {
            System.out.println(String.format("IO exception when posting articles to pipeline.\n Exception: %s", ex.toString()));
        }

    }

    private static String getPipelineURL() {
        System.out.println("Getting Pipeline URL");
        String URL = "http://localhost:9080";
        String env = ConfigLoader.getENV();
        if (env !=null && env.equals("AWS")) {
            String pipelineEndPoint = ConfigLoader.getPipelineEndpoint();
            if(!Strings.isNullOrEmpty(pipelineEndPoint))
                URL = pipelineEndPoint;
        }
        System.out.println(String.format("Pipeline endpoint:%s", URL));
        return URL;

    }
    public static void main (String [] args) throws IOException {

    }



}
