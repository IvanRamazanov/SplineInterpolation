package sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class XmlUnit {
    private String name;
    private HashMap<String,Object> parameters=new HashMap<>();
    private ArrayList<XmlUnit> children=new ArrayList<>();
    private String typeKey="",content;

    private long contentFirstIndex;

    XmlUnit(String name,List<String> rawParams){
        setName(name);
        // parse params
        if(rawParams!=null){
            // parse
            parameters=new HashMap<>();
            for (String p:rawParams
                 ) {
                String pName=p.substring(0,p.indexOf('=')),
                       pValue=p.substring(p.indexOf('=')+1);
                parameters.put(pName,pValue);
            }
        }
    }

    /**
     *
     * @param rawData smthng like: "!--name param1=value--!
     */
    public XmlUnit(String rawData){

        int len=rawData.length(),i=0;
        char ch=rawData.charAt(i);
        rawData=rawData.replaceAll("\t"," ");

        //cut end of string
        while(rawData.charAt(len-1)==' ')
            len--;

        while(ch==' ')
            ch=rawData.charAt(++i);

        if(rawData.substring(i).startsWith("!--")){
            typeKey="!--";
            i+=3;
            len-=3;
        }else if(rawData.substring(i).startsWith("?")){
            typeKey="?";
            i++;
            len--;
        }

        rawData=rawData.substring(i,len);
        len=rawData.length();
        i=0;
        // skip white space
        ch=rawData.charAt(i);
        while(ch==' ')
            ch=rawData.charAt(++i);
        rawData=rawData.substring(i);
        len=rawData.length();
        i=0;

        if(rawData.indexOf(' ')!=-1){
            name=rawData.substring(0,rawData.indexOf(' '));
            rawData=rawData.substring(name.length());
            len=rawData.length();
        }else{
            name=rawData;
            i=len;
        }


        while(rawData.indexOf("=")!=-1){
            ch=rawData.charAt(i);
            while(ch==' ')
                ch=rawData.charAt(++i);

            String pName=rawData.substring(i,rawData.indexOf('=')),pValue;
            i+=pName.length()+1;


            rawData=rawData.substring(i);
            len=rawData.length();
            int pStart=rawData.indexOf('\"')+1,pStop=rawData.indexOf('\"',pStart);
            pValue=rawData.substring(pStart,pStop);
            if(pStop+1<len)
                rawData=rawData.substring(pStop+1);
            else
                rawData="";
            i=0;

            parameters.put(pName,pValue);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<XmlUnit> getChildren() {
        return children;
    }

    public void addChildren(XmlUnit children) {
        this.children.add(children);
    }

    public boolean isComment(){
        return typeKey.equals("!--") || typeKey.equals("?");
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean contains(String paramName){
        return parameters.containsKey(paramName);
    }

    public XmlUnit findUnitWithParam(String paramName,String paramValue){
        XmlUnit match=null;
        if(parameters.containsKey(paramName)){
            if(parameters.get(paramName).equals(paramValue)){
                match=this;
            }
        }
        if(match==null){
            for(XmlUnit child:getChildren()){
                XmlUnit subMatch=child.findUnitWithParam(paramName,paramValue);
                if(subMatch!=null){
                    match=subMatch;
                    break;
                }
            }
        }
        return match;
    }

    /**
     * Looking for <asdf name="paramName">contentValue</asdf>
     * @param paramName
     * @param contentValue
     * @return
     */
    public boolean hasContent(String paramName,String contentValue){
        boolean match=false;
        if(parameters.containsKey("name")){
            if(parameters.get("name").equals(paramName)){
                match=getContent().equals(contentValue);
            }
        }
        return match;
    }

    public String getContent() {
        return content;
    }

    public String getParameter(String key){
        return (String)parameters.get(key);
    }

    public boolean hasChildren(){
        return children.size()>0;
    }

    @Override
    public String toString(){
        if(contains("Name")){
            return getParameter("Name");
        }else if(contains("name")) {
            return getParameter("name");
        }else{
            return getName();
        }
    }

    public long getContentFirstIndex() {
        return contentFirstIndex;
    }

    public void setContentFirstIndex(long contentFirstIndex) {
        this.contentFirstIndex = contentFirstIndex;
    }
}
