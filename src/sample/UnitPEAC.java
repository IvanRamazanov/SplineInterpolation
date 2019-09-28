package sample;

import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class UnitPEAC {
    private GridPane layout;
    private int row;
    private String unitName;
    private int unitIndex;

    public UnitPEAC(int index,XmlUnit unit){
        unitIndex=index;

        layout=new GridPane();
        row=0;

        layout.setHgap(10);

        XmlUnit params=null;

        for(XmlUnit ch:unit.getChildren()){
            if(ch.getParameter("name")!=null){
                if(ch.getParameter("name").equals("Properties")){
                    params=ch;

                    break;
                }
            }
        }

        if(params!=null) {
            for (XmlUnit ch : params.getChildren()) {
                String name;
                if((name=ch.getParameter("name"))==null){
                    name=ch.getName();
                }

                if(name.equals("name"))
                    unitName=ch.getContent();

                layout.add(new Label(name), 0, row);

                // content
                initContent(name, ch);

                row++;
            }
        }else
            throw new Error("No params in PEAC unit!");

    }

    private void initContent(String parentName,XmlUnit xmlUnit){
        if(xmlUnit.getChildren().isEmpty()){
            layout.add(new Label(xmlUnit.getContent()),1,row);
        }else{
            for(XmlUnit ch:xmlUnit.getChildren()){
                row++;

                String name;
                if((name=ch.getParameter("name"))==null){
                    name=ch.getName();
                }
                name=parentName+"|"+name;
                layout.add(new Label(name),0,row);

                //content
                initContent(name,ch);
            }
        }
    }

    public GridPane getLayout() {
        return layout;
    }

    public void setLayout(GridPane layout) {
        this.layout = layout;
    }

    public String getUnitName() {
        return unitName;
    }

    public int getUnitIndex(){
        return unitIndex;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }
}
