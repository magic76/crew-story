package com.crewpocket.story;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class StoryContext implements Serializable {
    public final List<String> characters=new ArrayList<>();
    public String currentEvent="";
    public final List<String> revealedFacts=new ArrayList<>();
    public final List<String> interactionHints=new ArrayList<>();
    public String spoilerBoundary="";

    public JSONObject toJson(){
        JSONObject o=new JSONObject();
        try{
            o.put("characters",new JSONArray(characters));
            o.put("currentEvent",currentEvent);
            o.put("revealedFacts",new JSONArray(revealedFacts));
            o.put("interactionHints",new JSONArray(interactionHints));
            o.put("spoilerBoundary",spoilerBoundary);
        }catch(Exception ignored){}
        return o;
    }
    public static StoryContext fromJson(JSONObject o){
        StoryContext c=new StoryContext(); if(o==null)return c;
        read(o.optJSONArray("characters"),c.characters);
        c.currentEvent=o.optString("currentEvent","");
        read(o.optJSONArray("revealedFacts"),c.revealedFacts);
        read(o.optJSONArray("interactionHints"),c.interactionHints);
        c.spoilerBoundary=o.optString("spoilerBoundary","");
        return c;
    }
    private static void read(JSONArray a,List<String> out){
        if(a==null)return;
        for(int i=0;i<a.length();i++){String v=a.optString(i,"");if(!v.isEmpty())out.add(v);}
    }
    public String toPromptBlock(){
        StringBuilder s=new StringBuilder();
        if(!characters.isEmpty())s.append("目前人物：").append(join(characters)).append("\n");
        if(!currentEvent.isEmpty())s.append("目前事件：").append(currentEvent).append("\n");
        if(!revealedFacts.isEmpty())s.append("孩子已知道：").append(join(revealedFacts)).append("\n");
        if(!spoilerBoundary.isEmpty())s.append("劇透邊界：").append(spoilerBoundary).append("\n");
        if(!interactionHints.isEmpty())s.append("可能疑問（只供理解，不要主動逐題問）：").append(join(interactionHints)).append("\n");
        return s.toString();
    }
    private static String join(List<String> a){StringBuilder s=new StringBuilder();for(int i=0;i<a.size();i++){if(i>0)s.append("、");s.append(a.get(i));}return s.toString();}
}
