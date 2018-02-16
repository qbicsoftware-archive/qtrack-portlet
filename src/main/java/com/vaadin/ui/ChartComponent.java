package com.vaadin.ui;

import com.vaadin.annotations.JavaScript;

// these .js files are associated with the ChartComponent
@JavaScript({"d3.v4.min.js", "GraphConnector.js", "GraphSelector.js", "HelperFunctions.js", "StackedBarChart.js",
        "LineChart.js", "CalendarChart.js"})

/*
 * Represents the charts created in GraphSelector.js; calling the function setData calls the identically named function
 * within the GraphSelector.js which results in redrawing of the javascript content
 */
public class ChartComponent extends AbstractJavaScriptComponent{

    void setData(String data, String selectedOptions) {
        getState().data = data;
        getState().selectedOptions = selectedOptions;
    }

    public String getData() {
        return getState().data;
    }

    @Override
    protected ChartComponentState getState() {
        return (ChartComponentState) super.getState();
    }

}
