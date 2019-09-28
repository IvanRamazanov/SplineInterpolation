package sample;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Controller {
    private XmlUnit objects=null;
    private ToggleGroup tg;
    private File file;

    @FXML
    private VBox objectArray;

    @FXML
    private ScrollPane contentArea,buttonScrollPane;

    @FXML
    private TextField searchBar;

    @FXML
    public void interpolate() throws IOException{
        if(file!=null && objects!=null) {

            XmlUnit match = objects.getChildren().get(tg.getToggles().indexOf(tg.getSelectedToggle()));

            XmlUnit prop = match.findUnitWithParam("name", "pointsArray");

            if(prop!=null){
                // new content Calculation
                String oldArr=prop.getContent();
                oldArr=oldArr.replaceAll(" ","");
                oldArr=oldArr.replaceAll("\n","");
                oldArr=oldArr.replaceAll("\t","");
                oldArr=oldArr.replaceAll("\r","");
                List<String> doubleList=new ArrayList<>();

                while (!oldArr.isEmpty()){
                    int idx=oldArr.indexOf(',');
                    if(idx!=-1) {
                        String part = oldArr.substring(0, idx);
                        doubleList.add(part);
                    }else if(!oldArr.isEmpty()){
                        doubleList.add(oldArr);
                        oldArr="";
                    }

                    oldArr=oldArr.substring(idx+1,oldArr.length());
                }

                File f=new File("tempData.bin");
                DataOutputStream dos=new DataOutputStream(new FileOutputStream(f));

                for(String str:doubleList) {
                    dos.writeDouble(Double.parseDouble(str));
                    //dos.writeInt((int)(Double.parseDouble(str)*100));
                }
                dos.close();

                // call python
                Process p = Runtime.getRuntime().exec("python interpDriver.py tempData.bin");

                BufferedReader stdInput = new BufferedReader(new
                        InputStreamReader(p.getInputStream()));

                // read the output from the command
                String consoleOut=null;
                while ((consoleOut = stdInput.readLine()) != null) {
                    if(consoleOut.equals("1")){
                        //all ok
                        XmlParser parser=new XmlParser(file);

                        //read content
                        String newContent="";
                        DataInputStream dis=new DataInputStream(new FileInputStream(f));
                        boolean available=true;
                        int strLen=8,cnt=0;
                        try{
                            while(available){
                                newContent+=String.format(Locale.getDefault(),"%.2f",dis.readDouble()).replaceAll(",",".");
                                //newContent+=Double.toString(((double)dis.readInt())/100.0);
                                if(cnt==strLen) {
                                    newContent +=",\n";
                                    cnt=0;
                                }else {
                                    newContent += ",";
                                    cnt++;
                                }
                            }
                        }catch(EOFException eof){
                            available=false;
                        }
                        dis.close();

                        newContent=newContent.substring(0,newContent.lastIndexOf(',')); // cut last 2 chars
                        String newFile=parser.replaceContent(prop,newContent);
                        f.delete();

                        FileChooser fc=new FileChooser();
                        fc.setInitialDirectory(file.getParentFile());
                        fc.setInitialFileName(file.getName());
                        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Panel","*.panel"));
                        File saveFile=fc.showSaveDialog(null);
                        BufferedWriter fileWriter=new BufferedWriter(new FileWriter(saveFile));

                        fileWriter.write(newFile);
                        fileWriter.close();

                        //reparse
                        if(file.equals(saveFile)){
                            int i=tg.getToggles().indexOf(tg.getSelectedToggle());
                            objectArray.getChildren().clear();

                            parseFile();

                            tg.selectToggle(tg.getToggles().get(i));
                        }
                    }else{
                        System.out.print("Python is bad");
                    }
                }
            }
        }
    }

    private void parseFile(){
        try {
            XmlParser parser=new XmlParser(file);

            List<XmlUnit> xmlList=parser.parseXml();

            if(xmlList!=null) {
                XmlUnit root=null;
                for(XmlUnit unit:xmlList){
                    if(unit.getName().equals("root")){
                        root=unit;
                        break;
                    }
                }

                if(root!=null){
                    XmlUnit definition=null;
                    for(XmlUnit unit:root.getChildren()){
                        if(unit.getParameter("name").equals("Definition")){
                            definition=unit;
                            break;
                        }
                    }

                    if(definition!=null){
                        objects=null;
                        for(XmlUnit unit:definition.getChildren()){
                            if(unit.getParameter("name").equals("Objects")){
                                objects=unit;
                                break;
                            }
                        }

                        if(objects!=null){
                            tg=new ToggleGroup();
                            tg.selectedToggleProperty().addListener((obsVal, oldVal, newVal) -> {
                                if (newVal == null)
                                    oldVal.setSelected(true);
                            });

                            int j=0;
                            for(XmlUnit object:objects.getChildren()){
                                UnitPEAC unitPEAC=new UnitPEAC(j,object);

                                ToggleButton btn;
                                objectArray.getChildren().add(btn=new ToggleButton(unitPEAC.getUnitName()));
                                btn.setMnemonicParsing(false);
                                btn.setToggleGroup(tg);
                                btn.selectedProperty().addListener((obs,oldVal,newVal)->{
                                    if(newVal)
                                        contentArea.setContent(unitPEAC.getLayout());
                                });

                                j++;
                            }

                            // init toggle
                            tg.selectToggle(tg.getToggles().get(0));
                        }else
                            throw new Error("No objects in xml file!");
                    }else
                        throw new Error("No definition in xml file!");
                }else
                    throw new Error("No root in xml file!");
            }else
                throw new Error("Null Xml file!");
        }catch(IOException ex){
            System.err.print(ex.getMessage());
        }
    }

    @FXML
    public void openFile(){
        FileChooser ch=new FileChooser();
        ch.getExtensionFilters().add(new FileChooser.ExtensionFilter("panel", "*.panel"));
        file=ch.showOpenDialog(null);
        if(file!=null) {
            parseFile();
        }
    }

    @FXML
    public void initialize(){

//        treeView.getSelectionModel().selectedItemProperty().addListener((v, oldValue, newValue) -> {
//// Body would go here
//            Object val=newValue.getValue();
//
//            if(val instanceof XmlUnit) {
//                contentArea.setText(((XmlUnit)val).getContent());
//            }
//        });

        searchBar.textProperty().addListener((obs,oldVal,newVal)->{
            if(!newVal.isEmpty()){
                findUnit(searchBar.getText());
            }
        });
    }

    private void findUnit(String name){
        int j=0;
        if(objects!=null){
            XmlUnit match=null;

            for(XmlUnit item:objects.getChildren()){
                match=findItem(item,"name",name);
                if(match!=null) {
                    Toggle btn=tg.getToggles().get(j);
                    tg.selectToggle(btn);

                    double d=(double)j/tg.getToggles().size();
                    d=d*buttonScrollPane.getVmax();
                    buttonScrollPane.setVvalue(d);
                    break;
                }
                j++;
            }

        if(match!=null) {
            String row = match.getParameter("name");

            // Now the row can be selected.

        }
        }
    }

    private XmlUnit findItem(XmlUnit item,String name,String value){
        if(item.hasContent(name,value)){
            return item;
        }else{
            XmlUnit match=null;
            for(XmlUnit leaf:item.getChildren()){
                match=findItem(leaf,name,value);
                if(match!=null){
                    return match;
                }
            }
        }
        return null;
    }
}
