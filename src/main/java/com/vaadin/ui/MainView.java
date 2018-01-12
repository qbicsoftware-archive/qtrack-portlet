package com.vaadin.ui;

import com.vaadin.data.HasValue;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.model.DataRequest;
import com.vaadin.model.DbConnector;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.*;
import com.vaadin.shared.ui.colorpicker.Color;
import org.bson.Document;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;


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
                "Circles", "Monthly", "LineChart", "Date");

        // hand the data (stored in chartComponent) and the selected options to the line chart
        setDataForCharts(chartComponent, selectedOptions);

        // add all the listener to the view
        addListenerToView(chartComponent, selectedOptions);

        // TODO: testing
        List<Document> menuItems = dbConnector.extractMenuItemsCollection();

        System.out.println("menu items:");
        System.out.println(menuItems);

        List<Button> buttonList = new ArrayList<>();
        for (Document b : menuItems) {
            System.out.println(b);
            System.out.println(b.get("button_caption"));

            // create the new button
            Button tempButton = new Button(b.get("button_caption").toString());
            tempButton.setIcon(VaadinIcons.valueOf(b.get("button_icon").toString()));
            tempButton.setStyleName("borderless");
            tempButton.setWidth("100%");

            // add it on top to the menu
            menu.addComponent(tempButton, 0);
            buttonList.add(tempButton);

            tempButton.addClickListener(event -> {
                selectedOptions.setPlotSelected("LineChart");
                viewTitle.setValue("sample data");
                setDataForCharts(chartComponent, selectedOptions);
            });
        }

        // List of planets
/*        ComboBox<String> select = new ComboBox<>("Select or Add a Planet");
        select.setItems("test213", "lalala", "hehe", "xyz");
        menu.addComponent(select);

        Button testButton = new Button("test");
        testButton.setIcon(VaadinIcons.AMBULANCE);
        testButton.setStyleName("borderless");
        testButton.setWidth("100%");
        menu.addComponent(testButton);*/

    }

    /**
     * prepares the user data by converting the unix time to a date and flattening the activities array
     * @param userDataAsJsonArray: user data stored in a json array:
     *                           [{"startDateInUTC":1510185600000,"averageSteps":2597.659574468085,"activities":
     *                           {"still":23249677,"walking":2153308,"in_vehicle":1745901,"sleeping":23000271},
     *                           "steps":489, "stdErrorOfMean" : 1008.1493738529028}, ...]
     * @return JSONArray [{"date":09-11-2017,"average steps":2597.659574468085,"still[ms]":23249677,
     * "walking[ms]":2153308,"in_vehicle[ms]":1745901,"sleeping[ms]":23000271,"steps":489,
     * "stdErrorOfMean" : 1008.1493738529028}, ...]
     */
    private JSONArray prepareUserData(JSONArray userDataAsJsonArray) {

        // iterate over the json array
        for (int i=0; i < userDataAsJsonArray.length(); i++){

            // get the current json object
            JSONObject currentJsonObject = userDataAsJsonArray.getJSONObject(i);

            // convert the date from unix time to a european data format (1510185600000 -> 09-11-2017) and put it back
            // to the json object
            Date date = new Date(currentJsonObject.getLong("startDateInUTC"));
            SimpleDateFormat sm = new SimpleDateFormat("dd-MM-yyyy");
            String strDate = sm.format(date);
            currentJsonObject.remove("startDateInUTC");
            currentJsonObject.put("date", strDate);

            // flatten the activities array: "activities": {"still":23249677,"walking":2153308,"in_vehicle":1745901,
            // "sleeping":23000271} changes to "still":23249677, "walking":2153308,"in_vehicle":1745901,"
            // sleeping":23000271
            JSONObject activities = currentJsonObject.getJSONObject("activities");
            for(int j = 0; j < activities.names().length(); j++){
                String activityName = activities.names().getString(j);
                long activityDuration = activities.getLong(activityName);
                currentJsonObject.put(activityName + "[ms]", activityDuration);
            }
            currentJsonObject.remove("activities");
            userDataAsJsonArray.put(i, currentJsonObject);
        }
        return userDataAsJsonArray;
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
            if (Objects.equals(valueChangeEvent.getValue(), "Custom Date") && !hasUserModifiedDate) {
                // open the settings window to let him select a custom date
                openSettingsWindow(chartComponent, selectedOptions);
            } else {
                // set the selected time and the data
                selectedOptions.setTimeSelected(valueChangeEvent.getValue());
                setDataForCharts(chartComponent, selectedOptions);
            }
        });

        // Handle the events with an anonymous class
        // https://vaadin.com/docs/-/part/framework/application/application-events.html
        // https://vaadin.com/docs/-/part/framework/application/application-notifications.html
        settings.addClickListener(event -> {
            // open the settings window
            openSettingsWindow(chartComponent, selectedOptions);
        });

        steps_linechart.addClickListener(event -> {
            selectedOptions.setPlotSelected("LineChart");
            viewTitle.setValue("Steps data");
            setDataForCharts(chartComponent, selectedOptions);
        });

        activity_barchart.addClickListener(event -> {
            selectedOptions.setPlotSelected("StackedBarChart");
            viewTitle.setValue("Activity data");
            setDataForCharts(chartComponent, selectedOptions);
        });

        // add a file downloader to the download_data button
        StreamResource resource = new StreamResource((StreamResource.StreamSource) () -> {
            try {
                // get the user data
                String userData = chartComponent.getData();

                System.out.println(userData);

                // convert it to json
                JSONArray userDataAsJsonArray = new JSONArray(userData);

                // prepare the data by converting the date from unix time to the local date any by flattening the
                // activities array
                userDataAsJsonArray = prepareUserData(userDataAsJsonArray);

                // convert it to csv
                String userDataAsCSV = CDL.toString(userDataAsJsonArray);

                return new ByteArrayInputStream(userDataAsCSV.getBytes());

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, "Data.csv");
        new FileDownloader(resource).extend(download_data);

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
    private void setDataForCharts(ChartComponent chartComponent, SelectedOptions selectedOptions) {

        switch (selectedOptions.getTimeSelected()) {
            case "Weekly":
                hasUserModifiedDate = false;
                selectedOptions.setStartDate(lastWeek()+86400000);
                selectedOptions.setEndDate(getNow()-86400000);
                chartComponent.setData(dbConnector.extractData(lastWeek(), getNow()),
                        selectedOptions.getJSONRepresentation());
                break;
            case "Monthly":
                hasUserModifiedDate = false;
                selectedOptions.setStartDate(lastMonth());
                selectedOptions.setEndDate(getNow()-86400000);
                chartComponent.setData(dbConnector.extractData(lastMonth(), getNow()),
                        selectedOptions.getJSONRepresentation());
                break;
            case "Yearly":
                hasUserModifiedDate = false;
                selectedOptions.setStartDate(lastYear());
                selectedOptions.setEndDate(getNow()-86400000);
                chartComponent.setData(dbConnector.extractData(lastYear()-86400000, getNow()),
                        selectedOptions.getJSONRepresentation());
                break;
            case "Custom Date":
                chartComponent.setData(dbConnector.extractData(selectedOptions.getStartDate(),
                        selectedOptions.getEndDate()), selectedOptions.getJSONRepresentation());
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

        if (selectedOptions.getPlotSelected().equals("LineChart")) {
            firstColumn.addComponent(new Label("Color selection:"));
            firstColumn.addComponent(avgStepsColorPicker);
            firstColumn.addComponent(userStepsColorPicker);
        }

        // third column:
        VerticalLayout thirdColumn = new VerticalLayout();
        thirdColumn.addComponent(new Label("Time interval selection:"));

        // let the user choose the time interval he wants to plot; default: one month
        DateField startDateField = new DateField();
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
        //fourthColumn.addComponent(new Label("Dot type selection:"));

        // select the dot type
        /*RadioButtonGroup<String> dotTypeRadioButton =
                //new RadioButtonGroup<>("Select Dot Type");
                new RadioButtonGroup<>();
        dotTypeRadioButton.setItems("Circles", "Rectangles", "Triangles");
        dotTypeRadioButton.setSelectedItem("Circles");
        dotTypeRadioButton.addValueChangeListener((HasValue.ValueChangeListener<String>) valueChangeEvent -> {
            selectedOptions.setDotTypeSelection(valueChangeEvent.getValue());
            Notification.show(valueChangeEvent.getValue());
        });*/
        //fourthColumn.addComponent(dotTypeRadioButton);    TODO

        VerticalLayout fifthColumn = new VerticalLayout();
        Button confirmSettings = new Button("Ok");
        confirmSettings.addClickListener(event2 -> {

            if (hasUserModifiedDate) {
                timeRadioButtonGroup.setSelectedItem("Custom Date");
                selectedOptions.setTimeSelected("Custom Date");
            }

            setDataForCharts(chartComponent, selectedOptions);
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
