package com.vaadin.ui;

import com.vaadin.shared.ui.colorpicker.Color;

/**
 * object holding all the selected options from the user
 */
class SelectedOptions {

    private Color colorForAvgSteps;
    private Color colorForUserSteps;
    private long startDate;
    private long endDate;
    private String dotTypeSelection;
    private String timeSelected;
    private String plotSelected;
    private String sortBarsBy;

    /**
     * constructor
     * @param colorForAvgSteps which color the user selected for the avg steps
     * @param colorForUserSteps which color the user selected for the user steps
     * @param startDate which start date the user has selected
     * @param endDate which end date the user has selected
     * @param dotTypeSelection which dot types the user has selected
     * @param timeSelected which time the user has selected
     * @param plotSelected whether the bar chart or the line chart is selected
     * @param sortBarsBy whether to sort the bars by date, or in ascending or in descending order
     */
    SelectedOptions(Color colorForAvgSteps, Color colorForUserSteps, long startDate, long endDate,
                    String dotTypeSelection, String timeSelected, String plotSelected, String sortBarsBy) {
        this.colorForAvgSteps = colorForAvgSteps;
        this.colorForUserSteps = colorForUserSteps;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dotTypeSelection = dotTypeSelection;
        this.timeSelected = timeSelected;
        this.plotSelected = plotSelected;
        this.sortBarsBy = sortBarsBy;
    }
    
    /**
     * returns the JSON representation of the object
     * @return the JSON representation
     */
    String getJSONRepresentation() {

        return "{\"colorForAvgSteps\":\"" + getColorForAvgSteps().getCSS() + "\"," +
                "\"colorForUserSteps\":\"" + getColorForUserSteps().getCSS() + "\"," +
                "\"startDate\":\"" + getStartDate() + "\"," +
                "\"endDate\":\"" + getEndDate() + "\"," +
                "\"timeSelected\":\"" + getTimeSelected() + "\"," +
                "\"dotTypeSelection\":\"" + getDotTypeSelection() + "\"," +
                "\"plotSelected\":\"" + getPlotSelected() + "\"," +
                "\"sortBarsBy\":\"" + getSortBarsBy() + "\"" +
                "}";
    }


    /*
    All the setter and getter methods for the fields..
     */

    Color getColorForAvgSteps() {
        return colorForAvgSteps;
    }

    void setColorForAvgSteps(Color colorForAvgSteps) {
        this.colorForAvgSteps = colorForAvgSteps;
    }

    Color getColorForUserSteps() {
        return colorForUserSteps;
    }

    void setColorForUserSteps(Color colorForUserSteps) {
        this.colorForUserSteps = colorForUserSteps;
    }

    long getStartDate() {
        return startDate;
    }

    void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    long getEndDate() {
        return endDate;
    }

    void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    String getDotTypeSelection() {
        return dotTypeSelection;
    }

    void setDotTypeSelection(String dotTypeSelection) {
        this.dotTypeSelection = dotTypeSelection;
    }

    String getTimeSelected() {
        return timeSelected;
    }

    void setTimeSelected(String timeSelected) {
        this.timeSelected = timeSelected;
    }

    String getPlotSelected() { return plotSelected; }

    void setPlotSelected(String plotSelected) { this.plotSelected = plotSelected; }

    public String getSortBarsBy() { return sortBarsBy; }

    public void setSortBarsBy(String sortBarsBy) { this.sortBarsBy = sortBarsBy; }
}
