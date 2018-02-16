package com.vaadin.model;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.fitness.Fitness;
import com.google.api.services.fitness.model.*;
import com.vaadin.server.VaadinSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Requests the data from Google Fit
 */
public class DataRequest extends AuthRequest{

    /* Google User ID for the current user*/
    public DataRequest() {
        myCredential = (Credential) VaadinSession.getCurrent().getAttribute("sessionCredential");
    }

    /**
     * returns the google fit data of the last year aggregated by each day
     * @param monthsAgo: integer noting from which month within a year we want to have the data; e.g. 3 if we want to
     *                 receive the data for one month 3 months ago
     * @return string with the google fit data from the month, which is monthsAgo months ago :)
     */
    public String getFitData(int monthsAgo) throws IOException {

        Fitness fit = new Fitness.Builder(HTTP_TRANSPORT, JSON_FACTORY, myCredential)
                .setApplicationName("TrackFit").build();

        // create the aggregate request object with the start and end date set
        AggregateRequest aggRequest = setupTimeForAggregateRequest(monthsAgo);

        // we want data to be aggregated by each day
        BucketByTime bucketByTime = new BucketByTime();
        bucketByTime.setDurationMillis(86400000L); // 24h
        aggRequest.setBucketByTime(bucketByTime);

        // specify the data type we want to request (see https://developers.google.com/fit/rest/v1/data-types)
        AggregateBy aggregateByStepsCount = new AggregateBy();
        aggregateByStepsCount.setDataTypeName("com.google.step_count.delta");

        // Continuous time interval of a single activity.
        AggregateBy aggregateByActivityData = new AggregateBy();
        aggregateByActivityData.setDataSourceId("derived:com.google.activity.segment:com.google.android.gms:" +
                "merge_activity_segments");
        aggregateByActivityData.setDataTypeName("com.google.activity.segment");

        // add the aggregateBy to the list
        ArrayList<AggregateBy> listOfAggregatesWithActivity = new ArrayList<>();
        listOfAggregatesWithActivity.add(aggregateByStepsCount);
        listOfAggregatesWithActivity.add(aggregateByActivityData);

        // set the list of aggregateBys for the request
        aggRequest.setAggregateBy(listOfAggregatesWithActivity);

        AggregateResponse aggResponse;

        try {
            // get the response for the request
            aggResponse = fit.users().dataset()
                    .aggregate("me", aggRequest).execute();

        // there is no activity data
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {

            // create a new list of aggregate bys without the activity aggregateBy
            ArrayList<AggregateBy> listOfAggregatesWithoutActivity = new ArrayList<>();
            listOfAggregatesWithoutActivity.add(aggregateByStepsCount);
            aggRequest.setAggregateBy(listOfAggregatesWithoutActivity);

            // get the response for the new request
            aggResponse = fit.users().dataset()
                    .aggregate("me", aggRequest).execute();
        }

        //System.out.println("aggResponse");
        //System.out.println(aggResponse.toPrettyString());

        return aggResponse.toString();
    }

    /**
     * sets the start time millis and the end time millis for the aggregate request according to monthsAgo
     * @param monthsAgo: integer noting from which month within a year we want to have the data; e.g. 3 if we want to
     *                 receive the data for one month 3 months ago
     * @return AggregateRequest with StartTimeMillis and EndTimeMillis set
     */
    private AggregateRequest setupTimeForAggregateRequest(int monthsAgo) {

        // get a calendar instance with Coordinated Universal Time
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        // set hours, minutes, seconds and milliseconds
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // go monthsAgo back
        cal.add(Calendar.MONTH, -monthsAgo);

        // get the end time of the time interval
        long endTime = cal.getTimeInMillis();

        // get the month before the end date
        cal.add(Calendar.MONTH, -1);

        // get the start time of the time interval
        long startTime = cal.getTimeInMillis();

        // create the request
        AggregateRequest aggRequest = new AggregateRequest();

        // set start- and end time for the request
        aggRequest.setStartTimeMillis(startTime);
        aggRequest.setEndTimeMillis(endTime);
        return aggRequest;
    }
}