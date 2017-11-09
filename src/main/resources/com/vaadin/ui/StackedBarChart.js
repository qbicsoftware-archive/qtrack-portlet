
// TODO: make a .js file for the helper functions

/*
 * calculates the number of ticks to use for the x axis
 * param n: number of measurements
 */
function calculateNumberOfTicks(n, width) {

    // calculate the number of ticks we can display
    var tickLength = 65;
    var tickPadding = 10;
    var numberOfTicks = Math.floor(width/(tickLength+tickPadding));

    // deal with a width too small or too large
    if (numberOfTicks < 1) {
        numberOfTicks = 1;
    } else if (numberOfTicks > n) {
        numberOfTicks = n;
    }

    console.log("width and width calculation")
    console.log(width);
    console.log(width/(tickLength+tickPadding));

    console.log("there are" + numberOfTicks + " ticks fitting in a width of " + width)
    console.log("every " + Math.floor(n/numberOfTicks) + "days")

    return Math.floor(n/numberOfTicks);
}


/*
 * this function returns the dates for the x axis labels (every x-th date, where x depends on the width of the
 * window)
 */
function getDatesForXAxis(x, width) {

    var daysOfMeasurements = [];

    // we have multiple dates as domain
    if (x.domain().length > 2) {
        daysOfMeasurements = x.domain();
    // we have two longs as domain
    } else {
        // get all dates between the first and the last measurement
        var now = new Date(x.domain()[1]);
        for (var d = new Date(x.domain()[0]); d <= now; d.setDate(d.getDate() + 1)) {
            daysOfMeasurements.push(new Date(d));
        }
    }

    // we are only interested in each xth day
    var everyXthDay = [];
    var delta = calculateNumberOfTicks(daysOfMeasurements.length, width);   // calculate how many x labels we can display
    for (i = 0; i < daysOfMeasurements.length; i=i+delta) {
        everyXthDay.push(daysOfMeasurements[i]);
    }

    return everyXthDay;
}




/**
 * calculates the total duration of all activities on that day and returns it
 * @param day: the object holding all the activities on that day
 * @return {number} the total time of all activities on the specified day
 */
function calculateTotalTimeForActivities(day) {

    var totalActivityTime = 0;
    var activities = day.activities;
    for (var activity in activities) {
        if (activities.hasOwnProperty(activity)) {
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

    var activityNames = [];
    for (var i = 0; i < data.length; i++) {
        activityNames.push(Object.getOwnPropertyNames(data[i].activities));
    }
    return Array.from(new Set([].concat.apply([], activityNames)));
}


/**
 * draws the stacked bar chart
 * @param data: array of objects holding the data to plot
 * @param g : svg component
 */
function drawStackedBarChart(data, selectedOptions, svg, g, width, height) {

    console.log("stacked bar chart");

    var timeFormat = d3.timeFormat("%Y-%m-%d");

    // create the x and y scales
    var x = d3.scaleBand()
        .rangeRound([0, width])
        .paddingInner(0.05)
        .align(0.1);

    var y = d3.scaleLinear()
        .rangeRound([height, 0]);

    // create the color scale
    // we have at most 9 categories, so this color scale is fine
    var colorScale = d3.scaleOrdinal()
        .range(['#e41a1c','#377eb8','#4daf4a','#984ea3','#ff7f00','#ffff33','#a65628','#f781bf','#999999']);

    // get all activity names from the user, e.g. "walking", "sleeping", "in_vehicle", "still", etc.
    var keys = extractActivityNames(data);

    // prepare the data ..
    data.forEach(function (d) {
        // .. by calculating the total activity time for each day
        d.total = calculateTotalTimeForActivities(d);
        // .. by creating new properties holding the individual activity time in hours
        for (var i = 0; i < keys.length; i++) {
            var activityName = keys[i];
            // if we don't have activity
            if (typeof d.activities[activityName] === "undefined") {
                d[activityName] = 0;
            } else {
                // we want hours, not milliseconds
                d[activityName] = d.activities[activityName]/60000/60;
            }
        }
        // .. by setting the date in a proper time format
        d.date = timeFormat(d.startMillis);
    });

    // sort the data by total activity time
    //data.sort(function(a, b) { return b.total - a.total; });

    // setup the domains for the scales
    x.domain(data.map(function(d) { return d.date; }));
    y.domain([0, d3.max(data, function(d) { return d.total/60000/60; })]).nice();
    colorScale.domain(keys);

    if (x.bandwidth() < 3) {
        x.paddingInner = 0;
    }

    // draw the bars
    g.append("g")
        .selectAll("g")
        .data(d3.stack().keys(keys)(data))
        .enter().append("g")
        .attr("fill", function(d) { return colorScale(d.key); })
        .selectAll("rect")
        .data(function(d) { return d; })
        .enter().append("rect")
        .attr("x", function(d) { return x(timeFormat(d.data.startMillis)); })
        .attr("y", function(d) { return y(d[1]); })
        .attr("height", function(d) { return y(d[0]) - y(d[1]); })
        .attr("width", x.bandwidth())
/*        .append("svg:title")
        .text(function(d, i) { return timeFormat(d.data.startMillis)})*/
        .on("mouseover", function() { tooltip.style("display", null); })
        .on("mouseout", function() { tooltip.style("display", "none"); })
        .on("mousemove", function(d) {
            var xPosition = d3.mouse(this)[0] - 5;
            var yPosition = d3.mouse(this)[1] - 5;
            tooltip.attr("transform", "translate(" + xPosition + "," + yPosition + ")");
            tooltip.select("text").text(Math.round((d[1]-d[0])*100)/100);
        });

    // TODO: tooltips similar to LineChart tooltips


    // draw the x axis
    g.append("g")
        .attr("transform", "translate(0," + height + ")")
        .call(d3.axisBottom(x)
            .tickValues(getDatesForXAxis(x, width))
        );

    // draw the y axis
    g.append("g")
        .attr("class", "axis")
        .call(d3.axisLeft(y).ticks(null, "s"))
        .append("text")
        .attr("x", 2)
        .attr("y", y(y.ticks().pop()) + 0.5)
        .attr("dy", "0.32em")
        .attr("fill", "#000")
        .attr("font-weight", "bold")
        .attr("text-anchor", "start")
        .text("Hours");

    // create the legend
    var legend = g.append("g")
        .attr("font-family", "sans-serif")
        .attr("font-size", 10)
        .attr("text-anchor", "end")
        .selectAll("g")
        .data(keys.slice().reverse())
        .enter().append("g")
        .attr("transform", function(d, i) { return "translate(0," + i * 20 + ")"; });

    // add the colored rectangles to the legend
    legend.append("rect")
        .attr("x", width + 20)
        .attr("width", 19)
        .attr("height", 19)
        .attr("fill", colorScale);

    // add the text to the legend
    legend.append("text")
        .attr("x", width + 15)
        .attr("y", 9.5)
        .attr("dy", "0.32em")
        .text(function(d) { return d; });


    // TODO: tooltips
    // Prep the tooltip bits, initial display is hidden
    var tooltip = svg.append("g")
        .attr("class", "tooltip")
        .style("display", "none");

    tooltip.append("rect")
        .attr("width", 60)
        .attr("height", 20)
        .attr("fill", "white")
        .style("opacity", 0.5);

    tooltip.append("text")
        .attr("x", 30)
        .attr("dy", "1.2em")
        .style("text-anchor", "middle")
        .attr("font-size", "12px")
        .attr("font-weight", "bold");



}



