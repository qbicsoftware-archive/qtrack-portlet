package com.vaadin.ui;

import com.vaadin.data.HasValue;
import com.vaadin.model.DataRequest;
import com.vaadin.model.DbConnector;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.colorpicker.Color;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;


/**
 * The MainView as proposed by Vaadin (see MainDesign.html for the actual MainView):
 * Deals with:
 *      - setting up the application (connecting to the DB; downloading the data; setting the data for the plots)
 *      - dealing with the listeners
 */
public class MainView extends MainDesign implements View {

    private String userID;  // the user ID for the database connection
    private DbConnector dbConnector; // the connector between Vaadin and MongoDB
    private Calendar cal;   // the calender
    private boolean hasUserModifiedDate = false;    // whether the user has modified the date to plot or not

    public MainView() {

        // set the userID and create a instance of the dbConnector
        this.userID = (String) VaadinSession.getCurrent().getAttribute("userID");
        dbConnector = new DbConnector(userID);

        // sets the user profile picture and the user name to display in the application
        updateUserDataForView();

        // gets the data from the database (currently: last year)
        getDataFromGoogleFit();
        cal = Calendar.getInstance();   // calendar to calculate which dates to use

        // create a new ChartComponent to display the charts
        ChartComponent chartComponent = new ChartComponent();
        contentArea.addComponent(chartComponent);

        // set the default item for the radio button group
        timeRadioButtonGroup.setSelectedItem("Monthly");

        // default options for the customize tab
        SelectedOptions selectedOptions = new SelectedOptions(Color.RED,
                new Color(70,130,180), lastMonth(), getNow(),
                "Circles", "Monthly", "LineChart");

        // hand the data (stored in chartComponent) and the selected options to the line chart
        setDataForLineChart(chartComponent, selectedOptions);

        // add all the listener to the view
        addListenerToView(chartComponent, selectedOptions);
    }


    /**
     * adds all the listeners (mostly for the buttons) to the view
     * @param chartComponent: the line chart to set the data for
     * @param selectedOptions: the object holding all the selected options
     */
    private void addListenerToView(ChartComponent chartComponent, SelectedOptions selectedOptions) {
        // change listener for the radio button group ()
        timeRadioButtonGroup.addValueChangeListener((HasValue.ValueChangeListener<String>) valueChangeEvent -> {

            // check if the user has clicked custom date..
            if (Objects.equals(valueChangeEvent.getValue(), "Custom Date")) {
                // open the settings window to let him select a custom date
                openSettingsWindow(chartComponent, selectedOptions);
            } else {
                // set the selected time and the data
                selectedOptions.setTimeSelected(valueChangeEvent.getValue());
                setDataForLineChart(chartComponent, selectedOptions);
            }
        });

        // Handle the events with an anonymous class
        // https://vaadin.com/docs/-/part/framework/application/application-events.html
        // https://vaadin.com/docs/-/part/framework/application/application-notifications.html
        dashboard.addClickListener(event -> {
            //dashboard.setCaption("dashboard clicked!");
            Notification.show("Dashboard clicked!");
        });

        reports.addClickListener(event -> {
            //reports.setCaption("reports clicked!");
            Notification.show("Reports clicked!");
        });

        settings.addClickListener(event -> {
            // open the settings window
            openSettingsWindow(chartComponent, selectedOptions);
        });

        steps_linechart.addClickListener(event -> {
            selectedOptions.setPlotSelected("LineChart");
            setDataForLineChart(chartComponent, selectedOptions);
        });

        activity_barchart.addClickListener(event -> {
            selectedOptions.setPlotSelected("StackedBarChart");
            setDataForLineChart(chartComponent, selectedOptions);
        });

        logout.addClickListener(event -> {
            // get back to login page
            Page.getCurrent().setLocation( "/" );
            // close the vaadin session
            VaadinSession.getCurrent().close();
        });
    }

    /**
     * sets the data for the line chart based on the selected time option from selectedOptions
     * @param chartComponent: the connector between vaadin and javascript
     * @param selectedOptions: Object holding all the selected options
     */
    private void setDataForLineChart(ChartComponent chartComponent, SelectedOptions selectedOptions) {

        switch (selectedOptions.getTimeSelected()) {
            case "Weekly":
                hasUserModifiedDate = false;
                chartComponent.setData(dbConnector.extractData(lastWeek(), getNow()), selectedOptions.getJSONRepresentation());
                break;
            case "Monthly":
                hasUserModifiedDate = false;
                chartComponent.setData(dbConnector.extractData(lastMonth(), getNow()), selectedOptions.getJSONRepresentation());
                break;
            case "Yearly":
                hasUserModifiedDate = false;
                chartComponent.setData(dbConnector.extractData(lastYear(), getNow()), selectedOptions.getJSONRepresentation());
                break;
            case "Custom Date":
                chartComponent.setData(dbConnector.extractData(selectedOptions.getStartDate(), selectedOptions.getEndDate()),
                        selectedOptions.getJSONRepresentation());
                break;
        }
    }

    /**
     * opens the settings window and updates the selectedOptions object according to the selected options
     * @param chartComponent: the connector between vaadin and javascript
     * @param selectedOptions: Object holding all the selected options
     */
    private void openSettingsWindow(ChartComponent chartComponent, SelectedOptions selectedOptions) {
        // Create a sub-window and set the content
        Window subWindow = new Window("Settings");
        HorizontalLayout subContent = new HorizontalLayout();
        subWindow.setContent(subContent);

        // Put some components in it

        // TODO: refactor the settings window
        // TODO: replace vertical and horizontal layouts with GridLayout

        // first column
        VerticalLayout firstColumn = new VerticalLayout();
        firstColumn.addComponent(new Label("Color selection:"));
        ColorPicker avgStepsColorPicker = new ColorPicker("Average Steps");
        avgStepsColorPicker.setCaption("Average Steps");
        avgStepsColorPicker.setPosition(
                Page.getCurrent().getBrowserWindowWidth() / 2 - 246/2,
                15);
        avgStepsColorPicker.setValue(selectedOptions.getColorForAvgSteps());
        avgStepsColorPicker.addValueChangeListener((HasValue.ValueChangeListener<Color>) valueChangeEvent -> {
            selectedOptions.setColorForAvgSteps(valueChangeEvent.getValue());
            avgStepsColorPicker.setValue(valueChangeEvent.getValue());
        });

        firstColumn.addComponent(avgStepsColorPicker);

        // second column
        VerticalLayout secondColumn = new VerticalLayout();
        ColorPicker userStepsColorPicker = new ColorPicker("Your Steps");
        userStepsColorPicker.setCaption("Your Steps");
        userStepsColorPicker.setPosition(
                Page.getCurrent().getBrowserWindowWidth() / 2 - 246/2,
                15);
        userStepsColorPicker.setValue(selectedOptions.getColorForUserSteps());

        // TODO: check if there is a open listener and a close listener for the color picker; if so then
        // on open:         subWindow.setClosable(false);
        // on close:         subWindow.setClosable(true);
        // OR: use a click listener and a boolean which switches its value everytime the colorpicker gets clicked


        userStepsColorPicker.addValueChangeListener((HasValue.ValueChangeListener<Color>) valueChangeEvent -> {
                selectedOptions.setColorForUserSteps(valueChangeEvent.getValue());
                userStepsColorPicker.setValue(valueChangeEvent.getValue());
        });
        firstColumn.addComponent(userStepsColorPicker);

        // third column:
        VerticalLayout thirdColumn = new VerticalLayout();
        thirdColumn.addComponent(new Label("Time interval selection:"));

        // let the user choose the time interval he wants to plot; default: one month
        DateField startDateField = new DateField();
        LocalDate endDate = LocalDate.now();
        startDateField.setValue(new java.sql.Date(new Date(selectedOptions.getStartDate()).getTime()).toLocalDate());
        startDateField.setCaption("Start date");
        startDateField.addValueChangeListener((HasValue.ValueChangeListener<LocalDate>) valueChangeEvent -> {

            System.out.println("start date:");
            System.out.println(valueChangeEvent.getValue());
            System.out.println(valueChangeEvent.getValue().toEpochDay());
            System.out.println(selectedOptions.getEndDate());

            // date selection is fine
            if (valueChangeEvent.getValue().toEpochDay()*86400000 < selectedOptions.getEndDate()) {
                selectedOptions.setStartDate(valueChangeEvent.getValue().toEpochDay()*86400000);
                hasUserModifiedDate = true;
            // start date is before end date
            } else {
                // set the start date back to the old value
                Date oldDate = new Date(selectedOptions.getStartDate());
                startDateField.setValue(new java.sql.Date(oldDate.getTime()).toLocalDate());
                Notification.show("Please select a date before the end date!", Notification.Type.WARNING_MESSAGE);
            }
        });
        thirdColumn.addComponent(startDateField);

        DateField endDateField = new DateField();
        endDateField.setValue(new java.sql.Date(new Date(selectedOptions.getEndDate()).getTime()).toLocalDate());
        endDateField.setCaption("End date");
        endDateField.addValueChangeListener((HasValue.ValueChangeListener<LocalDate>) valueChangeEvent -> {

            // date selection is fine
            if (valueChangeEvent.getValue().toEpochDay()*86400000 > selectedOptions.getStartDate() &&
                    valueChangeEvent.getValue().toEpochDay()*86400000 < new Date().getTime()) {
                selectedOptions.setEndDate(valueChangeEvent.getValue().toEpochDay()*86400000);
                hasUserModifiedDate = true;
            // end date is before start date
            } else {
                // set the end date back to the old value
                Date oldDate = new Date(selectedOptions.getEndDate());
                endDateField.setValue(new java.sql.Date(oldDate.getTime()).toLocalDate());

                // start date is a later date than end date
                if (valueChangeEvent.getValue().toEpochDay()*86400000 < selectedOptions.getStartDate()) {
                    Notification.show("Please select a date after the start date!",
                            Notification.Type.WARNING_MESSAGE);
                // end date is in the future
                } else if (valueChangeEvent.getValue().toEpochDay()*86400000 > new Date().getTime()) {
                    Notification.show("Please select a date in the past!",
                            Notification.Type.WARNING_MESSAGE);
                // some other things went wrong
                } else {
                    Notification.show("Please select a valid date!",
                            Notification.Type.WARNING_MESSAGE);
                }
            }
        });
        thirdColumn.addComponent(endDateField);


        // fourth column:
        VerticalLayout fourthColumn = new VerticalLayout();
        fourthColumn.addComponent(new Label("Dot type selection:"));

        // select the dot type
        RadioButtonGroup<String> dotTypeRadioButton =
                //new RadioButtonGroup<>("Select Dot Type");
                new RadioButtonGroup<>();
        dotTypeRadioButton.setItems("Circles", "Rectangles", "Triangles");
        dotTypeRadioButton.setSelectedItem("Circles");
        dotTypeRadioButton.addValueChangeListener((HasValue.ValueChangeListener<String>) valueChangeEvent -> {
            selectedOptions.setDotTypeSelection(valueChangeEvent.getValue());
            Notification.show(valueChangeEvent.getValue());
        });
        fourthColumn.addComponent(dotTypeRadioButton);

        VerticalLayout fifthColumn = new VerticalLayout();
        Button confirmSettings = new Button("Ok");
        confirmSettings.addClickListener(event2 -> {

            if (hasUserModifiedDate) {
                timeRadioButtonGroup.setSelectedItem("Custom Date");
                selectedOptions.setTimeSelected("Custom Date");
            }

            setDataForLineChart(chartComponent, selectedOptions);
            subWindow.close();
            System.out.println(selectedOptions.getJSONRepresentation());
        });
        fifthColumn.addComponent(confirmSettings);

        Button cancelSettings = new Button("Cancel");
        cancelSettings.addClickListener(clickEvent -> {
            timeRadioButtonGroup.setSelectedItem(selectedOptions.getTimeSelected());
            subWindow.close();
        });
        VerticalLayout sixthColumn = new VerticalLayout();
        sixthColumn.addComponent(cancelSettings);

        // add the columns to the subwindow
        subContent.addComponent(firstColumn);
        subContent.addComponent(secondColumn);
        subContent.addComponent(thirdColumn);
        subContent.addComponent(fourthColumn);
        subContent.addComponent(fifthColumn);
        subContent.addComponent(sixthColumn);

        // Set window size.
        //TODO: dynamic sizes
        subWindow.setHeight("300px");
        subWindow.setWidth("800px");

        // Center it in the browser window
        subWindow.center();

        // Open it in the UI
        // Add it to the root component
        UI.getCurrent().addWindow(subWindow);
    }

    /**
     * updates the user data to display
     */
    private void updateUserDataForView() {
        image.setSource(new ExternalResource(DbConnector.extractUserProfilePictureFromDatabase(userID)));
        nameLabel.setValue(DbConnector.extractUserRealName(userID));
        nameLabel.setStyleName("name-label");
    }

    /**
     * requests the data from google fit and stores it in the database
     */
    private void getDataFromGoogleFit() {
        DataRequest dataRequest = new DataRequest();

        // get the data for the last 12 months and store them in the database
        for (int month = 0; month < 12; month++) {
            try {

                // TODO:
                dbConnector.storeData(dataRequest.getFitData(month));
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error while downloading Fit Data");
            }
        }
    }

    /**
     * returns the current date in milliseconds
     * @return current date in milliseconds
     */
    private long getNow () {
        return cal.getTimeInMillis();
    }

    /**
     * returns the date seven days ago in milliseconds
     * @return date seven days ago in milliseconds
     */
    private long lastWeek() {
        Calendar lm = (Calendar)cal.clone();
        lm.add(Calendar.DAY_OF_YEAR, -8);
        return lm.getTimeInMillis();
    }

    /**
     * returns the date a month ago in milliseconds
     * @return date a month ago in milliseconds
     */
    private long lastMonth() {
        Calendar lm = (Calendar)cal.clone();
        lm.add(Calendar.MONTH, -1);
        return lm.getTimeInMillis();
    }

    /**
     * returns the date a year ago in milliseconds
     * @return date a year ago in milliseconds
     */
    private long lastYear() {
        Calendar lm = (Calendar)cal.clone();
        lm.add(Calendar.YEAR, -1);
        return lm.getTimeInMillis();
    }

    @Override
    /*
     * the "implementation" of the abstract enter method
     */
    public void enter(ViewChangeListener.ViewChangeEvent viewChangeEvent) {
    }

}
