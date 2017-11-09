
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
 * draws the line chart displaying the step data for the user and the average of other users
 * @param dat: the data to plot (array of objects containing the avg steps, user steps and date in milliseconds, etc)
 * @param selectedOptions: object holding the user selected options as properties, e.g. the selected time, the
 * the selected plot, colors, etc.
 * @param svg: the svg element where the drawn content gets added to
 * @param g: grouped element where the drawn content gets added to
 * @param width: the width of the plot
 * @param height: the height of the plot
 */
function drawLineChart(dat, selectedOptions, svg, g, width, height) {

    // set the ranges for the axis
    var x = d3.scaleLinear().range([10, width]);
    var y = d3.scaleLinear().range([height, 0]);


    console.log(dat);
    // Scale the range of the data
    x.domain(d3.extent(dat, function(d) { return d.millisstart; }));
    y.domain([0, d3.max(dat, function(d) {return Math.max(d.usersteps, d.averagesteps); })]);

    // define the 1st line
    var userStepsLine = d3.line()
        .x(function(d) { return x(d.millisstart); })
        .y(function(d) { return y(d.usersteps); });

    // define the 2nd line
    var avgStepsLine = d3.line()
        .x(function(d) { return x(d.millisstart); })
        .y(function(d) { return y(d.averagesteps); });


    // Add the user steps
    g.append("path")
        .data([dat])
        .attr("d", userStepsLine)
        .attr("stroke", selectedOptions.colorForUserSteps)
        .attr("fill", "none")
        .attr("stroke-width", "3px");


    var timeFormat = d3.timeFormat("%Y-%m-%d");

    // add the circles to select the tooltips
    svg.selectAll("dot")
        .data(dat)
        .enter().append("circle")
        .attr("r", 5)
        .attr("cx", function(d) { return x(d.startMillis); })
        .attr("cy", function(d) { return y(d.steps); })
        .attr("transform", "translate(50,12)")
        .attr("fill", selectedOptions.colorForUserSteps)
        .attr("id", function(d,i) { return "user_"+i; })
        .append("svg:title")
        .text(function(d, i) { return timeFormat(d.startMillis)});


    // Add the average steps
    g.append("path")
        .data([dat])
        .attr("d", avgStepsLine)
        .attr("stroke-width", "3px")
        .attr("fill", "none")
        .attr("stroke", selectedOptions.colorForAvgSteps);


    // add the circles to select the tooltips
    svg.selectAll("dot")
        .data(dat)
        .enter().append("circle")
        .attr("r", 5)
        .attr("cx", function(d) { return x(d.startMillis); })
        .attr("cy", function(d) { return y(d.averagesteps); })
        .attr("transform", "translate(50,12)")
        .attr("fill", selectedOptions.colorForAvgSteps)
        .attr("id", function(d,i) { return "avg_"+i; })
        .append("svg:title")
        .text(function(d, i) { return timeFormat(d.startMillis)});

    /**
     * creates the tooltip text elements which are displayed when the user is hovering over the dots
     * @param coords: object holding the x and y coords for the text element e.g. {"cx": 10, "cy": 50}
     * @param isHoveringOverUserDataPoint: boolean whether the user is hovering over a data point referring
     * to the user or referring to the average
     */
    function createTooltipTextElements(coords, isHoveringOverUserDataPoint) {

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

        document.getElementById("avg_"+k).addEventListener("mouseover", function() {
            console.log("mouse over avg" + k);
            console.log(x(dat[k].startMillis));
            console.log(y(dat[k].averagesteps));
            var isUserSelected = false;
            createTooltipTextElements({"cx": x(dat[k].startMillis), "cy": y(dat[k].averagesteps),
                "cy_other": y(dat[k].steps), "selectedSteps": dat[k].averagesteps,
                "notSelectedSteps": dat[k].steps}, isUserSelected);
        });

        document.getElementById("user_"+k).addEventListener("mouseover", function() {
            console.log("mouse over user" + k);
            console.log(x(dat[k].startMillis));
            console.log(y(dat[k].steps));
            var isUserSelected = true;
            createTooltipTextElements({"cx": x(dat[k].startMillis), "cy": y(dat[k].steps),
                "cy_other": y(dat[k].averagesteps), "selectedSteps": dat[k].steps,
                "notSelectedSteps": dat[k].averagesteps}, isUserSelected);
        });

        // when user stops hovering over a dot ..
        document.getElementById("avg_"+k).addEventListener("mouseout", function() {
            // .. delete the tooltip
            removeElementFromDOM("tooltip1");
            removeElementFromDOM("tooltip2");
        });

        // when user stops hovering over a dot ..
        document.getElementById("user_"+k).addEventListener("mouseout", function() {
            // .. delete the tooltip
            removeElementFromDOM("tooltip1");
            removeElementFromDOM("tooltip2");
        });

    }

    // add the event listeners for the tooltips to the data points
    for (var k = 0; k < dat.length; k++) {
        addEventListenerToDataPoint(k);
    }



    // Add the X Axis
    g.append("g")
        .attr("transform", "translate(0," + height + ")")
        .call(d3.axisBottom(x)
            .tickFormat(d3.timeFormat("%Y-%m-%d"))
            .tickValues(getDatesForXAxis(x, width))
        );

    // Add the Y Axis
    g.append("g")
        .call(d3.axisLeft(y));


    // create the color scale
    var colorScale = d3.scaleOrdinal()
        .range([selectedOptions.colorForUserSteps, selectedOptions.colorForAvgSteps]);

    // create the legend
    var legend = g.append("g")
        .attr("font-family", "sans-serif")
        .attr("font-size", 10)
        .attr("text-anchor", "end")
        .selectAll("g")
        .data(["You", "Other Users"])
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

}



