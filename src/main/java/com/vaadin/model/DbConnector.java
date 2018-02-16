package com.vaadin.model;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.*;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.util.JSON;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;

/**
 * Connects the vaadin session and the MongoDB
 */
public class DbConnector extends MongoClient {

    private MongoDatabase db;                               // the Mongo database
    private static MongoCollection<Document> userColl;      // collection storing the users
    private MongoCollection<Document> stepColl;             // collection storing the steps
    private MongoCollection<Document> daysColl;             // collection storing the days
    private MongoCollection<Document> activityColl;         // collection storing the activities and their duration
    private String sessionUserID;                           // the user session id (should be the same as the google
                                                            // account id)
    // see https://developers.google.com/fit/rest/v1/reference/activity-types for a list of activity types
    // maps the integer to the corresponding activity
    private Map<Integer, String> activityTypesValuesMapper = new HashMap<Integer, String>() {
        {
            // TODO: to extract more activities from google fit simply add them to this hash map
            put(0, "in_vehicle");
            put(1, "on_bicycle");
            put(2, "on_foot");
            put(8, "running");
            put(3, "still");
            //put(5, "tilting");
            //put(4, "unknown");
            put(7, "walking");
            put(72, "sleeping");
    /*        put(109, "light_sleep");
            put(110, "deep_sleep");
            put(111, "REM_sleep");
            put(112, "awake_during_sleep_cycle");*/
        }
    };

    /**
     * Accesses DB, creates an Instance if it does not exist yet.
     * @param sessionUserID: the user session id
     */
    public DbConnector(String sessionUserID) {
        // get the database; create it if it doesn't exist yet
        this.db = getDatabase("trackFit");
        this.sessionUserID = sessionUserID;

        // check if the user collection exists
        if (!this.db.listCollectionNames().into(new ArrayList<>()).contains("steps")) {
            // create the steps collection
            stepColl = db.getCollection("steps");

            // create the indexes for the collections for faster querying
            stepColl.createIndex(Indexes.ascending("startDateInUTC", "user"));
            stepColl.createIndex(Indexes.ascending("startDateInUTC", "user", "endDateInUTC"));
        }

        // check if the activities collection exists
        if (!this.db.listCollectionNames().into(new ArrayList<>()).contains("activities")) {
            // create the activities collection
            activityColl = db.getCollection("activities");

            // create the indexes for the collections for faster querying
            activityColl.createIndex(Indexes.ascending("dateInUTC", "user"));
        }

        // check if the days collection exists
        if (!this.db.listCollectionNames().into(new ArrayList<>()).contains("days")) {
            // create the days collection
            daysColl = db.getCollection("days");

            // create the indexes for the collections for faster querying
            daysColl.createIndex(Indexes.ascending("startDateInUTC", "user"));
        }

        // get the different collections
        userColl = db.getCollection("users");
        stepColl = db.getCollection("steps");
        daysColl = db.getCollection("days");
        activityColl = db.getCollection("activities");

    }

    /**
     * this function stores a new user in the database. If the user is already in the database his fields (name, email,
     * profile picture) are getting updated
     * @param googleUserData: the user data provided from google
     */
    void storeUser(String googleUserData) {

        // parse the json formatted string to a mongodb document
        Document userDoc = Document.parse(googleUserData);

        // use the google account id as unique id for the database
        Object user_id = userDoc.get("id");
        userDoc.append("_id", user_id);
        userDoc.remove("id");

        // update all fields (name, email, profile pic etc. ), if the user is already in the database
        // if he's not in the database: insert him
        userColl.replaceOne(eq("_id", user_id), userDoc, new UpdateOptions().upsert(true));
    }

    /**
     * This function stores the steps in the database. They are assigned to the user with the session id.
     * @param stepData: string representing the steps
     */
    public void storeData(String stepData) {

        // Create new Document parsed from Google JSON data
        Document stepDoc = Document.parse(stepData);

        // Split Document into Array
        @SuppressWarnings("unchecked")
        ArrayList<Document> dayList = (ArrayList<Document>)stepDoc.get("bucket");

        // grab the step and activity data for each day
        dayList.forEach(stepsDocument -> {

            // get the date
            long dateInUTC = Long.valueOf(stepsDocument.get("startTimeMillis").toString());

            // variables for holding the step data and the activity data
            int steps = -1;
            Map<String, Integer> activities = new HashMap<>();

            // Assign steps to currently logged in User
            stepsDocument.append("user", sessionUserID);

            // get the data set from the document and store it in a array list of documents
            @SuppressWarnings("unchecked")
            ArrayList<Document> dataSetList  =  (ArrayList<Document>) stepsDocument.get("dataset");

            // iterate over each document representing either step or activity data from google
            for (Document documentInDatasets : dataSetList) {

                // get the actual data from the document
                @SuppressWarnings("unchecked")
                ArrayList<Document> dataInDataSet = (ArrayList<Document>) documentInDatasets.get("point");

                // check if we have any data at all
                if (dataInDataSet.size() > 0 ) {

                    // iterate over the data entries (for step data we always have one; for activity data we have
                    // one for each activity type (walking, sleeping, still, etc.))
                    for (Document dataEntry : dataInDataSet) {

                        // we have step data
                        if (dataEntry.get("dataTypeName").equals("com.google.step_count.delta")) {

                            // we only have one step entry, so simply setting the variable steps is fine..
                            @SuppressWarnings("unchecked")
                            ArrayList<Document> valueList = (ArrayList<Document>)(dataEntry.get("value"));
                            steps =  valueList.get(0).getInteger("intVal");

                        // we have activity data
                        } else {

                            // get the activities for the activity entry
                            // we have three documents per value List
                            @SuppressWarnings("unchecked")
                            ArrayList<Document> valueList = (ArrayList<Document>)(dataEntry.get("value"));

                            // check if the activity is in the activityTypesValueMapper
                            if (activityTypesValuesMapper.get(valueList.get(0).getInteger("intVal")) != null) {

                                String activity = activityTypesValuesMapper.get(valueList.get(0).getInteger("intVal"));
                                int duration = valueList.get(1).getInteger("intVal");

                                // put the activities into the activities object
                                // we already have a activity entry for the current activity..
                                if (activities.get(activityTypesValuesMapper.get(activity)) != null) {
                                    int activityDuration = activities.get(activity);
                                    activities.put(activity, activityDuration + duration);    // update the old entry
                                    // we don't have a activity entry for the current activity
                                } else {
                                    activities.put(activity, duration);
                                }
                            }
                        }
                    }
                }
            }

            // TODO: if you don't want random data
            boolean generateRandomData = true;
            if (generateRandomData) {

                // we don't have any steps data ..
                if (steps < 0) {
                    // .. so we need to create some random data
                    steps = createRandomData(250, 5000);
                }
                // we don't have any activity data ..
                if (activities.size() < 1) {

                    // .. so we need to create some random activity data
                    int nanoSecondsToHours = 1000 * 60 * 60;        // activity duration is stored in nano seconds

                    // 6-8h of sleeping
                    activities.put("sleeping", createRandomData(nanoSecondsToHours * 6, nanoSecondsToHours * 8));
                    // 10mins to 2 hours of walking
                    activities.put("walking", createRandomData(nanoSecondsToHours / 6, nanoSecondsToHours * 2));
                    // 2-8h of still activity
                    activities.put("still", createRandomData(nanoSecondsToHours * 2, nanoSecondsToHours * 8));
                    // 10mins to 2 hours of vehicle
                    activities.put("in_vehicle", createRandomData(nanoSecondsToHours / 6, nanoSecondsToHours * 2));
                }
            }

            // Convert timeInMillis to long
            stepsDocument.put("steps", steps);
            stepsDocument.put("startDateInUTC", dateInUTC);
            stepsDocument.put("endDateInUTC", Long.valueOf(stepsDocument.get("endTimeMillis").toString()));

            // remove unnecessary fields in the document
            stepsDocument.remove("dataset");
            stepsDocument.remove("startTimeMillis");
            stepsDocument.remove("endTimeMillis");

            // create the document for the activities
            Document activityDoc = new Document("user", sessionUserID);
            activityDoc.put("activities", activities);
            activityDoc.put("dateInUTC", dateInUTC);

            // store activities in the database with the user id as identifier
            Bson activityFilter = and(eq("user", sessionUserID), eq("dateInUTC", dateInUTC));
            activityColl.replaceOne(activityFilter, activityDoc, new UpdateOptions().upsert(true));

            // Update Days Collection
            storeDayAndSteps(dateInUTC, steps, stepsDocument);

        });
    }

    /**
     * helper function to create some random numbers within the specified range
     * @param min: minimum range
     * @param max: maximum range
     * @return integer within the range [min, max+1]
     */
    private int createRandomData(int min, int max) {
        return min + (int)(Math.random() * ((max - min) + 1));
    }

    /**
     * stores the days (basically date, steps and number of entries) and the steps in the database
     * @param date : date in UTC
     * @param steps : steps on that day
     * @param stepsDocument: document for the steps
     */
    private void storeDayAndSteps(Long date, int steps, Document stepsDocument) {

        // the sums for calculating the mean and the std error mean
        double sum0 = 0;
        double sum1 = 0;
        double sum2 = 0;

        // holds the sums for calculating the mean and the std error mean
        Map<String, Double> sumsForMeanAndSEM = new HashMap<>();
        sumsForMeanAndSEM.put("sum0", sum0);
        sumsForMeanAndSEM.put("sum1", sum1);
        sumsForMeanAndSEM.put("sum2", sum2);

        // query the database for the current date
        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("dateInUTC", date);
        Document dayDoc = daysColl.find(whereQuery).limit(1).first();

        // day is already in the collection
        if (dayDoc != null) {

            // query the steps collection for the current date and user
            BasicDBObject andQuery = new BasicDBObject();
            List<BasicDBObject> queryFields = new ArrayList<>();
            queryFields.add(new BasicDBObject("user", sessionUserID));
            queryFields.add(new BasicDBObject("startDateInUTC", date));
            andQuery.put("$and", queryFields);
            Document stepsDoc = stepColl.find(andQuery).limit(1).first();

            // check if we have data from that user for the current day
            if (stepsDoc != null) {
                // day is in the collection and the users data for that day is in the collection, so we don't need to
                // do anything ..
                return;
            // we don't have the data from this user for this day
            } else {
                // get the hash map holding the sums of the other users for calculating the mean and the std error of
                // mean
                sumsForMeanAndSEM = (Map<String, Double>) dayDoc.get("sumsForMeanAndSEM");

                // get the sums from the hash map
                sum0 = sumsForMeanAndSEM.get("sum0");
                sum1 = sumsForMeanAndSEM.get("sum1");
                sum2 = sumsForMeanAndSEM.get("sum2");
            }
        }

        // update the sums with the values from the current day
        sum0 += 1;
        sum1 += steps;
        sum2 += steps*steps;

        // calculate the mean and the stdErrorOfMean (SEM)
        double mean = sum1/sum0;
        double stdDev = 0;
        if (sum0 >= 2) {	// we need at least two datapoints
            stdDev = Math.sqrt((sum0 * sum2 - sum1 * sum1)/(sum0 * (sum0 - 1)));
        }
        double stdErrorOfMean = stdDev/Math.sqrt(sum0);

        // update the hash map holding the sums
        sumsForMeanAndSEM.put("sum0", sum0);
        sumsForMeanAndSEM.put("sum1", sum1);
        sumsForMeanAndSEM.put("sum2", sum2);

        // create a new document for the current day
        Document newDayDoc = new Document();

        // put the values we want to store in the database in the new document
        newDayDoc.put("sumsForMeanAndSEM", sumsForMeanAndSEM);
        newDayDoc.put("mean", mean);
        newDayDoc.put("stdErrorOfMean", stdErrorOfMean);
        newDayDoc.put("dateInUTC", date);

        // store steps in the database with the user id as identifier
        Bson stepFilter = and(eq("user", sessionUserID), eq("startDateInUTC", date));
        stepColl.replaceOne(stepFilter, stepsDocument, new UpdateOptions().upsert(true));

        // store the current day in the database
        daysColl.replaceOne(eq("dateInUTC", date), newDayDoc, new UpdateOptions().upsert(true));

    }

    /**
     * extracts the user profile picture from the database
     * @param userID: the id of the user to query
     * @return the link to the user profile picture
     */
    public static String extractUserProfilePictureFromDatabase(String userID) {
        return userColl.find(eq("_id", userID)).first().getString("picture");
    }

    /**
     * extracts the user real name from the database
     * @param userID: the id of the user to query
     * @return the name of the user
     */
    public static String extractUserRealName(String userID) {
        return userColl.find(eq("_id", userID)).first().getString("name");
    }

    /**
     * TODO: experimental!
     * extracts the menu items which should be created and displayed from a collection in the database
     * @return ArrayList of MongoDB documents
     */
    public List<Document> extractMenuItemsCollection() {

        // collection named menu_items should have document with entries like this:
        /*
        button_caption: "Sample Chart"      // the caption for the button
        button_icon: "BOOKMARK"             // the vaadin icon for the button
        view_title: "Showing sample data"   // the title of the chart which is displayed in the top left corner
        plot_selected: "LineChart"          // ["LineChart", "StackedBarChart", "CalendarChart"] what kind of plot
                                               should be drawn
         */

        List<Document> docList = new ArrayList<>();
        try (MongoCursor<Document> cursor = db.getCollection("menu_items").find().iterator()) {
            while (cursor.hasNext()) {
                docList.add(cursor.next());
            }
        }
        return docList;
    }

    /**
     * extracts all the data from the database within the range of startTime and endTime and returns them in json format
     * @param startTime: the start of the interval (date in UTC)
     * @param endTime: the end of the interval (date in UTC)
     * @return json formatted string holding the date (in milliseconds), the steps and the activities of the current
     * user, the average steps of the other users and the std error of mean for it
     */
    public String extractData(Long startTime, Long endTime) {

        List<Document> docList = new ArrayList<>();

        for (Document document : db.getCollection("steps").aggregate(Arrays.asList(

                // we only want the data from the current user within the time range startTime to endTime
                match(
                        and(
                                eq("user", sessionUserID),
                                gte("startDateInUTC", startTime),
                                lte("endDateInUTC", endTime)
                        )
                ),

                // we want the average steps from the days collection
                lookup(
                        "days", "startDateInUTC", "dateInUTC", "means"
                ),

                // reshape the document by including only the startMillis, steps and average fields
                project(
                        fields(
                                excludeId(),
                                //include("user"),
                                include("startDateInUTC"),
                                include("steps"),
                                computed("mean", "$means.mean"),
                                computed("stdErrorOfMean", "$means.stdErrorOfMean")
                        )
                )
        ))) {
            // get the date
            Long date = (Long) document.get("startDateInUTC");

            // get the activities for the current user and the current date
            AggregateIterable<Document> databaseRequest = db.getCollection("activities").aggregate(Arrays.asList(

                    // get the activity document for the current user and the current date
                    match(
                            and(
                                    eq("user", sessionUserID),
                                    eq("dateInUTC", date)
                            )
                    ),
                    // we are only interested in the activities
                    project(
                            fields(
                                    excludeId(),
                                    include("activities")
                            )
                    )
            ));

            // create a document of the AggregateIterable (consists always of a single document)
            Document activities = new Document();
            for (Document doc : databaseRequest) {
                activities = (Document) doc.get("activities");
            }

            // if we have activity found ..
            if (!activities.isEmpty()) {
                // .. add it to the document ..
                document.put("activities", activities);
                // .. and the document to the list
                docList.add(document);
            // add the document without the activity to the list
            } else {
                docList.add(document);
            }

            // for some reason the average steps are projected onto a array containing a single double value,
            // so we simply extract the value and put it back into the document
            ArrayList mean = (ArrayList) document.get("mean");
            document.put("averageSteps", mean.get(0));
            document.remove("mean");

            ArrayList stdErrorOfMean = (ArrayList) document.get("stdErrorOfMean");
            document.put("stdErrorOfMean", stdErrorOfMean.get(0));

        }
        return JSON.serialize(docList);
    }
}
