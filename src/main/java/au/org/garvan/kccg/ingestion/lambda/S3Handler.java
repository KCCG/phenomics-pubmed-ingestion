package au.org.garvan.kccg.ingestion.lambda;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.common.collect.Lists;
import org.json.JSONArray;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Created by ahmed on 18/7/17.
 */
public class S3Handler {

    private static String bucketName = ConfigLoader.getS3Bucket();

    private static AmazonS3 s3client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());

    public static void archiveArticles(List<Article> finalArticles) {
        s3client.setRegion(Region.getRegion(Regions.fromName(ConfigLoader.getREGION())));

        List<List<Article>> batchSplits = Lists.partition(finalArticles, 1000);
        batchSplits.forEach(lstArticles -> {
            String keyName = String.format("%d.json", getEpoch());
            System.out.println(String.format("Posting batch to S3. File name is:%s", keyName));
            JSONArray jsonArrayArticle = new JSONArray();
            lstArticles.forEach(x -> jsonArrayArticle.put(x.constructJsonObject()));
            byte[] bytesToWrite = jsonArrayArticle.toString().getBytes();
            ObjectMetadata omd = new ObjectMetadata();
            omd.setContentLength(bytesToWrite.length);
            s3client.putObject(new PutObjectRequest(bucketName, keyName, new ByteArrayInputStream(bytesToWrite), omd));

        });

    }

    public static long getEpoch() {
        LocalDateTime ldt = LocalDateTime.now();
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("Australia/Sydney"));
        return zdt.toInstant().toEpochMilli();

    }

}
