package com.vaadin.ui;

import com.vaadin.annotations.JavaScript;
import com.vaadin.shared.ui.JavaScriptComponentState;

/**
 * Created by caspar on 04.07.17.
 */

@JavaScript({"d3.v4.min.js", "myGraphConnector.js", "myGraph.js"})

public class LineChart extends AbstractJavaScriptComponent{

    public void setData(String data) {
        getState().data = data;
    }

    public String getData() {
        return getState().data;
    }

    @Override
    protected LineChartState getState() {
        return (LineChartState) super.getState();
    }

}
