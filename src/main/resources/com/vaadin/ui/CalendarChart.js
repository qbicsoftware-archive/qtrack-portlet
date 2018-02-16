/**
 * Draws the Calendar Chart; based on the implementation by Mike Bostock available on bl.ocks:
 * https://bl.ocks.org/mbostock/4063318
 */

// the height for each cell
var CELL_HEIGHT = 19;

/**
 * creates the legend for the horizontal color scale
 * @param element: element for the svg container
 * @param width: width for the legend
 * @param colorScaleLegendHeight: height for the legend
 * @param lastYear: Date, last year we have data from
 * @param firstYear: Date, first year we have data from
 * @param adjusted_height: height for each year "block"
 * @param margin: object, holds the margins
 * @param colorScale: the color scale
 */
function createHorizontalColorScaleLegend(element, width, colorScaleLegendHeight, lastYear, firstYear, adjusted_height,
                                          margin, colorScale) {

    // create the svg which holds the horizontal color scale legend; move it on top of the other divs
    var colorScaleLegendSVG = d3.select(element).append("svg").attr("width", width / 2).attr("height",
        colorScaleLegendHeight).attr("transform", "translate(0," +
        (lastYear + 4 - firstYear) * adjusted_height * (-1) + ")");

    // create the color scale legend
    var colorScaleLegend = colorScaleLegendSVG.append("g")
        .attr("font-family", "sans-serif")
        .attr("font-size", 10)
        .attr("text-anchor", "end")
        .selectAll("g")
        .data(colorScale.range())
        .enter().append("g")
        .attr("transform", function (d, i) {
            return "translate(" + i * 40 + ",20)";
        })
        .attr("class", "legend");

    // add the caption of the color scale legend
    colorScaleLegendSVG.append("text")
        .attr("class", "caption")
        .attr("x", margin.left)
        .attr("y", margin.top)
        .attr("fill", "#000")
        .attr("text-anchor", "start")
        .attr("font-weight", "bold")
        .text("Activity duration (h)");

    // add the colored rectangles to the legend
    colorScaleLegend.append("rect")
        .attr("x", margin.left)
        .attr("width", 30)
        .attr("height", CELL_HEIGHT)
        .attr("fill", function (d) {
            return d;
        });

    // add the domain values of the respective colors to the legend
    colorScaleLegend.append("text")
        .attr("x", margin.left + CELL_HEIGHT)
        .attr("y", CELL_HEIGHT*1.5)
        .attr("dy", "0.32em")
        .text(function (d) {
            return colorScale.invertExtent(d)[0].toFixed(2);
        });    // invertExtent returns e.g. [40, 70]

    // we need to add the last domain value manually, since we have one domain value more than we have colors (we are
    // calling colorScaleLegend with the array of colors; so we're one element short..)
    colorScaleLegendSVG.append("text")
        .attr("x", margin.left + 40 * colorScale.range().length + CELL_HEIGHT)
        .attr("y", CELL_HEIGHT*1.5 + 20)
        .attr("dy", "0.32em")
        .attr("font-family", "sans-serif")
        .attr("font-size", 10)
        .attr("text-anchor", "end")
        .text(colorScale.domain()[1].toFixed(2));    // upper domain boundary
}




/**
 * draws the calendar chart
 * @param data: array of objects holding the data: Object { still: 25533517, walking: 1161451, in_vehicle: 1675745, … }
                                                     averageSteps: 3399.5
                                                     date: "17-01-2018"
                                                     in_vehicle: 0.46548472222222226
                                                     sleeping: 6.65306
                                                     startDateInUTC: 1516147200000
                                                     stdErrorOfMean: 1372.5
                                                     steps: 4772
                                                     still: 7.092643611111112
                                                     total: 7.092643611111112
                                                     walking: 0.3226252777777778
 * @param selectedOptions: options selected by the user; currently not used
 * @param element: element for the svg container
 * @param svg: svg container which holds the g container
 * @param g: container which holds the plots
 * @param width: width available for the svg container
 * @param height: height available for the svg container
 * @param margin: margin for the svg: container
 * @param selectedActivities_: the activities to display selected by the user
 */
function drawCalendarChart(data, selectedOptions, element, svg, g, width, height, margin, selectedActivities_) {

    // 75 pixels space for the horizontal legend displaying the color scale
    var colorScaleLegendHeight = 75;

    // get all activity "names" (e.g. walking, still, in_vehicle, etc.) of the user and store it as array
    var allActivities = extractActivityNames(data);

    // holds the activities selected for display by the user
    var selectedActivities;

    // since this function calls itself recursively when the user changes the activity selection, we need to deal with
    // no selected activities. If this is the case, we set all available activities as selected by the user.
    if (selectedActivities_ === null || selectedActivities_.length === 0) {
        // get all activity names from the user, e.g. "walking", "sleeping", "in_vehicle", "still", etc.
        selectedActivities = allActivities;
    } else {
        selectedActivities = selectedActivities_;
    }

    // remove all previously drawn elements
    d3.selectAll("svg").remove();

    // calculate the cell size and the height for each "year block"
    var cellSize = Math.floor((width-margin.right) / 53);  // there are 52 (+1 overlap) weeks in 1 year
    var adjusted_height = cellSize*8;

    // get the first and last year we have data from
    var firstYear = new Date(data[0].startDateInUTC).getFullYear();
    var lastYear = new Date(data[data.length-1].startDateInUTC).getFullYear();

    // create the "blocks" each one representing one year
    svg = d3.select(element)
        .selectAll("svg")
        .data(d3.range(firstYear, lastYear+1))
        .enter().append("svg")
        .attr("width", width)
        .attr("height", adjusted_height)
        .attr("transform", "translate(0," + colorScaleLegendHeight + ")")
        .append("g")
        .attr("transform", "translate(" + ((width - cellSize * 53) / 2) + ","
            + (adjusted_height - cellSize * 7 - 1) + ")");

    // write the year next to the "year row"
    svg.append("text")
        .attr("transform", "translate(-6," + cellSize * 3.5 + ")rotate(-90)")
        .attr("font-family", "sans-serif")
        .attr("font-size", 10)
        .attr("text-anchor", "middle")
        .text(function(d) { return d; });

    // time format for the dates
    var timeFormat = d3.timeFormat(selectedOptions.timeFormat);

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
        // we want hours, not milliseconds
        d.total = d.total/60000/60;
        // .. by setting the date in a proper time format
        d.date = timeFormat(d.startDateInUTC);
    });

    // get the minimum and the maximum value we have for the total activity time
    var minValueInData = Math.floor(Math.min.apply(Math,data.map(function(d){return d.total;})));
    var maxValueInData = Math.ceil(Math.max.apply(Math,data.map(function(d){return d.total;})));

    // setup the color scale; since we have a limited number of activities, we can use a hard coded color scale
    var color = d3.scaleQuantize()
        .domain([minValueInData, maxValueInData])
        .range(['#deebf7','#c6dbef','#9ecae1','#6baed6','#4292c6','#2171b5','#08519c','#08306b']);

    // create an object holding the dates as key and the activity duration as value
    var nestedData = d3.nest()
        .key(function(d) { return d.date; })
        .rollup(function(d) { return d[0].total; })
        .object(data);

    // prepare drawing the individual days
    var rect = svg.append("g")
        .attr("fill", "none")
        .attr("stroke", "#ccc")
        .selectAll("rect")
        .data(function(d) { return d3.timeDays(new Date(d, 0, 1), new Date(d + 1, 0, 1)); })
        .enter().append("rect")
        .attr("width", cellSize)
        .attr("height", cellSize)
        .attr("x", function(d) { return d3.timeWeek.count(d3.timeYear(d), d) * cellSize; })
        .attr("y", function(d) { return d.getDay() * cellSize; })
        .datum(timeFormat);

    // actually draw them
    rect.filter(function(d) { return d in nestedData; })
        .attr("fill", function(d) { return color(nestedData[d]); })
        .append("title")
        .text(function(d) { return d + ": " + nestedData[d].toFixed(2) + " h"; });

    // create the horizontal legend for the color scale
    createHorizontalColorScaleLegend(element, width, colorScaleLegendHeight, lastYear, firstYear, adjusted_height,
                                     margin, color);

    // holds the vertical legend for selecting the activities to display
    var activitySelectionSVG = d3.select(element).append("svg").attr("width", margin.right).attr("height",
        height).attr("transform", "translate(" + width / 2 + "," +
        (lastYear + 1 - firstYear) * adjusted_height * (-1) + ")");

    // the array which holds all the categories for the legend
    var legendCategoryArray = [];

    // create the legend for the activity selection
    var legend = activitySelectionSVG.append("g")
        .attr("font-family", "sans-serif")
        .attr("font-size", 10)
        .attr("text-anchor", "end")
        .selectAll("g")
        .data(allActivities.slice().reverse())
        .enter().append("g")
        .attr("transform", function (d, i) {
            return "translate(0," + i * 20 + ")";
        })
        .attr("class", function (d) {
            // remove the spaces from the category and add it to the array
            legendCategoryArray.push(d.replace(/\s+/g, ''));
            return "legend";
        });

    //reverse order to match order in which bars are stacked
    legendCategoryArray = legendCategoryArray.reverse();

    // add the colored rectangles to the legend
    legend.append("rect")
        .attr("x", margin.right-20)
        .attr("y", 5)
        .attr("width", CELL_HEIGHT)
        .attr("height", CELL_HEIGHT)
        .attr("fill", "#C0C0C0")
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

                if (selectedActivities.length > 0) {
                    // disable the clicked category
                    selectedActivities.splice(posOfCurrentCategoryInArray, 1);
                }

                // category is not in array -> add it
            } else {
                selectedActivities.push(currentCategory);
            }

            // redraw the chart with the selected categories
            try {
                return selectedActivities;
            } finally {
                drawCalendarChart(data, selectedOptions, element, svg, g, width, height, margin, selectedActivities);
            }
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
        .attr("x", margin.right - 35)
        .attr("y", 9.5 + 5)
        .attr("dy", "0.32em")
        .text(function (d) {
            return d;
        });

    return selectedActivities;
}

