
// TODO: make a .js file for the helper functions

function computeSum(array) {
    var count=0;
    for (var i=array.length; i--;) {
     count+=array[i];
    }
    return count;
}

function computeMean(array) {
    return computeSum(array) / array.length;
}

function computeVariance(array) {
    var mean = computeMean(array);
    return computeMean(array.map(function(num) {
        return Math.pow(num - mean, 2);
    }));
}




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

    // we have multiple dates as domain (= activity data)
    if (x.domain().length > 2) {
        daysOfMeasurements = x.domain();
    // we have two longs as domain (= steps data)
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
function calculateTotalTimeForActivities(day, keys) {

    var totalActivityTime = 0;
    var activities = day.activities;
    for (var activity in activities) {
        if (activities.hasOwnProperty(activity) && (keys.indexOf(activity) > -1)) {
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
    //return Array.from(new Set([].concat.apply([], activityNames))).sort();
}


/**
 * draws the stacked bar chart
 * @param data: array of objects holding the data to plot
 * @param g : svg component
 */
function drawStackedBarChart(data, selectedOptions, element, svg, g, width, height, margin, _keys, active_categories) {

    var keys;
    console.log(data);
    var allKeys = extractActivityNames(data);
    var active_categories;      // TODO: rename to camel case
    var active_categories_;     // TODO: rename to camel case

    if (_keys === null) {
        // get all activity names from the user, e.g. "walking", "sleeping", "in_vehicle", "still", etc.
        keys = allKeys;
        active_categories = "0";
    } else {
        keys = _keys;
        // remove previously drawn elements
        d3.select("svg").remove();

        // create the svg with the corresponding size
        svg = d3.select(element).append("svg:svg").attr("width", width+margin.right).attr("height",
                                                                                    height+margin.top+margin.bottom),
            g  = svg.append("g").attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    }

    active_categories_ = keys;  // TODO: check if it makes sense; currently only for testing purposes

    console.log("stacked bar chart");
    console.log("keys: " + keys);
    console.log("height: " + height);

    var timeFormat = d3.timeFormat("%Y-%m-%d");

    // create the x and y scales
    var x = d3.scaleBand()
        .rangeRound([0, width])
        .paddingInner(0.05)
        .align(0.1);

    var y = d3.scaleLinear()
        .rangeRound([height, 0]);

    // create the color scale
    // we have at most 9 categories (+1 color for the mouseover), so this color scale is fine
    var colorRange =
        ['#a6cee3','#1f78b4','#b2df8a','#33a02c','#fb9a99','#e31a1c','#fdbf6f','#ff7f00','#cab2d6','#6a3d9a'];
    var colorScale = d3.scaleOrdinal()
        //.range(['#e41a1c','#377eb8','#4daf4a','#984ea3','#ff7f00','#ffff33','#a65628','#f781bf','#999999']);
        .range(colorRange);


    // prepare the data ..
    data.forEach(function (d) {
        // .. by calculating the total activity time for each day
        d.total = calculateTotalTimeForActivities(d, keys);
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

    // calculate the standard deviation for all of the activities and store it in an object
    stdDeviationsForActivities = {};
    for (var k = 0; k < keys.length; k++) {     // iterate over all activities
        var activity = keys[k];
        activityValues = data.map(a => a.activities[activity]);     // get the values for the current activity
        stdDeviationsForActivities[activity] = Math.sqrt(computeVariance(activityValues));  // compute the std dev
    }

    // sort the keys in ascending order, so that activities with low std deviation are at the bottom of the chart
    keysSorted = Object.keys(stdDeviationsForActivities).sort(function(a,b){
        return stdDeviationsForActivities[a]-stdDeviationsForActivities[b]
    });
    keys = keysSorted;


    // sort the data by total activity time
    //data.sort(function(a, b) { return b.total - a.total; });

    // setup the domains for the scales
    x.domain(data.map(function(d) { return d.date; }));
    y.domain([0, d3.max(data, function(d) { return d.total/60000/60; })]).nice();
    colorScale.domain(allKeys);

    console.log("y domain" + y.domain());

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
        .attr("id", function(d,i) { return i; })
        .append("svg:title")
        .text(function(d, i) { return timeFormat(d.data.startMillis)});

        /*.on("mouseover", function(d,i) {
            d3.select(this).attr("fill", function() { return colorRange[colorRange.length-1]});  // TODO: last element form color range
            })
        .on("mouseout", function(d, i) { d3.select(this).attr("fill", function() {
            console.log(d);
            return "";
            });});*/
        /*.on("mouseover", function() { tooltip.style("display", null); })
        .on("mouseout", function() { tooltip.style("display", "none"); })
        .on("mousemove", function(d,i) {
            var xPosition = d3.mouse(this)[0] - 5;
            var yPosition = d3.mouse(this)[1] - 5;
            tooltip.attr("transform", "translate(" + xPosition + "," + yPosition + ")");
            tooltip.select("text").text(Math.round((d[1]-d[0])*100)/100);
            //console.log("i: " + i);
            //console.log("d: " + d);
        });*/

    // TODO: tooltips similar to LineChart tooltips

    console.log("x axis:");
    console.log(getDatesForXAxis(x, width));

    // draw the x axis
    g.append("g")
        .attr("class", "axis")
        .attr("transform", "translate(0," + height + ")")
        .call(d3.axisBottom(x)
            .tickValues(getDatesForXAxis(x, width))
        );

    // draw the y axis
    g.append("g")
        .attr("class", "axis")
        .call(d3.axisLeft(y).ticks(null, "r"))
        .append("text")
        .attr("x", 2)
        .attr("y", y(y.ticks().pop()) + 0.5)
        .attr("dy", "0.32em")
        .attr("fill", "#000")
        .attr("font-weight", "bold")
        .attr("text-anchor", "start")
        .text("Hours");

    var legendClassArray = [];  // TODO: added

    // create the legend
    var legend = g.append("g")
        .attr("font-family", "sans-serif")
        .attr("font-size", 10)
        .attr("text-anchor", "end")
        .selectAll("g")
        .data(allKeys.slice().reverse())
        .enter().append("g")
        .attr("transform", function(d, i) { return "translate(0," + i * 20 + ")"; })
        .attr("class", function (d) {       // TODO: ADDED
            legendClassArray.push(d.replace(/\s+/g, '')); //remove spaces
            return "legend";
        });


    //reverse order to match order in which bars are stacked    // TODO: added
    legendClassArray = legendClassArray.reverse();



    // add the colored rectangles to the legend
    legend.append("rect")
        .attr("x", width + 20)
        .attr("width", 19)
        .attr("height", 19)
        .attr("fill", colorScale)
        .attr("id", function (d, i) {
            return "id" + d.replace(/\s+/g, '');
        })
        .style("stroke", "black")
        .style("stroke-width", 2)
        .on("mouseover", function(d,i){
            d3.select(this).style("cursor", "pointer");
        })
        .on("click",function(d, i){

            var currentCategory = this.id.split("id").pop();
            var posInArrayOfCurrentCategory = active_categories_.indexOf(currentCategory);

            // category is in array -> user wants to disable the category -> remove it
            if (posInArrayOfCurrentCategory > -1) {
                active_categories_.splice(posInArrayOfCurrentCategory, 1);
            // category is not in array -> add it
            } else {
                active_categories_.push(currentCategory);
            }

            console.log(active_categories_);

            drawStackedBarChart(data, selectedOptions, element, svg, g, width, height, margin,
                active_categories_, active_categories);


            for (i = 0; i < legendClassArray.length; i++) {
                if (active_categories_.indexOf(legendClassArray[i]) < 0) {
                    d3.select("#id" + legendClassArray[i])
                        .style("stroke", "none");
                } else {
                    d3.select("#id" + legendClassArray[i])
                                                .style("stroke", "black")
                                                .style("stroke-width", 2);
                }
            }



        });



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


    /**
     * creates the tooltip text elements which are displayed when the user is hovering over the dots
     * @param coords: object holding the x and y coords for the text element e.g. {"cx": 10, "cy": 50}
     * @param isHoveringOverUserDataPoint: boolean whether the user is hovering over a data point referring
     * to the user or referring to the average
     */
    function createTooltipTextElements(coords) {

        var textToDisplay = {"selected": "", "notSelected": ""};
        if (isHoveringOverUserDataPoint) {
            textToDisplay.selected = "Your Steps: ";
            textToDisplay.notSelected = "Average Steps: ";
        } else {
            textToDisplay.selected = "Average Steps: ";
            textToDisplay.notSelected = "Your Steps: ";
        }

        // add the text for the currently selected dot
        svg.append("text")
            .attr("dy", ".35em")
            .attr("y", coords.cy+10)
            .attr("dx", coords.cx+30)
            .attr("text-anchor", "end")
            .style("fill", "black")
            .style("font-weight", "bold")
            .style("font-size", "large")
            .attr("id", "tooltip1")
            .text(textToDisplay.selected + "" + Math.round(coords.selectedSteps));

        // add the text for the other data point at the same time
        svg.append("text")
            .attr("dy", ".35em")
            .attr("y", coords.cy_other+10)
            .attr("dx", coords.cx+30)
            .attr("text-anchor", "end")
            .style("fill", "black")
            .style("font-weight", "bold")
            .style("font-size", "large")
            .attr("id", "tooltip2")
            .text(textToDisplay.notSelected + "" + Math.round(coords.notSelectedSteps));
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

    // deals with asynchronity of javascript
    /**
     * @param k: event listener are added to the k-th data point
     */
    function addEventListenerToDataPoint(k) {

        var event = document.createEvent('Event');
        event.initEvent('mouseover', true, true);

        document.getElementById(k).addEventListener("mouseover", function() {
/*            createTooltipTextElements({"cx": x(dat[k].startMillis), "cy": y(dat[k].averagesteps),
                "cy_other": y(dat[k].steps), "selectedSteps": dat[k].averagesteps,
                "notSelectedSteps": dat[k].steps}, isUserSelected);*/
            console.log(data[k]);
        });

        // when user stops hovering over a dot ..
        document.getElementById(k).addEventListener("mouseout", function() {
            // .. delete the tooltip
            removeElementFromDOM("tooltip1");
            removeElementFromDOM("tooltip2");
        });
    }

    // add the event listeners for the tooltips to the data points
    for (var k = 0; k < data.length; k++) {
        addEventListenerToDataPoint(k);
    }

}



