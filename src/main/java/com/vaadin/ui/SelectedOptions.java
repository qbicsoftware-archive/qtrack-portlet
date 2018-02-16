package com.vaadin.ui;

import com.vaadin.shared.ui.colorpicker.Color;

/**
 * object holding all the selected options from the user; object is then used for passing the selected options in json
 * format to javascript
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
    private String timeFormat;

    /**
     * constructor
     * @param colorForAvgSteps which color the user selected for the avg steps for the steps line chart
     * @param colorForUserSteps which color the user selected for the user steps for the steps line chart
     * @param startDate which start date the user has selected
     * @param endDate which end date the user has selected
     * @param dotTypeSelection which dot types the user has selected; TODO: currently not used!
     * @param timeSelected which time the user has selected: one out of ["Weekly", "Monthly", "Yearly", "Custom"]
     * @param plotSelected what kind of plot is selected: one out of ["LineChart", "StackedBarChart", "CalendarChart"]
     * @param sortBarsBy whether to sort the bars by date, or in ascending or in descending order; TODO: currently not
     *                   used
     */
    SelectedOptions(Color colorForAvgSteps, Color colorForUserSteps, long startDate, long endDate,
                    String dotTypeSelection, String timeSelected, String plotSelected, String sortBarsBy,
                    String timeFormat) {
        this.colorForAvgSteps = colorForAvgSteps;
        this.colorForUserSteps = colorForUserSteps;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dotTypeSelection = dotTypeSelection;
        this.timeSelected = timeSelected;
        this.plotSelected = plotSelected;
        this.sortBarsBy = sortBarsBy;
        this.timeFormat = timeFormat;
    }
    
    /**
     * returns the JSON representation of the fields of the object with key-value pairs:
     * {"key1": value1, "key2": value2, ..}
     * @return string in json format
     */
    String getJSONRepresentation() {

        return "{\"colorForAvgSteps\":\"" + getColorForAvgSteps().getCSS() + "\"," +
                "\"colorForUserSteps\":\"" + getColorForUserSteps().getCSS() + "\"," +
                "\"startDate\":\"" + getStartDate() + "\"," +
                "\"endDate\":\"" + getEndDate() + "\"," +
                "\"timeSelected\":\"" + getTimeSelected() + "\"," +
                "\"dotTypeSelection\":\"" + getDotTypeSelection() + "\"," +
                "\"plotSelected\":\"" + getPlotSelected() + "\"," +
                "\"sortBarsBy\":\"" + getSortBarsBy() + "\"," +
                "\"timeFormat\":\"" + getTimeFormat() + "\"" +
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

    String getSortBarsBy() { return sortBarsBy; }

    void setSortBarsBy(String sortBarsBy) { this.sortBarsBy = sortBarsBy; }

    String getTimeFormat() { return timeFormat; }

    void setTimeFormat(String timeFormat) { this.timeFormat = timeFormat; }
}
