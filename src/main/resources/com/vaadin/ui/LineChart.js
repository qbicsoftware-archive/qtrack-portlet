/**
 * Draws the line chart displaying the steps data.
 */

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

    // Scale the range of the data
    x.domain(d3.extent(dat, function(d) { return d.startDateInUTC; }));
    y.domain([0, d3.max(dat, function(d) {return Math.max(d.steps, d.averageSteps); })]);

    // define the 1st line
    var userStepsLine = d3.line()
        .x(function(d) { return x(d.startDateInUTC); })
        .y(function(d) { return y(d.steps); });

    // define the 2nd line
    var avgStepsLine = d3.line()
        .x(function(d) { return x(d.startDateInUTC); })
        .y(function(d) { return y(d.averageSteps); });

    // Add the user steps
    g.append("path")
        .data([dat])
        .attr("d", userStepsLine)
        .attr("stroke", selectedOptions.colorForUserSteps)
        .attr("fill", "none")
        .attr("stroke-width", "3px");

    // time format for the dates
    var timeFormat = d3.timeFormat(selectedOptions.timeFormat);

    // add the circles to select the tooltips
    svg.selectAll("dot")
        .data(dat)
        .enter().append("circle")
        .attr("r", 5)
        .attr("cx", function(d) { return x(d.startDateInUTC); })
        .attr("cy", function(d) { return y(d.steps); })
        .attr("transform", "translate(50,12)")
        .attr("fill", selectedOptions.colorForUserSteps)
        .attr("id", function(d,i) { return "user_"+i; })
        .append("svg:title")
        .text(function(d, i) { return timeFormat(d.startDateInUTC)});

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
        .attr("cx", function(d) { return x(d.startDateInUTC); })
        .attr("cy", function(d) { return y(d.averageSteps); })
        .attr("transform", "translate(50,12)")
        .attr("fill", selectedOptions.colorForAvgSteps)
        .attr("id", function(d,i) { return "avg_"+i; })
        .append("svg:title")
        .text(function(d, i) { return timeFormat(d.startDateInUTC)});

    // Add Error Line
    svg.append("g").selectAll("line")
    	.data(dat).enter()
        .append("line")
        .attr("class", "error-line")
        .attr("x1", function(d) {
            return x(d.startDateInUTC);
        })
        .attr("y1", function(d) {
            return y(d.averageSteps + d.stdErrorOfMean);
        })
        .attr("x2", function(d) {
            return x(d.startDateInUTC);
        })
        .attr("y2", function(d) {
            return y(d.averageSteps - d.stdErrorOfMean);
        })
        .attr("transform", "translate(50,12)")
        .attr("stroke-width", 2)
        .style("stroke-dasharray", ("3, 3"))
        .attr("stroke", "black");

    // Add Error Top Cap
    svg.append("g").selectAll("line")
    	.data(dat).enter()
        .append("line")
        .attr("class", "error-cap")
        .attr("x1", function(d) {
      	    return x(d.startDateInUTC) - 4;
        })
        .attr("y1", function(d) {
            return y(d.averageSteps + d.stdErrorOfMean);
        })
        .attr("x2", function(d) {
      	    return x(d.startDateInUTC) + 4;
        })
        .attr("y2", function(d) {
      	    return y(d.averageSteps + d.stdErrorOfMean);
        })
        .attr("transform", "translate(50,12)")
        .attr("stroke-width", 2)
        .attr("stroke", "black");

     // Add Error Bottom Cap
    svg.append("g").selectAll("line")
    	.data(dat).enter()
        .append("line")
        .attr("class", "error-cap")
        .attr("x1", function(d) {
        	return x(d.startDateInUTC) - 4;
        })
        .attr("y1", function(d) {
        	return y(d.averageSteps - d.stdErrorOfMean);
        })
        .attr("x2", function(d) {
        	return x(d.startDateInUTC) + 4;
        })
        .attr("y2", function(d) {
        	return y(d.averageSteps - d.stdErrorOfMean);
        })
        .attr("transform", "translate(50,12)")
        .attr("stroke-width", 2)
        .attr("stroke", "black");

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
     * adds event listener to the k-th data point
     * @param k: index of the data point to add the event listener to
     */
    function addEventListenerToDataPoint(k) {

        var event = document.createEvent('Event');
        event.initEvent('mouseover', true, true);

        document.getElementById("avg_"+k).addEventListener("mouseover", function() {
            var isUserSelected = false;
            createTooltipTextElements({"cx": x(dat[k].startDateInUTC), "cy": y(dat[k].averageSteps),
                "cy_other": y(dat[k].steps), "selectedSteps": dat[k].averageSteps,
                "notSelectedSteps": dat[k].steps}, isUserSelected);
        });

        document.getElementById("user_"+k).addEventListener("mouseover", function() {
            var isUserSelected = true;
            createTooltipTextElements({"cx": x(dat[k].startDateInUTC), "cy": y(dat[k].steps),
                "cy_other": y(dat[k].averageSteps), "selectedSteps": dat[k].steps,
                "notSelectedSteps": dat[k].averageSteps}, isUserSelected);
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
        addEventListenerToDataPoint(k, x, y);
    }

    // Add the X Axis
    g.append("g")
        .attr("transform", "translate(0," + height + ")")
        .call(d3.axisBottom(x)
            .tickFormat(timeFormat)
            .tickValues(getDatesForXAxis(x, width))
        );

    // Add the Y Axis
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
        .text("Steps");

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