package au.org.garvan.kccg.ingestion.lambda;

import com.google.common.base.Strings;
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
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static DateTimeFormatter formatterLongMonth = DateTimeFormatter.ofPattern("dd-MMM-yyyy");


    @Getter
    private int PMID;

    @Getter
    private LocalDate articleDate;

    @Getter
    private LocalDate dateCreated;

    @Getter
    private LocalDate dateRevised;

    @Getter
    private String articleTitle;

    @Getter
    private String articleAbstract;

    @Getter
    private String language;

    @Getter
    private List<Author> authors;
    @Getter
    private List<MeshHeading> meshHeadingList;

    @Getter
    private Publication publication;

    @Getter
    private Boolean isComplete;

    public Article(JSONObject inputObject) {
        try {
            articleDate = dateCreated = dateRevised = null;
            isComplete = Boolean.FALSE;

            PMID = (int) inputObject.getJSONObject("MedlineCitation").getJSONObject("PMID").get("content");
            try {
                articleTitle = (String) inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").get("ArticleTitle");
            } catch (Exception e) {
                JSONObject titleObject = inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").getJSONObject("ArticleTitle");
                if (titleObject.has("content")) {
                    if (titleObject.get("content") instanceof JSONArray) {
                        JSONArray titleArray = (JSONArray) titleObject.get("content");
                        String tempTitle = "";
                        for (Object obj : titleArray) {
                            tempTitle = String.format("%s %s", tempTitle, obj);
                        }
                        articleTitle = tempTitle.trim();
                    } else if (titleObject.get("content") instanceof String) {
                        articleTitle = (String) titleObject.get("content");
                    }
                    else throw e;
                } else
                    throw e;
            }

            Object lang = inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").get("Language");
            if (lang instanceof JSONArray )
            {
                language= ((JSONArray)lang).get(0).toString();
            }
            else
                language = lang.toString();

            if (inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").has("AuthorList")) {
                authors = constructAuthors(inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").getJSONObject("AuthorList"));
            }

            if (inputObject.getJSONObject("MedlineCitation").has("DateRevised")) {
                dateRevised = constructDate(inputObject.getJSONObject("MedlineCitation").getJSONObject("DateRevised"));
            }

            if (inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").has("ArticleDate")) {
                articleDate = constructDate(inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").getJSONObject("ArticleDate"));
            } else if (inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").has("Journal")) {
                if (inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").getJSONObject("Journal").has("JournalIssue")) {
                    if (inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").getJSONObject("Journal").getJSONObject("JournalIssue").has("PubDate")) {
                        articleDate = constructDate(inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").getJSONObject("Journal").getJSONObject("JournalIssue").getJSONObject("PubDate"));
                    }

                }
            } else
                articleDate = dateRevised;


            if (inputObject.getJSONObject("MedlineCitation").has("DateCreated")) {
                dateCreated = constructDate(inputObject.getJSONObject("MedlineCitation").getJSONObject("DateCreated"));
            }
            else {
                // As date created is vanished from response so it is safe to assume article date as date created.
                dateCreated = articleDate;
            }


            if (inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").has("Abstract")) {
                articleAbstract = constructAbstract(inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").getJSONObject("Abstract"));
            } else
                articleAbstract = "";

            if (inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").has("Journal")) {
                publication = new Publication(inputObject.getJSONObject("MedlineCitation").getJSONObject("Article").getJSONObject("Journal"));
            }

            if (inputObject.getJSONObject("MedlineCitation").has("MeshHeadingList")) {
                meshHeadingList = constructMeshHeadings(inputObject.getJSONObject("MedlineCitation").getJSONObject("MeshHeadingList"));
            }

            if (articleDate == null) {
                articleDate = LocalDate.now();
            }

            if (!Strings.isNullOrEmpty(articleAbstract))
                isComplete = Boolean.TRUE;
            else
            {
                System.out.println(String.format("Abstract missing in Article ID:%s", PMID));

            }

        } catch (JSONException e) {
            System.out.println(String.format("JSON Error in Article ID:%s\n Exception: %s", PMID, e.toString()));
        } catch (ClassCastException e) {
            System.out.println(String.format("Cast Error in Article ID:%s\n Exception: %s", PMID, e.toString()));

        }


    }

    public Article(int PMID, LocalDate articleDate, String articleTitle, String articleAbstract, String language, Boolean isComplete) {
        this.PMID = PMID;
        this.articleDate = articleDate;
        this.articleTitle = articleTitle;
        this.articleAbstract = articleAbstract;
        this.language = language;
        this.isComplete = isComplete;

    }

    private LocalDate constructDate(JSONObject jsonDate) {

        String day = jsonDate.has("Day") ? jsonDate.get("Day").toString() : "01";
        String month = jsonDate.has("Month") ? jsonDate.get("Month").toString() : "01";
        String year = jsonDate.has("Year") ? jsonDate.get("Year").toString() : "1900";
        String strDate = String.format("%s-%s-%s", day, month, year);
        LocalDate containerDate;
        if (month.length() == 2)
            containerDate = LocalDate.parse(strDate, formatter);
        else if (month.length() == 3)
            containerDate = LocalDate.parse(strDate, formatterLongMonth);
        else
            containerDate = LocalDate.now();

        return containerDate;

    }

    private String constructAbstract(JSONObject jsonAbstract) {
        String finalAbstract = "";
        Object obj = jsonAbstract.get("AbstractText");

        if (obj instanceof JSONArray) {
            for (Object innerObj : (JSONArray) obj) {
                if (innerObj instanceof JSONObject) {
                    if (((JSONObject) innerObj).has("content")) {
                        Object deepObject = ((JSONObject) innerObj).get("content");
                        if (deepObject instanceof String)
                            finalAbstract = finalAbstract.concat((String) deepObject);
                        else if (deepObject instanceof JSONArray) {
                            String deepString = "";
                            for (Object partialSent : (JSONArray) deepObject) {
                                deepString = deepString.concat((String) partialSent);
                            }
                            finalAbstract = finalAbstract.concat((String) deepString);
                        }
                    }
                } else if (innerObj instanceof String) {
                    finalAbstract = finalAbstract.concat(innerObj.toString());
                }

            }
        } else if (obj instanceof String) {
            finalAbstract = obj.toString();

        } else if (obj instanceof JSONObject) {
            if (((JSONObject) obj).has("content")) {
                Object deepObject = ((JSONObject) obj).get("content");
                if (deepObject instanceof String)
                    finalAbstract = finalAbstract.concat((String) deepObject);
                else if (deepObject instanceof JSONArray) {
                    String deepString = "";
                    for (Object partialSent : (JSONArray) deepObject) {
                        deepString = deepString.concat((String) partialSent);
                    }
                    finalAbstract = finalAbstract.concat((String) deepString);
                }
            }
        }


        return finalAbstract;
    }

    private List<Author> constructAuthors(JSONObject jsonAuthorsList) {
        Object obj = jsonAuthorsList.get("Author");
        if (obj instanceof JSONArray) {
            return StreamSupport.stream(jsonAuthorsList.getJSONArray("Author").spliterator(), false)
                    .map(JSONObject.class::cast)
                    .map(x -> new Author(x)).collect(Collectors.toList());

        } else if (obj instanceof JSONObject) {
            return Arrays.asList(new Author((JSONObject) obj));

        }

        return new ArrayList<>();

    }

private List<MeshHeading> constructMeshHeadings(JSONObject jsonMeshHeadingList) {
        Object obj = jsonMeshHeadingList.get("MeshHeading");
        if (obj instanceof JSONArray) {
            return StreamSupport.stream(jsonMeshHeadingList.getJSONArray("MeshHeading").spliterator(), false)
                    .map(JSONObject.class::cast)
                    .map(x -> new MeshHeading(x)).collect(Collectors.toList());

        } else if (obj instanceof JSONObject) {
            return Arrays.asList(new MeshHeading((JSONObject) obj));

        }

        return new ArrayList<>();

    }

    public JSONObject constructJsonObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("PMID", PMID);
        jsonObject.put("articleDate", articleDate != null ? articleDate.toString() : "");
        jsonObject.put("dateCreated", dateCreated != null ? dateCreated.toString() : "");
        jsonObject.put("dateRevised", dateRevised != null ? dateRevised.toString() : "");
        jsonObject.put("articleTitle", articleTitle);
        jsonObject.put("articleAbstract", articleAbstract);
        jsonObject.put("language", language);
        jsonObject.put("publication", publication.constructJson());

        if(authors != null)
        {
            JSONArray authorsArray = new JSONArray();
            for (Author a: authors)
                authorsArray.put(a.constructJson());
            jsonObject.put("authors", authorsArray);
        }

        if(meshHeadingList != null)
        {
            JSONArray meshArray = new JSONArray();
            for (MeshHeading m: meshHeadingList)
                meshArray.put(m.constructJson());
            jsonObject.put("meshHeadingList", meshArray);
        }



        jsonObject.put("dateCreatedEpoch", dateCreated != null ? dateCreated.toEpochDay() : LocalDate.now().toEpochDay());
        return jsonObject;


    }


}
