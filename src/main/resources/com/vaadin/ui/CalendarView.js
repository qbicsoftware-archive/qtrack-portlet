

function drawCalendarView(data, selectedOptions, element, svg, g, width, height, margin, selectedActivities_) {

    var selectedActivities;
    console.log(data);
    var allActivities = extractActivityNames(data);

    var colorScaleLegendHeight = 75; // 75 pixels space for the horizontal legend displaying the color scale


    if (selectedActivities_ === null || selectedActivities_.length === 0) {
        // get all activity names from the user, e.g. "walking", "sleeping", "in_vehicle", "still", etc.
        selectedActivities = allActivities;
    } else {
        selectedActivities = selectedActivities_;
    }

    // check if there is at least one category to display
    var hasCategoryBeenFound = false;
    console.log("selected activities:");
    for (var i = 0; i < selectedActivities.length; i++) {
        console.log(selectedActivities[i]);
        if (allActivities.indexOf(selectedActivities[i]) > -1) {
            hasCategoryBeenFound = true;
        }
    }

    // if no category to display has been found, display all categories
    if (!hasCategoryBeenFound) {
        selectedActivities = allActivities;
    }

    // remove previously drawn elements
    d3.selectAll("svg").remove();

    // calculate the cell size and the height for each "year row"
    var cellSize = Math.floor((width-margin.right) / 53);  // there are 52 (+1 overlap) weeks in 1 year
    var adjusted_height = cellSize*8;

    // get the first and last year we have data from
    var firstYear = new Date(data[0].startDateInUTC).getFullYear();
    var lastYear = new Date(data[data.length-1].startDateInUTC).getFullYear();

    // create the "rows" each one representing one year
    var svg = d3.select(element)
      .selectAll("svg")
      .data(d3.range(firstYear, lastYear+1))
      .enter().append("svg")
        .attr("width", width+margin.right)
        .attr("height", adjusted_height)
        .attr("transform", "translate(0," + 75 + ")")
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

    // TODO: make activities selectable
/*
    var allActivities = extractActivityNames(data);
    var selectedActivities = allActivities;
*/


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

    var minValueInData = Math.floor(Math.min.apply(Math,data.map(function(d){return d.total;})));
    var maxValueInData = Math.ceil(Math.max.apply(Math,data.map(function(d){return d.total;})));

    var color = d3.scaleQuantize()
      .domain([minValueInData, maxValueInData])
      .range(['#deebf7','#c6dbef','#9ecae1','#6baed6','#4292c6','#2171b5','#08519c','#08306b']);

    function uniq(a) {
        var seen = {};
        return a.filter(function(item) {
            return seen.hasOwnProperty(item) ? false : (seen[item] = true);
        });
    }

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

    // the array which holds all the categories for the legend
    var legendCategoryArray = [];

    // create the svg which holds the horizontal legend; move it on top of the other divs
    var legendSVG = d3.select(element).append("svg").attr("width", width+margin.right).attr("height",
        colorScaleLegendHeight).attr("transform", "translate(0," + (lastYear+1-firstYear)*adjusted_height*(-1) + ")");

    // create the legend
    var colorScaleLegend = legendSVG.append("g")
        .attr("font-family", "sans-serif")
        .attr("font-size", 10)
        .attr("text-anchor", "end")
        .selectAll("g")
        .data(color.range())
        .enter().append("g")
        .attr("transform", function(d, i) { return "translate(" + i * 40 + ",20)"; })
        .attr("class", function (d) {
            // remove the spaces from the category and add it to the array
            // legendCategoryArray.push(d.replace(/\s+/g, ''));
            return "legend";
        });

    // TODO
    //    .data(color.range())

    //reverse order to match order in which bars are stacked
    legendCategoryArray = legendCategoryArray.reverse();

    // add the caption of the color scale legend
    legendSVG.append("text")
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
        .attr("height", 19)
        .attr("fill", function(d) { return d;} );

    // TODO: replace 19 with height of cell (?) or create separate variable holding the height of the legend rectangles
    // TODO: replace 9.5 with the variable in the the line above, divided by two

    // add the domain values of the respective colors to the legend
    colorScaleLegend.append("text")
        .attr("x", margin.left+9.5)
        .attr("y", 19+9.5)
        .attr("dy", "0.32em")
        .text(function(d) { return color.invertExtent(d)[0].toFixed(2); });    // invertExtent returns e.g. [40, 70]

    // we need to add the last domain value manually, since we have one domain value more than we have colors (we are
    // calling colorScaleLegend with the array of colors; so we're one element short..)
    legendSVG.append("text")
        .attr("x", margin.left+40*color.range().length+9.5)
        .attr("y", 19+9.5+20)
        .attr("dy", "0.32em")
        .attr("font-family", "sans-serif")
        .attr("font-size", 10)
        .attr("text-anchor", "end")
        .text(color.domain()[1].toFixed(2));    // upper domain boundary

   // create the legend
    var legend = legendSVG.append("g")
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

            console.log("selected activities:");
            console.log(selectedActivities);

            // redraw the chart with the selected categories
            try {
                return selectedActivities;
            } finally {
                drawCalendarView(data, selectedOptions, element, svg, g, width, height, margin, selectedActivities);
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
        .attr("x", width + 15)
        .attr("y", 9.5)
        .attr("dy", "0.32em")
        .text(function(d) { return d; });


}

