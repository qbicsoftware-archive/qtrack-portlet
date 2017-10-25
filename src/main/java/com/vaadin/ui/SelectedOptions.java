package com.vaadin.ui;

import com.vaadin.shared.ui.colorpicker.Color;

/**
 * object holding all the selected options from the user
 */
class SelectedOptions {

    private Color colorForAvgSteps;
    private Color colorForUserSteps;
    private String startDate;
    private String endDate;
    private String dotTypeSelection;
    private String timeSelected;

    /**
     * constructor
     * @param colorForAvgSteps which color the user selected for the avg steps
     * @param colorForUserSteps which color the user selected for the user steps
     * @param startDate which start date the user has selected
     * @param endDate which end date the user has selected
     * @param dotTypeSelection which dot types the user has selected
     * @param timeSelected which time the user has selected
     */
    SelectedOptions(Color colorForAvgSteps, Color colorForUserSteps, String startDate, String endDate,
                    String dotTypeSelection, String timeSelected) {
        this.colorForAvgSteps = colorForAvgSteps;
        this.colorForUserSteps = colorForUserSteps;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dotTypeSelection = dotTypeSelection;
        this.timeSelected = timeSelected;
    }
    
    /**
     * returns the JSON representation
     * @return the JSON representation of the object
     */
    String getJSONRepresentation() {

        return "{\"colorForAvgSteps\":\"" + getColorForAvgSteps().getCSS() + "\"," +
                "\"colorForUserSteps\":\"" + getColorForUserSteps().getCSS() + "\"," +
                "\"startDate\":\"" + getStartDate() + "\"," +
                "\"endDate\":\"" + getEndDate() + "\"," +
                "\"timeSelected\":\"" + getTimeSelected() + "\"," +
                "\"dotTypeSelection\":\"" + getDotTypeSelection() + "\"" +
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

    String getStartDate() {
        return startDate;
    }

    void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    String getEndDate() {
        return endDate;
    }

    void setEndDate(String endDate) {
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
}
