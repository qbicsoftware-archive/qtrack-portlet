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

    public MainView() {

        // set the userID and create a instance of the dbConnector
        this.userID = (String) VaadinSession.getCurrent().getAttribute("userID");
        dbConnector = new DbConnector(userID);

        // sets the user profile picture and the user name to display in the application
        updateUserDataForView();

        // gets the data from the database (currently: last year)
        getDataFromGoogleFit();
        cal = Calendar.getInstance();   // calendar to calculate which dates to use

        // create a new LineChart to display the charts
        LineChart lineChart = new LineChart();
        contentArea.addComponent(lineChart);

        // set the default item for the radio button group
        timeRadioButtonGroup.setSelectedItem("Monthly");

        // default options for the customize tab
        SelectedOptions selectedOptions = new SelectedOptions(Color.RED,
                new Color(70,130,180), String.valueOf(lastMonth()), String.valueOf(getNow()),
                "Circles", "Monthly");

        // hand the data and the selected options to the line chart
        setDataForLineChart(lineChart, selectedOptions);

        // add all the listener to the view
        addListenerToView(lineChart, selectedOptions);
    }

    /**
     * adds all the listeners (mostly for the buttons) to the view
     * @param lineChart: the line chart to set the data for
     * @param selectedOptions: the object holding all the selected options
     */
    private void addListenerToView(LineChart lineChart, SelectedOptions selectedOptions) {
        // change listener for the radio button group ()
        timeRadioButtonGroup.addValueChangeListener((HasValue.ValueChangeListener<String>) valueChangeEvent -> {
                selectedOptions.setTimeSelected(valueChangeEvent.getValue());
                setDataForLineChart(lineChart, selectedOptions);
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

        customize.addClickListener(event -> {
            // open the settings window
            openSettingsWindow(lineChart, selectedOptions);
        });

        admin.addClickListener(event -> {
            //admin.setCaption("admin clicked!");
            Notification.show("Admin clicked!");
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
     * @param lineChart: the connector between vaadin and javascript
     * @param selectedOptions: Object holding all the selected options
     */
    private void setDataForLineChart(LineChart lineChart, SelectedOptions selectedOptions) {
        switch (selectedOptions.getTimeSelected()) {
            case "Weekly":
                lineChart.setData(dbConnector.extractData(lastWeek(), getNow()), selectedOptions.getJSONRepresentation());
                break;
            case "Monthly":
                lineChart.setData(dbConnector.extractData(lastMonth(), getNow()), selectedOptions.getJSONRepresentation());
                break;
            case "Yearly":
                lineChart.setData(dbConnector.extractData(lastYear(), getNow()), selectedOptions.getJSONRepresentation());
                break;
        }
    }

    /**
     * opens the settings window and updates the selectedOptions object according to the selected options
     * @param lineChart: the connector between vaadin and javascript
     * @param selectedOptions: Object holding all the selected options
     */
    private void openSettingsWindow(LineChart lineChart, SelectedOptions selectedOptions) {
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
        startDateField.setValue(endDate.minusMonths(1));
        startDateField.setCaption("Start date");
        startDateField.addValueChangeListener((HasValue.ValueChangeListener<LocalDate>) valueChangeEvent ->
                selectedOptions.setStartDate(String.valueOf(valueChangeEvent.getValue().toEpochDay()*86400000)));
        thirdColumn.addComponent(startDateField);

        DateField endDateField = new DateField();
        endDateField.setValue(LocalDate.now());
        endDateField.setCaption("End date");
        endDateField.addValueChangeListener((HasValue.ValueChangeListener<LocalDate>) valueChangeEvent -> {
            selectedOptions.setEndDate(String.valueOf(valueChangeEvent.getValue().toEpochDay()*86400000));

            //selectedOptions.setEndDate(valueChangeEvent.getValue().toString());
            //Notification.show(valueChangeEvent.getValue().toString());
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
            setDataForLineChart(lineChart, selectedOptions);
            subWindow.close();
            System.out.println(selectedOptions.getJSONRepresentation());
        });
        fifthColumn.addComponent(confirmSettings);

        Button cancelSettings = new Button("Cancel");
        cancelSettings.addClickListener(clickEvent -> subWindow.close());
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
                dbConnector.storeSteps(dataRequest.getFitData(month));
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
        lm.add(Calendar.DAY_OF_YEAR, -7);
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
