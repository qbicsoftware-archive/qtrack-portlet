package com.vaadin.ui;

public class SelectedOptions {

    private String colorForAvgSteps;
    private String colorForUserSteps;
    private String startDate;
    private String endDate;
    private String dotTypeSelection;
    private String timeSelected;

    public SelectedOptions(String colorForAvgSteps, String colorForUserSteps, String startDate, String endDate,
                           String dotTypeSelection, String timeSelected) {
        this.colorForAvgSteps = colorForAvgSteps;
        this.colorForUserSteps = colorForUserSteps;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dotTypeSelection = dotTypeSelection;
        this.timeSelected = timeSelected;

    }

    public String getTimeSelected() {
        return timeSelected;
    }

    public void setTimeSelected(String timeSelected) {
        this.timeSelected = timeSelected;
    }

    public String getColorForAvgSteps() {
        return colorForAvgSteps;
    }

    public void setColorForAvgSteps(String colorForAvgSteps) {
        this.colorForAvgSteps = colorForAvgSteps;
    }

    public String getColorForUserSteps() {
        return colorForUserSteps;
    }

    public void setColorForUserSteps(String colorForUserSteps) {
        this.colorForUserSteps = colorForUserSteps;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getDotTypeSelection() {
        return dotTypeSelection;
    }

    public void setDotTypeSelection(String dotTypeSelection) {
        this.dotTypeSelection = dotTypeSelection;
    }

    public String getJSONRepresentation() {

        return "{\"colorForAvgSteps\":\"" + getColorForAvgSteps() + "\"," +
                "\"colorForUserSteps\":\"" + getColorForUserSteps() + "\"," +
                "\"startDate\":\"" + getStartDate() + "\"," +
                "\"endDate\":\"" + getEndDate() + "\"," +
                "\"timeSelected\":\"" + getTimeSelected() + "\"," +
                "\"dotTypeSelection\":\"" + getDotTypeSelection() + "\"" +
                "}";

    }
}
