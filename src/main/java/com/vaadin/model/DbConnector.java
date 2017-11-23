package com.vaadin.model;

import com.mongodb.MongoClient;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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
    private String sessionUserID;                           // the user session id (should be the same as the google account id)
    private Map<Integer, String> activityTypesValuesMapper  // maps the integer to the corresponding activity
            = new HashMap<Integer, String>()
    {{                                                      // TODO: other activities
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
    }};


    /**
     * Accesses DB, creates an Instance if it does not exist yet.
     * @param sessionUserID: the user session id
     */
    public DbConnector(String sessionUserID) {
        this.db = getDatabase("trackFit");
        this.sessionUserID = sessionUserID;
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
        ArrayList<Document> dayList = (ArrayList<Document>)stepDoc.get("bucket");


        // grab the step and activity data for each day
        dayList.forEach(document -> {

            // get the date
            long dateInMillis = Long.valueOf(document.get("startTimeMillis").toString());

            // variables for holding the step data and the activity data
            int steps = -1;
            Map<String, Integer> activities = new HashMap<>();

            // Assign steps to currently logged in User
            document.append("user", sessionUserID);

            // get the data set from the document and store it in a array list of documents
            ArrayList<Document> dataSetList  =  (ArrayList<Document>) document.get("dataset");

            // TODO: from here to ...

            // iterate over each document representing either step or activity data from google
            for (Document documentInDatasets : dataSetList) {

                // get the actual data from the document
                ArrayList<Document> dataInDataSet = (ArrayList<Document>) documentInDatasets.get("point");

                // check if we have any data at all
                if (dataInDataSet.size() > 0 ) {

                    // iterate over the data entries (for step data we always have one; for activity data we have
                    // one for each activity type (walking, sleeping, still, etc.))
                    for (Document dataEntry : dataInDataSet) {

                        // we have step data
                        if (dataEntry.get("dataTypeName").equals("com.google.step_count.delta")) {

                            // we only have one step entry, so simply setting the variable steps is fine..
                            ArrayList<Document> valueList = (ArrayList<Document>)(dataEntry.get("value"));
                            steps =  valueList.get(0).getInteger("intVal");

                        // we have activity data
                        } else {

                            // get the activities for the activity entry
                            // we have three documents per value List
                            ArrayList<Document> valueList = (ArrayList<Document>)(dataEntry.get("value"));

                            // check if the activity is in the activityTypesValueMapper
                            if (activityTypesValuesMapper.get(valueList.get(0).getInteger("intVal")) != null) {

                                String activity = activityTypesValuesMapper.get(valueList.get(0).getInteger("intVal"));
                                int duration = valueList.get(1).getInteger("intVal");
/*                                String activityType =
                                        activityTypesValuesMapper.get(valueList.get(2).getInteger("intVal"));*/

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

            // Convert timeInMillis to long
            document.put("steps", steps);
            document.put("startMillis", dateInMillis);
            document.put("endMillis", Long.valueOf(document.get("endTimeMillis").toString()));

            // remove unnecessary fields in the document
            document.remove("dataset");
            document.remove("startTimeMillis");
            document.remove("endTimeMillis");

            // store steps in the database with the user id as identifier
            Bson stepFilter = and(eq("user", sessionUserID), eq("startMillis", dateInMillis));
            stepColl.replaceOne(stepFilter, document, new UpdateOptions().upsert(true));

            // create the document for the activities
            Document activityDoc = new Document("user", sessionUserID);
            activityDoc.put("activities", activities);
            activityDoc.put("timeMillis", dateInMillis);

            // store activities in the database with the user id as identifier
            Bson activityFilter = and(eq("user", sessionUserID), eq("timeMillis", dateInMillis));
            activityColl.replaceOne(activityFilter, activityDoc, new UpdateOptions().upsert(true));

            // Update Days Collection
            storeDay(dateInMillis, steps, activities);
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
     * stores the days (basically date, steps and number of entries) in the database
     * @param date : the date in milliseconds since 1970
     * @param steps : the steps on that day
     * @param activities: the activities on that day
     */
    private void storeDay(Long date, int steps, Map<String, Integer> activities) {

        Document newDayDoc = new Document("_id", date);
        // In the case, that this is the first entry on this day
        double newAverage = steps;
        int entries = 0;

        // Recalculate values when this day is already in the collection
        if (daysColl.count(eq("_id", date)) > 0) {
            double oldAverage = Double.valueOf(daysColl.find(eq("_id", date)).first().get("average").toString());
            entries = daysColl.find(eq("_id", date)).first().getInteger("entries");
            newAverage = ((oldAverage * entries) + steps) / (entries + 1);
        }

        // update the number of entries, the average and the date
        newDayDoc.put("entries", entries+1 );
        newDayDoc.put("average", newAverage);
        newDayDoc.put("timeMillis", date);

        // update the entry in the day collection (insert if it doesn't exist yet)
        daysColl.replaceOne(eq("_id", date), newDayDoc, new UpdateOptions().upsert(true));
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
     * extracts all the data from the database within the range of startTime and endTime and returns them in json format
     * @param startTime: the start of the interval (date in milliseconds since 1970)
     * @param endTime: the end of the interval (date in milliseconds since 1970)
     * @return json formatted string holding the date (in milliseconds), the steps of the current user and the
     *          average steps of the other users
     */
    public String extractData(Long startTime, Long endTime) {

        List<Document> docList = new ArrayList<>();

        for (Document document : db.getCollection("steps").aggregate(Arrays.asList(

                // we only want the data from the current user within the time range startTime to endTime
                match(
                        and(
                                eq("user", sessionUserID),
                                gte("startMillis", startTime),
                                lte("endMillis", endTime)
                        )
                ),

                // we want the average steps from the days collection
                lookup(
                        "days", "startMillis", "_id", "averages"
                ),

                // reshape the document by including only the startMillis, steps and average fields
                project(
                        fields(
                                excludeId(),
                                //include("user"),
                                include("startMillis"),
                                include("steps"),
                                computed("average", "$averages.average")
                        )
                )
        ))) {

            // get the date
            Long date = (Long) document.get("startMillis");

            // get the activities for the current user and the current date
            AggregateIterable<Document> databaseRequest = db.getCollection("activities").aggregate(Arrays.asList(

                    // get the activity document for the current user and the current date
                    match(
                            and(
                                    eq("user", sessionUserID),
                                    eq("timeMillis", date)
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
        }
        return JSON.serialize(docList);
    }
}
