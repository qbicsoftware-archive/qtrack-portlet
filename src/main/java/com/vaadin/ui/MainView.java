package com.vaadin.ui;

import com.google.api.client.auth.oauth2.Credential;
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
 * Created by caspar on 08.06.17.
 */
public class MainView extends MainDesign implements View {
    private String userID;
    Credential myCredential;
    private DbConnector dbConnector;
    private Calendar cal;

    public MainView() {
        this.userID = (String) VaadinSession.getCurrent().getAttribute("userID");
        dbConnector = new DbConnector(userID);
        updateUserProfile();
        downloadData();
        cal = Calendar.getInstance();

        LineChart lineChart = new LineChart();

        //System.out.println(data from the lastMonth);
        //System.out.println(lastMonth() + "   " + getNow());
        //System.out.println(dbConnector.extractData(lastMonth(), getNow()));

        // TODO: get first data session from the user
        // TODO: change to user selection; default: lastMonth(), getNow()

        contentArea.addComponent(lineChart);
        timeRadioButtonGroup.setSelectedItem("Monthly");

        SelectedOptions selectedOptions = new SelectedOptions(Color.RED.getCSS(),
                new Color(70,130,180).getCSS(), String.valueOf(lastMonth()), String.valueOf(getNow()),
                "Circles", "Monthly");

        setDataForLineChart(lineChart, selectedOptions);


        // listener for buttons:
        // TODO:

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
            openSettingsWindow(selectedOptions);
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
     * @param lineChart: the line chart to plot
     * @param selectedOptions: Object holding all the selected options
     */
    private void setDataForLineChart(LineChart lineChart, SelectedOptions selectedOptions) {
        switch (selectedOptions.getTimeSelected()) {
            case "Weekly":
                lineChart.setData(dbConnector.extractData(lastWeek(), getNow()));
                break;
            case "Monthly":
                lineChart.setData(dbConnector.extractData(lastMonth(), getNow()));
                break;
            case "Yearly":
                lineChart.setData(dbConnector.extractData(lastYear(), getNow()));
                break;
        }
    }

    /**
     * opens the settings window and updates the selectedOptions object according to the selected options
     * @param selectedOptions: Object holding all the selected options
     */
    private void openSettingsWindow(SelectedOptions selectedOptions) {
        // Create a sub-window and set the content
        Window subWindow = new Window("Settings");
        HorizontalLayout subContent = new HorizontalLayout();
        subWindow.setContent(subContent);

        // Put some components in it

        // TODO: replace vertical and horizontal layouts with GridLayout

        // first column
        VerticalLayout firstColumn = new VerticalLayout();
        firstColumn.addComponent(new Label("Color selection:"));
        ColorPicker avgStepsColorPicker = new ColorPicker("Average Steps");
        avgStepsColorPicker.setCaption("Average Steps");
        avgStepsColorPicker.setPosition(
                Page.getCurrent().getBrowserWindowWidth() / 2 - 246/2,
                15);
        avgStepsColorPicker.addValueChangeListener((HasValue.ValueChangeListener<Color>) valueChangeEvent -> {
            selectedOptions.setColorForAvgSteps(valueChangeEvent.getValue().getCSS());
        });

        avgStepsColorPicker.setValue(Color.RED);
        firstColumn.addComponent(avgStepsColorPicker);

        // second column
        VerticalLayout secondColumn = new VerticalLayout();
        ColorPicker userStepsColorPicker = new ColorPicker("Your Steps");
        userStepsColorPicker.setCaption("Your Steps");
        userStepsColorPicker.setPosition(
                Page.getCurrent().getBrowserWindowWidth() / 2 - 246/2,
                15);
        userStepsColorPicker.setValue(new Color(70,130,180));
        userStepsColorPicker.addValueChangeListener((HasValue.ValueChangeListener<Color>) valueChangeEvent -> {
            selectedOptions.setColorForUserSteps(valueChangeEvent.getValue().getCSS());
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
        startDateField.addValueChangeListener((HasValue.ValueChangeListener<LocalDate>) valueChangeEvent -> {
            selectedOptions.setStartDate(String.valueOf(valueChangeEvent.getValue().toEpochDay()*86400000));
        });
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
            // TODO: call redraw event
            //subWindow.close();
            System.out.println(selectedOptions.getJSONRepresentation());
        });
        fifthColumn.addComponent(confirmSettings);

        Button cancelSettings = new Button("Cancel");
        cancelSettings.addClickListener(clickEvent -> {
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

    private void updateUserProfile() {
        //userLabel.setValue("User ID: " + userID);
        image.setSource(new ExternalResource(DbConnector.extractUserPicture(userID)));
        nameLabel.setValue(DbConnector.extractUserRealName(userID));
        nameLabel.setStyleName("name-label");
    }

    private void downloadData() {
        DataRequest dataRequest = new DataRequest();

        // TODO: add parameter for dataRequest.getFitData so that it's called multiple times (12 for a year)

        for (int month = 0; month < 12; month++) {
            try {
                dbConnector.storeSteps(dataRequest.getFitData(month));
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error while downloading Fit Data");
            }
        }
    }

    private long getNow () {

        return cal.getTimeInMillis();
    }

    private long lastWeek() {
        Calendar lm = (Calendar)cal.clone();
        lm.add(Calendar.DAY_OF_YEAR, -7);
        return lm.getTimeInMillis();
    }

    private long lastMonth() {
        Calendar lm = (Calendar)cal.clone();
        lm.add(Calendar.MONTH, -1);
        return lm.getTimeInMillis();
    }

    private long lastYear() {
        Calendar lm = (Calendar)cal.clone();
        lm.add(Calendar.YEAR, -1);
        return lm.getTimeInMillis();
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent viewChangeEvent) {

    }

}
