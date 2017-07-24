package au.org.garvan.kccg.ingestion.lambda;

import jdk.nashorn.internal.objects.annotations.Property;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by ahmed on 3/7/17.
 */
public class Article {
    private  static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Property
    @Getter
    private int PMID;

    @Getter
    @Property
    private LocalDate articleDate;

    @Getter
    @Property
    private LocalDate dateCreated;

    @Getter
    @Property
    private LocalDate dateRevised;

    @Getter
    @Property
    private String articleTitle;

    @Getter
    @Property
    private String articleAbstract;

    @Getter
    @Property
    private String language;

    @Getter
    @Property
    private List<Author> authors;

    @Getter
    @Property
    private Boolean isComplete;

    public Article(JSONObject inputObject){
        try {
            articleDate = dateCreated = dateRevised= null;
            isComplete = Boolean.FALSE;

            PMID = (int) inputObject.getJSONObject("MedlineCitation").getJSONObject("PMID").get("content");
            articleTitle = (String) inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").get("ArticleTitle");
            language = (String) inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").get("Language");

            if (inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").has("AuthorList")){
                authors = constructAuthors(inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").getJSONObject("AuthorList"));
            }

            if (inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").has("ArticleDate")){
                articleDate = constructDate(inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").getJSONObject("ArticleDate"));
            }
            else
                articleDate = LocalDate.now();

            if (inputObject.getJSONObject("MedlineCitation").has("DateCreated"))
            {
                dateCreated = constructDate(inputObject.getJSONObject("MedlineCitation").getJSONObject("DateCreated"));
            }

            if (inputObject.getJSONObject("MedlineCitation").has("DateRevised"))
            {
                dateRevised = constructDate(inputObject.getJSONObject("MedlineCitation").getJSONObject("DateRevised"));
            }

            if (inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").has("Abstract")) {
                articleAbstract = constructAbstract(inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").getJSONObject("Abstract"));
            }

            else
                articleAbstract = "";

            isComplete = Boolean.TRUE;
        }
        catch (JSONException e)
        {
            System.out.println(String.format("JSON Error in Article ID:%s\n Exception: %s",PMID,e.toString()));
        }
        catch (ClassCastException e)
        {
            System.out.println(String.format("Cast Error in Article ID:%s\n Exception: %s",PMID,e.toString()));

        }



    }

    public Article(int PMID, LocalDate articleDate, String articleTitle, String articleAbstract, String language,  Boolean isComplete){
        this.PMID = PMID ;
        this.articleDate = articleDate;
        this.articleTitle = articleTitle;
        this.articleAbstract = articleAbstract;;
        this.language = language;
        this.isComplete = isComplete;

    }

    private LocalDate constructDate(JSONObject jsonDate){
        String strDate = String.format("%s-%s-%s", jsonDate.get("Day"),jsonDate.get("Month"),jsonDate.get("Year"));
        LocalDate containerDate = LocalDate.parse(strDate,formatter);
        return  containerDate;

    }

    private String constructAbstract(JSONObject jsonAbstract) {
        String finalAbstract ="";
        Object obj =  jsonAbstract.get("AbstractText");

        if (obj instanceof JSONArray){

            for (Object innerObj : (JSONArray)obj)
            {
                if (innerObj instanceof JSONObject)
                {
                   finalAbstract= finalAbstract.concat((String) ((JSONObject) innerObj).get("content"));

                }
                else if (innerObj instanceof  String)
                {
                    finalAbstract= finalAbstract.concat(innerObj.toString());
                }

            }
        }
        else if (obj instanceof String){
            finalAbstract = obj.toString();
        }

        return finalAbstract;

    }

    private List<Author> constructAuthors(JSONObject jsonAuthorsList)
    {
        Object obj =  jsonAuthorsList.get("Author");
        if (obj instanceof JSONArray){
            return StreamSupport.stream(jsonAuthorsList.getJSONArray("Author").spliterator(),false)
                    .map(JSONObject.class::cast)
                    .map(x-> new Author(x)).collect(Collectors.toList());

        }
        else if (obj instanceof JSONObject){
            return Arrays.asList(new Author((JSONObject) obj));

        }

        return new ArrayList<>();

    }

    public JSONObject constructJsonObject(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("PMID", PMID);
        jsonObject.put("articleDate", articleDate != null ? articleDate.toString(): "");
        jsonObject.put("dateCreated", dateCreated != null ? dateCreated.toString(): "");
        jsonObject.put("dateRevised", dateRevised != null ? dateRevised.toString(): "");
        jsonObject.put("articleTitle", articleTitle);
        jsonObject.put("articleAbstract", articleAbstract);
        jsonObject.put("language", language);
        jsonObject.put("authors", authors != null ? String.format("[%s]", authors.stream().map(Author::constructJson).collect(Collectors.joining(", "))):"");
        jsonObject.put("dateCreatedEpoch",dateCreated !=null ? dateCreated.toEpochDay() : LocalDate.now().toEpochDay());
        return jsonObject;


    }


}
