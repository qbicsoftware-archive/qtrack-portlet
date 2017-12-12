

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

/**
 * creates the tooltip text elements which are displayed when the user is hovering over the dots
 * @param coords: object holding the x and y coords for the text element e.g. {"cx": 10, "cy": 50}
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

/**
 * adds event listener to the k-th data point
 * @param k: index of the data point to add the event listener to
 */
function addEventListenerToDataPoint(k) {

    var event = document.createEvent('Event');
    event.initEvent('mouseover', true, true);

    document.getElementById(k).addEventListener("mouseover", function() {
        /*            createTooltipTextElements({"cx": x(dat[k].startDateInUTC), "cy": y(dat[k].averagesteps),
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

/**
 * draws the stacked bar chart
 * @param data: array of objects holding the data to plot
 * @param g : svg component
 */
/**
 * draws the stacked bar chart
 * @param data: object holding the data to plot
 * @param selectedOptions: options selected by the user; currently not used
 * @param element: element for the svg container
 * @param svg: svg container which holds the g container
 * @param g: container which holds the plots
 * @param width: width available for the svg container
 * @param height: height available for the svg container
 * @param margin: margin for the svg: container
 * @param selectedActivities_: the activities to display selected by the user
 * @return {Array.<*>|*}
 */
function drawStackedBarChart(data, selectedOptions, element, svg, g, width, height, margin, selectedActivities_) {

    var selectedActivities;
    console.log(data);
    var allActivities = extractActivityNames(data);

    if (selectedActivities_ === null || selectedActivities_.length === 0) {
        // get all activity names from the user, e.g. "walking", "sleeping", "in_vehicle", "still", etc.
        selectedActivities = allActivities;
    } else {
        selectedActivities = selectedActivities_;
    }

    // check if there is at least one category to display
    var hasCategoryBeenFound = false;
    for (var i = 0; i < selectedActivities.length; i++) {
        console.log(selectedActivities[i]);
        console.log(allActivities.indexOf(selectedActivities[i]) > -1);
        if (allActivities.indexOf(selectedActivities[i]) > -1) {
            hasCategoryBeenFound = true;
        }
    }

    // if no category to display has been found, display all categories
    if (!hasCategoryBeenFound) {
        selectedActivities = allActivities;
    }

    // remove previously drawn elements
    d3.select("svg").remove();

    // create the svg with the corresponding size
    svg = d3.select(element).append("svg:svg").attr("width", width+margin.right).attr("height",
        height+margin.top+margin.bottom);
    g  = svg.append("g").attr("transform", "translate(" + margin.left + "," + margin.top + ")");

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

    // time format for the dates
    var timeFormat = d3.timeFormat("%d-%m-%Y");

    // prepare the data ..
    data.forEach(function (d) {
        // .. by calculating the total activity time for each day
        d.total = calculateTotalTimeForActivities(d, selectedActivities);
        // .. by creating new properties holding the individual activity time in hours
        for (var j = 0; j < selectedActivities.length; j++) {
            var activityName = selectedActivities[j];
            // if we don't have activity on that day
            if (typeof d.activities[activityName] === "undefined") {
                d[activityName] = 0;
            } else {
                // we want hours, not milliseconds
                d[activityName] = d.activities[activityName]/60000/60;
            }
        }
        // .. by setting the date in a proper time format
        d.date = timeFormat(d.startDateInUTC);
    });

    // calculate the standard deviation for all of the activities and store it in an object
    var stdDeviationsForActivities = {};
    for (var k = 0; k < selectedActivities.length; k++) {
        var activity = selectedActivities[k];
        // get the values for the current activity
        var activityValues = data.map(a => a.activities[activity]);
        // compute the std dev
        stdDeviationsForActivities[activity] = Math.sqrt(computeArrayVariance(activityValues));
    }

    // sort the keys in ascending order, so that activities with low std deviation are at the bottom of the chart
    selectedActivities = Object.keys(stdDeviationsForActivities).sort(function(a, b){
        return stdDeviationsForActivities[a]-stdDeviationsForActivities[b]
    });

    // sort the data by total activity time


    //data.sort(function(a, b) { return b.total - a.total; });

    // setup the domains for the scales
    x.domain(data.map(function(d) { return d.date; }));
    y.domain([0, d3.max(data, function(d) { return d.total/60000/60; })]).nice();
    colorScale.domain(allActivities);

    // if there are too many bars, remove the spacing
    if (x.bandwidth() < 3) {
        x.paddingInner = 0;
    }

    // draw the bars
    g.append("g")
        .selectAll("g")
        .data(d3.stack().keys(selectedActivities)(data))
        .enter().append("g")
        .attr("fill", function(d) { return colorScale(d.key); })
        .selectAll("rect")
        .data(function(d) { return d; })
        .enter().append("rect")
        .attr("x", function(d) { return x(timeFormat(d.data.startDateInUTC)); })
        .attr("y", function(d) { return y(d[1]); })
        .attr("height", function(d) { return y(d[0]) - y(d[1]); })
        .attr("width", x.bandwidth())
        .attr("id", function(d,i) { return i; })
        .append("svg:title")
        .text(function(d) { return timeFormat(d.data.startDateInUTC)});

    /*.on("mouseover", function(d,i) {
     d3.select(this).attr("fill", function() { return colorRange[colorRange.length-1]});
     // TODO: last element form color range
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

    // the array which holds all the categories for the legend
    var legendCategoryArray = [];

    // create the legend
    var legend = g.append("g")
        .attr("font-family", "sans-serif")
        .attr("font-size", 10)
        .attr("text-anchor", "end")
        .selectAll("g")
        .data(allActivities.slice().reverse())
        .enter().append("g")
        .attr("transform", function(d, i) { return "translate(0," + i * 20 + ")"; })
        .attr("class", function (d) {
            // remove the spaces from the category and add it to the array
            legendCategoryArray.push(d.replace(/\s+/g, ''));
            return "legend";
        });

    //reverse order to match order in which bars are stacked
    legendCategoryArray = legendCategoryArray.reverse();

    // add the colored rectangles to the legend
    legend.append("rect")
        .attr("x", width + 20)
        .attr("width", 19)
        .attr("height", 19)
        .attr("fill", colorScale)
        .attr("id", function (d) {
            return "id" + d.replace(/\s+/g, '');
        })
        .on("mouseover", function(){
            d3.select(this).style("cursor", "pointer");
        })
        .on("click",function(){
            var currentCategory = this.id.split("id").pop();
            var posOfCurrentCategoryInArray = selectedActivities.indexOf(currentCategory);

            // category is in array -> user wants to disable the category -> remove it
            if (posOfCurrentCategoryInArray > -1) {

                // if there is at least one active category
                if (selectedActivities.length > 0) {
                    // disable the clicked category
                    selectedActivities.splice(posOfCurrentCategoryInArray, 1);
                } else {
                    // redraw the chart with all categories set to selected
                    drawStackedBarChart(data, selectedOptions, element, svg, g, width, height, margin, allActivities);
                }
            // category is not in array -> add it
            } else {
                selectedActivities.push(currentCategory);
            }

            // redraw the chart with the selected categories
            drawStackedBarChart(data, selectedOptions, element, svg, g, width, height, margin, selectedActivities);
        });

    // mark the active categories as selected in the legend by setting a border for the respective rectangle
    for (var m = 0; m < legendCategoryArray.length; m++) {
        if (selectedActivities.indexOf(legendCategoryArray[m]) < 0) {
            d3.select("#id" + legendCategoryArray[m])
                .style("stroke", "none");
        } else {
            d3.select("#id" + legendCategoryArray[m])
                .style("stroke", "black")
                .style("stroke-width", 2);
        }
    }

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

    // add the event listeners for the tooltips to the data points
/*
    for (var n = 0; n < data.length; n++) {
        addEventListenerToDataPoint(n);
    }
*/

    // return the categories selected by the user
    return selectedActivities;
}