package com.vaadin.model;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.fitness.Fitness;
import com.google.api.services.fitness.model.*;
import com.vaadin.server.VaadinSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Requests the data from google fit
 */
public class DataRequest extends AuthRequest{

    /* Google User ID for the current user*/
    public DataRequest() {
        myCredential = (Credential) VaadinSession.getCurrent().getAttribute("sessionCredential");
    }

    /**
     * returns the google fit data of the last year aggregated by each day
     * @param monthsAgo: integer noting from which month within a year we want to have the data; e.g. 3 if we want to
     *                 receive the data from the data 3 months ago
     * @return string with the google fit data
     */
    public String getFitData(int monthsAgo) throws IOException {

        Fitness fit = new Fitness.Builder(HTTP_TRANSPORT, JSON_FACTORY, myCredential)
                .setApplicationName("TrackFit").build();

        // Setting a start and end date for the request.
        Calendar cal = Calendar.getInstance();
        long now = cal.getTimeInMillis();
        cal.setTimeInMillis(now - now  % (24 * 60 * 60 * 1000)); // Find the last full day
        cal.add(Calendar.MONTH, -monthsAgo);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, -1);
        long startTime = cal.getTimeInMillis();

        // create the request
        AggregateRequest aggRequest = new AggregateRequest();

        // set start- and end time for the request
        aggRequest.setStartTimeMillis(startTime);
        aggRequest.setEndTimeMillis(endTime);

        // we want data to be aggregated by each day
        BucketByTime bucketByTime = new BucketByTime();
        bucketByTime.setDurationMillis(86400000L); // 24h
        aggRequest.setBucketByTime(bucketByTime);

        // specify the data type we want to request (see https://developers.google.com/fit/rest/v1/data-types)
        AggregateBy aggregateBy1 = new AggregateBy();
        aggregateBy1.setDataTypeName("com.google.step_count.delta");

        // TODO: get sleeping data; activities are be stored in ..
/*
        com.google.activity.sample
        com.google.activity.segment
        com.google.activity.summary
*/
        // see https://developers.google.com/fit/rest/v1/data-types#public_data_types
        // and https://developers.google.com/fit/rest/v1/reference/activity-types
        // activity     activity id
        /*  Sleeping 	                 72
            Light sleep 	            109
            Deep sleep 	                110
            REM sleep 	                111
            Awake (during sleep cycle) 	112
        */


        // Instantaneous sample of the current activity.
        //AggregateBy aggregateBy2 = new AggregateBy();
        //aggregateBy2.setDataTypeName("com.google.activity.sample");       // TODO: doesnt work this way

        // Continuous time interval of a single activity.
        AggregateBy aggregateBy3 = new AggregateBy();
        aggregateBy3.setDataTypeName("com.google.activity.segment");

        // Total time and number of segments in a particular activity for a time interval.
        AggregateBy aggregateBy4 = new AggregateBy();
        aggregateBy4.setDataSourceId("derived:com.google.:com.google.android.gms:aggregated");     // TODO
        aggregateBy4.setDataTypeName("com.google.activity.summary");        // TODO: doesnt work

/*      error message:
            com.google.api.client.googleapis.json.GoogleJsonResponseException: 400 Bad Request
            {
                "code" : 400,
                    "errors" : [ {
                "domain" : "global",
                        "message" : "no default datasource found for: com.google.activity.summary",
                        "reason" : "invalidArgument"
            } ],
                "message" : "no default datasource found for: com.google.activity.summary"
            }
        */

        // add the aggregateBy to the list
        ArrayList<AggregateBy> list = new ArrayList<>();
        list.add(aggregateBy1);

        // TODO: testing
        //list.add(aggregateBy2);
        list.add(aggregateBy3);
        //list.add(aggregateBy4);

        // set the list of aggregateBys for the request
        aggRequest.setAggregateBy(list);

        System.out.println("aggRequest:");
        System.out.println(aggRequest.toPrettyString());

        // get the response for the request
        AggregateResponse aggResponse = fit.users().dataset()
                .aggregate("me", aggRequest).execute();

        System.out.println("aggResponse");
        System.out.println(aggResponse.toPrettyString());

        return aggResponse.toString();
    }
}