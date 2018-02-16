/**
 * Holds sum functions for the different charts which are not directly related with drawing.
 */

/**
 * computes the sum of the array
 * @param array
 * @return {number}: sum of the array
 */
function computeArraySum(array) {
    var count=0;
    for (var i=array.length; i--;) {
        count+=array[i];
    }
    return count;
}

/**
 * computes the mean of the array
 * @param array
 * @return {number}: mean of the array
 */
function computeArrayMean(array) {
    return computeArraySum(array) / array.length;
}

/**
 * computes the variance of the array
 * @param array:
 * @return {number}: variance of the array
 */
function computeArrayVariance(array) {
    var mean = computeArrayMean(array);
    return computeArrayMean(array.map(function(num) {
        return Math.pow(num - mean, 2);
    }));
}

/**
 * calculates the number of ticks to use for the x axis
 * @param n: number of measurements we have to display
 * @param width: maximum width for the ticks
 * @return {number}: number of ticks we can properly display
 */
function calculateNumberOfTicks(n, width) {

    // calculate the number of ticks we can display
    var tickLength = 65;
    var tickPadding = 20;
    var numberOfTicks = Math.floor(width/(tickLength+tickPadding));

    // deal with a width too small or too large
    if (numberOfTicks < 1) {
        numberOfTicks = 1;
    } else if (numberOfTicks > n) {
        numberOfTicks = n;
    }

    return Math.floor(n/numberOfTicks);
}

/**
 * returns the dates for the x axis (every x-th date, where x depends on the width of the window)
 * @param xScale: scale of the x axis
 * @param windowWidth: width of the window we have for the application
 * @return {Array} holding the dates for the ticks for the x axis
 */
function getDatesForXAxis(xScale, windowWidth) {

    // holds the dates for which we have measurements
    var datesOfMeasurements = [];

    // distinguish between activity data ..
    if (xScale.domain().length > 2) {
        datesOfMeasurements = xScale.domain();
        // .. and steps data
    } else {
        // get all dates between the first and the last measurement
        var now = new Date(xScale.domain()[1]);
        for (var d = new Date(xScale.domain()[0]); d <= now; d.setDate(d.getDate() + 1)) {
            datesOfMeasurements.push(new Date(d));
        }
    }

    // we can only display every x^th day
    var everyXthDay = [];
    // calculate the number of dates we need to skip between two tick labels
    var delta = calculateNumberOfTicks(datesOfMeasurements.length, windowWidth);
    // insert every x^th day to the array
    for (var i = 0; i < datesOfMeasurements.length; i = i + delta) {
        everyXthDay.push(datesOfMeasurements[i]);
    }
    return everyXthDay;
}


/**
 * removes the element with the elementId from DOM
 * @param: elementId: the Id of the element to remove
 */
function removeElementFromDOM(elementId) {

    var tooltipElem = document.getElementById(elementId);
    if (tooltipElem !== null) {
        tooltipElem.parentElement.removeChild(tooltipElem);
    }
}


/**
 * calculates the total duration of all selected activities on the specified day and returns it
 * @param day: object holding all the activities on that day
 * @param selectedActivities: array holding the activities selected by the user
 * @return {number}: total activity time
 */
function calculateTotalTimeForActivities(day, selectedActivities) {

    var totalActivityTime = 0;
    var activities = day.activities;
    for (var activity in activities) {
        if (activities.hasOwnProperty(activity) && (selectedActivities.indexOf(activity) > -1)) {
            totalActivityTime += activities[activity]
        }
    }
    return totalActivityTime;
}


/**
 * extracts all the activities of the array of object
 * @param data: array of objects
 * @return {*} array with all the activity names, e.g. ["walking", "sleeping", "still"]
 */
function extractActivityNames(data) {

    // iterate over the data and push the activity name to the array
    var activityNames = [];
    for (var i = 0; i < data.length; i++) {
        activityNames.push(Object.getOwnPropertyNames(data[i].activities));
    }

    // create a set to get the activities only once even though they might appear multiple times
    var uniqueActivityNames = new Set([].concat.apply([], activityNames));

    // convert set back to array and return it
    return Array.from(uniqueActivityNames);
}

