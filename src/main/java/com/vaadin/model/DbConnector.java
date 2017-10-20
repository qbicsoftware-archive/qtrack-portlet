package com.vaadin.model;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.util.JSON;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;


public class DbConnector extends MongoClient {

    MongoDatabase db;                               // the Mongo database
    static MongoCollection<Document> userColl;      // collection storing the users
    MongoCollection<Document> stepColl;             // collection storing the steps
    MongoCollection<Document> daysColl;             // collection storing the days
    String sessionUserID;                           // the user session id (should be the same as the google account id)


    /**
     * Accesses DB, creates an Instance if it does not exist yet.c
     * @param sessionUserID: the user session id
     */
    public DbConnector(String sessionUserID) {
        this.db = getDatabase("trackFit");
        this.sessionUserID = sessionUserID;
        userColl = db.getCollection("users");
        stepColl = db.getCollection("steps");
        daysColl = db.getCollection("days");
    }

    /**
     * this function stores a new user in the database. If the user is already in the database his fields (name, email,
     * profile picture) are getting updated
     * @param googleUserData: the user data provided from google
     */
    public void storeUser(String googleUserData) {

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
    public void storeSteps(String stepData) {

        // Create new Document parsed from Google JSON data
        Document stepDoc = Document.parse(stepData);

        // Split Document into Array
        ArrayList<Document> stepList = (ArrayList<Document>)stepDoc.get("bucket");

        stepList.forEach(document -> {
            long startTime = Long.valueOf(document.get("endTimeMillis").toString());
            int steps;

            // Assign steps to currently logged in User
            document.append("user", sessionUserID);

            // Ugly loop through nested arrays to get the Step value. Maybe there is a better way.
            ArrayList<Document> datasetList  =  (ArrayList<Document>) document.get("dataset");
            ArrayList<Document> pointList  =  ((ArrayList<Document>) datasetList.get(0).get("point"));

            // check if we have activity during that day
            if (pointList.size() > 0 ) {
                ArrayList<Document> valueList = (ArrayList<Document>)(pointList.get(0).get("value"));
                steps =  valueList.get(0).getInteger("intVal");

            // we don't have activity..
            } else {

                // Set step value to 0 if no activity for this day
                //steps = 0;      // TODO: change it back to 0

                // create some random data
                int max = 5000;
                int min = 250;
                steps = min + (int)(Math.random() * ((max - min) + 1));
            }

            // Convert timeInMillis to long
            document.put("steps", steps);
            document.put("startMillis", startTime);
            document.append("date", new Date(startTime));
            document.put("endMillis", Long.valueOf(document.get("endTimeMillis").toString()));

            // Clean up
            document.remove("dataset");
            document.remove("startTimeMillis");
            document.remove("endTimeMillis");

            Bson myFilters = and(eq("user", sessionUserID), eq("startMillis", startTime));
            stepColl.replaceOne(myFilters, document, new UpdateOptions().upsert(true));

            // Update Days Collection
            storeDays(startTime, steps);
        });

    }

    public void storeDays(Long startTime, int steps) {

        Document newDayDoc = new Document("_id", startTime);
        // In the case, that this is the first entry on this day
        double newAverage = steps;
        int entries = 0;

        // Recalculate values when this day is already in the collection
        if (daysColl.count(eq("_id", startTime)) > 0) {
            double oldAverage = Double.valueOf(daysColl.find(eq("_id", startTime)).first().get("average").toString());
            entries = daysColl.find(eq("_id", startTime)).first().getInteger("entries");
            newAverage = ((oldAverage * entries) + steps) / (entries + 1);
        }

        newDayDoc.put("entries", entries+1 );
        newDayDoc.put("average", newAverage);
        newDayDoc.put("date", new Date(startTime));

        daysColl.replaceOne(eq("_id", startTime), newDayDoc, new UpdateOptions().upsert(true));

    }

    public static String extractUserPicture(String userID) {
        return userColl.find(eq("_id", userID)).first().getString("picture");
    }

    public static String extractUserRealName(String userID) {
        return userColl.find(eq("_id", userID)).first().getString("name");
    }


    public String extractData(Long startTime, Long endTime) {

        List<Document> docList = new ArrayList<>();

        MongoCursor<Document> iterator = db.getCollection("steps").aggregate(Arrays.asList(
                match(
                        and(
                                eq("user", sessionUserID),
                                gte("startMillis", startTime),
                                lte("endMillis", endTime)
                        )
                ),
                lookup(
                        "days", "startMillis", "_id", "averages"
                ),
                project(
                        fields(
                                excludeId(),
                                //include("user"),
                                include("startMillis"),
                                include("steps"),
                                computed("average", "$averages.average")

                        )
                )
        )).iterator();

        while (iterator.hasNext()) {
            docList.add(iterator.next());
        }

        return JSON.serialize(docList);

    }

}
