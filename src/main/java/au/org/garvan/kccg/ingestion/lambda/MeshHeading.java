package au.org.garvan.kccg.ingestion.lambda;

import jdk.nashorn.internal.objects.annotations.Property;
import lombok.Getter;
import org.json.JSONObject;

/**
 * Created by ahmed on 7/3/18.
 */
public class MeshHeading {

    @Getter
    @Property
    private String UI;
    @Getter
    @Property
    private String text;
    @Getter
    @Property
    private Boolean isMajor;

    public MeshHeading(JSONObject jsonMeshHeading){
            JSONObject descriptor = jsonMeshHeading.getJSONObject("DescriptorName");
            UI = descriptor.has("UI")?(String) descriptor.get("UI"):"";
            text = descriptor.has("content")?(String) descriptor.get("content"):"";
            isMajor = descriptor.has("MajorTopicYN")?  ((String) descriptor.get("MajorTopicYN")=="N"?false:true) :false;
    }

  public JSONObject constructJson()
  {
      JSONObject meshHeading = new JSONObject();
      meshHeading.put("UI", UI);
      meshHeading.put("text", text);
      meshHeading.put("isMajor", isMajor);
      return meshHeading;
  }

}
