/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sample;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.File;

/**
 *
 * @author Иван
 */
public class XmlParser {
    private File fileName;
    private BufferedReader fileReader;
    private long fileIndex;
    private final int BUFF_LEN=1024*4;
    private int effectiveBuffSize,currentBuffIndex;
    private char[] chars=new char[BUFF_LEN];

    XmlParser(File file){
        this.fileName=file;
    }

    public static String rgbToHash(String rgb){
        String R,G,B;
        int r,g,b;
        char divider='.';
        int i=rgb.indexOf(divider);
        if(i==-1) {
            divider = ',';
            if ((i = rgb.indexOf(divider)) == -1)
                throw new Error("Wrong RGB formatting in: " + rgb);
        }
        R=rgb.substring(0,i);
        G=rgb.substring(i+1);
        i=G.indexOf(divider);
        if(i==-1)
            throw new Error("Wrong RGB formatting in:" +rgb);
        B=G.substring(i+1);
        G=G.substring(0,i);
        try{
            r=Integer.parseInt(R);
            g=Integer.parseInt(G);
            b=Integer.parseInt(B);
            StringBuilder out=new StringBuilder("#");
            //R
            if(r<16)
                out.append("0"+Integer.toHexString(r));
            else if(r<256)
                out.append(Integer.toHexString(r));
            else
                throw new Error("R in rgb greater than 255!: "+rgb);

            //G
            if(g<16)
                out.append("0"+Integer.toHexString(g));
            else if(g<256)
                out.append(Integer.toHexString(g));
            else
                throw new Error("G in rgb greater than 255!: "+rgb);

            //B
            if(b<16)
                out.append("0"+Integer.toHexString(b));
            else if(b<256)
                out.append(Integer.toHexString(b));
            else
                throw new Error("B in rgb greater than 255!: "+rgb);

            //output
            return out.toString();
        }catch(Exception ex){
            System.err.print(ex.getMessage());
        }
        return null;
    }

    public List<XmlUnit> parseXml() throws IOException{
        fileReader=new BufferedReader(new FileReader(fileName));
        List<XmlUnit> out=new ArrayList<>();

        //updateChars();
        if(updateChars()<=0){
            return null;
        }
        fileIndex=0;
        currentBuffIndex=-1;
        while(effectiveBuffSize>0) {
            char ch=getNextLetter();
            if (ch == '<') {
                XmlUnit xml = getXmlUnit();
                out.add(xml);
                if (!xml.isComment()) {
                    // find end
                    parseXmlUnitBody(xml);
                }
            }
        }

        fileReader.close();
        return out;
    }

    public static TreeView getTree(List<XmlUnit> list){
        TreeView<XmlUnit> out=new TreeView<>();

        TreeItem<XmlUnit> root=new TreeItem<>();
        out.setRoot(root);
        out.setShowRoot(false);

        for (XmlUnit xml:list) {
            TreeItem tItem=getTreeItem(xml);

            root.getChildren().add(tItem);
        }

        return out;
    }

    private static TreeItem<XmlUnit> getTreeItem(XmlUnit xml){
        TreeItem<XmlUnit> tItem=new TreeItem<>();

        tItem.setValue(xml);

        if(xml.hasChildren()){
            for(XmlUnit subXml:xml.getChildren()){
                tItem.getChildren().add(getTreeItem(subXml));
            }
        }

        return tItem;
    }

    private void ignoreKey() throws IOException{
        while(getNextLetter()!='>');
    }

    /**
     *
     * @param xmlUnit
     */
    private void parseXmlUnitBody(XmlUnit xmlUnit) throws IOException{
        boolean isEndFound=false;
        StringBuilder rawContent=new StringBuilder();
        char c=chars[currentBuffIndex];
        if(!isLetter(c))
            c=getNextLetter();

        xmlUnit.setContentFirstIndex(fileIndex+currentBuffIndex);
        while(!isEndFound){
//            if(cnt>=buffLen) {
//                buffLen = fileReader.read(buff, 0, buff.length);
//                cnt=0;
//                if(buffLen<=0)
//                    throw new Error("Can't find end of "+ xmlUnit.getName());
//            }

            if(c=='<'){
                if((c=getNextLetter())=='/'){ // end of the body?
                    getNextLetter();
                    String name=getWord();
                    if(name.equals(xmlUnit.getName())){
                        isEndFound=true;
                        xmlUnit.setContent(rawContent.toString());
                    }else{
                        throw new Error("Wrong ending of unit!");
                    }
                }else{
                    // new unit
                    XmlUnit xml = getXmlUnit();
                    parseXmlUnitBody(xml);
                    xmlUnit.addChildren(xml);

                    c=getNextLetter();
                }
            }else{
                rawContent.append(c);

                c=getNextLetter();
            }
        }
    }

    private int updateChars() throws IOException{
        effectiveBuffSize=fileReader.read(chars, 0, BUFF_LEN);
        fileIndex+=effectiveBuffSize;
        currentBuffIndex=0;
        return effectiveBuffSize;
    }

    private char getNextLetter() throws IOException {
        if(++currentBuffIndex==effectiveBuffSize)
            updateChars();
        while(!isLetter(chars[currentBuffIndex])) {
            currentBuffIndex++;
            if(currentBuffIndex==effectiveBuffSize)
                updateChars();
        }
        return chars[currentBuffIndex];
    }

    private boolean isLetter(char ch){
        return ch != ' ' && ch != '\t' && ch != '\n' && ch != '\r' && ch != 0;
    }

    /**
     * starts from current buff position
     * @return a word string
     */
    private String getWord() throws IOException{
        StringBuilder out=new StringBuilder();
        boolean endFlag=false;
        char ch;

        while(!endFlag){
            ch=chars[currentBuffIndex];
            if(isLetter(ch)&&ch!='>'){
                out.append(ch);
            }else{
                endFlag=out.length()>0; // if empty, continue
                if(endFlag)
                    break;
            }
            currentBuffIndex++;
            if(currentBuffIndex==effectiveBuffSize)
                updateChars();
        }
        return out.toString();
    }

    /**
     * starts from current buff position
     * @return a word string
     */
    private String getXmlAttribute() throws IOException{
        StringBuilder out=new StringBuilder();
        boolean endFlag=false;
        char ch;

        //parse name
        while(!endFlag){
            ch=chars[currentBuffIndex];
            if(isLetter(ch)&&ch!='>'&&ch!='='){
                out.append(ch);
            }else{
                endFlag=out.length()>0; // if empty, continue
                if(endFlag)
                    break;
            }
            currentBuffIndex++;
            if(currentBuffIndex==effectiveBuffSize)
                updateChars();
        }

        if(chars[currentBuffIndex]!='=')
            throw new Error("Wrong attribute format! No equal '=' char found!");

        out.append(chars[currentBuffIndex]); // append '='

        //parse value
        getNextLetter();

        if(chars[currentBuffIndex]!='\"' && chars[currentBuffIndex]!='\'')
            throw new Error("Wrong attribute format! Value should be inside \" or \' !");

        endFlag=false;
        getNextLetter();
        while(!endFlag){
            ch=chars[currentBuffIndex];
            if(ch=='\'' || ch=='\"'){
                endFlag=true;
            }else if(ch=='>'){
                throw new Error("Wrong attribute format! Value has no ending \" or \' !");
            }else{
                out.append(ch);
            }
            currentBuffIndex++;
            if(currentBuffIndex==effectiveBuffSize)
                updateChars();
        }

        return out.toString();
    }

    /**
     * if current == '<'
     * @return
     */
    private XmlUnit getXmlUnit() throws IOException{
        String name, key;
        List<String> rawContent=new ArrayList<>();

        if(chars[currentBuffIndex]=='<')
            getNextLetter();

        key=getXmlHeader();
        return new XmlUnit(key);

//        name=getWord();
//
//        if(chars[currentBuffIndex]!='>'){
//            // find end
//            while(chars[currentBuffIndex]!='>'){
//                getNextLetter();
//                rawContent.add(getXmlAttribute());
//            }
//        }
//        return new XmlUnit(name,rawContent);
    }

    private String getXmlHeader() throws IOException{
        boolean endFlag=false;
        StringBuilder out=new StringBuilder();
        char ch;
        while(!endFlag){
            if(currentBuffIndex>=chars.length) {
                int iadf = 0;
            }
            ch=chars[currentBuffIndex];
            if(ch=='>'){
                endFlag=true;
            }else{
                out.append(ch);
            }
            currentBuffIndex++;
            if(currentBuffIndex==effectiveBuffSize) {
                updateChars();
                if (effectiveBuffSize<=0)
                    throw new Error("End of file! At getXmlHeader");
            }
        }
        return out.toString();
    }

    private StringBuilder cleanStartOfBuff(char[] buff,int len){
        StringBuilder sb=new StringBuilder(buff.length/2);
        boolean flag=false;
        for(int i=0;i<len;i++){
            char ch=buff[i];
            if((ch!='\n')&&(ch!='\t')&&(ch!=' ')&&(ch!='\r')&&!flag) {
                flag = true;
                sb.append(ch);
            }else{
                if(flag){
                    sb.append(ch);
                }
            }
        }
        return sb;
    }

    public static List<String> getBlockList(String stream){
        Scanner sc=new Scanner(stream);
        StringBuilder out=new StringBuilder();
        List<String> output=new ArrayList<>(2);
        sc.useDelimiter("\r\n");
        while(sc.hasNext()){
            String blockName=sc.next();
            if(blockName.startsWith("\t")){
                throw new Error("Wrong block formatting!");
            }
            String endKey=blockName.substring(0,1)+"/"+blockName.substring(1);

            //for each block
            while(sc.hasNext()){
                String line=sc.next();
                if(line.equals(endKey)){
                    output.add(out.toString());
                    out.setLength(0);
                    break;
                }else{
                    out.append(removeTab(line)+"\r\n"); //remove one \t char
                }
            }
        }
        return output;
    }

    public static String removeTab(String in){
        if(in.startsWith("\t")){
            return in.substring(1);
        }else if(in.startsWith("    ")){
            return in.substring(4);
        }else{
            throw new Error("No tabulation in: "+in);
        }
    }

    public static String getBlock(java.io.InputStream stream,String key) throws IOException{
        Scanner sc=new Scanner(stream);
        StringBuilder out=new StringBuilder();
        String endKey="</"+key.substring(1);
        sc.useDelimiter("\r\n");
        while(sc.hasNext()){
            String line=sc.next();
            if(line.equals(key)){
                int cnt=0;
                while(sc.hasNext()){
                    line=sc.next();
                    if(line.equals(key)) {
                        cnt++;
                        out.append(removeTab(line)+"\r\n"); //remove one \t char
                    }else
                    if(line.equals(endKey)){
                        if(cnt==0)
                            return out.toString();
                        else {
                            out.append(removeTab(line)+"\r\n");
                            cnt--;
                        }
                    }else{
                        out.append(removeTab(line)+"\r\n");
                    }
                }
            }
        }
        return null;
    }

    static public String formatBlock(String block){
        Scanner sc=new Scanner(block);
        StringBuilder sb=new StringBuilder();
        sc.useDelimiter("\r\n");
        int cnt=0;
        while(sc.hasNext()){
            String line=sc.next()+"\r\n";

            if(line.startsWith("</")){
                cnt--;
            }
            for(int i=0;i<cnt;i++){ //append
                line="\t"+line;
            }
            if(!line.contains("</")){
                cnt++;
            }

            sb.append(line);
        }
        return sb.toString();
    }

    public String replaceContent(XmlUnit unit,String newContent) throws IOException{
        long contentStartIndex=unit.getContentFirstIndex();
        StringBuilder out=new StringBuilder();
        fileReader=new BufferedReader(new FileReader(fileName));

        //updateChars();
        if(updateChars()<=0){
            return null;
        }
        fileIndex=0;
        currentBuffIndex=0;

        boolean contentReplaced=false;
        while(effectiveBuffSize>0) {
            if((fileIndex+effectiveBuffSize>contentStartIndex)&&!contentReplaced){
                // add only part of buffer

                currentBuffIndex=(int)(contentStartIndex-fileIndex);
                out.append(chars,0,currentBuffIndex);

                //fill content
                out.append(newContent);

                //shift buffer
                while(chars[currentBuffIndex]!='<'){
                    currentBuffIndex++;
                    if(currentBuffIndex==effectiveBuffSize){
                        updateChars();
                    }
                }

                out.append(chars,currentBuffIndex,effectiveBuffSize-currentBuffIndex);

                //reset flag
                contentReplaced=true;
            }else{
                out.append(chars,0,effectiveBuffSize);
            }
            updateChars();
        }

        fileReader.close();

        return out.toString();
    }
}

